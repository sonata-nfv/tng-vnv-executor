package app.util

import app.model.docker_compose.DockerCompose
import app.model.docker_compose.Service

import app.model.test.*
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@PropertySource("classpath:application.properties")
@Scope(value = "singleton")

@Slf4j(value = "logger")
class Converter {

    static Converter instance

    @Value('${DC.VOLUME_PATH}')
    String VOLUME_PATH

    //@Value('${DC.CUSTOM_COMMAND}')
    //String CUSTOM_COMMAND

    @Value('${TD.ERROR.NO_PHASES_INFO}')
    String ERROR_NO_PHASES_INFO

    @Value('${TD.ERROR.NO_EXECUTION_PHASE}')
    String ERROR_NO_EXECUTION_PHASE

    @Value('${TD.ERROR.NO_STEPS_EXECUTION_PHASE}')
    String ERROR_NO_STEPS_EXECUTION_PHASE

    @Value('${TD.ERROR.NO_VERIFICATION_PHASE}')
    String ERROR_NO_VERIFICATION_PHASE

    @Value('${TD.ERROR.NO_SETUP_PHASE}')
    String ERROR_NO_SETUP_PHASE

    @Value('${TD.ERROR.NO_PROBES_INFO}')
    String ERROR_NO_PROBES_INFO

    @Value('${TD.ERROR.NO_PROBE_ID}')
    String ERROR_NO_PROBE_ID

    @Value('${TD.ERROR.NO_ENTRYPOINT_START_DELAY}')
    String ERROR_NO_ENTRYPOINT_START_DELAY

    @Value('${TD.ERROR.NO_ENTRYPOINT_DEPENDENCY}')
    String ERROR_NO_ENTRYPOINT_DEPENDENCY

    @Value('${TD.ERROR.DUPLICATE_SERVICE}')
    String ERROR_DUPLICATE_SERVICE

    @Value('${TD.ERROR.NO_DEPENDENCY_FOUND}')
    String ERROR_NO_DEPENDENCY_FOUND

    @Value('${DC.WAIT_FOR_SCRIPT}')
    private String WAIT_FOR_SCRIPT

    @Value('${DC.TEST_PATH}')
    private String TEST_PATH

    @Value('${DC.VOLUME_DOCKER_SOCK}')
    private String VOLUME_DOCKER_SOCK

    @Value('${DC.VOLUME_DOCKER}')
    private String VOLUME_DOCKER

    @Value('${DC.VOLUME_COMPOSE_FILE}')
    private String VOLUME_COMPOSE_FILE

    @Autowired
    @Qualifier("yaml")
    ObjectMapper mapper


    private Converter() {}

    static Converter getInstance() {
        if (!instance) {
            instance = new Converter()
        }
        return instance
    }

    TestDescriptor getTestDescriptor(String testDescriptorString) throws IOException, JsonParseException, JsonMappingException {

        def testDescriptor = mapper.readValue(testDescriptorString, TestDescriptor.class)
        if (!testDescriptor.uuid) {
            testDescriptor.uuid = UUID.randomUUID().toString()
        }

        logger.debug("Read test descriptor: \n ${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testDescriptor)}")
        return testDescriptor

    }

