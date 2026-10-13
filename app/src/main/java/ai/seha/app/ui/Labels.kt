package ai.seha.app.ui

import ai.seha.app.data.ActivityLevel
import ai.seha.app.data.Diet
import ai.seha.app.data.Goal
import ai.seha.app.data.Provider
import ai.seha.app.data.tr

fun goalLabel(g: Goal): String = when (g) {
    Goal.LOSE -> tr("Lose fat", "خسارة الدهون")
    Goal.GAIN -> tr("Build muscle", "بناء العضلات")
    Goal.RECOMP -> tr("Leaner and stronger", "أنحف وأقوى")
    Goal.MAINTAIN -> tr("Stay healthy", "المحافظة على الصحة")
}

fun goalHint(g: Goal): String = when (g) {
    Goal.LOSE -> tr("Eat a little under what you burn, keep protein high.", "تناول أقل قليلاً مما تحرق مع بروتين مرتفع.")
    Goal.GAIN -> tr("Eat a little over what you burn and train with weights.", "تناول أكثر قليلاً مما تحرق وتمرّن بالأوزان.")
    Goal.RECOMP -> tr("Same weight, less fat, more muscle. Slow but steady.", "الوزن نفسه مع دهون أقل وعضلات أكثر. بطيء لكنه ثابت.")
    Goal.MAINTAIN -> tr("Keep your weight and build good habits.", "حافظ على وزنك وابنِ عادات جيدة.")
}

fun activityLabel(a: ActivityLevel): String = when (a) {
    ActivityLevel.SEDENTARY -> tr("Mostly sitting", "جلوس معظم اليوم")
    ActivityLevel.LIGHT -> tr("Lightly active", "نشاط خفيف")
    ActivityLevel.MODERATE -> tr("Active", "نشيط")
    ActivityLevel.ACTIVE -> tr("Very active", "نشيط جداً")
    ActivityLevel.ATHLETE -> tr("Athlete", "رياضي")
}

fun dietLabel(d: Diet): String = when (d) {
    Diet.HALAL -> tr("Halal", "حلال")
    Diet.ANY -> tr("No restriction", "بلا قيود")
    Diet.VEGETARIAN -> tr("Vegetarian", "نباتي")
    Diet.VEGAN -> tr("Vegan", "نباتي صرف")
    Diet.PESCATARIAN -> tr("Fish, no meat", "سمك بلا لحوم")
}

fun providerLabel(p: Provider): String = when (p) {
    Provider.GEMINI -> "Gemini"
    Provider.OPENAI -> "GPT"
    Provider.CLAUDE -> "Claude"
}

/** The id is what gets stored and sent to the AI; the label is what the user sees. */
fun conditionOptions(): List<Pair<String, String>> = listOf(
    "diabetes" to tr("Diabetes", "السكري"),
    "high blood pressure" to tr("High blood pressure", "ضغط الدم المرتفع"),
    "high cholesterol" to tr("High cholesterol", "ارتفاع الكوليسترول"),
    "heart condition" to tr("Heart condition", "مرض في القلب"),
    "kidney" to tr("Kidney condition", "مرض في الكلى"),
    "thyroid" to tr("Thyroid", "الغدة الدرقية"),
    "digestive (IBS, reflux)" to tr("Digestion (IBS, reflux)", "الهضم (قولون، ارتجاع)"),
    "asthma" to tr("Asthma", "الربو"),
    "pregnant or breastfeeding" to tr("Pregnant or breastfeeding", "حمل أو رضاعة"),
)

fun mealTypeLabel(type: String): String = ai.seha.app.widget.mealTypeName(type)
