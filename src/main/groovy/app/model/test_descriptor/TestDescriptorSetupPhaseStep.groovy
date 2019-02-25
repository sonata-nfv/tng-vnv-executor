package app.model.test_descriptor

class TestDescriptorSetupPhaseStep extends TestDescriptorPhaseStep {

    String action
    List<Probe> probes

    @Override
    String toString() {
        return "TestDescriptorSetupPhaseStep{name=${name}, description=${description}, action=${action}," +
                " probes=${probes}}"
    }
}


