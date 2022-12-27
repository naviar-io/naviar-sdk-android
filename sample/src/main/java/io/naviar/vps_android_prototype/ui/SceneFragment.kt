package io.naviar.vps_android_prototype.ui

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.RawRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.ar.core.Config
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import io.naviar.vps_android_prototype.R
import io.naviar.vps_android_prototype.databinding.FmtSceneBinding
import io.naviar.vps_android_prototype.databinding.MenuSceneBinding
import io.naviar.vps_android_prototype.util.Logger
import io.naviar.vps_sdk.common.CoordinateConverter
import io.naviar.vps_sdk.data.MobileVps
import io.naviar.vps_sdk.data.Photo
import io.naviar.vps_sdk.data.VpsConfig
import io.naviar.vps_sdk.domain.model.GpsPoseModel
import io.naviar.vps_sdk.ui.VpsArFragment
import io.naviar.vps_sdk.ui.VpsCallback
import io.naviar.vps_sdk.ui.VpsService
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController.Visibility
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class SceneFragment : Fragment(R.layout.fmt_scene), VpsCallback, Scene.OnUpdateListener {

    private companion object {
        const val INDICATOR_COLOR_DELAY = 500L
        const val BASE_COLOR_FACTOR = "baseColorFactor"
    }

    private val location: Location = Location.Polytech

    private var vpsConfig: VpsConfig = VpsConfig.getOutdoorConfig(location.locationIds)

    private var occluderEnable: Boolean = false
    private val occluderNode: Node = Node()
    private val robotNode: Node = Node()

    private val binding: FmtSceneBinding by viewBinding(FmtSceneBinding::bind)

    private val vpsArFragment: VpsArFragment
        get() = childFragmentManager.findFragmentById(binding.vFragmentContainer.id) as VpsArFragment

    private val vMap: MapView
        get() = binding.vMap

    private val vpsService: VpsService
        get() = vpsArFragment.vpsService

    private val coordinateConverter: CoordinateConverter by lazy {
        CoordinateConverter.instance()
    }

    private val marker: Marker by lazy {
        Marker(vMap)
            .apply {
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_heading)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                vMap.overlays.add(this)
                vMap.controller.setZoom(18.0)
            }
    }

    private var errorDialog: Dialog? = null

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.vTouchZone.setOnLongClickListener {
            showMenu()
            true
        }

        initVpsService()

        loadModel(R.raw.robot) {
            robotNode.renderable = it
            vpsService.worldNode
                .addChild(robotNode)

            setupRobotModel()
        }
        loadModel(location.occluderRawId) {
            occluderNode.renderable = it
            setupOccluder()
        }
        vpsArFragment.setOnSessionConfigurationListener { session, config ->
            config.focusMode = Config.FocusMode.FIXED
            session.resume()
        }

        vpsService.startVpsService()

        with(vMap) {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(Visibility.NEVER)
        }
    }

    override fun onStart() {
        super.onStart()
        vpsArFragment.arSceneView.scene.addOnUpdateListener(this)
    }

    override fun onResume() {
        super.onResume()
        vMap.onResume()
    }

    override fun onPause() {
        super.onPause()
        vMap.onPause()
    }

    override fun onStop() {
        super.onStop()
        vpsArFragment.arSceneView.scene.removeOnUpdateListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        vMap.onDetach()
    }

    override fun onSuccess() {
        updateVpsStatus(true)
    }

    override fun onFail() {
        updateVpsStatus(false)
    }

    override fun onStateChange(state: VpsService.State) {
        Logger.debug("VPS service: $state")
    }

    override fun onError(error: Throwable) {
        Logger.error(error)
        showError(error)
    }

    override fun onUpdate(frameTime: FrameTime?) {
        val gpsPose = coordinateConverter.convertToGlobalCoordinate(vpsService.cameraPose)
        if (gpsPose == GpsPoseModel.EMPTY) return

        marker.position = GeoPoint(gpsPose.latitude, gpsPose.longitude)
        marker.rotation = gpsPose.heading
        vMap.controller.setCenter(marker.position)
    }

    private fun loadModel(@RawRes rawRes: Int, completeCallback: (Renderable) -> Unit) {
        ModelRenderable.builder()
            .setSource(context, rawRes)
            .setIsFilamentGltf(true)
            .build()
            .thenApply(completeCallback)
            .exceptionally { Logger.error(it) }
    }

    private fun updateOccluderState() {
        if (occluderEnable) {
            vpsService.worldNode.addChild(occluderNode)
        } else {
            vpsService.worldNode.removeChild(occluderNode)
        }
    }

    private fun showMenu() {
        val menuBinding = MenuSceneBinding.inflate(layoutInflater)

        menuBinding.cbAutofocus.isChecked = vpsArFragment.isAutofocus()
        menuBinding.cbMobileVps.isChecked = vpsConfig.localizationType is MobileVps
        menuBinding.cbGps.isChecked = vpsConfig.useGps
        menuBinding.cbOccluder.isChecked = occluderEnable

        AlertDialog.Builder(requireContext())
            .setView(menuBinding.root)
            .setPositiveButton(R.string.apply) { _, _ ->
                vpsArFragment.setAutofocus(menuBinding.cbAutofocus.isChecked)
                occluderEnable = menuBinding.cbOccluder.isChecked

                restartVpsService(
                    menuBinding.cbMobileVps.isChecked,
                    menuBinding.cbGps.isChecked,
                )
            }
            .show()
    }

    private fun initVpsService() {
        with(vpsService) {
            setVpsCallback(this@SceneFragment)
            setVpsConfig(vpsConfig)
        }
    }

    private fun setupOccluder() {
        val engine = EngineInstance.getEngine().filamentEngine
        val renderableManager = engine.renderableManager

        occluderNode.renderableInstance?.filamentAsset?.let { asset ->
            val r = 7f / 255
            val g = 7f / 225
            val b = 143f / 225
            val a = 0.3f
            for (entity in asset.entities) {
                val renderable = renderableManager.getInstance(entity)
                if (renderable != 0) {
                    val materialInstance = renderableManager.getMaterialInstanceAt(renderable, 0)
                    materialInstance.setParameter(BASE_COLOR_FACTOR, r, g, b, a)
                }
            }
        }
    }

    private fun setupRobotModel() {
        with(robotNode) {
            localPosition = location.localPosition
            localRotation = location.localRotation
            localScale = location.scale

            renderableInstance.animate(true)
                .start()
        }
    }

    private fun restartVpsService(
        mobileVpsEnable: Boolean,
        gpsEnable: Boolean,
    ) {
        vpsService.stopVpsService()

        vpsConfig = vpsConfig.copy(
            localizationType = if (mobileVpsEnable) MobileVps() else Photo,
            useGps = gpsEnable
        )
        vpsService.setVpsConfig(vpsConfig)
        updateOccluderState()

        vpsService.startVpsService()
    }

    private fun updateVpsStatus(isSuccess: Boolean) {
        val indicatorColor = if (isSuccess) Color.GREEN else Color.RED
        binding.vIndicator.background.setTint(indicatorColor)

        lifecycleScope.launchWhenCreated {
            delay(INDICATOR_COLOR_DELAY)
            binding.vIndicator.background.setTint(Color.WHITE)
        }
    }

    private fun showError(e: Throwable) {
        errorDialog?.let { it.findViewById<TextView>(android.R.id.message).text = e.toString() }
            ?.also { return }

        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(e.toString())
            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { errorDialog = null }
            .show()
            .also { errorDialog = it }
    }

}