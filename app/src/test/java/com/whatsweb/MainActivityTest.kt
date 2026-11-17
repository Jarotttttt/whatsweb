package com.whatsweb

import androidx.appcompat.app.AppCompatActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityTest {

    @Test
    fun testMainActivityClassExistsAndInheritsAppCompatActivity() {
        val clazz = MainActivity::class.java
        assertEquals("com.whatsweb.MainActivity", clazz.name)
        assertTrue(AppCompatActivity::class.java.isAssignableFrom(clazz))
    }
}
