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
import app.model.docker_compose.Service
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
import groovy.util.logging.Slf4j
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.TaskExecutor
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Api
@Slf4j(value = "logger")
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
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Cancel an executing test", notes = "Receive a test uuid, check if it is running and cancel it if possible")
    @ApiResponses([
            @ApiResponse(code = 200, message = "Test has been cancelled"),
            @ApiResponse(code = 404, message = "The test with the provided uuid was not found"),
            @ApiResponse(code = 500, message = "There was a problem during the test cancelling")
    ])
    ResponseEntity<String> testExecutionCancel(@PathVariable("test_id") String testId) {

        //TODO: something more is need to be done?

        def testExecutionOpt = testExecutionRepository.findById(testId.toString())
        if (testExecutionOpt.isPresent()) {

            try {
                //docker-compose down
                try {
                    def process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    logger.info("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    process.waitForProcessOutput()
                    logger.info("> ${process}")
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    //send callback message
                    callback = test.getCallback(Callback.CallbackTypes.cancel)
                    logger.error("Error executing docker-compose command. Sending message to ${callback.path}")
                    return
                }

                //delete folders
                /*try {
                    def process = Runtime.getRuntime().exec("rm -rf /${testId}")
                    logger.info("Executing: rm -rf /${testId}")
                    process.waitForProcessOutput()
                    logger.info("> ${process}")
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    logger.error("Error deleting folders")
                }*/

                // Update Database
                def testExecution = testExecutionOpt.get()
                testExecution.state = TestExecution.TestState.CANCELLED
                testExecutionRepository.save(testExecution)

                return responseUtils.getResponseEntity(
                        HttpStatus.OK,
                        "test-state",
                        TestExecution.TestState.CANCELLED)

            } catch (Exception e) {
                logger.error("Error cancelling the test execution ${testId}: ${e.getMessage()}".toString(), e)
                return responseUtils.getErrorResponseEntity(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error cancelling the test execution ${testId}: ${e.getMessage()}".toString(),
                        e.getCause())
            }
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
                Process process

                //Execute docker-compose up command

                try {
                    process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up -d")
                    logger.info("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up -d")
                    process.waitForProcessOutput()
                    logger.info("> ${process}")
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    //send callback message
                    callback = test.getCallback(Callback.CallbackTypes.cancel)
                    logger.error("Error executing docker-compose up command. Sending message to ${callback.path}. ${e.toString()}")
                    return
                }

                //Update Database with running status
                def testExecution = testExecutionRepository.findById(testId).orElse(null) as TestExecution
                if (testExecution) {
                    testExecution.state = TestExecution.TestState.RUNNING
                    testExecutionRepository.save(testExecution)
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
                    logger.error("Error waiting for test completion: ${e.getMessage()}".toString(), e)
                    //send callback message
                    return
                }


                //Update Tests results in Tests Results Repository
                //TODO

                //Update Database with status completed/error

                testExecution = testExecutionRepository.findById(testId).orElse(null) as TestExecution
                if (testExecution) {
                    testExecution.state = TestExecution.TestState.COMPLETED
                    testExecutionRepository.save(testExecution)
                }

                //Execute docker-compose down command

                /*try {
                    process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    logger.info("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    process.waitForProcessOutput()
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    callback = test.getCallback(Callback.CallbackTypes.cancel)
                    logger.error("Error executing docker-compose down command. Sending message to ${callback.path}")
                    return
                }*/

                //Launch Validation/Verification
                logger.info("${testId} SUCCESSFULLY execution. Validation Pending...")
                executeValidation(testId, test)
            }
        })
    }

    private void executeValidation(final String testId, Test test) {
        taskExecutor.execute(new Runnable() {
            @Override
            void run() {

                logger.info("-- Starting validation")
                def callback
                def service
                def resultsFolder

                for(step in (List<TestDescriptorVerificationPhaseStep>)test.getTest().getPhase(TestDescriptorPhases.VERIFICATION_PHASE).getSteps()) {
                    for(exerciseStep in (List<TestDescriptorExercisePhaseStep>)test.getTest().getPhase(TestDescriptorPhases.EXERCISE_PHASE).getSteps()) {
                        if (exerciseStep.getName().equals(step.getStep())) {
                            service = exerciseStep.getRun()
                            break
                        }
                    }
                    logger.info("Validating ${testId}-${service}")

                    for (condition in step.getConditions()){

                        try {
                            resultsFolder = new File("/executor/tests/${testId}/results/${service}")

                            for (file in resultsFolder.listFiles()){
                                if (file.getName().contains(condition.getFile())){
                                    validator.validateConditions(condition, file)
                                }
                            }

                            logger.info("${testId}-${service}-${condition.getName()} = ${condition.getVerdict()}")

                        } catch (Exception e){
                            callback = test.getCallback(Callback.CallbackTypes.cancel)
                            logger.error("Error validating ${testId}-${service}. Sending message to ${callback.path}")
                            logger.error(e.toString())
                        }
                    }
                }
                logger.info("validation FINISHED")
            }
        })
    }
}