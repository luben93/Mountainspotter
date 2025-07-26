package com.mountainspotter.shared.viewmodel

import com.mountainspotter.shared.model.*
import com.mountainspotter.shared.platform.LocationService
import com.mountainspotter.shared.platform.CompassService
import com.mountainspotter.shared.platform.PermissionManager
import com.mountainspotter.shared.repository.MountainRepository
import com.mountainspotter.shared.service.MountainCalculationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MountainSpotterViewModel(
    private val locationService: LocationService,
    private val compassService: CompassService,
    private val permissionManager: PermissionManager,
    private val mountainRepository: MountainRepository,
    private val calculationService: MountainCalculationService
) {
    // Define a dedicated IO dispatcher for background operations
    private val ioDispatcher = Dispatchers.Default

    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _uiState = MutableStateFlow(MountainSpotterUiState())
    val uiState: StateFlow<MountainSpotterUiState> = _uiState.asStateFlow()
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    private val _compassData = MutableStateFlow<CompassData?>(null)
    val compassData: StateFlow<CompassData?> = _compassData.asStateFlow()
    
    private val _visiblePeaks = MutableStateFlow<List<VisiblePeak>>(emptyList())
    val visiblePeaks: StateFlow<List<VisiblePeak>> = _visiblePeaks.asStateFlow()
    
    init {
        checkPermissions()
    }
    
    private fun checkPermissions() {
        viewModelScope.launch {
            val hasLocationPermission = permissionManager.isLocationPermissionGranted()
            _uiState.value = _uiState.value.copy(hasLocationPermission = hasLocationPermission)
            
            if (hasLocationPermission) {
                startServices()
            }
        }
    }
    
    fun requestLocationPermission() {
        viewModelScope.launch {
            val granted = permissionManager.requestLocationPermission()
            _uiState.value = _uiState.value.copy(hasLocationPermission = granted)
            
            if (granted) {
                startServices()
            }
        }
    }
    
    private fun startServices() {
        viewModelScope.launch {
            // Start location updates
            locationService.startLocationUpdates()
                .collect { location ->
                    _currentLocation.value = location
                    location?.let { updateVisiblePeaks(it) }
                }
        }
        
        viewModelScope.launch {
            // Start compass updates
            if (compassService.isCompassAvailable()) {
                compassService.startCompassUpdates()
                    .collect { compassData ->
                        _compassData.value = compassData
                    }
            }
        }
    }
    
    private suspend fun updateVisiblePeaks(userLocation: Location) {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Use withContext to move network operations to background thread
            val peaks = withContext(ioDispatcher) {
                mountainRepository.getPeaksNearLocation(userLocation)
            }

            // Run calculation on background thread as well
            val visiblePeaks = withContext(ioDispatcher) {
                calculationService.calculateVisiblePeaks(userLocation, peaks)
            }

            _visiblePeaks.value = visiblePeaks
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }
    
    fun refresh() {
        _currentLocation.value?.let { location ->
            viewModelScope.launch {
                updateVisiblePeaks(location)
            }
        }
    }
    
    fun openAppSettings() {
        permissionManager.openAppSettings()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun onCleared() {
        locationService.stopLocationUpdates()
        compassService.stopCompassUpdates()
    }
}

data class MountainSpotterUiState(
    val isLoading: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val error: String? = null
)
