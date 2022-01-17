package org.keycloak.crd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import java.util.ArrayList;
import java.util.List;

@Group("org.test")
@Version("v1alpha1")
public
class Keycloak extends CustomResource<Void, FSMStatus> implements Namespaced {

    @JsonIgnore
    public List<OwnerReference> getOwnerRefereces() {
        var ownerReferences = new ArrayList<OwnerReference>();
        ownerReferences.add(
                new OwnerReferenceBuilder()
                        .withController(true)
                        .withBlockOwnerDeletion(true)
                        .withApiVersion(this.getApiVersion())
                        .withKind(this.getKind())
                        .withName(this.getMetadata().getName())
                        .withUid(this.getMetadata().getUid())
                        .build()
        );
        return ownerReferences;
    }

    public Keycloak withStatus(FSMStatus status) {
        this.setStatus(status);
        return this;
    }
}
