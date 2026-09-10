package com.spoofer.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CommandArgumentsTest {
    @Test
    fun `parses a route controlled by duration`() {
        val command =
            CommandArguments.parse(
                action = CommandArguments.ACTION_START_ROUTE,
                extras =
                    mapOf(
                        CommandArguments.LATITUDE to "-33.4372",
                        CommandArguments.LONGITUDE to "-70.6506",
                        CommandArguments.DESTINATION_LATITUDE to "-33.4020",
                        CommandArguments.DESTINATION_LONGITUDE to "-70.5780",
                        CommandArguments.DURATION_SECONDS to "900",
                    ),
            )

        assertEquals(
            SpooferCommand.StartRoute(
                origin = Coordinate(-33.4372, -70.6506),
                destination = Coordinate(-33.4020, -70.5780),
                speedKmh = null,
                durationSeconds = 900,
            ),
            command,
        )
    }

    @Test
    fun `rejects coordinates outside their valid range`() {
        assertThrows(IllegalArgumentException::class.java) {
            CommandArguments.parse(
                action = CommandArguments.ACTION_SET_LOCATION,
                extras =
                    mapOf(
                        CommandArguments.LATITUDE to "91",
                        CommandArguments.LONGITUDE to "-70",
                    ),
            )
        }
    }

    @Test
    fun `rejects ambiguous route pacing`() {
        assertThrows(IllegalArgumentException::class.java) {
            CommandArguments.parse(
                action = CommandArguments.ACTION_START_ROUTE,
                extras =
                    mapOf(
                        CommandArguments.LATITUDE to "-33",
                        CommandArguments.LONGITUDE to "-70",
                        CommandArguments.DESTINATION_LATITUDE to "-34",
                        CommandArguments.DESTINATION_LONGITUDE to "-71",
                        CommandArguments.SPEED_KMH to "40",
                        CommandArguments.DURATION_SECONDS to "900",
                    ),
            )
        }
    }
}
