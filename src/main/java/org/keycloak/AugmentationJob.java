package org.keycloak;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.crd.Keycloak;

public class AugmentationJob {

    public static boolean isOk(Job actual, Job desired) {
        return actual.getSpec().equals(desired.getSpec());
    }

    public static Job getJob(KubernetesClient client, Keycloak kc) {
        return client
                .batch()
                .v1()
                .jobs()
                .inNamespace(kc.getMetadata().getNamespace())
                .withName(kc.getMetadata().getName())
                .get();
    }

    public static Job desiredJob(Keycloak kc) {

        // Files to store:
        // /opt/keycloak/lib/quarkus/build-system.properties
        // /opt/keycloak/lib/quarkus/generated-bytecode.jar
        // /opt/keycloak/lib/quarkus/quarkus-application.dat
        // /opt/keycloak/lib/quarkus/transformed-bytecode.jar

        return new JobBuilder()
                .withNewMetadata()
                .withName(kc.getMetadata().getName())
                .withNamespace(kc.getMetadata().getNamespace())
                .withOwnerReferences(kc.getOwnerRefereces())
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .withRestartPolicy("Never")
                .addNewContainer()
                .withName("keycloak-main")
                .withImage("quay.io/keycloak/keycloak-x:latest")
                .withCommand("/bin/bash")
                .withArgs("-c", "/opt/keycloak/bin/kc.sh build && sleep infinity")
                .addNewVolumeMount()
                .withName("augmentation")
                .withMountPath("/opt/keycloak/augmented")
                .withReadOnly(false)
                .endVolumeMount()
                .endContainer()
                .addNewVolume()
                .withName("augmentation")
                .withNewSecret()
                .withSecretName(kc.getMetadata().getName() + "-augmentation")
                .withDefaultMode(256)
                .endSecret()
                .endVolume()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

}
