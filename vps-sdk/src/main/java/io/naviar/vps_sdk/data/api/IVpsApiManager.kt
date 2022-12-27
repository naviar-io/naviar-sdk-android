package io.naviar.vps_sdk.data.api

internal interface IVpsApiManager {

    fun getVpsApi(url: String): VpsApi

}