package app.model.test

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties
@ApiModel(value = "Callback entity", description = "Complete data of a callback descriptor")
class Callback {

    @ApiModelProperty(required = false, hidden = true)
    String uuid

    CallbackTypes name

    String path

    enum CallbackTypes {
        running("running"), cancel("cancel"), finish("finish")

        String name

        CallbackTypes(String name) {
            this.name = name
        }

        boolean equalsToString(CallbackTypes name) {
            return (name.toString().equalsIgnoreCase(this.name))
        }
    }
}