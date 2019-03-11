package app.model.test

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties
@ApiModel(value = "Test entity", description = "Complete data of a test")
class Test {

    @ApiModelProperty(required = false, hidden = true)
    String uuid

    List<Callback> callbacks = new ArrayList<>()

    TestDescriptor test

    Callback getCallback(Callback.CallbackTypes type) {
        if(callbacks) {
            for(callback in callbacks) {
                if(type.equalsToString(callback.name)) {
                    return callback
                }
            }
        }
        return null
    }
}
