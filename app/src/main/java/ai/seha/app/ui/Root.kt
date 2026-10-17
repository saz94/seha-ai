package ai.seha.app.ui

import ai.seha.app.data.tr
import ai.seha.app.widget.requestPinWidget
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Where the user is: one of five tabs, with any opened screens stacked on top. */
class Nav {
    var tab by mutableStateOf("today")

    /** The day the Meals tab is showing; the planner starts from it. */
    var mealDate by mutableStateOf(java.time.LocalDate.now().toString())
    val stack = mutableStateListOf<String>()

    fun open(route: String) {
        if (stack.lastOrNull() != route) stack.add(route)
    }

    fun back() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun goTab(name: String) {
        stack.clear()
        tab = name
    }
}

private class Tab(val id: String, val icon: ImageVector, val label: String)

@Composable
fun AppRoot(vm: AppViewModel, startRoute: String?) {
    val nav = remember { Nav() }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(startRoute) {
        when (startRoute) {
            null -> {}
            "today", "meals", "coach", "progress", "more" -> nav.goTab(startRoute)
            "pintoday" -> requestPinWidget(context, today = true)
            "pinmeal" -> requestPinWidget(context, today = false)
            else -> if (vm.data.profile.onboarded) nav.open(startRoute)
        }
    }
    LaunchedEffect(vm.message) {
        val text = vm.message ?: return@LaunchedEffect
        vm.message = null
        snackbar.showSnackbar(text)
    }

    if (!vm.data.profile.onboarded) {
        Onboarding(vm)
        return
    }

    BackHandler(enabled = nav.stack.isNotEmpty() || nav.tab != "today") {
        if (nav.stack.isNotEmpty()) nav.back() else nav.tab = "today"
    }

    val route = nav.stack.lastOrNull()
    if (route != null) {
        Box(Modifier.fillMaxSize()) {
            val parts = route.split("/")
            when (parts[0]) {
                "checkin" -> CheckInScreen(vm, nav)
                "plan" -> PlanMealsScreen(vm, nav)
                "meal" -> MealScreen(vm, nav, parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" })
                "shopping" -> ShoppingScreen(vm, nav)
                "measure" -> MeasureScreen(vm, nav)
                "water" -> WaterScreen(vm, nav)
                "ai" -> AiSettingsScreen(vm, nav)
                "profile" -> ProfileScreen(vm, nav)
                "medical" -> MedicalScreen(vm, nav)
                "health" -> HealthScreen(vm, nav)
                else -> LaunchedEffect(route) { nav.back() }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
        }
        return
    }

    val tabs = listOf(
        Tab("today", Icons.Outlined.WbSunny, tr("Today", "اليوم")),
        Tab("meals", Icons.Outlined.Restaurant, tr("Meals", "الوجبات")),
        Tab("coach", Icons.Outlined.Forum, tr("Coach", "المدرب")),
        Tab("progress", Icons.Outlined.Insights, tr("Progress", "التقدّم")),
        Tab("more", Icons.Outlined.Tune, tr("Settings", "الإعدادات")),
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // the keyboard takes the bar's place while typing to the coach
            if (!WindowInsets.isImeVisible) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                for (tab in tabs) {
                    NavigationBarItem(
                        selected = nav.tab == tab.id,
                        onClick = { nav.tab = tab.id },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (nav.tab) {
                "meals" -> MealsTab(vm, nav)
                "coach" -> CoachTab(vm, nav)
                "progress" -> ProgressTab(vm, nav)
                "more" -> SettingsTab(vm, nav)
                else -> TodayTab(vm, nav)
            }
        }
    }
}
