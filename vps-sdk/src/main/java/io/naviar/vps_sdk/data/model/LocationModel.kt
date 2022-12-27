package io.naviar.vps_sdk.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class LocationModel(
    @Json(name = "gps")
    val gps: GpsModel,
    @Json(name = "compass")
    val compass: CompassModel?
)
