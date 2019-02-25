package app.model.test_descriptor

class TestDescriptorVerificationPhase extends TestDescriptorPhase {
    List<TestDescriptorVerificationPhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorVerificationPhase{id=${id}, steps=${steps.toString()}}"
    }
}