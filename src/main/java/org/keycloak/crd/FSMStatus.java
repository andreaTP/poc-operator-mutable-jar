package org.keycloak.crd;

import org.keycloak.ControllerFSM;

public class FSMStatus {
    private ControllerFSM processed;
    private ControllerFSM next;

    public FSMStatus(ControllerFSM processed, ControllerFSM next) {
        this.processed = processed;
        this.next = next;
    }

    public ControllerFSM getProcessed() {
        return processed;
    }

    public ControllerFSM getNext() {
        return next;
    }
}
