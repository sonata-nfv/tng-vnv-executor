import app.config.GeneralConfig
import app.model.test.*
import spock.lang.Specification

class TestDescriptionSpec extends Specification {

    static def mapper
    static def testDescriptorString
    static def testDescriptor

    def setupSpec() {
        testDescriptorString = getClass().getResource('/test-descriptor-correct.json').text
        mapper = new GeneralConfig().objectMapper()
    }

    def "Test Descriptor is valid"() {

        when: testDescriptor = mapper.readValue(testDescriptorString as String, Test.class)
        then: noExceptionThrown()
    }

    def "Setup Phase exists"() {

        when: def setupPhase = testDescriptor.test.getPhase(TestDescriptorPhases.SETUP_PHASE) as TestDescriptorSetupPhase
        then: setupPhase != null
    }

    def "Setup Phase has steps"() {

        when: def setupPhase = testDescriptor.test.getPhase(TestDescriptorPhases.SETUP_PHASE) as TestDescriptorSetupPhase
        then: setupPhase.steps.size() > 0
    }

    def "Setup Phase has probes"() {

        when: def steps = (testDescriptor.test.getPhase(TestDescriptorPhases.SETUP_PHASE) as TestDescriptorSetupPhase).steps
        then: steps.any{step -> step.probes && step.probes.size() > 0}
    }

    def "Exercise Phase exists"() {

        when: def exercisePhase = testDescriptor.test.getPhase(TestDescriptorPhases.EXERCISE_PHASE) as TestDescriptorExercisePhase
        then: exercisePhase != null
    }

    def "Exercise Phase has steps"() {

        when: def exercisePhase = testDescriptor.test.getPhase(TestDescriptorPhases.EXERCISE_PHASE) as TestDescriptorExercisePhase
        then: exercisePhase.steps.size() > 0
    }

    def "Verification Phase exists"() {

        when: def verificationPhase = testDescriptor.test.getPhase(TestDescriptorPhases.VERIFICATION_PHASE) as TestDescriptorVerificationPhase
        then: verificationPhase != null
    }

    def "Verification Phase has steps"() {

        when: def verificationPhase = testDescriptor.test.getPhase(TestDescriptorPhases.VERIFICATION_PHASE) as TestDescriptorVerificationPhase
        then: verificationPhase.steps.size() > 0
    }

    def "Test Descriptor is not valid"() {

        setup:
        def testDescriptorString = getClass().getResource('/test-descriptor-error.json').text
        def mapper = new GeneralConfig().objectMapperYaml()

        when: mapper.readValue(testDescriptorString as String, TestDescriptor.class)

        then: thrown(Exception.class)
    }
}