package org.luben93

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.mountainspotter.shared.platform.CompassService
import com.mountainspotter.shared.platform.LocationService
import com.mountainspotter.shared.platform.PermissionManager
import com.mountainspotter.shared.repository.MountainRepository
import com.mountainspotter.shared.service.MountainCalculationService
import com.mountainspotter.shared.viewmodel.MountainSpotterViewModel

// Android implementation of the getViewModel function
actual fun getViewModel(): MountainSpotterViewModel {
    return try {
        val context = org.luben93.MainApplication.INSTANCE
        createViewModel(context)
    } catch (e: Exception) {
        createPlaceholderViewModel()
    }
}

// Helper function to create a ViewModel with Android context
private fun createViewModel(context: Context): MountainSpotterViewModel {
    val locationService = getLocationService(context)
    val compassService = getCompassService(context)
    val permissionManager = getPermissionManager(context)
    val mountainRepository = getMountainRepository()
    val calculationService = com.mountainspotter.shared.service.MountainCalculationService()

    return MountainSpotterViewModel(
        locationService = locationService,
        compassService = compassService,
        permissionManager = permissionManager,
        mountainRepository = mountainRepository,
        calculationService = calculationService
    )
}

// Helper function to create a placeholder ViewModel when context isn't available yet
private fun createPlaceholderViewModel(): MountainSpotterViewModel {
    val locationService = DummyLocationService()
    val compassService = DummyCompassService()
    val permissionManager = DummyPermissionManager()
    val mountainRepository = getMountainRepository()
    val calculationService = com.mountainspotter.shared.service.MountainCalculationService()

    return MountainSpotterViewModel(
        locationService = locationService,
        compassService = compassService,
        permissionManager = permissionManager,
        mountainRepository = mountainRepository,
        calculationService = calculationService
    )
}

// Implementation of the expect functions from ViewModelFactory.kt
actual fun getLocationService(): LocationService {
    return try {
        val context = org.luben93.MainApplication.INSTANCE
        getLocationService(context)
    } catch (e: Exception) {
        DummyLocationService()
    }
}

actual fun getCompassService(): CompassService {
    return try {
        val context = org.luben93.MainApplication.INSTANCE
        getCompassService(context)
    } catch (e: Exception) {
        DummyCompassService()
    }
}

actual fun getPermissionManager(): PermissionManager {
    return try {
        val context = org.luben93.MainApplication.INSTANCE
        getPermissionManager(context)
    } catch (e: Exception) {
        DummyPermissionManager()
    }
}

actual fun getMountainRepository(): MountainRepository {
    return MountainRepository()
}

// Android-specific helper functions
private fun getLocationService(context: Context): LocationService {
    return com.mountainspotter.shared.platform.AndroidLocationService(context)
}

private fun getCompassService(context: Context): CompassService {
    return com.mountainspotter.shared.platform.AndroidCompassService(context)
}

private fun getPermissionManager(context: Context): PermissionManager {
    return com.mountainspotter.shared.platform.AndroidPermissionManager(context)
}

// Dummy implementations for when context is not available
private class DummyLocationService : LocationService {
    override fun startLocationUpdates() = kotlinx.coroutines.flow.emptyFlow()
    override fun stopLocationUpdates() {}
}

private class DummyCompassService : CompassService {
    override fun isCompassAvailable() = false
    override fun startCompassUpdates() = kotlinx.coroutines.flow.emptyFlow()
    override fun stopCompassUpdates() {}
}

private class DummyPermissionManager : PermissionManager {
    override suspend fun isLocationPermissionGranted() = false
    override suspend fun requestLocationPermission() = false
    override fun openAppSettings() {}
}
