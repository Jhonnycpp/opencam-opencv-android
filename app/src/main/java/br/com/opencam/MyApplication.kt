package br.com.opencam

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class MyApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        OpenCVLoader.initDebug()
    }
}
