package io.naviar.vps_sdk.domain.interactor

import io.naviar.vps_sdk.data.LocalizationType
import io.naviar.vps_sdk.data.model.CameraIntrinsics
import io.naviar.vps_sdk.domain.model.*

internal interface IVpsInteractor {

    suspend fun prepareVpsLocationModel(
        locationIds: Array<String>,
        source: ByteArray,
        sessionId: String,
        localizationType: LocalizationType,
        cameraPose: NodePoseModel,
        gpsLocation: GpsLocationModel? = null,
        compass: CompassModel,
        cameraIntrinsics: CameraIntrinsics
    ): VpsLocationModel

    suspend fun calculateNodePose(
        url: String,
        vpsLocationModel: VpsLocationModel
    ): LocalizationModel?

    fun destroy()

}