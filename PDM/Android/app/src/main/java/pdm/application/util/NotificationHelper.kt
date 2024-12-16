package pdm.application.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import pdm.application.MainActivity
import pdm.application.R
import java.util.Calendar

class NotificationHelper(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_TOURNAMENT_UPDATES = "tournament_updates"
        const val CHANNEL_REGISTRATION = "registration_reminders"
        const val CHANNEL_START_TIMES = "start_time_reminders"
        const val CHANNEL_RESULTS = "tournament_results"
        private const val TAG = "NotificationHelper"

        private const val NOTIFICATION_ID_UPDATE = 1000
        private const val NOTIFICATION_ID_REGISTRATION = 2000
        private const val NOTIFICATION_ID_START = 3000
        private const val NOTIFICATION_ID_RESULT = 4000
    }

    init {
        createNotificationChannels()
    }

    fun requestNotificationPermission(activity: FragmentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                activity.requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showNotification(title: String, message: String, notificationId: Int) {
        try {
            if (!hasNotificationPermission()) {
                return
            }
            val notification = buildBasicNotification(
                CHANNEL_TOURNAMENT_UPDATES,
                title,
                message
            ).build()
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle security exception
            e.printStackTrace()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_TOURNAMENT_UPDATES,
                    "Tournament Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Updates about tournament status changes"
                    enableLights(true)
                    lightColor = Color.BLUE
                },
                NotificationChannel(
                    CHANNEL_REGISTRATION,
                    "Registration Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders about tournament registration deadlines"
                    enableLights(true)
                    lightColor = Color.RED
                },
                NotificationChannel(
                    CHANNEL_START_TIMES,
                    "Start Time Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders about tournament start times"
                    enableLights(true)
                    lightColor = Color.GREEN
                },
                NotificationChannel(
                    CHANNEL_RESULTS,
                    "Tournament Results",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Updates about tournament results"
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun buildBasicNotification(
        channelId: String,
        title: String,
        message: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
    }

    fun showRegistrationReminder(
        tournamentName: String,
        hoursLeft: Int,
        tournamentId: String,
        message: String
    ) {
        if (!isAppropriateTimeForNotification()) return

        val notification = buildBasicNotification(
            CHANNEL_REGISTRATION,
            "Registration Reminder: $tournamentName",
            message,
            NotificationCompat.PRIORITY_HIGH
        ).apply {
            setCategory(NotificationCompat.CATEGORY_REMINDER)
        }.build()

        notificationManager.notify(NOTIFICATION_ID_REGISTRATION + tournamentId.hashCode(), notification)
    }

    fun showTournamentStartingReminder(
        tournamentName: String,
        startTime: Long,
        tournamentId: String,
        message: String
    ) {
        val notification = buildBasicNotification(
            CHANNEL_START_TIMES,
            "Tournament Starting: $tournamentName",
            message,
            NotificationCompat.PRIORITY_HIGH
        ).apply {
            setCategory(NotificationCompat.CATEGORY_EVENT)
        }.build()

        notificationManager.notify(NOTIFICATION_ID_START + tournamentId.hashCode(), notification)
    }

    fun showTournamentResultNotification(
        tournamentName: String,
        winner: String,
        tournamentId: String,
        message: String
    ) {
        if (!isAppropriateTimeForNotification()) return

        val notification = buildBasicNotification(
            CHANNEL_RESULTS,
            "Tournament Results: $tournamentName",
            message
        ).build()

        notificationManager.notify(NOTIFICATION_ID_RESULT + tournamentId.hashCode(), notification)
    }

    private fun isAppropriateTimeForNotification(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in 8..22 // Only show notifications between 8 AM and 10 PM
    }

    // In NotificationHelper.kt
    fun showBasicNotification(title: String, message: String, notificationId: Int) {
        try {
            Log.d(TAG, "Attempting to show notification: $title - $message")

            if (!hasNotificationPermission()) {
                Log.e(TAG, "No notification permission")
                return
            }

            val notification = buildBasicNotification(
                CHANNEL_TOURNAMENT_UPDATES,
                title,
                message
            ).apply {
                priority = NotificationCompat.PRIORITY_HIGH  // Make it more visible
            }.build()

            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Notification shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }


    fun showTournamentNotification(s: String, s1: String, i: Int) {

    }
}