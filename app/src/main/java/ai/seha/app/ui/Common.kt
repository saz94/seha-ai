package ai.seha.app.ui

import ai.seha.app.data.tr
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A full screen with a back arrow and a scrolling body. */
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = tr("Back", "رجوع"))
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = bottom,
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/** The plain white (or dark) sheet most content sits on. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.medium,
        color = color,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 6.dp))
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((value, label) in options) {
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
fun <T> MultiChoice(options: List<Pair<T, String>>, selected: Set<T>, onToggle: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((value, label) in options) {
            FilterChip(
                selected = value in selected,
                onClick = { onToggle(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, suffix: String = "", decimal: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.replace(',', '.').filter { it.isDigit() || (decimal && it == '.') }.take(6)) },
        label = { Text(label) },
        suffix = if (suffix.isNotEmpty()) ({ Text(suffix) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    )
}

@Composable
fun TextBox(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", lines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = MaterialTheme.colorScheme.outline) }) else null,
        singleLine = lines == 1,
        minLines = lines,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun MainButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, busy: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

/** Five dots, from low to high. */
@Composable
fun Scale5(value: Int, onChange: (Int) -> Unit, low: String, high: String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 1..5) {
                val on = i == value
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onChange(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$i",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Hint(low)
            Hint(high)
        }
    }
}

/** Minus, the figure, plus. */
@Composable
fun Stepper(text: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(onMinus) { Icon(Icons.Outlined.Remove, contentDescription = tr("Less", "أقل")) }
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.tabular(),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(92.dp),
        )
        RoundIcon(onPlus) { Icon(Icons.Outlined.Add, contentDescription = tr("More", "أكثر")) }
    }
}

@Composable
private fun RoundIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * The app's signature: a three-quarter ring marked like a tailor's tape, filled as far as [progress].
 * Drawn the same way round in Arabic and English, as a dial would be.
 */
@Composable
fun TapeGauge(progress: Float, track: Color, fill: Color, tick: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.085f
            val inset = stroke / 2 + size.minDimension * 0.07f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(track, 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            val sweep = 270f * progress.coerceIn(0f, 1f)
            if (sweep > 0.5f) drawArc(fill, 135f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // tape marks outside the ring: a long one every fifth
            val centre = Offset(size.width / 2, size.height / 2)
            val outer = size.minDimension / 2
            for (i in 0..45) {
                val angle = (135.0 + i * 6.0) * PI / 180.0
                val long = i % 5 == 0
                val from = outer - size.minDimension * (if (long) 0.05f else 0.028f)
                drawLine(
                    color = tick,
                    start = Offset(centre.x + (from * cos(angle)).toFloat(), centre.y + (from * sin(angle)).toFloat()),
                    end = Offset(centre.x + (outer * cos(angle)).toFloat(), centre.y + (outer * sin(angle)).toFloat()),
                    strokeWidth = if (long) 2.2f * density else 1.2f * density,
                    cap = StrokeCap.Round,
                )
            }
        }
        content()
    }
}

/** A thin bar with a label and "have / target". */
@Composable
fun MacroBar(label: String, value: Int, target: Int, unit: String, color: Color, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val soft = if (onDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    val strong = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = soft, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (onDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(if (target <= 0) 0f else (value.toFloat() / target).coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("$value / $target $unit", style = MaterialTheme.typography.labelMedium.tabular(), color = strong, maxLines = 1)
    }
}

/** A line over time with an optional dashed goal line. Time always runs left to right. */
@Composable
fun LineChart(values: List<Double>, firstLabel: String, lastLabel: String, modifier: Modifier = Modifier, goal: Double? = null, unit: String = "") {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val goalColor = MaterialTheme.colorScheme.secondary
    val all = values + listOfNotNull(goal)
    val low = (all.minOrNull() ?: 0.0)
    val high = (all.maxOrNull() ?: 1.0)
    val pad = ((high - low) * 0.15).coerceAtLeast(0.5)
    val min = low - pad
    val max = high + pad
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier) {
            Row(Modifier.fillMaxWidth().height(150.dp)) {
                Column(Modifier.width(44.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(trimmed(high), style = MaterialTheme.typography.labelSmall.tabular(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (unit.isNotEmpty()) Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(trimmed(low), style = MaterialTheme.typography.labelSmall.tabular(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Canvas(Modifier.weight(1f).fillMaxSize()) {
                    fun y(v: Double): Float = (size.height * (1 - (v - min) / (max - min))).toFloat()
                    drawLine(grid, Offset(0f, y(high)), Offset(size.width, y(high)), 1f * density)
                    drawLine(grid, Offset(0f, y(low)), Offset(size.width, y(low)), 1f * density)
                    if (goal != null) {
                        drawLine(
                            goalColor, Offset(0f, y(goal)), Offset(size.width, y(goal)), 1.5f * density,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 6f * density)),
                        )
                    }
                    if (values.size == 1) {
                        drawCircle(line, 5f * density, Offset(size.width / 2, y(values[0])))
                    } else if (values.size > 1) {
                        val step = size.width / (values.size - 1)
                        val path = Path()
                        values.forEachIndexed { i, v ->
                            if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * step, y(v))
                        }
                        drawPath(path, line, style = Stroke(3f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawCircle(line, 5f * density, Offset(size.width, y(values.last())))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(start = 44.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(firstLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(lastLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun trimmed(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}

/** Small bars, one per day; the last one is today. */
@Composable
fun DayBars(values: List<Float>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val rest = MaterialTheme.colorScheme.surfaceContainerHighest
    val top = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Canvas(modifier.fillMaxWidth().height(44.dp)) {
        if (values.isEmpty()) return@Canvas
        val gap = 6f * density
        val w = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = (size.height * (v / top)).coerceAtLeast(3f * density)
            drawRoundRect(
                color = if (v <= 0f) rest else color,
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * density),
            )
        }
    }
}

@Composable
fun BottomBar(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp), content = content)
    }
}
