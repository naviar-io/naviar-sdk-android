package io.naviar.vps_android_prototype.ui

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import io.naviar.vps_android_prototype.R

sealed interface Location {
    val locationIds: Array<String>
    val occluderRawId: Int
    val localPosition: Vector3
    val localRotation: Quaternion
    val scale: Vector3

    object Polytech : Location {
        override val locationIds: Array<String> = arrayOf("polytech")
        override val occluderRawId: Int = R.raw.polytech
        override val localPosition: Vector3 = Vector3.zero()
        override val localRotation: Quaternion = Quaternion.identity()
        override val scale: Vector3 = Vector3.one()
    }

    object VDNH_Arch : Location {
        override val locationIds: Array<String> = Array<String>(1){"vdnh_arka"}
        override val occluderRawId: Int = R.raw.vdnh_arch
        override val localPosition: Vector3 = Vector3(25f, -4f, 0f)
        override val localRotation: Quaternion = Quaternion(  0f,   -0.906f,   0f,   -0.423f)
        override val scale: Vector3 = Vector3.one()
    }

    object VDNH_Pavilion : Location {
        override val locationIds: Array<String> = Array<String>(1){"vdnh_pavilion"}
        override val occluderRawId: Int = R.raw.vdnh_pavilion
        override val localPosition: Vector3 = Vector3(80f, -15f, 10f)
        override val localRotation: Quaternion = Quaternion(  0f, -0.819f,   0f,  -0.574f)
        override val scale: Vector3 = Vector3.one()
    }

    object ArtPlay : Location {
        override val locationIds: Array<String> = Array<String>(1){"artplay"}
        override val occluderRawId: Int = R.raw.artplay
        override val localPosition: Vector3 = Vector3(51.8f, -8.4f, 26.3f)
        override val localRotation: Quaternion = Quaternion(0f, 0.1053096f, 0f, 0.9944395f)
        override val scale: Vector3 = Vector3(0.5f, 0.5f, 0.5f)
    }

    object Flacon : Location {
        override val locationIds: Array<String> = Array<String>(1){"flacon"}
        override val occluderRawId: Int = R.raw.flacon
        override val localPosition: Vector3 = Vector3(-50.3f, -3.9f, 5.3f)
        override val localRotation: Quaternion = Quaternion(0f, 0.0725419f, 0f, 0.9973654f)
        override val scale: Vector3 = Vector3(0.5f, 0.5f, 0.5f)
    }

    object Gorky : Location {
        override val locationIds: Array<String> = Array<String>(1){"gorky_park"}
        override val occluderRawId: Int = R.raw.gorky_park
        override val localPosition: Vector3 = Vector3(-22f, 0f, -14.8f)
        override val localRotation: Quaternion = Quaternion(  0f, -0.866f,   0f, 0.5f)
        override val scale: Vector3 = Vector3(0.5f, 0.5f, 0.5f)
    }

    object Hlebzavod : Location {
        override val locationIds: Array<String> = Array<String>(1){"hlebozavod9"}
        override val occluderRawId: Int = R.raw.khlebzavod
        override val localPosition: Vector3 = Vector3(-32.3f, -17.2f, 83.3f)
        override val localRotation: Quaternion = Quaternion(  0f, 0f,   0f, 0f)
        override val scale: Vector3 = Vector3.one()
    }
}