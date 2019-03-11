package app.util


import app.model.docker_compose.Service
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

@Component
@PropertySource("classpath:application.properties")
@Scope(value = "singleton")
@Slf4j(value = "logger")
class FileUtils {

    @Value('${DC.TEST_PATH}')
    private String TEST_PATH

    @Value('${DC.RESULTS_PATH}')
    private String RESULTS_PATH

    @Value('${DC.WAIT_FOR_SCRIPT}')
    private String WAIT_FOR_SCRIPT

    @Value('${DC.COMPOSE_FILES_PATH}')
    private String COMPOSE_FILES_PATH

    private static FileUtils instance = null

    private FileUtils() {}

    static FileUtils getInstance() {
        if(!instance) {
            instance = new FileUtils()
        }
        return instance
    }

    void createTestDirectories(String testId, List<Service> services) throws FileAlreadyExistsException, RuntimeException{

        def bashScriptsFolder = new File("/executor/bash_scripts")
        def bashComposeFilesFolder = new File("/executor/compose_files")
        def bashTestsFolder = new File("/executor/tests")

        if (!bashScriptsFolder.exists()){
            bashScriptsFolder.mkdir()
        }

        if (!bashComposeFilesFolder.exists()){
            bashComposeFilesFolder.mkdir()
        }

        if (!bashTestsFolder.exists()){
            bashTestsFolder.mkdir()
        }

        def sharedScriptFile = new File(String.format(WAIT_FOR_SCRIPT))
        //def scriptOriginal = new File("/wait_for.sh")

        if (!sharedScriptFile.exists()){
            logger.info("/wait_for.sh is not yet copied")
            def process = Runtime.getRuntime().exec("mv /wait_for.sh /executor/bash_scripts/wait_for.sh")
            logger.info("Executing: mv /wait_for.sh /executor/bash_scripts/wait_for.sh")
            process.waitForProcessOutput()
            if (!process.toString().contains("exitValue=0")) {
               logger.error("Error moving wait_for.sh script")
            }
        } else {
            logger.info("/wait_for.sh is already copied to ${WAIT_FOR_SCRIPT}")
        }

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

        def path = "${String.format(COMPOSE_FILES_PATH)}"

        new File("${path}/${testId}-docker-compose.yml").withWriter('utf-8') { writer ->
            writer.write(dockerCompose)
        }
    }
}
