package org.luben93

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.Foundation.NSError
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    isFrontCamera: Boolean,
    onSwitchCamera: () -> Unit
) {
    var captureSession by remember { mutableStateOf<AVCaptureSession?>(null) }
    var previewLayer by remember { mutableStateOf<AVCaptureVideoPreviewLayer?>(null) }
    val cameraPosition = if (isFrontCamera) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack
    var permissionGranted by remember { mutableStateOf(false) }
    var sessionRunning by remember { mutableStateOf(false) }

    // Request camera permission first
    LaunchedEffect(Unit) {
        println("CameraPreview: Requesting camera permission")
        val authStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        println("CameraPreview: Current auth status: $authStatus")

        when (authStatus) {
            AVAuthorizationStatusAuthorized -> {
                println("CameraPreview: Camera permission already granted")
                permissionGranted = true
            }
            AVAuthorizationStatusNotDetermined -> {
                println("CameraPreview: Requesting camera permission")
                // Use suspendCancellableCoroutine for proper async handling
                permissionGranted = suspendCancellableCoroutine { continuation ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        println("CameraPreview: Permission result: $granted")
                        // Resume on main thread to ensure proper state updates
                        launch(Dispatchers.Main) {
                            continuation.resume(granted, onCancellation = {})
                        }
                    }
                }
            }
            else -> {
                println("CameraPreview: Camera permission denied or restricted")
                permissionGranted = false
            }
        }
    }

    // Setup camera when permission is granted or camera position changes
    LaunchedEffect(permissionGranted, cameraPosition) {
        if (permissionGranted) {
            println("CameraPreview: Setting up camera session for position $cameraPosition")
            try {
                // Stop previous session if running
                captureSession?.stopRunning()
                sessionRunning = false
                
                val (session, layer) = setupCameraSession(cameraPosition)
                captureSession = session
                previewLayer = layer
                sessionRunning = true
                println("CameraPreview: Camera session setup complete")
            } catch (e: Exception) {
                println("CameraPreview: Error setting up camera: $e")
                sessionRunning = false
            }
        }
    }

    // Cleanup on component disposal
    DisposableEffect(Unit) {
        onDispose {
            println("CameraPreview: Disposing component")
            captureSession?.let { session ->
                if (session.running) {
                    session.stopRunning()
                }
            }
        }
    }

    if (permissionGranted && sessionRunning && previewLayer != null) {
        UIKitView(
            modifier = modifier,
            factory = {
                println("CameraPreview: Creating UIView")
                val view = UIView()
                view.backgroundColor = UIColor.blackColor
                
                // Don't set a fixed frame - let the modifier handle sizing
                view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight

                // Add preview layer and configure it properly
                previewLayer?.let { layer ->
                    // Remove from any existing parent first
                    layer.removeFromSuperlayer()
                    
                    view.layer.addSublayer(layer)
                    
                    // Make sure the layer is properly configured
                    layer.videoGravity = AVLayerVideoGravityResizeAspectFill

                    println("CameraPreview: Added preview layer to view")
                }

                view
            },
            update = { view ->
                // Update preview layer frame when view bounds change
                previewLayer?.let { layer ->
                    val bounds = view.bounds
                    println("CameraPreview: Updating layer frame to bounds: width=${bounds.useContents { size.width }}, height=${bounds.useContents { size.height }}")

                    CATransaction.begin()
                    CATransaction.setValue(true, kCATransactionDisableActions)
                    layer.frame = bounds
                    CATransaction.commit()
                }
            },
            onRelease = { view ->
                println("CameraPreview: Releasing camera view")
                previewLayer?.removeFromSuperlayer()
                captureSession?.let { session ->
                    if (session.running) {
                        session.stopRunning()
                    }
                }
                captureSession = null
                previewLayer = null
            }
        )
    } else if (!permissionGranted) {
        // Show permission request UI
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                "Camera permission required\nPlease grant camera access in Settings",
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    } else {
        // Show loading state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                "Starting camera...",
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }


}

