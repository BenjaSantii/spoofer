package com.spoofer.usecase

import com.spoofer.data.DirectionsRepository
import com.spoofer.data.RouteInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.geometry.LatLng

class SpeedSimulationUseCaseTest {
    private val repository = mockk<DirectionsRepository>()
    private val simulation = SpeedSimulationUseCase(repository)

    @Test
    fun `moves along the route by the requested distance`() =
        runTest {
            val origin = LatLng(0.0, 0.0)
            val destination = LatLng(0.0, 0.001)
            coEvery { repository.getRoute(origin, destination) } returns
                RouteInfo(
                    polyline = listOf(origin, destination),
                    durationSeconds = 60,
                    distanceMeters = 111,
                )
            simulation.initialize(origin, destination)

            val result = requireNotNull(simulation.tick(50f))

            assertFalse(result.arrived)
            assertEquals(50.0, result.totalDistance, 0.001)
            assertEquals(0.00045, result.position.longitude, 0.00001)
        }

    @Test
    fun `stops at the final route point`() =
        runTest {
            val origin = LatLng(0.0, 0.0)
            val destination = LatLng(0.0, 0.001)
            coEvery { repository.getRoute(origin, destination) } returns
                RouteInfo(
                    polyline = listOf(origin, destination),
                    durationSeconds = 60,
                    distanceMeters = 111,
                )
            simulation.initialize(origin, destination)

            val result = requireNotNull(simulation.tick(200f))

            assertTrue(result.arrived)
            assertEquals(destination, result.position)
        }
}
