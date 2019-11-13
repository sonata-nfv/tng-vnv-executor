[![Build Status](http://jenkins.sonata-nfv.eu/buildStatus/icon?job=tng-vnv-executor/master)](https://jenkins.sonata-nfv.eu/job/tng-vnv-executor)

<p align="center"><img src="https://github.com/sonata-nfv/tng-api-gtw/wiki/images/sonata-5gtango-logo-500px.png" /></p>

# Executor for 5GTANGO Verification and Validation
This is a [5GTANGO](http://www.5gtango.eu) component to execute the Verification and Validation Tests

The Executor module is responsible for executing the Verification and Validation tests requested by the [Curator](https://github.com/sonata-nfv/tng-vnv-curator) component.

It receives a test request with the associated descriptor file (Test Descriptor), a file that contains the test configurations, dependencies, validation and verification conditions, etc. With this information, Executor generates a docker-compose.yaml file and executes the tests sequence with docker-compose tools.

Once tests are finished, Executor check validation and verification conditions, stores the results in the V&V repository and generates a "Completion Test Response" to Curator component.

Please, visit the associated [wiki](https://github.com/sonata-nfv/tng-vnv-executor/wiki) to obtain more information (architecture, workflow, examples, REST Api, etc)

## Installing / Getting Started

This component is implemented in Spring Boot 2.1.3, using Java 11

## Build from source code

```bash
$ git clone https://github.com/sonata-nfv/tng-vnv-executor.git # Clone this repository
$ cd tng-vnv-executor
$ gradlew bootRun
```

## Run the docker image

This will generate a docker image with the latest version of the code. Before building, a test suite is executed.
```bash
$ gradlew clean test build docker
```

To execute the container:
```bash
$ docker run -d -it --name tng-vnv-executor \
 -e CALLBACKS='enabled' \
 -e DELETE_FINISHED_TEST='disabled' \
 -e RESULTS_REPO_URL=<RESULTS_REPO_URL> \
 -e VNV_NFS_SERVER_IP='tng-vnv' \
 -e VNV_NFS_PATH='/executor' \
 -v /var/run/docker.sock:/var/run/docker.sock \
 -v /usr/bin/docker:/usr/bin/docker \
 -v /executor:/executor \
 -p 6300:8080 \
 --network tango \
 sonatanfv/tng-vnv-executor:latest
```

where:
- CALLBACKS: optional environment variable to enable/disable the callbacks from executor to curator
  - values: enabled/disabled
  - default: enabled
- DELETE_FINISHED_TESTS: by default, the generated probes' output files are deleted from VnV Executor host once tests are completed and the results are stored in the repo. For developing purposes. This optional environment variable can maintain these files in the output folders without deletion when is disabled.
  - values: enabled/disabled
  - default: enabled
- RESULTS_REPO_URL: mandatory environment variable that contains the tests results repository http://host:port URL where the results will be stored
- VNV_NFS_SERVER_IP: variable that contains VNV and NFS IP. By default, "tng-vnv"
- VNV_NFS_PATH: variable that contains NFS path. By default, "/executor"
- port: internally, the VnV executor uses the 8080 port. This port can be mapped to another desired port. In 5GTango environments the selected port is 6300 
- network: network where all VnV components (planner, curator, platform adaptor and executor) are configured

## Developing

### Build With
We are using the Spring Boot Framework, org.springframework.boot' version '2.1.3.RELEASE' with the next dependencies (mavenCentral):

| Group | Name | Version |
|---|---|---|
|gradle.plugin.com.palantir.gradle.docker|gradle-docker|0.21.0
|org.springframework.boot|spring-boot-starter-actuator|
|org.codehaus.groovy|groovy-all:2.5.6|
|com.fasterxml.jackson.dataformat|jackson-dataformat-yaml|2.9.8
|org.apache.commons|commons-lang3|3.4
|org.apache.httpcomponents|httpclient|
|org.springframework.boot|spring-boot-starter-web|2.1.3.RELEASE
|org.springframework.boot|spring-boot-starter-data-jpa|2.1.3.RELEASE
|io.springfox|springfox-swagger2:2.9.2
|io.springfox|springfox-swagger-ui:2.9.2
|com.h2database|h2|1.4.198
|org.spockframework|spock-core|1.2-groovy-2.4
|org.springframework.boot|spring-boot-starter-test|
|org.spockframework|spock-spring|1.2-groovy-2.4

### Prerequisites

No specific libraries are required for building this project. The following tools are used to build the component:

- `Java JDK (11+)`
- `gradle (4.9)`
- `docker (18.x)`

### Submiting changes

Changes to the repository can be requested using [this repository's issues](https://github.com/sonata-nfv/tng-vnv-executor/issues) and [pull requests](https://github.com/sonata-nfv/tng-vnv-executor/pulls) mechanisms.

## Versioning

For the versions available, see the [link to tags on this repository](https://github.com/sonata-nfv/tng-vnv-executor/releases).

## Tests

Unit tests are defined in the /src/test folder. To run these tests:

```bash
$ gradle clean test
```

## Database

The Executor component uses an internal H2 in memory database that persists the data in a vnv-executor-db.mv.db file. In containerized version, this file is stored outside the container using a volume.

## Api Reference

Please, check the API in this swagger file: [swagger.json](https://github.com/sonata-nfv/tng-vnv-executor/blob/master/doc/swagger.json), or visit this [url](https://sonata-nfv.github.io/tng-doc/) and select the "5GTANGO V&V Executor API v1" spec in the drop down menu.


## Licensing

This 5GTANGO component is published under Apache 2.0 license. Please see the [LICENSE](LICENSE) file for more details.

## Lead Developers

The following lead developers are responsible for this repository and have admin rights. They can, for example, merge pull requests.

* Santiago Rodríguez ([srodriguez](https://github.com/srodriguezOPT))
* Felipe Vicens ([felipevicens](https://github.com/felipevicens))
* José Bonnet ([jbonnet](https://github.com/jbonnet))

## Feedback-Channel

- You may use the mailing list [sonata-dev-list](mailto:sonata-dev@lists.atosresearch.eu)
- Gitter room [![Gitter](https://badges.gitter.im/sonata-nfv/Lobby.svg)](https://gitter.im/sonata-nfv/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
