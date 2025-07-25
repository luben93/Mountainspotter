import SwiftUI
import shared
import Combine

class IOSMountainSpotterViewModel: ObservableObject {
    @Published var hasLocationPermission = false
    @Published var isLoading = false
    @Published var error: String?
    @Published var currentLocation: shared.Location?
    @Published var compassData: CompassData?
    @Published var visiblePeaks: [VisiblePeak] = []
    
    private var mountainSpotterViewModel: MountainSpotterViewModel?
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        setupViewModel()
    }
    
    private func setupViewModel() {
        mountainSpotterViewModel = MountainSpotterViewModel(
            locationService: LocationService(),
            compassService: CompassService(),
            permissionManager: PermissionManager(),
            mountainRepository: MountainRepository(),
            calculationService: MountainCalculationService()
        )
        
        // Observe the shared viewmodel state
        mountainSpotterViewModel?.uiState.collect { [weak self] uiState in
            DispatchQueue.main.async {
                self?.hasLocationPermission = uiState.hasLocationPermission
                self?.isLoading = uiState.isLoading
                self?.error = uiState.error
            }
        }
        
        mountainSpotterViewModel?.currentLocation.collect { [weak self] location in
            DispatchQueue.main.async {
                self?.currentLocation = location
            }
        }
        
        mountainSpotterViewModel?.compassData.collect { [weak self] compassData in
            DispatchQueue.main.async {
                self?.compassData = compassData
            }
        }
        
        mountainSpotterViewModel?.visiblePeaks.collect { [weak self] peaks in
            DispatchQueue.main.async {
                self?.visiblePeaks = peaks
            }
        }
    }
    
    func checkPermissions() {
        // This will be handled automatically by the shared viewmodel
    }
    
    func requestLocationPermission() {
        mountainSpotterViewModel?.requestLocationPermission()
    }
    
    func refresh() {
        mountainSpotterViewModel?.refresh()
    }
    
    deinit {
        mountainSpotterViewModel?.onCleared()
    }
}

// Extension to make Kotlin Flow observable in Swift
extension Kotlinx_coroutines_coreFlow {
    func collect<T>(
        onEach: @escaping (T) -> Void
    ) {
        let collector = FlowCollector<T> { value in
            onEach(value)
        }
        
        self.collect(collector: collector) { error in
            if let error = error {
                print("Flow collection error: \(error)")
            }
        }
    }
}

class FlowCollector<T>: Kotlinx_coroutines_coreFlowCollector {
    let onEach: (T) -> Void
    
    init(onEach: @escaping (T) -> Void) {
        self.onEach = onEach
    }
    
    func emit(value: Any?) async throws {
        if let value = value as? T {
            onEach(value)
        }
    }
}
