package ai.seha.app.domain

import ai.seha.app.data.ActivityLevel
import ai.seha.app.data.AppData
import ai.seha.app.data.Goal
import ai.seha.app.data.Profile
import ai.seha.app.data.Sex
import ai.seha.app.data.tr
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class Targets(
    val bmr: Int,
    val maintenance: Int,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val waterMl: Int,
    /** True when the calorie floor stopped the plan from going lower. */
    val floored: Boolean,
    /** True when protein was held down because of a kidney condition. */
    val proteinLimited: Boolean,
)

object Calc {
    fun age(p: Profile, today: LocalDate = LocalDate.now()): Int = (today.year - p.birthYear).coerceIn(10, 100)

    fun bmi(weightKg: Double, heightCm: Double): Double {
        val m = heightCm / 100.0
        return if (m <= 0) 0.0 else weightKg / (m * m)
    }

    fun bmiLabel(bmi: Double): String = when {
        bmi <= 0 -> ""
        bmi < 18.5 -> tr("Underweight", "أقل من الطبيعي")
        bmi < 25 -> tr("Healthy range", "وزن صحي")
        bmi < 30 -> tr("Overweight", "وزن زائد")
        else -> tr("Obese range", "سمنة")
    }

    private fun activityFactor(a: ActivityLevel): Double = when (a) {
        ActivityLevel.SEDENTARY -> 1.2
        ActivityLevel.LIGHT -> 1.375
        ActivityLevel.MODERATE -> 1.55
        ActivityLevel.ACTIVE -> 1.725
        ActivityLevel.ATHLETE -> 1.9
    }

    fun hasKidneyCondition(p: Profile): Boolean =
        p.conditions.any { it == "kidney" } || p.medicalNotes.contains("kidney", true) || p.medicalNotes.contains("كلى")

    /** Mifflin-St Jeor resting energy, an activity multiplier, then the goal. */
    fun targets(p: Profile, weightKg: Double): Targets {
        val age = age(p)
        val bmr = 10 * weightKg + 6.25 * p.heightCm - 5 * age + if (p.sex == Sex.MALE) 5 else -161
        val maintenance = bmr * activityFactor(p.activity)
        val pace = p.paceKgPerWeek.coerceIn(0.1, 1.0)
        val change = when (p.goal) {
            // a kilogram of body fat is roughly 7700 kcal; never cut more than a quarter of maintenance
            Goal.LOSE -> -minOf(pace * 7700 / 7, maintenance * 0.25)
            Goal.GAIN -> minOf(pace * 7700 / 7, 500.0).coerceAtLeast(200.0)
            Goal.RECOMP -> -maintenance * 0.08
            Goal.MAINTAIN -> 0.0
        }
        val floor = if (p.sex == Sex.MALE) 1500.0 else 1200.0
        val raw = maintenance + change
        val floored = raw < floor
        val calories = (maxOf(raw, floor) / 10).roundToInt() * 10

        // Protein is set per kilogram; for a high BMI it is based on the target weight instead.
        val heavy = bmi(weightKg, p.heightCm) >= 30
        val base = if (heavy && p.targetWeightKg > 0) minOf(weightKg, p.targetWeightKg + 5) else weightKg
        val kidney = hasKidneyCondition(p)
        val perKg = when {
            kidney -> 0.8
            p.goal == Goal.RECOMP -> 2.0
            p.goal == Goal.LOSE -> 1.8
            p.goal == Goal.GAIN -> 1.8
            else -> 1.4
        }
        val protein = (base * perKg).roundToInt()
        val fat = (calories * 0.27 / 9).roundToInt()
        val carbs = ((calories - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(50)
        val water = waterTarget(p, weightKg, 0, true)
        return Targets(bmr.roundToInt(), maintenance.roundToInt(), calories, protein, carbs, fat, water.coerceIn(1500, 4500), floored, kidney)
    }

    /**
     * Daily water from the body: 35 ml per kg, more for a heavy day of activity, more in a hot
     * climate, and extra for every workout minute logged today. Rounded to a quarter litre.
     */
    fun waterTarget(p: Profile, weightKg: Double, workoutMinutes: Int, hotClimate: Boolean): Int {
        var ml = weightKg * 35
        ml *= when (p.activity) {
            ActivityLevel.SEDENTARY -> 0.95
            ActivityLevel.LIGHT -> 1.0
            ActivityLevel.MODERATE -> 1.08
            ActivityLevel.ACTIVE -> 1.15
            ActivityLevel.ATHLETE -> 1.25
        }
        if (hotClimate) ml += 400
        ml += minOf(workoutMinutes, 90) * 10
        return ((ml / 250).roundToInt() * 250).coerceIn(1500, 5000)
    }

    /** Weeks left to the target weight at the chosen pace, or null when there is no weight goal. */
    fun weeksToGoal(p: Profile, weightKg: Double): Int? {
        if (p.goal != Goal.LOSE && p.goal != Goal.GAIN) return null
        val left = abs(weightKg - p.targetWeightKg)
        if (left < 0.3) return 0
        return (left / p.paceKgPerWeek.coerceIn(0.1, 1.0)).roundToInt().coerceAtLeast(1)
    }

    /** 0..1: how far the weight has moved from the start towards the target. */
    fun goalProgress(p: Profile, weightKg: Double): Float {
        val total = p.targetWeightKg - p.startWeightKg
        if (abs(total) < 0.1) return 1f
        return ((weightKg - p.startWeightKg) / total).toFloat().coerceIn(0f, 1f)
    }

    /** Consecutive days with a check-in, ending today or yesterday. */
    fun streak(data: AppData, today: LocalDate = LocalDate.now()): Int {
        val days = data.checkins.map { it.date }.toSet()
        var day = if (days.contains(today.toString())) today else today.minusDays(1)
        var n = 0
        while (days.contains(day.toString())) {
            n++
            day = day.minusDays(1)
        }
        return n
    }

    fun workoutsThisWeek(data: AppData, today: LocalDate = LocalDate.now()): Int {
        val from = today.minusDays(6).toString()
        return data.checkins.count { it.date >= from && it.workedOut }
    }
}

object Dates {
    fun today(): String = LocalDate.now().toString()

    private fun locale(): Locale = if (ai.seha.app.data.Lang.ar) Locale("ar") else Locale.ENGLISH

    /** "Mon 12 Oct" */
    fun short(date: String): String = try {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE d MMM", locale()))
    } catch (e: Exception) {
        date
    }

    /** "12 Oct" */
    fun dayMonth(date: String): String = try {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM", locale()))
    } catch (e: Exception) {
        date
    }

    fun weekday(date: String): String = try {
        LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.FULL, locale())
    } catch (e: Exception) {
        date
    }

    fun long(date: String): String = try {
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale()))
    } catch (e: Exception) {
        date
    }
}

/** 72.5 -> "72.5", 72.0 -> "72" */
fun Double.trim1(): String {
    val r = (this * 10).roundToInt() / 10.0
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}
