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
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectZero
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.*

private sealed interface CameraAccess {
    object Undefined : CameraAccess
    object Denied : CameraAccess
    object Authorized : CameraAccess
}

private val deviceTypes = listOf(
    AVCaptureDeviceTypeBuiltInWideAngleCamera,
    AVCaptureDeviceTypeBuiltInDualWideCamera,
    AVCaptureDeviceTypeBuiltInDualCamera,
    AVCaptureDeviceTypeBuiltInUltraWideCamera,
    AVCaptureDeviceTypeBuiltInTripleCamera
)

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    isFrontCamera: Boolean,
    onSwitchCamera: () -> Unit
) {
    var cameraAccess: CameraAccess by remember { mutableStateOf(CameraAccess.Undefined) }
    
    // Request camera permission
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> {
                cameraAccess = CameraAccess.Authorized
            }
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> {
                cameraAccess = CameraAccess.Denied
            }
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { success ->
                    cameraAccess = if (success) CameraAccess.Authorized else CameraAccess.Denied
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
                AuthorizedCamera(isFrontCamera, modifier)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun AuthorizedCamera(
    isFrontCamera: Boolean,
    modifier: Modifier
) {
    val cameraPosition = if (isFrontCamera) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack
    
    // Find camera device
    val camera: AVCaptureDevice? = remember(cameraPosition) {
        AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = deviceTypes,
            mediaType = AVMediaTypeVideo,
            position = cameraPosition,
        ).devices.firstOrNull() as? AVCaptureDevice
    }

    if (camera != null) {
        RealDeviceCamera(camera, modifier)
    } else {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Camera not available for selected position",
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun RealDeviceCamera(
    camera: AVCaptureDevice,
    modifier: Modifier
) {
    // Create capture session - following JetBrains imageviewer pattern
    val captureSession: AVCaptureSession = remember(camera) {
        AVCaptureSession().also { captureSession ->
            captureSession.sessionPreset = AVCaptureSessionPresetPhoto
            
            // Create device input - simplified approach like JetBrains example
            val captureDeviceInput: AVCaptureDeviceInput = 
                AVCaptureDeviceInput.deviceInputWithDevice(device = camera, error = null)!!
            
            captureSession.addInput(captureDeviceInput)
        }
    }
    
    // Create preview layer
    val cameraPreviewLayer = remember(captureSession) {
        AVCaptureVideoPreviewLayer(session = captureSession).apply {
            videoGravity = AVLayerVideoGravityResizeAspectFill
        }
    }

    DisposableEffect(captureSession) {
        onDispose {
            if (captureSession.running) {
                captureSession.stopRunning()
            }
        }
    }

    UIKitView(
        modifier = modifier.fillMaxSize().background(Color.Black),
        factory = {
            // Create custom container view following JetBrains pattern
            val cameraContainer = object : UIView(frame = CGRectZero.readValue()) {
                override fun layoutSubviews() {
                    CATransaction.begin()
                    CATransaction.setValue(true, kCATransactionDisableActions)
                    layer.setFrame(frame)
                    cameraPreviewLayer.setFrame(frame)
                    CATransaction.commit()
                }
            }
            
            // Add preview layer to container
            cameraContainer.layer.addSublayer(cameraPreviewLayer)
            
            // Start the capture session immediately like JetBrains example
            captureSession.startRunning()
            
            cameraContainer
        }
    )
}
