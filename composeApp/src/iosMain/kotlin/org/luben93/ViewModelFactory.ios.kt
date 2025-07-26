package org.luben93

import com.mountainspotter.shared.platform.CompassService
import com.mountainspotter.shared.platform.LocationService
import com.mountainspotter.shared.platform.PermissionManager
import com.mountainspotter.shared.repository.MountainRepository
import com.mountainspotter.shared.service.MountainCalculationService
import com.mountainspotter.shared.viewmodel.MountainSpotterViewModel

// iOS implementation of the ViewModel factory
actual fun getViewModel(): MountainSpotterViewModel {
    return MountainSpotterViewModel(
        locationService = getLocationService(),
        compassService = getCompassService(),
        permissionManager = getPermissionManager(),
        mountainRepository = getMountainRepository(),
        calculationService = MountainCalculationService()
    )
}

// Implement expect functions from ViewModelFactory.kt with proper initialization
actual fun getLocationService(): LocationService {
    val service = LocationService()
    // Make sure location service is properly initialized
    return service
}

actual fun getCompassService(): CompassService {
    val service = CompassService()
    // Ensure compass service is ready to be used
    return service
}

actual fun getPermissionManager(): PermissionManager {
    val manager = PermissionManager()
    // Initialize with any necessary context
    return manager
}

actual fun getMountainRepository(): MountainRepository {
    // Create with proper API configuration for iOS
    return MountainRepository()
}
