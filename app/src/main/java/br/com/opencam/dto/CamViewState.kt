package br.com.opencam.dto

sealed class CamViewState

sealed interface CamState {
    object NeedPermission : CamState
    object DeviceHaveNotCam : CamState
    object CamRun : CamState
    object Unknown : CamState
}
