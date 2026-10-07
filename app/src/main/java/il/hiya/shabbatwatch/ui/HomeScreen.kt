package il.hiya.shabbatwatch.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import il.hiya.shabbatwatch.Constants
import il.hiya.shabbatwatch.R
import il.hiya.shabbatwatch.ShabbatDuration
import il.hiya.shabbatwatch.ShabbatLocation
import il.hiya.shabbatwatch.clock.ShabbatClockActivity
import il.hiya.shabbatwatch.mode.LocationStore
import il.hiya.shabbatwatch.mode.PermissionStatus
import il.hiya.shabbatwatch.mode.ShabbatModeController
import il.hiya.shabbatwatch.mode.ShabbatState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val END_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE HH:mm", Locale.forLanguageTag("he"))

private enum class HomePage { MAIN, CITY, CANDLE_MINUTES }

@Composable
fun HomeScreen(controller: ShabbatModeController) {
    val context = LocalContext.current
    val locationStore = remember { LocationStore(context) }
    val location by locationStore.location.collectAsStateWithLifecycle(initialValue = Constants.LOCATION)
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(HomePage.MAIN) }
    val backToMain = { page = HomePage.MAIN }
    when (page) {
        HomePage.MAIN -> MainPage(controller, location) { page = it }
        HomePage.CITY -> CityPickerScreen(
            current = location,
            onPick = { scope.launch { locationStore.setCity(it) }; backToMain() },
            onBack = backToMain,
        )
        HomePage.CANDLE_MINUTES -> CandleMinutesPickerScreen(
            current = location,
            onPick = { scope.launch { locationStore.setCandleMinutes(it) }; backToMain() },
            onBack = backToMain,
        )
    }
}

@Composable
private fun MainPage(controller: ShabbatModeController, location: ShabbatLocation, onNavigate: (HomePage) -> Unit) {
    val state by controller.state.collectAsStateWithLifecycle(initialValue = ShabbatState())
    val permissions = remember { controller.permissionStatus() }
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = { TimeText() }) {
        ScalingLazyColumn(state = listState) {
            item { Title() }
            item { PermissionWarnings(permissions) }
            if (state.active) {
                activeItems(state, controller)
            } else {
                inactiveItems(controller)
                item { LocationChips(location, onNavigate) }
            }
        }
    }
}

@Composable
private fun Title() {
    Text(
        text = stringResource(R.string.home_title),
        style = MaterialTheme.typography.title2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun androidx.wear.compose.foundation.lazy.ScalingLazyListScope.activeItems(
    state: ShabbatState,
    controller: ShabbatModeController,
) {
    item { ActiveStatus(state.endAtMillis) }
    item { OpenClockChip() }
    item { DeactivateChip(controller) }
}

private fun androidx.wear.compose.foundation.lazy.ScalingLazyListScope.inactiveItems(
    controller: ShabbatModeController,
) {
    item { DurationPicker(controller) }
}

@Composable
private fun ActiveStatus(endAtMillis: Long) {
    val endText = Instant.ofEpochMilli(endAtMillis).atZone(ZoneId.systemDefault()).format(END_TIME_FORMAT)
    Text(
        text = stringResource(R.string.status_active_until, endText),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OpenClockChip() {
    val context = LocalContext.current
    Chip(
        onClick = { ShabbatClockActivity.launch(context) },
        label = { Text(stringResource(R.string.action_open_clock)) },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DeactivateChip(controller: ShabbatModeController) {
    Chip(
        onClick = { controller.deactivateAsync() },
        label = { Text(stringResource(R.string.action_deactivate)) },
        colors = ChipDefaults.primaryChipColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Duration radio list followed by the activate button. Kept in one composable to share state. */
@Composable
private fun DurationPicker(controller: ShabbatModeController) {
    var selected by remember { mutableStateOf(ShabbatDuration.SHABBAT) }
    androidx.compose.foundation.layout.Column {
        ShabbatDuration.entries.forEach { duration ->
            DurationChip(duration, selected == duration) { selected = duration }
        }
        Chip(
            onClick = { controller.activateAsync(selected) },
            label = { Text(stringResource(R.string.action_activate)) },
            colors = ChipDefaults.primaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DurationChip(duration: ShabbatDuration, checked: Boolean, onSelect: () -> Unit) {
    ToggleChip(
        checked = checked,
        onCheckedChange = { onSelect() },
        label = { Text(stringResource(duration.labelRes())) },
        toggleControl = { RadioButton(selected = checked) },
        colors = ToggleChipDefaults.toggleChipColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** City and candle-lighting custom; each opens its picker. */
@Composable
private fun LocationChips(location: ShabbatLocation, onNavigate: (HomePage) -> Unit) {
    androidx.compose.foundation.layout.Column {
        Chip(
            onClick = { onNavigate(HomePage.CITY) },
            label = { Text(stringResource(R.string.setting_city, location.name)) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Chip(
            onClick = { onNavigate(HomePage.CANDLE_MINUTES) },
            label = { Text(stringResource(R.string.setting_candle_minutes, location.candleLightingMinutes)) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PermissionWarnings(permissions: PermissionStatus) {
    if (permissions.allGranted) return
    val lines = buildList {
        add(stringResource(R.string.warn_missing_permissions))
        if (!permissions.secureSettings) add(stringResource(R.string.warn_secure_settings))
        if (!permissions.dnd) add(stringResource(R.string.warn_dnd))
        if (!permissions.overlay) add(stringResource(R.string.warn_overlay))
        if (!permissions.batteryUnrestricted) add(stringResource(R.string.warn_battery))
        add(Constants.GUIDE_URL)
    }
    Text(
        text = lines.joinToString("\n"),
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.caption2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun ShabbatDuration.labelRes(): Int = when (this) {
    ShabbatDuration.SHABBAT -> R.string.duration_shabbat
    ShabbatDuration.SHABBAT_AND_YOM_TOV -> R.string.duration_shabbat_yomtov
    ShabbatDuration.THREE_DAYS -> R.string.duration_three_days
}
