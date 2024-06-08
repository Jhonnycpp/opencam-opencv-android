package br.com.opencam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.opencam.adapter.CameraAdapter
import br.com.opencam.dto.CamState
import br.com.opencam.dto.CamType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraAdapter: CameraAdapter
) : ViewModel() {
    private var _selectedCam: MutableStateFlow<CamType> = MutableStateFlow(CamType.BACK)
    private var _canUseCam: MutableStateFlow<CamState> = MutableStateFlow(CamState.Unknown)
    private var _minVariance: MutableStateFlow<Double> = MutableStateFlow(Double.MAX_VALUE)
    private var _currentVariance: MutableStateFlow<Pair<Double, Boolean>> = MutableStateFlow(Double.MAX_VALUE to false)
    private var _enabledGrayScale: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val canUseCam: StateFlow<CamState> get() = _canUseCam.asStateFlow()
    val selectedCam: StateFlow<CamType> get() = _selectedCam.asStateFlow()
    val minVariance: StateFlow<Double> get() = _minVariance.asStateFlow()
    val currentVariance: StateFlow<Pair<Double, Boolean>> get() = _currentVariance.asStateFlow()
    val enabledGrayScale: StateFlow<Boolean> = _enabledGrayScale.asStateFlow()

    fun prepareCamera(context: Context) {
        viewModelScope.launch {
            hasCamera()
            hasPermission(context)
        }
    }

    fun permissionGrantedToUseCam() {
        viewModelScope.launch {
            _canUseCam.value = CamState.CamRun
        }
    }

    fun swapCam() {
        if (_selectedCam.value == CamType.BACK) {
            _selectedCam.value = CamType.FRONT
        } else {
            _selectedCam.value = CamType.BACK
        }
    }

    fun processImage(image: Bitmap?, rotationDegrees: Int): Bitmap? {
        if (image == null) return null

        return image.run {
            val fastAnalysed = BlurValidation.isBlurredImageFastDetection(this)
            val slowlyAnalysed = BlurValidation.isBlurredImageSlowlyDetection(this)

            if (slowlyAnalysed && fastAnalysed < _minVariance.value) _minVariance.value = fastAnalysed.also {
                Log.d("CameraViewModel", "minimal value: $it")
            }

            _currentVariance.value = fastAnalysed to slowlyAnalysed

            // TODO(CleanArch): Mover para um adapter
            if (_enabledGrayScale.value) {
                // Create OpenCV mat object and copy content from bitmap
                val mat = Mat()
                Utils.bitmapToMat(image, mat)

                // Convert to grayscale
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

                // Make a mutable bitmap to copy grayscale image
                val grayBitmap = image.copy(image.config, true)
                Utils.matToBitmap(mat, grayBitmap)

                grayBitmap.rotate(rotationDegrees)
            } else {
                rotate(rotationDegrees)
            }
        }
    }

    fun setGrayScale(value: Boolean) {
        _enabledGrayScale.value = value
    }

    private fun hasCamera() {
        if (!cameraAdapter.hasCamera()) {
            _canUseCam.value = CamState.DeviceHaveNotCam
        }
    }

    private fun hasPermission(context: Context) {
        when (PackageManager.PERMISSION_GRANTED) {
            context.checkSelfPermission(Manifest.permission.CAMERA) -> _canUseCam.value = CamState.CamRun
            else -> _canUseCam.value = CamState.NeedPermission
        }
    }

    private fun Bitmap?.rotate(degrees: Int): Bitmap? = if (this == null) {
        null
    } else {
        Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
    }
}
