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


import app.config.ApplicationConfig
import app.config.GeneralConfig
import app.model.test.*
import app.logic.Converter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [Converter.class, GeneralConfig.class, ApplicationConfig.class])
@TestPropertySource("classpath:application.properties")
class TestDescriptionErrorSpec extends Specification {

    @Autowired
    Converter converter

    static TestDescriptor testDescriptor

    static TestDescriptorSetupPhase setupPhase
    static TestDescriptorExercisePhase exercisePhase
    static TestDescriptorVerificationPhase verificationPhase

    def setup() {
        testDescriptor = new TestDescriptor()
        testDescriptor.phases = new ArrayList<>()

        setupPhase = new TestDescriptorSetupPhase()
        setupPhase.steps = new ArrayList<>()
        setupPhase.steps.add(new TestDescriptorSetupPhaseStep())

        exercisePhase = new TestDescriptorExercisePhase()
        exercisePhase.steps = new ArrayList<>()
        exercisePhase.steps.add(new TestDescriptorExercisePhaseStep())

        verificationPhase = new TestDescriptorVerificationPhase()
        verificationPhase.steps = new ArrayList<>()
        verificationPhase.steps.add(new TestDescriptorVerificationPhaseStep())

        testDescriptor.phases.add(setupPhase)
        testDescriptor.phases.add(exercisePhase)
        testDescriptor.phases.add(verificationPhase)
    }

    def "No Phases"() {

        setup: testDescriptor.phases = new ArrayList<>()
        when: converter.getDockerCompose(testDescriptor)

        then:
        def ex = thrown(RuntimeException.class)
        ex.message == converter.ERROR_NO_PHASES_INFO
    }

    def "Setup Phase does not exist"() {

        setup:
        testDescriptor.phases = new ArrayList<>()
        testDescriptor.phases.add(exercisePhase)
        testDescriptor.phases.add(verificationPhase)

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == converter.ERROR_NO_SETUP_PHASE
    }

    def "Setup Phase with no steps"() {

        setup:
        testDescriptor.phases = new ArrayList<>()
        testDescriptor.phases.add(exercisePhase)
        testDescriptor.phases.add(verificationPhase)
        testDescriptor.phases.add(new TestDescriptorSetupPhase())


        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == converter.ERROR_NO_PROBES_INFO
    }

    def "Setup Phase has not probes"() {

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == converter.ERROR_NO_PROBES_INFO
    }

    def "Exercise Phase does not exist"() {

        setup:
        testDescriptor.phases = new ArrayList<>()
        testDescriptor.phases.add(setupPhase)
        testDescriptor.phases.add(verificationPhase)

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == converter.ERROR_NO_EXECUTION_PHASE
    }

    def "Exercise Phase with no steps"() {

        setup:
        testDescriptor.phases = new ArrayList<>()
        testDescriptor.phases.add(setupPhase)
        testDescriptor.phases.add(verificationPhase)
        testDescriptor.phases.add(new TestDescriptorExercisePhase())

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == converter.ERROR_NO_STEPS_EXECUTION_PHASE
    }

    def "Exercise Phase with unknown probes"() {

        setup:

        def probe = new Probe()
        probe.id = "probeId"
        def list = new ArrayList()
        list.add(probe)
        setupPhase.steps.get(0).probes = list

        def exerciseStep = exercisePhase.steps.get(0)
        exerciseStep.run = "unknownProbe"

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == String.format(converter.ERROR_NO_PROBE_ID, "unknownProbe")
    }

    def "Exercise Phase with unknown dependencies"() {

        setup:

        def probe = new Probe()
        probe.id = "probeId"
        def list = new ArrayList()
        list.add(probe)
        setupPhase.steps.get(0).probes = list

        exercisePhase.steps = new ArrayList<>()

        def step1 = new TestDescriptorExercisePhaseStep()
        step1.name = "service1"
        step1.run = "probeId"
        exercisePhase.steps.add(step1)

        def step2 = new TestDescriptorExercisePhaseStep()
        step2.name = "service2"
        step2.run = "probeId"
        step2.entrypoint ="entrypoint"
        step2.dependencies = new ArrayList<>()
        step2.dependencies.add("unknownDependency")
        exercisePhase.steps.add(step2)

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == String.format(converter.ERROR_NO_DEPENDENCY_FOUND, "unknownDependency")
    }

    def "Exercise Phase with duplicate services"() {

        setup:

        def probe = new Probe()
        probe.id = "probeId"
        def list = new ArrayList()
        list.add(probe)
        setupPhase.steps.get(0).probes = list

        exercisePhase.steps = new ArrayList<>()

        def step1 = new TestDescriptorExercisePhaseStep()
        step1.name = "service1"
        step1.run = "probeId"
        exercisePhase.steps.add(step1)

        def step2 = new TestDescriptorExercisePhaseStep()
        step2.name = "service1"
        step2.run = "probeId"
        exercisePhase.steps.add(step2)

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == String.format(converter.ERROR_DUPLICATE_SERVICE, "service1")
    }

    def "Exercise Phase -- no entrypoint with start_delay"() {

        setup:

        def probe = new Probe()
        probe.id = "probeId"
        def list = new ArrayList()
        list.add(probe)
        setupPhase.steps.get(0).probes = list

        exercisePhase.steps = new ArrayList<>()

        def step = new TestDescriptorExercisePhaseStep()
        step.name = "service1"
        step.run = "probeId"
        step.start_delay = 5
        exercisePhase.steps.add(step)

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == converter.ERROR_NO_ENTRYPOINT_START_DELAY
    }

    def "Exercise Phase -- no entrypoint with dependencies"() {

        setup:

        def probe = new Probe()
        probe.id = "probeId"
        def list = new ArrayList()
        list.add(probe)
        setupPhase.steps.get(0).probes = list

        exercisePhase.steps = new ArrayList<>()

        def step = new TestDescriptorExercisePhaseStep()
        step.name = "service1"
        step.run = "probeId"
        step.dependencies = new ArrayList<>()
        step.dependencies.add("dependency")
        exercisePhase.steps.add(step)

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == converter.ERROR_NO_ENTRYPOINT_DEPENDENCY
    }

    def "Verification Phase does not exist"() {

        setup:
        testDescriptor.phases = new ArrayList<>()
        testDescriptor.phases.add(exercisePhase)
        testDescriptor.phases.add(setupPhase)

        when: converter.getDockerCompose(testDescriptor)
        then:
        def ex = thrown(RuntimeException.class)
        ex.message == converter.ERROR_NO_VERIFICATION_PHASE
    }
}