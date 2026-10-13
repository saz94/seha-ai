package ai.seha.app.ui

import ai.seha.app.data.CheckIn
import ai.seha.app.data.Goal
import ai.seha.app.data.Meal
import ai.seha.app.data.tr
import ai.seha.app.domain.Calc
import ai.seha.app.domain.Dates
import ai.seha.app.domain.trim1
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalTime

@Composable
fun TodayTab(vm: AppViewModel, nav: Nav) {
    val data = vm.data
    val today = Dates.today()
    val t = vm.targets
    val plan = data.plan(today)
    val checkIn = data.checkIn(today)
    val colors = LocalSehaColors.current
    var askWeight by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refreshTip() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---- greeting
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Dates.long(today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val hour = LocalTime.now().hour
                val hello = when {
                    hour < 12 -> tr("Good morning", "صباح الخير")
                    hour < 18 -> tr("Good afternoon", "مساء الخير")
                    else -> tr("Good evening", "مساء الخير")
                }
                Text(
                    if (data.profile.name.isBlank()) hello else "$hello, ${data.profile.name}",
                    style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            val streak = Calc.streak(data)
            if (streak > 0) {
                Row(
                    Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("$streak day streak", "$streak يوم متتالٍ"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        // ---- the day's energy: the tape gauge
        val eaten = plan?.meals?.filter { it.eaten } ?: emptyList()
        val kcal = eaten.sumOf { it.calories }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
                .clickable { nav.tab = "meals" }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TapeGauge(
                progress = if (t.calories > 0) kcal.toFloat() / t.calories else 0f,
                track = Color.White.copy(alpha = 0.18f),
                fill = Color(0xFFF6C263),
                tick = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(148.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(t.calories - kcal).coerceAtLeast(0)}", style = MaterialTheme.typography.headlineMedium.tabular(), color = Color.White)
                    Text(tr("kcal left", "سعرة متبقية"), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroBar(tr("Protein", "بروتين"), eaten.sumOf { it.protein }, t.protein, tr("g", "غ"), Color.White, onDark = true)
                MacroBar(tr("Carbs", "كربوهيدرات"), eaten.sumOf { it.carbs }, t.carbs, tr("g", "غ"), Color(0xFFF6C263), onDark = true)
                MacroBar(tr("Fat", "دهون"), eaten.sumOf { it.fat }, t.fat, tr("g", "غ"), Color(0xFFF0A8C8), onDark = true)
            }
        }

        // ---- check-in
        if (checkIn == null) {
            Panel(color = MaterialTheme.colorScheme.primaryContainer) {
                Text(tr("How was your day?", "كيف كان يومك؟"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                Text(
                    tr("Did you work out? How did you sleep and eat? A minute now keeps your coach useful.", "هل تمرّنت؟ كيف نمت وأكلت؟ دقيقة الآن تجعل مدربك أدق."),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                MainButton(tr("Check in", "سجّل يومك"), { nav.open("checkin") })
            }
        } else {
            Panel(onClick = { nav.open("checkin") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(colors.good), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(tr("Checked in today", "سجّلت يومك"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(tr("Edit", "تعديل"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Hint(checkInLine(checkIn))
                if (vm.feedbackBusy && checkIn.coachNote.isBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Hint(tr("Your coach is reading your day…", "مدربك يقرأ يومك…"))
                    }
                } else if (checkIn.coachNote.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(checkIn.coachNote, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ---- coach tip
        if (checkIn == null || checkIn.coachNote.isBlank()) {
            Panel(onClick = { nav.tab = "coach" }) {
                Text(tr("From your coach", "من مدربك"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(if (data.tip.isNotBlank() && vm.hasKey) data.tip else builtInTip(vm), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ---- weight and water
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel(Modifier.weight(1f), onClick = { askWeight = true }) {
                Hint(tr("Weight", "الوزن"))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(data.latestWeight.trim1(), style = MaterialTheme.typography.headlineSmall.tabular())
                    Spacer(Modifier.width(4.dp))
                    Text(tr("kg", "كغ"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                }
                val p = data.profile
                Hint(
                    when {
                        p.goal == Goal.LOSE || p.goal == Goal.GAIN -> {
                            val left = kotlin.math.abs(data.latestWeight - p.targetWeightKg)
                            if (left < 0.3) tr("Target reached", "بلغت الهدف") else tr("${left.trim1()} kg to go", "بقي ${left.trim1()} كغ")
                        }
                        else -> tr("Tap to log", "اضغط للتسجيل")
                    },
                )
            }
            Panel(Modifier.weight(1f)) {
                val water = data.water[today] ?: 0
                Hint(tr("Water", "الماء"))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text((water / 1000.0).trim1(), style = MaterialTheme.typography.headlineSmall.tabular())
                    Spacer(Modifier.width(4.dp))
                    Text("/ ${(t.waterMl / 1000.0).trim1()} " + tr("L", "ل"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                }
                Row(
                    Modifier.clip(CircleShape).clickable { vm.addWater(250) }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.water, modifier = Modifier.size(16.dp))
                    Text(tr("Add a glass", "أضف كوباً"), style = MaterialTheme.typography.labelLarge, color = colors.water)
                }
            }
        }

        // ---- from Health Connect
        val health = data.healthOn(today)
        if (data.settings.healthConnect && health != null) {
            Panel(onClick = { nav.open("health") }) {
                Text(tr("From Health Connect", "من Health Connect"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row {
                    Figure(tr("Steps", "الخطوات"), "${health.steps}", Modifier.weight(1f))
                    Figure(tr("Sleep", "النوم"), if (health.sleepMinutes > 0) tr("${health.sleepMinutes / 60}h ${health.sleepMinutes % 60}m", "${health.sleepMinutes / 60}س ${health.sleepMinutes % 60}د") else "–", Modifier.weight(1f))
                    Figure(tr("Exercise", "التمرين"), tr("${health.exerciseMinutes} min", "${health.exerciseMinutes} د"), Modifier.weight(1f))
                }
            }
        } else if (!data.settings.healthConnect) {
            Panel(onClick = { nav.open("health") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Connect Health Connect", "اربط Health Connect"), style = MaterialTheme.typography.titleSmall)
                        Hint(tr("Bring in steps, sleep, workouts and weight from your phone and watch.", "اجلب الخطوات والنوم والتمارين والوزن من هاتفك وساعتك."))
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ---- today's meals
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Today's meals", "وجبات اليوم"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (plan != null) TextButton(onClick = { nav.tab = "meals" }) { Text(tr("All meals", "كل الوجبات")) }
        }
        if (plan == null || plan.meals.isEmpty()) {
            Panel {
                Text(tr("No meals planned for today yet.", "لا توجد وجبات مخططة لليوم بعد."), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = { nav.open("plan") }) { Text(tr("Plan today's meals", "خطط وجبات اليوم")) }
            }
        } else {
            for (meal in plan.meals) MealRow(meal, onOpen = { nav.open("meal/$today/${meal.id}") }, onToggle = { vm.toggleEaten(today, meal.id) })
        }
    }

    if (askWeight) WeightDialog(data.latestWeight, onSave = { vm.logWeight(it); askWeight = false }, onClose = { askWeight = false })
}

@Composable
fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Hint(label)
        Text(value, style = MaterialTheme.typography.titleMedium.tabular(), maxLines = 1)
    }
}

fun checkInLine(c: CheckIn): String {
    val workout = if (c.workedOut) {
        tr("Worked out", "تمرّنت") + (if (c.workoutMinutes > 0) tr(" ${c.workoutMinutes} min", " ${c.workoutMinutes} د") else "")
    } else tr("No workout", "بلا تمرين")
    return workout + "  ·  " + tr("Sleep ${c.sleepHours.trim1()} h", "نوم ${c.sleepHours.trim1()} س") + "  ·  " +
        tr("Mood ${c.mood}/5", "المزاج ${c.mood}/5") + "  ·  " + tr("Energy ${c.energy}/5", "الطاقة ${c.energy}/5")
}

/** The tip shown when no AI key has been added. */
private fun builtInTip(vm: AppViewModel): String {
    val t = vm.targets
    val data = vm.data
    val done = Calc.workoutsThisWeek(data)
    val planned = data.profile.workoutsPerWeek
    val tip = when {
        data.checkins.isEmpty() -> tr(
            "Your targets are ${t.calories} kcal and ${t.protein} g protein a day. Check in tonight so I can start following your week.",
            "هدفك ${t.calories} سعرة و${t.protein} غ بروتين يومياً. سجّل يومك الليلة لأبدأ متابعة أسبوعك.",
        )
        planned > 0 && done < planned -> tr(
            "$done of $planned workouts done this week. Protein target today: ${t.protein} g.",
            "أنجزت $done من $planned تمارين هذا الأسبوع. هدف البروتين اليوم: ${t.protein} غ.",
        )
        else -> tr(
            "You hit your workouts this week. Keep protein near ${t.protein} g and sleep 7 hours or more.",
            "أكملت تمارين الأسبوع. حافظ على البروتين قرب ${t.protein} غ ونم 7 ساعات أو أكثر.",
        )
    }
    return if (vm.hasKey) tip else tip + "\n" + tr("Add an AI key in Settings for personal coaching.", "أضف مفتاح الذكاء الاصطناعي من الإعدادات لتدريب شخصي.")
}

/** One meal in a list: name, numbers, and a round button to mark it eaten. */
@Composable
fun MealRow(meal: Meal, onOpen: () -> Unit, onToggle: () -> Unit) {
    val colors = LocalSehaColors.current
    Panel(onClick = onOpen, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(mealTypeLabel(meal.type), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(meal.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Hint(
                    "${meal.calories} " + tr("kcal", "سعرة") + "  ·  ${meal.protein} " + tr("g protein", "غ بروتين") +
                        if (meal.minutes > 0) "  ·  ${meal.minutes} " + tr("min", "د") else "",
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (meal.eaten) colors.good else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = if (meal.eaten) tr("Eaten", "تم أكلها") else tr("Mark as eaten", "علّمها كمأكولة"),
                    tint = if (meal.eaten) Color.White else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun WeightDialog(current: Double, onSave: (Double) -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf(current.trim1()) }
    val value = text.toDoubleOrNull()
    val ok = value != null && value in 35.0..250.0
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(tr("Today's weight", "وزن اليوم")) },
        text = {
            Column {
                NumberField(tr("Weight", "الوزن"), text, { text = it }, Modifier.fillMaxWidth(), tr("kg", "كغ"))
                Spacer(Modifier.height(6.dp))
                Hint(tr("Weigh in the morning, before eating, for a steady trend.", "قِس وزنك صباحاً قبل الأكل لتحصل على اتجاه ثابت."))
            }
        },
        confirmButton = { TextButton(onClick = { if (ok) onSave(value!!) }, enabled = ok) { Text(tr("Save weight", "حفظ الوزن")) } },
        dismissButton = { TextButton(onClick = onClose) { Text(tr("Cancel", "إلغاء")) } },
    )
}
