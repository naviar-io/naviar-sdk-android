package io.naviar.vps_sdk.domain.model

internal data class CompassModel(
    val accuracy: Float,
    val heading: Float,
    val timestamp: Double
)
