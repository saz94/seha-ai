package ai.seha.app.ui

import ai.seha.app.data.CheckIn
import ai.seha.app.data.tr
import ai.seha.app.domain.Dates
import ai.seha.app.domain.trim1
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** The daily question: how was your day, did you work out? */
@Composable
fun CheckInScreen(vm: AppViewModel, nav: Nav) {
    val today = Dates.today()
    val health = vm.data.healthOn(today)
    val existing = vm.data.checkIn(today)
    // A first check-in starts from what Health Connect already knows about today.
    var c by remember {
        mutableStateOf(
            existing ?: CheckIn(
                date = today,
                sleepHours = if (health != null && health.sleepMinutes > 0) Math.round(health.sleepMinutes / 30.0) / 2.0 else 7.0,
                workedOut = health != null && health.exerciseMinutes >= 15,
                workoutMinutes = health?.exerciseMinutes?.toInt() ?: 0,
            ),
        )
    }
    var weight by remember { mutableStateOf("") }

    Screen(
        title = tr("How was your day?", "كيف كان يومك؟"),
        onBack = { nav.back() },
        bottom = {
            BottomBar {
                MainButton(tr("Save check-in", "حفظ التسجيل"), {
                    weight.toDoubleOrNull()?.let { if (it in 35.0..250.0) vm.logWeight(it) }
                    vm.saveCheckIn(c.copy(coachNote = ""))
                    nav.back()
                })
            }
        },
    ) {
        Hint(Dates.long(today))

        Heading(tr("Did you work out?", "هل تمرّنت؟"))
        Choice(listOf(true to tr("Yes", "نعم"), false to tr("No", "لا")), c.workedOut, { c = c.copy(workedOut = it) })
        if (c.workedOut) {
            val kinds = listOf(
                tr("Weights", "أوزان"), tr("Cardio", "كارديو"), tr("Walk", "مشي"), tr("Sport", "رياضة"),
                tr("Swim", "سباحة"), tr("Stretching", "تمدد"),
            )
            Choice(kinds.map { it to it }, c.workoutType, { c = c.copy(workoutType = it) })
            Stepper(
                tr("${c.workoutMinutes} min", "${c.workoutMinutes} د"),
                { c = c.copy(workoutMinutes = (c.workoutMinutes - 5).coerceAtLeast(0)) },
                { c = c.copy(workoutMinutes = (c.workoutMinutes + 5).coerceAtMost(300)) },
            )
        }

        Heading(tr("Did you eat on plan?", "هل التزمت بخطة الأكل؟"))
        Choice(listOf(2 to tr("Yes", "نعم"), 1 to tr("Partly", "جزئياً"), 0 to tr("No", "لا")), c.ateOnPlan, { c = c.copy(ateOnPlan = it) })

        Heading(tr("Sleep last night", "نوم الليلة الماضية"))
        Stepper(
            tr("${c.sleepHours.trim1()} h", "${c.sleepHours.trim1()} س"),
            { c = c.copy(sleepHours = (c.sleepHours - 0.5).coerceAtLeast(0.0)) },
            { c = c.copy(sleepHours = (c.sleepHours + 0.5).coerceAtMost(14.0)) },
        )
        if (health != null && health.sleepMinutes > 0) Hint(tr("Health Connect recorded ${health.sleepMinutes / 60}h ${health.sleepMinutes % 60}m.", "سجّل Health Connect ${health.sleepMinutes / 60}س ${health.sleepMinutes % 60}د."))

        Heading(tr("Mood", "المزاج"))
        Scale5(c.mood, { c = c.copy(mood = it) }, tr("Low", "منخفض"), tr("Great", "ممتاز"))

        Heading(tr("Energy", "الطاقة"))
        Scale5(c.energy, { c = c.copy(energy = it) }, tr("Drained", "منهك"), tr("Full of energy", "مليء بالطاقة"))

        Heading(tr("Weight today (optional)", "وزن اليوم (اختياري)"))
        NumberField(tr("Weight", "الوزن"), weight, { weight = it }, Modifier.fillMaxWidth(), tr("kg", "كغ"))

        Heading(tr("Anything to tell your coach?", "هل تريد إخبار مدربك بشيء؟"))
        TextBox(tr("Notes", "ملاحظات"), c.notes, { c = c.copy(notes = it) }, placeholder = tr("For example: knee felt sore, ate out at lunch", "مثال: ألم في الركبة، أكلت خارج البيت"), lines = 3)
    }
}
