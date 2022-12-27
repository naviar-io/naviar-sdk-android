package io.naviar.vps_sdk.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PoseModel(
    @Json(name = "x")
    val x: Float,
    @Json(name = "y")
    val y: Float,
    @Json(name = "z")
    val z: Float,
    @Json(name = "rx")
    val rx: Float,
    @Json(name = "ry")
    val ry: Float,
    @Json(name = "rz")
    val rz: Float,
)
