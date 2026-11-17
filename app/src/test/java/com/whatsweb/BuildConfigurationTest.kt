package com.whatsweb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BuildConfigurationTest {
    @Test
    fun verifyApplicationPackageName() {
        val expectedPackage = "com.whatsweb"
        assertEquals("com.whatsweb", expectedPackage)
    }

    @Test
    fun verifyBrandResourcesExist() {
        assertNotEquals(0, R.color.whatsapp_teal)
        assertNotEquals(0, R.color.whatsapp_green)
        assertNotEquals(0, R.color.whatsapp_light_green)
        assertNotEquals(0, R.drawable.ic_refresh)
        assertNotEquals(0, R.drawable.ic_more_vert)
        assertNotEquals(0, R.string.app_name)
        assertNotEquals(0, R.string.app_description)
    }
}
