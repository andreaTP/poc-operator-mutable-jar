package org.keycloak;

import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.function.Function;

public enum ControllerFSM {
    UNKNOWN(ControllerFSM::unknown),
    AUGMENTATION_STARTED(ControllerFSM::augmentationStarted),
    AUGMENTATION_FINISHED(ControllerFSM::augmentationFinished),
    DONE(ControllerFSM::done);

    private final Function<FSMContext, ControllerFSM> current;

    ControllerFSM(Function<FSMContext, ControllerFSM> current) {
        if (current != null) {
            this.current = current;
        } else {
            this.current = ControllerFSM::unknown;
        }
    }

    ControllerFSM apply(FSMContext fsmContext) {
        return current.apply(fsmContext);
    }

    public static ControllerFSM unknown(FSMContext fsmContext) {
        var kc = fsmContext.getKeycloak();

        if (kc == null) {
            return UNKNOWN;
        } else {
            var actual = KeycloakDeployment.getDeployment(fsmContext.getClient(), kc);
            var desired = KeycloakDeployment.desiredDeployment(kc);

            if (KeycloakDeployment.isOk(actual, desired)) {
                return DONE;
            } else {
                // TODO: go on from here
                // Create secret
                // Create the job

                fsmContext.getClient().apps().deployments().createOrReplace(desired);

                return AUGMENTATION_STARTED;
            }
        }
    }

    public static ControllerFSM augmentationStarted(FSMContext FSMContext) {
        return null;
    }

    public static ControllerFSM augmentationFinished(FSMContext FSMContext) {
        return null;
    }

    public static ControllerFSM done(FSMContext FSMContext) {
        return null;
    }
}
