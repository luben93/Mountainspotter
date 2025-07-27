package org.luben93

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("MissingPermission")
@Composable
actual fun CameraPreview(
    modifier: Modifier,
    isFrontCamera: Boolean,
    onSwitchCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    
    // Initialize state variables first
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    // Update camera when selector changes
    LaunchedEffect(cameraSelector, cameraProvider) {
        if (cameraProvider != null && previewView != null) {
            try {
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView!!.surfaceProvider)
                
                cameraProvider!!.unbindAll()
                cameraProvider!!.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                Log.d("CameraPreview", "Camera switched successfully to ${if (isFrontCamera) "front" else "back"}")
            } catch (e: Exception) {
                Log.e("CameraPreview", "Failed to switch camera", e)
            }
        }
    }

    // Check camera permission
    LaunchedEffect(Unit) {
        hasPermission = checkCameraPermission(context)
        Log.d("CameraPreview", "Camera permission granted: $hasPermission")
    }

    // Get camera provider
    LaunchedEffect(cameraProviderFuture) {
        try {
            cameraProvider = cameraProviderFuture.get()
            Log.d("CameraPreview", "Camera provider obtained")
        } catch (e: Exception) {
            Log.e("CameraPreview", "Failed to get camera provider", e)
        }
    }

    if (!hasPermission) {
        // Show permission request UI
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Camera permission required\nPlease grant camera access in Settings",
                color = Color.White
            )
        }
    } else if (cameraProvider != null) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val preview = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewView = preview
                
                val previewUseCase = Preview.Builder().build()
                previewUseCase.setSurfaceProvider(preview.surfaceProvider)

                try {
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewUseCase
                    )
                    Log.d("CameraPreview", "Camera bound successfully")
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Failed to bind camera", e)
                }

                preview
            },
            update = { preview ->
                // Update is handled by LaunchedEffect above
            }
        )
    } else {
        // Show loading state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Starting camera...",
                color = Color.White
            )
        }
    }
}

// Function to switch between front and back camera
fun switchCamera(currentSelector: CameraSelector): CameraSelector {
    return if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }
}

private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}