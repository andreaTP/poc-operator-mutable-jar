package org.keycloak;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import org.keycloak.crd.Keycloak;

public class FSMContext {
    private KubernetesClient client;
    private Keycloak keycloak;
    private Context context;

    public FSMContext(KubernetesClient client, Keycloak keycloak, Context context) {
        this.client = client;
        this.keycloak = keycloak;
        this.context = context;
    }

    public KubernetesClient getClient() {
        return client;
    }

    public Keycloak getKeycloak() {
        return keycloak;
    }

    public Context getContext() {
        return context;
    }
}
