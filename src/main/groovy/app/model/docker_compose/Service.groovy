package app.model.docker_compose

import app.model.test_descriptor.Probe
import com.fasterxml.jackson.annotation.JsonIgnore

class Service {

    String image
    Number scale
    String command
    List<String> environment
    List<String> depends_on
    List<String> volumes

    @JsonIgnore
    String name

    Service() {}

    Service(Probe probe) {

        def parameters = new ArrayList<String>()
        for(parameter in probe.parameters) {
            for(key in parameter.keySet()) {
                def parametersVar = "${key}=${parameter.get(key)}".toString()
                parameters.add(parametersVar)
            }
        }

        name = probe.name
        environment = parameters
        image = probe.image
    }

    @Override
    String toString() {
        return "Service{name=${name}, image=${image}, scale=${scale}, command=${command}, environment=${environment}, depends_on=${depends_on}, volumes=${volumes}}"
    }
}
