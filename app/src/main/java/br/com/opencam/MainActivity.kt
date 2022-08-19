package br.com.opencam

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.Surface.ROTATION_0
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import br.com.opencam.databinding.ActivityMainBinding
import br.com.opencam.dto.CamState
import br.com.opencam.dto.CamType
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        lifecycleScope.launchWhenStarted {
            viewModel.canUseCam.collectLatest {
                when (it) {
                    CamState.DeviceHaveNotCam -> TODO("Exibir o fragment de que o device não possui camera.")
                    CamState.NeedPermission -> requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
                    CamState.CamRun -> runCamera()
                    CamState.Unknown -> viewModel.prepareCamera(this@MainActivity)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.selectedCam.collectLatest {
                runCamera(it)
            }
        }

        binding.swapCam.setOnClickListener {
            ProcessCameraProvider.getInstance(this).cancel(true)
            viewModel.swapCam()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    viewModel.permissionGrantedToUseCam()
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }
            else -> return
        }
    }

    @SuppressLint("UnsafeOptInUsageError", "ClickableViewAccessibility")
    private fun runCamera(camType: CamType = CamType.BACK) {
        ProcessCameraProvider.getInstance(this@MainActivity).also { processCameraProvider ->
            processCameraProvider.addListener({
                val cameraProvider = processCameraProvider.get()

                cameraProvider.unbindAll()

                val preview = Preview.Builder().setTargetRotation(ROTATION_0).build()
                val imageCapture =
                    ImageCapture.Builder().setTargetRotation(ROTATION_0).setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetRotation(ROTATION_0)
                    .build()
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    var bitmap = imageProxy.image.toBitmap()

                    imageProxy.close()

                    bitmap = viewModel.processImage(bitmap, rotationDegrees)
                    binding.imageView.post {
                        bitmap?.also { binding.imageView.setImageBitmap(it) }
                    }
                }

                val cameraSelector = CameraSelector.Builder().requireLensFacing(camType.id).build()

                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageAnalysis)
                    .addUseCase(imageCapture)
                    .build()

                val camera = cameraProvider.bindToLifecycle(this@MainActivity as LifecycleOwner, cameraSelector, useCaseGroup)
                binding.imageView.setOnTouchListener { _, motionEvent ->
                    val meteringPoint = binding.previewView.meteringPointFactory.createPoint(motionEvent.x, motionEvent.y)
                    val action = FocusMeteringAction.Builder(meteringPoint).build()
                    camera.cameraControl.startFocusAndMetering(action)
                    Log.d("ImageView", "onTouch processed image to focus.")
                    true
                }

                binding.swapCam.visibility = View.VISIBLE

                preview.setSurfaceProvider(binding.previewView.surfaceProvider)
            }, ContextCompat.getMainExecutor(this@MainActivity))
        }
    }

    private fun Image?.toBitmap(): Bitmap? {
        if (this == null) return null

        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    companion object {
        const val CAMERA_PERMISSION_CODE = 1
    }
}
