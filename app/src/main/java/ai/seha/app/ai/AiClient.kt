package ai.seha.app.ai

import ai.seha.app.data.Provider
import ai.seha.app.data.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiReply(val text: String, val sources: List<String>)

class AiException(message: String) : Exception(message)

/** One turn of a conversation. */
class Turn(val fromUser: Boolean, val text: String)

/**
 * Talks to the three AI services directly from the phone, each with the user's own key.
 * All three can search the web when [web] is on.
 */
object AiClient {

    suspend fun ask(
        provider: Provider,
        key: String,
        model: String,
        system: String,
        turns: List<Turn>,
        web: Boolean,
        maxTokens: Int = 4000,
    ): AiReply = withContext(Dispatchers.IO) {
        if (key.isBlank()) throw AiException(tr("Add your AI key in Settings first.", "أضف مفتاح الذكاء الاصطناعي من الإعدادات أولاً."))
        try {
            when (provider) {
                Provider.GEMINI -> gemini(key, model, system, turns, web, maxTokens)
                Provider.OPENAI -> openAi(key, model, system, turns, web)
                Provider.CLAUDE -> claude(key, model, system, turns, web, maxTokens)
            }
        } catch (e: AiException) {
            throw e
        } catch (e: java.net.UnknownHostException) {
            throw AiException(tr("No internet connection.", "لا يوجد اتصال بالإنترنت."))
        } catch (e: java.net.SocketTimeoutException) {
            throw AiException(tr("The AI took too long to answer. Try again.", "تأخر رد الذكاء الاصطناعي. حاول مرة أخرى."))
        } catch (e: Exception) {
            throw AiException(e.message ?: e.javaClass.simpleName)
        }
    }

    // ------------------------------------------------------------------ Google Gemini

