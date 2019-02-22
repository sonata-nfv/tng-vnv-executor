package app.controller


import app.model.Service
import app.util.FileUtils
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j(value = "logger")
class ExecutorController {

    @Autowired
    app.util.Converter converter

    @Autowired
    FileUtils fileUtils

    @PostMapping(value = "/test-executions")
    ResponseEntity<?> testExecutionRequest(@RequestBody String testDescriptorFile) {

        def testDescriptor
        try {
            testDescriptor = converter.getTestDescriptor(testDescriptorFile)
        } catch(Exception e) {
            logger.error("Error reading the test descriptor: ${e.getMessage()}".toString(), e)
            return ResponseEntity.badRequest()
                    .body("Error reading the test descriptor, is it valid? ${e.getMessage()}".toString())
        }

        def dockerCompose
        try {
            dockerCompose = converter.getDockerCompose(testDescriptor)
        } catch(Exception e) {
            logger.error("Error creating the docker-compose: ${e.getMessage()}".toString(), e)
            return ResponseEntity.badRequest().body("Error creating the docker-compose: ${e.getMessage()}".toString())
        }

        try {

            fileUtils.createTestDirectories(testDescriptor.id, new ArrayList<Service>(dockerCompose.services.values()))
            def dockerComposeString = converter.serializeDockerCompose(dockerCompose)
            fileUtils.createDockerComposeFile(testDescriptor.id, dockerComposeString)
        } catch (Exception e) {
            logger.error("Error storing the docker-compose file: ${e.getMessage()}".toString(), e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error storing the docker-compose file: ${e.getMessage()}".toString())
        }

        return ResponseEntity.ok(testDescriptor.id)
    }
}
