package com.spoofer.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RoutePacingTest {
    @Test
    fun `calculates speed from route distance and duration`() {
        val speed = RoutePacing.speedMetersPerSecond(distanceMeters = 9_000.0, durationSeconds = 900)

        assertEquals(10f, speed, 0.001f)
    }

    @Test
    fun `rejects zero duration`() {
        assertThrows(IllegalArgumentException::class.java) {
            RoutePacing.speedMetersPerSecond(distanceMeters = 1_000.0, durationSeconds = 0)
        }
    }

    @Test
    fun `rejects zero distance`() {
        assertThrows(IllegalArgumentException::class.java) {
            RoutePacing.speedMetersPerSecond(distanceMeters = 0.0, durationSeconds = 60)
        }
    }
}
