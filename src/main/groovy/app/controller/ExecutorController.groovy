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
import app.model.test.Test
import app.model.test.TestDescriptor
import app.logic.Converter
import app.logic.Executor
import app.util.FileUtils
import app.util.ResponseUtils
import com.fasterxml.jackson.databind.ObjectMapper
import app.util.TangoLogger
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.PropertySource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Api
@PropertySource("classpath:application.properties")
class ExecutorController {

    @Autowired
    Converter converter

    @Autowired
    FileUtils fileUtils

    @Autowired
    ResponseUtils responseUtils

    @Autowired
    Executor executor

    @Autowired
    TestExecutionRepository testExecutionRepository

    @Autowired
    ObjectMapper mapper

    //Tango logger
    def tangoLogger = new TangoLogger()
    String tangoLoggerType = null;
    String tangoLoggerOperation = null;
    String tangoLoggerMessage = null;
    String tangoLoggerStatus = null;

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

        if (!testDescriptor.getTest_uuid()) {
            // ID is generated, never will already exist in DB
            testDescriptor.test_uuid = UUID.randomUUID().toString()

            while (testExecutionRepository.findById(testDescriptor.test_uuid).isPresent()) {
                testDescriptor.test_uuid = UUID.randomUUID().toString()
            }
        } else {
            while (testExecutionRepository.findById(testDescriptor.getTest_uuid()).isPresent()) {
                testDescriptor.test_uuid = UUID.randomUUID().toString()
            }
        }

        def message = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(test)
        tangoLoggerType = "I";
        tangoLoggerOperation = "ExecutorController.testExecutionRequest";
        tangoLoggerMessage = ("Test plan with ${testDescriptor.getTest_uuid()} will be executed. Test request received from curator: ${message}");
        tangoLoggerStatus = "200";
        tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

        def dockerCompose
        try {
            dockerCompose = converter.getDockerCompose(testDescriptor)

        } catch (Exception e) {
            tangoLoggerType = "E";
            tangoLoggerOperation = "ExecutorController.testExecutionRequest";
            tangoLoggerMessage = ("Error creating the docker-compose: ${e.getMessage()}".toString()+e.toString());
            tangoLoggerStatus = "500";
            tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

            return responseUtils.getErrorResponseEntity(
                    HttpStatus.BAD_REQUEST,
                    "Error creating the docker-compose: ${e.getMessage()}".toString(),
                    e.getCause())
        }

        try {
            fileUtils.createTestDirectories(testDescriptor.test_uuid, new ArrayList<Service>(dockerCompose.services.values()))
            def dockerComposeString = converter.serializeDockerCompose(dockerCompose)
            fileUtils.createDockerComposeFile(testDescriptor.test_uuid, dockerComposeString)

        } catch (Exception e) {
            tangoLoggerType = "E";
            tangoLoggerOperation = "ExecutorController.testExecutionRequest";
            tangoLoggerMessage = ("Error storing the docker-compose file: ${e.getMessage()}".toString()+e.toString());
            tangoLoggerStatus = "500";
            tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)
            return responseUtils.getErrorResponseEntity(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error storing the docker-compose file: ${e.getMessage()}".toString(),
                    e.getCause())
        }

        try {
            def testExecution = new TestExecution(testDescriptor.test_uuid, converter.serializeDockerCompose(dockerCompose))
            testExecutionRepository.save(testExecution)
        } catch (Exception e) {
            tangoLoggerType = "E";
            tangoLoggerOperation = "ExecutorController.testExecutionRequest";
            tangoLoggerMessage = ("Error storing the test exercise in DB: ${e.getMessage()}".toString()+e.toString());
            tangoLoggerStatus = "500";
            tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)
            return responseUtils.getErrorResponseEntity(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error storing the test exercise in DB: ${e.getMessage()}".toString(),
                    e.getCause())
        }

        executor.executeTest(testDescriptor.test_uuid, dockerCompose, test)

        return responseUtils.getResponseEntity(
                HttpStatus.ACCEPTED,
                "test_uuid",
                testDescriptor.test_uuid)
    }

    @RequestMapping(method = RequestMethod.DELETE,
            path = "/api/v1/test-executions/{test_id}/cancel",
            consumes = "application/json",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Cancel an executing test", notes = "Receive a test test_uuid and callbacks, check if it is running and cancel it if possible")
    @ApiResponses([
            @ApiResponse(code = 200, message = "Test has been cancelled"),
            @ApiResponse(code = 404, message = "The test with the provided test_uuid was not found"),
            @ApiResponse(code = 500, message = "There was a problem during the test cancelling")
    ])
    ResponseEntity<String> testCancelExecutionRequest(@PathVariable("test_id") String testId, @RequestBody Test callbacks) {

        def testExecutionOpt = testExecutionRepository.findById(testId.toString())

        if (testExecutionOpt.isPresent()) {

            executor.cancelTest(testId, callbacks)

            return responseUtils.getResponseEntity(
                    HttpStatus.ACCEPTED,
                    "test-test_uuid", testId)
        }

        return responseUtils.getErrorResponseEntity(
                HttpStatus.NOT_FOUND,
                "Test not found",
                null)
    }

}
