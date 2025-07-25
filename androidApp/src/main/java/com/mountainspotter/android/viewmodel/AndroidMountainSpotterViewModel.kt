package com.mountainspotter.android.viewmodel

import androidx.lifecycle.ViewModel
import com.mountainspotter.shared.model.*
import com.mountainspotter.shared.viewmodel.MountainSpotterViewModel
import com.mountainspotter.shared.viewmodel.MountainSpotterUiState
import kotlinx.coroutines.flow.*

class AndroidMountainSpotterViewModel(
    private val delegateViewModel: MountainSpotterViewModel
) : ViewModel() {
    
    val uiState: StateFlow<MountainSpotterUiState> = delegateViewModel.uiState
    val currentLocation: StateFlow<Location?> = delegateViewModel.currentLocation
    val compassData: StateFlow<CompassData?> = delegateViewModel.compassData
    val visiblePeaks: StateFlow<List<VisiblePeak>> = delegateViewModel.visiblePeaks
    
    fun requestLocationPermission() {
        delegateViewModel.requestLocationPermission()
    }
    
    fun refresh() {
        delegateViewModel.refresh()
    }
    
    fun openAppSettings() {
        delegateViewModel.openAppSettings()
    }
    
    fun clearError() {
        delegateViewModel.clearError()
    }
    
    override fun onCleared() {
        super.onCleared()
        delegateViewModel.onCleared()
    }
}
