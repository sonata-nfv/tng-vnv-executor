package app.controller

import app.database.TestExecution
import app.database.TestExecutionRepository
import app.model.docker_compose.Service
import app.model.test_descriptor.TestDescriptor
import app.util.Converter
import app.util.FileUtils
import app.util.ResponseUtils
import groovy.util.logging.Slf4j
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
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
    ResponseUtils responseUtils

    @Autowired
    TestExecutionRepository testExecutionRepository

    @RequestMapping(method = RequestMethod.POST,
            path = "/test-executions",
            consumes = "application/yaml",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiOperation(value = "Start a test", notes = "Receive a test descriptor, check its validity and create the docker-compose and directories, starting the test")
    @ApiResponses([
        @ApiResponse(code = 202, message = "Test Descriptor is valid, test is being executed"),
        @ApiResponse(code = 400, message = "Test Descriptor is not valid"),
        @ApiResponse(code = 500, message = "There was a problem during the test building")
    ])
    ResponseEntity<String> testExecutionRequest(@RequestBody TestDescriptor testDescriptor) {

        if (!testDescriptor.id) {
            testDescriptor.id = UUID.randomUUID().toString()
        }

        def dockerCompose
        try {
            dockerCompose = converter.getDockerCompose(testDescriptor)
        } catch(Exception e) {
            logger.error("Error creating the docker-compose: ${e.getMessage()}".toString(), e)
            return responseUtils.getErrorResponseEntity(
                    HttpStatus.BAD_REQUEST,
                    "Error creating the docker-compose: ${e.getMessage()}".toString(),
                    e.getCause())
        }

        try {

            fileUtils.createTestDirectories(testDescriptor.id, new ArrayList<Service>(dockerCompose.services.values()))
            def dockerComposeString = converter.serializeDockerCompose(dockerCompose)
            fileUtils.createDockerComposeFile(testDescriptor.id, dockerComposeString)
        } catch (Exception e) {
            logger.error("Error storing the docker-compose file: ${e.getMessage()}".toString(), e)
            return responseUtils.getErrorResponseEntity(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error storing the docker-compose file: ${e.getMessage()}".toString(),
                    e.getCause())
        }

        //Update Database with docker-compose file and starting status
        def testExecution = new TestExecution(testDescriptor.id, converter.serializeDockerCompose(dockerCompose))
        testExecutionRepository.save(testExecution)

        //Execute docker-compose up command

        sleep(1000)

        //Update Database with running status

        testExecution = testExecutionRepository.findById(testDescriptor.id).orElse(null) as TestExecution
        if(testExecution) {
            testExecution.state = TestExecution.TestState.RUNNING
            testExecutionRepository.save(testExecution)
        }

        //Wait for completion
        sleep(5000)

        //Update Database with status completed/error

        testExecution = testExecutionRepository.findById(testDescriptor.id).orElse(null) as TestExecution
        if(testExecution) {
            testExecution.state = TestExecution.TestState.COMPLETED
            testExecutionRepository.save(testExecution)
        }

        //Execute docker-compose down command and delete volumes/directories


        return responseUtils.getResponseEntity(
                HttpStatus.ACCEPTED,
                "test-id",
                testDescriptor.id)
    }

    @RequestMapping(method = RequestMethod.PUT,
            path = "/test-executions/{test_id}/cancel",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Cancel an executing test", notes = "Receive a test id, check if it is running and cancel it if possible")
    @ApiResponses([
            @ApiResponse(code = 200, message = "Test has been cancelled"),
            @ApiResponse(code = 404, message = "The test with the provided id was not found"),
            @ApiResponse(code = 500, message = "There was a problem during the test cancelling")
    ])
    ResponseEntity<String> testExecutionCancel(@PathVariable("test_id") String testId) {

        return responseUtils.getErrorResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Method not implemented yet",
                null)
    }
}
