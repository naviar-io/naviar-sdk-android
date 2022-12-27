package io.naviar.vps_sdk.domain.model

import io.naviar.vps_sdk.data.LocalizationType
import io.naviar.vps_sdk.data.model.CameraIntrinsics

internal data class VpsLocationModel(
    val locationIds: Array<String>,
    val sessionId: String,
    val userId: String,
    val timestamp: Double,
    val gpsLocation: GpsLocationModel?,
    val compass: CompassModel,
    val trackingPose: NodePoseModel,
    val localizationType: LocalizationType,
    val byteArray: ByteArray,
    val cameraIntrinsics: CameraIntrinsics
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VpsLocationModel

        if (!locationIds.contentEquals(other.locationIds)) return false
        if (sessionId != other.sessionId) return false
        if (userId != other.userId) return false
        if (timestamp != other.timestamp) return false
        if (gpsLocation != other.gpsLocation) return false
        if (compass != other.compass) return false
        if (trackingPose != other.trackingPose) return false
        if (localizationType != other.localizationType) return false
        if (!byteArray.contentEquals(other.byteArray)) return false
        if (cameraIntrinsics != other.cameraIntrinsics) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sessionId.hashCode()
        result = 31 * result + locationIds.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (gpsLocation?.hashCode() ?: 0)
        result = 31 * result + compass.hashCode()
        result = 31 * result + trackingPose.hashCode()
        result = 31 * result + localizationType.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        result = 31 * result + cameraIntrinsics.hashCode()
        return result
    }
}
