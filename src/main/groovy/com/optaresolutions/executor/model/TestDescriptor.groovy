package com.optaresolutions.executor.model

class TestDescriptor {

    Map<TestDescriptorPhases, TestDescriptorPhase> phases = new HashMap<>()
    Map<String, Probe> probes = new HashMap<>()

    TestDescriptorPhase getPhase(TestDescriptorPhases type) {
        if(phases) {
            def phase = phases.get(type)
            if (phase) {
                return phase
            }
        }
        return null
    }
}
