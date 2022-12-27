package io.naviar.vps_sdk.util

import io.naviar.vps_sdk.common.CoordinateConverter
import io.naviar.vps_sdk.domain.model.GpsPoseModel
import io.naviar.vps_sdk.domain.model.LocalizationModel
import io.naviar.vps_sdk.domain.model.NodePoseModel
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CoordinateConverterTest {

    private companion object {
        val vpsPose = NodePoseModel(
            x = 10.660357f,
            y = 22.578745f,
            z = -11.782142f,
            rx = -3.6863844f,
            ry = 126.660576f,
            rz = -3.5424306f
        )
        val gpsPose = GpsPoseModel(
            altitude = 0.0,
            latitude = 55.73577496195804,
            longitude = 37.53170602738539,
            heading = 256.9994f
        )
    }

    private val coordinateConverter: CoordinateConverter = CoordinateConverter()

    @Before
    fun setup() {
        coordinateConverter.updatePoseModel(
            LocalizationModel(vpsPose, NodePoseModel.DEFAULT, gpsPose)
        )
    }

    @Test
    fun convertToGlobalCoordinate() {
        val result = coordinateConverter.convertToGlobalCoordinate(vpsPose)

        val expectedGpsPose = gpsPose.copy(heading = 360 - gpsPose.heading)
        Assert.assertEquals(expectedGpsPose, result)
    }

    @Test
    fun convertToGlobalCoordinate_empty() {
        coordinateConverter.updatePoseModel(LocalizationModel.EMPTY)
        val result = coordinateConverter.convertToGlobalCoordinate(NodePoseModel.EMPTY)

        Assert.assertEquals(GpsPoseModel.EMPTY, result)
    }

    @Test
    fun convertToLocalCoordinate() {
        val result = coordinateConverter.convertToLocalCoordinate(gpsPose)

        val expectedVpsPose = vpsPose.copy(
            x = -vpsPose.x,
            y = -vpsPose.y,
            z = -vpsPose.z,
            rx = 0f,
            ry = 126.66058f, //rounding error - 126.660576
            rz = 0f
        )
        Assert.assertEquals(expectedVpsPose, result)
    }

    @Test
    fun convertToLocalCoordinate_empty() {
        coordinateConverter.updatePoseModel(LocalizationModel.EMPTY)
        val result = coordinateConverter.convertToLocalCoordinate(GpsPoseModel.EMPTY)

        Assert.assertEquals(NodePoseModel.EMPTY, result)
    }

}