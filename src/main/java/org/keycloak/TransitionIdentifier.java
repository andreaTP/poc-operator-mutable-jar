package org.keycloak;

import java.util.Objects;

public class TransitionIdentifier {
    private ControllerFSM previous;
    private ControllerFSM next;

    public TransitionIdentifier(ControllerFSM previous, ControllerFSM next) {
        this.previous = previous;
        this.next = next;
    }

    @Override
    public int hashCode() {
        return Objects.hash(previous.name(), next.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransitionIdentifier that = (TransitionIdentifier) o;
        return previous.name() == that.previous.name() && next.name() == that.next.name();
    }
}
