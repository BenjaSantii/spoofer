package com.spoofer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun SpooferMap(
    origin: LatLng?,
    destination: LatLng?,
    route: List<LatLng>,
    spoofedLocation: LatLng?,
    cameraTarget: LatLng?,
    cameraRequestId: Long,
    routeColor: Color,
    onMapClick: (LatLng) -> Unit,
    onOriginClick: () -> Unit,
    onDestinationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapControlsBottomMargin = with(LocalDensity.current) { 168.dp.roundToPx() }
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnOriginClick by rememberUpdatedState(onOriginClick)
    val currentOnDestinationClick by rememberUpdatedState(onDestinationClick)
    val mapView = remember { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleLoaded by remember { mutableStateOf(false) }

    DisposableEffect(mapView, lifecycle) {
        mapView.onCreate(null)
        var started = false
        var resumed = false

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
            started = true
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
            resumed = true
        }

        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START ->
                        if (!started) {
                            mapView.onStart()
                            started = true
                        }
                    Lifecycle.Event.ON_RESUME ->
                        if (!resumed) {
                            mapView.onResume()
                            resumed = true
                        }
                    Lifecycle.Event.ON_PAUSE ->
                        if (resumed) {
                            mapView.onPause()
                            resumed = false
                        }
                    Lifecycle.Event.ON_STOP ->
                        if (started) {
                            mapView.onStop()
                            started = false
                        }
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { mapLibreMap ->
                    map = mapLibreMap
                    mapLibreMap.uiSettings.apply {
                        isRotateGesturesEnabled = true
                        setAttributionMargins(0, 0, 0, mapControlsBottomMargin)
                        setLogoMargins(0, 0, 0, mapControlsBottomMargin)
                    }
                    mapLibreMap.addOnMapClickListener { point ->
                        currentOnMapClick(point)
                        true
                    }
                    mapLibreMap.setOnMarkerClickListener { marker ->
                        when (marker.title) {
                            "Origin" -> currentOnOriginClick()
                            "Destination" -> currentOnDestinationClick()
                        }
                        false
                    }
                    mapLibreMap.setStyle(MAP_STYLE_URL) { styleLoaded = true }
                }
            }
        },
        modifier = modifier,
    )

    LaunchedEffect(map, styleLoaded, origin, destination, route, spoofedLocation, routeColor) {
        val mapLibreMap = map ?: return@LaunchedEffect
        if (!styleLoaded) return@LaunchedEffect

        mapLibreMap.clear()
        origin?.let { mapLibreMap.addMarker(MarkerOptions().position(it).title("Origin")) }
        destination?.let { mapLibreMap.addMarker(MarkerOptions().position(it).title("Destination")) }
        spoofedLocation?.let { mapLibreMap.addMarker(MarkerOptions().position(it).title("Spoofed location")) }
        if (route.isNotEmpty()) {
            mapLibreMap.addPolyline(
                PolylineOptions()
                    .addAll(route)
                    .color(routeColor.toArgb())
                    .width(6f),
            )
        }
    }

    LaunchedEffect(map, styleLoaded, cameraRequestId) {
        val target = cameraTarget ?: return@LaunchedEffect
        if (!styleLoaded) return@LaunchedEffect
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16.0))
    }
}
