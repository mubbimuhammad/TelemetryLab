package com.example.telemetrylab

import android.app.Application
import com.example.telemetrylab.utils.JankStatsCollector

class TelemetryLabApplication : Application() {

    lateinit var jankStatsCollector: JankStatsCollector
        private set

    override fun onCreate() {
        super.onCreate()
        jankStatsCollector = JankStatsCollector()
    }
}