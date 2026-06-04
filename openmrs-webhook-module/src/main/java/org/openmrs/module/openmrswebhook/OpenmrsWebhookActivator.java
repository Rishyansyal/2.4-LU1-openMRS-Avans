package org.openmrs.module.openmrswebhook;

import java.util.Date;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenmrsWebhookActivator extends BaseModuleActivator {
    private static final Logger log = LoggerFactory.getLogger(OpenmrsWebhookActivator.class);

    @Override
    public void started() {
        AppointmentWebhookProperties properties = AppointmentWebhookProperties.fromOpenMrsGlobalProperties();

        new EventSubscriptionRegistrar()
            .registerEncounterListener(new AppointmentEventListener(
                AppointmentWebhookDispatcher::fromOpenMrsGlobalProperties));

        scheduleRetryTask(properties);
    }

    private void scheduleRetryTask(AppointmentWebhookProperties properties) {
        SchedulerService scheduler = Context.getSchedulerService();
        TaskDefinition task = scheduler.getTaskByName(WebhookRetryTask.TASK_NAME);
        if (task == null) {
            task = new TaskDefinition();
            task.setName(WebhookRetryTask.TASK_NAME);
            task.setUuid(WebhookRetryTask.TASK_UUID);
            task.setDescription("Retries failed OpenMRS appointment webhooks from the module outbox.");
            task.setStartTime(new Date());
        }

        task.setTaskClass(WebhookRetryTask.class.getName());
        task.setRepeatInterval(properties.retryIntervalMillis());
        task.setStartOnStartup(true);
        scheduler.saveTaskDefinition(task);
        scheduler.scheduleIfNotRunning(task);

        log.info("Scheduled OpenMRS webhook outbox retry task every {} ms.", properties.retryIntervalMillis());
    }
}
