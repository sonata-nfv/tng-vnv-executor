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
import app.model.docker_compose.DockerCompose
import app.model.test.Callback
import app.model.test.Test
import app.util.ResponseUtils
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
@Slf4j(value = "logger")
@PropertySource("classpath:application.properties")
class Executor {

    @Autowired
    TaskExecutor taskExecutor

    @Autowired
    TestExecutionRepository testExecutionRepository

    @Value('${CALLBACKS}')
    String CALLBACKS

    void executeTest(final String testId, DockerCompose dockerCompose, Test test) {
        taskExecutor.execute(new Runnable() {
            @Override
            void run() {

                def callback
                Process process
                def testExecution = testExecutionRepository.findById(testId).orElse(null) as TestExecution

                //Execute docker-compose up command

                logger.info("Callbacks to Curator are ${CALLBACKS}")

                try {
                    process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up -d")
                    logger.info("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up -d")
                    process.waitForProcessOutput()

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
                        ResponseUtils.postCallback(" ${ callback.getPath().replace("<test_uuid>",testId)}", response)
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
                    ResponseUtils.postCallback("${callback.getPath().replace("<test_uuid>",testId)}", response)
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
                        ResponseUtils.postCallback("${callback.getPath().replace("<test_uuid>",testId)}", response)
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
                Validator.executeValidation(testId, test)
            }
        })
    }

    void cancelTest(final String testId, Test callbacks) {
        taskExecutor.execute(new Runnable() {
            @Override
            void run() {

                def callback = callbacks.getCallback(Callback.CallbackTypes.cancel)
                def testExecutionOpt = testExecutionRepository.findById(testId.toString())

                //docker-compose down
                try {
                    def process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    logger.info("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")
                    process.waitForProcessOutput()

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
                        ResponseUtils.postCallback("${callback.getPath()}", response)
                    }
                    return
                }

                //delete folders
                try {
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
                    ResponseUtils.postCallback("${callback.getPath()}", response)
                }
            }
        })
    }
}
