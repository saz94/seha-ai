package ai.seha.app.widget

import ai.seha.app.MainActivity
import ai.seha.app.R
import ai.seha.app.data.AppData
import ai.seha.app.data.Appearance
import ai.seha.app.data.Meal
import ai.seha.app.data.Store
import ai.seha.app.data.tr
import ai.seha.app.domain.Calc
import ai.seha.app.domain.trim1
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

private val White = ColorProvider(Color.White)
private val Soft = ColorProvider(Color(0xBFFFFFFF))
private val Saffron = ColorProvider(Color(0xFFF6C263))

/** Opens the app, optionally straight on a screen (see the "route" handling in ui/Root.kt). */
private fun openApp(context: Context, route: String? = null): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (route != null) {
            putExtra("route", route)
            // makes each shortcut a distinct intent, so the launcher does not reuse the wrong one
            action = "ai.seha.app.OPEN.$route"
        }
    }

/** Redraws every placed widget. Called by the app whenever the data changes. */
suspend fun refreshWidgets(context: Context) {
    TodayWidget().updateAll(context)
    MealWidget().updateAll(context)
}

/** Asks the launcher to place a widget, so nobody has to hunt for the long-press menu. */
fun requestPinWidget(context: Context, today: Boolean): Boolean = try {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) false
    else manager.requestPinAppWidget(
        ComponentName(context, if (today) TodayWidgetReceiver::class.java else MealWidgetReceiver::class.java), null, null,
    )
} catch (e: Exception) {
    false
}

private suspend fun load(context: Context): AppData? = withContext(Dispatchers.IO) {
    Appearance.restoreLanguage(context)
    try {
        Store.load(context).takeIf { it.profile.onboarded }
    } catch (e: Exception) {
        null
    }
}

private fun style(size: Int, color: ColorProvider = White, weight: FontWeight = FontWeight.Normal) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = weight)

// ------------------------------------------------------------------ today at a glance

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent { TodayContent(context, data) }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

@Composable
private fun TodayContent(context: Context, data: AppData?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(16.dp)
            .clickable(actionStartActivity(openApp(context, if (data != null && data.checkIn(LocalDate.now().toString()) == null) "checkin" else null))),
    ) {
        if (data == null) {
            Text(context.getString(R.string.app_name), style = style(16, weight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))
            Text(tr("Open the app to set up your plan.", "افتح التطبيق لإعداد خطتك."), style = style(13, Soft))
            return@Column
        }
        val today = LocalDate.now().toString()
        val t = Calc.targets(data.profile, data.latestWeight)
        val plan = data.plan(today)
        val eaten = plan?.meals?.filter { it.eaten }?.sumOf { it.calories } ?: 0
        val checkedIn = data.checkIn(today) != null
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Today", "اليوم"), style = style(14, weight = FontWeight.Medium), maxLines = 1, modifier = GlanceModifier.defaultWeight())
            Text(
                if (checkedIn) tr("Checked in", "تم التسجيل") else tr("Tap to check in", "اضغط للتسجيل"),
                style = style(12, if (checkedIn) Soft else Saffron, FontWeight.Medium), maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(if (plan != null) "$eaten" else "${t.calories}", style = style(26, weight = FontWeight.Bold), maxLines = 1)
            Text(
                if (plan != null) " / ${t.calories} " + tr("kcal eaten", "سعرة") else " " + tr("kcal target", "سعرة الهدف"),
                style = style(12, Soft), maxLines = 1,
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        val health = data.healthOn(today)
        val water = data.water[today] ?: 0
        val waterTarget = Calc.waterTarget(data.profile, data.latestWeight, data.checkIn(today)?.takeIf { it.workedOut }?.workoutMinutes ?: 0, data.settings.hotClimate)
        Row(GlanceModifier.fillMaxWidth()) {
            Figure(tr("Weight", "الوزن"), data.latestWeight.trim1() + " " + tr("kg", "كغ"), GlanceModifier.defaultWeight())
            Figure(tr("Water", "الماء"), "${water / 1000.0} / ${waterTarget / 1000.0} " + tr("L", "ل"), GlanceModifier.defaultWeight())
            if (health != null && health.steps > 0) Figure(tr("Steps", "الخطوات"), "${health.steps}", GlanceModifier.defaultWeight())
            else Figure(tr("Streak", "التتابع"), "${Calc.streak(data)} " + tr("days", "يوم"), GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: GlanceModifier) {
    Column(modifier) {
        Text(label, style = style(11, Soft), maxLines = 1)
        Text(value, style = style(13, White, FontWeight.Medium), maxLines = 1)
    }
}

// ------------------------------------------------------------------ next meal

class MealWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent { MealContent(context, data) }
    }
}

class MealWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MealWidget()
}

/** The first meal of today that has not been eaten; before 11 breakfast comes first, and so on. */
private fun nextMeal(data: AppData): Meal? {
    val meals = data.plan(LocalDate.now().toString())?.meals ?: return null
    val open = meals.filter { !it.eaten }
    if (open.isEmpty()) return null
    val hour = LocalTime.now().hour
    val preferred = when {
        hour < 11 -> "breakfast"
        hour < 16 -> "lunch"
        hour < 18 -> "snack"
        else -> "dinner"
    }
    return open.firstOrNull { it.type == preferred } ?: open.first()
}

fun mealTypeName(type: String): String = when (type) {
    "breakfast" -> tr("Breakfast", "الفطور")
    "lunch" -> tr("Lunch", "الغداء")
    "dinner" -> tr("Dinner", "العشاء")
    else -> tr("Snack", "وجبة خفيفة")
}

@Composable
private fun MealContent(context: Context, data: AppData?) {
    val meal = data?.let { nextMeal(it) }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(actionStartActivity(openApp(context, "meals"))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (meal == null) {
            Text(tr("Next meal", "الوجبة القادمة"), style = style(12, Soft), maxLines = 1)
            Text(
                if (data?.plan(LocalDate.now().toString()) != null) tr("All of today's meals are done", "اكتملت وجبات اليوم")
                else tr("Tap to plan today's meals", "اضغط لتخطيط وجبات اليوم"),
                style = style(15, weight = FontWeight.Medium), maxLines = 1,
            )
        } else {
            Text(mealTypeName(meal.type) + "  ·  ${meal.calories} " + tr("kcal", "سعرة") + "  ·  ${meal.protein} " + tr("g protein", "غ بروتين"), style = style(12, Soft), maxLines = 1)
            Text(meal.name, style = style(15, weight = FontWeight.Medium), maxLines = 1)
        }
    }
}
