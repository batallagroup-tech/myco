package com.batallagroup.myco.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.batallagroup.myco.presentation.theme.MycoTheme
import com.batallagroup.myco.service.MycoBleService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MycoTheme {
                MycoApp()
            }
        }

        startBleService()
    }

    private fun startBleService() {
        val intent = Intent(this, MycoBleService::class.java)
        startForegroundService(intent)
    }
}
