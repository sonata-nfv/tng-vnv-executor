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


import app.model.test.Condition
import app.util.Validator
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

class ValidatorSpec extends Specification {

    @Autowired
    Validator validator

    static File txtFile
    static File jsonFile
    static def ERROR_UNKNOWN_OPTION = "Validation FAILED. Unknown option"

    def setupSpec() {
        jsonFile = new File('/test-validator.json')
        txtFile = new File('/test-validator.json')
    }

    def "Json eq"() {

        setup:
        Condition parser = new Condition()
        parser.setType("json")
        parser.setCondition("=")
        parser.setFind("error_rate")
        parser.setValue("0.04")
        parser.setFile("file")
        parser.setName("name")
        parser.setVerdict("pass")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json eq2"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("==")
        parser.setFind("error_rate")
        parser.setValue("0.04")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json neq"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("!=")
        parser.setFind("error_rate")
        parser.setValue("0.05")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json gt"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition(">")
        parser.setFind("error_rate")
        parser.setValue("0.03")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()

    }

    def "Json get"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition(">=")
        parser.setFind("error_rate")
        parser.setValue("0.04")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()

    }

    def "Json lt"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("<")
        parser.setFind("error_rate")
        parser.setValue("0.05")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json let"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("<=")
        parser.setFind("error_rate")
        parser.setValue("0.04")

        when: validator.validateConditions(parser, jsonFile)
        then: noExceptionThrown()
    }

    def "Json unknown condition"() {

        setup:
        def parser = new Condition()
        parser.setType("json")
        parser.setCondition("<>")
        parser.setFind("error_rate")
        parser.setValue("0.04")

        when: validator.validateConditions(parser, jsonFile)
        then:
        def ex = thrown(Exception.class)
        ex.message == ERROR_UNKNOWN_OPTION

    }

    def "Text present"() {

        setup:
        def parser = new Condition()
        parser.setType("txt")
        parser.setFind("OK")
        parser.setCondition("present")

        when: validator.validateConditions(parser, txtFile)
        then: noExceptionThrown()
    }

    def "Text not present"() {

        setup:
        def parser = new Condition()
        parser.setType("txt")
        parser.setFind("KO")
        parser.setCondition("not present")

        when: validator.validateConditions(parser, txtFile)
        then: noExceptionThrown()
    }

    def "Text unknown condition"() {

        setup:
        def parser = new Condition()
        parser.setType("txt")
        parser.setFind("KK")
        parser.setCondition("present")

        when: validator.validateConditions(parser, txtFile)
        then:
        def ex = thrown(Exception.class)
        ex.message == ERROR_UNKNOWN_OPTION
    }
}