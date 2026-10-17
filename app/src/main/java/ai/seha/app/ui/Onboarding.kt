package ai.seha.app.ui

import ai.seha.app.data.ActivityLevel
import ai.seha.app.data.Appearance
import ai.seha.app.data.Diet
import ai.seha.app.data.Goal
import ai.seha.app.data.Lang
import ai.seha.app.data.Profile
import ai.seha.app.data.Sex
import ai.seha.app.data.tr
import ai.seha.app.domain.Calc
import ai.seha.app.domain.trim1
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private const val STEPS = 6

@Composable
fun Onboarding(vm: AppViewModel) {
    var step by rememberSaveable { mutableStateOf(0) }
    var p by remember { mutableStateOf(vm.data.profile) }
    var age by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }

    fun number(text: String): Double = text.toDoubleOrNull() ?: 0.0
    val ageOk = number(age) in 14.0..90.0
    val heightOk = number(height) in 120.0..230.0
    val weightOk = number(weight) in 35.0..250.0
    val needsTarget = p.goal == Goal.LOSE || p.goal == Goal.GAIN
    val targetOk = !needsTarget || (number(target) in 35.0..250.0 &&
        if (p.goal == Goal.LOSE) number(target) < number(weight) else number(target) > number(weight))

    /** The profile as it stands, with the typed figures folded in. */
    fun built(): Profile = p.copy(
        birthYear = LocalDate.now().year - number(age).toInt(),
        heightCm = number(height),
        startWeightKg = number(weight),
        targetWeightKg = if (needsTarget) number(target) else number(weight),
    )

    val canGoOn = when (step) {
        1 -> ageOk && heightOk && weightOk
        2 -> targetOk
        else -> true
    }

    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        // progress: one segment per step
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until STEPS) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (step) {
                0 -> {
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Orb(Modifier.size(260.dp), mood = "happy")
                        Surface(
                            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 18.dp),
                        ) {
                            Text(tr("Hello!", "مرحباً!"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                        }
                    }
                    Text(
                        buildAnnotatedString {
                            append(tr("Your ", "مساعدك "))
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(tr("smart coach", "الذكي")) }
                            append(tr(" for your body", " لجسمك"))
                        },
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        tr(
                            "Answer a few questions and Seha builds your calorie and protein targets, plans meals you actually want to eat, and checks in with you every day.",
                            "أجب عن بضعة أسئلة ليحسب «صحة» سعراتك وبروتينك، ويخطط وجبات تحب أكلها، ويسألك عن يومك كل مساء.",
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Heading(tr("Language", "اللغة"), Modifier.align(Alignment.CenterHorizontally))
                    Choice(
                        options = listOf("en" to "English", "ar" to "العربية"),
                        selected = if (Lang.ar) "ar" else "en",
                        onSelect = { Appearance.setLanguage(it) },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

                1 -> {
                    StepTitle(tr("About you", "عنك"), tr("These set how much energy your body uses.", "هذه تحدد كمية الطاقة التي يستهلكها جسمك."))
                    TextBox(tr("Name", "الاسم"), p.name, { p = p.copy(name = it.take(30)) })
                    Choice(listOf(Sex.MALE to tr("Male", "ذكر"), Sex.FEMALE to tr("Female", "أنثى")), p.sex, { p = p.copy(sex = it) })
                    NumberField(tr("Age", "العمر"), age, { age = it }, Modifier.fillMaxWidth(), tr("years", "سنة"), decimal = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberField(tr("Height", "الطول"), height, { height = it }, Modifier.weight(1f), tr("cm", "سم"))
                        NumberField(tr("Weight", "الوزن"), weight, { weight = it }, Modifier.weight(1f), tr("kg", "كغ"))
                    }
                    if (heightOk && weightOk) {
                        val bmi = Calc.bmi(number(weight), number(height))
                        Hint(tr("BMI ${bmi.trim1()}: ${Calc.bmiLabel(bmi)}", "مؤشر الكتلة ${bmi.trim1()}: ${Calc.bmiLabel(bmi)}"))
                    }
                }

                2 -> {
                    StepTitle(tr("Your goal", "هدفك"), tr("You can change this any time.", "يمكنك تغييره في أي وقت."))
                    Choice(Goal.values().map { it to goalLabel(it) }, p.goal, { p = p.copy(goal = it) })
                    Hint(goalHint(p.goal))
                    if (needsTarget) {
                        NumberField(tr("Target weight", "الوزن المستهدف"), target, { target = it }, Modifier.fillMaxWidth(), tr("kg", "كغ"))
                        if (target.isNotEmpty() && !targetOk) {
                            Text(
                                if (p.goal == Goal.LOSE) tr("To lose fat, the target must be below your weight now.", "لخسارة الدهون يجب أن يكون الهدف أقل من وزنك الحالي.")
                                else tr("To build muscle, the target must be above your weight now.", "لبناء العضلات يجب أن يكون الهدف أعلى من وزنك الحالي."),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Heading(tr("Pace", "الوتيرة"))
                        Choice(
                            listOf(
                                0.25 to tr("Gentle, 0.25 kg a week", "هادئة، 0.25 كغ أسبوعياً"),
                                0.5 to tr("Steady, 0.5 kg a week", "ثابتة، 0.5 كغ أسبوعياً"),
                                0.75 to tr("Fast, 0.75 kg a week", "سريعة، 0.75 كغ أسبوعياً"),
                            ),
                            p.paceKgPerWeek, { p = p.copy(paceKgPerWeek = it) },
                        )
                    }
                    Heading(tr("Outside the gym, your day is", "خارج النادي، يومك"))
                    Choice(ActivityLevel.values().map { it to activityLabel(it) }, p.activity, { p = p.copy(activity = it) })
                    Heading(tr("Workouts a week", "عدد التمارين أسبوعياً"))
                    Stepper("${p.workoutsPerWeek}", { p = p.copy(workoutsPerWeek = (p.workoutsPerWeek - 1).coerceAtLeast(0)) }, { p = p.copy(workoutsPerWeek = (p.workoutsPerWeek + 1).coerceAtMost(7)) })
                }

                3 -> {
                    StepTitle(
                        tr("Medical history", "التاريخ الصحي"),
                        tr("Optional. The coach and the meal planner adapt to it. It stays on your phone and is only sent to the AI service you choose.", "اختياري. يتكيّف المدرب ومخطط الوجبات معه. يبقى على هاتفك ولا يُرسل إلا لخدمة الذكاء الاصطناعي التي تختارها."),
                    )
                    MedicalFields(p) { p = it }
                }

                4 -> {
                    StepTitle(tr("Food", "الطعام"), tr("So the meal plans fit how you eat.", "لتناسب خطط الوجبات طريقة أكلك."))
                    FoodFields(p) { p = it }
                }

                else -> {
                    val profile = built()
                    val t = Calc.targets(profile, profile.startWeightKg)
                    StepTitle(tr("Your plan", "خطتك"), tr("Worked out from your answers. It updates as your weight changes.", "محسوبة من إجاباتك، وتتحدث مع تغيّر وزنك."))
                    PlanSummary(profile, t.calories, t.protein, t.carbs, t.fat, t.maintenance)
                    if (t.floored) {
                        Hint(tr("Your pace was eased: the app does not plan below ${t.calories} kcal a day.", "خُففت الوتيرة: التطبيق لا يخطط لأقل من ${t.calories} سعرة يومياً."))
                    }
                    if (t.proteinLimited) {
                        Hint(tr("Protein is kept low because of the kidney condition you listed. Confirm the right amount with your doctor.", "أُبقي البروتين منخفضاً بسبب حالة الكلى التي ذكرتها. تأكد من الكمية المناسبة مع طبيبك."))
                    }
                    Hint(tr("Seha gives general fitness and nutrition guidance. It is not medical advice; check with your doctor before big changes, especially with a medical condition.", "يقدّم «صحة» إرشاداً عاماً في اللياقة والتغذية، وليس نصيحة طبية؛ استشر طبيبك قبل التغييرات الكبيرة، خاصة مع وجود حالة صحية."))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) {
                TextButton(onClick = { step-- }) { Text(tr("Back", "رجوع")) }
                Spacer(Modifier.width(8.dp))
            }
            MainButton(
                text = when (step) {
                    0 -> tr("Get started", "ابدأ")
                    STEPS - 1 -> tr("Start using Seha", "ابدأ استخدام صحة")
                    else -> tr("Continue", "متابعة")
                },
                onClick = { if (step < STEPS - 1) step++ else vm.finishOnboarding(built()) },
                enabled = canGoOn,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The headline card: daily calories with the three macros. Also shown on the profile screen. */
@Composable
fun PlanSummary(profile: Profile, calories: Int, protein: Int, carbs: Int, fat: Int, maintenance: Int) {
    val colors = LocalSehaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
            .padding(20.dp),
    ) {
        Text(tr("Eat each day", "تناول يومياً"), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.75f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$calories", style = MaterialTheme.typography.displaySmall.tabular(), color = Color.White)
            Spacer(Modifier.width(6.dp))
            Text(tr("kcal", "سعرة"), style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 7.dp))
        }
        Text(
            tr("You burn about $maintenance kcal a day.", "تحرق نحو $maintenance سعرة يومياً."),
            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MacroFigure(tr("Protein", "بروتين"), protein, Modifier.weight(1f))
            MacroFigure(tr("Carbs", "كربوهيدرات"), carbs, Modifier.weight(1f))
            MacroFigure(tr("Fat", "دهون"), fat, Modifier.weight(1f))
        }
        val weeks = Calc.weeksToGoal(profile, profile.startWeightKg)
        if (weeks != null && weeks > 0) {
            Spacer(Modifier.height(14.dp))
            Text(
                tr("At this pace you reach ${profile.targetWeightKg.trim1()} kg in about $weeks weeks.", "بهذه الوتيرة تصل إلى ${profile.targetWeightKg.trim1()} كغ خلال نحو $weeks أسبوعاً."),
                style = MaterialTheme.typography.bodyMedium, color = Color.White,
            )
        }
    }
}

@Composable
private fun MacroFigure(label: String, grams: Int, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f), maxLines = 1)
        Text("$grams " + tr("g", "غ"), style = MaterialTheme.typography.titleMedium.tabular(), color = Color.White)
    }
}

/** Used by onboarding and by the medical screen in Settings. */
@Composable
fun MedicalFields(p: Profile, onChange: (Profile) -> Unit) {
    Heading(tr("Do any of these apply to you?", "هل ينطبق عليك أي من هذه؟"))
    MultiChoice(conditionOptions(), p.conditions.toSet(), { id ->
        onChange(p.copy(conditions = if (id in p.conditions) p.conditions - id else p.conditions + id))
    })
    TextBox(tr("Food allergies or intolerances", "حساسية أو عدم تحمّل أطعمة"), p.allergies, { onChange(p.copy(allergies = it)) }, placeholder = tr("For example: nuts, lactose", "مثال: المكسرات، اللاكتوز"))
    TextBox(tr("Medications you take", "الأدوية التي تتناولها"), p.medications, { onChange(p.copy(medications = it)) })
    TextBox(tr("Injuries or pain", "إصابات أو آلام"), p.injuries, { onChange(p.copy(injuries = it)) }, placeholder = tr("For example: lower back, right knee", "مثال: أسفل الظهر، الركبة اليمنى"))
    TextBox(tr("Anything else the coach should know", "أي شيء آخر يجب أن يعرفه المدرب"), p.medicalNotes, { onChange(p.copy(medicalNotes = it)) }, lines = 2)
}

@Composable
fun FoodFields(p: Profile, onChange: (Profile) -> Unit) {
    Heading(tr("Diet", "النظام الغذائي"))
    Choice(listOf(Diet.HALAL, Diet.ANY, Diet.PESCATARIAN, Diet.VEGETARIAN, Diet.VEGAN).map { it to dietLabel(it) }, p.diet, { onChange(p.copy(diet = it)) })
    TextBox(tr("Foods you never want", "أطعمة لا تريدها أبداً"), p.dislikes, { onChange(p.copy(dislikes = it)) }, placeholder = tr("For example: tomato, mushrooms", "مثال: الطماطم، الفطر"))
    TextBox(tr("Cuisines you enjoy", "مطابخ تحبها"), p.cuisines, { onChange(p.copy(cuisines = it)) }, placeholder = tr("For example: Emirati, Indian, Italian", "مثال: إماراتي، هندي، إيطالي"))
    Heading(tr("Meals a day", "عدد الوجبات يومياً"))
    Stepper("${p.mealsPerDay}", { onChange(p.copy(mealsPerDay = (p.mealsPerDay - 1).coerceAtLeast(2))) }, { onChange(p.copy(mealsPerDay = (p.mealsPerDay + 1).coerceAtMost(5))) })
}
