package br.com.opencam.di

import br.com.opencam.adapter.AndroidCamera
import br.com.opencam.adapter.CameraAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class MainModuleBind {
    @Binds
    abstract fun bindCameraAdapter(androidCamera: AndroidCamera): CameraAdapter
}
