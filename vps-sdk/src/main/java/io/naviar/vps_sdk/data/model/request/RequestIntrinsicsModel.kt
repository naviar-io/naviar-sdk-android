package io.naviar.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RequestIntrinsicsModel(
    @Json(name = "width")
    val width: Int = 0,
    @Json(name = "height")
    val height: Int = 0,
    @Json(name = "cx")
    val cx: Float = 0.0f,
    @Json(name = "cy")
    val cy: Float = 0.0f,
    @Json(name = "fx")
    val fx: Float = 0.0f,
    @Json(name = "fy")
    val fy: Float = 0.0f
)
