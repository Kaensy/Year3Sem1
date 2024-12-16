// Create new file: workers/CountdownWorker.kt
package pdm.application.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pdm.application.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CountdownWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting countdown work")

        val message = "Tournament check performed at ${
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        }"

        try {
            NotificationHelper(applicationContext).showBasicNotification(
                "Background Check",
                message,
                System.currentTimeMillis().toInt()
            )
            Log.d(TAG, "Notification sent successfully: $message")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            return Result.failure()
        }
    }

    companion object {
        private const val TAG = "CountdownWorker"
    }
}