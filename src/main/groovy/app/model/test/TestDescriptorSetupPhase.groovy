package app.model.test

class TestDescriptorSetupPhase extends TestDescriptorPhase {

    String id = "setup"
    List<TestDescriptorSetupPhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorSetupPhase{uuid=${id}, steps=${steps.toString()}}"
    }
}