package org.keycloak;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.keycloak.crd.Keycloak;

import java.util.*;

public class AugmentationSecret {

    public static String getSecretName(Keycloak kc, String fileName) {
        return KubernetesResourceUtil.sanitizeName(fileName + "-" + kc.getMetadata().getName() + "-augmentation");
    }

    public static Resource<Secret> secretSelector(KubernetesClient client, Keycloak kc, String fileName) {
        var name = getSecretName(kc, fileName);
        return client
                .secrets()
                .inNamespace(kc.getMetadata().getNamespace())
                .withName(name);
    }

    private static Base64.Encoder encoder = Base64.getEncoder();

    public static Secret desiredSecret(Keycloak kc, String fileName, byte[] content) {
        var name = getSecretName(kc, fileName);
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
