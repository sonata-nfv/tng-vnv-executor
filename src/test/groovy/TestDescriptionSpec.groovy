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


import app.config.GeneralConfig
import app.model.test.*
import spock.lang.Specification

class TestDescriptionSpec extends Specification {

    static def mapper
    static def testDescriptorString
    static def testDescriptor

    def setupSpec() {
        testDescriptorString = getClass().getResource('/test-descriptor-correct.json').text
        mapper = new GeneralConfig().objectMapper()
    }

    def "Test Descriptor is valid"() {

        when: testDescriptor = mapper.readValue(testDescriptorString as String, Test.class)
        then: noExceptionThrown()
    }

    def "Setup Phase exists"() {

        when: def setupPhase = testDescriptor.test.getPhase(TestDescriptorPhases.SETUP_PHASE) as TestDescriptorSetupPhase
        then: setupPhase != null
    }

    def "Setup Phase has steps"() {

        when: def setupPhase = testDescriptor.test.getPhase(TestDescriptorPhases.SETUP_PHASE) as TestDescriptorSetupPhase
        then: setupPhase.steps.size() > 0
    }

    def "Setup Phase has probes"() {

        when: def steps = (testDescriptor.test.getPhase(TestDescriptorPhases.SETUP_PHASE) as TestDescriptorSetupPhase).steps
        then: steps.any{step -> step.probes && step.probes.size() > 0}
    }

    def "Exercise Phase exists"() {

        when: def exercisePhase = testDescriptor.test.getPhase(TestDescriptorPhases.EXERCISE_PHASE) as TestDescriptorExercisePhase
        then: exercisePhase != null
    }

    def "Exercise Phase has steps"() {

        when: def exercisePhase = testDescriptor.test.getPhase(TestDescriptorPhases.EXERCISE_PHASE) as TestDescriptorExercisePhase
        then: exercisePhase.steps.size() > 0
    }

    def "Verification Phase exists"() {

        when: def verificationPhase = testDescriptor.test.getPhase(TestDescriptorPhases.VERIFICATION_PHASE) as TestDescriptorVerificationPhase
        then: verificationPhase != null
    }

    def "Verification Phase has steps"() {

        when: def verificationPhase = testDescriptor.test.getPhase(TestDescriptorPhases.VERIFICATION_PHASE) as TestDescriptorVerificationPhase
        then: verificationPhase.steps.size() > 0
    }

    def "Test Descriptor is not valid"() {

        setup:
        def testDescriptorString = getClass().getResource('/test-descriptor-error.json').text
        def mapper = new GeneralConfig().objectMapperYaml()

        when: mapper.readValue(testDescriptorString as String, TestDescriptor.class)

        then: thrown(Exception.class)
    }
}