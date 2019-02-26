package app.util

import app.model.docker_compose.DockerCompose
import app.model.test_descriptor.Probe
import app.model.docker_compose.Service
import app.model.test_descriptor.TestDescriptor
import app.model.test_descriptor.TestDescriptorExercisePhase
import app.model.test_descriptor.TestDescriptorPhases
import app.model.test_descriptor.TestDescriptorSetupPhase
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
    private String VOLUME_PATH

    @Value('${DC.CUSTOM_COMMAND}')
    private String CUSTOM_COMMAND

    @Value('${TD.ERROR.NO_PHASES_INFO}')
    private String ERROR_NO_PHASES_INFO

    @Value('${TD.ERROR.NO_EXECUTION_PHASE}')
    private String ERROR_NO_EXECUTION_PHASE

    @Value('${TD.ERROR.NO_STEPS_EXECUTION_PHASE}')
    private String ERROR_NO_STEPS_EXECUTION_PHASE

    @Value('${TD.ERROR.NO_VERIFICATION_PHASE}')
    private String ERROR_NO_VERIFICATION_PHASE

    @Value('${TD.ERROR.NO_SETUP_PHASE}')
    private String ERROR_NO_SETUP_PHASE

    @Value('${TD.ERROR.NO_PROBES_INFO}')
    private String ERROR_NO_PROBES_INFO

    @Value('${TD.ERROR.NO_PROBE_ID}')
    private String ERROR_NO_PROBE_ID

    @Value('${TD.ERROR.NO_ENTRYPOINT_START_DELAY}')
    private String NO_ENTRYPOINT_START_DELAY

    @Value('${DC.WAIT_FOR_SCRIPT}')
    private String WAIT_FOR_SCRIPT

    @Value('${DC.TEST_PATH}')
    private String TEST_PATH

    @Autowired
    @Qualifier("yaml")
    ObjectMapper mapper


    private Converter() {}

    static Converter getInstance() {
        if(!instance) {
            instance = new Converter()
        }
        return instance
    }

    TestDescriptor getTestDescriptor(String testDescriptorString) throws IOException, JsonParseException, JsonMappingException{

        def testDescriptor = mapper.readValue(testDescriptorString, TestDescriptor.class)
        if (!testDescriptor.id) {
            testDescriptor.id = UUID.randomUUID().toString()
        }

        logger.debug("Read test descriptor: \n ${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testDescriptor)}")
        return testDescriptor

    }

    DockerCompose getDockerCompose(TestDescriptor testDescriptor) throws RuntimeException {

        // Checking if the mandatory components are established
        TestDescriptorExercisePhase exercisePhase =
                testDescriptor.getPhase(TestDescriptorPhases.EXERCISE_PHASE) as TestDescriptorExercisePhase

        if(!exercisePhase) {
            throw new RuntimeException(ERROR_NO_EXECUTION_PHASE)
        }

        if(!exercisePhase.steps) {
            throw new RuntimeException(ERROR_NO_STEPS_EXECUTION_PHASE)
        }

        TestDescriptorSetupPhase setupPhase =
                testDescriptor.getPhase(TestDescriptorPhases.SETUP_PHASE) as TestDescriptorSetupPhase

        if(!setupPhase) {
            throw new RuntimeException(ERROR_NO_SETUP_PHASE)
        }

        if(!setupPhase.steps) {
            throw new RuntimeException(ERROR_NO_PROBES_INFO)
        }

        def probes = null
        for (step in setupPhase.steps) {
            if(step.probes) {
                probes = new HashMap<String, Probe>()
                for (probe in step.probes) {
                    probes.put(probe.name, probe)
                }

                break
            }
        }

        if(!probes){
            throw new RuntimeException(ERROR_NO_PROBES_INFO)
        }

        // Creating the docker-compose
        def dockerCompose = new DockerCompose()
        for(step in exercisePhase.steps) {

            Probe probe = probes.get(step.run) as Probe
            if(!probe) {
                throw new RuntimeException(String.format(ERROR_NO_PROBE_ID, step.run))
            }

            def service = new Service(probe)

            if(!step.instances) {
                step.instances = 1
            }

            service.scale = step.instances

            //working with dependencies
            TEST_PATH=String.format(TEST_PATH, testDescriptor.id)
            service.depends_on = new ArrayList<>()
            def WAIT_FOR_CMD=""
            for (dep in step.dependency){
                service.depends_on.add(dep.toString())
                WAIT_FOR_CMD = "${WAIT_FOR_CMD}; ${WAIT_FOR_SCRIPT} ${dep} ${TEST_PATH}".toString()
            }
            //removing first ";" to command
            WAIT_FOR_CMD = WAIT_FOR_CMD.replaceFirst("; ","");

            service.volumes = new ArrayList<>()
            service.volumes.add(String.format(VOLUME_PATH, testDescriptor.id, service.name))
            service.volumes.add(String.format(WAIT_FOR_SCRIPT,":",WAIT_FOR_SCRIPT))

            if (step.start_delay) {

                if(step.start_delay == 0) {
                    step.start_delay = null
                }

                else if(!step.entrypoint) {
                    throw new RuntimeException(NO_ENTRYPOINT_START_DELAY)
                }
            }

            if(step.entrypoint) {
                def command = step.start_delay ? "${WAIT_FOR_CMD}; sleep ${step.start_delay}; ${step.entrypoint}".toString() : "{WAIT_FOR_CMD}; ${step.entrypoint}".toString()
                service.command = String.format(CUSTOM_COMMAND, command)
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