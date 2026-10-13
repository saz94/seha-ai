package ai.seha.app.notify

import ai.seha.app.MainActivity
import ai.seha.app.R
import ai.seha.app.data.Appearance
import ai.seha.app.data.Store
import ai.seha.app.data.tr
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Once a day, in the evening, asks "how was your day?" if today has no check-in yet. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        Appearance.restoreLanguage(context)
        val data = Store.load(context)
        if (!data.profile.onboarded || !data.settings.reminderOn) return Result.success()
        if (data.checkIn(LocalDate.now().toString()) != null) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, tr("Daily check-in", "تسجيل اليوم"), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "checkin")
            action = "ai.seha.app.OPEN.checkin"
        }
        val pending = PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_seha)
            .setContentTitle(tr("How was your day?", "كيف كان يومك؟"))
            .setContentText(tr("Did you work out? It takes a minute to check in.", "هل تمرّنت؟ التسجيل يأخذ دقيقة."))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1, notification)
        } catch (e: SecurityException) {
            // notifications were turned off between the check and now
        }
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "checkin"
        private const val WORK = "daily-checkin"

        fun schedule(context: Context, on: Boolean, hour: Int) {
            val work = WorkManager.getInstance(context.applicationContext)
            if (!on) {
                work.cancelUniqueWork(WORK)
                return
            }
            val now = LocalDateTime.now()
            var next = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .build()
            work.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
