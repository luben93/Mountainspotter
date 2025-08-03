# AI-Enhanced Mountain Spotter - New Features

This document outlines the newly implemented AI-based camera calibration and zoom features.

## New Features Implemented

### 1. AI-Based Camera Calibration
- **Location**: `shared/src/commonMain/kotlin/com/mountainspotter/shared/service/CameraCalibrationService.kt`
- **Purpose**: Automatically determines optimal camera parameters using simulated neural network analysis
- **Features**:
  - Automatic field of view estimation
  - Compass correction calculation
  - Horizon detection and translation adjustment
  - Confidence scoring for calibration results

### 2. Enhanced Camera Controls
- **Location**: `composeApp/src/commonMain/kotlin/org/luben93/CameraView.kt`
- **New Controls**:
  - 🤖 AI Calibration button - Triggers automatic parameter estimation
  - ↺ Reset button - Resets calibration to manual mode
  - Visual feedback showing calibration status (Green = AI Calibrated, Yellow = Manual)

### 3. Zoom and Gesture Support
- **Pinch-to-zoom**: Users can zoom in/out on the camera view (1x to 5x)
- **Pan gestures**: Fine-tune translation for better peak alignment
- **Dynamic FOV**: Field of view adjusts automatically with zoom level
- **Visual indicators**: Real-time display of zoom level and FOV

### 4. Enhanced Peak Overlay
- **Location**: `composeApp/src/commonMain/kotlin/org/luben93/HorizonOverlay.kt`
- **Improvements**:
  - Uses AI-determined camera parameters for accurate peak positioning
  - Adaptive text size based on zoom level
  - Color coding: Red (AI calibrated), Magenta (manual mode)
  - Horizon line changes color to indicate calibration status

### 5. Comprehensive UI Feedback
- **Real-time parameters display**:
  - Current zoom level
  - Field of view (FOV)
  - Compass correction values
  - Translation offsets
  - Calibration status indicator

## How to Use

### Basic Usage
1. Open the Mountain Spotter app
2. Grant location permissions
3. Tap the camera button (📷) to open camera view
4. View mountains overlaid on the camera feed

### AI Calibration
1. In camera view, tap the AI button (🤖)
2. Wait ~1 second for AI processing
3. Observe the improved peak alignment
4. Notice the status changes to "AI Calibrated ✓"

### Zoom and Adjustment
1. Use pinch gestures to zoom in/out (1x to 5x)
2. Pan gestures to fine-tune positioning
3. Tap reset (↺) to return to manual mode
4. Re-calibrate as needed

## Technical Details

### AI Simulation
The current implementation simulates a local neural network for demonstration purposes. In a production environment, this would be replaced with:
- TensorFlow Lite model for mobile devices
- Camera frame analysis for feature detection
- Real horizon and peak detection algorithms
- Machine learning-based parameter optimization

### Performance
- AI calibration: ~1 second processing time
- Gesture response: Real-time updates
- Peak filtering: Optimized for up to 40 visible peaks
- Memory usage: Minimal additional overhead

### Testing
- Comprehensive unit tests in `CameraCalibrationServiceTest.kt`
- Tests cover zoom clamping, translation limits, and calibration logic
- All new tests pass successfully

## Visual Indicators

### Calibration Status
- **Green horizon line**: AI calibrated
- **Yellow horizon line**: Manual mode
- **Green compass**: AI calibrated
- **Yellow compass**: Manual mode

### Peak Colors
- **Red peaks**: AI calibrated mode
- **Magenta peaks**: Manual mode

### UI Elements
- **Zoom display**: Shows current zoom level (e.g., "2.5x")
- **FOV display**: Shows field of view (e.g., "36°")
- **Status indicator**: "AI Calibrated ✓" or "Manual Mode"

This implementation provides a solid foundation for AI-enhanced mountain peak identification with user-friendly controls and comprehensive visual feedback.