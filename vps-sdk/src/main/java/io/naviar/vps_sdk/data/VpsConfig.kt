package io.naviar.vps_sdk.data

data class VpsConfig(
    val vpsUrl: String = "https://vps.naviar.io/",
    val locationIds: Array<String>,
    val intervalLocalizationMS: Long = 2500,
    val useGps: Boolean = false,
    val localizationType: LocalizationType = MobileVps(),
    val failsCountToResetSession: Int = 5,
    val updateWorldDurationMS: Long = 500,
    val updateWorldDistanceLimit: Float = 2f,
    val updateWorldAngleLimit: Float = 10f
) {
    companion object {

        fun getIndoorConfig(locationIds: Array<String>): VpsConfig =
            VpsConfig(
                locationIds = locationIds,
                useGps = false
            )

        fun getOutdoorConfig(locationIds: Array<String>): VpsConfig =
            VpsConfig(
                locationIds = locationIds,
                useGps = true
            )

    }
}