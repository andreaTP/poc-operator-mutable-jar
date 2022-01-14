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
        return new JobBuilder()
                .withNewMetadata()
                .withName(kc.getMetadata().getName())
                .withNamespace(kc.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withName("keycloak-main")
                .withImage("quay.io/keycloak/keycloak-x:latest")
                .withCommand("build")
                .addNewPort()
                .withHostPort(8080)
                .endPort()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

}
