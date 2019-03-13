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
import app.database.TestExecutionRepository
import app.model.test.Condition
import app.util.Validator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [Application.class, GeneralConfig.class, ApplicationConfig.class, TestExecutionRepository.class])
@TestPropertySource("classpath:application-test.properties")
class ValidatorSpec extends Specification {

    @Autowired
    Validator validator

    static File txtFile
    static File jsonFile
    static def ERROR_UNKNOWN_OPTION = "Validation FAILED. Unknown option"
    static def ERROR_STRING_NOT_PRESENT ="Validation [KK present] FAILED"
    static def ERROR_STRING_PRESENT ="Validation [OK not present] FAILED"

    def setupSpec() {
        jsonFile = new File(getClass().getResource('/test-validator.json').toURI())
        txtFile = new File(getClass().getResource('/test-validator.txt').toURI())
    }

    def "Json eq"() {

        setup:
        Condition condition = new Condition()
        condition.type="json"
        condition.condition="="
        condition.find="error_rate"
        condition.value="0.04"
        condition.file="file"
        condition.name="name"
        condition.verdict="pass"

        when: validator.validateConditions(condition, jsonFile)
        then: noExceptionThrown()
    }

    def "Json eq2"() {

        setup:
        def condition = new Condition()
        condition.type="json"
        condition.condition="=="
        condition.find="error_rate"
        condition.value="0.04"

        when: validator.validateConditions(condition, jsonFile)
        then: noExceptionThrown()
    }

    def "Json neq"() {

        setup:
        def condition = new Condition()
        condition.type="json"
        condition.condition="!="
        condition.find="error_rate"
        condition.find="0.05"

        when: validator.validateConditions(condition, jsonFile)
        then: noExceptionThrown()
    }

    def "Json gt"() {

        setup:
        def condition = new Condition()
        condition.type="json"
        condition.condition=">"
        condition.find="error_rate"
        condition.value="0.03"

        when: validator.validateConditions(condition, jsonFile)
        then: noExceptionThrown()

    }

    def "Json get"() {

        setup:
        def condition = new Condition()
        condition.type="json"
        condition.condition=">="
        condition.find="error_rate"
        condition.value="0.04"

        when: validator.validateConditions(condition, jsonFile)
        then: noExceptionThrown()

    }

    def "Json lt"() {

        setup:
        def condition = new Condition()
        condition.type="json"
        condition.condition="<"
        condition.find="error_rate"
        condition.value="0.05"

        when: validator.validateConditions(condition, jsonFile)
        then: noExceptionThrown()
    }

    def "Json let"() {

        setup:
        def condition = new Condition()
        condition.type="json"
        condition.condition="<="
        condition.find="error_rate"
        condition.value="0.04"

        when: validator.validateConditions(condition, jsonFile)
        then: noExceptionThrown()
    }

    def "Json unknown condition"() {

        setup:
        def condition = new Condition()
        condition.type="json"
        condition.condition="<>"
        condition.find="error_rate"
        condition.value="0.04"

        when: validator.validateConditions(condition, jsonFile)
        then:
        def ex = thrown(Exception.class)
        ex.message.contains(ERROR_UNKNOWN_OPTION)

    }

    def "Text present"() {

        setup:
        def condition = new Condition()
        condition.type="txt"
        condition.find="OK"
        condition.condition="present"

        when: validator.validateConditions(condition, txtFile)
        then: noExceptionThrown()
    }

    def "Text not present"() {

        setup:
        def condition = new Condition()
        condition.type="txt"
        condition.find="KO"
        condition.condition="not present"

        when: validator.validateConditions(condition, txtFile)
        then: noExceptionThrown()
    }

    def "Text present failed"(){
        setup:
        def condition = new Condition()
        condition.type="txt"
        condition.find="KK"
        condition.condition="present"

        when: validator.validateConditions(condition, txtFile)
        then:
        def ex = thrown(Exception.class)
        ex.message.contains(ERROR_STRING_NOT_PRESENT)
    }

    def "Text not present failed"(){
        setup:
        def condition = new Condition()
        condition.type="txt"
        condition.find="OK"
        condition.condition="not present"

        when: validator.validateConditions(condition, txtFile)
        then:
        def ex = thrown(Exception.class)
        ex.message.contains(ERROR_STRING_PRESENT)
    }

    def "Text unknown condition"() {

        setup:
        def condition = new Condition()
        condition.type="txt"
        condition.find="OK"
        condition.condition="prese"

        when: validator.validateConditions(condition, txtFile)
        then:
        def ex = thrown(Exception.class)
        ex.message.contains(ERROR_UNKNOWN_OPTION)
    }
}