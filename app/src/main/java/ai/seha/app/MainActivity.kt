package ai.seha.app

import ai.seha.app.data.Appearance
import ai.seha.app.data.Store
import ai.seha.app.ui.AppRoot
import ai.seha.app.ui.AppViewModel
import ai.seha.app.ui.SehaTheme
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light / dark / follow-the-phone. AppCompat also restores the chosen language.
        Appearance.applyTheme(Store.load(this).settings.themeMode)
        super.onCreate(savedInstanceState)
        Appearance.syncLanguage(this)
        enableEdgeToEdge()

        // "route" opens a particular screen: used by the widgets, the daily reminder, and when
        // taking screenshots. "demo" loads sample data and is only used for screenshots.
        if (intent?.getBooleanExtra("demo", false) == true) vm.loadDemo()
        val startRoute = if (savedInstanceState == null) intent?.getStringExtra("route") else null

        setContent {
            SehaTheme(vm.data.settings.palette) {
                AppRoot(vm = vm, startRoute = startRoute)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.checkHealth()
    }
}
