package app.model.test_descriptor

class TestDescriptorVerificationPhase extends TestDescriptorPhase {

    String id = "verification"
    List<TestDescriptorVerificationPhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorVerificationPhase{id=${id}, steps=${steps.toString()}}"
    }
}