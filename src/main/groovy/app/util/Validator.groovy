/*
 * Copyright (c) 2015 SONATA-NFV, 2017 5GTANGO [, ANY ADDITIONAL AFFILIATION]
 * ALL RIGHTS RESERVED.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Neither the name of the SONATA-NFV, 5GTANGO [, ANY ADDITIONAL AFFILIATION]
 * nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * This work has been performed in the framework of the SONATA project,
 * funded by the European Commission under Grant number 671517 through
 * the Horizon 2020 and 5G-PPP programmes. The authors would like to
 * acknowledge the contributions of their colleagues of the SONATA
 * partner consortium (www.sonata-nfv.eu).
 *
 * This work has been performed in the framework of the 5GTANGO project,
 * funded by the European Commission under Grant number 761493 through
 * the Horizon 2020 and 5G-PPP programmes. The authors would like to
 * acknowledge the contributions of their colleagues of the 5GTANGO
 * partner consortium (www.5gtango.eu).
 */

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

                logger.info("Checking that ${condition.getFind()} ${condition.getCondition()} than ${condition.getValue()} in ${resultsFile} file".toString())

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
                            throw  new Exception("Validation [${condition.getFind()} present] FAILED")
                        }
                        break
                    case "not present":
                        if (resultsFile.getText().contains(condition.getFind())) {
                            throw  new Exception("Validation [${condition.getFind()} not present] FAILED")
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
