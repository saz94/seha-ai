package ai.seha.app.data

import kotlinx.serialization.Serializable

/** The app's two languages. Set once per activity start; see MainActivity. */
object Lang {
    @Volatile
    var ar: Boolean = false
}

/** Picks the English or the Arabic wording. */
fun tr(en: String, ar: String): String = if (Lang.ar) ar else en

enum class Sex { MALE, FEMALE }
enum class Goal { LOSE, GAIN, RECOMP, MAINTAIN }
enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, ATHLETE }
enum class Diet { ANY, HALAL, VEGETARIAN, VEGAN, PESCATARIAN }
enum class Provider { GEMINI, OPENAI, CLAUDE }

@Serializable
data class Profile(
    val onboarded: Boolean = false,
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val birthYear: Int = 1995,
    val heightCm: Double = 172.0,
    val startWeightKg: Double = 75.0,
    val targetWeightKg: Double = 72.0,
    val goal: Goal = Goal.LOSE,
    val activity: ActivityLevel = ActivityLevel.LIGHT,
    val workoutsPerWeek: Int = 3,
    /** Kilograms per week the user wants to lose or gain. */
    val paceKgPerWeek: Double = 0.5,
    // ---- medical history, in the user's own words
    val conditions: List<String> = emptyList(),
    val medications: String = "",
    val injuries: String = "",
    val allergies: String = "",
    val medicalNotes: String = "",
    // ---- food
    val diet: Diet = Diet.HALAL,
    val dislikes: String = "",
    val mealsPerDay: Int = 3,
    val cuisines: String = "",
)

@Serializable
data class CheckIn(
    val date: String,
    val mood: Int = 3,
    val energy: Int = 3,
    val sleepHours: Double = 7.0,
    val workedOut: Boolean = false,
    val workoutType: String = "",
    val workoutMinutes: Int = 0,
    /** 0 = no, 1 = partly, 2 = yes. */
    val ateOnPlan: Int = 1,
    val notes: String = "",
    val coachNote: String = "",
)

@Serializable
data class WeightEntry(val date: String, val kg: Double, val fromHealth: Boolean = false)

@Serializable
data class Measurement(
    val date: String,
    val waist: Double? = null,
    val chest: Double? = null,
    val arm: Double? = null,
    val hips: Double? = null,
    val thigh: Double? = null,
    val bodyFat: Double? = null,
)

@Serializable
data class Photo(val date: String, val file: String)

@Serializable
data class Meal(
    val id: String = "",
    /** breakfast, lunch, dinner or snack. */
    val type: String = "lunch",
    val name: String = "",
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val minutes: Int = 0,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val source: String = "",
    val sourceName: String = "",
    val eaten: Boolean = false,
)

@Serializable
data class DayPlan(val date: String, val meals: List<Meal> = emptyList())

@Serializable
data class ChatMsg(val fromUser: Boolean, val text: String, val time: Long = 0, val sources: List<String> = emptyList())

/** One day as read from Health Connect. */
@Serializable
data class HealthDay(
    val date: String,
    val steps: Long = 0,
    val sleepMinutes: Long = 0,
    val activeKcal: Int = 0,
    val exerciseMinutes: Long = 0,
    val restingHr: Int = 0,
)

@Serializable
data class Settings(
    /** "system", "light" or "dark". */
    val themeMode: String = "system",
    /** oasis, dusk, dune or rose. */
    val palette: String = "oasis",
    val provider: Provider = Provider.GEMINI,
    val modelGemini: String = "gemini-3.8-flash",
    val modelOpenAi: String = "gpt-5.5",
    val modelClaude: String = "claude-sonnet-5-5",
    /** Let the AI search cooking and health websites. */
    val webSearch: Boolean = true,
    val reminderOn: Boolean = true,
    val reminderHour: Int = 21,
    val healthConnect: Boolean = false,
    /** What the user last asked the meal planner for. */
    val lastWanted: String = "",
    val lastAvoid: String = "",
) {
    fun model(p: Provider = provider): String = when (p) {
        Provider.GEMINI -> modelGemini
        Provider.OPENAI -> modelOpenAi
        Provider.CLAUDE -> modelClaude
    }
}

@Serializable
data class AppData(
    val profile: Profile = Profile(),
    val settings: Settings = Settings(),
    val checkins: List<CheckIn> = emptyList(),
    val weights: List<WeightEntry> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val photos: List<Photo> = emptyList(),
    val plans: List<DayPlan> = emptyList(),
    val chat: List<ChatMsg> = emptyList(),
    /** Millilitres of water per day. */
    val water: Map<String, Int> = emptyMap(),
    val health: List<HealthDay> = emptyList(),
    /** A short coach message for the Today screen, and the day it was written for. */
    val tip: String = "",
    val tipDate: String = "",
) {
    fun checkIn(date: String) = checkins.firstOrNull { it.date == date }
    fun plan(date: String) = plans.firstOrNull { it.date == date }
    fun healthOn(date: String) = health.firstOrNull { it.date == date }
    val latestWeight: Double get() = weights.maxByOrNull { it.date }?.kg ?: profile.startWeightKg
}
