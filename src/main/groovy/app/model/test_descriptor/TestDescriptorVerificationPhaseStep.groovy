package app.model.test_descriptor

class TestDescriptorVerificationPhaseStep extends TestDescriptorPhaseStep {

    String step
    List<Map<String, Object>> condition

    @Override
    String toString() {
        return "TestDescriptorVerificationCondition{name=${name}, description=${description}, step=${step}," +
                " condition=${condition}}"
    }
}

