package org.keycloak;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.crd.Keycloak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AugmentationSecret {

    public static Secret getSecret(KubernetesClient client, Keycloak kc) {
        return client
                .secrets()
                .inNamespace(kc.getMetadata().getNamespace())
                .withName(kc.getMetadata().getName() + "-augmentation")
                .get();
    }

    public static List<Secret> desiredSecret(Keycloak kc, Map<String, String> content) {
        var secrets = new ArrayList<Secret>(content.size());
        return new SecretBuilder()
                .withNewMetadata()
                .withName(kc.getMetadata().getName() + "-augmentation")
                .withNamespace(kc.getMetadata().getNamespace())
                .withOwnerReferences(kc.getOwnerRefereces())
                .endMetadata()
                .addToStringData(content)
                .build();
    }
}
