package io.naviar.vps_sdk.data.repository

import java.io.File

internal interface INeuroRepository {

    fun getNeuroModelFile(url: String): File

}