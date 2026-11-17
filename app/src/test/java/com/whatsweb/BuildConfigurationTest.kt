package com.whatsweb

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildConfigurationTest {
    @Test
    fun verifyApplicationPackageName() {
        val expectedPackage = "com.whatsweb"
        assertEquals("com.whatsweb", expectedPackage)
    }
}
