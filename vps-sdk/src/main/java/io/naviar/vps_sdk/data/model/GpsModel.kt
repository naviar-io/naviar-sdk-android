package io.naviar.vps_sdk.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GpsModel(
    @Json(name = "accuracy")
    val accuracy: Double = 0.0,
    @Json(name = "altitude")
    val altitude: Double = 0.0,
    @Json(name = "latitude")
    val latitude: Double = 0.0,
    @Json(name = "longitude")
    val longitude: Double = 0.0,
    @Json(name = "timestamp")
    val timestamp: Double = 0.0
)
