package ai.seha.app.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    coerceInputValues = true
    explicitNulls = false
}

/** Everything the user enters lives in one file on the phone. */
object Store {
    private const val FILE = "seha.json"
    private val lock = Any()

    fun load(context: Context): AppData = synchronized(lock) {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) return AppData()
        try {
            AppJson.decodeFromString<AppData>(file.readText())
        } catch (e: Exception) {
            // Keep the unreadable file aside instead of silently replacing it.
            try {
                file.copyTo(File(context.filesDir, "seha-unreadable.json"), overwrite = true)
            } catch (ignored: Exception) {
            }
            AppData()
        }
    }

    fun save(context: Context, data: AppData) = synchronized(lock) {
        val tmp = File(context.filesDir, "$FILE.tmp")
        tmp.writeText(AppJson.encodeToString(data))
        val target = File(context.filesDir, FILE)
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun export(data: AppData): String = AppJson.encodeToString(data)

    fun import(text: String): AppData = AppJson.decodeFromString<AppData>(text)
}

/** The AI keys are kept apart from the health data so a backup file never contains them. */
class Keys(context: Context) {
    private val store = context.applicationContext.getSharedPreferences("seha_keys", Context.MODE_PRIVATE)

    fun get(p: Provider): String = store.getString(p.name, "")?.trim() ?: ""

    fun set(p: Provider, key: String) {
        store.edit().putString(p.name, key.trim()).apply()
    }

    fun has(p: Provider): Boolean = get(p).isNotEmpty()
}

object Appearance {
    fun applyTheme(mode: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            },
        )
    }

    /** "" (follow the phone), "en" or "ar". */
    fun currentLanguage(): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return when {
            tags.startsWith("ar") -> "ar"
            tags.startsWith("en") -> "en"
            else -> ""
        }
    }

    fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }

    /** Whether this context shows Arabic. Also remembered for widgets and reminders. */
    fun syncLanguage(context: Context) {
        val arabic = context.resources.configuration.locales[0].language == "ar"
        Lang.ar = arabic
        context.applicationContext.getSharedPreferences("seha_ui", Context.MODE_PRIVATE)
            .edit().putBoolean("arabic", arabic).apply()
    }

    /** For code that runs without the app being open. */
    fun restoreLanguage(context: Context) {
        Lang.ar = context.applicationContext.getSharedPreferences("seha_ui", Context.MODE_PRIVATE)
            .getBoolean("arabic", false)
    }
}
