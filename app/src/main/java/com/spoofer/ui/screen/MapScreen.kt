package com.spoofer.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spoofer.model.SpoofMode
import com.spoofer.ui.component.JoystickOverlay
import com.spoofer.ui.component.LocationSearchBar
import com.spoofer.ui.component.MockLocationSetupDialog
import com.spoofer.ui.component.SpooferMap
import com.spoofer.ui.component.StatusChip
import com.spoofer.usecase.RoutePacing
import com.spoofer.viewmodel.FavoritesViewModel
import com.spoofer.viewmodel.MapViewModel
import com.spoofer.viewmodel.SpoofViewModel
import org.maplibre.android.geometry.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    spoofViewModel: SpoofViewModel,
    favoriteViewModel: FavoritesViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
) {
    val targetLatLng by mapViewModel.targetLatLng.collectAsState()
    val originLatLng by mapViewModel.originLatLng.collectAsState()
    val selectedMode by mapViewModel.selectedMode.collectAsState()
    val cameraPosition by mapViewModel.cameraPosition.collectAsState()
    val isSpoofing by mapViewModel.isSpoofing.collectAsState()
    val currentSpoofedLocation by mapViewModel.currentSpoofedLocation.collectAsState()
    val spoofMode by mapViewModel.spoofMode.collectAsState()
    val elapsedSeconds by mapViewModel.elapsedSeconds.collectAsState()
    val speedKmh by mapViewModel.speedKmh.collectAsState()
    val speedMode by mapViewModel.speedMode.collectAsState()
    val durationMinutes by mapViewModel.durationMinutes.collectAsState()
    val currentSpeedKmh by mapViewModel.currentSpeedKmh.collectAsState()
    val transportMode by mapViewModel.transportMode.collectAsState()
    val joySpeedKmh by mapViewModel.joySpeedKmh.collectAsState()
    val totalDistanceTraveled by mapViewModel.totalDistanceTraveled.collectAsState()
    val currentHeading by mapViewModel.currentHeading.collectAsState()

    val routeInfo by spoofViewModel.routeInfo.collectAsState()
    val routePreview by spoofViewModel.routePreview.collectAsState()
    val remainingDistance by spoofViewModel.remainingDistance.collectAsState()
    val isLoadingRoute by spoofViewModel.isLoadingRoute.collectAsState()
    val routeError by spoofViewModel.routeError.collectAsState()
    val showSetupDialog by spoofViewModel.showSetupDialog.collectAsState()

    val favorites by favoriteViewModel.favorites.collectAsState()

    val haptic = LocalHapticFeedback.current
    val scaffoldState = rememberBottomSheetScaffoldState()

    var showFavoritesSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveDialogName by remember { mutableStateOf("") }
    var originText by remember { mutableStateOf("My Location") }
    var destText by remember { mutableStateOf("") }
    var routePointSelection by remember { mutableStateOf(RoutePointSelection.DESTINATION) }
    var mapCameraTarget by remember { mutableStateOf<LatLng?>(null) }
    var mapCameraRequestId by remember { mutableStateOf(0L) }
    val favoritesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Bug 12 fix: remember the last known mode so StatusChip has content during its
    // exit animation (spoofMode becomes null before the animation completes).
    var lastKnownSpoofMode by remember { mutableStateOf<SpoofMode?>(null) }
    LaunchedEffect(spoofMode) { if (spoofMode != null) lastKnownSpoofMode = spoofMode }

    val isJoystickActive = isSpoofing && spoofMode == SpoofMode.JOYSTICK
    val isJoystickPreview = !isSpoofing && spoofMode == SpoofMode.JOYSTICK

    val onStartStop: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (isSpoofing) {
            spoofViewModel.stopSpoofing()
        } else {
            when (selectedMode) {
                SpoofMode.STATIC ->
                    targetLatLng?.let {
                        spoofViewModel.startStaticSpoof(it)
                    }
                SpoofMode.DIRECTIONS -> {
                    val origin = originLatLng ?: cameraPosition
                    val dest = targetLatLng
                    val routeSpeedMps =
                        when (speedMode) {
                            com.spoofer.model.SpeedMode.MANUAL -> speedKmh / 3.6f
                            com.spoofer.model.SpeedMode.CURRENT -> currentSpeedKmh / 3.6f
                            com.spoofer.model.SpeedMode.DURATION ->
                                routeInfo?.takeIf { it.distanceMeters > 0 }?.let {
                                    RoutePacing.speedMetersPerSecond(
                                        distanceMeters = it.distanceMeters.toDouble(),
                                        durationSeconds = (durationMinutes * 60).toLong(),
                                    )
                                }
                        }
                    if (origin != null && dest != null && routeSpeedMps != null && routeSpeedMps > 0) {
                        spoofViewModel.startDirectionsSpoof(
                            origin = origin,
                            destination = dest,
                            speedMps = routeSpeedMps,
                            durationSeconds =
                                if (speedMode == com.spoofer.model.SpeedMode.DURATION) {
                                    (durationMinutes * 60).toLong()
                                } else {
                                    null
                                },
                        )
                    }
                }
                SpoofMode.JOYSTICK ->
                    originLatLng?.let { origin ->
                        spoofViewModel.startJoystick(origin, joySpeedKmh / 3.6f)
                    }
            }
        }
    }

    LaunchedEffect(isSpoofing) {
        if (isSpoofing && currentSpoofedLocation != null) {
            mapCameraTarget = currentSpoofedLocation
            mapCameraRequestId++
        }
    }

    LaunchedEffect(originLatLng) {
        // Bug 14 fix: sync the origin text field to real coordinates when the
        // GPS-derived origin loads, instead of showing the literal "My Location" string.
        if (originText == "My Location" && originLatLng != null) {
            originText = "%.5f, %.5f".format(originLatLng!!.latitude, originLatLng!!.longitude)
        }
    }
    LaunchedEffect(Unit) {
        mapViewModel.loadInitialLocation()
        spoofViewModel.checkMockLocationProvider()
    }
    LaunchedEffect(cameraPosition, isSpoofing) {
        // Bug 2 fix: don't move the camera to the real GPS position while spoofing is
        // active — doing so fights the spoofed-location camera effect and causes a
        // brief rubber-band snap back to the real location.
        if (!isSpoofing && cameraPosition != null) {
            mapCameraTarget = cameraPosition
            mapCameraRequestId++
        }
    }

    LaunchedEffect(selectedMode, originLatLng, targetLatLng) {
        if (selectedMode == SpoofMode.DIRECTIONS && originLatLng != null && targetLatLng != null) {
            spoofViewModel.fetchRoutePreview(originLatLng!!, targetLatLng!!)
        } else {
            spoofViewModel.clearRoutePreview()
        }
    }

    LaunchedEffect(isJoystickActive, currentSpoofedLocation) {
        if (isJoystickActive && currentSpoofedLocation != null) {
            mapCameraTarget = currentSpoofedLocation
            mapCameraRequestId++
        }
    }
    LaunchedEffect(isJoystickActive, joySpeedKmh) {
        if (isJoystickActive) spoofViewModel.updateJoystickSpeed(joySpeedKmh / 3.6f)
    }

    LaunchedEffect(selectedMode) {
        if (selectedMode == SpoofMode.DIRECTIONS) {
            spoofViewModel.clearRoutePreview()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 144.dp,
        sheetShape = MaterialTheme.shapes.extraLarge,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        containerColor = MaterialTheme.colorScheme.background,
        sheetContent = {
            BottomSheetContent(
                selectedMode = selectedMode,
                onModeSelected = { mapViewModel.setMode(it) },
                targetLatLng = targetLatLng,
                isSpoofing = isSpoofing,
                onSaveFavorite = { showSaveDialog = true },
                originText = originText, destText = destText,
                onOriginTextChange = { originText = it },
                onDestTextChange = { destText = it },
                onOriginSelected = { latLng ->
                    mapViewModel.setOrigin(latLng)
                    routePointSelection = RoutePointSelection.DESTINATION
                },
                onSwap = { mapViewModel.swapOriginAndDestination() },
                onDestSelected = { latLng ->
                    mapViewModel.setTarget(latLng)
                },
                onSearchPlace = { query -> mapViewModel.searchPlaces(query) },
                speedKmh = speedKmh, onSpeedChange = { mapViewModel.setSpeedKmh(it) },
                speedMode = speedMode, onSpeedModeChange = { mapViewModel.setSpeedMode(it) },
                durationMinutes = durationMinutes,
                onDurationChange = { mapViewModel.setDurationMinutes(it) },
                currentSpeedKmh = currentSpeedKmh,
                routePointSelection = routePointSelection,
                onRoutePointSelectionChange = { routePointSelection = it },
                transportMode = transportMode, onTransportModeChange = { mapViewModel.setTransportMode(it) },
                routeInfo = routeInfo, remainingDistance = remainingDistance,
                isLoadingRoute = isLoadingRoute,
                routeError = routeError,
                joySpeedKmh = joySpeedKmh, onJoySpeedChange = { mapViewModel.setJoySpeedKmh(it) },
                totalDistanceTraveled = totalDistanceTraveled, currentHeading = currentHeading,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            SpooferMap(
                modifier = Modifier.fillMaxSize(),
                origin = originLatLng,
                destination = targetLatLng,
                route = routePreview,
                spoofedLocation = currentSpoofedLocation,
                cameraTarget = mapCameraTarget,
                cameraRequestId = mapCameraRequestId,
                routeColor = MaterialTheme.colorScheme.primary,
                onMapClick = { latLng ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (selectedMode == SpoofMode.DIRECTIONS) {
                        when (routePointSelection) {
                            RoutePointSelection.ORIGIN -> {
                                mapViewModel.setOrigin(latLng)
                                routePointSelection = RoutePointSelection.DESTINATION
                            }
                            RoutePointSelection.DESTINATION -> {
                                mapViewModel.setTarget(latLng)
                            }
                        }
                    } else {
                        mapViewModel.setTarget(latLng)
                    }
                },
                onOriginClick = { routePointSelection = RoutePointSelection.ORIGIN },
                onDestinationClick = { routePointSelection = RoutePointSelection.DESTINATION },
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp),
            ) {
                LocationSearchBar(
                    onLocationSelected = { latLng ->
                        mapViewModel.setTarget(latLng)
                        mapCameraTarget = latLng
                        mapCameraRequestId++
                    },
                    onFavoritesClick = { showFavoritesSheet = true },
                    onHistoryClick = onNavigateToHistory,
                    onSettingsClick = onNavigateToSettings,
                    onSearch = { query -> mapViewModel.searchPlaces(query) },
                )
                Spacer(Modifier.height(8.dp))
                // Bug 12 fix: drive AnimatedVisibility with isSpoofing, but render
                // lastKnownSpoofMode (non-null) so the chip has content during exit.
                lastKnownSpoofMode?.let { mode ->
                    StatusChip(
                        mode = mode,
                        elapsedSeconds = elapsedSeconds,
                        isActive = isSpoofing,
                    )
                }
            }

            JoystickOverlay(
                isActive = isJoystickActive,
                isPreview = isJoystickPreview,
                onInput = { spoofViewModel.updateJoystick(it.angle, it.magnitude, joySpeedKmh / 3.6f) },
            )

            val fabInteraction = remember { MutableInteractionSource() }
            val fabPressed by fabInteraction.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue = if (fabPressed) 0.96f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
                label = "fab_scale",
            )

            SmallFloatingActionButton(
                onClick = {
                    // Bug 11 fix: refresh the real GPS position so the camera moves
                    // to where the device actually is, not just the current camera center.
                    mapViewModel.loadInitialLocation()
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        // Bug 10 fix: use navigationBarsPadding() so the FAB clears the
                        // gesture navigation bar on edge-to-edge devices.
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 96.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation =
                    FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                    ),
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(24.dp))
            }

            ExtendedFloatingActionButton(
                onClick = onStartStop,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        // Bug 10 fix: use navigationBarsPadding() instead of hardcoded
                        // 80dp so the FAB sits above the gesture/nav bar on all devices.
                        .navigationBarsPadding()
                        .padding(bottom = 80.dp)
                        .scale(fabScale),
                interactionSource = fabInteraction,
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor =
                    if (isSpoofing) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                contentColor =
                    if (isSpoofing) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                elevation =
                    FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp,
                    ),
            ) {
                Icon(
                    if (isSpoofing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isSpoofing) "Stop" else "Start",
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (isSpoofing) "Stop spoofing" else "Start spoofing",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    if (showFavoritesSheet) {
        FavoritesSheet(
            favorites = favorites,
            onSelect = { location ->
                val ll = LatLng(location.latitude, location.longitude)
                mapViewModel.setTarget(ll)
                mapCameraTarget = ll
                mapCameraRequestId++
                showFavoritesSheet = false
            },
            onDelete = { favoriteViewModel.delete(it) },
            onDismiss = { showFavoritesSheet = false },
            sheetState = favoritesSheetState,
        )
    }

    if (showSaveDialog && targetLatLng != null) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                saveDialogName = ""
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Save Location") },
            text = {
                OutlinedTextField(
                    value = saveDialogName,
                    onValueChange = { saveDialogName = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Home, Work, Park") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (saveDialogName.isNotBlank()) {
                        favoriteViewModel.save(saveDialogName.trim(), targetLatLng!!.latitude, targetLatLng!!.longitude)
                        showSaveDialog = false
                        saveDialogName = ""
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    saveDialogName = ""
                }) { Text("Cancel") }
            },
        )
    }

    if (showSetupDialog) MockLocationSetupDialog(onDismiss = { spoofViewModel.dismissSetupDialog() })
}

enum class RoutePointSelection {
    ORIGIN,
    DESTINATION,
}
