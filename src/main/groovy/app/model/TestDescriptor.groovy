package app.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties
class TestDescriptor {

    String id
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
