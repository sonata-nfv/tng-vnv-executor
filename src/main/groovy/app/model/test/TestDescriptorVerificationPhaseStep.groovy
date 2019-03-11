package app.model.test

class TestDescriptorVerificationPhaseStep extends TestDescriptorPhaseStep {

    String step
    //List<Map<String, Object>> condition
    List<Condition> conditions

    @Override
    String toString() {
        return "TestDescriptorVerificationPhaseStep{name=${name}, description=${description}, step=${step}," +
                " condition=${condition}}"
    }
}

