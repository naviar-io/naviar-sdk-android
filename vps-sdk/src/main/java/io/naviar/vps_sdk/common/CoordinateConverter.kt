package io.naviar.vps_sdk.common

import com.google.ar.sceneform.math.Vector3
import io.naviar.vps_sdk.domain.model.GpsPoseModel
import io.naviar.vps_sdk.domain.model.LocalizationModel
import io.naviar.vps_sdk.domain.model.NodePoseModel
import io.naviar.vps_sdk.util.toRadians
import org.koin.core.context.GlobalContext
import kotlin.math.cos
import kotlin.math.sin

class CoordinateConverter internal constructor() {

    companion object {
        private const val TWO_PI_DEGREES = 360

        private const val MERIDIAN_ONE_DEGREES_DISTANCE = 40007.863 * 1000.0 / TWO_PI_DEGREES
        private const val EQUATOR_ONE_DEGREES_DISTANCE = 40075.0 * 1000.0 / TWO_PI_DEGREES

        fun instance(): CoordinateConverter =
            GlobalContext.get().get()
    }

    private var prevVPSPose: NodePoseModel = NodePoseModel.EMPTY
    private var prevGPSPose: GpsPoseModel = GpsPoseModel.EMPTY

    private var angleDifference: Float = 0f

    internal fun updatePoseModel(localizationModel: LocalizationModel) {
        prevVPSPose = localizationModel.vpsPose
        prevGPSPose = localizationModel.gpsPose
        angleDifference = -localizationModel.gpsPose.heading - localizationModel.vpsPose.ry
    }

    fun convertToGlobalCoordinate(cameraPoseModel: NodePoseModel): GpsPoseModel {
        if (prevVPSPose == NodePoseModel.EMPTY) return GpsPoseModel.EMPTY

        val prevCoordinate = rotatedCoordinate(angleDifference, prevVPSPose.getPosition())
        val currentCoordinate = rotatedCoordinate(angleDifference, cameraPoseModel.getPosition())
        val coordinate = calculateGpsCoordinate(prevCoordinate, currentCoordinate, prevGPSPose)

        var heading = cameraPoseModel.ry + angleDifference
        when {
            heading < 0 -> heading += TWO_PI_DEGREES
            heading > TWO_PI_DEGREES -> heading -= TWO_PI_DEGREES
        }
        return GpsPoseModel(0.0, coordinate.latitude, coordinate.longitude, heading)
    }

    fun convertToLocalCoordinate(gpsPoseModel: GpsPoseModel): NodePoseModel {
        if (prevVPSPose == NodePoseModel.EMPTY) return NodePoseModel.EMPTY

        val position = calculateArCoreCoordinate(
            prevGPSPose,
            gpsPoseModel,
            prevVPSPose.getPosition(),
            -angleDifference
        )
        val angleY = -gpsPoseModel.heading - angleDifference
        return NodePoseModel(position.x, position.y, position.z, 0f, angleY, 0f)
    }

    private fun calculateGpsCoordinate(
        prevCoordinate: Vector3,
        currentCoordinate: Vector3,
        gpsPoseModel: GpsPoseModel
    ): GpsPoseModel {
        val dx = currentCoordinate.x - prevCoordinate.x
        val dz = currentCoordinate.z - prevCoordinate.z

        val oneDegreesLongitude =
            cos(Math.toRadians(gpsPoseModel.latitude)) * EQUATOR_ONE_DEGREES_DISTANCE

        val latitude = gpsPoseModel.latitude - dz / MERIDIAN_ONE_DEGREES_DISTANCE
        val longitude = gpsPoseModel.longitude + dx / oneDegreesLongitude

        return GpsPoseModel(latitude = latitude, longitude = longitude)
    }

    private fun calculateArCoreCoordinate(
        prevGpsPoseModel: GpsPoseModel,
        gpsPoseModel: GpsPoseModel,
        prevNodePosition: Vector3,
        angleDifference: Float,
    ): Vector3 {
        val prevLatitude = prevGpsPoseModel.latitude
        val prevLongitude = prevGpsPoseModel.longitude
        val latitude = gpsPoseModel.latitude
        val longitude = gpsPoseModel.longitude

        val oneDegreesLongitude = cos(Math.toRadians(prevLatitude)) * EQUATOR_ONE_DEGREES_DISTANCE
        val dx = (longitude - prevLongitude) * oneDegreesLongitude
        val dz = (latitude - prevLatitude) * MERIDIAN_ONE_DEGREES_DISTANCE

        val coordinateDifference = rotatedCoordinate(
            angleDifference,
            Vector3(dx.toFloat(), prevNodePosition.y, dz.toFloat())
        )
        return Vector3(
            prevNodePosition.x + coordinateDifference.x,
            prevNodePosition.y,
            prevNodePosition.z + coordinateDifference.z
        )
    }

    private fun rotatedCoordinate(angle: Float, point: Vector3): Vector3 {
        val rad = angle.toRadians()
        val newX = point.x * cos(rad) + point.z * sin(rad)
        val newZ = point.x * sin(rad) - point.z * cos(rad)
        return Vector3(-newX, point.y, newZ)
    }

}