    DockerCompose getDockerCompose(TestDescriptor testDescriptor) throws RuntimeException {

        // Checking if the mandatory components are established
        if (!testDescriptor.phases) {
            throw new RuntimeException(ERROR_NO_PHASES_INFO)
        }

        TestDescriptorSetupPhase setupPhase =
                testDescriptor.getPhase(TestDescriptorPhases.SETUP_PHASE) as TestDescriptorSetupPhase

        if (!setupPhase) {
            throw new RuntimeException(ERROR_NO_SETUP_PHASE)
        }

        TestDescriptorVerificationPhase verificationPhase =
                testDescriptor.getPhase(TestDescriptorPhases.VERIFICATION_PHASE) as TestDescriptorVerificationPhase

        if (!verificationPhase) {
            throw new RuntimeException(ERROR_NO_VERIFICATION_PHASE)
        }

        TestDescriptorExercisePhase exercisePhase =
                testDescriptor.getPhase(TestDescriptorPhases.EXERCISE_PHASE) as TestDescriptorExercisePhase

        if (!exercisePhase) {
            throw new RuntimeException(ERROR_NO_EXECUTION_PHASE)
        }

        if (!exercisePhase.steps) {
            throw new RuntimeException(ERROR_NO_STEPS_EXECUTION_PHASE)
        }


        if (!setupPhase.steps) {
            throw new RuntimeException(ERROR_NO_PROBES_INFO)
        }

        def probes = null
        for (step in setupPhase.steps) {
            if (step.probes) {
                probes = new HashMap<String, Probe>()
                for (probe in step.probes) {
                    probes.put(probe.id, probe)
                }

                break
            }
        }

        if (!probes) {
            throw new RuntimeException(ERROR_NO_PROBES_INFO)
        }

        // Creating the docker-compose
        def dockerCompose = new DockerCompose()
        for (step in exercisePhase.steps) {

            for (auxStep in exercisePhase.steps) {
                if (exercisePhase.steps.indexOf(auxStep) == exercisePhase.steps.indexOf(step)) {
                    continue
                }

                if (auxStep.name == step.name) {
                    throw new RuntimeException(String.format(ERROR_DUPLICATE_SERVICE, step.name))
                }
            }

            Probe probe = probes.get(step.run) as Probe
            if (!probe) {
                throw new RuntimeException(String.format(ERROR_NO_PROBE_ID, step.run))
            }

            def service = new Service(probe)

            if (!step.instances) {
                step.instances = 1
            }

            List<String> waitForCmd = new ArrayList<>()

            service.scale = step.instances
            if (step.dependencies) {

                if (!step.entrypoint) {
                    throw new RuntimeException(ERROR_NO_ENTRYPOINT_DEPENDENCY)
                }

                service.depends_on = step.dependencies
                for (dep in service.depends_on) {
                    if (!(probes.get(dep))) {
                        throw new RuntimeException(String.format(ERROR_NO_DEPENDENCY_FOUND, dep))
                    }

                    waitForCmd.add("${WAIT_FOR_SCRIPT} \"${dep}\" \"${testDescriptor.uuid}\" \"/compose_file/docker-compose.yml\"".toString())
                }
            }

            service.volumes = new ArrayList<>()
            service.volumes.add(String.format(VOLUME_COMPOSE_FILE, testDescriptor.uuid))
            service.volumes.add(String.format(VOLUME_DOCKER_SOCK))
            service.volumes.add(String.format(VOLUME_DOCKER))
            service.volumes.add(String.format(VOLUME_PATH, testDescriptor.uuid, service.name))

            if (waitForCmd.size() != 0) {
                service.volumes.add("${WAIT_FOR_SCRIPT}:${WAIT_FOR_SCRIPT}".toString())
            }

            if (step.start_delay) {

                if (step.start_delay == 0) {
                    step.start_delay = null
                } else if (!step.entrypoint) {
                    throw new RuntimeException(ERROR_NO_ENTRYPOINT_START_DELAY)
                }
            }

            List<String> entrypoint = new ArrayList<>()
            List<String> entrypointCommands = new ArrayList<>()

            if (step.entrypoint){
                if (step.start_delay) {
                    if (waitForCmd.size() == 0){
                        entrypointCommands.add("sleep ${step.start_delay}".toString())
                        entrypointCommands.add(step.entrypoint.toString())
                    } else {
                        for (waitCmd in waitForCmd){
                            entrypointCommands.add(waitCmd.toString())
                        }
                        entrypointCommands.add("sleep ${step.start_delay}".toString())
                        entrypointCommands.add(step.entrypoint.toString())
                    }
                } else {
                    if (waitForCmd.size() == 0){
                        entrypointCommands.add(step.entrypoint.toString())
                    } else {
                        for (waitCmd in waitForCmd){
                            entrypointCommands.add(waitCmd.toString())
                        }
                        entrypointCommands.add(step.entrypoint.toString())
                    }
                }
            }

            if (entrypointCommands.size() != 0) {

                entrypoint.add("/bin/sh")
                entrypoint.add("-c")

                def entrypointCmd = ""

                for (cmd in entrypointCommands) {
                    entrypointCmd = "${entrypointCmd}${cmd}\n".toString()
                }

                entrypoint.add(entrypointCmd)

                service.entrypoint = entrypoint
            }

            if (step.command) {
                service.command = step.command
            }

            dockerCompose.services.put(service.name, service)
        }

        logger.debug("Convert test descriptor in docker-compose: \n ${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dockerCompose)}")
        return dockerCompose

    }

    String serializeDockerCompose(DockerCompose dockerCompose) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dockerCompose)
    }
}