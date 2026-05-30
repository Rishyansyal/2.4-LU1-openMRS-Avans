package org.openmrs.module.openmrswebhook;

import org.openmrs.Encounter;
import org.openmrs.event.Event;
import org.openmrs.event.EventListener;

public class EventSubscriptionRegistrar {
    private static final String[] ACTIONS = {"CREATED", "UPDATED", "VOIDED", "UNVOIDED"};

    public void registerEncounterListener(EventListener listener) {
        for (String action : ACTIONS) {
            Event.subscribe(Encounter.class, action, listener);
        }
    }
}
