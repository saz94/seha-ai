package ai.seha.app.ui

import ai.seha.app.data.Goal
import ai.seha.app.data.Measurement
import ai.seha.app.data.Photo
import ai.seha.app.data.tr
import ai.seha.app.domain.Calc
import ai.seha.app.domain.Dates
import ai.seha.app.domain.trim1
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun ProgressTab(vm: AppViewModel, nav: Nav) {
    val data = vm.data
    val p = data.profile
    val weight = data.latestWeight
    val weights = data.weights.sortedBy { it.date }.takeLast(60)
    var askWeight by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Photo?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addPhoto(uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(tr("Progress", "التقدّم"), style = MaterialTheme.typography.headlineSmall)

        // ---- start, now, target
        Panel {
            val change = weight - p.startWeightKg
            Text(
                when {
                    abs(change) < 0.1 -> tr("Starting point: ${p.startWeightKg.trim1()} kg", "نقطة البداية: ${p.startWeightKg.trim1()} كغ")
                    change < 0 -> tr("${abs(change).trim1()} kg down since you started", "نزلت ${abs(change).trim1()} كغ منذ البداية")
                    else -> tr("${change.trim1()} kg up since you started", "زدت ${change.trim1()} كغ منذ البداية")
                },
                style = MaterialTheme.typography.titleLarge,
            )
            if (p.goal == Goal.LOSE || p.goal == Goal.GAIN) {
                Spacer(Modifier.height(12.dp))
                val progress = Calc.goalProgress(p, weight)
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxWidth(progress).height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.primary))
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Figure(tr("Start", "البداية"), p.startWeightKg.trim1(), Modifier.weight(1f))
                    Figure(tr("Now", "الآن"), weight.trim1(), Modifier.weight(1f))
                    Figure(tr("Target", "الهدف"), p.targetWeightKg.trim1(), Modifier.weight(1f))
                }
                val weeks = Calc.weeksToGoal(p, weight)
                if (weeks != null) {
                    Spacer(Modifier.height(8.dp))
                    Hint(if (weeks == 0) tr("You reached your target. Consider switching your goal to staying healthy.", "بلغت هدفك. فكّر في تغيير هدفك إلى المحافظة على الصحة.") else tr("About $weeks weeks to go at your pace.", "بقي نحو $weeks أسبوعاً بوتيرتك."))
                }
            }
        }

        // ---- weight over time
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Weight", "الوزن"), style = MaterialTheme.typography.titleMedium)
                    val bmi = Calc.bmi(weight, p.heightCm)
                    Hint(tr("BMI ${bmi.trim1()}, ${Calc.bmiLabel(bmi)}", "مؤشر الكتلة ${bmi.trim1()}، ${Calc.bmiLabel(bmi)}"))
                }
                FilledTonalButton(onClick = { askWeight = true }) { Text(tr("Log weight", "سجّل الوزن")) }
            }
            Spacer(Modifier.height(14.dp))
            if (weights.isEmpty()) {
                Hint(tr("Log your weight a few mornings a week and the trend appears here.", "سجّل وزنك بضع مرات صباحاً كل أسبوع ليظهر الاتجاه هنا."))
            } else {
                LineChart(
                    values = weights.map { it.kg },
                    firstLabel = Dates.dayMonth(weights.first().date),
                    lastLabel = Dates.dayMonth(weights.last().date),
                    goal = if (p.goal == Goal.LOSE || p.goal == Goal.GAIN) p.targetWeightKg else null,
                    unit = tr("kg", "كغ"),
                )
                if (p.goal == Goal.LOSE || p.goal == Goal.GAIN) {
                    Spacer(Modifier.height(6.dp))
                    Hint(tr("The dashed line is your target.", "الخط المتقطع هو هدفك."))
                }
            }
        }

        // ---- the last seven days
        val today = LocalDate.now()
        val week = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
        val checkins = week.mapNotNull { data.checkIn(it) }
        Panel {
            Text(tr("Last 7 days", "آخر 7 أيام"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row {
                Figure(tr("Workouts", "التمارين"), "${checkins.count { it.workedOut }} / ${p.workoutsPerWeek}", Modifier.weight(1f))
                Figure(tr("Avg sleep", "متوسط النوم"), if (checkins.isEmpty()) "–" else tr("${checkins.map { it.sleepHours }.average().trim1()} h", "${checkins.map { it.sleepHours }.average().trim1()} س"), Modifier.weight(1f))
                Figure(tr("Avg energy", "متوسط الطاقة"), if (checkins.isEmpty()) "–" else "${checkins.map { it.energy }.average().trim1()} / 5", Modifier.weight(1f))
            }
            val steps = week.map { (data.healthOn(it)?.steps ?: 0L).toFloat() }
            if (steps.any { it > 0f }) {
                Spacer(Modifier.height(14.dp))
                Hint(tr("Steps per day", "الخطوات يومياً"))
                Spacer(Modifier.height(6.dp))
                DayBars(steps)
                Spacer(Modifier.height(4.dp))
                Hint(tr("Average ${steps.filter { it > 0 }.average().toInt()} steps", "المتوسط ${steps.filter { it > 0 }.average().toInt()} خطوة"))
            } else {
                Spacer(Modifier.height(14.dp))
                Hint(tr("Workout minutes per day", "دقائق التمرين يومياً"))
                Spacer(Modifier.height(6.dp))
                DayBars(week.map { (data.checkIn(it)?.takeIf { c -> c.workedOut }?.workoutMinutes ?: 0).toFloat() })
            }
        }

        // ---- tape measurements
        val first = data.measurements.minByOrNull { it.date }
        val last = data.measurements.maxByOrNull { it.date }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Measurements", "القياسات"), style = MaterialTheme.typography.titleMedium)
                    Hint(if (last == null) tr("The tape shows changes the scale misses.", "شريط القياس يُظهر ما لا يُظهره الميزان.") else tr("Last taken ${Dates.dayMonth(last.date)}", "آخر قياس ${Dates.dayMonth(last.date)}"))
                }
                FilledTonalButton(onClick = { nav.open("measure") }) { Text(tr("Measure", "قياس جديد")) }
            }
            if (last != null && first != null) {
                Spacer(Modifier.height(12.dp))
                val rows = listOf(
                    Triple(tr("Waist", "الخصر"), last.waist, first.waist), Triple(tr("Chest", "الصدر"), last.chest, first.chest),
                    Triple(tr("Arm", "الذراع"), last.arm, first.arm), Triple(tr("Hips", "الورك"), last.hips, first.hips),
                    Triple(tr("Thigh", "الفخذ"), last.thigh, first.thigh),
                ).filter { it.second != null }
                for ((label, now, start) in rows) {
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        val diff = if (start != null && now != null && first.date != last.date) now - start else 0.0
                        if (abs(diff) >= 0.1) {
                            Text((if (diff > 0) "+" else "−") + abs(diff).trim1(), style = MaterialTheme.typography.labelLarge.tabular(), color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                        }
                        Text((now ?: 0.0).trim1() + " " + tr("cm", "سم"), style = MaterialTheme.typography.titleSmall.tabular())
                    }
                }
                if (last.bodyFat != null) {
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text(tr("Body fat", "نسبة الدهون"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text((last.bodyFat ?: 0.0).trim1() + "%", style = MaterialTheme.typography.titleSmall.tabular())
                    }
                }
            }
        }

        // ---- photos
        Panel {
            Text(tr("Progress photos", "صور التقدّم"), style = MaterialTheme.typography.titleMedium)
            Hint(tr("Same light, same pose, every two weeks. Photos stay on this phone.", "الإضاءة نفسها والوقفة نفسها كل أسبوعين. الصور تبقى على هذا الهاتف."))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(width = 96.dp, height = 128.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Text(tr("Add photo", "أضف صورة"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                for (photo in data.photos.sortedByDescending { it.date }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = vm.photoFile(photo),
                            contentDescription = tr("Photo from ${Dates.dayMonth(photo.date)}", "صورة بتاريخ ${Dates.dayMonth(photo.date)}"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 96.dp, height = 128.dp).clip(MaterialTheme.shapes.small).clickable { viewing = photo },
                        )
                        Spacer(Modifier.height(4.dp))
                        Hint(Dates.dayMonth(photo.date))
                    }
                }
            }
        }

        // ---- recent weigh-ins
        if (weights.isNotEmpty()) {
            Panel {
                Text(tr("Recent weigh-ins", "آخر القياسات"), style = MaterialTheme.typography.titleMedium)
                for (entry in weights.takeLast(5).reversed()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Dates.short(entry.date), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (entry.fromHealth) {
                            Hint("Health Connect")
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(entry.kg.trim1() + " " + tr("kg", "كغ"), style = MaterialTheme.typography.titleSmall.tabular())
                        IconButton(onClick = { vm.deleteWeight(entry.date) }) {
                            Icon(Icons.Outlined.Close, contentDescription = tr("Remove this weigh-in", "حذف هذا القياس"), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (askWeight) WeightDialog(weight, onSave = { vm.logWeight(it); askWeight = false }, onClose = { askWeight = false })

    viewing?.let { photo ->
        AlertDialog(
            onDismissRequest = { viewing = null },
            title = { Text(Dates.long(photo.date)) },
            text = {
                AsyncImage(
                    model = vm.photoFile(photo), contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(380.dp).clip(MaterialTheme.shapes.small),
                )
            },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text(tr("Close", "إغلاق")) } },
            dismissButton = { TextButton(onClick = { vm.deletePhoto(photo); viewing = null }) { Text(tr("Delete photo", "حذف الصورة")) } },
        )
    }
}

/** Tape measurements for today. */
@Composable
fun MeasureScreen(vm: AppViewModel, nav: Nav) {
    val today = Dates.today()
    val last = vm.data.measurements.maxByOrNull { it.date }
    fun start(v: Double?): String = v?.trim1() ?: ""
    var waist by remember { mutableStateOf(start(last?.waist)) }
    var chest by remember { mutableStateOf(start(last?.chest)) }
    var arm by remember { mutableStateOf(start(last?.arm)) }
    var hips by remember { mutableStateOf(start(last?.hips)) }
    var thigh by remember { mutableStateOf(start(last?.thigh)) }
    var fat by remember { mutableStateOf(start(last?.bodyFat)) }
    val cm = tr("cm", "سم")

    Screen(
        title = tr("Measurements", "القياسات"),
        onBack = { nav.back() },
        bottom = {
            BottomBar {
                MainButton(tr("Save measurements", "حفظ القياسات"), {
                    vm.saveMeasurement(
                        Measurement(today, waist.toDoubleOrNull(), chest.toDoubleOrNull(), arm.toDoubleOrNull(), hips.toDoubleOrNull(), thigh.toDoubleOrNull(), fat.toDoubleOrNull()),
                    )
                    nav.back()
                }, enabled = listOf(waist, chest, arm, hips, thigh, fat).any { it.toDoubleOrNull() != null })
            }
        },
    ) {
        Hint(tr("Measure in the morning, tape flat and snug, not tight. Fill in only what you measure.", "قِس صباحاً والشريط مستوٍ وملاصق دون شدّ. املأ ما تقيسه فقط."))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(tr("Waist", "الخصر"), waist, { waist = it }, Modifier.weight(1f), cm)
            NumberField(tr("Chest", "الصدر"), chest, { chest = it }, Modifier.weight(1f), cm)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(tr("Arm", "الذراع"), arm, { arm = it }, Modifier.weight(1f), cm)
            NumberField(tr("Hips", "الورك"), hips, { hips = it }, Modifier.weight(1f), cm)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(tr("Thigh", "الفخذ"), thigh, { thigh = it }, Modifier.weight(1f), cm)
            NumberField(tr("Body fat", "نسبة الدهون"), fat, { fat = it }, Modifier.weight(1f), "%")
        }
        Hint(tr("Waist: at the navel. Chest: across the widest part. Arm: flexed, at the peak. Hips and thigh: at the widest point.", "الخصر: عند السرّة. الصدر: عند أعرض نقطة. الذراع: مشدودة عند القمة. الورك والفخذ: عند أعرض نقطة."))
    }
}
