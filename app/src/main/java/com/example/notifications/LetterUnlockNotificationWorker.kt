package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.util.AppLanguage
import com.example.util.DateAndPhotoUtils
import com.example.util.LanguagePreferences
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val VENT_LETTERS_CHANNEL_ID = "vent_letters_channel"
private const val KEY_LETTER_ID = "letter_id"
private const val KEY_WRITTEN_DATE_ISO = "written_date_iso"

class LetterUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val letterId = intent.getStringExtra(KEY_LETTER_ID) ?: "future_letter"
        val writtenDateIso = intent.getStringExtra(KEY_WRITTEN_DATE_ISO).orEmpty()
        LetterUnlockNotificationHelper.showNotification(context, letterId, writtenDateIso)
    }
}

class LetterUnlockWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val letterId = inputData.getString(KEY_LETTER_ID) ?: "future_letter"
        val writtenDateIso = inputData.getString(KEY_WRITTEN_DATE_ISO).orEmpty()
        LetterUnlockNotificationHelper.showNotification(applicationContext, letterId, writtenDateIso)
        return Result.success()
    }
}

object LetterUnlockNotificationHelper {

    fun showNotification(context: Context, letterId: String, writtenDateIso: String) {
        LetterUnlockScheduler.createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                return
            }
        }

        val lang = LanguagePreferences(context).getResolvedLanguage()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_tab", "letters")
            putExtra("unlocked_letter_id", letterId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            letterId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedDate = if (writtenDateIso.isNotBlank()) {
            DateAndPhotoUtils.formatShortDate(writtenDateIso, lang)
        } else {
            if (lang == AppLanguage.RU) "прошлого" else "the past"
        }

        val notifTitle = if (lang == AppLanguage.RU) {
            "Письмо из прошлого открыто!"
        } else {
            "Time Capsule Unlocked!"
        }

        val notifBody = if (lang == AppLanguage.RU) {
            "Ваше письмо себе от $formattedDate теперь доступно для чтения в Vent."
        } else {
            "Your letter from $formattedDate is now unlocked and ready to read in Vent."
        }

        // Authentic clean Android notification:
        // - Small icon on the left & in status bar: Vent's monochrome botanical leaf logo (R.drawable.ic_notification_vent)
        // - App name "Vent" is already shown in Android subheader, so content title is clean
        // - No oversized redundant largeIcon on the right pushing the text down
        val notification = NotificationCompat.Builder(context, VENT_LETTERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_vent)
            .setContentTitle(notifTitle)
            .setContentText(notifBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(letterId.hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }
}

object LetterUnlockScheduler {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val lang = LanguagePreferences(context).getResolvedLanguage()
            val name = if (lang == AppLanguage.RU) "Письма в будущее" else "Time Capsule Letters"
            val descriptionText = if (lang == AppLanguage.RU) {
                "Уведомления об открытии писем себе в будущее"
            } else {
                "Notifications when your sealed future letters unlock"
            }
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(VENT_LETTERS_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleUnlockNotification(
        context: Context,
        letterId: String,
        unlockDateIso: String,
        writtenDateIso: String,
        unlockHourOfDay: Int = 9,
        unlockMinute: Int = 0,
        replaceExisting: Boolean = true
    ) {
        val nowMillis = System.currentTimeMillis()
        val unlockCal = DateAndPhotoUtils.parseIsoToCalendar(unlockDateIso).apply {
            set(Calendar.HOUR_OF_DAY, unlockHourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, unlockMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val rawDiff = unlockCal.timeInMillis - nowMillis
        if (!replaceExisting && rawDiff <= 0L) {
            return
        }

        // 1. Exact Alarm via AlarmManager (fires at the EXACT minute without WorkManager battery batching)
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null && rawDiff > 0L) {
                val alarmIntent = Intent(context, LetterUnlockReceiver::class.java).apply {
                    action = "com.example.vent.ACTION_UNLOCK_LETTER_$letterId"
                    putExtra(KEY_LETTER_ID, letterId)
                    putExtra(KEY_WRITTEN_DATE_ISO, writtenDateIso)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    letterId.hashCode(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, unlockCal.timeInMillis, pendingIntent)
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, unlockCal.timeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, unlockCal.timeInMillis, pendingIntent)
                }
            }
        } catch (_: Exception) {
        }

        // 2. WorkManager as background fallback (reliable across reboots and power saving)
        val delayMillis = rawDiff.coerceAtLeast(3_000L)
        val data = workDataOf(
            KEY_LETTER_ID to letterId,
            KEY_WRITTEN_DATE_ISO to writtenDateIso
        )
        val workRequest = OneTimeWorkRequestBuilder<LetterUnlockWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("future_letter_unlock_$letterId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "unlock_letter_$letterId",
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelUnlockNotification(context: Context, letterId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                val alarmIntent = Intent(context, LetterUnlockReceiver::class.java).apply {
                    action = "com.example.vent.ACTION_UNLOCK_LETTER_$letterId"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    letterId.hashCode(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        } catch (_: Exception) {
        }
        WorkManager.getInstance(context).cancelUniqueWork("unlock_letter_$letterId")
    }
}
