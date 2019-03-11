package app.model.test

class TestDescriptorExercisePhaseStep extends TestDescriptorPhaseStep {

    String run
    Number start_delay
    String entrypoint
    String command
    Number instances
    List<Map<String, String>> output
    List<String> dependencies

    @Override
    String toString() {
        return "TestDescriptorExercisePhaseStep{name=${name}, description=${description}, run=${run}," +
                " instances=${instances}, start_delay=${start_delay}, entrypoint=${entrypoint}, command=${command}, output=${output}" +
                " dependencies=${dependencies}}"
    }
}
