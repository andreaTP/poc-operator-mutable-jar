package org.keycloak;

import java.util.function.Function;
import java.util.logging.Logger;

public enum ControllerFSM {
    UNKNOWN(ControllerFSM::unknown),
    AUGMENTATION_STARTED(ControllerFSM::augmentationStarted),
    AUGMENTATION_FINISHED(ControllerFSM::augmentationFinished),
    DONE(ControllerFSM::done),
    ERROR(ControllerFSM::error);

    private static final Logger logger = Logger.getLogger(ControllerFSM.class.getName());

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

        var actual = KeycloakDeployment.getDeployment(fsmContext.getClient(), kc);
        var desired = KeycloakDeployment.desiredDeployment(kc);

        if (KeycloakDeployment.isOk(actual, desired)) {
            logger.info("Everything is fine");
            return DONE;
        } else {
            // Simplification: form now we blindly re-create the resources
            logger.info("Recreating the the resources");

            // WORKAROUND
            // fsmContext.getClient().batch().v1().jobs().createOrReplace(AugmentationJob.desiredJob(kc));
            if (AugmentationJob.getJob(fsmContext.getClient(), fsmContext.getKeycloak()) != null) {
                fsmContext
                        .getClient()
                        .batch()
                        .v1()
                        .jobs()
                        .inNamespace(kc.getMetadata().getNamespace())
                        .withName(kc.getMetadata().getName())
                        .delete();
            }
            fsmContext.getClient().batch().v1().jobs().create(AugmentationJob.desiredJob(kc));

            return AUGMENTATION_STARTED;
        }
    }

    // need to create the job
    // fetch the files
    // create the secret
    // go ahead ...

    public static ControllerFSM augmentationStarted(FSMContext fsmContext) {
        logger.info("Augmentation has started, waiting for it to finish");

        var job = AugmentationJob.getJob(fsmContext.getClient(), fsmContext.getKeycloak());
        if (job == null) {
            logger.info("The Job doesn't exists anymore!");
            return UNKNOWN;
        } else {
            if (job.getStatus().getFailed() > 0) {
                logger.info("The Job Failed");
                return ERROR;
                // TODO: need to check the logs
            } else if (job.getStatus().getSucceeded() > 0) {
                logger.info("The Job Succeeded");
                return AUGMENTATION_FINISHED;
            } else {
                logger.info("Waiting for the Job to finish");
                return AUGMENTATION_STARTED;
            }
        }
    }

    public static ControllerFSM augmentationFinished(FSMContext fsmContext) {
        // Here we need to copy the files from the pod
        fsmContext
                .getClient()
                .pods()
                .inNamespace(fsmContext.getKeycloak().getMetadata().getNamespace())
                .withName("TODO")
                .file("/opt/keycloak/lib/quarkus/build-system.properties")
                .read()

        // WORKAROUND
        // fsmContext.getClient().secrets().createOrReplace(AugmentationSecret.desiredSecret(kc));
        if (AugmentationSecret.getSecret(fsmContext.getClient(), fsmContext.getKeycloak()) != null) {
            fsmContext
                    .getClient()
                    .secrets()
                    .inNamespace(kc.getMetadata().getNamespace())
                    .withName(kc.getMetadata().getName() + "-augmentation")
                    .delete();
        }
        fsmContext.getClient().secrets().create(AugmentationSecret.desiredSecret(kc));

        // and now delete the Job

        fsmContext
                .getClient()
                .apps()
                .deployments()
                .createOrReplace(KeycloakDeployment.desiredDeployment(fsmContext.getKeycloak()));

        return DONE;
    }

    public static ControllerFSM done(FSMContext fsmContext) {
        var kc = fsmContext.getKeycloak();

        var actual = KeycloakDeployment.getDeployment(fsmContext.getClient(), kc);
        var desired = KeycloakDeployment.desiredDeployment(kc);

        // Here we can better control the messages in the Status for example
        if (KeycloakDeployment.isOk(actual, desired)) {
            return DONE;
        } else {
            return UNKNOWN;
        }
    }

    public static ControllerFSM error(FSMContext FSMContext) {
        logger.info("The Job Failed - we wait for the CR to be re-created to retry reconciling");
        return ERROR;
    }
}
