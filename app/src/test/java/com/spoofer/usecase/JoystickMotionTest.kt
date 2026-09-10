package com.spoofer.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.geometry.LatLng

class JoystickMotionTest {
    @Test
    fun `north movement increases latitude`() {
        val origin = LatLng(-33.45, -70.65)

        val result = JoystickMotion.move(origin, bearingDegrees = 0f, distanceMeters = 10.0)

        assertTrue(result.latitude > origin.latitude)
        assertEquals(origin.longitude, result.longitude, 0.0000001)
    }

    @Test
    fun `east movement increases longitude`() {
        val origin = LatLng(-33.45, -70.65)

        val result = JoystickMotion.move(origin, bearingDegrees = 90f, distanceMeters = 10.0)

        assertEquals(origin.latitude, result.latitude, 0.0000001)
        assertTrue(result.longitude > origin.longitude)
    }

    @Test
    fun `zero magnitude leaves position unchanged`() {
        val origin = LatLng(-33.45, -70.65)

        val result = JoystickMotion.move(origin, bearingDegrees = 180f, distanceMeters = 0.0)

        assertEquals(origin, result)
    }
}
