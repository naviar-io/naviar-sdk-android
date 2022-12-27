package io.naviar.vps_sdk.domain.model

import kotlin.Double.Companion.NaN
import kotlin.Float.Companion.NaN as FloatNan

data class GpsPoseModel(
    val altitude: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val heading: Float = 0f
) {
    companion object {
        val DEFAULT = GpsPoseModel(0.0, 0.0, 0.0, 0f)
        val EMPTY = GpsPoseModel(NaN, NaN, NaN, FloatNan)
    }
}
