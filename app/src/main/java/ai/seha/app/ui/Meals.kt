package ai.seha.app.ui

import ai.seha.app.data.Meal
import ai.seha.app.data.tr
import ai.seha.app.domain.Dates
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun MealsTab(vm: AppViewModel, nav: Nav) {
    val data = vm.data
    val today = LocalDate.now()
    val days = (0..6).map { today.plusDays(it.toLong()).toString() }
    if (nav.mealDate !in days) nav.mealDate = days[0]
    val date = nav.mealDate
    val plan = data.plan(date)
    val t = vm.targets
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Meals", "الوجبات"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (data.plans.any { it.date >= days[0] }) TextButton(onClick = { nav.open("shopping") }) { Text(tr("Shopping list", "قائمة التسوق")) }
        }

        // ---- the next seven days
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (day in days) {
                val on = day == date
                val planned = data.plan(day) != null
                Column(
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .clickable { nav.mealDate = day }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val fg = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Text(
                        if (day == days[0]) tr("Today", "اليوم") else Dates.weekday(day).take(if (ai.seha.app.data.Lang.ar) 12 else 3),
                        style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.75f),
                    )
                    Text("${LocalDate.parse(day).dayOfMonth}", style = MaterialTheme.typography.titleMedium.tabular(), color = fg)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (planned) (if (on) fg else MaterialTheme.colorScheme.primary) else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (vm.planning != null) {
                Panel(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(vm.planning ?: "", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(tr("This can take a minute. You can keep using the app.", "قد يستغرق ذلك دقيقة. يمكنك متابعة استخدام التطبيق."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        TextButton(onClick = { vm.cancelPlanning() }) { Text(tr("Stop", "إيقاف")) }
                    }
                }
            }
            vm.planError?.let { error ->
                Panel(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(tr("The plan was not finished", "لم تكتمل الخطة"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            if (plan != null && plan.meals.isNotEmpty()) {
                val kcal = plan.meals.sumOf { it.calories }
                val protein = plan.meals.sumOf { it.protein }
                Panel {
                    Hint(Dates.long(date))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        MacroBar(tr("Calories", "السعرات"), kcal, t.calories, tr("kcal", "سعرة"), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                        MacroBar(tr("Protein", "بروتين"), protein, t.protein, tr("g", "غ"), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                }
                for (meal in plan.meals) {
                    MealRow(meal, onOpen = { nav.open("meal/$date/${meal.id}") }, onToggle = { vm.toggleEaten(date, meal.id) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { nav.open("plan") }, enabled = vm.planning == null) { Text(tr("Plan this day again", "أعد تخطيط هذا اليوم")) }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = tr("Remove this day's plan", "حذف خطة هذا اليوم"))
                    }
                }
            } else if (vm.planning == null) {
                Panel {
                    Text(tr("Nothing planned for ${Dates.long(date)}", "لا توجد خطة ليوم ${Dates.long(date)}"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Hint(
                        tr(
                            "Tell the planner what you feel like and what to avoid. It builds a day or a week of meals that add up to ${t.calories} kcal and ${t.protein} g protein, with ingredients and steps.",
                            "أخبر المخطط بما تشتهي وما تتجنب. يبني يوماً أو أسبوعاً من الوجبات بمجموع ${t.calories} سعرة و${t.protein} غ بروتين، مع المقادير والخطوات.",
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    if (vm.hasKey) {
                        MainButton(tr("Plan meals", "خطط الوجبات"), { nav.open("plan") })
                    } else {
                        Text(tr("The meal planner needs an AI key (GPT, Gemini or Claude).", "يحتاج مخطط الوجبات إلى مفتاح ذكاء اصطناعي (GPT أو Gemini أو Claude)."), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        MainButton(tr("Add an AI key", "أضف مفتاح الذكاء الاصطناعي"), { nav.open("ai") })
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("Remove this day's plan?", "حذف خطة هذا اليوم؟")) },
            text = { Text(tr("The meals for ${Dates.long(date)} will be removed.", "ستُحذف وجبات ${Dates.long(date)}.")) },
            confirmButton = { TextButton(onClick = { vm.deletePlan(date); confirmDelete = false }) { Text(tr("Remove plan", "حذف الخطة")) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Keep it", "إبقاء")) } },
        )
    }
}

/** What do you want to eat, what to avoid, and for how many days. */
@Composable
fun PlanMealsScreen(vm: AppViewModel, nav: Nav) {
    val start = nav.mealDate
    val t = vm.targets
    var week by remember { mutableStateOf(false) }
    var wanted by remember { mutableStateOf(vm.data.settings.lastWanted) }
    var avoid by remember { mutableStateOf(vm.data.settings.lastAvoid) }

    Screen(
        title = tr("Plan meals", "تخطيط الوجبات"),
        onBack = { nav.back() },
        bottom = {
            BottomBar {
                MainButton(
                    if (week) tr("Plan 7 days", "خطط 7 أيام") else tr("Plan this day", "خطط هذا اليوم"),
                    {
                        val first = LocalDate.parse(start)
                        val dates = if (week) (0..6).map { first.plusDays(it.toLong()).toString() } else listOf(start)
                        vm.planMeals(dates, wanted.trim(), avoid.trim())
                        nav.back()
                    },
                    enabled = vm.hasKey && vm.planning == null,
                )
            }
        },
    ) {
        Heading(tr("Plan for", "خطط لـ"))
        Choice(
            listOf(false to Dates.long(start), true to tr("7 days from ${Dates.dayMonth(start)}", "7 أيام من ${Dates.dayMonth(start)}")),
            week, { week = it },
        )

        Heading(tr("What do you feel like eating?", "ماذا تشتهي أن تأكل؟"))
        TextBox(
            tr("Foods or dishes", "أطعمة أو أطباق"), wanted, { wanted = it },
            placeholder = tr("For example: chicken, rice, something quick", "مثال: دجاج، أرز، شيء سريع"), lines = 2,
        )
        Hint(tr("Name just a food, like \"chicken\", and the planner picks a dish with it. Leave empty for a surprise.", "اذكر طعاماً فقط مثل «دجاج» ليختار المخطط طبقاً منه. اتركه فارغاً للمفاجأة."))

        Heading(tr("Anything to avoid?", "هل تريد تجنّب شيء؟"))
        TextBox(tr("Leave these out", "استبعد هذه"), avoid, { avoid = it }, placeholder = tr("For example: tomato", "مثال: الطماطم"), lines = 2)
        val always = listOf(vm.data.profile.dislikes, vm.data.profile.allergies).filter { it.isNotBlank() }.joinToString(", ")
        if (always.isNotEmpty()) Hint(tr("Always left out from your profile: $always", "مستبعد دائماً من ملفك: $always"))

        Panel {
            Hint(tr("Each day adds up to", "مجموع كل يوم"))
            Text(
                "${t.calories} " + tr("kcal", "سعرة") + "  ·  ${t.protein} " + tr("g protein", "غ بروتين") + "  ·  ${vm.data.profile.mealsPerDay} " + tr("meals", "وجبات"),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(6.dp))
            Hint(
                if (vm.data.settings.webSearch) tr("${providerLabel(vm.data.settings.provider)} searches cooking and nutrition websites and links each recipe to its source.", "يبحث ${providerLabel(vm.data.settings.provider)} في مواقع الطبخ والتغذية ويربط كل وصفة بمصدرها.")
                else tr("Web search is off, so recipes come from ${providerLabel(vm.data.settings.provider)}'s own knowledge.", "البحث في الإنترنت متوقف، فالوصفات من معرفة ${providerLabel(vm.data.settings.provider)} نفسه."),
            )
        }
        if (!vm.hasKey) {
            Panel(color = MaterialTheme.colorScheme.errorContainer, onClick = { nav.open("ai") }) {
                Text(tr("Add an AI key in Settings to plan meals.", "أضف مفتاح الذكاء الاصطناعي من الإعدادات لتخطيط الوجبات."), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

/** One recipe: numbers, ingredients, steps and where it came from. */
@Composable
fun MealScreen(vm: AppViewModel, nav: Nav, date: String, id: String) {
    val meal = vm.data.plan(date)?.meals?.firstOrNull { it.id == id }
    val context = LocalContext.current
    var askSwap by remember { mutableStateOf(false) }
    if (meal == null) {
        Screen(tr("Meal", "وجبة"), onBack = { nav.back() }) { Hint(tr("This meal is no longer in the plan.", "هذه الوجبة لم تعد في الخطة.")) }
        return
    }
    Screen(
        title = mealTypeLabel(meal.type),
        onBack = { nav.back() },
        actions = {
            IconButton(onClick = { share(context, recipeText(meal)) }) { Icon(Icons.Outlined.Share, contentDescription = tr("Share recipe", "مشاركة الوصفة")) }
        },
        bottom = {
            BottomBar {
                MainButton(
                    if (meal.eaten) tr("Mark as not eaten", "علّمها كغير مأكولة") else tr("Mark as eaten", "علّمها كمأكولة"),
                    { vm.toggleEaten(date, meal.id) },
                )
            }
        },
    ) {
        Text(meal.name, style = MaterialTheme.typography.headlineSmall)
        Hint(Dates.long(date) + if (meal.minutes > 0) "  ·  ${meal.minutes} " + tr("min", "د") else "")

        Panel {
            Row {
                Figure(tr("Calories", "السعرات"), "${meal.calories}", Modifier.weight(1f))
                Figure(tr("Protein", "بروتين"), "${meal.protein} " + tr("g", "غ"), Modifier.weight(1f))
                Figure(tr("Carbs", "كربوهيدرات"), "${meal.carbs} " + tr("g", "غ"), Modifier.weight(1f))
                Figure(tr("Fat", "دهون"), "${meal.fat} " + tr("g", "غ"), Modifier.weight(1f))
            }
        }

        if (meal.ingredients.isNotEmpty()) {
            Heading(tr("Ingredients", "المقادير"))
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (item in meal.ingredients) {
                        Row {
                            Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Spacer(Modifier.width(10.dp))
                            Text(item, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        if (meal.steps.isNotEmpty()) {
            Heading(tr("How to make it", "طريقة التحضير"))
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    meal.steps.forEachIndexed { i, step ->
                        Row {
                            Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (meal.source.isNotBlank()) {
            Panel(onClick = { openLink(context, meal.source) }) {
                Hint(tr("Based on a recipe from", "مبنية على وصفة من"))
                Text(
                    meal.sourceName.ifBlank { hostOf(meal.source) },
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline,
                )
            }
        }

        if (vm.planning != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Hint(vm.planning ?: "")
            }
        } else if (vm.hasKey) {
            FilledTonalButton(onClick = { askSwap = true }) { Text(tr("Swap this meal", "استبدل هذه الوجبة")) }
        }
        vm.planError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Hint(tr("Calories and macros are estimates worked out by the AI. Check labels if you need exact figures.", "السعرات والعناصر تقديرات من الذكاء الاصطناعي. راجع الملصقات إن احتجت أرقاماً دقيقة."))
    }

    if (askSwap) {
        var wanted by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askSwap = false },
            title = { Text(tr("Swap this meal", "استبدل هذه الوجبة")) },
            text = {
                Column {
                    Hint(tr("The replacement keeps about the same calories and protein.", "البديل يحافظ على السعرات والبروتين تقريباً."))
                    Spacer(Modifier.height(10.dp))
                    TextBox(tr("What would you like instead? (optional)", "ماذا تفضّل بدلاً منها؟ (اختياري)"), wanted, { wanted = it })
                }
            },
            confirmButton = { TextButton(onClick = { vm.swapMeal(date, meal, wanted.trim()); askSwap = false }) { Text(tr("Find a replacement", "ابحث عن بديل")) } },
            dismissButton = { TextButton(onClick = { askSwap = false }) { Text(tr("Cancel", "إلغاء")) } },
        )
    }
}

/** Every ingredient of the planned days from today on, to tick off in the shop. */
@Composable
fun ShoppingScreen(vm: AppViewModel, nav: Nav) {
    val context = LocalContext.current
    val today = Dates.today()
    val plans = vm.data.plans.filter { it.date >= today && it.meals.any { m -> m.ingredients.isNotEmpty() } }
    val ticked = remember { mutableStateMapOf<String, Boolean>() }
    Screen(
        title = tr("Shopping list", "قائمة التسوق"),
        onBack = { nav.back() },
        actions = {
            IconButton(onClick = {
                share(context, plans.joinToString("\n\n") { p -> Dates.long(p.date) + "\n" + p.meals.flatMap { it.ingredients }.joinToString("\n") { "- $it" } })
            }) { Icon(Icons.Outlined.Share, contentDescription = tr("Share list", "مشاركة القائمة")) }
        },
    ) {
        if (plans.isEmpty()) Hint(tr("Plan some meals and their ingredients appear here.", "خطط بعض الوجبات لتظهر مقاديرها هنا."))
        for (plan in plans) {
            Heading(Dates.long(plan.date))
            Panel(padding = 6.dp) {
                for (meal in plan.meals) {
                    for (item in meal.ingredients) {
                        val key = plan.date + meal.id + item
                        val done = ticked[key] == true
                        Row(Modifier.fillMaxWidth().clickable { ticked[key] = !done }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = done, onCheckedChange = { ticked[key] = it })
                            Text(
                                item, style = MaterialTheme.typography.bodyMedium,
                                color = if (done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (done) TextDecoration.LineThrough else null,
                                textAlign = TextAlign.Start, modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun recipeText(meal: Meal): String = buildString {
    appendLine(meal.name)
    appendLine("${meal.calories} kcal, P ${meal.protein} g, C ${meal.carbs} g, F ${meal.fat} g")
    if (meal.ingredients.isNotEmpty()) {
        appendLine()
        meal.ingredients.forEach { appendLine("- $it") }
    }
    if (meal.steps.isNotEmpty()) {
        appendLine()
        meal.steps.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }
    }
    if (meal.source.isNotBlank()) {
        appendLine()
        appendLine(meal.source)
    }
}

fun share(context: Context, text: String) {
    try {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
    } catch (e: Exception) {
    }
}

fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
    }
}

fun hostOf(url: String): String = try {
    Uri.parse(url).host?.removePrefix("www.") ?: url
} catch (e: Exception) {
    url
}
