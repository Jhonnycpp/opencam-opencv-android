package br.com.opencam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.opencam.adapter.CameraAdapter
import br.com.opencam.dto.CamState
import br.com.opencam.dto.CamType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraAdapter: CameraAdapter
) : ViewModel() {
    private var camType: CamType = CamType.BACK
    private var _selectedCam = MutableSharedFlow<CamType>()
    var canUseCam = MutableStateFlow<CamState>(CamState.Unknown)
    val selectedCam: SharedFlow<CamType> get() = _selectedCam.asSharedFlow()

    fun prepareCamera(context: Context) {
        viewModelScope.launch {
            hasCamera()
            hasPermission(context)
        }
    }

    fun permissionGrantedToUseCam() {
        viewModelScope.launch {
            canUseCam.emit(CamState.CamRun)
        }
    }

    fun swapCam() {
        viewModelScope.launch {
            camType = if (camType == CamType.BACK) {
                _selectedCam.emit(CamType.FRONT)
                CamType.FRONT
            } else {
                _selectedCam.emit(CamType.BACK)
                CamType.BACK
            }
        }
    }

    fun processImage(image: Bitmap?, rotationDegrees: Int): Bitmap? {
        if (image == null) return null

        return image.run {
            // Create OpenCV mat object and copy content from bitmap
            val mat = Mat()
            Utils.bitmapToMat(image, mat)

            // Convert to grayscale
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

            // Make a mutable bitmap to copy grayscale image
            val grayBitmap = image.copy(image.config, true)
            Utils.matToBitmap(mat, grayBitmap)

            grayBitmap.rotate(rotationDegrees)
        }
    }

    private suspend fun hasCamera() {
        if (!cameraAdapter.hasCamera()) {
            canUseCam.emit(CamState.DeviceHaveNotCam)
        }
    }

    private suspend fun hasPermission(context: Context) {
        when (PackageManager.PERMISSION_GRANTED) {
            context.checkSelfPermission(Manifest.permission.CAMERA) -> canUseCam.emit(CamState.CamRun)
            else -> canUseCam.emit(CamState.NeedPermission)
        }
    }

    private fun Bitmap?.colorFilter(
        color: Int = Color.GREEN,
        mode: PorterDuff.Mode = PorterDuff.Mode.DARKEN
    ): Bitmap? {
        if (this == null) return null
        val bitmap = copy(Bitmap.Config.ARGB_8888, true)
        Paint().apply {
            val filter = PorterDuffColorFilter(color, mode)
            colorFilter = filter
            Canvas(bitmap).drawBitmap(this@colorFilter, 0f, 0f, this)
        }
        return bitmap
    }

    private fun Bitmap?.rotate(degrees: Int): Bitmap? = if (this == null)
        null
    else
        Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees.toFloat()) }, true)

}
