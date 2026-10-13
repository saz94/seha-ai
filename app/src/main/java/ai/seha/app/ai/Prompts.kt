package ai.seha.app.ai

import ai.seha.app.data.ActivityLevel
import ai.seha.app.data.AppData
import ai.seha.app.data.CheckIn
import ai.seha.app.data.Diet
import ai.seha.app.data.Goal
import ai.seha.app.data.Lang
import ai.seha.app.data.Meal
import ai.seha.app.data.Sex
import ai.seha.app.data.tr
import ai.seha.app.domain.Calc
import ai.seha.app.domain.Targets
import ai.seha.app.domain.trim1
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

object Prompts {

    private fun language(): String = if (Lang.ar) "Arabic (Modern Standard, simple and friendly)" else "English"

    private fun goalText(g: Goal) = when (g) {
        Goal.LOSE -> "lose body fat"
        Goal.GAIN -> "build muscle and gain weight"
        Goal.RECOMP -> "lose fat while keeping or building muscle (recomposition)"
        Goal.MAINTAIN -> "stay at the current weight and keep healthy"
    }

    private fun activityText(a: ActivityLevel) = when (a) {
        ActivityLevel.SEDENTARY -> "mostly sitting"
        ActivityLevel.LIGHT -> "lightly active"
        ActivityLevel.MODERATE -> "moderately active"
        ActivityLevel.ACTIVE -> "very active"
        ActivityLevel.ATHLETE -> "athlete-level activity"
    }

    private fun dietText(d: Diet) = when (d) {
        Diet.ANY -> "no restriction"
        Diet.HALAL -> "halal only (no pork, no alcohol in cooking)"
        Diet.VEGETARIAN -> "vegetarian"
        Diet.VEGAN -> "vegan"
        Diet.PESCATARIAN -> "pescatarian"
    }

    /** What the AI is told about the person, in plain text. */
    fun about(data: AppData, t: Targets): String {
        val p = data.profile
        val weight = data.latestWeight
        val sb = StringBuilder()
        sb.appendLine("PERSON")
        if (p.name.isNotBlank()) sb.appendLine("Name: ${p.name}")
        sb.appendLine("Sex: ${if (p.sex == Sex.MALE) "male" else "female"}, age ${Calc.age(p)}, height ${p.heightCm.trim1()} cm")
        sb.appendLine("Weight now: ${weight.trim1()} kg (started at ${p.startWeightKg.trim1()} kg, target ${p.targetWeightKg.trim1()} kg)")
        sb.appendLine("BMI: ${Calc.bmi(weight, p.heightCm).trim1()}")
        sb.appendLine("Goal: ${goalText(p.goal)}; daily life: ${activityText(p.activity)}; plans ${p.workoutsPerWeek} workouts a week")
        sb.appendLine("Daily targets set by the app: ${t.calories} kcal, protein ${t.protein} g, carbs ${t.carbs} g, fat ${t.fat} g, water ${t.waterMl} ml")
        sb.appendLine("Diet: ${dietText(p.diet)}")
        if (p.dislikes.isNotBlank()) sb.appendLine("Never use these foods: ${p.dislikes}")
        if (p.cuisines.isNotBlank()) sb.appendLine("Likes these cuisines: ${p.cuisines}")
        sb.appendLine()
        sb.appendLine("MEDICAL HISTORY (as the person wrote it)")
        sb.appendLine("Conditions: ${if (p.conditions.isEmpty()) "none stated" else p.conditions.joinToString(", ")}")
        sb.appendLine("Food allergies or intolerances: ${p.allergies.ifBlank { "none stated" }}")
        sb.appendLine("Medications: ${p.medications.ifBlank { "none stated" }}")
        sb.appendLine("Injuries or pain: ${p.injuries.ifBlank { "none stated" }}")
        if (p.medicalNotes.isNotBlank()) sb.appendLine("Other notes: ${p.medicalNotes}")
        return sb.toString()
    }

