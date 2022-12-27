package io.naviar.vps_android_prototype.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import io.naviar.vps_android_prototype.R
import io.naviar.vps_android_prototype.databinding.AcMainBinding

class MainActivity : AppCompatActivity(R.layout.ac_main) {

    private val binding: AcMainBinding by viewBinding(AcMainBinding::bind)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }

}