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


import app.Application
import app.config.ApplicationConfig
import app.config.GeneralConfig
import app.database.TestExecution
import app.database.TestExecutionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [Application.class, GeneralConfig.class, ApplicationConfig.class, TestExecutionRepository.class])
@TestPropertySource("classpath:application-test.properties")
class DatabaseSpec extends Specification {

    @Autowired
    TestExecutionRepository testExecutionRepository

    static TestExecution testExecution

    def setupSpec() {
        testExecution = new TestExecution()
        testExecution.uuid = UUID.randomUUID().toString()
        testExecution.setState(TestExecution.TestState.STARTING)
    }

    def "TestExecution inserted and found"() {

        when:
        testExecutionRepository.save(testExecution)
        def aux = testExecutionRepository.findById(testExecution.getUuid()).get()

        then:
        noExceptionThrown()
        aux.created != null
        aux.lastModifiedDate != null
        aux.state == TestExecution.TestState.STARTING

    }

    def "TestExecution updating"() {

        setup: testExecution.state = TestExecution.TestState.CANCELLED
        when:
        testExecutionRepository.save(testExecution)
        def aux = testExecutionRepository.findById(testExecution.uuid).get()

        then:
        noExceptionThrown()
        aux.state == TestExecution.TestState.CANCELLED
        aux.created != null
        aux.lastModifiedDate != null
    }

    def "TestExecution deleted"() {

        when:
        testExecutionRepository.delete(testExecution)
        testExecutionRepository.findById(testExecution.uuid).get()

        then: thrown(NoSuchElementException.class)
    }

}
