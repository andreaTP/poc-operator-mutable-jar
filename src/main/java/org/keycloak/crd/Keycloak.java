package org.keycloak.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("org.test")
@Version("v1alpha1")
public
class Keycloak extends CustomResource<Void, FSMStatus> implements Namespaced {

}
