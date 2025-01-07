package pdm.application.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class GyroscopeSensorManager(
    context: Context,
    private val onSortingChanged: (SortingCriteria, Boolean) -> Unit
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var currentRotationX = 0f // Forward/backward tilt
    private var currentRotationY = 0f // Left/right tilt

    private var currentSortingCriteria = SortingCriteria.DATE
    private var isAscending = true

    private val tiltThreshold = 1.5f // Adjust sensitivity
    private var lastTiltActionTime = 0L
    private val tiltCooldown = 1000L // Prevent too frequent changes
    private var accumulatedRotationX = 0f
    private var accumulatedRotationY = 0f
    private val accumulationThreshold = 2.0f  // Need to accumulate this much rotation

    enum class SortingCriteria {
        DATE,
        PRIZE_POOL,
        PARTICIPANTS,
        REGISTRATION_STATUS
    }

    fun startListening() {
        gyroscope?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTiltActionTime < tiltCooldown) {
                return
            }

            // Accumulate rotation
            accumulatedRotationX += abs(event.values[0]) // Use absolute values
            accumulatedRotationY += abs(event.values[1])

            // Only trigger if we've accumulated enough motion
            if (accumulatedRotationX > accumulationThreshold ||
                accumulatedRotationY > accumulationThreshold) {

                // Check direction of tilt
                when {
                    event.values[0] > tiltThreshold -> {
                        if (!isAscending) {
                            isAscending = true
                            onSortingChanged(currentSortingCriteria, isAscending)
                            lastTiltActionTime = currentTime
                        }
                    }
                    event.values[0] < -tiltThreshold -> {
                        if (isAscending) {
                            isAscending = false
                            onSortingChanged(currentSortingCriteria, isAscending)
                            lastTiltActionTime = currentTime
                        }
                    }
                    event.values[1] > tiltThreshold -> {
                        currentSortingCriteria = getNextSortingCriteria()
                        onSortingChanged(currentSortingCriteria, isAscending)
                        lastTiltActionTime = currentTime
                    }
                    event.values[1] < -tiltThreshold -> {
                        currentSortingCriteria = getPreviousSortingCriteria()
                        onSortingChanged(currentSortingCriteria, isAscending)
                        lastTiltActionTime = currentTime
                    }
                }

                // Reset accumulation after triggering
                accumulatedRotationX = 0f
                accumulatedRotationY = 0f
            }

            // Gradually decay accumulated rotation to prevent false triggers
            accumulatedRotationX *= 0.95f
            accumulatedRotationY *= 0.95f
        }
    }

    private fun getNextSortingCriteria(): SortingCriteria {
        return when (currentSortingCriteria) {
            SortingCriteria.DATE -> SortingCriteria.PRIZE_POOL
            SortingCriteria.PRIZE_POOL -> SortingCriteria.PARTICIPANTS
            SortingCriteria.PARTICIPANTS -> SortingCriteria.REGISTRATION_STATUS
            SortingCriteria.REGISTRATION_STATUS -> SortingCriteria.DATE
        }
    }

    private fun getPreviousSortingCriteria(): SortingCriteria {
        return when (currentSortingCriteria) {
            SortingCriteria.DATE -> SortingCriteria.REGISTRATION_STATUS
            SortingCriteria.PRIZE_POOL -> SortingCriteria.DATE
            SortingCriteria.PARTICIPANTS -> SortingCriteria.PRIZE_POOL
            SortingCriteria.REGISTRATION_STATUS -> SortingCriteria.PARTICIPANTS
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
}