    private fun recent(data: AppData): String {
        val sb = StringBuilder()
        val from = LocalDate.now().minusDays(7).toString()
        val checkins = data.checkins.filter { it.date >= from }.sortedBy { it.date }
        sb.appendLine("LAST 7 DAYS")
        if (checkins.isEmpty()) sb.appendLine("No check-ins yet.")
        for (c in checkins) sb.appendLine(line(c))
        val weights = data.weights.sortedBy { it.date }.takeLast(8)
        if (weights.isNotEmpty()) sb.appendLine("Weights: " + weights.joinToString(", ") { "${it.date}: ${it.kg.trim1()} kg" })
        val health = data.health.filter { it.date >= from }.sortedBy { it.date }
        if (health.isNotEmpty()) {
            sb.appendLine("From Health Connect: " + health.joinToString("; ") {
                "${it.date}: ${it.steps} steps, sleep ${it.sleepMinutes / 60}h${it.sleepMinutes % 60}m, ${it.exerciseMinutes} min exercise" +
                    if (it.restingHr > 0) ", resting HR ${it.restingHr}" else ""
            })
        }
        val m = data.measurements.maxByOrNull { it.date }
        if (m != null) {
            sb.appendLine(
                "Latest measurements (${m.date}): " + listOfNotNull(
                    m.waist?.let { "waist ${it.trim1()} cm" }, m.chest?.let { "chest ${it.trim1()} cm" },
                    m.arm?.let { "arm ${it.trim1()} cm" }, m.hips?.let { "hips ${it.trim1()} cm" },
                    m.thigh?.let { "thigh ${it.trim1()} cm" }, m.bodyFat?.let { "body fat ${it.trim1()}%" },
                ).joinToString(", "),
            )
        }
        val plan = data.plan(LocalDate.now().toString())
        if (plan != null && plan.meals.isNotEmpty()) {
            sb.appendLine("Today's meal plan: " + plan.meals.joinToString("; ") { "${it.type}: ${it.name} (${it.calories} kcal)${if (it.eaten) " eaten" else ""}" })
        }
        return sb.toString()
    }

    private fun line(c: CheckIn): String =
        "${c.date}: mood ${c.mood}/5, energy ${c.energy}/5, sleep ${c.sleepHours.trim1()} h, " +
            (if (c.workedOut) "worked out (${c.workoutType.ifBlank { "unspecified" }}, ${c.workoutMinutes} min)" else "no workout") +
            ", ate on plan: ${listOf("no", "partly", "yes")[c.ateOnPlan.coerceIn(0, 2)]}" +
            if (c.notes.isNotBlank()) ", note: ${c.notes}" else ""

    private const val SAFETY =
        "You are not a doctor. Respect the medical history: never suggest foods the person is allergic to, and adapt " +
            "advice to their conditions, medications and injuries. For anything that needs a medical decision " +
            "(symptoms, medication changes, very low calorie diets, pain that persists), say clearly that they should ask their doctor. " +
            "Never recommend eating below the app's calorie target, extreme fasting, or unsafe supplements."

    fun coachSystem(data: AppData, t: Targets): String =
        "You are the coach inside Seha AI, a personal health and physique app used by one person. " +
            "Be warm, direct and practical, like a good trainer who also knows nutrition. Keep answers short: a few sentences, " +
            "or a short list when steps help. Use the person's own numbers. Write plain text only: no markdown, no asterisks, no headings. Today is ${LocalDate.now()}.\n" +
            "Always answer in ${language()}.\n$SAFETY\n\n" + about(data, t) + "\n" + recent(data)

    fun checkInFeedback(c: CheckIn): String =
        "I just finished today's check-in: ${line(c)}\n" +
            "Give me feedback in 2 to 4 sentences: what went well, and the one thing to focus on tomorrow. No headings."

    fun dailyTip(): String =
        "Give me one short, specific tip for today (2 sentences at most) based on my last few days and my goal. No greeting, no headings."

    // ------------------------------------------------------------------ meal plans

    fun mealSystem(data: AppData, t: Targets, web: Boolean): String =
        "You plan meals inside Seha AI, a personal health app. You write clear home-cooking recipes that hit calorie and macro targets.\n" +
            "Write every name, ingredient and step in ${language()}.\n$SAFETY\n" +
            (if (web) "Search reputable cooking and nutrition websites for real recipes that fit, adapt the portions to the targets, " +
                "and put the page you based each recipe on in \"source\" (full URL) and \"sourceName\" (site name). Write the steps in your own words.\n"
            else "Leave \"source\" and \"sourceName\" empty.\n") +
            "Answer with ONE JSON object and nothing else: no markdown, no code fence, no commentary.\n\n" + about(data, t)

    fun mealRequest(date: String, weekday: String, t: Targets, mealsPerDay: Int, wanted: String, avoid: String, alreadyPlanned: List<String>): String {
        val types = when {
            mealsPerDay <= 2 -> "lunch and dinner"
            mealsPerDay == 3 -> "breakfast, lunch and dinner"
            mealsPerDay == 4 -> "breakfast, lunch, dinner and one snack"
            else -> "breakfast, lunch, dinner and two snacks"
        }
        return "Plan the meals for $weekday $date: $types.\n" +
            "The day must total about ${t.calories} kcal (within 5%), with about ${t.protein} g protein, ${t.carbs} g carbs and ${t.fat} g fat.\n" +
            (if (wanted.isNotBlank()) "I want to eat: $wanted. Build meals around these; if I only name a food (for example \"chicken\"), choose a good dish with it.\n" else "") +
            (if (avoid.isNotBlank()) "Do not use these at all, not even as a garnish: $avoid.\n" else "") +
            (if (alreadyPlanned.isNotEmpty()) "Already planned on other days, so choose different dishes: ${alreadyPlanned.joinToString(", ")}.\n" else "") +
            "Ingredients must have exact amounts in grams, millilitres or pieces for ONE serving. Steps must be complete enough for a beginner.\n" +
            "JSON shape:\n" +
            "{\"meals\":[{\"type\":\"breakfast|lunch|dinner|snack\",\"name\":\"\",\"calories\":0,\"protein\":0,\"carbs\":0,\"fat\":0,\"minutes\":0," +
            "\"ingredients\":[\"\"],\"steps\":[\"\"],\"source\":\"\",\"sourceName\":\"\"}]}"
    }

