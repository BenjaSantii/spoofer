package com.spoofer.location

import android.app.AppOpsManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("DEPRECATION")
class MockLocationPermissionCheckerTest {
    private val appOpsManager = mockk<AppOpsManager>()
    private val checker = MockLocationPermissionChecker(appOpsManager, uid = 1234, packageName = "com.example.spoofer")

    @Test
    fun `reports permission granted when mock location app op is allowed`() {
        every {
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, 1234, "com.example.spoofer")
        } returns AppOpsManager.MODE_ALLOWED

        assertTrue(checker.isAllowed())
        verify(exactly = 1) {
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, 1234, "com.example.spoofer")
        }
    }

    @Test
    fun `reports permission missing when mock location app op is ignored`() {
        every {
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, 1234, "com.example.spoofer")
        } returns AppOpsManager.MODE_IGNORED

        assertFalse(checker.isAllowed())
    }

    @Test
    fun `reports permission missing when app ops cannot be read`() {
        every {
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, 1234, "com.example.spoofer")
        } throws SecurityException("denied")

        assertFalse(checker.isAllowed())
    }
}
