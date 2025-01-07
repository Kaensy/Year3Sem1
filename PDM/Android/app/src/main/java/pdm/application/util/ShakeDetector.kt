package pdm.application.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val shakeThreshold = 800f // Adjust this value to change sensitivity

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        // Only process if sufficient time has passed between events
        if ((currentTime - lastUpdate) > 100) {
            val diffTime = currentTime - lastUpdate
            lastUpdate = currentTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val speed = sqrt(
                (x - lastX) * (x - lastX) +
                        (y - lastY) * (y - lastY) +
                        (z - lastZ) * (z - lastZ)
            ) / diffTime * 10000

            if (speed > shakeThreshold) {
                onShake()
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
}