package br.com.opencam.dto

import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing

enum class CamType(@LensFacing val id: Int) {
    FRONT(CameraSelector.LENS_FACING_FRONT),
    BACK(CameraSelector.LENS_FACING_BACK),
}
