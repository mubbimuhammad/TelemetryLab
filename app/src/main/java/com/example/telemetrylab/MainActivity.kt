package com.example.telemetrylab

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import com.example.telemetrylab.screen.TelemetryScreen
import com.example.telemetrylab.screen.TelemetryViewModel
import com.example.telemetrylab.ui.theme.TelemetryLabTheme

class MainActivity : ComponentActivity() {

    private lateinit var jankStats: JankStats
    private lateinit var metricsStateHolder: PerformanceMetricsState.Holder

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            TelemetryLabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: TelemetryViewModel = viewModel()
                    TelemetryScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()


        metricsStateHolder = PerformanceMetricsState.getHolderForHierarchy(window.decorView)


        jankStats = JankStats.createAndTrack(
            window,
            JankStats.OnFrameListener { frameData ->

                (application as? TelemetryLabApplication)?.jankStatsCollector?.recordFrame(frameData)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (::jankStats.isInitialized) {
            jankStats.isTrackingEnabled = true
        }
        if (::metricsStateHolder.isInitialized) {
            metricsStateHolder.state?.putState("Activity", "MainActivity")
        }
    }

    override fun onPause() {
        super.onPause()
        if (::jankStats.isInitialized) {
            jankStats.isTrackingEnabled = false
        }
    }
}