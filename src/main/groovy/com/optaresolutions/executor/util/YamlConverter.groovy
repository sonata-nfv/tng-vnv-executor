package com.optaresolutions.executor.util

import com.optaresolutions.executor.model.DockerCompose
import com.optaresolutions.executor.model.Probe
import com.optaresolutions.executor.model.TestDescriptor
import com.optaresolutions.executor.model.TestDescriptorExercisePhase
import com.optaresolutions.executor.model.TestDescriptorPhases
import com.optaresolutions.executor.model.TestDescriptorVerificationPhase
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.Tag

@Component
@PropertySource("classpath:application.properties")
@Scope(value = "singleton")

@Slf4j(value = "logger")
class YamlConverter {

    static YamlConverter instance

    @Value('${TD.PHASES_ID}')
    private String PHASES_ID

    @Value('${TD.SETUP_PHASE_ID}')
    private String SETUP_PHASE_ID

    @Value('${TD.EXERCISE_PHASE_ID}')
    private String EXERCISE_PHASE_ID

    @Value('${TD.VERIFICATION_PHASE_ID}')
    private String VERIFICATION_PHASE_ID

    @Autowired
    @Qualifier("default")
    Yaml yaml

    @Autowired
    @Qualifier("skipMissingProperties")
    Yaml yamlSkipMissingProperties

    @Autowired
    @Qualifier("skipTags")
    Yaml yamlSkipTags


    private YamlConverter() {}

    static YamlConverter getInstance() {
        if(!instance) {
            instance = new YamlConverter()
        }
        return instance
    }

    TestDescriptor getTestDescriptor(String testDescriptorString) {

        def testDescriptor = new TestDescriptor()
        testDescriptor.phases = new HashMap<>()

        Map<String, Object> testDescriptorMap = yaml.load(testDescriptorString)
        logger.info(testDescriptorMap.toString())

        List<Object> phases =  (testDescriptorMap.get(PHASES_ID) as List<Object>)
        logger.info(phases.toString())
        for(phase in phases) {
            logger.info(phase.toString())

            def id = (phase as Map<Object,Object>).get("id")
            if(id) {

                switch (id) {
                    case SETUP_PHASE_ID:
                        def steps = (phase as Map<Object,Object>).get("steps")
                        for(step in steps) {
                            def probes = (step as Map<Object,Object>).get("probes")
                            if(probes) {
                                for(probe in probes) {
                                    def testDescriptorProbe = yamlSkipMissingProperties.loadAs(yaml.dump(probe), Probe.class)

                                    testDescriptor.probes.put(testDescriptorProbe.id, testDescriptorProbe)
                                    logger.info("PROBE ${testDescriptor.probes}")
                                }
                            }
                        }
                        break

                        case EXERCISE_PHASE_ID:
                            def testDescriptorExercisePhase = yamlSkipMissingProperties.loadAs(yaml.dump((phase as Map<Object,Object>)), TestDescriptorExercisePhase.class)
                            testDescriptor.phases.put(TestDescriptorPhases.EXERCISE_PHASE, testDescriptorExercisePhase as TestDescriptorExercisePhase)
                            break

                        case VERIFICATION_PHASE_ID:
                            def testDescriptorVerificationPhase = yamlSkipMissingProperties.loadAs(yaml.dump((phase as Map<Object,Object>)), TestDescriptorVerificationPhase.class)
                            testDescriptor.phases.put(TestDescriptorPhases.VERIFICATION_PHASE, testDescriptorVerificationPhase as TestDescriptorVerificationPhase)
                            break
                        default:
                        logger.info("DEFAULT")
                        break
                }
            }
        }

        return testDescriptor
    }

    String dumpDockerFile(DockerCompose dockerCompose) {

       return  yamlSkipTags.dumpAs(dockerCompose, Tag.MAP, DumperOptions.FlowStyle.BLOCK)

    }
}