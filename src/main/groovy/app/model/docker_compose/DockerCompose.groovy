package app.model.docker_compose

import app.model.test_descriptor.Probe
import com.fasterxml.jackson.annotation.JsonIgnore

class DockerCompose {

    String version = '2.2'
    Map<String, Service> services = new HashMap<>()

    @Override
    String toString() {
        return "DockerCompose{version=${version}, services=${services.toString()}}"
    }
}
