package app.model.test

class TestDescriptorVerificationPhase extends TestDescriptorPhase {

    String id = "verification"
    List<TestDescriptorVerificationPhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorVerificationPhase{uuid=${id}, steps=${steps.toString()}}"
    }
}