    private fun gemini(key: String, model: String, system: String, turns: List<Turn>, web: Boolean, maxTokens: Int): AiReply {
        val contents = JSONArray()
        for (t in turns) {
            contents.put(
                JSONObject()
                    .put("role", if (t.fromUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", t.text))),
            )
        }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("maxOutputTokens", maxTokens * 2))
        if (web) body.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val json = post(url, mapOf("x-goog-api-key" to key), body)

        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw AiException(tr("Gemini returned no answer.", "لم يرجع Gemini أي إجابة."))
        val text = StringBuilder()
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.optBoolean("thought", false)) continue
                text.append(part.optString("text", ""))
            }
        }
        val sources = mutableListOf<String>()
        val chunks = candidate.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")
        if (chunks != null) {
            for (i in 0 until chunks.length()) {
                val webChunk = chunks.optJSONObject(i)?.optJSONObject("web") ?: continue
                val uri = webChunk.optString("uri", "")
                if (uri.isNotEmpty()) sources.add(label(webChunk.optString("title", ""), uri))
            }
        }
        if (text.isBlank()) throw AiException(tr("Gemini returned an empty answer.", "أرجع Gemini إجابة فارغة."))
        return AiReply(text.toString().trim(), sources.distinct().take(6))
    }

    // ------------------------------------------------------------------ OpenAI (GPT)

    private fun openAi(key: String, model: String, system: String, turns: List<Turn>, web: Boolean): AiReply {
        val input = JSONArray()
        for (t in turns) {
            input.put(JSONObject().put("role", if (t.fromUser) "user" else "assistant").put("content", t.text))
        }
        val body = JSONObject()
            .put("model", model)
            .put("instructions", system)
            .put("input", input)
        if (web) body.put("tools", JSONArray().put(JSONObject().put("type", "web_search")))

        val json = post("https://api.openai.com/v1/responses", mapOf("Authorization" to "Bearer $key"), body)

        val text = StringBuilder()
        val sources = mutableListOf<String>()
        val output = json.optJSONArray("output")
        if (output != null) {
            for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue
                if (item.optString("type") != "message") continue
                val content = item.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val block = content.optJSONObject(j) ?: continue
                    if (block.optString("type") != "output_text") continue
                    text.append(block.optString("text", ""))
                    val notes = block.optJSONArray("annotations") ?: continue
                    for (k in 0 until notes.length()) {
                        val note = notes.optJSONObject(k) ?: continue
                        val uri = note.optString("url", "")
                        if (uri.isNotEmpty()) sources.add(label(note.optString("title", ""), uri))
                    }
                }
            }
        }
        if (text.isBlank()) throw AiException(tr("GPT returned an empty answer.", "أرجع GPT إجابة فارغة."))
        return AiReply(text.toString().trim(), sources.distinct().take(6))
    }

    // ------------------------------------------------------------------ Anthropic Claude

    private fun claude(key: String, model: String, system: String, turns: List<Turn>, web: Boolean, maxTokens: Int): AiReply {
        val messages = JSONArray()
        for (t in turns) {
            messages.put(JSONObject().put("role", if (t.fromUser) "user" else "assistant").put("content", t.text))
        }
        val text = StringBuilder()
        val sources = mutableListOf<String>()
        // A long search can pause the turn; sending the paused answer back continues it.
        for (round in 0 until 4) {
            val body = JSONObject()
                .put("model", model)
                .put("max_tokens", maxTokens)
                .put("system", system)
                .put("messages", messages)
            if (web) {
                body.put(
                    "tools",
                    JSONArray().put(JSONObject().put("type", "web_search_20250305").put("name", "web_search").put("max_uses", 4)),
                )
            }
            val json = post(
                "https://api.anthropic.com/v1/messages",
                mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01"),
                body,
            )
            val content = json.optJSONArray("content") ?: JSONArray()
            for (i in 0 until content.length()) {
                val block = content.optJSONObject(i) ?: continue
                if (block.optString("type") != "text") continue
                text.append(block.optString("text", ""))
                val cites = block.optJSONArray("citations") ?: continue
                for (k in 0 until cites.length()) {
                    val cite = cites.optJSONObject(k) ?: continue
                    val uri = cite.optString("url", "")
                    if (uri.isNotEmpty()) sources.add(label(cite.optString("title", ""), uri))
                }
            }
            if (json.optString("stop_reason") != "pause_turn") break
            messages.put(JSONObject().put("role", "assistant").put("content", content))
        }
        if (text.isBlank()) throw AiException(tr("Claude returned an empty answer.", "أرجع Claude إجابة فارغة."))
        return AiReply(text.toString().trim(), sources.distinct().take(6))
    }

    // ------------------------------------------------------------------ shared

    /** A source is kept as "Title|https://..." so the screen can show the title and open the link. */
    private fun label(title: String, uri: String): String = (if (title.isBlank()) uri else title.replace("|", " ")) + "|" + uri

    private fun post(url: String, headers: Map<String, String>, body: JSONObject): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 240_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            for ((name, value) in headers) conn.setRequestProperty(name, value)
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw AiException(explain(code, raw))
            return JSONObject(raw)
        } finally {
            conn.disconnect()
        }
    }

    private fun explain(code: Int, raw: String): String {
        val detail = try {
            val json = JSONObject(raw)
            val error = json.optJSONObject("error")
            error?.optString("message", "") ?: json.optString("message", "")
        } catch (e: Exception) {
            ""
        }
        val reason = when (code) {
            400 -> tr("The AI service rejected the request. Check the model name in Settings.", "رفضت الخدمة الطلب. تحقق من اسم النموذج في الإعدادات.")
            401, 403 -> tr("The AI key was not accepted. Check it in Settings.", "لم يُقبل مفتاح الذكاء الاصطناعي. تحقق منه في الإعدادات.")
            404 -> tr("That model name was not found. Check it in Settings.", "اسم النموذج غير موجود. تحقق منه في الإعدادات.")
            429 -> tr("The AI service says you hit its usage limit. Wait a little, or check your plan.", "وصلت إلى حد الاستخدام لدى الخدمة. انتظر قليلاً أو تحقق من خطتك.")
            in 500..599 -> tr("The AI service is having trouble. Try again shortly.", "الخدمة تواجه مشكلة. حاول بعد قليل.")
            else -> tr("The AI service returned an error.", "أرجعت الخدمة خطأ.")
        }
        return if (detail.isBlank()) "$reason ($code)" else "$reason ($code)\n${detail.take(300)}"
    }
}
