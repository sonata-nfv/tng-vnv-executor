package app.model.test_descriptor

class TestDescriptorSetupPhase extends TestDescriptorPhase {

    List<TestDescriptorSetupPhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorSetupPhase{id=${id}, steps=${steps.toString()}}"
    }
}