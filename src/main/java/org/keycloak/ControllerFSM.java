package org.keycloak;

import io.fabric8.kubernetes.client.dsl.ExecListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

public enum ControllerFSM {
    UNKNOWN(ControllerFSM::unknown),
    AUGMENTATION_STARTED(ControllerFSM::augmentationStarted),
    AUGMENTATION_FINISHED(ControllerFSM::augmentationFinished),
    DONE(ControllerFSM::done),
    ERROR(ControllerFSM::error);

    private static final Logger logger = Logger.getLogger(ControllerFSM.class.getName());

    public static Set<String> fileNames = Set.of(
        "build-system.properties",
        "generated-bytecode.jar",
        "quarkus-application.dat",
        "transformed-bytecode.jar"
    );

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
        var desired = KeycloakDeployment.desiredDeployment(kc, fileNames);

        if (KeycloakDeployment.isOk(actual, desired)) {
            logger.info("Everything is fine");
            return DONE;
        } else {
            logger.info("Recreating the the resources");

            // WORKAROUND
            // fsmContext.getClient().batch().v1().jobs().createOrReplace(AugmentationJob.desiredJob(kc));
//            if (AugmentationJob.getJob(fsmContext.getClient(), fsmContext.getKeycloak()) != null) {
//                fsmContext
//                        .getClient()
//                        .batch()
//                        .v1()
//                        .jobs()
//                        .inNamespace(kc.getMetadata().getNamespace())
//                        .withName(kc.getMetadata().getName())
//                        .delete();
//            }
            if (AugmentationJob.getJob(fsmContext.getClient(), fsmContext.getKeycloak()) == null) {
                fsmContext
                        .getClient()
                        .batch()
                        .v1()
                        .jobs()
                        .create(AugmentationJob.desiredJob(kc));
            }

            return AUGMENTATION_STARTED;
        }
    }

    // need to create the job
    // fetch the files
    // create the secret
    // go ahead ...

    private static boolean isKeycloakBuildFinished(FSMContext fsmContext) {
        var augmentationFinished = false;
        try {
            var jobLogs = AugmentationJob
                    .jobSelector(fsmContext.getClient(), fsmContext.getKeycloak())
                    .getLog(false);

            return jobLogs.contains("Quarkus augmentation completed");
        } catch (Exception e) {
            logger.info("No logs");
            return false;
        }
    }

    public static ControllerFSM augmentationStarted(FSMContext fsmContext) {
        logger.info("Augmentation has started, waiting for it to finish");

        var job = AugmentationJob.getJob(fsmContext.getClient(), fsmContext.getKeycloak());
        if (job == null) {
            logger.info("The Job doesn't exists anymore!");
            return UNKNOWN;
        } else {
            if (job.getStatus() != null && job.getStatus().getFailed() != null && job.getStatus().getFailed() > 0) {
                logger.info("The Job Failed");
                return ERROR;
            } else if (job.getStatus() != null &&
                    job.getStatus().getActive() != null &&
                    job.getStatus().getActive() > 0 &&
                    isKeycloakBuildFinished(fsmContext)) {
                logger.info("The Job Succeeded");
                return AUGMENTATION_FINISHED;
            } else {
                logger.info("Waiting for the Job to finish");
                return AUGMENTATION_STARTED;
            }
        }
    }

    public static ControllerFSM augmentationFinished(FSMContext fsmContext) {
        logger.info("Augmentation finished");

        var kc = fsmContext.getKeycloak();
        // Here we need to copy the files from the pod
//        AugmentationJob
//                .jobSelector(fsmContext.getClient(), fsmContext.getKeycloak())
//                        .fromServer().get().

        var jobPodName = fsmContext
                .getClient()
                .pods()
                .inNamespace(kc.getMetadata().getNamespace())
                .withLabel("job-name", kc.getMetadata().getName())
                .list()
                .getItems()
                .get(0)
                .getMetadata()
                .getName();

        Map<String, byte[]> files = new HashMap();

        for (var file: fileNames) {
            try {
                logger.severe("Reading content of " + file);
                var content = fsmContext
                        .getClient()
                        .pods()
                        .inNamespace(kc.getMetadata().getNamespace())
                        .withName(jobPodName)
                        .file("/opt/keycloak/lib/quarkus/" + file)
                        .read()
                        .readAllBytes();

                files.put(file, content);

                // TODO: provide an alternative backed by a PVC? Or engineer this even more to split files
                if (content.length > 1000000) { // !Mb is the Secret maximum size
                    throw new IllegalArgumentException("The augmented file " + file + " is bigger than 1Mb(" + content.length + ") and cannot be stored in a Secret");
                }
            } catch (Exception ex) {
                logger.severe("Cannot read file " + file + " content");
                throw new RuntimeException(ex);
            }
        }

        for (var file: fileNames) {
            // WORKAROUND
            // fsmContext.getClient().secrets().createOrReplace(AugmentationSecret.desiredSecret(kc));
            if (AugmentationSecret.getSecret(fsmContext.getClient(), fsmContext.getKeycloak(), file) != null) {
                fsmContext
                        .getClient()
                        .secrets()
                        .inNamespace(kc.getMetadata().getNamespace())
                        .withName(kc.getMetadata().getName() + "-augmentation-" + file)
                        .delete();
            }

            logger.info("Creating secret " + file);
            // Secret single file limit is 1 Mb -> we store each file into a separate secret
            fsmContext
                    .getClient()
                    .secrets()
                    .create(AugmentationSecret.desiredSecret(kc, file, files.get(file)));
        }

        // delete the Job
        logger.info("Deleting job");
        AugmentationJob
                .jobSelector(fsmContext.getClient(), kc)
                .delete();

        // and finally create the Deployment

        logger.info("Creating deployment");
        fsmContext
                .getClient()
                .apps()
                .deployments()
                .createOrReplace(KeycloakDeployment.desiredDeployment(fsmContext.getKeycloak(), fileNames));

        return DONE;
    }

    //    REMOVE ME???
    private static class SimpleListener implements ExecListener {

        @Override
        public void onOpen() {
            System.out.println("The shell will remain open for 10 seconds.");
        }

        @Override
        public void onFailure(Throwable t, Response failureResponse) {
            System.err.println("shell barfed");
        }

        @Override
        public void onClose(int code, String reason) {
            System.out.println("The shell will now close.");
        }
    }

    public static ControllerFSM done(FSMContext fsmContext) {
        var kc = fsmContext.getKeycloak();

        var actual = KeycloakDeployment.getDeployment(fsmContext.getClient(), kc);
        var desired = KeycloakDeployment.desiredDeployment(kc, fileNames);

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
