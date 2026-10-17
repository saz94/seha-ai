package ai.seha.app.ui

import ai.seha.app.data.ActivityLevel
import ai.seha.app.data.Appearance
import ai.seha.app.data.Goal
import ai.seha.app.data.Provider
import ai.seha.app.data.Sex
import ai.seha.app.data.tr
import ai.seha.app.domain.Calc
import ai.seha.app.domain.Dates
import ai.seha.app.domain.trim1
import ai.seha.app.health.Health
import ai.seha.app.widget.requestPinWidget
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import java.time.LocalDate

@Composable
fun SettingsTab(vm: AppViewModel, nav: Nav) {
    val data = vm.data
    val s = data.settings
    val context = LocalContext.current
    var confirmErase by remember { mutableStateOf(false) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportText().toByteArray()) }
                vm.message = tr("Backup saved.", "تم حفظ النسخة الاحتياطية.")
            } catch (e: Exception) {
                vm.message = tr("Could not save the backup.", "تعذّر حفظ النسخة الاحتياطية.")
            }
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            } catch (e: Exception) {
                null
            }
            vm.message = if (text != null && vm.importText(text)) tr("Backup restored.", "تمت استعادة النسخة الاحتياطية.")
            else tr("That file is not a Seha backup.", "هذا الملف ليس نسخة احتياطية من صحة.")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(tr("Settings", "الإعدادات"), style = MaterialTheme.typography.headlineSmall)

        Panel(padding = 4.dp) {
            val t = vm.targets
            LinkRow(
                tr("Profile and goal", "الملف والهدف"),
                goalLabel(data.profile.goal) + "  ·  ${t.calories} " + tr("kcal", "سعرة") + "  ·  ${t.protein} " + tr("g protein", "غ بروتين"),
            ) { nav.open("profile") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LinkRow(
                tr("Medical history", "التاريخ الصحي"),
                if (data.profile.conditions.isEmpty() && data.profile.allergies.isBlank()) tr("Nothing added", "لم يُضف شيء")
                else if (data.profile.conditions.isEmpty()) tr("Allergies noted", "الحساسية مسجّلة")
                else tr("${data.profile.conditions.size} conditions listed", "${data.profile.conditions.size} حالات مذكورة"),
            ) { nav.open("medical") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LinkRow(
                tr("AI coach", "المدرب الذكي"),
                providerLabel(s.provider) + "  ·  " + if (vm.hasKey) tr("key added", "المفتاح مضاف") else tr("no key yet", "لا يوجد مفتاح"),
            ) { nav.open("ai") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LinkRow(
                tr("Water", "الماء"),
                tr("${vm.waterTarget / 1000.0} L a day", "${vm.waterTarget / 1000.0} ل يومياً") + "  ·  " + if (s.waterReminder) tr("reminders on", "التذكير مفعّل") else tr("reminders off", "التذكير متوقف"),
            ) { nav.open("water") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LinkRow(
                "Health Connect",
                if (s.healthConnect && vm.healthGranted) tr("Connected", "متصل") else tr("Not connected", "غير متصل"),
            ) { nav.open("health") }
        }

        Heading(tr("Language", "اللغة"))
        Choice(
            listOf("en" to "English", "ar" to "العربية", "" to tr("Same as phone", "مثل الهاتف")),
            Appearance.currentLanguage(), { Appearance.setLanguage(it) },
        )

        Heading(tr("Appearance", "المظهر"))
        Choice(
            listOf("system" to tr("Same as phone", "مثل الهاتف"), "light" to tr("Light", "فاتح"), "dark" to tr("Dark", "داكن")),
            s.themeMode, { vm.saveSettings(s.copy(themeMode = it)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val names = mapOf("aura" to tr("Aura", "هالة"), "oasis" to tr("Oasis", "واحة"), "dusk" to tr("Dusk", "غسق"), "dune" to tr("Dune", "كثيب"), "rose" to tr("Rose", "ورد"))
            for (palette in Palettes) {
                val on = palette.id == s.palette
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { vm.saveSettings(s.copy(palette = palette.id)) }.padding(4.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .border(2.dp, if (on) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(palette.primary),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(names[palette.id] ?: palette.id, style = MaterialTheme.typography.labelMedium, color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Heading(tr("Daily check-in reminder", "تذكير التسجيل اليومي"))
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Ask me how my day was", "اسألني كيف كان يومي"), style = MaterialTheme.typography.titleSmall)
                    Hint(tr("Only if you have not checked in yet.", "فقط إن لم تسجّل يومك بعد."))
                }
                Switch(checked = s.reminderOn, onCheckedChange = { on ->
                    vm.saveSettings(s.copy(reminderOn = on))
                    if (on && Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
            if (s.reminderOn) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Around", "قرابة الساعة"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Stepper(
                        "%02d:00".format(java.util.Locale.ENGLISH, s.reminderHour),
                        { vm.saveSettings(s.copy(reminderHour = (s.reminderHour + 23) % 24)) },
                        { vm.saveSettings(s.copy(reminderHour = (s.reminderHour + 1) % 24)) },
                    )
                }
            }
        }

        Heading(tr("Home-screen widgets", "ودجات الشاشة الرئيسية"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { if (!requestPinWidget(context, today = true)) vm.message = tr("Long-press your home screen and choose Widgets.", "اضغط مطولاً على الشاشة الرئيسية واختر الودجات.") }) { Text(tr("Add Today", "أضف «اليوم»")) }
            OutlinedButton(onClick = { if (!requestPinWidget(context, today = false)) vm.message = tr("Long-press your home screen and choose Widgets.", "اضغط مطولاً على الشاشة الرئيسية واختر الودجات.") }) { Text(tr("Add Next meal", "أضف «الوجبة القادمة»")) }
        }

        Heading(tr("Your data", "بياناتك"))
        Hint(tr("Everything is stored on this phone only. A backup file holds your profile, check-ins, weights and plans (not photos or AI keys).", "كل شيء محفوظ على هذا الهاتف فقط. ملف النسخة الاحتياطية يضم ملفك وتسجيلاتك وأوزانك وخططك (دون الصور ومفاتيح الذكاء الاصطناعي)."))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { saveBackup.launch("seha-backup-${LocalDate.now()}.json") }) { Text(tr("Save a backup", "حفظ نسخة احتياطية")) }
            OutlinedButton(onClick = { openBackup.launch(arrayOf("application/json", "*/*")) }) { Text(tr("Restore", "استعادة")) }
        }
        TextButton(onClick = { confirmErase = true }) { Text(tr("Erase all my data", "مسح كل بياناتي"), color = MaterialTheme.colorScheme.error) }

        Hint(tr("Seha AI gives general fitness and nutrition guidance, not medical advice. Version 1.1", "يقدّم صحة AI إرشاداً عاماً في اللياقة والتغذية وليس نصيحة طبية. الإصدار 1.1"))
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text(tr("Erase all your data?", "مسح كل بياناتك؟")) },
            text = { Text(tr("Your profile, check-ins, weights, photos, meal plans and conversation are deleted from this phone. This cannot be undone.", "سيُحذف ملفك وتسجيلاتك وأوزانك وصورك وخطط وجباتك ومحادثتك من هذا الهاتف. لا يمكن التراجع.")) },
            confirmButton = { TextButton(onClick = { vm.eraseEverything(); confirmErase = false }) { Text(tr("Erase everything", "مسح الكل"), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text(tr("Cancel", "إلغاء")) } },
        )
    }
}

@Composable
private fun LinkRow(title: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Hint(detail)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ------------------------------------------------------------------ AI services

private class Service(val provider: Provider, val maker: String, val keyUrl: String, val note: String)

@Composable
fun AiSettingsScreen(vm: AppViewModel, nav: Nav) {
    val s = vm.data.settings
    val context = LocalContext.current
    val services = listOf(
        Service(Provider.GEMINI, "Google", "https://aistudio.google.com/apikey", tr("Has a free tier. Searches with Google.", "له باقة مجانية. يبحث عبر Google.")),
        Service(Provider.OPENAI, "OpenAI", "https://platform.openai.com/api-keys", tr("Paid per use.", "الدفع حسب الاستخدام.")),
        Service(Provider.CLAUDE, "Anthropic", "https://console.anthropic.com/settings/keys", tr("Paid per use.", "الدفع حسب الاستخدام.")),
    )
    Screen(title = tr("AI coach", "المدرب الذكي"), onBack = { nav.back() }) {
        Hint(
            tr(
                "The coach and the meal planner run on one of three AI services, with your own key. Add a key for any of them and choose which one to use.",
                "يعمل المدرب ومخطط الوجبات عبر إحدى ثلاث خدمات ذكاء اصطناعي بمفتاحك الخاص. أضف مفتاحاً لأي منها واختر ما تستخدمه.",
            ),
        )
        for (service in services) {
            val p = service.provider
            var key by remember(p) { mutableStateOf(vm.keys.get(p)) }
            var show by remember(p) { mutableStateOf(false) }
            var testing by remember(p) { mutableStateOf(false) }
            var result by remember(p) { mutableStateOf<String?>(null) }
            val selected = s.provider == p
            Panel(
                modifier = if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium) else Modifier,
            ) {
                Row(Modifier.clip(MaterialTheme.shapes.small).clickable { vm.saveSettings(s.copy(provider = p)) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected, onClick = { vm.saveSettings(s.copy(provider = p)) })
                    Column(Modifier.weight(1f)) {
                        Text(providerLabel(p), style = MaterialTheme.typography.titleMedium)
                        Hint(service.maker + "  ·  " + service.note)
                    }
                    if (selected) Text(tr("In use", "مستخدم"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it.trim()
                        vm.saveKey(p, key)
                        result = null
                    },
                    label = { Text(tr("API key", "مفتاح API")) },
                    singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { show = !show }) {
                            Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (show) tr("Hide key", "إخفاء المفتاح") else tr("Show key", "إظهار المفتاح"))
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextBox(
                    tr("Model", "النموذج"), s.model(p),
                    { text ->
                        val model = text.trim()
                        vm.saveSettings(
                            when (p) {
                                Provider.GEMINI -> s.copy(modelGemini = model)
                                Provider.OPENAI -> s.copy(modelOpenAi = model)
                                Provider.CLAUDE -> s.copy(modelClaude = model)
                            },
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = {
                            testing = true
                            result = null
                            vm.testKey(p) { error ->
                                testing = false
                                result = error ?: "ok"
                            }
                        },
                        enabled = key.isNotBlank() && !testing,
                    ) {
                        if (testing) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(tr("Test key", "اختبر المفتاح"))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        tr("Get a key", "احصل على مفتاح"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { openLink(context, service.keyUrl) },
                    )
                }
                result?.let {
                    Spacer(Modifier.height(8.dp))
                    if (it == "ok") Text(tr("The key works.", "المفتاح يعمل."), style = MaterialTheme.typography.bodySmall, color = LocalSehaColors.current.good)
                    else Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Search the web", "البحث في الإنترنت"), style = MaterialTheme.typography.titleSmall)
                    Hint(tr("Lets the AI look up recipes on cooking sites and facts on health sites, and link its sources. Slower and uses more of your quota.", "يتيح للذكاء الاصطناعي البحث عن الوصفات في مواقع الطبخ والمعلومات في مواقع الصحة وربط مصادره. أبطأ ويستهلك أكثر من حصتك."))
                }
                Switch(checked = s.webSearch, onCheckedChange = { vm.saveSettings(s.copy(webSearch = it)) })
            }
        }
        Hint(
            tr(
                "Keys are stored only on this phone. When you use the coach or the planner, your profile, medical notes and recent check-ins are sent to the service you selected so it can answer.",
                "المفاتيح محفوظة على هذا الهاتف فقط. عند استخدام المدرب أو المخطط يُرسل ملفك وملاحظاتك الصحية وتسجيلاتك الأخيرة إلى الخدمة التي اخترتها لتتمكن من الإجابة.",
            ),
        )
    }
}

// ------------------------------------------------------------------ profile

@Composable
fun ProfileScreen(vm: AppViewModel, nav: Nav) {
    var p by remember { mutableStateOf(vm.data.profile) }
    var age by remember { mutableStateOf(Calc.age(vm.data.profile).toString()) }
    var height by remember { mutableStateOf(vm.data.profile.heightCm.trim1()) }
    var target by remember { mutableStateOf(vm.data.profile.targetWeightKg.trim1()) }
    val weight = vm.data.latestWeight
    val needsTarget = p.goal == Goal.LOSE || p.goal == Goal.GAIN
    val ok = (age.toIntOrNull() ?: 0) in 14..90 && (height.toDoubleOrNull() ?: 0.0) in 120.0..230.0 &&
        (!needsTarget || (target.toDoubleOrNull() ?: 0.0) in 35.0..250.0)
    val built = p.copy(
        birthYear = LocalDate.now().year - (age.toIntOrNull() ?: Calc.age(p)),
        heightCm = height.toDoubleOrNull() ?: p.heightCm,
        targetWeightKg = if (needsTarget) target.toDoubleOrNull() ?: p.targetWeightKg else weight,
    )

    Screen(
        title = tr("Profile and goal", "الملف والهدف"),
        onBack = { nav.back() },
        bottom = { BottomBar { MainButton(tr("Save changes", "حفظ التغييرات"), { vm.saveProfile(built); nav.back() }, enabled = ok) } },
    ) {
        val t = Calc.targets(built, weight)
        PlanSummary(built.copy(startWeightKg = weight), t.calories, t.protein, t.carbs, t.fat, t.maintenance)
        if (t.proteinLimited) Hint(tr("Protein is kept low because of the kidney condition you listed. Confirm the right amount with your doctor.", "أُبقي البروتين منخفضاً بسبب حالة الكلى التي ذكرتها. تأكد من الكمية المناسبة مع طبيبك."))

        Heading(tr("About you", "عنك"))
        TextBox(tr("Name", "الاسم"), p.name, { p = p.copy(name = it.take(30)) })
        Choice(listOf(Sex.MALE to tr("Male", "ذكر"), Sex.FEMALE to tr("Female", "أنثى")), p.sex, { p = p.copy(sex = it) })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(tr("Age", "العمر"), age, { age = it }, Modifier.weight(1f), tr("years", "سنة"), decimal = false)
            NumberField(tr("Height", "الطول"), height, { height = it }, Modifier.weight(1f), tr("cm", "سم"))
        }

        Heading(tr("Goal", "الهدف"))
        Choice(Goal.values().map { it to goalLabel(it) }, p.goal, { p = p.copy(goal = it) })
        Hint(goalHint(p.goal))
        if (needsTarget) {
            NumberField(tr("Target weight", "الوزن المستهدف"), target, { target = it }, Modifier.fillMaxWidth(), tr("kg", "كغ"))
            Choice(
                listOf(0.25 to tr("0.25 kg a week", "0.25 كغ أسبوعياً"), 0.5 to tr("0.5 kg a week", "0.5 كغ أسبوعياً"), 0.75 to tr("0.75 kg a week", "0.75 كغ أسبوعياً")),
                p.paceKgPerWeek, { p = p.copy(paceKgPerWeek = it) },
            )
        }
        Heading(tr("Outside the gym, your day is", "خارج النادي، يومك"))
        Choice(ActivityLevel.values().map { it to activityLabel(it) }, p.activity, { p = p.copy(activity = it) })
        Heading(tr("Workouts a week", "عدد التمارين أسبوعياً"))
        Stepper("${p.workoutsPerWeek}", { p = p.copy(workoutsPerWeek = (p.workoutsPerWeek - 1).coerceAtLeast(0)) }, { p = p.copy(workoutsPerWeek = (p.workoutsPerWeek + 1).coerceAtMost(7)) })

        Heading(tr("Food", "الطعام"))
        FoodFields(p) { p = it }
    }
}

@Composable
fun MedicalScreen(vm: AppViewModel, nav: Nav) {
    var p by remember { mutableStateOf(vm.data.profile) }
    Screen(
        title = tr("Medical history", "التاريخ الصحي"),
        onBack = { nav.back() },
        bottom = { BottomBar { MainButton(tr("Save changes", "حفظ التغييرات"), { vm.saveProfile(p); nav.back() }) } },
    ) {
        Hint(tr("The coach and the meal planner adapt to what you write here. It stays on your phone and is only sent to the AI service you choose.", "يتكيّف المدرب ومخطط الوجبات مع ما تكتبه هنا. يبقى على هاتفك ولا يُرسل إلا لخدمة الذكاء الاصطناعي التي تختارها."))
        MedicalFields(p) { p = it }
    }
}

// ------------------------------------------------------------------ Health Connect

@Composable
fun HealthScreen(vm: AppViewModel, nav: Nav) {
    val context = LocalContext.current
    val connected = vm.data.settings.healthConnect && vm.healthGranted
    val ask = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        vm.onHealthPermissions(granted)
    }
    Screen(title = "Health Connect", onBack = { nav.back() }) {
        Hint(
            tr(
                "Health Connect is Android's shared health store. Google Fit, Fitbit, Samsung Health and most watches and scales write into it, and Seha reads your steps, sleep, workouts, resting heart rate and weight from there. Weights you log in Seha are written back.",
                "Health Connect هو مخزن الصحة المشترك في أندرويد. تكتب فيه Google Fit و Fitbit و Samsung Health ومعظم الساعات والموازين، ويقرأ «صحة» منه خطواتك ونومك وتمارينك ونبض الراحة ووزنك. والأوزان التي تسجّلها في «صحة» تُكتب فيه.",
            ),
        )
        when (vm.healthStatus) {
            Health.UNSUPPORTED -> Panel(color = MaterialTheme.colorScheme.errorContainer) {
                Text(tr("Health Connect is not available on this phone.", "Health Connect غير متوفر على هذا الهاتف."), color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Health.NEEDS_INSTALL -> Panel {
                Text(tr("Install or update Health Connect", "ثبّت أو حدّث Health Connect"), style = MaterialTheme.typography.titleMedium)
                Hint(tr("It is a free app from Google.", "تطبيق مجاني من Google."))
                Spacer(Modifier.height(10.dp))
                MainButton(tr("Open in Play Store", "افتح في متجر Play"), {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")).setPackage("com.android.vending"))
                    } catch (e: Exception) {
                        openLink(context, "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                    }
                })
            }
            else -> if (!connected) {
                MainButton(tr("Connect Health Connect", "اربط Health Connect"), { ask.launch(Health.permissions) })
                Hint(tr("Android shows a list of what Seha may read. You can allow all or only some.", "يعرض أندرويد قائمة بما يمكن لـ«صحة» قراءته. يمكنك السماح بالكل أو بالبعض."))
            } else {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Connected", "متصل"), style = MaterialTheme.typography.titleMedium, color = LocalSehaColors.current.good, modifier = Modifier.weight(1f))
                        FilledTonalButton(onClick = { vm.syncHealth(quiet = false) }, enabled = !vm.healthBusy) {
                            if (vm.healthBusy) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(tr("Update now", "حدّث الآن"))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val days = vm.data.health.sortedByDescending { it.date }.take(7)
                    if (days.isEmpty()) Hint(tr("Nothing read yet.", "لم يُقرأ شيء بعد."))
                    for (day in days) {
                        Row(Modifier.padding(vertical = 5.dp)) {
                            Text(Dates.short(day.date), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.3f))
                            Text(tr("${day.steps} steps", "${day.steps} خطوة"), style = MaterialTheme.typography.bodyMedium.tabular(), modifier = Modifier.weight(1.2f))
                            Text(if (day.sleepMinutes > 0) tr("${day.sleepMinutes / 60}h ${day.sleepMinutes % 60}m", "${day.sleepMinutes / 60}س ${day.sleepMinutes % 60}د") else "–", style = MaterialTheme.typography.bodyMedium.tabular(), modifier = Modifier.weight(1f))
                        }
                    }
                }
                OutlinedButton(onClick = { ask.launch(Health.permissions) }) { Text(tr("Change what Seha may read", "غيّر ما يمكن لـ«صحة» قراءته")) }
                TextButton(onClick = { vm.disconnectHealth() }) { Text(tr("Stop using Health Connect", "أوقف استخدام Health Connect")) }
            }
        }
    }
}
