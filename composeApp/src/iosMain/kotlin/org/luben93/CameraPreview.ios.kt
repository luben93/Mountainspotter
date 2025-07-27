package org.luben93

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSError
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    onSwitchCamera: () -> Unit
) {
    var captureSession by remember { mutableStateOf<AVCaptureSession?>(null) }
    var previewLayer by remember { mutableStateOf<AVCaptureVideoPreviewLayer?>(null) }
    var cameraPosition by remember { mutableStateOf(AVCaptureDevicePositionBack) }
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
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    println("CameraPreview: Permission result: $granted")
                    permissionGranted = granted
                }
            }
            else -> {
                println("CameraPreview: Camera permission denied or restricted")
                permissionGranted = false
            }
        }
    }

    // Setup camera when permission is granted
    LaunchedEffect(permissionGranted, cameraPosition) {
        if (permissionGranted) {
            println("CameraPreview: Setting up camera session")
            try {
                val (session, layer) = setupCameraSession(cameraPosition)
                captureSession = session
                previewLayer = layer
                sessionRunning = true
                println("CameraPreview: Camera session setup complete")
            } catch (e: Exception) {
                println("CameraPreview: Error setting up camera: $e")
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
                
                // Ensure the view has a proper frame initially
                view.setFrame(platform.CoreGraphics.CGRectMake(0.0, 0.0, 300.0, 400.0))

                // Add preview layer and configure it properly
                previewLayer?.let { layer ->
                    // Remove from any existing parent first
                    layer.removeFromSuperlayer()
                    
                    view.layer.addSublayer(layer)
                    
                    // Set initial frame
                    layer.frame = view.bounds
                    
                    // Make sure the layer is properly configured
                    layer.videoGravity = platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
                    
                    println("CameraPreview: Added preview layer to view with frame: ${view.bounds}")
                }

                view
            },
            update = { view ->
                // Update preview layer frame when view bounds change
                previewLayer?.let { layer ->
                    platform.QuartzCore.CATransaction.begin()
                    platform.QuartzCore.CATransaction.setValue(true, platform.QuartzCore.kCATransactionDisableActions)
                    layer.frame = view.bounds
                    platform.QuartzCore.CATransaction.commit()
                    println("CameraPreview: Updated preview layer frame to ${view.bounds}")
                }
            },
            onRelease = { view ->
                println("CameraPreview: Releasing camera view")
                previewLayer?.removeFromSuperlayer()
                captureSession?.stopRunning()
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

    // Handle camera switch button press
    LaunchedEffect(onSwitchCamera) {
        // This will be called when the switch button is pressed
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private suspend fun setupCameraSession(
    cameraPosition: AVCaptureDevicePosition
): Pair<AVCaptureSession, AVCaptureVideoPreviewLayer> {
    println("setupCameraSession: Starting setup for position $cameraPosition")

    val session = AVCaptureSession()

    // Configure session preset first
    session.beginConfiguration()
    if (session.canSetSessionPreset(AVCaptureSessionPresetHigh)) {
        session.sessionPreset = AVCaptureSessionPresetHigh
        println("setupCameraSession: Set preset to high quality")
    }

    // Find camera device
    val deviceTypes = listOf(
        AVCaptureDeviceTypeBuiltInWideAngleCamera,
        AVCaptureDeviceTypeBuiltInDualCamera,
        AVCaptureDeviceTypeBuiltInTripleCamera
    )

    val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
        deviceTypes = deviceTypes,
        mediaType = AVMediaTypeVideo,
        position = cameraPosition
    )

    val device = discoverySession?.devices?.firstOrNull {
        (it as? AVCaptureDevice)?.position == cameraPosition
    } as? AVCaptureDevice

    if (device == null) {
        println("setupCameraSession: No camera device found for position $cameraPosition")
        throw RuntimeException("No camera device found")
    }

    println("setupCameraSession: Found device: ${device.localizedName}")

    // Create input
    memScoped {
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, errorPtr.ptr)

        if (input == null) {
            val error = errorPtr.value
            println("setupCameraSession: Failed to create input: ${error?.localizedDescription}")
            throw RuntimeException("Failed to create camera input")
        }

        if (!session.canAddInput(input)) {
            println("setupCameraSession: Cannot add input to session")
            throw RuntimeException("Cannot add input to session")
        }

        session.addInput(input)
        println("setupCameraSession: Added input to session")
    }

    session.commitConfiguration()

    // Create preview layer
    val previewLayer = AVCaptureVideoPreviewLayer(session = session)
    previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
    println("setupCameraSession: Created preview layer")

    // Start session on background queue to avoid blocking UI
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
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
    }

    return Pair(session, previewLayer)
}

// Function to switch camera (to be called from button press)
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun switchCameraPosition(
    currentSession: AVCaptureSession?,
    currentPosition: AVCaptureDevicePosition,
    onComplete: (AVCaptureSession, AVCaptureVideoPreviewLayer, AVCaptureDevicePosition) -> Unit
) {
    val newPosition = if (currentPosition == AVCaptureDevicePositionFront) {
        AVCaptureDevicePositionBack
    } else {
        AVCaptureDevicePositionFront
    }

    println("switchCameraPosition: Switching from $currentPosition to $newPosition")

    currentSession?.stopRunning()

    kotlinx.coroutines.GlobalScope.launch {
        try {
            val (newSession, newLayer) = setupCameraSession(newPosition)
            onComplete(newSession, newLayer, newPosition)
        } catch (e: Exception) {
            println("switchCameraPosition: Error switching camera: $e")
        }
    }
}