@OptIn(ExperimentalForeignApi::class)
private suspend fun setupCameraSession(
    cameraPosition: AVCaptureDevicePosition
): Pair<AVCaptureSession, AVCaptureVideoPreviewLayer> = withContext(Dispatchers.Default) {
    println("setupCameraSession: Starting setup for position $cameraPosition")

    val session = AVCaptureSession()

    // Configure session preset first
    session.beginConfiguration()
    
    // Try different presets in order of preference
    val presets = listOf(
        AVCaptureSessionPresetHigh,
        AVCaptureSessionPresetMedium,
        AVCaptureSessionPreset640x480
    )
    
    for (preset in presets) {
        if (session.canSetSessionPreset(preset)) {
            session.sessionPreset = preset
            println("setupCameraSession: Set preset to $preset")
            break
        }
    }

    // Find camera device - use a more robust approach
    val device = findCameraDevice(cameraPosition)
    if (device == null) {
        session.commitConfiguration()
        throw RuntimeException("No camera device found for position $cameraPosition")
    }

    println("setupCameraSession: Found device: ${device.localizedName}")

    // Create input
    @OptIn(BetaInteropApi::class)
    val input = memScoped {
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        val result = AVCaptureDeviceInput.deviceInputWithDevice(device, errorPtr.ptr)
        
        if (result == null) {
            val error = errorPtr.value
            println("setupCameraSession: Failed to create input: ${error?.localizedDescription}")
            session.commitConfiguration()
            throw RuntimeException("Failed to create camera input: ${error?.localizedDescription}")
        }
        
        result
    }

    if (!session.canAddInput(input)) {
        session.commitConfiguration()
        throw RuntimeException("Cannot add input to session")
    }

    session.addInput(input)
    println("setupCameraSession: Added input to session")

    session.commitConfiguration()

    // Create preview layer with proper configuration
    val previewLayer = AVCaptureVideoPreviewLayer(session = session)
    previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
    
    // Ensure layer connection is properly set up for iOS
    previewLayer.connection?.let { connection ->
        if (connection.isVideoOrientationSupported()) {
            connection.videoOrientation = AVCaptureVideoOrientationPortrait
        }
        if (connection.isVideoMirroringSupported() && cameraPosition == AVCaptureDevicePositionFront) {
            connection.videoMirrored = true
        }
    }
    
    println("setupCameraSession: Created preview layer with orientation support")

    // Start session
    try {
        if (!session.running) {
            session.startRunning()
            println("setupCameraSession: Session started running: ${session.running}")
        } else {
            println("setupCameraSession: Session was already running")
        }
    } catch (e: Exception) {
        println("setupCameraSession: Error starting session: $e")
        throw e
    }

    return@withContext Pair(session, previewLayer)
}

@OptIn(ExperimentalForeignApi::class)
private fun findCameraDevice(position: AVCaptureDevicePosition): AVCaptureDevice? {
    // Try different device types in order of preference
    val deviceTypesToTry = listOf(
        AVCaptureDeviceTypeBuiltInWideAngleCamera,
        AVCaptureDeviceTypeBuiltInDualCamera,
        AVCaptureDeviceTypeBuiltInTripleCamera,
        AVCaptureDeviceTypeBuiltInDualWideCamera
    )
    
    for (deviceType in deviceTypesToTry) {
        try {
            val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
                deviceTypes = listOf(deviceType),
                mediaType = AVMediaTypeVideo,
                position = position
            )
            
            val device = discoverySession.devices.firstOrNull {
                (it as? AVCaptureDevice)?.position == position
            } as? AVCaptureDevice
            
            if (device != null) {
                println("findCameraDevice: Found device type $deviceType for position $position")
                return device
            }
        } catch (e: Exception) {
            println("findCameraDevice: Failed to find device type $deviceType: $e")
            continue
        }
    }
    
    // Fallback: try to get default device for position
    try {
        val allDevices = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo)
        return allDevices.firstOrNull {
            (it as? AVCaptureDevice)?.position == position
        } as? AVCaptureDevice
    } catch (e: Exception) {
        println("findCameraDevice: Failed to get default device: $e")
        return null
    }
}
