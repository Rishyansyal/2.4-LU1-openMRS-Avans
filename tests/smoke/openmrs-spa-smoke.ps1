param(
    [string]$BaseUrl = "http://localhost:3032",
    [switch]$SkipBrowser
)

$ErrorActionPreference = "Stop"

function Assert-Http {
    param(
        [string]$Path,
        [int[]]$ExpectedStatus = @(200),
        [string]$ExpectedContentType = ""
    )

    $url = "$BaseUrl$Path"
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $client = [System.Net.Http.HttpClient]::new($handler)
    try {
        $response = $client.GetAsync($url).GetAwaiter().GetResult()
    }
    finally {
        $client.Dispose()
        $handler.Dispose()
    }

    if ($ExpectedStatus -notcontains [int]$response.StatusCode) {
        throw "Unexpected HTTP status for ${url}: $([int]$response.StatusCode). Expected: $($ExpectedStatus -join ', ')"
    }

    if ($ExpectedContentType) {
        $contentType = [string]$response.Content.Headers.ContentType
        if (!$contentType.Contains($ExpectedContentType)) {
            throw "Unexpected content type for ${url}: '$contentType'. Expected it to contain '$ExpectedContentType'."
        }
    }

    return $response
}

Write-Host "Checking OpenMRS O3 routes at $BaseUrl"

$openmrs = Assert-Http -Path "/openmrs" -ExpectedStatus @(301, 302, 308)
if ([string]$openmrs.Headers.Location -notmatch "/openmrs/spa/home$") {
    throw "/openmrs did not redirect to /openmrs/spa/home. Location: $($openmrs.Headers.Location)"
}

$legacy = Assert-Http -Path "/openmrs/legacy" -ExpectedStatus @(301, 302, 308)
if ([string]$legacy.Headers.Location -notmatch "/openmrs/login.htm$") {
    throw "/openmrs/legacy did not redirect to /openmrs/login.htm. Location: $($legacy.Headers.Location)"
}

$spaHome = Assert-Http -Path "/openmrs/spa/home" -ExpectedContentType "text/html"
$html = $spaHome.Content.ReadAsStringAsync().GetAwaiter().GetResult()
if ($html.Contains('$SPA_PATH') -or $html.Contains('$API_URL') -or $html.Contains('$SPA_CONFIG_URLS')) {
    throw "SPA HTML still contains unresolved runtime placeholders."
}

$assets = @(
    @{ Path = "/openmrs/spa/importmap.json"; ContentType = "json" },
    @{ Path = "/openmrs/spa/routes.registry.json"; ContentType = "json" },
    @{ Path = "/openmrs/spa/config-core_demo.json"; ContentType = "json" }
)

foreach ($asset in $assets) {
    $response = Assert-Http -Path $asset.Path -ExpectedContentType $asset.ContentType
    $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ($content.Contains('$SPA_PATH') -or $content.Contains('$API_URL') -or $content.Contains('$SPA_CONFIG_URLS')) {
        throw "$($asset.Path) still contains unresolved runtime placeholders."
    }
}

if ($SkipBrowser) {
    Write-Host "Browser console check skipped."
    exit 0
}

if (!(Get-Command node -ErrorAction SilentlyContinue) -or !(Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Warning "Node/npm not found; HTTP smoke checks passed, browser console check skipped."
    exit 0
}

$script = @"
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  const errors = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(msg.text());
  });
  page.on('pageerror', (error) => errors.push(error.message));
  const response = await page.goto('$BaseUrl/openmrs/spa/home', { waitUntil: 'networkidle', timeout: 45000 });
  if (!response || response.status() >= 400) {
    throw new Error('OpenMRS SPA returned HTTP ' + (response && response.status()));
  }
  const text = await page.locator('body').innerText({ timeout: 10000 }).catch(() => '');
  if (!text || text.trim().length < 5) {
    throw new Error('OpenMRS SPA body appears blank.');
  }
  if (errors.length) {
    throw new Error('Browser console errors:\n' + errors.join('\n'));
  }
  await browser.close();
})();
"@

$playwrightDir = Join-Path $env:TEMP "openmrs-spa-smoke-playwright"
New-Item -ItemType Directory -Force -Path $playwrightDir | Out-Null
$packageJson = Join-Path $playwrightDir "package.json"
if (!(Test-Path $packageJson)) {
    Push-Location $playwrightDir
    try {
        npm init -y | Out-Null
    }
    finally {
        Pop-Location
    }
}

$playwrightPackage = Join-Path $playwrightDir "node_modules\playwright\package.json"
if (!(Test-Path $playwrightPackage)) {
    Push-Location $playwrightDir
    try {
        npm install playwright@1.54.2 --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to install Playwright package."
        }
    }
    finally {
        Pop-Location
    }
}

$jsPath = Join-Path $playwrightDir "openmrs-spa-smoke.js"
Set-Content -LiteralPath $jsPath -Value $script -Encoding UTF8

try {
    node $jsPath
    if ($LASTEXITCODE -ne 0) {
        throw "Browser smoke check failed."
    }
}
finally {
    Remove-Item -LiteralPath $jsPath -ErrorAction SilentlyContinue
}

Write-Host "OpenMRS O3 smoke checks passed."
