package app.model.test

enum TestDescriptorPhases {
    SETUP_PHASE("setup"), EXERCISE_PHASE("exercise"), VERIFICATION_PHASE("verification")

    String name

    TestDescriptorPhases(String name) {
        this.name = name
    }

    boolean equalsToString(String phase) {
        return (phase.equalsIgnoreCase(this.name))
    }
}
