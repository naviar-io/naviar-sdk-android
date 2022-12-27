package io.naviar.vps_sdk.data.model.response

import com.squareup.moshi.*

@JsonClass(generateAdapter = true)
internal data class ResponseDataModel(
    @Json(name = "status")
    val status: String,
    @Json(name = "status_description")
    val statusDescription: String? = null,
    @Json(name = "attributes")
    val attributes: ResponseAttributesModel? = null
)