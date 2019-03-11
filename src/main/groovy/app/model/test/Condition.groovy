package app.model.test

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties
@ApiModel(value = "Condition entity", description = "Complete data of a parser")
class Condition {

    @ApiModelProperty(required = false)
    String type

    String name
    String file
    String find
    String value
    String verdict
    String condition

    @Override
    String toString() {
        return "Condition{type=${type}, name=${name}, file=${file}, find=${find}, value=${value}, verdict=${verdict}, condition=${condition}}"
    }
}
