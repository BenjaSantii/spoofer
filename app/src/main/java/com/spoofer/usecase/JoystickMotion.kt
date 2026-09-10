package com.spoofer.usecase

import org.maplibre.android.geometry.LatLng
import kotlin.math.cos
import kotlin.math.sin

object JoystickMotion {
    fun move(
        origin: LatLng,
        bearingDegrees: Float,
        distanceMeters: Double,
    ): LatLng {
        val radians = Math.toRadians(bearingDegrees.toDouble())
        val latitudeScale = METERS_TO_LATITUDE_DEGREES
        val longitudeScale = latitudeScale / cos(Math.toRadians(origin.latitude)).coerceAtLeast(0.01)
        return LatLng(
            origin.latitude + distanceMeters * cos(radians) * latitudeScale,
            origin.longitude + distanceMeters * sin(radians) * longitudeScale,
        )
    }

    private const val METERS_TO_LATITUDE_DEGREES = 1.0 / 111_320.0
}
