package org.keycloak;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.crd.Keycloak;

public class KeycloakDeployment {

    public static boolean isOk(Deployment actual, Deployment desired) {
        if (actual == null || actual.getSpec() == null) {
            return false;
        }
        return actual.getSpec().equals(desired.getSpec());
    }

    public static Deployment getDeployment(KubernetesClient client, Keycloak kc) {
        return client
                .apps()
                .deployments()
                .inNamespace(kc.getMetadata().getNamespace())
                .withName(kc.getMetadata().getName())
                .get();
    }

    public static Deployment desiredDeployment(Keycloak kc) {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(kc.getMetadata().getName())
                .withNamespace(kc.getMetadata().getNamespace())
                .withOwnerReferences(kc.getOwnerRefereces())
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
//                .addNewInitContainer()
//                // TODO: init container image and command
//                .addNewVolumeMount()
//                .withName("distribution")
//                .withMountPath("/mnt/distribution")
//                .endVolumeMount()
//                .addNewVolumeMount()
//                .withName("augmentation")
//                .withMountPath("/mnt/augmentation")
//                .endVolumeMount()
//                .endInitContainer()
                .addNewContainer()
                .withName("keycloak-main")
                .withImage("quay.io/keycloak/keycloak-x:latest")
                .withArgs("start")
                .addNewPort()
                .withHostPort(8080)
                .endPort()
                .addNewVolumeMount()
                .withName("augmentation")
                .withMountPath("/opt/keycloak/lib/quarkus")
                .withReadOnly(true)
                .endVolumeMount()
                .endContainer()
                // Secret containing the tar.gz of the cached distribution
                .addNewVolume()
                .withName("augmentation")
                .withNewSecret()
                .withSecretName(kc.getMetadata().getName() + "-augmentation")
                .endSecret()
                .endVolume()
                // Empty dir where the distribution will be unpacked
//                .addNewVolume()
//                .withName("distribution")
//                .withNewEmptyDir()
//                .endEmptyDir()
//                .endVolume()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

}
