package com.spoofer.command

data class Coordinate(
    val latitude: Double,
    val longitude: Double,
)

sealed interface SpooferCommand {
    data class SetLocation(val location: Coordinate) : SpooferCommand

    data class StartRoute(
        val origin: Coordinate,
        val destination: Coordinate,
        val speedKmh: Float?,
        val durationSeconds: Long?,
    ) : SpooferCommand

    data object Stop : SpooferCommand
}

object CommandArguments {
    const val ACTION_SET_LOCATION = "com.benjasanti.spoofer.command.SET_LOCATION"
    const val ACTION_START_ROUTE = "com.benjasanti.spoofer.command.START_ROUTE"
    const val ACTION_STOP = "com.benjasanti.spoofer.command.STOP"

    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val DESTINATION_LATITUDE = "destination_latitude"
    const val DESTINATION_LONGITUDE = "destination_longitude"
    const val SPEED_KMH = "speed_kmh"
    const val DURATION_SECONDS = "duration_seconds"

    fun parse(
        action: String?,
        extras: Map<String, String>,
    ): SpooferCommand =
        when (action) {
            ACTION_SET_LOCATION -> SpooferCommand.SetLocation(parseCoordinate(extras, LATITUDE, LONGITUDE))
            ACTION_START_ROUTE -> parseRoute(extras)
            ACTION_STOP -> SpooferCommand.Stop
            else -> throw IllegalArgumentException("Unknown command action: $action")
        }

    private fun parseRoute(extras: Map<String, String>): SpooferCommand.StartRoute {
        val speedKmh = extras[SPEED_KMH]?.toFloatOrNull()
        val durationSeconds = extras[DURATION_SECONDS]?.toLongOrNull()
        require((speedKmh == null) != (durationSeconds == null)) {
            "Provide exactly one of speed_kmh or duration_seconds"
        }
        if (speedKmh != null) require(speedKmh > 0) { "Speed must be greater than zero" }
        if (durationSeconds != null) require(durationSeconds > 0) { "Duration must be greater than zero" }

        return SpooferCommand.StartRoute(
            origin = parseCoordinate(extras, LATITUDE, LONGITUDE),
            destination = parseCoordinate(extras, DESTINATION_LATITUDE, DESTINATION_LONGITUDE),
            speedKmh = speedKmh,
            durationSeconds = durationSeconds,
        )
    }

    private fun parseCoordinate(
        extras: Map<String, String>,
        latitudeKey: String,
        longitudeKey: String,
    ): Coordinate {
        val latitude =
            extras[latitudeKey]?.toDoubleOrNull()
                ?: throw IllegalArgumentException("Missing or invalid $latitudeKey")
        val longitude =
            extras[longitudeKey]?.toDoubleOrNull()
                ?: throw IllegalArgumentException("Missing or invalid $longitudeKey")
        require(latitude in -90.0..90.0) { "$latitudeKey must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "$longitudeKey must be between -180 and 180" }
        return Coordinate(latitude, longitude)
    }
}
