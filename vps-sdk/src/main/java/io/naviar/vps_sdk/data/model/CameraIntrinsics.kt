package io.naviar.vps_sdk.data.model

internal data class CameraIntrinsics(
    val fx: Float = 0f,
    val fy: Float = 0f,
    val cx: Float = 0f,
    val cy: Float = 0f,
    val width: Int = 0,
    val height: Int = 0
) {
    companion object {
        val DEFAULT = CameraIntrinsics(0f, 0f, 0f, 0f, 0, 0)
    }
}
