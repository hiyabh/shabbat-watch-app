package il.hiya.shabbatwatch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import il.hiya.shabbatwatch.Constants
import il.hiya.shabbatwatch.R
import il.hiya.shabbatwatch.ShabbatLocation

@Composable
fun CityPickerScreen(current: ShabbatLocation, onPick: (ShabbatLocation) -> Unit, onBack: () -> Unit) {
    PickerScreen(
        title = stringResource(R.string.picker_city_title),
        options = Constants.CITIES,
        label = { it.name },
        isSelected = { it.name == current.name },
        onPick = onPick,
        onBack = onBack,
    )
}

@Composable
fun CandleMinutesPickerScreen(current: ShabbatLocation, onPick: (Int) -> Unit, onBack: () -> Unit) {
    PickerScreen(
        title = stringResource(R.string.picker_candle_title),
        options = Constants.CANDLE_MINUTES_OPTIONS,
        label = { stringResource(R.string.candle_minutes_format, it) },
        isSelected = { it == current.candleLightingMinutes },
        onPick = onPick,
        onBack = onBack,
    )
}

/** Single-choice list; picking an option applies it and returns to the home screen. */
@Composable
private fun <T> PickerScreen(
    title: String,
    options: List<T>,
    label: @Composable (T) -> String,
    isSelected: (T) -> Boolean,
    onPick: (T) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val selectedIndex = options.indexOfFirst(isSelected).coerceAtLeast(0)
    // +1: the title occupies the first list slot.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = selectedIndex + 1)
    Scaffold {
        ScalingLazyColumn(state = listState) {
            item { PickerTitle(title) }
            items(options) { option ->
                val checked = isSelected(option)
                ToggleChip(
                    checked = checked,
                    onCheckedChange = { onPick(option) },
                    label = { Text(label(option)) },
                    toggleControl = { RadioButton(selected = checked) },
                    colors = ToggleChipDefaults.toggleChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PickerTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.title3,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
