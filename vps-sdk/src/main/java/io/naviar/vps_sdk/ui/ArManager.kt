package io.naviar.vps_sdk.ui

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.annotation.MainThread
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.utilities.AndroidPreconditions
import io.naviar.vps_sdk.data.VpsConfig
import io.naviar.vps_sdk.data.model.CameraIntrinsics
import io.naviar.vps_sdk.domain.model.NodePoseModel
import io.naviar.vps_sdk.util.Constant.QUALITY
import io.naviar.vps_sdk.util.Quaternion
import io.naviar.vps_sdk.util.getEulerAngles
import io.naviar.vps_sdk.util.getQuaternion
import io.naviar.vps_sdk.util.getTranslation
import java.io.ByteArrayOutputStream

internal class ArManager : Scene.OnUpdateListener {

    private companion object {
        const val WORLD_NODE_NAME = "World"
        const val MS_IN_SEC = 1000f

        const val UPDATE_WORLD_DURATION = 1f
        const val UPDATE_WORLD_DISTANCE_LIMIT = 2f
        const val UPDATE_WORLD_ANGLE_LIMIT = 10f
        val FORWARD: Vector3 = Vector3.forward()
    }

    val worldNode: Node by lazy {
        Node()
            .apply {
                name = WORLD_NODE_NAME
                addChild(poseInWorldNode)
            }
    }
    private val poseInWorldNode: Node = Node()

    private var arSceneView: ArSceneView? = null

    private val camera: Camera
        get() = checkArSceneView().scene.camera

    private val prevWorldPoseMatrix: Matrix = Matrix()
    private val nextWorldPoseMatrix: Matrix = Matrix()

    private var updateWorldDuration: Float = UPDATE_WORLD_DURATION
    private var updateWorldDistanceLimit: Float = UPDATE_WORLD_DISTANCE_LIMIT
    private var updateWorldAngleLimit: Float = UPDATE_WORLD_ANGLE_LIMIT
    private var updateWorldTimer: Float = -1f

    fun init(arSceneView: ArSceneView, vpsConfig: VpsConfig) {
        this.arSceneView = arSceneView

        this.updateWorldDuration = vpsConfig.updateWorldDurationMS / MS_IN_SEC
        this.updateWorldDistanceLimit = vpsConfig.updateWorldDistanceLimit
        this.updateWorldAngleLimit = vpsConfig.updateWorldAngleLimit

        arSceneView.scene.addOnUpdateListener(this)
    }

    override fun onUpdate(frameTime: FrameTime) {
        updateWorldNodePose(frameTime.deltaSeconds)
    }

    fun destroy() {
        val arSceneView = arSceneView ?: return

        worldNode.renderable = null
        detachWorldNode()
        arSceneView.scene.removeOnUpdateListener(this)
        this.arSceneView = null
        updateWorldTimer = -1f
    }

    fun detachWorldNode() {
        arSceneView?.scene?.removeChild(worldNode)
    }

    fun restoreCameraPose(vpsPose: NodePoseModel, trackingPose: NodePoseModel) {
        val arSceneView = arSceneView ?: return

        if (!arSceneView.scene.children.contains(worldNode)) {
            arSceneView.scene.addChild(worldNode)
        }

        val trackingPoseMatrix = getCameraPoseMatrix(
            Vector3(trackingPose.x, trackingPose.y, trackingPose.z),
            Quaternion(trackingPose.rx, trackingPose.ry, trackingPose.rz)
        )
        val vpsPoseMatrix = getVpsPoseMatrix(vpsPose)

        Matrix.multiply(trackingPoseMatrix, vpsPoseMatrix, nextWorldPoseMatrix)
        prevWorldPoseMatrix.set(worldNode.worldModelMatrix)

        updateWorldTimer = updateWorldDuration
    }

    @MainThread
    fun getCameraPose(): NodePoseModel {
        val position = camera.worldPosition
        val rotation = camera.worldRotation
            .getEulerAngles()

        return NodePoseModel(
            x = position.x,
            y = position.y,
            z = position.z,
            rx = rotation.x,
            ry = rotation.y,
            rz = rotation.z,
        )
    }

