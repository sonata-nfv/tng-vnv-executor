package com.optaresolutions.executor.model


class TestDescriptorPhase {
    String id
    List<TestDescriptorPhaseStep> steps
}

class TestDescriptorExercisePhase extends TestDescriptorPhase {

    List<TestDescriptorExercisePhaseStep> steps

    TestDescriptorExercisePhase() {}

    TestDescriptorExercisePhase(List<TestDescriptorExercisePhaseStep> steps) {
        this.steps = steps
    }
}

class TestDescriptorVerificationPhase extends TestDescriptorPhase {

    List<TestDescriptorVerificationPhaseStep> steps

    TestDescriptorVerificationPhase() {}

    TestDescriptorVerificationPhase(List<TestDescriptorVerificationPhaseStep> steps) {
        this.steps = steps
    }
}

class TestDescriptorPhaseStep {
    String name
    String description
}

class TestDescriptorExercisePhaseStep extends TestDescriptorPhaseStep {

    String run
    int startDelay
    int instances
    List<Tuple<String>> output
    List<String> dependency
}



class TestDescriptorVerificationPhaseStep extends TestDescriptorPhaseStep {

    String step
    List<TestDescriptorVerificationCondition> condition

}


class TestDescriptorVerificationCondition {
    Map<String, Object> parser
}


