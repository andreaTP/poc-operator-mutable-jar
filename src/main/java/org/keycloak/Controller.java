package org.keycloak;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import org.keycloak.crd.FSMStatus;
import org.keycloak.crd.Keycloak;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@ControllerConfiguration(namespaces = Constants.WATCH_CURRENT_NAMESPACE, finalizerName = Constants.NO_FINALIZER)
public class Controller implements Reconciler<Keycloak>, ErrorStatusHandler<Keycloak>, EventSourceInitializer<Keycloak> {

    private KubernetesClient client;

    Controller(KubernetesClient client) {
        this.client = client;
    }

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

        // TODO: come up with a better control flow for transitions and retry
        if (current == ControllerFSM.AUGMENTATION_STARTED && next == ControllerFSM.AUGMENTATION_STARTED) {
            return UpdateControl.<Keycloak>noUpdate().rescheduleAfter(5, TimeUnit.SECONDS);
        } else if (current == ControllerFSM.AUGMENTATION_STARTED && next == ControllerFSM.AUGMENTATION_FINISHED) {
            keycloak.setStatus(new FSMStatus(current, next));
            return UpdateControl.updateStatus(keycloak).rescheduleAfter(0);
        }

        if (keycloak.getStatus() == null || keycloak.getStatus().getProcessed() != processed || keycloak.getStatus().getNext() != next) {
            keycloak.setStatus(new FSMStatus(current, next));
            return UpdateControl.updateStatus(keycloak);
        } else {
            return UpdateControl.noUpdate();
        }
    }

    @Override
    public Optional<Keycloak> updateErrorStatus(Keycloak keycloak, RetryInfo retryInfo, RuntimeException e) {
        keycloak.setStatus(new FSMStatus());
        return Optional.of(keycloak);
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<Keycloak> eventSourceContext) {
        SharedIndexInformer<Job> jobsInformer =
                client
                        .batch()
                        .v1()
                        .jobs()
                        .inAnyNamespace()
                        .withLabel("app.kubernetes.io/managed-by", "keycloak-operator")
                        .runnableInformer(0);

        return List.of(new InformerEventSource<>(
                jobsInformer, job -> {
            var ownerReferences = job.getMetadata().getOwnerReferences();
            if (!ownerReferences.isEmpty()) {
                return Set.of(new ResourceID(ownerReferences.get(0).getName(),
                        job.getMetadata().getNamespace()));
            } else {
                return Set.of();
            }
        }));
    }
}
