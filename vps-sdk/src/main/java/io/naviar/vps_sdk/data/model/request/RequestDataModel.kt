package io.naviar.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
internal data class RequestDataModel(
    @Json(name = "attributes")
    val attributes: RequestAttributesModel
)
