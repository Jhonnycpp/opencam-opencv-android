package br.com.opencam.adapter

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidCamera @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraAdapter {

    override fun hasCamera() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
}
