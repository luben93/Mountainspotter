import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = IOSMountainSpotterViewModel()
    
    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                Text("Mountain Spotter")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .padding(.horizontal)
                
                // Permission handling
                if !viewModel.hasLocationPermission {
                    PermissionRequestView {
                        viewModel.requestLocationPermission()
                    }
                    .padding(.horizontal)
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            // Location info
                            LocationInfoView(
                                location: viewModel.currentLocation,
                                compassData: viewModel.compassData
                            )
                            .padding(.horizontal)
                            
                            // Loading/Error states
                            if viewModel.isLoading {
                                HStack {
                                    Spacer()
                                    ProgressView()
                                    Spacer()
                                }
                                .padding()
                            }
                            
                            if let error = viewModel.error {
                                Text("Error: \(error)")
                                    .foregroundColor(.red)
                                    .padding()
                                    .background(Color.red.opacity(0.1))
                                    .cornerRadius(8)
                                    .padding(.horizontal)
                            }
                            
                            // Refresh button
                            Button("Refresh Peaks") {
                                viewModel.refresh()
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                            .padding(.horizontal)
                            
                            // Visible peaks
                            Text("Visible Peaks (\(viewModel.visiblePeaks.filter { $0.isVisible }.count))")
                                .font(.title2)
                                .fontWeight(.semibold)
                                .padding(.horizontal)
                            
                            LazyVStack(spacing: 8) {
                                ForEach(viewModel.visiblePeaks.filter { $0.isVisible }, id: \.peak.id) { peak in
                                    PeakCardView(peak: peak)
                                        .padding(.horizontal)
                                }
                            }
                        }
                    }
                }
                
                Spacer()
            }
        }
        .onAppear {
            viewModel.checkPermissions()
        }
    }
}

struct PermissionRequestView: View {
    let onRequestPermission: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Location Permission Required")
                .font(.title2)
                .fontWeight(.semibold)
            
            Text("This app needs location access to show you nearby mountain peaks.")
                .font(.body)
            
            Button("Grant Location Permission") {
                onRequestPermission()
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.blue)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
        .padding()
        .background(Color.gray.opacity(0.1))
        .cornerRadius(12)
    }
}

struct LocationInfoView: View {
    let location: shared.Location?
    let compassData: CompassData?
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Current Status")
                .font(.title2)
                .fontWeight(.semibold)
            
            if let location = location {
                Text("Location: \(String(format: "%.6f", location.latitude.doubleValue)), \(String(format: "%.6f", location.longitude.doubleValue))")
                if let altitude = location.altitude {
                    Text("Altitude: \(Int(altitude.doubleValue.rounded()))m")
                }
            } else {
                Text("Location: Not available")
            }
            
            if let compass = compassData {
                Text("Bearing: \(Int(Double(compass.azimuth).rounded()))°")
                Text("Pitch: \(Int(Double(compass.pitch).rounded()))°")
            } else {
                Text("Compass: Not available")
            }
        }
        .padding()
        .background(Color.gray.opacity(0.1))
        .cornerRadius(12)
    }
}

struct PeakCardView: View {
    let peak: VisiblePeak
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(peak.peak.name)
                .font(.title3)
                .fontWeight(.semibold)
            
            Text("\(Int(peak.peak.elevation.doubleValue.rounded()))m elevation")
                .font(.body)
                .foregroundColor(.secondary)
            
            HStack {
                Text("Distance: \(String(format: "%.1f", peak.distance.doubleValue))km")
                Spacer()
                Text("Bearing: \(Int(peak.bearing.doubleValue.rounded()))°")
            }
            
            Text("Elevation Angle: \(String(format: "%.1f", peak.elevationAngle.doubleValue))°")
            
            if let country = peak.peak.country {
                Text(country)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color.white)
        .cornerRadius(12)
        .shadow(radius: 2)
    }
}

#Preview {
    ContentView()
}
