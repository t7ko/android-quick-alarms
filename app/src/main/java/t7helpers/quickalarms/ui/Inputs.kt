package t7helpers.quickalarms.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import t7helpers.quickalarms.AlarmTimes
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** A number with minus and plus buttons. */
@Composable
fun Stepper(
    label: String,
    value: Int,
    range: IntRange,
    format: (Int) -> String = { it.toString() },
    onChange: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = value > range.first) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            format(value),
            Modifier.widthIn(min = 64.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = value < range.last) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Minutes on the wheel go in steps of this size; typing allows any minute. */
private const val WHEEL_MINUTE_STEP = 5

/**
 * A dialog to pick a time: scroll wheels at first, or typing after "Type". [buttons] read
 * the time with the function they get.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    extra: @Composable () -> Unit = {},
    buttons: @Composable RowScope.(picked: () -> LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    var typing by remember { mutableStateOf(false) }
    var wheelTime by remember { mutableStateOf(AlarmTimes.roundToStep(initial, WHEEL_MINUTE_STEP)) }
    // A fresh input each time typing starts, showing what the wheels had.
    val input = key(typing) { rememberTimePickerState(wheelTime.hour, wheelTime.minute, is24) }
    val typed = { LocalTime.of(input.hour, input.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f))
                TextButton(onClick = {
                    if (typing) wheelTime = AlarmTimes.roundToStep(typed(), WHEEL_MINUTE_STEP)
                    typing = !typing
                }) { Text(if (typing) "Wheel" else "Type") }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (typing) {
                    TimeInput(input)
                } else {
                    TimeWheels(wheelTime, is24) { wheelTime = it }
                }
                extra()
            }
        },
        confirmButton = { Row { buttons { if (typing) typed() else wheelTime } } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TimeWheels(time: LocalTime, is24: Boolean, onChange: (LocalTime) -> Unit) {
    val hours = remember(is24) {
        val format = DateTimeFormatter.ofPattern(if (is24) "HH" else "h a")
        (0..23).map { LocalTime.of(it, 0).format(format) }
    }
    val minutes = remember { (0 until 60 step WHEEL_MINUTE_STEP).map { it.toString().padStart(2, '0') } }
    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Wheel(hours, time.hour, { onChange(time.withHour(it)) }, Modifier.width(if (is24) 80.dp else 112.dp))
        Text(":", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.headlineMedium)
        Wheel(minutes, time.minute / WHEEL_MINUTE_STEP, { onChange(time.withMinute(it * WHEEL_MINUTE_STEP)) }, Modifier.width(80.dp))
    }
}
