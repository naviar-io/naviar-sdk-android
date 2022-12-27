package io.naviar.vps_sdk.ui

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Bundle
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import io.naviar.vps_sdk.common.CompassManager
import io.naviar.vps_sdk.common.CoordinateConverter
import io.naviar.vps_sdk.data.VpsConfig
import io.naviar.vps_sdk.data.model.CameraIntrinsics
import io.naviar.vps_sdk.domain.interactor.IVpsInteractor
import io.naviar.vps_sdk.domain.model.GpsLocationModel
import io.naviar.vps_sdk.domain.model.LocalizationModel
import io.naviar.vps_sdk.domain.model.NodePoseModel
import io.naviar.vps_sdk.domain.model.VpsLocationModel
import io.naviar.vps_sdk.ui.VpsService.State
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.util.*
import kotlin.properties.Delegates.notNull

internal class VpsServiceImpl(
    private val vpsInteractor: IVpsInteractor,
    private val arManager: ArManager,
    private val locationManager: LocationManager,
    private val compassManager: CompassManager,
    private val coordinateConverter: CoordinateConverter
) : VpsService {

    private companion object {
        const val MIN_INTERVAL_MS = 0L
        const val MIN_DISTANCE_IN_METERS = 1f
        const val UI_DELAY = 100L
        const val FIRST_LOCALIZATION_DELAY = 1000L
    }

    override val worldNode: Node
        get() = arManager.worldNode

    override val isRun: Boolean
        get() = vpsJob != null

    override val cameraPose: NodePoseModel
        get() = arManager.getCameraLocalPose()

    private val vpsHandlerException: CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            handleException(throwable)
        }

    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() +
                Dispatchers.Main +
                vpsHandlerException
    )

    private var vpsJob: Job? = null
    private var state: State = State.STOP

    private var lastLocalization: LocalizationModel = LocalizationModel.EMPTY

    private lateinit var vpsConfig: VpsConfig
    private var vpsCallback: VpsCallback? = null

    private var locationListener: LocationListener? = null

    private var hasFocus: Boolean = false

    private var sessionId: String by notNull()

    override fun bindArSceneView(arSceneView: ArSceneView) {
        arManager.init(arSceneView, vpsConfig)
    }

    override fun resume() {
        hasFocus = true
        if (state == State.PAUSE) {
            internalStartVpsService()
        }
    }

    override fun pause() {
        hasFocus = false
        if (state == State.RUN) {
            state = State.PAUSE
            internalStopVpsService()
        }
    }

    override fun destroy() {
        stopVpsService()
        arManager.destroy()
        vpsInteractor.destroy()
    }

    override fun setVpsConfig(vpsConfig: VpsConfig) {
        this.vpsConfig = vpsConfig
    }

    override fun setVpsCallback(vpsCallback: VpsCallback) {
        this.vpsCallback = vpsCallback
    }

    override fun startVpsService() {
        if (!::vpsConfig.isInitialized)
            throw IllegalStateException("VpsConfig not set. First call setVpsConfig(VpsConfig)")

        internalStartVpsService()
    }

    override fun stopVpsService() {
        if (state != State.STOP) {
            state = State.STOP
            internalStopVpsService()
        }
    }

    private fun internalStartVpsService() {
        if (vpsJob != null) return

        requestLocationIfNeed()
        compassManager.start()

        vpsJob = scope.launch(Dispatchers.Default) {
            while (!hasFocus) delay(UI_DELAY)

            if (state != State.RUN) {
                state = State.RUN
                withContext(Dispatchers.Main) {
                    vpsCallback?.onStateChange(state)
                }
            }
            localization()
        }
    }

    private fun internalStopVpsService() {
        vpsCallback?.onStateChange(state)

        if (vpsJob == null) return

        compassManager.stop()
        coordinateConverter.updatePoseModel(LocalizationModel.EMPTY)

        arManager.detachWorldNode()
        stopRequestLocation()
        vpsJob?.cancel()
        vpsJob = null
    }

    private suspend fun localization() {
        var firstLocalize = true
        var failureCount = 0
        sessionId = UUID.randomUUID().toString()

        while (state == State.RUN) {
            if (failureCount == vpsConfig.failsCountToResetSession) {
                sessionId = UUID.randomUUID().toString()
                failureCount = 0
            }

            val startTimeMillis = System.currentTimeMillis()
            val vpsLocationModel = withContext(Dispatchers.Default) { createVpsLocationModel() }
            val duration = System.currentTimeMillis() - startTimeMillis

            CoroutineScope(Dispatchers.IO).launch {
                val result = vpsInteractor.calculateNodePose(vpsConfig.vpsUrl, vpsLocationModel)
                if (vpsJob?.isActive != true) return@launch

                if (result == null) {
                    failureCount++
                    withContext(Dispatchers.Main) {
                        vpsCallback?.onFail()
                    }
                } else {
                    failureCount = 0
                    firstLocalize = false
                    successLocalization(result)
                }
            }

            val delay = if (firstLocalize)
                FIRST_LOCALIZATION_DELAY
            else
                vpsConfig.intervalLocalizationMS
            delay(delay - duration)
        }
    }

    private suspend fun successLocalization(localizationModel: LocalizationModel) {
        withContext(Dispatchers.Main) {
            arManager.restoreCameraPose(localizationModel.vpsPose, localizationModel.trackingPose)
            vpsCallback?.onSuccess()
        }
        coordinateConverter.updatePoseModel(localizationModel)

        lastLocalization = localizationModel
    }

    private suspend fun createVpsLocationModel(): VpsLocationModel {
        val byteArray: ByteArray
        val cameraPose: NodePoseModel
        val cameraIntrinsics: CameraIntrinsics

        val gpsLocation = if (vpsConfig.useGps) getLastKnownLocation() else null

        withContext(Dispatchers.Main) {
            byteArray = waitAcquireCameraImage()
            cameraIntrinsics = arManager.getCameraIntrinsics()
            cameraPose = arManager.getCameraPose()
        }

        return vpsInteractor.prepareVpsLocationModel(
            locationIds = vpsConfig.locationIds,
            source = byteArray,
            sessionId = sessionId,
            localizationType = vpsConfig.localizationType,
            cameraPose = cameraPose,
            gpsLocation = gpsLocation,
            compass = compassManager.getCompassModel(),
            cameraIntrinsics = cameraIntrinsics
        )
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationIfNeed() {
        if (vpsConfig.useGps && locationListener == null) {
            locationListener = DummyLocationListener()
                .also { locationListener ->
                    locationManager.requestLocationUpdates(
                        GPS_PROVIDER,
                        MIN_INTERVAL_MS,
                        MIN_DISTANCE_IN_METERS,
                        locationListener
                    )
                }
        }
    }

    private fun stopRequestLocation() {
        locationManager.removeUpdates(locationListener ?: return)
        locationListener = null
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): GpsLocationModel? =
        if (locationManager.isProviderEnabled(GPS_PROVIDER)) {
            locationManager.getLastKnownLocation(GPS_PROVIDER)
                ?.let { GpsLocationModel.from(it) }
        } else {
            null
        }

    private suspend fun waitAcquireCameraImage(): ByteArray {
        var byteArray: ByteArray? = null
        while (byteArray == null) {
            try {
                delay(UI_DELAY)
                byteArray = arManager.acquireCameraImageAsByteArray()
            } catch (e: NotYetAvailableException) {
            }
        }
        return byteArray
    }

    private fun handleException(throwable: Throwable) {
        scope.launch {
            if (throwable is HttpException) {
                vpsJob = null
                internalStartVpsService()
            } else {
                stopVpsService()
            }
            vpsCallback?.onError(throwable)
        }
    }

    private class DummyLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) = Unit
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

}