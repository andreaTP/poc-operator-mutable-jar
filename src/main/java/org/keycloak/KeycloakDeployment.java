package org.keycloak;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.keycloak.crd.Keycloak;

import java.util.ArrayList;
import java.util.Set;

public class KeycloakDeployment {

    public static boolean isOk(Deployment actual, Deployment desired) {
        if (actual == null || actual.getSpec() == null) {
            return false;
        }
        // This can be better
        return actual.getSpec().equals(desired.getSpec());
    }

    public static RollableScalableResource<Deployment> deploymentSelector(KubernetesClient client, Keycloak kc) {
        return client
                .apps()
                .deployments()
                .inNamespace(kc.getMetadata().getNamespace())
                .withName(kc.getMetadata().getName());
    }

    public static Deployment desiredDeployment(Keycloak kc, Set<String> fileNames) {

        var volumeMounts = new ArrayList<VolumeMount>();
        var volumes = new ArrayList<Volume>();

        for (var file: fileNames) {
            var name = KubernetesResourceUtil.sanitizeName(file);

            volumeMounts.add(
                new VolumeMountBuilder()
                        .withName(name)
                        .withMountPath("/opt/keycloak/lib/quarkus/" + file)
                        .withSubPath(file)
                        .withReadOnly(true)
                        .build());

            volumes.add(
                new VolumeBuilder()
                        .withName(name)
                        .withNewSecret()
                        .withSecretName(AugmentationSecret.getSecretName(kc, file))
                        .endSecret()
                        .build());
        }

        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(kc.getMetadata().getName())
                .withNamespace(kc.getMetadata().getNamespace())
                .withOwnerReferences(kc.getOwnerRefereces())
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels("app", "keycloak")
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", "keycloak")
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("keycloak-main")
                .withImage("quay.io/keycloak/keycloak-x:latest")
                .withArgs("start", "--hostname-strict=false", "--http-enabled=true")
                .addNewPort()
                .withContainerPort(8080)
                .endPort()
                .withVolumeMounts(volumeMounts)
                .endContainer()
                .withVolumes(volumes)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

    }

}
