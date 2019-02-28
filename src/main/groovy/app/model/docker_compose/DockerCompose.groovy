package app.model.docker_compose

class DockerCompose {

    String version = "'2.2'"
    Map<String, Service> services = new HashMap<>()

    @Override
    String toString() {
        return "DockerCompose{version=${version}, services=${services.toString()}}"
    }
}
