package app.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "id",
        defaultImpl = TestDescriptorPhase.class,
        visible = true)

@JsonSubTypes([
        @JsonSubTypes.Type(name = "setup", value = TestDescriptorSetupPhase.class),
        @JsonSubTypes.Type(name = "exercise", value = TestDescriptorExercisePhase.class),
        @JsonSubTypes.Type(name = "verification", value = TestDescriptorVerificationPhase.class)
])

class TestDescriptorPhase {
    String id
    List<TestDescriptorPhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorPhase{id=${id}, steps=${steps.toString()}}"
    }
}

class TestDescriptorPhaseStep {
    String name
    String description

    @Override
    String toString() {
        return "TestDescriptorPhaseStep{name=${name}, description=${description}}"
    }
}

class TestDescriptorSetupPhase extends TestDescriptorPhase {

    List<TestDescriptorSetupPhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorSetupPhase{id=${id}, steps=${steps.toString()}}"
    }
}

class TestDescriptorSetupPhaseStep extends TestDescriptorPhaseStep {

    String action
    List<Probe> probes

    @Override
    String toString() {
        return "TestDescriptorSetupPhaseStep{name=${name}, description=${description}, action=${action}," +
                " probes=${probes}}"
    }
}

class TestDescriptorExercisePhase extends TestDescriptorPhase {

    List<TestDescriptorExercisePhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorExercisePhase{id=${id}, steps=${steps.toString()}}"
    }
}

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

class TestDescriptorVerificationPhase extends TestDescriptorPhase {
    List<TestDescriptorVerificationPhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorVerificationPhase{id=${id}, steps=${steps.toString()}}"
    }
}


class TestDescriptorVerificationPhaseStep extends TestDescriptorPhaseStep {

    String step
    List<TestDescriptorVerificationCondition> condition

    @Override
    String toString() {
        return "TestDescriptorVerificationCondition{name=${name}, description=${description}, step=${step}," +
                " condition=${condition}}"
    }
}


class TestDescriptorVerificationCondition {
    Map<String, Object> parser
}