    @MainThread
    fun getCameraLocalPose(): NodePoseModel {
        poseInWorldNode.worldPosition = camera.worldPosition
        poseInWorldNode.worldRotation = camera.worldRotation

        val localPosition = poseInWorldNode.localPosition
        val localRotation = poseInWorldNode.localRotation
            .getEulerAngles()

        return NodePoseModel(
            x = localPosition.x,
            y = localPosition.y,
            z = localPosition.z,
            rx = localRotation.x,
            ry = localRotation.y,
            rz = localRotation.z,
        )
    }

    @MainThread
    fun acquireCameraImageAsByteArray(): ByteArray {
        AndroidPreconditions.checkUiThread()
        val image = checkArSceneView().arFrame?.acquireCameraImage()
            ?: throw IllegalStateException("Frame is null")
        return image.toByteArray()
            .also { image.close() }
    }

    fun getCameraIntrinsics(): CameraIntrinsics {
        val imageIntrinsics = arSceneView?.arFrame?.camera?.imageIntrinsics
            ?: return CameraIntrinsics.DEFAULT

        val focalLength = imageIntrinsics.focalLength        //The order of values is {fx, fy}.
        val principalPoint = imageIntrinsics.principalPoint  //The order of values is {cx, cy}.

        // By default used horizontal orientation
        // For portrait orientation changed order of values
        return CameraIntrinsics(
            fx = focalLength[1],
            fy = focalLength[0],
            cx = principalPoint[1],
            cy = principalPoint[0],
            width = imageIntrinsics.imageDimensions[1],
            height = imageIntrinsics.imageDimensions[0]
        )
    }

    private fun checkArSceneView(): ArSceneView =
        checkNotNull(arSceneView) { "ArSceneView is null. Call bindArSceneView(ArSceneView)" }

    private fun getCameraPoseMatrix(position: Vector3, rotation: Quaternion): Matrix =
        Matrix().apply {
            makeTrs(position, rotation.alignHorizontal(), Vector3.one())
        }

    private fun getVpsPoseMatrix(nodePose: NodePoseModel): Matrix =
        Matrix().apply {
            val positionMatrix = Matrix()
                .apply { makeTranslation(nodePose.getPosition()) }
            val rotationMatrix = Matrix()
                .apply { makeRotation(nodePose.getRotation().alignHorizontal()) }

            Matrix.multiply(rotationMatrix, positionMatrix, this)
        }

    private fun updateWorldNodePose(deltaTime: Float) {
        if (updateWorldTimer < 0f) return

        updateWorldTimer -= deltaTime
        val ratio = 1f - maxOf(0f, updateWorldTimer / updateWorldDuration)

        updateWorldNodePosition(ratio)
        updateWorldNodeRotation(ratio)
    }

    private fun updateWorldNodePosition(ratio: Float) {
        val newPosition = nextWorldPoseMatrix.getTranslation()

        if (!worldNode.localPosition.equals(newPosition)) {
            val prevPosition = prevWorldPoseMatrix.getTranslation()
            worldNode.localPosition =
                if (length(prevPosition, newPosition) < updateWorldDistanceLimit) {
                    Vector3.lerp(prevPosition, newPosition, ratio)
                } else {
                    newPosition
                }
        }
    }

    private fun updateWorldNodeRotation(ratio: Float) {
        val newRotation = nextWorldPoseMatrix.getQuaternion()

        if (!worldNode.localRotation.equals(newRotation)) {
            val prevRotation = prevWorldPoseMatrix.getQuaternion()
            worldNode.localRotation =
                if (length(prevRotation, newRotation) < updateWorldAngleLimit) {
                    Quaternion.slerp(prevRotation, newRotation, ratio)
                } else {
                    newRotation
                }
        }
    }

    private fun length(lhs: Vector3, rhs: Vector3): Float =
        Vector3.subtract(lhs, rhs).length()

    private fun length(lhs: Quaternion, rhs: Quaternion): Float =
        Vector3.angleBetweenVectors(
            Quaternion.rotateVector(lhs, FORWARD),
            Quaternion.rotateVector(rhs, FORWARD)
        )

    private fun Image.toByteArray(): ByteArray {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        return ByteArrayOutputStream().use { out ->
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), QUALITY, out)
            out.toByteArray()
        }
    }

    private fun Quaternion.alignHorizontal(): Quaternion {
        val dir = Quaternion.rotateVector(this, FORWARD)
        dir.y = 0f
        return Quaternion.rotationBetweenVectors(FORWARD, dir)
    }

}