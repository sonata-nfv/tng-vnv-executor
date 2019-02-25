package app.model.test_descriptor

class TestDescriptorExercisePhase extends TestDescriptorPhase {

    List<TestDescriptorExercisePhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorExercisePhase{id=${id}, steps=${steps.toString()}}"
    }
}

