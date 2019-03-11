package app.model.test

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties
@ApiModel(value = "TestDescriptor entity", description = "Complete data of a test descriptor")
class TestDescriptor {

    @ApiModelProperty(required = false, hidden = true)
    String uuid

    List<TestDescriptorPhase> phases = new ArrayList<>()

    TestDescriptorPhase getPhase(TestDescriptorPhases type) {
        if(phases) {
            for(phase in phases) {
                if(type.equalsToString(phase.id)) {
                    return phase
                }
            }

        }
        return null
    }
}
