package org.luben93

import com.mountainspotter.shared.platform.CompassService
import com.mountainspotter.shared.platform.LocationService
import com.mountainspotter.shared.platform.PermissionManager
import com.mountainspotter.shared.repository.MountainRepository
import com.mountainspotter.shared.service.MountainCalculationService
import com.mountainspotter.shared.viewmodel.MountainSpotterViewModel

// Android implementation of the ViewModel factory
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
    val context = MainApplication.INSTANCE
    return LocationService(context)
}

actual fun getCompassService(): CompassService {
    val context = MainApplication.INSTANCE
    return CompassService(context)
}

actual fun getPermissionManager(): PermissionManager {
    val context = MainApplication.INSTANCE
    return PermissionManager(context)
}

actual fun getMountainRepository(): MountainRepository {
    return MountainRepository()
}
