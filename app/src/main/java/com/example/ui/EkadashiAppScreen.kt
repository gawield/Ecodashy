package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.astro.EkadashiEvent
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkadashiAppScreen(
    viewModel: EkadashiViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Runtime location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.tryDetectGps(fallbackToPreset = false)
        } else {
            viewModel.setLocation(uiState.selectedLocation)
        }
    }

    var selectedDetailEvent by remember { mutableStateOf<EkadashiEvent?>(null) }
    val displayHeroEvent = selectedDetailEvent ?: uiState.nextEvent

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ekadashi_app_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                LocationHeader(
                    location = uiState.selectedLocation,
                    isLoading = uiState.isLoading,
                    statusMessage = uiState.locationStatusMessage,
                    onRefreshGps = {
                        val finePerm = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarsePerm = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (finePerm || coarsePerm) {
                            viewModel.tryDetectGps(fallbackToPreset = false)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onChangeLocation = { viewModel.toggleLocationPicker(true) },
                    onOpenGuide = { viewModel.toggleFastingGuide(true) },
                    onDismissStatusMessage = { viewModel.clearStatusMessage() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // If user selected an item from list, show a return button
                if (selectedDetailEvent != null && selectedDetailEvent != uiState.nextEvent) {
                    item {
                        OutlinedButton(
                            onClick = { selectedDetailEvent = null },
                            modifier = Modifier.fillMaxWidth().testTag("back_to_next_button")
                        ) {
                            Text("← Return to Nearest Upcoming Ekadashi")
                        }
                    }
                }

                // 1. Next / Selected Ekadashi Hero Card
                if (displayHeroEvent != null) {
                    item {
                        HeroEkadashiCard(
                            event = displayHeroEvent,
                            countdownText = if (displayHeroEvent == uiState.nextEvent) uiState.countdownText else ""
                        )
                    }

                    // 2. Parana Highlight Card
                    item {
                        ParanaHighlightCard(event = displayHeroEvent)
                    }
                }

                // 3. Astronomical Core Mini Dashboard
                item {
                    AstroMiniDashboard(
                        snapshot = uiState.astroSnapshot,
                        onOpenInspector = { viewModel.toggleAstroInspector(true) }
                    )
                }

                // 4. Upcoming Ekadashis list
                if (uiState.upcomingEvents.isNotEmpty()) {
                    item {
                        UpcomingEkadashiList(
                            events = uiState.upcomingEvents,
                            selectedId = displayHeroEvent?.id,
                            onSelectEvent = { event ->
                                selectedDetailEvent = event
                            }
                        )
                    }
                }
            }

            // Loading overlay
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Computing ephemeris...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets & Dialogs
    if (uiState.showLocationPicker) {
        LocationPickerSheet(
            currentLocation = uiState.selectedLocation,
            onSelectLocation = { point -> viewModel.setLocation(point) },
            onDetectGps = {
                val finePerm = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (finePerm) {
                    viewModel.tryDetectGps(fallbackToPreset = false)
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            onOpenCustomDialog = { viewModel.toggleCustomCoordsDialog(true) },
            onDismiss = { viewModel.toggleLocationPicker(false) }
        )
    }

    if (uiState.showCustomCoordsDialog) {
        CustomCoordinatesDialog(
            initialPoint = uiState.selectedLocation,
            onConfirm = { name, lat, lon, zone ->
                viewModel.setCustomCoordinates(name, lat, lon, zone)
                viewModel.toggleCustomCoordsDialog(false)
            },
            onDismiss = { viewModel.toggleCustomCoordsDialog(false) }
        )
    }

    if (uiState.showAstroInspector) {
        AstroInspectorSheet(
            snapshot = uiState.astroSnapshot,
            onDismiss = { viewModel.toggleAstroInspector(false) }
        )
    }

    if (uiState.showFastingGuide) {
        FastingGuideDialog(
            onDismiss = { viewModel.toggleFastingGuide(false) }
        )
    }
}
