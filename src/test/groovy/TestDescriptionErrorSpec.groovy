import app.config.RestConfig
import app.model.test_descriptor.*
import spock.lang.Specification

class TestDescriptionErrorSpec extends Specification {

    static def mapper
    static def testDescriptorString
    static def testDescriptor

    def setupSpec() {
        testDescriptorString = getClass().getResource('/test-descriptor-error.yml').text
        mapper = new RestConfig().objectMapperYaml()
    }

    def "Test Descriptor is valid"() {

        when: testDescriptor = mapper.readValue(testDescriptorString as String, TestDescriptor.class)
        then: noExceptionThrown()
    }

    def "Setup Phase has not probes"() {

        when: def steps = (testDescriptor.getPhase(TestDescriptorPhases.SETUP_PHASE) as TestDescriptorSetupPhase).steps
        then: steps.every{step -> !step.probes}
    }

    def "Exercise Phase does not exist"() {

        when: def exercisePhase = testDescriptor.getPhase(TestDescriptorPhases.EXERCISE_PHASE) as TestDescriptorExercisePhase
        then: exercisePhase == null
    }
}