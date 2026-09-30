package il.hiya.shabbatwatch.clock

import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Text
import il.hiya.shabbatwatch.Constants
import il.hiya.shabbatwatch.R
import il.hiya.shabbatwatch.mode.LocationStore
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val INTERACTIVE_COLOR = Color.White
private val AMBIENT_COLOR = Color(0xFFBDBDBD)
private val TIME_SIZE = 60.sp
private val CANDLES_SIZE = 22.sp
private val CANDLES_ICON_WIDTH = 44.dp
private val CANDLES_ICON_HEIGHT = 22.dp
private val LINE_SIZE = 16.sp
private const val SHIFT_PERIOD_MINUTES = 3
private const val SECONDS_PER_MINUTE = 60L

/** U+1F56F CANDLE + VS16 (emoji presentation). Probed alone: hasGlyph() answers for one glyph. */
private const val SINGLE_CANDLE = "🕯️"

/**
 * Candles, time, Hebrew date, parsha/holiday and the next candle-lighting / end time.
 * Identical layout in interactive and ambient mode so the only visible change on wake is
 * brightness. In ambient the block shifts a pixel or two each minute against burn-in.
 */
@Composable
fun ClockScreen(isAmbient: Boolean, ambientTick: Int) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(isAmbient, ambientTick) {
        if (isAmbient) {
            now = ZonedDateTime.now()
        } else {
            while (true) {
                now = ZonedDateTime.now()
                delay(Constants.CLOCK_TICK_MILLIS)
            }
        }
    }
    val calendar = rememberShabbatCalendar()
    val minuteKey = now.toEpochSecond() / SECONDS_PER_MINUTE
    val info = remember(minuteKey, calendar) { calendar.infoFor(now) }
    val shift = burnInShiftDp(now.minute, isAmbient)
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        ClockContent(now, info, isAmbient, Modifier.offset(x = shift.dp, y = shift.dp))
    }
}

/** Calendar for the city and candle-lighting custom chosen on the home screen. */
@Composable
private fun rememberShabbatCalendar(): ShabbatCalendar {
    val context = LocalContext.current
    val store = remember { LocationStore(context) }
    val location by store.location.collectAsStateWithLifecycle(initialValue = Constants.LOCATION)
    return remember(location) { ShabbatCalendar(ShabbatTimes(location)) }
}

@Composable
private fun ClockContent(now: ZonedDateTime, info: ShabbatInfo, isAmbient: Boolean, modifier: Modifier) {
    val color = if (isAmbient) AMBIENT_COLOR else INTERACTIVE_COLOR
    val weight = if (isAmbient) FontWeight.Light else FontWeight.Normal
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Candles(color)
        Text(text = now.format(TIME_FORMAT), color = color, fontSize = TIME_SIZE, fontWeight = weight)
        Text(text = info.hebrewDate.format(), color = color, fontSize = LINE_SIZE)
        info.title?.let { Text(text = titleText(it), color = color, fontSize = LINE_SIZE) }
        info.zman?.let { Text(text = zmanText(it), color = color, fontSize = LINE_SIZE) }
    }
}

/** Emoji when the system font has the glyph; otherwise a plain white vector. */
@Composable
private fun Candles(color: Color) {
    val hasEmoji = remember { Paint().hasGlyph(SINGLE_CANDLE) }
    if (hasEmoji) {
        Text(text = stringResource(R.string.candles_emoji), fontSize = CANDLES_SIZE)
    } else {
        Image(
            painter = painterResource(R.drawable.ic_candles),
            contentDescription = stringResource(R.string.candles_content_description),
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier.size(CANDLES_ICON_WIDTH, CANDLES_ICON_HEIGHT),
        )
    }
}

@Composable
private fun titleText(title: DayTitle): String {
    val base = when (title.kind) {
        TitleKind.PARSHA -> stringResource(R.string.parsha_format, title.name)
        TitleKind.HOLIDAY -> title.name
    }
    val special = title.special ?: return base
    return base + stringResource(R.string.title_separator) + special
}

@Composable
private fun zmanText(zman: ZmanLine): String {
    val time = zman.time.format(TIME_FORMAT)
    return when (zman.kind) {
        ZmanKind.CANDLE_LIGHTING -> stringResource(R.string.zman_candle_lighting, time)
        ZmanKind.SHABBAT_ENDS -> stringResource(R.string.zman_shabbat_ends, time)
        ZmanKind.HOLIDAY_ENDS -> stringResource(R.string.zman_holiday_ends, time)
    }
}

/** Cycles -shift, 0, +shift over three minutes; no shift while interactive. */
internal fun burnInShiftDp(minute: Int, isAmbient: Boolean): Int {
    if (!isAmbient) return 0
    return (minute % SHIFT_PERIOD_MINUTES - 1) * Constants.BURN_IN_SHIFT_DP
}
