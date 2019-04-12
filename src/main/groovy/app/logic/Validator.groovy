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

package app.logic

import app.database.TestExecution
import app.database.TestExecutionRepository
import app.model.callback.Response
import app.model.resultsRepo.Result
import app.model.test.Callback
import app.model.test.Condition
import app.model.test.Test
import app.model.test.TestDescriptorExercisePhaseStep
import app.model.test.TestDescriptorPhases
import app.model.test.TestDescriptorVerificationPhaseStep
import app.util.ResponseUtils
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

import java.time.Instant
import java.time.ZoneOffset

@Component
@Slf4j(value = "logger")
@PropertySource("classpath:application.properties")
class Validator {

    @Autowired
    TaskExecutor taskExecutor

    @Autowired
    TestExecutionRepository testExecutionRepository

    @Value('${DELETE_FINISHED_TEST}')
    String DELETE_FINISHED_TEST

    @Value('${CALLBACKS}')
    String CALLBACKS

    @Value('${RESULTS_REPO_URL}')
    String RESULTS_REPO_URLT

    String repoUrl = "${RESULTS_REPO_URL}/trr/test-suite-results"

    void executeValidation(final String testId, Test test) {
        taskExecutor.execute(new Runnable() {
            @Override
            void run() {

                def callback
                def service
                def resultsFolder
                def testExecution = testExecutionRepository.findById(testId).orElse(null) as TestExecution

                Instant instant

                //generating result with all files

                Result result = new Result()
                instant = testExecution.created.toInstant()
                result.started_at = instant.atOffset(ZoneOffset.UTC).toString()
                result.status = "EXECUTED"
                result.instance_uuid=test.getTest().getService_instance_uuid()
                result.package_id=test.getTest().getPackage_descriptor_uuid()
                result.service_uuid=test.getTest().getNetwork_service_descriptor_uuid()
                Date updatedAt = new Date()
                result.updated_at = updatedAt.toInstant().atOffset(ZoneOffset.UTC).toString()
                result.test_uuid=test.getTest().getTest_descriptor_uuid()
                result.uuid=test.getTest().getTest_uuid()

                def exercisePhaseSteps = (List<TestDescriptorExercisePhaseStep>)test.getTest().getPhase(TestDescriptorPhases.EXERCISE_PHASE).getSteps()

                resultsFolder = new File("/executor/tests/${testId}/output")

                List<String> results
                List<String> details
                List<Map<String, Object>> listOfMapResults = new ArrayList<>()
                List<Map<String, Object>> listOfMapDetails = new ArrayList<>()

                for (probe in resultsFolder.listFiles()){
                    results = new ArrayList<>()
                    details = new ArrayList<>()
                    for (instance in probe.listFiles()){
                        for (file in instance.listFiles()){
                            for (step in exercisePhaseSteps){
                                if (step.run == probe.name){
                                    // results file
                                    def resultsFileName
                                    for (output in step.getOutput()){
                                        resultsFileName = output.get("results")
                                        break
                                    }
                                    if (file.name == resultsFileName){
                                        results.add(file.getText())
                                    } else { //details file
                                        details.add(file.getText())
                                    }
                                    break
                                }
                            }
                        }
                    }
                    Map<String, Object> map = new HashMap<String, Object>()
                    map.put(probe.name, results)
                    listOfMapResults.add(map)
                    map = new HashMap<String, Object>()
                    map.put(probe.name, details)
                    listOfMapDetails.add(map)
                }
                result.details = listOfMapDetails
                result.results = listOfMapResults

                logger.info("-- Starting validation")

                for (step in (List<TestDescriptorVerificationPhaseStep>) test.getTest().getPhase(TestDescriptorPhases.VERIFICATION_PHASE).getSteps()) {
                    for (exerciseStep in exercisePhaseSteps) {
                        if (exerciseStep.getName().equals(step.getStep())) {
                            service = exerciseStep.getRun()
                            break
                        }
                    }
                    logger.info("Validating ${testId}-${service}")

                    for (condition in step.getConditions()) {

                        try {
                            resultsFolder = new File("/executor/tests/${testId}/results/${service}")

                            for (instance in resultsFolder.listFiles()){

                                for (file in instance.listFiles()) {
                                    if (file.getName() == (condition.getFile())) {
                                        validateConditions(condition, file)
                                    }
                                }

                            }
                            logger.info("${testId}-${service}-${condition.getName()} = ${condition.getVerdict()}")

                        } catch (Exception e) {

                            if (testExecution) {
                                testExecution.state = TestExecution.TestState.ERROR
                                testExecution.lastModifiedDate = new Date()
                                testExecutionRepository.save(testExecution)
                            }

                            //Saving result in repo as ERROR
                            try{
                                instant = testExecution.lastModifiedDate.toInstant()
                                result.ended_at=instant.atOffset(ZoneOffset.UTC).toString()
                                result.status="ERROR"
                                ResponseUtils.postTestResult(repoUrl, result)
                            } catch (Exception ex){
                                if (CALLBACKS.toUpperCase()=="ENABLED") {
                                    callback = test.getCallback(Callback.CallbackTypes.cancel)
                                    def message = "Error saving results in repo ${testId}-${service}: ${ex.toString()}"
                                    logger.error(message)
                                    Response response = new Response()
                                    response.setTest_uuid(testId)
                                    response.setStatus("ERROR")
                                    response.setMessage(message)
                                    ResponseUtils.postCallback("${callback.getPath().replace("<test_uuid>",testId)}", response)
                                }
                            }

                            if (CALLBACKS.toUpperCase()=="ENABLED") {
                                callback = test.getCallback(Callback.CallbackTypes.cancel)
                                def message = "Error validating ${testId}-${service}: ${e.toString()}"
                                logger.error(message)
                                Response response = new Response()
                                response.setTest_uuid(testId)
                                response.setStatus("ERROR")
                                response.setMessage(message)
                                ResponseUtils.postCallback("${callback.getPath().replace("<test_uuid>",testId)}", response)
                            }
                        }
                    }
                }
                logger.info("validation FINISHED")

                //Update Database with status completed

                if (testExecution) {
                    testExecution.state = TestExecution.TestState.COMPLETED
                    testExecution.lastModifiedDate = new Date()
                    testExecutionRepository.save(testExecution)
                }

                //Update Tests results in Tests Results Repository
                def resultsUuid
                try{
                    result.status="PASSED"
                    instant = testExecution.lastModifiedDate.toInstant()
                    result.ended_at=instant.atOffset(ZoneOffset.UTC).toString()
                    resultsUuid = ResponseUtils.postTestResult(repoUrl, result)
                } catch (Exception e){
                    if (CALLBACKS.toUpperCase()=="ENABLED") {
                        callback = test.getCallback(Callback.CallbackTypes.cancel)
                        def message = "Error saving results in repo ${testId}-${service}: ${e.toString()}"
                        logger.error(message)
                        Response response = new Response()
                        response.setTest_uuid(testId)
                        response.setStatus("ERROR")
                        response.setMessage(message)
                        ResponseUtils.postCallback("${callback.getPath().replace("<test_uuid>",testId)}", response)
                        return
                    }
                }

                //Callback
                if (CALLBACKS.toUpperCase()!="DISABLED") {
                    callback = test.getCallback(Callback.CallbackTypes.finish)
                    Response response = new Response()
                    response.setTest_uuid(testId)
                    response.setStatus("COMPLETED")
                    response.setResults_uuid(resultsUuid)
                    ResponseUtils.postCallback("${callback.getPath().replace("<test_uuid>",testId)}", response)
                }

                //tests results repo

                //delete folders
                try {
                    if(DELETE_FINISHED_TEST.toUpperCase()!="DISABLED"){
                        def process = Runtime.getRuntime().exec("rm -rf /executor/tests/${testId}")
                        logger.info("Executing: rm -rf /executor/tests/${testId}")
                        process.waitForProcessOutput()

                        if (!process.toString().contains("exitValue=0")) {
                            throw new Exception("FAILED")
                        }

                        process = Runtime.getRuntime().exec("rm -rf /executor/compose_files/${testId}-docker-compose.yml")
                        logger.info("Executing: rm -rf /executor/compose_files/${testId}-docker-compose.yml")
                        process.waitForProcessOutput()

                        if (!process.toString().contains("exitValue=0")) {
                            throw new Exception("FAILED")
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error deleting compose file")
                }
            }
        })
    }

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
