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
import app.util.TangoLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
@PropertySource("classpath:application.properties")
class Executor {

    @Autowired
    Validator validator

    @Autowired
    TaskExecutor taskExecutor

    @Autowired
    ResponseUtils responseUtils

    @Autowired
    TestExecutionRepository testExecutionRepository

    @Value('${CALLBACKS}')
    String CALLBACKS

    //Tango logger
    def tangoLogger = new TangoLogger()
    String tangoLoggerType = null;
    String tangoLoggerOperation = null;
    String tangoLoggerMessage = null;
    String tangoLoggerStatus = null;

    void executeTest(final String testId, DockerCompose dockerCompose, Test test) {
        taskExecutor.execute(new Runnable() {
            @Override
            void run() {

                def callback
                Process process
                def testExecution = testExecutionRepository.findById(testId).orElse(null) as TestExecution

                //Execute docker-compose up command

                tangoLoggerType = "I";
                tangoLoggerOperation = "Executor.executeTest";
                tangoLoggerMessage = ("Callbacks to Curator are ${CALLBACKS}");
                tangoLoggerStatus = "200";
                tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                try {
                    process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up -d")

                    tangoLoggerType = "I";
                    tangoLoggerOperation = "Executor.executeTest";
                    tangoLoggerMessage = ("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} up -d");
                    tangoLoggerStatus = "200";
                    tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                    def stdout = new StringWriter()
                    def stderr = new StringWriter()
                    process.waitForProcessOutput(stdout, stderr)

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

                        tangoLoggerType = "E";
                        tangoLoggerOperation = "Executor.executeTest";
                        tangoLoggerMessage = ("Error executing docker-compose up command: ${e.toString()}");
                        tangoLoggerStatus = "500";
                        tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                        Response response = new Response()
                        response.setTest_uuid(testId)
                        response.setStatus("ERROR")
                        response.setMessage(tangoLoggerMessage)
                        responseUtils.postCallback(" ${ callback.getPath().replace("<test_uuid>",testId)}", response)
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
                    responseUtils.postCallback("${callback.getPath().replace("<test_uuid>",testId)}", response)
                }

                //Wait for completion
                try {
                    /*def exitedProbes

                    while (exitedProbes < dockerCompose.services.size()){

                        exitedProbes = 0

                        for (service in dockerCompose.services) {
                            logger.info("waiting for ${testId}-${service.value.getName()}")
                            logger.info("sh /executor/bash_scripts/wait_for.sh \"${service.value.getName()}\" \"${testId}\" \"/executor/compose_files/${testId}-docker-compose.yml\"")
                            process = Runtime.getRuntime().exec("sh /executor/bash_scripts/wait_for.sh ${service.value.getName()} ${testId} /executor/compose_files/${testId}-docker-compose.yml")
                            //process.waitFor()
                            process.waitForOrKill(30000)
                            logger.info("> ${process}")
                            if (process.toString().contains("exitValue=0")) {
                                logger.info("${testId}-${service.value.getName()} finished OK")
                                exitedProbes = exitedProbes +1
                            } else {
                                logger.info("${testId}-${service.value.getName()} NOT finished")
                                if (!process.toString().contains("exitValue=143")){
                                    throw new Exception("FAILED")
                                }
                            }
                        }
                    }*/
                    for (service in dockerCompose.services) {
                        tangoLoggerType = "I";
                        tangoLoggerOperation = "Executor.executeTest";
                        tangoLoggerMessage = ("waiting for ${testId}-${service.value.getName()}");
                        tangoLoggerStatus = "200";
                        tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                        tangoLoggerType = "I";
                        tangoLoggerOperation = "Executor.executeTest";
                        tangoLoggerMessage = ("sh /executor/bash_scripts/wait_for.sh \"${service.value.getName()}\" \"${testId}\" \"/executor/compose_files/${testId}-docker-compose.yml\"");
                        tangoLoggerStatus = "200";
                        tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                        process = Runtime.getRuntime().exec("sh /executor/bash_scripts/wait_for.sh ${service.value.getName()} ${testId} /executor/compose_files/${testId}-docker-compose.yml")
                        def stdout = new StringWriter()
                        def stderr = new StringWriter()
                        process.waitForProcessOutput(stdout, stderr)

                        tangoLoggerType = "I";
                        tangoLoggerOperation = "Executor.executeTest";
                        tangoLoggerMessage = ("> ${process}");
                        tangoLoggerStatus = "200";
                        tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                        if (!process.toString().contains("exitValue=0")) {
                            tangoLoggerType = "E";
                            tangoLoggerOperation = "Executor.executeTest";
                            tangoLoggerMessage = ("${testId}-${service.value.getName()} FAILED");
                            tangoLoggerStatus = "500";
                            tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                            throw new Exception("FAILED")
                        }
                    }
                } catch (Exception e) {

                    process.destroy()

                    if (testExecution) {
                        testExecution.state = TestExecution.TestState.ERROR
                        testExecutionRepository.save(testExecution)
                    }

                    if (CALLBACKS.toUpperCase()=="ENABLED") {
                        callback = test.getCallback(Callback.CallbackTypes.cancel)

                        tangoLoggerType = "E";
                        tangoLoggerOperation = "Executor.executeTest";
                        tangoLoggerMessage = ("Error waiting for test completion: ${e.toString()}");
                        tangoLoggerStatus = "500";
                        tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                        Response response = new Response()
                        response.setTest_uuid(testId)
                        response.setStatus("ERROR")
                        response.setMessage(tangoLoggerMessage)
                        responseUtils.postCallback("${callback.getPath().replace("<test_uuid>",testId)}", response)
                    }
                    return
                }

                //Execute docker-compose down command
                try {
                    process = Runtime.getRuntime().exec("docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v")

                    tangoLoggerType = "I";
                    tangoLoggerOperation = "Executor.executeTest";
                    tangoLoggerMessage = ("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v");
                    tangoLoggerStatus = "200";
                    tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                    def stdout = new StringWriter()
                    def stderr = new StringWriter()
                    process.waitForProcessOutput(stdout, stderr)
                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    tangoLoggerType = "E";
                    tangoLoggeroperation = "Executor.executeTest";
                    tangoLoggerMessage = ("Error executing docker-compose down command. Sending message to ${callback.path}");
                    tangoLoggerStatus = "500";
                    tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                    return
                }

                //Launch Validation/Verification
                tangoLoggerType = "I";
                tangoLoggerOperation = "Executor.executeTest";
                tangoLoggerMessage = ("${testId} SUCCESSFULLY execution. Validation Pending...");
                tangoLoggerStatus = "200";
                tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                validator.executeValidation(testId, test)
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

                    tangoLoggerType = "I";
                    tangoLoggerOperation = "Executor.cancelTest";
                    tangoLoggerMessage = ("Executing: docker-compose -f /executor/compose_files/${testId}-docker-compose.yml -p ${testId} down -v");
                    tangoLoggerStatus = "200";
                    tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                    def stdout = new StringWriter()
                    def stderr = new StringWriter()
                    process.waitForProcessOutput(stdout, stderr)

                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    if (CALLBACKS.toUpperCase()=="ENABLED") {
                        tangoLoggerType = "E";
                        tangoLoggerOperation = "Executor.cancelTest";
                        tangoLoggerMessage = ("Error executing docker-compose down command: ${e.toString()}");
                        tangoLoggerStatus = "500";
                        tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                        Response response = new Response()
                        response.setTest_uuid(testId)
                        response.setStatus("ERROR")
                        response.setMessage(tangoLoggerMessage)
                        responseUtils.postCallback("${callback.getPath()}", response)
                    }
                    return
                }

                //delete folders
                try {
                    def process = Runtime.getRuntime().exec("rm -rf /executor/tests/${testId}")

                    tangoLoggerType = "I";
                    tangoLoggerOperation = "Executor.cancelTest";
                    tangoLoggerMessage = ("Executing: rm -rf /executor/tests/${testId}");
                    tangoLoggerStatus = "200";
                    tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                    def stdout = new StringWriter()
                    def stderr = new StringWriter()
                    process.waitForProcessOutput(stdout, stderr)

                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }

                    process = Runtime.getRuntime().exec("rm -rf /executor/compose_files/${testId}-docker-compose.yml")

                    tangoLoggerType = "I";
                    tangoLoggerOperation = "Executor.cancelTest";
                    tangoLoggerMessage = ("Executing: rm -rf /executor/compose_files/${testId}-docker-compose.yml");
                    tangoLoggerStatus = "200";
                    tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)

                    process.waitForProcessOutput(stdout, stderr)

                    if (!process.toString().contains("exitValue=0")) {
                        throw new Exception("FAILED")
                    }
                } catch (Exception e) {
                    tangoLoggerType = "E";
                    tangoLoggerOperation = "Executor.cancelTest";
                    tangoLoggerMessage = ("Error deleting folders");
                    tangoLoggerStatus = "500";
                    tangoLogger.log(tangoLoggerType, tangoLoggerOperation, tangoLoggerMessage, tangoLoggerStatus)
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
                    responseUtils.postCallback("${callback.getPath()}", response)
                }
            }
        })
    }
}
