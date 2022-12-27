package io.naviar.vps_sdk.data.repository

import io.naviar.vps_sdk.domain.model.LocalizationModel
import io.naviar.vps_sdk.domain.model.VpsLocationModel

internal interface IVpsRepository {

    suspend fun requestLocalization(
        url: String,
        vpsLocationModel: VpsLocationModel
    ): LocalizationModel?

}