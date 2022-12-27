package io.naviar.vps_sdk.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.naviar.vps_sdk.data.model.LocationModel
import io.naviar.vps_sdk.data.model.PoseModel

@JsonClass(generateAdapter = true)
internal data class RequestAttributesModel(
    @Json(name = "location_ids")
    val locationIds: Array<String>,
    @Json(name = "session_id")
    val sessionId: String,
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "timestamp")
    val timestamp: Double,
    @Json(name = "location")
    val location: LocationModel?,
    @Json(name = "client_coordinate_system")
    val clientCoordinateSystem: String = "arcore",
    @Json(name = "tracking_pose")
    val trackingPose: PoseModel,
    @Json(name = "intrinsics")
    val intrinsics: RequestIntrinsicsModel
)
