package com.optaresolutions.executor.controller

import com.optaresolutions.executor.model.DockerCompose
import com.optaresolutions.executor.model.Service
import com.optaresolutions.executor.model.TestDescriptorExercisePhase
import com.optaresolutions.executor.model.TestDescriptorPhases
import com.optaresolutions.executor.util.YamlConverter
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.WebServerException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Slf4j(value = "logger")
class ExecutorController {

    @Autowired
    YamlConverter yamlConverter

    @PostMapping(value = "/test")
    ResponseEntity<?> getIndexInfo(@RequestBody String testDescriptorFile) {

        try {

            def testDescriptor = yamlConverter.getTestDescriptor(testDescriptorFile)
            logger.info("TEST DESCRIPTOR: ${testDescriptor.toString()}")


            def exercisePhase = testDescriptor.getPhase(TestDescriptorPhases.EXERCISE_PHASE)
            if(!exercisePhase) {
                throw new RuntimeException("No exercise phase found")
            }

            def dockerCompose = new DockerCompose()
            for(step in (exercisePhase as TestDescriptorExercisePhase).steps) {
                def service = new Service()
                def probe = testDescriptor.probes.get(step.run)
                if(!probe) {
                    throw new RuntimeException("No probe with id ${step.run} found")
                }

                def environment = new ArrayList<String>()
                for(parameter in probe.parameters) {
                    for(key in parameter.keySet()) {
                        def environmentVar = "${key}=${parameter.get(key)}".toString()
                        environment.add(environmentVar)
                    }
                }

                service.container_name = probe.name
                service.environment = environment
                service.image = probe.image
                service.scale = step.instances
                service.depends_on = step.dependency
                service.volumes.add("./results/<test_id>/${service.container_name}/:/".toString())
                dockerCompose.services.put(service.container_name, service)
            }


            def dockerComposeFile = yamlConverter.dumpDockerFile(dockerCompose)
            logger.info("Docker-Compose: ${dockerComposeFile}")

            new File("C:\\Users\\lalvarez\\Desktop\\docker-compose.yml").withWriter('utf-8') { writer ->
                writer.write(dockerComposeFile)
            }
            return ResponseEntity.ok("")

        } catch(Exception exception) {
            logger.error(exception.getMessage(), exception)
            return ResponseEntity.badRequest().body(exception.getMessage())
        }
    }
}