    fun swapRequest(old: Meal, wanted: String, avoid: String): String =
        "Replace this ${old.type}: \"${old.name}\" with a different dish of about ${old.calories} kcal and ${old.protein} g protein.\n" +
            (if (wanted.isNotBlank()) "I would like: $wanted.\n" else "") +
            (if (avoid.isNotBlank()) "Do not use these at all: $avoid.\n" else "") +
            "Ingredients with exact amounts for ONE serving, complete steps.\n" +
            "JSON shape:\n" +
            "{\"meals\":[{\"type\":\"${old.type}\",\"name\":\"\",\"calories\":0,\"protein\":0,\"carbs\":0,\"fat\":0,\"minutes\":0," +
            "\"ingredients\":[\"\"],\"steps\":[\"\"],\"source\":\"\",\"sourceName\":\"\"}]}"

    private fun strings(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until a.length()) {
            val v = a.optString(i, "").trim()
            if (v.isNotEmpty()) out.add(v)
        }
        return out
    }

    /** Pulls the JSON object out of the AI's answer, tolerating code fences and stray text around it. */
    fun parseMeals(answer: String): List<Meal> {
        val start = answer.indexOf('{')
        val end = answer.lastIndexOf('}')
        if (start < 0 || end <= start) return emptyList()
        return try {
            val array = JSONObject(answer.substring(start, end + 1)).optJSONArray("meals") ?: return emptyList()
            val out = mutableListOf<Meal>()
            for (i in 0 until array.length()) {
                val m = array.optJSONObject(i) ?: continue
                val name = m.optString("name", "").trim()
                if (name.isEmpty()) continue
                val type = m.optString("type", "snack").lowercase().trim()
                val source = m.optString("source", "").trim()
                out.add(
                    Meal(
                        id = UUID.randomUUID().toString(),
                        type = if (type in setOf("breakfast", "lunch", "dinner", "snack")) type else "snack",
                        name = name,
                        calories = m.optInt("calories", 0),
                        protein = m.optInt("protein", 0),
                        carbs = m.optInt("carbs", 0),
                        fat = m.optInt("fat", 0),
                        minutes = m.optInt("minutes", 0),
                        ingredients = strings(m.optJSONArray("ingredients")),
                        steps = strings(m.optJSONArray("steps")),
                        source = if (source.startsWith("http")) source else "",
                        sourceName = m.optString("sourceName", "").trim(),
                    ),
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** What the coach says when no AI key has been added: simple rules on today's check-in. */
    fun offlineFeedback(c: CheckIn, t: Targets): String {
        val parts = mutableListOf<String>()
        if (c.workedOut) parts.add(tr("Workout done. That is the habit that changes your physique.", "أنجزت تمرينك. هذه العادة هي ما يغيّر جسمك."))
        else parts.add(tr("No workout today. A 20 minute walk tomorrow still counts.", "لا تمرين اليوم. مشي 20 دقيقة غداً يُحسب أيضاً."))
        if (c.sleepHours < 6.5) parts.add(tr("You slept under 6.5 hours; short sleep raises hunger, so aim for an earlier night.", "نمت أقل من 6.5 ساعة؛ قلة النوم تزيد الجوع، فحاول النوم أبكر."))
        if (c.ateOnPlan == 0) parts.add(tr("Food went off plan. Don't make up for it by skipping meals; just return to ${t.calories} kcal tomorrow.", "خرج الأكل عن الخطة. لا تعوّض بتفويت الوجبات؛ عد غداً إلى ${t.calories} سعرة."))
        if (c.ateOnPlan == 2) parts.add(tr("You ate on plan. Keep protein near ${t.protein} g.", "التزمت بخطة الأكل. حافظ على البروتين قرب ${t.protein} غ."))
        if (c.energy <= 2) parts.add(tr("Energy was low: check water and sleep first.", "طاقتك منخفضة: راجع الماء والنوم أولاً."))
        return parts.joinToString(" ")
    }
}
