package com.example.telemetrylab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.telemetrylab.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.system.measureTimeMillis

class TelemetryService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_COMPUTE_LOAD = "EXTRA_COMPUTE_LOAD"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "telemetry_channel"

        private val _frameFlow = MutableSharedFlow<FrameData>()
        val frameFlow = _frameFlow.asSharedFlow()

        fun startService(context: Context, computeLoad: Int) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_COMPUTE_LOAD, computeLoad)
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateComputeLoad(context: Context, computeLoad: Int) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_COMPUTE_LOAD, computeLoad)
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var processingJob: Job? = null
    private var currentComputeLoad = 2
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentComputeLoad = intent.getIntExtra(EXTRA_COMPUTE_LOAD, 2)
                if (!isRunning) {
                    startForegroundService()
                    startProcessing()
                } else {

                    updateProcessing()
                }
            }
            ACTION_STOP -> {
                stopProcessing()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        isRunning = true
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telemetry Lab")
            .setContentText("Processing frames at ${getFrameRate()} Hz")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telemetry Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows telemetry processing status"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startProcessing() {
        processingJob?.cancel()
        processingJob = serviceScope.launch {
            while (isActive) {
                val frameTime = measureTimeMillis {
                    processFrame()
                }

                runCatching {
                    _frameFlow.emit(FrameData(
                        latency = frameTime,
                        timestamp = System.currentTimeMillis()
                    ))
                }

                delay(getFrameDelay())
            }
        }
    }

    private fun updateProcessing() {

        startProcessing()


        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun stopProcessing() {
        processingJob?.cancel()
        processingJob = null
        isRunning = false
    }

    private suspend fun processFrame() = withContext(Dispatchers.Default) {
        val size = 256
        val array = Array(size) { FloatArray(size) { kotlin.random.Random.nextFloat() } }
        val kernel = arrayOf(
            floatArrayOf(0.1f, 0.1f, 0.1f),
            floatArrayOf(0.1f, 0.2f, 0.1f),
            floatArrayOf(0.1f, 0.1f, 0.1f)
        )

        val adjustedLoad = getAdjustedComputeLoad()

        repeat(adjustedLoad) {
            convolve(array, kernel)
        }

        val flat = array.flatMap { it.toList() }
        val mean = flat.average().toFloat()
        val std = kotlin.math.sqrt(
            flat.map { value -> (value - mean) * (value - mean) }.average()
        ).toFloat()
    }

    private fun convolve(array: Array<FloatArray>, kernel: Array<FloatArray>): Array<FloatArray> {
        val rows = array.size
        val cols = array[0].size
        val result = Array(rows) { FloatArray(cols) }

        for (i in 1 until rows - 1) {
            for (j in 1 until cols - 1) {
                var sum = 0f
                for (ki in -1..1) {
                    for (kj in -1..1) {
                        sum += array[i + ki][j + kj] * kernel[ki + 1][kj + 1]
                    }
                }
                result[i][j] = sum
            }
        }

        return result
    }

    private fun getFrameRate(): Int {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (powerManager.isPowerSaveMode) 10 else 20
    }

    private fun getFrameDelay(): Long {
        val frameRate = getFrameRate()
        return 1000L / frameRate
    }

    private fun getAdjustedComputeLoad(): Int {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (powerManager.isPowerSaveMode) {
            maxOf(1, currentComputeLoad - 1)
        } else {
            currentComputeLoad
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    data class FrameData(
        val latency: Long,
        val timestamp: Long
    )
}