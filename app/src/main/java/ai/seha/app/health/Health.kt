package ai.seha.app.health

import ai.seha.app.data.HealthDay
import ai.seha.app.data.WeightEntry
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Health Connect is Android's shared health store: Google Fit, Fitbit, Samsung Health, watches and
 * scales all write into it, and this app reads from it (and writes back the weight you log here).
 */
object Health {
    const val AVAILABLE = 0
    const val NEEDS_INSTALL = 1
    const val UNSUPPORTED = 2

    private val STEPS = HealthPermission.getReadPermission(StepsRecord::class)
    private val SLEEP = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val WEIGHT = HealthPermission.getReadPermission(WeightRecord::class)
    private val WEIGHT_WRITE = HealthPermission.getWritePermission(WeightRecord::class)
    private val EXERCISE = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val ACTIVE = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val RESTING = HealthPermission.getReadPermission(RestingHeartRateRecord::class)

    val permissions: Set<String> = setOf(STEPS, SLEEP, WEIGHT, WEIGHT_WRITE, EXERCISE, ACTIVE, RESTING)

    fun status(context: Context): Int = try {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> NEEDS_INSTALL
            else -> UNSUPPORTED
        }
    } catch (e: Exception) {
        UNSUPPORTED
    }

    private fun client(context: Context) = HealthConnectClient.getOrCreate(context.applicationContext)

    suspend fun granted(context: Context): Set<String> = try {
        if (status(context) != AVAILABLE) emptySet() else client(context).permissionController.getGrantedPermissions()
    } catch (e: Exception) {
        emptySet()
    }

    /** The last [days] days, oldest first. Only reads what the user allowed. */
    suspend fun readDays(context: Context, days: Int): List<HealthDay> {
        val allowed = granted(context)
        if (allowed.isEmpty()) return emptyList()
        val client = client(context)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val out = mutableListOf<HealthDay>()
        for (back in (days - 1) downTo 0) {
            val date = today.minusDays(back.toLong())
            val start = date.atStartOfDay(zone).toInstant()
            val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant(), Instant.now())
            if (!end.isAfter(start)) continue
            var steps = 0L
            var active = 0
            var exercise = 0L
            var resting = 0
            var sleep = 0L
            try {
                val metrics = mutableSetOf<AggregateMetric<*>>()
                if (STEPS in allowed) metrics.add(StepsRecord.COUNT_TOTAL)
                if (ACTIVE in allowed) metrics.add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                if (EXERCISE in allowed) metrics.add(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)
                if (RESTING in allowed) metrics.add(RestingHeartRateRecord.BPM_AVG)
                if (metrics.isNotEmpty()) {
                    val r = client.aggregate(AggregateRequest(metrics = metrics, timeRangeFilter = TimeRangeFilter.between(start, end)))
                    if (STEPS in allowed) steps = r[StepsRecord.COUNT_TOTAL] ?: 0L
                    if (ACTIVE in allowed) active = r[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.roundToInt() ?: 0
                    if (EXERCISE in allowed) exercise = r[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes() ?: 0L
                    if (RESTING in allowed) resting = (r[RestingHeartRateRecord.BPM_AVG] ?: 0L).toInt()
                }
                if (SLEEP in allowed) {
                    // "Last night" for a date: sleep that ended between the previous noon and this noon.
                    val from = date.minusDays(1).atTime(12, 0).atZone(zone).toInstant()
                    val to = minOf(date.atTime(12, 0).atZone(zone).toInstant(), Instant.now())
                    if (to.isAfter(from)) {
                        val r = client.aggregate(
                            AggregateRequest(
                                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                                timeRangeFilter = TimeRangeFilter.between(from, to),
                            ),
                        )
                        sleep = r[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes() ?: 0L
                    }
                }
            } catch (e: Exception) {
                // one unreadable day should not lose the others
            }
            out.add(HealthDay(date.toString(), steps, sleep, active, exercise, resting))
        }
        return out
    }

    /** One weight per day (the latest reading of that day). */
    suspend fun readWeights(context: Context, days: Int): List<WeightEntry> {
        if (WEIGHT !in granted(context)) return emptyList()
        return try {
            val zone = ZoneId.systemDefault()
            val start = LocalDate.now().minusDays(days.toLong()).atStartOfDay(zone).toInstant()
            val response = client(context).readRecords(
                ReadRecordsRequest(recordType = WeightRecord::class, timeRangeFilter = TimeRangeFilter.between(start, Instant.now())),
            )
            response.records
                .sortedBy { it.time }
                .associate { it.time.atZone(zone).toLocalDate().toString() to it.weight.inKilograms }
                .map { WeightEntry(it.key, (it.value * 10).roundToInt() / 10.0, fromHealth = true) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun writeWeight(context: Context, kg: Double) {
        if (WEIGHT_WRITE !in granted(context)) return
        try {
            val now = Instant.now()
            client(context).insertRecords(
                listOf(
                    WeightRecord(
                        time = now,
                        zoneOffset = ZoneId.systemDefault().rules.getOffset(now),
                        weight = Mass.kilograms(kg),
                        metadata = Metadata.manualEntry(),
                    ),
                ),
            )
        } catch (e: Exception) {
            // logging the weight in the app must not fail because Health Connect refused it
        }
    }
}
