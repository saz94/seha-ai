package ai.seha.app.ui

import ai.seha.app.data.ChatMsg
import ai.seha.app.data.tr
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CoachTab(vm: AppViewModel, nav: Nav) {
    val chat = vm.data.chat
    val scroll = rememberScrollState()
    var input by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(chat.size, vm.chatBusy) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Orb(Modifier.size(56.dp), mood = if (vm.chatBusy) "thinking" else "idle", glow = false)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Coach", "المدرب"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (vm.hasKey) tr("Answers from ${providerLabel(vm.data.settings.provider)}, using your profile and last 7 days", "إجابات من ${providerLabel(vm.data.settings.provider)} بناءً على ملفك وآخر 7 أيام")
                    else tr("Needs an AI key", "يحتاج إلى مفتاح ذكاء اصطناعي"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (chat.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) { Icon(Icons.Outlined.DeleteSweep, contentDescription = tr("Clear conversation", "مسح المحادثة")) }
            }
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!vm.hasKey) {
                Panel {
                    Orb(Modifier.size(150.dp).align(Alignment.CenterHorizontally), mood = "happy")
                    Text(tr("Connect your coach", "اربط مدربك"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Hint(tr("The coach runs on GPT, Gemini or Claude with your own key. Add one and it can answer questions, review your week and adjust to your medical history.", "يعمل المدرب عبر GPT أو Gemini أو Claude بمفتاحك الخاص. أضف مفتاحاً ليجيب عن أسئلتك ويراجع أسبوعك ويراعي تاريخك الصحي."))
                    Spacer(Modifier.height(12.dp))
                    MainButton(tr("Add an AI key", "أضف مفتاح الذكاء الاصطناعي"), { nav.open("ai") })
                }
            } else if (chat.isEmpty()) {
                Orb(Modifier.size(170.dp).align(Alignment.CenterHorizontally), mood = "happy")
                Text(tr("Hello! Ask me anything", "مرحباً! اسألني أي شيء"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                Hint(tr("About training, food or your progress. Try one of these:", "عن التمرين أو الأكل أو تقدّمك. جرّب أحد هذه:"))
                val ideas = listOf(
                    tr("What should I train today?", "ماذا أتمرّن اليوم؟"),
                    tr("Review my week and tell me what to change", "راجع أسبوعي وأخبرني ماذا أغيّر"),
                    tr("I get hungry at night. What can I do?", "أجوع في الليل. ماذا أفعل؟"),
                    tr("Give me a 30 minute home workout", "أعطني تمريناً منزلياً لمدة 30 دقيقة"),
                )
                for (idea in ideas) {
                    Panel(onClick = { vm.sendChat(idea) }, padding = 14.dp) { Text(idea, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            for (msg in chat) Bubble(msg)
            if (vm.chatBusy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Orb(Modifier.size(36.dp), mood = "thinking", glow = false)
                    Spacer(Modifier.width(8.dp))
                    Hint(tr("Thinking…", "يفكّر…"))
                }
            }
            vm.chatError?.let { error ->
                Panel(color = MaterialTheme.colorScheme.errorContainer, padding = 12.dp) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        if (vm.hasKey) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(tr("Message your coach", "اكتب لمدربك")) },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                val canSend = input.isNotBlank() && !vm.chatBusy
                Box(
                    Modifier
                        .padding(bottom = 4.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(enabled = canSend) {
                            vm.sendChat(input)
                            input = ""
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send, contentDescription = tr("Send", "إرسال"),
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(tr("Clear this conversation?", "مسح هذه المحادثة؟")) },
            text = { Text(tr("Your check-ins and plans are not affected.", "لن تتأثر تسجيلاتك وخططك.")) },
            confirmButton = { TextButton(onClick = { vm.clearChat(); confirmClear = false }) { Text(tr("Clear conversation", "مسح المحادثة")) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(tr("Cancel", "إلغاء")) } },
        )
    }
}

@Composable
private fun Bubble(msg: ChatMsg) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!msg.fromUser) {
            OrbAvatar(Modifier.size(30.dp))
            Spacer(Modifier.width(6.dp))
        }
        Column(
            Modifier
                .widthIn(max = 320.dp)
                .clip(
                    if (msg.fromUser) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
                )
                .background(if (msg.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            SelectionContainer {
                Text(
                    msg.text.replace("**", ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (msg.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
            for (source in msg.sources.take(4)) {
                val title = source.substringBeforeLast("|")
                val url = source.substringAfterLast("|")
                Spacer(Modifier.height(6.dp))
                Text(
                    title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { openLink(context, url) },
                )
            }
        }
    }
}
