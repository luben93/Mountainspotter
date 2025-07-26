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

// Implement expect functions from ViewModelFactory.kt
actual fun getLocationService(): LocationService {
    return LocationService()
}

actual fun getCompassService(): CompassService {
    return CompassService()
}

actual fun getPermissionManager(): PermissionManager {
    return PermissionManager()
}

actual fun getMountainRepository(): MountainRepository {
    return MountainRepository()
}
