package org.luben93

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mountainspotter.shared.model.CompassData
import com.mountainspotter.shared.model.Location
import com.mountainspotter.shared.model.VisiblePeak
import com.mountainspotter.shared.viewmodel.MountainSpotterViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun App() {
    val viewModel = getViewModel()
    val uiState by viewModel.uiState.collectAsState(initial = com.mountainspotter.shared.viewmodel.MountainSpotterUiState())
    val visiblePeaks by viewModel.visiblePeaks.collectAsState(initial = emptyList())
    val compassData by viewModel.compassData.collectAsState(initial = null)
    val currentLocation by viewModel.currentLocation.collectAsState(initial = null)

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!uiState.hasLocationPermission) {
                PermissionRequest(
                    onRequestPermission = { viewModel.requestLocationPermission() },
                    onOpenSettings = { viewModel.openAppSettings() }
                )
            } else {
                MountainSpotterContent(
                    isLoading = uiState.isLoading,
                    visiblePeaks = visiblePeaks,
                    compassData = compassData,
                    currentLocation = currentLocation,
                    onRefresh = { viewModel.refresh() },
                    errorMessage = uiState.error,
                    onClearError = { viewModel.clearError() }
                )
            }
        }
    }
    
    // Cleanup when the composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onCleared()
        }
    }
}

@Composable
fun PermissionRequest(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Location permission is required to use Mountain Spotter")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) {
            Text("Open Settings")
        }
    }
}

@Composable
fun MountainSpotterContent(
    isLoading: Boolean,
    visiblePeaks: List<VisiblePeak>,
    compassData: CompassData?,
    currentLocation: Location?,
    onRefresh: () -> Unit,
    errorMessage: String?,
    onClearError: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Header with location info
            currentLocation?.let {
                Text(
                    "Current Location: ${it.latitude.formatDecimal(5)}° N, ${it.longitude.formatDecimal(5)}° E",
                    style = MaterialTheme.typography.titleMedium
                )
                it.altitude?.let { alt ->
                    Text(
                        "Altitude: ${alt.formatDecimal(1)} m",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            compassData?.let {
                Text(
                    "Compass: ${it.azimuth.formatDecimal(1)}°",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // List of visible peaks
            Text(
                "Visible Peaks (${visiblePeaks.size})",
                style = MaterialTheme.typography.titleLarge
            )
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
                )
            } else if (visiblePeaks.isEmpty()) {
                Text(
                    "No peaks found nearby. Try refreshing or move to a different location.",
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                visiblePeaks.forEach { peak ->
                    PeakItem(peak)
                }
            }
        }
        
        // Floating action button for refresh
        FloatingActionButton(
            onClick = onRefresh,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            // Replace with appropriate icon
            Text("↻")
        }
        
        // Error message
        errorMessage?.let {
            AlertDialog(
                onDismissRequest = onClearError,
                title = { Text("Error") },
                text = { Text(it) },
                confirmButton = {
                    Button(onClick = onClearError) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun PeakItem(peak: VisiblePeak) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                peak.peak.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Elevation: ${peak.peak.elevation.formatDecimal(0)} m",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Distance: ${peak.distance.formatDecimal(1)} km",
                style = MaterialTheme.typography.bodyMedium
            )
            peak.peak.country?.let {
                Text(
                    "Country: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Helper functions to format Double and Float values with specific decimal places
expect fun Double.formatDecimal(decimals: Int): String
expect fun Float.formatDecimal(decimals: Int): String

// Platform-specific function to get the ViewModel
expect fun getViewModel(): MountainSpotterViewModel
