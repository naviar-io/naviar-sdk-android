package io.naviar.vps_sdk.domain.model

internal data class LocalizationModel(
    val vpsPose: NodePoseModel,
    val trackingPose: NodePoseModel,
    val gpsPose: GpsPoseModel
) {
    companion object {
        val EMPTY = LocalizationModel(NodePoseModel.EMPTY, NodePoseModel.EMPTY, GpsPoseModel.EMPTY)
    }
}
