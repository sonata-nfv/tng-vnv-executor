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

package app.controller

import app.database.TestExecution
import app.database.TestExecutionRepository
import app.model.callback.Response
import app.model.docker_compose.Service
import app.model.resultsRepo.PostTestSuiteResponse
import app.model.resultsRepo.Result
import app.model.test.Callback
import app.model.test.Test
import app.model.test.TestDescriptor
import app.model.test.TestDescriptorExercisePhaseStep
import app.model.test.TestDescriptorPhases
import app.model.test.TestDescriptorVerificationPhaseStep
import app.util.Converter
import app.util.FileUtils
import app.util.ResponseUtils
import app.util.Validator
import app.model.docker_compose.DockerCompose
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import groovy.util.logging.Slf4j
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.core.task.TaskExecutor
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate

import java.text.Format
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset

@RestController
@Api
@Slf4j(value = "logger")
@PropertySource("classpath:application.properties")
class ExecutorController {

    @Autowired
    Converter converter

    @Autowired
    FileUtils fileUtils

    @Autowired
    TaskExecutor taskExecutor

    @Autowired
    ResponseUtils responseUtils

    @Autowired
    Validator validator

    @Autowired
    TestExecutionRepository testExecutionRepository

    @Value('${CALLBACK_SERVER_NAME}')
    String CALLBACK_SERVER_NAME

    @Value('${CALLBACK_SERVER_PORT}')
    String CALLBACK_SERVER_PORT

    @Value('${RESULTS_REPO_NAME}')
    String RESULTS_REPO_NAME

    @Value('${RESULTS_REPO_PORT}')
    String RESULTS_REPO_PORT

    @Value('${CALLBACKS}')
    String CALLBACKS

    @Value('${DELETE_FINISHED_TEST}')
    String DELETE_FINISHED_TEST

    @RequestMapping(method = RequestMethod.POST,
            path = "/api/v1/test-executions",
            consumes = "application/json",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value = "Start a test", notes = "Receive a test descriptor, check its validity and create the docker-compose and directories, starting the test")
    @ApiResponses([
            @ApiResponse(code = 202, message = "Test Descriptor is valid, test is being executed"),
            @ApiResponse(code = 400, message = "Test Descriptor is not valid"),
            @ApiResponse(code = 500, message = "There was a problem during the test building")
    ])
    ResponseEntity<String> testExecutionRequest(@RequestBody Test test) {

        //get TD
        TestDescriptor testDescriptor = test.getTest()

        // ID is generated, never will already exist in DB
        testDescriptor.uuid = UUID.randomUUID().toString()
        while (testExecutionRepository.findById(testDescriptor.uuid).isPresent()) {
            testDescriptor.uuid = UUID.randomUUID().toString()
        }

        def dockerCompose
        try {
            dockerCompose = converter.getDockerCompose(testDescriptor)

        } catch (Exception e) {
            logger.error("Error creating the docker-compose: ${e.getMessage()}".toString(), e)
            return responseUtils.getErrorResponseEntity(
                    HttpStatus.BAD_REQUEST,
                    "Error creating the docker-compose: ${e.getMessage()}".toString(),
                    e.getCause())
        }

        try {
            fileUtils.createTestDirectories(testDescriptor.uuid, new ArrayList<Service>(dockerCompose.services.values()))
            def dockerComposeString = converter.serializeDockerCompose(dockerCompose)
            fileUtils.createDockerComposeFile(testDescriptor.uuid, dockerComposeString)

        } catch (Exception e) {
            logger.error("Error storing the docker-compose file: ${e.getMessage()}".toString(), e)
            return responseUtils.getErrorResponseEntity(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error storing the docker-compose file: ${e.getMessage()}".toString(),
                    e.getCause())
        }

        try {
            def testExecution = new TestExecution(testDescriptor.uuid, converter.serializeDockerCompose(dockerCompose))
            testExecutionRepository.save(testExecution)
        } catch (Exception e) {
            logger.error("Error storing the test exercise in DB: ${e.getMessage()}".toString(), e)
            return responseUtils.getErrorResponseEntity(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error storing the test exercise in DB: ${e.getMessage()}".toString(),
                    e.getCause())
        }

        executeTest(testDescriptor.uuid, dockerCompose, test)

        return responseUtils.getResponseEntity(
                HttpStatus.ACCEPTED,
                "test-uuid",
                testDescriptor.uuid)
    }

