package app.util


import app.model.test.Condition
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

@Component
@Slf4j(value = "logger")
class Validator {

    void validateConditions(Condition condition, File resultsFile){

        try{

            if ("json" == (condition.getType())){

                logger.info("Checking that ${condition.getFind()} ${condition.getCondition()} than ${condition.value} in ${resultsFile} file".toString())

                def value = findDeep(new JsonSlurper().parseText(resultsFile.getText()), condition.getFind()).toString()

                switch (condition.getCondition()){
                    case ">":
                        if (!(value.toDouble() > condition.getValue().toDouble())) {
                            throw new Exception("Validation [${value} > ${condition.getValue()}]  FAILED")
                        }
                        break
                    case "<":
                        if (!(value.toDouble() < condition.getValue().toDouble())) {
                            throw new Exception("Validation [${value} < ${condition.getValue()}]  FAILED")
                        }
                        break
                    case "!=":
                        if (value == condition.getValue()) {
                            throw new Exception("Validation [${value} = ${condition.getValue()}]  FAILED")
                        }
                        break
                    case "=":
                        if (value != condition.getValue()) {
                            throw new Exception("Validation [${value} = ${condition.getValue()}]  FAILED")
                        }
                        break
                    case "==":
                        if (value != condition.getValue()) {
                            throw new Exception("Validation [${value} = ${condition.getValue()}]  FAILED")
                        }
                        break
                    case ">=":
                        if (!(value.toDouble() >= condition.getValue().toDouble())) {
                            throw new Exception("Validation [${value} >= ${condition.getValue()}]  FAILED")
                        }
                        break
                    case "<=":
                        if (!(value.toDouble() <= condition.getValue().toDouble())) {
                            throw new Exception("Validation [${value} <= ${condition.getValue()}]  FAILED")
                        }
                        break
                    default:
                        throw new Exception("Validation FAILED. Unknown option")
                }

            } else { //txt file
                logger.info("Checking that ${condition.getFind()} is ${condition.getCondition()} in ${resultsFile} file".toString())
                switch (condition.getCondition()){
                    case "present":
                        if (!resultsFile.getText().contains(condition.getFind())) {
                            throw  new Exception("Validation [${condition.getFind()} present]  FAILED")
                        }
                        break
                    case "not present":
                        if (resultsFile.getText().contains(condition.getFind())) {
                            throw  new Exception("Validation [${condition.getFind()} not present]FAILED")
                        }
                        break
                    default:
                        throw  new Exception("Validation FAILED. Unknown option")
                }

            }

        } catch (Exception e){
            throw new Exception(e)
        }
    }

    def mapOrCollection (def it) {
        it instanceof Map || it instanceof Collection
    }

    def findDeep(def tree, String key) {
        switch (tree) {
            case Map: return tree.findResult { k, v ->
                mapOrCollection(v)
                        ? findDeep(v, key)
                        : k == key
                        ? v
                        : null
            }
            case Collection: return tree.findResult { e ->
                mapOrCollection(e)
                        ? findDeep(e, key)
                        : null
            }
            default: return null
        }
    }
}
