package io.naviar.vps_android_prototype

import android.app.Application
import io.naviar.vps_sdk.VpsSdk
import org.osmdroid.config.Configuration

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        VpsSdk.init(this)

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

}