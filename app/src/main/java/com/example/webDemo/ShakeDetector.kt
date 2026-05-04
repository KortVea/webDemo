package com.example.webDemo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects shake gestures using the accelerometer.
 *
 * Reports axis ("x" or "y") and normalized intensity (0..1) via [onShake].
 */
class ShakeDetector(
    context: Context,
    private val onShake: (axis: String, intensity: Double) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** Minimum acceleration (m/s²) beyond gravity to count as a shake.
     *  Lowered to 3.0 so emulator virtual-sensor sliders can trigger it. */
    private val shakeThreshold = 3.0

    /** Cool-down between reported shakes (ms). */
    private val cooldownMs = 300L

    private var lastShakeTime = 0L

    // Previous sample for high-pass filtering
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var initialized = false

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        initialized = false
    }

    // ---- SensorEventListener ----

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!initialized) {
            lastX = x; lastY = y; lastZ = z
            initialized = true
            return
        }

        val dx = x - lastX
        val dy = y - lastY
        val dz = z - lastZ
        lastX = x; lastY = y; lastZ = z

        val magnitude = sqrt((dx * dx + dy * dy + dz * dz).toDouble())
        Log.d("ShakeDetector", "magnitude=${"%.2f".format(magnitude)}")
        if (magnitude < shakeThreshold) return

        val now = System.currentTimeMillis()
        if (now - lastShakeTime < cooldownMs) return
        lastShakeTime = now

        // Determine dominant axis (ignore z for our use-case)
        val axis = if (abs(dx) >= abs(dy)) "x" else "y"

        // Normalize intensity
        val maxExpected = 20.0
        val intensity = ((magnitude - shakeThreshold) / (maxExpected - shakeThreshold))
            .coerceIn(0.0, 1.0)

        Log.d("ShakeDetector", "Shake detected: axis=$axis, intensity=${"%.2f".format(intensity)}")
        onShake(axis, intensity)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
