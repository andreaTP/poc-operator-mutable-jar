package org.keycloak;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.keycloak.crd.FSMStatus;
import org.keycloak.crd.Keycloak;

@ControllerConfiguration(namespaces = Constants.WATCH_CURRENT_NAMESPACE, finalizerName = Constants.NO_FINALIZER)
public class Controller implements Reconciler<Keycloak> {

    @Inject
    KubernetesClient client;

    @Override
    public UpdateControl<Keycloak> reconcile(Keycloak keycloak, Context context) {
        var fsmContext = new FSMContext(client, keycloak, context);

        ControllerFSM current = null;
        ControllerFSM processed = null;
        ControllerFSM next = null;
        if (keycloak.getStatus() == null || keycloak.getStatus().getNext() == null) {
            current = ControllerFSM.UNKNOWN;
        } else {
            current = keycloak.getStatus().getNext();
        }

        next = current.apply(fsmContext);
        processed = current;

        if (keycloak.getStatus().getProcessed() != processed || keycloak.getStatus().getNext() != next) {
            keycloak.setStatus(new FSMStatus(current, next));
            return UpdateControl.updateStatus(keycloak);
        } else {
            return UpdateControl.noUpdate();
        }
    }
}
