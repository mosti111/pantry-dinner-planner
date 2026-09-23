package com.pantry;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {
    @Test
    void modulesHaveNoCyclesOrIllegalDependencies() {
        ApplicationModules.of(PantryApplication.class).verify();
    }
}
