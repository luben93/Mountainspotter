package org.luben93

import com.mountainspotter.shared.platform.CompassService
import com.mountainspotter.shared.platform.LocationService
import com.mountainspotter.shared.platform.PermissionManager
import com.mountainspotter.shared.repository.MountainRepository
import com.mountainspotter.shared.service.MountainCalculationService
import com.mountainspotter.shared.viewmodel.MountainSpotterViewModel

// Platform-specific service factories
expect fun getLocationService(): LocationService
expect fun getCompassService(): CompassService
expect fun getPermissionManager(): PermissionManager
expect fun getMountainRepository(): MountainRepository
