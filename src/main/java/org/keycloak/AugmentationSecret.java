package org.keycloak;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.crd.Keycloak;

import java.util.*;

public class AugmentationSecret {

    public static Secret getSecret(KubernetesClient client, Keycloak kc, String fileName) {
        var name = kc.getMetadata().getName() + "-augmentation-" + fileName;
        return client
                .secrets()
                .inNamespace(kc.getMetadata().getNamespace())
                .withName(name)
                .get();
    }

    private static Base64.Encoder encoder = Base64.getEncoder();

    public static Secret desiredSecret(Keycloak kc, String fileName, byte[] content) {
        var name = kc.getMetadata().getName() + "-augmentation-" + fileName;
        return new SecretBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(kc.getMetadata().getNamespace())
                .withOwnerReferences(kc.getOwnerRefereces())
                .endMetadata()
                .addToData(fileName, encoder.encodeToString(content))
                .build();
    }
}
