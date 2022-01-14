package org.keycloak;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.crd.Keycloak;

import java.util.HashMap;

public class AugmentationSecret {

    public static Secret getSecret(KubernetesClient client, Keycloak kc) {
        return client
                .secrets()
                .inNamespace(kc.getMetadata().getNamespace())
                .withName(kc.getMetadata().getName() + "-augmentation")
                .get();
    }

    public static Secret desiredSecret(Keycloak kc) {
        return new SecretBuilder()
                .withNewMetadata()
                .withName(kc.getMetadata().getName() + "-augmentation")
                .withNamespace(kc.getMetadata().getNamespace())
                .withOwnerReferences(kc.getOwnerRefereces())
                .endMetadata()
                .build();
    }
}
