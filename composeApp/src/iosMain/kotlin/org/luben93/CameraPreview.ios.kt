package org.luben93

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

private sealed interface CameraAccess {
    object Undefined : CameraAccess
    object Denied : CameraAccess
    object Authorized : CameraAccess
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    isFrontCamera: Boolean,
    onSwitchCamera: () -> Unit
) {
    var cameraAccess: CameraAccess by remember { mutableStateOf(CameraAccess.Undefined) }
    
    // Request camera permission with proper main thread handling
    LaunchedEffect(Unit) {
        val currentStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        println("iOS Camera: Current permission status: $currentStatus")
        
        when (currentStatus) {
            AVAuthorizationStatusAuthorized -> {
                println("iOS Camera: Permission already granted")
                cameraAccess = CameraAccess.Authorized
            }
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> {
                println("iOS Camera: Permission denied or restricted")
                cameraAccess = CameraAccess.Denied
            }
            AVAuthorizationStatusNotDetermined -> {
                println("iOS Camera: Requesting permission")
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    println("iOS Camera: Permission callback - granted: $granted")
                    dispatch_async(dispatch_get_main_queue()) {
                        cameraAccess = if (granted) CameraAccess.Authorized else CameraAccess.Denied
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (cameraAccess) {
            CameraAccess.Undefined -> {
                Text(
                    "Requesting camera permission...",
                    color = Color.White
                )
            }
            CameraAccess.Denied -> {
                Text(
                    "Camera access denied\nPlease grant camera access in Settings",
                    color = Color.White
                )
            }
            CameraAccess.Authorized -> {
                CameraView(isFrontCamera, modifier)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
private fun CameraView(
    isFrontCamera: Boolean,
    modifier: Modifier
) {
    val cameraPosition = if (isFrontCamera) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack
    
    println("iOS Camera: Setting up camera for position: $cameraPosition")

    UIKitView(factory = {
        println("iOS Camera: Creating UIKitView factory")

        // Create the main container view
        val containerView = object : UIView(frame = CGRectZero.readValue()) {
            override fun layoutSubviews() {
                super.layoutSubviews()
                println("iOS Camera: Container layoutSubviews called, frame: $frame")

                // Update all sublayers frames
                layer.sublayers?.let { sublayers ->
                    (0 until sublayers.count()).forEach { index ->
                        val sublayer = sublayers[index]
                        if (sublayer is AVCaptureVideoPreviewLayer) {
                            println("iOS Camera: Updating preview layer frame")
                            CATransaction.begin()
                            CATransaction.setValue(true, kCATransactionDisableActions)
                            sublayer.setFrame(bounds)
                            CATransaction.commit()
                        }
                    }
                }
            }
        }

        try {
            // Create capture session
            val captureSession = AVCaptureSession()
            captureSession.sessionPreset = AVCaptureSessionPresetHigh
            println("iOS Camera: Created capture session")

            // Find camera device
            val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
                deviceTypes = listOf(
                    AVCaptureDeviceTypeBuiltInWideAngleCamera,
                    AVCaptureDeviceTypeBuiltInDualWideCamera,
                    AVCaptureDeviceTypeBuiltInDualCamera,
                    AVCaptureDeviceTypeBuiltInUltraWideCamera,
                    AVCaptureDeviceTypeBuiltInTripleCamera
                ),
                mediaType = AVMediaTypeVideo,
                position = cameraPosition
            )

            val camera = discoverySession.devices.firstOrNull() as? AVCaptureDevice
            if (camera == null) {
                println("iOS Camera: No camera found for position $cameraPosition")
                return@UIKitView containerView
            }

            println("iOS Camera: Found camera: ${camera.localizedName}")

            // Create device input
            val deviceInput = memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val input = AVCaptureDeviceInput.deviceInputWithDevice(camera, errorPtr.ptr)
                val error = errorPtr.value
                if (error != null) {
                    println("iOS Camera: Error creating device input: ${error.localizedDescription}")
                    return@memScoped null
                }
                input
            }

            if (deviceInput == null) {
                println("iOS Camera: Failed to create device input")
                return@UIKitView containerView
            }

            // Add input to session
            if (captureSession.canAddInput(deviceInput)) {
                captureSession.addInput(deviceInput)
                println("iOS Camera: Added device input to session")
            } else {
                println("iOS Camera: Cannot add device input to session")
                return@UIKitView containerView
            }

            // Create preview layer
            val previewLayer = AVCaptureVideoPreviewLayer(session = captureSession)
            previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            previewLayer.setFrame(containerView.bounds)

            // Add preview layer to container
            containerView.layer.addSublayer(previewLayer)
            println("iOS Camera: Added preview layer to container")

            // Start capture session on background queue
            val sessionQueue = dispatch_queue_create("camera.session.queue", null)
            dispatch_async(sessionQueue) {
                if (!captureSession.running) {
                    captureSession.startRunning()
                    println("iOS Camera: Started capture session")
                }
            }

        } catch (e: Exception) {
            println("iOS Camera: Exception during setup: ${e.message}")
        }

        containerView
    },
        modifier = modifier.fillMaxSize().background(Color.Black),
        properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true))
}
