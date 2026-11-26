package com.smartfitness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmartFitnessBaselineTests {

    @Test
    void projectIdentityIsSmartFitness() {
        assertEquals("SmartFitnessApplication", SmartFitnessApplication.class.getSimpleName());
    }
}
