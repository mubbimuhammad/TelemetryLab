package com.example.telemetrylab.utils

import androidx.metrics.performance.FrameData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

class JankStatsCollector {

    data class JankStats(
        val jankPercentage: Float = 0f,
        val totalJankFrames: Int = 0,
        val totalFrames: Int = 0
    )

    private val frameDataQueue = ConcurrentLinkedQueue<FrameData>()
    private val thirtySecondsInNanos = 30_000_000_000L

    private val _jankStatsFlow = MutableStateFlow(JankStats())
    val jankStatsFlow = _jankStatsFlow.asStateFlow()

    fun recordFrame(frameData: FrameData) {
        frameDataQueue.offer(frameData)
        cleanupOldFrames()
        updateStats()
    }

    private fun cleanupOldFrames() {
        val cutoffTime = System.nanoTime() - thirtySecondsInNanos

        while (frameDataQueue.isNotEmpty()) {
            val oldestFrame = frameDataQueue.peek()
            if (oldestFrame.frameStartNanos < cutoffTime) {
                frameDataQueue.poll()
            } else {
                break
            }
        }
    }

    private fun updateStats() {
        val frames = frameDataQueue.toList()
        if (frames.isEmpty()) {
            _jankStatsFlow.value = JankStats()
            return
        }

        val totalFrames = frames.size
        val jankFrames = frames.count { it.isJank }
        val jankPercentage = jankFrames.toFloat() / totalFrames * 100

        _jankStatsFlow.value = JankStats(
            jankPercentage = jankPercentage,
            totalJankFrames = jankFrames,
            totalFrames = totalFrames
        )
    }

    fun reset() {
        frameDataQueue.clear()
        _jankStatsFlow.value = JankStats()
    }
}