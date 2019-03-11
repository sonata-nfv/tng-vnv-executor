package app.model.test

class TestDescriptorExercisePhase extends TestDescriptorPhase {

    String id = "exercise"
    List<TestDescriptorExercisePhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorExercisePhase{uuid=${id}, steps=${steps.toString()}}"
    }
}

