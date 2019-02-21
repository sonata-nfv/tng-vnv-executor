package com.optaresolutions.executor.model

class DockerCompose {

    String version = '2.2'
    Map<String, Service> services = new HashMap<>()
}

class Service {
    String image
    String container_name
    int scale
    List<String> environment = new ArrayList<>()
    List<String> depends_on = new ArrayList<>()
    List<String> volumes = new ArrayList<>()
}
