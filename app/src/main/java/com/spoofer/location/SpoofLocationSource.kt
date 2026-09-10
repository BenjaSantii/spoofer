package com.spoofer.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpoofLocationSource
    @Inject
    constructor(
        private val realLocationProvider: RealLocationProvider,
    ) {
        private var scope: CoroutineScope? = null
        private var speedCollectionJob: Job? = null

        private val _currentSpeedKmh = MutableStateFlow(0f)
        val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

        init {
            start()
        }

        private fun start() {
            scope?.cancel()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            speedCollectionJob =
                scope!!.launch {
                    realLocationProvider.getLocationUpdatesWithSpeed(1000).collect { result ->
                        val speedMs = result.speedMs
                        _currentSpeedKmh.value = if (speedMs > 0.5f) speedMs * 3.6f else 0f
                    }
                }
        }

        fun stop() {
            speedCollectionJob?.cancel()
            speedCollectionJob = null
            scope?.cancel()
            scope = null
        }
    }
