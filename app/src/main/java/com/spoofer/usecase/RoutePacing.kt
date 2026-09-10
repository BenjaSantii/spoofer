package com.spoofer.usecase

object RoutePacing {
    fun speedMetersPerSecond(
        distanceMeters: Double,
        durationSeconds: Long,
    ): Float {
        require(distanceMeters > 0) { "Distance must be greater than zero" }
        require(durationSeconds > 0) { "Duration must be greater than zero" }
        return (distanceMeters / durationSeconds).toFloat()
    }
}