    @RequestMapping(method = RequestMethod.DELETE,
            path = "/api/v1/test-executions/{test_id}/cancel",
            consumes = "application/json",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Cancel an executing test", notes = "Receive a test uuid and callbacks, check if it is running and cancel it if possible")
    @ApiResponses([
            @ApiResponse(code = 200, message = "Test has been cancelled"),
            @ApiResponse(code = 404, message = "The test with the provided uuid was not found"),
            @ApiResponse(code = 500, message = "There was a problem during the test cancelling")
    ])
    ResponseEntity<String> testCancelExecutionRequest(@PathVariable("test_id") String testId, @RequestBody Test callbacks) {

        def testExecutionOpt = testExecutionRepository.findById(testId.toString())

        if (testExecutionOpt.isPresent()) {

            cancelTest(testId, callbacks)

            return responseUtils.getResponseEntity(
                    HttpStatus.ACCEPTED,
                    "test-uuid", testId)
        }

        return responseUtils.getErrorResponseEntity(
                HttpStatus.NOT_FOUND,
                "Test not found",
                null)
    }

    private void executeTest(final String testId, DockerCompose dockerCompose, Test test) {
        taskExecutor.execute(new Runnable() {
            @Override
            void run() {

                def callback
                def callbackBaseUrl = "http://${CALLBACK_SERVER_NAME}:${CALLBACK_SERVER_PORT}"
                Process process
                def testExecution = testExecutionRepository.findById(testId).orElse(null) as TestExecution

                //Execute docker-compose up command

                logger.info("Callbacks to Curator are ${CALLBACKS}")

                try {
                    process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up -d")
                    logger.info("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up -d")
                    process.waitForProcessOutput()
                    //logger.info("> ${process}")
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {

                    if (testExecution) {
                        testExecution.state = TestExecution.TestState.ERROR
                        testExecutionRepository.save(testExecution)
                    }

                    if (CALLBACKS.toUpperCase()=="ENABLED"){
                        callback = test.getCallback(Callback.CallbackTypes.cancel)
                        def message = "Error executing docker-compose up command: ${e.toString()}"
                        logger.error(message)
                        Response response = new Response()
                        response.setTest_uuid(testId)
                        response.setStatus("ERROR")
                        response.setMessage(message)
                        postCallback("${callbackBaseUrl}${callback.getPath()}", response)
                    }
                    return
                }

                //Update Database with running status
                if (testExecution) {
                    testExecution.state = TestExecution.TestState.RUNNING
                    testExecutionRepository.save(testExecution)
                }

                if (CALLBACKS.toUpperCase()=="ENABLED") {
                    callback = test.getCallback(Callback.CallbackTypes.running)
                    Response response = new Response()
                    response.setTest_uuid(testId)
                    response.setStatus("RUNNING")
                    postCallback("${callbackBaseUrl}${callback.getPath()}", response)
                }

                //Wait for completion
                try {
                    for (service in dockerCompose.services) {
                        logger.info("waiting for ${testId}-${service.value.getName()}")
                        logger.info("sh /executor/bash_scripts/wait_for.sh \"${service.value.getName()}\" \"${testId}\" \"/executor/compose_files/${testId}-docker-compose.yml\"")
                        process = Runtime.getRuntime().exec("sh /executor/bash_scripts/wait_for.sh ${service.value.getName()} ${testId} /executor/compose_files/${testId}-docker-compose.yml")
                        process.waitFor()
                        logger.info("> ${process}")
                        if (!process.toString().contains("exitValue=0")) {
                            throw new Exception("FAILED")
                        }
                    }
                } catch (Exception e) {

                    logger.info("Probe FAILED: ${process.toString()}")

                    if (testExecution) {
                        testExecution.state = TestExecution.TestState.ERROR
                        testExecutionRepository.save(testExecution)
                    }

                    if (CALLBACKS.toUpperCase()=="ENABLED") {
                        callback = test.getCallback(Callback.CallbackTypes.cancel)
                        def message = "Error waiting for test completion: ${e.toString()}"
                        logger.error(message)
                        Response response = new Response()
                        response = new Response()
                        response.setTest_uuid(testId)
                        response.setStatus("ERROR")
                        response.setMessage(message)
                        postCallback("${callbackBaseUrl}${callback.getPath()}", response)
                    }
                    return
                }

                //Execute docker-compose down command
                try {
                    process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    logger.info("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    process.waitForProcessOutput()
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    logger.error("Error executing docker-compose down command. Sending message to ${callback.path}")
                    return
                }

                //Launch Validation/Verification
                logger.info("${testId} SUCCESSFULLY execution. Validation Pending...")
                executeValidation(testId, test)
            }
        })
    }

    private void cancelTest(final String testId, Test callbacks) {
        taskExecutor.execute(new Runnable() {
            @Override
            void run() {

                def callback = callbacks.getCallback(Callback.CallbackTypes.cancel)
                def testExecutionOpt = testExecutionRepository.findById(testId.toString())
                def callbackBaseUrl = "http://${CALLBACK_SERVER_NAME}:${CALLBACK_SERVER_PORT}"

                //docker-compose down
                try {
                    def process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    logger.info("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    process.waitForProcessOutput()
                    //logger.info("> ${process}")
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    if (CALLBACKS.toUpperCase()=="ENABLED") {
                        def message = "Error executing docker-compose down command: ${e.toString()}"
                        logger.error(message)
                        Response response = new Response()
                        response.setTest_uuid(testId)
                        response.setStatus("ERROR")
                        response.setMessage(message)
                        postCallback("${callbackBaseUrl}${callback.getPath()}", response)
                    }
                    return
                }

                //delete folders
                try {
                    def process = Runtime.getRuntime().exec("rm -rf /executor/tests/${testId}")
                    logger.info("Executing: rm -rf /executor/tests/${testId}")
                    process.waitForProcessOutput()
                    //logger.info("> ${process}")
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }

                    process = Runtime.getRuntime().exec("rm -rf /executor/compose_files/${testId}-docker-compose.yml")
                    logger.info("Executing: rm -rf /executor/compose_files/${testId}-docker-compose.yml")
                    process.waitForProcessOutput()
                    //logger.info("> ${process}")
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    logger.error("Error deleting folders")
                }

                // Update Database
                def testExecution = testExecutionOpt.get()
                testExecution.state = TestExecution.TestState.CANCELLED
                testExecutionRepository.save(testExecution)


                //Callback
                if (CALLBACKS.toUpperCase()=="ENABLED") {
                    Response response = new Response()
                    response.setTest_uuid(testId)
                    response.setStatus("CANCELLED")
                    postCallback("${callbackBaseUrl}${callback.getPath()}", response)
                }
            }
        })
    }

    private void executeValidation(final String testId, Test test) {
        taskExecutor.execute(new Runnable() {
            @Override
            void run() {

                def callback
                def service
                def resultsFolder

                def repoUrl = "http://${RESULTS_REPO_NAME}:${RESULTS_REPO_PORT}/trr/test-suite-results"
                def callbackBaseUrl = "http://${CALLBACK_SERVER_NAME}:${CALLBACK_SERVER_PORT}"
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
                result.updated_at = new Date()
                result.uuid=test.getTest().getTest_descriptor_uuid()
                result.test_uuid=test.getTest().getTest_descriptor_uuid()

                def exercisePhaseSteps = (List<TestDescriptorExercisePhaseStep>)test.getTest().getPhase(TestDescriptorPhases.EXERCISE_PHASE).getSteps()

                resultsFolder = new File("/executor/tests/${testId}/output")

                List<String> results
                List<String> details
                List<Map<String, Object>> listOfMapResults = new ArrayList<>()
                List<Map<String, Object>> listOfMapDetails = new ArrayList<>()

                for (probe in resultsFolder.listFiles()){
                    //logger.info("<<<<<< Probe: ${probe.name}")
                    results = new ArrayList<>()
                    details = new ArrayList<>()
                    for (instance in probe.listFiles()){
                        //logger.info("-- instance: ${instance.name}")
                        for (file in instance.listFiles()){
                            for (step in exercisePhaseSteps){
                                if (step.run == probe.name){
                                    // results file
                                    //logger.info("detected file: ${file.name}")
                                    def resultsFileName
                                    for (output in step.getOutput()){
                                        resultsFileName = output.get("results")
                                        //logger.info("the results file name is: ${resultsFileName}")
                                        break
                                    }
                                    if (file.name == resultsFileName){
                                        //logger.info("adding ${file.name} to results")
                                        results.add(file.getText())
                                    } else { //details file
                                        //logger.info("adding ${file.name} to details")
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
                                        validator.validateConditions(condition, file)
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
                                postTestResult(repoUrl, result)
                            } catch (Exception ex){
                                if (CALLBACKS.toUpperCase()=="ENABLED") {
                                    callback = test.getCallback(Callback.CallbackTypes.cancel)
                                    def message = "Error saving results in repo ${testId}-${service}: ${ex.toString()}"
                                    logger.error(message)
                                    Response response = new Response()
                                    response.setTest_uuid(testId)
                                    response.setStatus("ERROR")
                                    response.setMessage(message)
                                    postCallback("${callbackBaseUrl}${callback.getPath()}", response)
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
                                postCallback("${callbackBaseUrl}${callback.getPath()}", response)
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
                    resultsUuid = postTestResult(repoUrl, result)
                } catch (Exception e){
                    if (CALLBACKS.toUpperCase()=="ENABLED") {
                        callback = test.getCallback(Callback.CallbackTypes.cancel)
                        def message = "Error saving results in repo ${testId}-${service}: ${e.toString()}"
                        logger.error(message)
                        Response response = new Response()
                        response.setTest_uuid(testId)
                        response.setStatus("ERROR")
                        response.setMessage(message)
                        postCallback("${callbackBaseUrl}${callback.getPath()}", response)
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
                    postCallback("${callbackBaseUrl}${callback.getPath()}", response)
                }

                //tests results repo

                //delete folders
                try {
                    if(DELETE_FINISHED_TEST.toUpperCase()!="DISABLED"){
                        def process = Runtime.getRuntime().exec("rm -rf /executor/tests/${testId}")
                        logger.info("Executing: rm -rf /executor/tests/${testId}")
                        process.waitForProcessOutput()
                        //logger.info("> ${process}")
                        if (!process.toString().contains("exitValue=0")) {
                            throw new Exception("FAILED")
                        }

                        process = Runtime.getRuntime().exec("rm -rf /executor/compose_files/${testId}-docker-compose.yml")
                        logger.info("Executing: rm -rf /executor/compose_files/${testId}-docker-compose.yml")
                        process.waitForProcessOutput()
                        //logger.info("> ${process}")
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

    private void postCallback(String url, Object payload) {

        RestTemplate restTemplate = new RestTemplate()

        try {

            logger.info("Callbacks ${CALLBACKS}. Sending callback to ${url}")

            URI uri = new URI(url)

            HttpHeaders headers = new HttpHeaders()
            headers.setContentType(MediaType.APPLICATION_JSON)

            HttpEntity<Response> request = new HttpEntity<>(payload, headers)

            def response = restTemplate.postForEntity(uri, request, String.class)

        } catch (Exception e) {
            logger.error(e)
        }
    }

    private String postTestResult(String url, Object payload) {

        RestTemplate restTemplate = new RestTemplate()

        try {

            logger.info("Sending results to ${url}:")
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(payload);
            logger.info(json)

            URI uri = new URI(url)

            HttpHeaders headers = new HttpHeaders()
            headers.setContentType(MediaType.APPLICATION_JSON)

            HttpEntity<Response> request = new HttpEntity<>(payload, headers)

            ResponseEntity response = restTemplate.postForEntity(uri, request, PostTestSuiteResponse.class)

            return response.getBody().getUuid()

        } catch (Exception e) {
            logger.error(e)
        }
    }

}
