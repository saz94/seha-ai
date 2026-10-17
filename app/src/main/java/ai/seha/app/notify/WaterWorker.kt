package ai.seha.app.notify

import ai.seha.app.MainActivity
import ai.seha.app.R
import ai.seha.app.data.Appearance
import ai.seha.app.data.Store
import ai.seha.app.data.tr
import ai.seha.app.domain.Calc
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
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** A nudge to drink, only during the day and only when you are behind where you should be by now. */
class WaterWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        Appearance.restoreLanguage(context)
        val data = Store.load(context)
        val s = data.settings
        if (!data.profile.onboarded || !s.waterReminder) return Result.success()
        val hour = LocalTime.now().hour
        if (hour < s.waterStartHour || hour >= s.waterEndHour) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val today = LocalDate.now().toString()
        val workout = data.checkIn(today)?.takeIf { it.workedOut }?.workoutMinutes ?: 0
        val target = Calc.waterTarget(data.profile, data.latestWeight, workout, s.hotClimate)
        val had = data.water[today] ?: 0
        // where you should be by this hour, if you drank evenly through the day
        val dayLength = (s.waterEndHour - s.waterStartHour).coerceAtLeast(1)
        val expected = target * (hour - s.waterStartHour + 1) / dayLength
        if (had >= expected || had >= target) return Result.success()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, tr("Water reminders", "تذكير شرب الماء"), NotificationManager.IMPORTANCE_LOW),
        )
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "water")
            action = "ai.seha.app.OPEN.water"
        }
        val pending = PendingIntent.getActivity(context, 2, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val left = target - had
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_seha)
            .setContentTitle(tr("Time for a glass of water", "حان وقت كوب ماء"))
            .setContentText(tr("${had / 1000.0} L so far, ${left / 1000.0} L to go today.", "شربت ${had / 1000.0} ل حتى الآن، بقي ${left / 1000.0} ل اليوم."))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2, notification)
        } catch (e: SecurityException) {
        }
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "water"
        private const val WORK = "water-reminder"

        fun schedule(context: Context, on: Boolean, everyHours: Int) {
            val work = WorkManager.getInstance(context.applicationContext)
            if (!on) {
                work.cancelUniqueWork(WORK)
                return
            }
            val request = PeriodicWorkRequestBuilder<WaterWorker>(everyHours.coerceIn(1, 6).toLong(), TimeUnit.HOURS).build()
            work.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
