package io.naviar.vps_sdk.ui

import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import io.naviar.vps_sdk.VpsSdkInitializationException
import io.naviar.vps_sdk.data.VpsConfig
import io.naviar.vps_sdk.domain.model.NodePoseModel
import org.koin.core.context.GlobalContext

interface VpsService {

    companion object {

        @JvmStatic
        fun newInstance(): VpsService {
            GlobalContext.getOrNull() ?: throw VpsSdkInitializationException()

            return GlobalContext.get().get()
        }
    }

    val worldNode: Node
    val isRun: Boolean
    val cameraPose: NodePoseModel

    fun bindArSceneView(arSceneView: ArSceneView)
    fun resume()
    fun pause()
    fun destroy()

    fun setVpsConfig(vpsConfig: VpsConfig)
    fun setVpsCallback(vpsCallback: VpsCallback)

    fun startVpsService()
    fun stopVpsService()

    enum class State {
        RUN, PAUSE, STOP
    }

}