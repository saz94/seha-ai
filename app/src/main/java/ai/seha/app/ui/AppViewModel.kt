package ai.seha.app.ui

import ai.seha.app.ai.AiClient
import ai.seha.app.ai.AiException
import ai.seha.app.ai.AiReply
import ai.seha.app.ai.Prompts
import ai.seha.app.ai.Turn
import ai.seha.app.data.AppData
import ai.seha.app.data.Appearance
import ai.seha.app.data.ChatMsg
import ai.seha.app.data.CheckIn
import ai.seha.app.data.DayPlan
import ai.seha.app.data.Demo
import ai.seha.app.data.Keys
import ai.seha.app.data.Meal
import ai.seha.app.data.Measurement
import ai.seha.app.data.Photo
import ai.seha.app.data.Profile
import ai.seha.app.data.Provider
import ai.seha.app.data.Settings
import ai.seha.app.data.Store
import ai.seha.app.data.WeightEntry
import ai.seha.app.data.tr
import ai.seha.app.domain.Calc
import ai.seha.app.domain.Dates
import ai.seha.app.domain.Targets
import ai.seha.app.health.Health
import ai.seha.app.notify.ReminderWorker
import ai.seha.app.widget.refreshWidgets
import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<Application>()
    val keys = Keys(app)

    var data by mutableStateOf(Store.load(app))
        private set

    /** Bumped when a key is saved, so screens that show "key added" redraw. */
    var keysVersion by mutableStateOf(0)
        private set

    /** A one-line message for the bar at the bottom of the screen. */
    var message by mutableStateOf<String?>(null)

    // ---- meal planner
    var planning by mutableStateOf<String?>(null)
        private set
    var planError by mutableStateOf<String?>(null)
    private var planJob: Job? = null

    // ---- coach
    var chatBusy by mutableStateOf(false)
        private set
    var chatError by mutableStateOf<String?>(null)
    var feedbackBusy by mutableStateOf(false)
        private set

    // ---- Health Connect
    var healthStatus by mutableStateOf(Health.status(app))
        private set
    var healthGranted by mutableStateOf(false)
        private set
    var healthBusy by mutableStateOf(false)
        private set

    private var demo = false

    val targets: Targets get() = Calc.targets(data.profile, data.latestWeight)
    val hasKey: Boolean get() = keysVersion >= 0 && keys.has(data.settings.provider)

    init {
        ReminderWorker.schedule(app, data.settings.reminderOn && data.profile.onboarded, data.settings.reminderHour)
    }

    private fun update(change: (AppData) -> AppData) {
        val next = change(data)
        data = next
        if (demo) return
        viewModelScope.launch(Dispatchers.IO) {
            Store.save(context, next)
            try {
                refreshWidgets(context)
            } catch (e: Exception) {
                // a widget that fails to redraw must not affect saving
            }
        }
    }

    /** Sample data, used only when photographing the screens. */
    fun loadDemo() {
        if (demo) return
        demo = true
        data = Demo.data()
    }

    // ------------------------------------------------------------------ profile and settings

    fun finishOnboarding(profile: Profile) {
        val today = Dates.today()
        update {
            it.copy(
                profile = profile.copy(onboarded = true),
                weights = (it.weights.filter { w -> w.date != today } + WeightEntry(today, profile.startWeightKg)).sortedBy { w -> w.date },
            )
        }
        ReminderWorker.schedule(context, data.settings.reminderOn, data.settings.reminderHour)
    }

    fun saveProfile(profile: Profile) = update { it.copy(profile = profile.copy(onboarded = true)) }

    fun saveSettings(settings: Settings) {
        val old = data.settings
        update { it.copy(settings = settings) }
        if (old.themeMode != settings.themeMode) Appearance.applyTheme(settings.themeMode)
        if (old.reminderOn != settings.reminderOn || old.reminderHour != settings.reminderHour) {
            ReminderWorker.schedule(context, settings.reminderOn, settings.reminderHour)
        }
    }

    fun saveKey(provider: Provider, key: String) {
        keys.set(provider, key)
        keysVersion++
    }

    fun exportText(): String = Store.export(data)

    fun importText(text: String): Boolean = try {
        val imported = Store.import(text)
        update { imported }
        Appearance.applyTheme(imported.settings.themeMode)
        true
    } catch (e: Exception) {
        false
    }

    fun eraseEverything() {
        File(context.filesDir, "photos").deleteRecursively()
        update { AppData(settings = it.settings) }
    }

    // ------------------------------------------------------------------ daily tracking

    fun logWeight(kg: Double, date: String = Dates.today()) {
        update { d -> d.copy(weights = (d.weights.filter { it.date != date } + WeightEntry(date, kg)).sortedBy { it.date }) }
        if (date == Dates.today() && data.settings.healthConnect && !demo) {
            viewModelScope.launch { Health.writeWeight(context, kg) }
        }
    }

    fun deleteWeight(date: String) = update { d -> d.copy(weights = d.weights.filter { it.date != date }) }

    fun addWater(ml: Int) {
        val today = Dates.today()
        update { d -> d.copy(water = d.water + (today to ((d.water[today] ?: 0) + ml).coerceAtLeast(0))) }
    }

    fun saveMeasurement(m: Measurement) =
        update { d -> d.copy(measurements = (d.measurements.filter { it.date != m.date } + m).sortedBy { it.date }) }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "photos").apply { mkdirs() }
                val file = File(dir, "p${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                withContext(Dispatchers.Main) {
                    update { d -> d.copy(photos = d.photos + Photo(Dates.today(), file.name)) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { message = tr("Could not save that photo.", "تعذّر حفظ الصورة.") }
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        File(File(context.filesDir, "photos"), photo.file).delete()
        update { d -> d.copy(photos = d.photos.filter { it.file != photo.file }) }
    }

    fun photoFile(photo: Photo): File = File(File(context.filesDir, "photos"), photo.file)

    fun saveCheckIn(checkIn: CheckIn) {
        update { d -> d.copy(checkins = (d.checkins.filter { it.date != checkIn.date } + checkIn).sortedBy { it.date }) }
        feedbackBusy = true
        viewModelScope.launch {
            val note = if (hasKey) {
                try {
                    ask(Prompts.coachSystem(data, targets), listOf(Turn(true, Prompts.checkInFeedback(checkIn))), web = false, maxTokens = 600).text
                } catch (e: AiException) {
                    Prompts.offlineFeedback(checkIn, targets)
                }
            } else {
                Prompts.offlineFeedback(checkIn, targets)
            }
            update { d -> d.copy(checkins = d.checkins.map { if (it.date == checkIn.date) it.copy(coachNote = note) else it }) }
            feedbackBusy = false
        }
    }

    // ------------------------------------------------------------------ AI

    private suspend fun ask(system: String, turns: List<Turn>, web: Boolean, maxTokens: Int): AiReply {
        val s = data.settings
        return AiClient.ask(s.provider, keys.get(s.provider), s.model(), system, turns, web && s.webSearch, maxTokens)
    }

    /** Checks a key by asking the service one tiny question. */
    fun testKey(provider: Provider, done: (String?) -> Unit) {
        viewModelScope.launch {
            val error = try {
                AiClient.ask(provider, keys.get(provider), data.settings.model(provider), "Answer with the single word: ok", listOf(Turn(true, "ok?")), web = false, maxTokens = 200)
                null
            } catch (e: AiException) {
                e.message ?: "error"
            }
            done(error)
        }
    }

    fun sendChat(text: String) {
        if (text.isBlank() || chatBusy) return
        chatError = null
        update { d -> d.copy(chat = d.chat + ChatMsg(true, text.trim(), System.currentTimeMillis())) }
        chatBusy = true
        viewModelScope.launch {
            try {
                val turns = data.chat.takeLast(16).map { Turn(it.fromUser, it.text) }
                val reply = ask(Prompts.coachSystem(data, targets), turns, web = true, maxTokens = 1500)
                update { d -> d.copy(chat = (d.chat + ChatMsg(false, reply.text, System.currentTimeMillis(), reply.sources)).takeLast(200)) }
            } catch (e: AiException) {
                chatError = e.message
            }
            chatBusy = false
        }
    }

    fun clearChat() = update { it.copy(chat = emptyList()) }

    /** A fresh one-line tip for the Today screen, at most once a day. */
    fun refreshTip() {
        val today = Dates.today()
        if (!hasKey || data.tipDate == today || demo) return
        update { it.copy(tipDate = today) }
        viewModelScope.launch {
            try {
                val reply = ask(Prompts.coachSystem(data, targets), listOf(Turn(true, Prompts.dailyTip())), web = false, maxTokens = 400)
                update { it.copy(tip = reply.text) }
            } catch (e: AiException) {
                // the Today screen simply keeps the built-in tip
            }
        }
    }

    // ------------------------------------------------------------------ meal plans

    fun planMeals(dates: List<String>, wanted: String, avoid: String) {
        if (planning != null || dates.isEmpty()) return
        planError = null
        update { it.copy(settings = it.settings.copy(lastWanted = wanted, lastAvoid = avoid)) }
        planning = tr("Starting…", "جارٍ البدء…")
        planJob = viewModelScope.launch {
            try {
                val t = targets
                val system = Prompts.mealSystem(data, t, data.settings.webSearch)
                val chosen = mutableListOf<String>()
                for ((index, date) in dates.withIndex()) {
                    planning = if (dates.size == 1) tr("Finding recipes and building your day…", "جارٍ البحث عن الوصفات وبناء يومك…")
                    else tr("Planning ${Dates.weekday(date)} (${index + 1} of ${dates.size})…", "جارٍ تخطيط ${Dates.weekday(date)} (${index + 1} من ${dates.size})…")
                    val request = Prompts.mealRequest(date, LocalDate.parse(date).dayOfWeek.name.lowercase(), t, data.profile.mealsPerDay, wanted, avoid, chosen)
                    var meals = Prompts.parseMeals(ask(system, listOf(Turn(true, request)), web = true, maxTokens = 6000).text)
                    if (meals.isEmpty()) {
                        // one more try without the web, which sometimes makes the answer wander off the format
                        meals = Prompts.parseMeals(ask(system, listOf(Turn(true, request)), web = false, maxTokens = 6000).text)
                    }
                    if (meals.isEmpty()) throw AiException(tr("The AI answered in a form the app could not read. Try again.", "أجاب الذكاء الاصطناعي بصيغة لم يستطع التطبيق قراءتها. حاول مرة أخرى."))
                    chosen.addAll(meals.map { it.name })
                    val order = listOf("breakfast", "lunch", "snack", "dinner")
                    val sorted = meals.sortedBy { order.indexOf(it.type).let { i -> if (i < 0) 9 else i } }
                    update { d -> d.copy(plans = (d.plans.filter { it.date != date } + DayPlan(date, sorted)).sortedBy { it.date }.takeLast(60)) }
                }
            } catch (e: AiException) {
                planError = e.message
            }
            planning = null
        }
    }

    fun cancelPlanning() {
        planJob?.cancel()
        planning = null
    }

    fun swapMeal(date: String, meal: Meal, wanted: String) {
        if (planning != null) return
        planError = null
        planning = tr("Finding a replacement…", "جارٍ البحث عن بديل…")
        planJob = viewModelScope.launch {
            try {
                val system = Prompts.mealSystem(data, targets, data.settings.webSearch)
                val answer = ask(system, listOf(Turn(true, Prompts.swapRequest(meal, wanted, data.settings.lastAvoid))), web = true, maxTokens = 3000)
                val replacement = Prompts.parseMeals(answer.text).firstOrNull()
                    ?: throw AiException(tr("The AI answered in a form the app could not read. Try again.", "أجاب الذكاء الاصطناعي بصيغة لم يستطع التطبيق قراءتها. حاول مرة أخرى."))
                update { d ->
                    d.copy(plans = d.plans.map { p ->
                        if (p.date != date) p else p.copy(meals = p.meals.map { if (it.id == meal.id) replacement.copy(id = meal.id, type = meal.type) else it })
                    })
                }
            } catch (e: AiException) {
                planError = e.message
            }
            planning = null
        }
    }

    fun toggleEaten(date: String, mealId: String) = update { d ->
        d.copy(plans = d.plans.map { p -> if (p.date != date) p else p.copy(meals = p.meals.map { if (it.id == mealId) it.copy(eaten = !it.eaten) else it }) })
    }

    fun deletePlan(date: String) = update { d -> d.copy(plans = d.plans.filter { it.date != date }) }

    // ------------------------------------------------------------------ Health Connect

    fun checkHealth() {
        healthStatus = Health.status(context)
        viewModelScope.launch {
            healthGranted = Health.granted(context).isNotEmpty()
            if (healthGranted && data.settings.healthConnect) syncHealth(quiet = true)
        }
    }

    fun onHealthPermissions(granted: Set<String>) {
        healthGranted = granted.isNotEmpty()
        update { it.copy(settings = it.settings.copy(healthConnect = healthGranted)) }
        if (healthGranted) syncHealth(quiet = false)
        else message = tr("Health Connect access was not given.", "لم يُمنح الوصول إلى Health Connect.")
    }

    fun disconnectHealth() = update { it.copy(settings = it.settings.copy(healthConnect = false)) }

    fun syncHealth(quiet: Boolean) {
        if (healthBusy || demo) return
        healthBusy = true
        viewModelScope.launch {
            try {
                val days = Health.readDays(context, 14)
                val weights = Health.readWeights(context, 60)
                update { d ->
                    val known = d.weights.filter { !it.fromHealth }.map { it.date }.toSet()
                    val merged = d.weights.filter { !it.fromHealth } + weights.filter { it.date !in known }
                    val oldDays = d.health.filter { old -> days.none { it.date == old.date } }
                    d.copy(health = (oldDays + days).sortedBy { it.date }.takeLast(90), weights = merged.sortedBy { it.date })
                }
                if (!quiet) message = tr("Health Connect is up to date.", "تم تحديث بيانات Health Connect.")
            } catch (e: Exception) {
                if (!quiet) message = tr("Could not read Health Connect.", "تعذّرت قراءة Health Connect.")
            }
            healthBusy = false
        }
    }
}
