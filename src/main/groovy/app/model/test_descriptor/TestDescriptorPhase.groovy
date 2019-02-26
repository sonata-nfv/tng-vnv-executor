package app.model.test_descriptor

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.annotations.ApiModel

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

@ApiModel(subTypes = [TestDescriptorExercisePhase.class, TestDescriptorVerificationPhase.class, TestDescriptorSetupPhase.class])
class TestDescriptorPhase {
    String id
    List<TestDescriptorPhaseStep> steps

    @Override
    String toString() {
        return "TestDescriptorPhase{id=${id}, steps=${steps.toString()}}"
    }
}




