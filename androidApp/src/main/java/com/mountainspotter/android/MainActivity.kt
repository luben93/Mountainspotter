package com.mountainspotter.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.mountainspotter.android.ui.CameraView
import com.mountainspotter.android.ui.theme.MountainSpotterTheme
import com.mountainspotter.android.viewmodel.AndroidMountainSpotterViewModel
import com.mountainspotter.shared.model.VisiblePeak
import com.mountainspotter.shared.platform.CompassService
import com.mountainspotter.shared.platform.LocationService
import com.mountainspotter.shared.platform.PermissionManager
import com.mountainspotter.shared.repository.MountainRepository
import com.mountainspotter.shared.service.MountainCalculationService
import com.mountainspotter.shared.viewmodel.MountainSpotterViewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MountainSpotterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    
                    val viewModel: AndroidMountainSpotterViewModel = viewModel {
                        AndroidMountainSpotterViewModel(
                            MountainSpotterViewModel(
                                locationService = LocationService(context),
                                compassService = CompassService(context),
                                permissionManager = PermissionManager(context),
                                mountainRepository = MountainRepository(),
                                calculationService = MountainCalculationService()
                            )
                        )
                    }
                    
                    val permissionState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.CAMERA
                        )
                    )
                    
                    LaunchedEffect(permissionState.allPermissionsGranted) {
                        if (permissionState.permissions.find { 
                            it.permission == android.Manifest.permission.ACCESS_FINE_LOCATION 
                        }?.status?.isGranted == true) {
                            viewModel.requestLocationPermission()
                        }
                    }
                    
                    var showCameraView by remember { mutableStateOf(false) }
                    
                    if (showCameraView) {
                        // Camera View
                        val uiState by viewModel.uiState.collectAsState()
                        val currentLocation by viewModel.currentLocation.collectAsState()
                        val compassData by viewModel.compassData.collectAsState()
                        val visiblePeaks by viewModel.visiblePeaks.collectAsState()
                        
                        CameraView(
                            visiblePeaks = visiblePeaks,
                            currentLocation = currentLocation,
                            compassData = compassData,
                            onBack = { showCameraView = false }
                        )
                    } else {
                        // List View
                        MountainSpotterScreen(
                            viewModel = viewModel,
                            onRequestPermission = {
                                permissionState.launchMultiplePermissionRequest()
                            },
                            hasLocationPermission = permissionState.permissions.find { 
                                it.permission == android.Manifest.permission.ACCESS_FINE_LOCATION 
                            }?.status?.isGranted == true,
                            hasCameraPermission = permissionState.permissions.find { 
                                it.permission == android.Manifest.permission.CAMERA 
                            }?.status?.isGranted == true,
                            onShowCameraView = { showCameraView = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MountainSpotterScreen(
    viewModel: AndroidMountainSpotterViewModel,
    onRequestPermission: () -> Unit,
    hasLocationPermission: Boolean,
    hasCameraPermission: Boolean,
    onShowCameraView: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val compassData by viewModel.compassData.collectAsState()
    val visiblePeaks by viewModel.visiblePeaks.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mountain Spotter",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Camera View Button
            if (hasLocationPermission && hasCameraPermission) {
                IconButton(onClick = onShowCameraView) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Permission handling
        if (!hasLocationPermission || !hasCameraPermission) {
            PermissionRequest(
                onRequestPermission = onRequestPermission,
                hasLocationPermission = hasLocationPermission,
                hasCameraPermission = hasCameraPermission
            )
            return
        }
        
        // Location info
        LocationInfo(
            location = currentLocation,
            compassData = compassData
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Loading/Error states
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Refresh button
        Button(
            onClick = { viewModel.refresh() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Peaks")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Visible peaks list
        Text(
            text = "Visible Peaks (${visiblePeaks.count { it.isVisible }})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(visiblePeaks.filter { it.isVisible }) { peak ->
                PeakCard(peak = peak)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PermissionRequest(
    onRequestPermission: () -> Unit,
    hasLocationPermission: Boolean,
    hasCameraPermission: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!hasLocationPermission) {
                Text(
                    text = "• Location access to show nearby mountain peaks",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (!hasCameraPermission) {
                Text(
                    text = "• Camera access for the horizon view",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
fun LocationInfo(
    location: com.mountainspotter.shared.model.Location?,
    compassData: com.mountainspotter.shared.model.CompassData?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            location?.let {
                Text("Location: ${it.latitude.toString().take(8)}, ${it.longitude.toString().take(8)}")
                it.altitude?.let { alt ->
                    Text("Altitude: ${alt.roundToInt()}m")
                }
            } ?: Text("Location: Not available")
            
            compassData?.let {
                Text("Bearing: ${it.azimuth.roundToInt()}°")
                Text("Pitch: ${it.pitch.roundToInt()}°")
            } ?: Text("Compass: Not available")
        }
    }
}

@Composable
fun PeakCard(peak: VisiblePeak) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = peak.peak.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "${peak.peak.elevation.roundToInt()}m elevation",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Distance: ${(peak.distance * 10).roundToInt() / 10.0}km")
                Text("Bearing: ${peak.bearing.roundToInt()}°")
            }
            
            Text("Elevation Angle: ${(peak.elevationAngle * 10).roundToInt() / 10.0}°")
            
            peak.peak.country?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
