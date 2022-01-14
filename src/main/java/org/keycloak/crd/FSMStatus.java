package org.keycloak.crd;

import org.keycloak.ControllerFSM;

public class FSMStatus {
    public void setProcessed(ControllerFSM processed) {
        this.processed = processed;
    }

    public void setNext(ControllerFSM next) {
        this.next = next;
    }

    private ControllerFSM processed;
    private ControllerFSM next;

    public FSMStatus() {
        this.processed = null;
        this.next = null;
    }

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
