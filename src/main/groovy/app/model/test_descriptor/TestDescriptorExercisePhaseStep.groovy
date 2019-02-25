package app.model.test_descriptor

class TestDescriptorExercisePhaseStep extends TestDescriptorPhaseStep {

    String run
    Number start_delay
    String entrypoint
    Number instances
    List<Map<String, String>> output
    List<String> dependency

    @Override
    String toString() {
        return "TestDescriptorExercisePhaseStep{name=${name}, description=${description}, run=${run}," +
                " instances=${instances}, start_delay=${start_delay}, entrypoint=${entrypoint}, output=${output}" +
                " dependency=${dependency}}"
    }
}
