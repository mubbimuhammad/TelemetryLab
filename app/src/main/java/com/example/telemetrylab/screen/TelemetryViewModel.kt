package com.example.telemetrylab.screen

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemetrylab.TelemetryLabApplication
import com.example.telemetrylab.service.TelemetryService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _computeLoad = MutableStateFlow(2f)
    val computeLoad = _computeLoad.asStateFlow()

    private val _currentLatency = MutableStateFlow(0L)
    val currentLatency = _currentLatency.asStateFlow()

    private val _averageLatency = MutableStateFlow(0L)
    val averageLatency = _averageLatency.asStateFlow()

    private val _jankPercentage = MutableStateFlow(0f)
    val jankPercentage = _jankPercentage.asStateFlow()

    private val _jankFrameCount = MutableStateFlow(0)
    val jankFrameCount = _jankFrameCount.asStateFlow()

    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode = _isPowerSaveMode.asStateFlow()

    private val _counter = MutableStateFlow(0)
    val counter = _counter.asStateFlow()

    private val frameLatencies = ArrayList<Long>()
    private val maxLatencyHistory = 600

    init {

        viewModelScope.launch {
            TelemetryService.frameFlow.collect { frameData ->
                updateFrameStats(frameData)
            }
        }


        viewModelScope.launch {
            checkPowerSaveMode()
        }


        viewModelScope.launch {
            while (true) {
                if (_isRunning.value) {
                    _counter.value = (_counter.value + 1) % 1000
                }
                kotlinx.coroutines.delay(100)
            }
        }


        viewModelScope.launch {
            val jankStatsCollector = (application as TelemetryLabApplication).jankStatsCollector
            jankStatsCollector.jankStatsFlow.collect { stats ->
                _jankPercentage.value = stats.jankPercentage
                _jankFrameCount.value = stats.totalJankFrames
            }
        }
    }

    fun toggleRunning() {
        val context = getApplication<Application>()
        if (_isRunning.value) {
            stopTelemetry(context)
        } else {
            startTelemetry(context)
        }
    }

    fun updateComputeLoad(load: Float) {
        _computeLoad.value = load
        val context = getApplication<Application>()
        if (_isRunning.value) {
            TelemetryService.updateComputeLoad(context, load.toInt())
        }
    }

    private fun startTelemetry(context: Context) {
        _isRunning.value = true
        frameLatencies.clear()
        TelemetryService.startService(context, _computeLoad.value.toInt())
    }

    private fun stopTelemetry(context: Context) {
        _isRunning.value = false
        TelemetryService.stopService(context)
        resetStats()
    }

    private fun updateFrameStats(frameData: TelemetryService.FrameData) {
        _currentLatency.value = frameData.latency

        frameLatencies.add(frameData.latency)
        if (frameLatencies.size > maxLatencyHistory) {
            frameLatencies.removeAt(0)
        }

        if (frameLatencies.isNotEmpty()) {
            _averageLatency.value = frameLatencies.average().toLong()
        }
    }

    private fun checkPowerSaveMode() {
        val context = getApplication<Application>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isPowerSaveMode.value = powerManager.isPowerSaveMode
    }

    private fun resetStats() {
        _currentLatency.value = 0
        _averageLatency.value = 0
        frameLatencies.clear()
    }
}