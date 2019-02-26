package app.util


import app.model.docker_compose.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.nio.file.FileAlreadyExistsException

@Component
@PropertySource("classpath:application.properties")
@Scope(value = "singleton")

class FileUtils {

    @Value('${DC.TEST_PATH}')
    private String TEST_PATH

    @Value('${DC.RESULTS_PATH}')
    private String RESULTS_PATH

    private static FileUtils instance = null

    private FileUtils() {}

    static FileUtils getInstance() {
        if(!instance) {
            instance = new FileUtils()
        }
        return instance
    }

    void createTestDirectories(String testId, List<Service> services) throws FileAlreadyExistsException, RuntimeException{

        def rootDir = new File(String.format(TEST_PATH, testId))
        if(rootDir.exists()) {
            throw new FileAlreadyExistsException("Directory ${rootDir.getAbsolutePath()} already exists")
        }

        if (!rootDir.mkdir()) {
            throw new RuntimeException("Error creating the test directory: ${rootDir.getAbsolutePath()}")
        }

        for(service in services) {
            def resultsDir = new File(String.format(RESULTS_PATH, testId, service.name))
            if(resultsDir.exists()) {
                throw new FileAlreadyExistsException("Directory ${resultsDir.getAbsolutePath()} already exists")
            }

            if (!resultsDir.mkdirs()) {
                throw new RuntimeException("Error creating the test result directory: ${resultsDir.getAbsolutePath()}")
            }
        }
    }

    void createDockerComposeFile(String testId, String dockerCompose) throws FileNotFoundException {

        def path = String.format(TEST_PATH, testId)
        def rootDir = new File(path)
        if(!rootDir.exists()) {
            throw new FileNotFoundException("Directory ${rootDir.getAbsolutePath()} not found")
        }

        new File("${path}/docker-compose.yml").withWriter('utf-8') { writer ->
            writer.write(dockerCompose)
        }
    }
}
