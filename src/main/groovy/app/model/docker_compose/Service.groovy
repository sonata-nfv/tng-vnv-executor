package app.model.docker_compose

import app.model.test.Probe
import com.fasterxml.jackson.annotation.JsonIgnore

class Service {

    String image
    Number scale
    String command
    List<String> entrypoint
    List<String> environment
    List<String> depends_on
    List<String> volumes

    @JsonIgnore
    String name

    Service() {}

    Service(Probe probe) {

        def parameters = new ArrayList<Map>()
        for(parameter in probe.parameters) {
            //for(key in parameter.keySet()) {
                def parametersVar = "${parameter.get("key")}=${parameter.get("value")}".toString()
                parameters.add(parametersVar)
            //}
        }

        name = probe.name
        environment = parameters
        image = probe.image
    }

    @Override
    String toString() {
        return "Service{name=${name}, image=${image}, scale=${scale}, entrypoint=${entrypoint}, command=${command}, environment=${environment}, depends_on=${depends_on}, volumes=${volumes}}"
    }
}
