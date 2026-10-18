package t7helpers.quickalarms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val VISIBLE_ITEMS = 5
private val ITEM_HEIGHT = 44.dp

/** Copies of the labels in the list, so the wheel seems endless in both directions. */
private const val LAPS = 1000

/**
 * A wheel to pick one of [labels] by swiping up and down; it wraps around and stops with
 * an item in the middle. Tapping an item moves it to the middle. [onSelect] gets the
 * index of the middle item each time it changes.
 */
@Composable
fun Wheel(labels: List<String>, initial: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val count = labels.size
    val start = count * (LAPS / 2) + initial
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start - VISIBLE_ITEMS / 2)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val select by rememberUpdatedState(onSelect)
    val middle by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: start
        }
    }
    var reported by remember { mutableIntStateOf(initial) }
    LaunchedEffect(middle) {
        val index = middle % count
        if (index != reported) {
            reported = index
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            select(index)
        }
    }

    Box(modifier.height(ITEM_HEIGHT * VISIBLE_ITEMS), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(ITEM_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(count * LAPS) { i ->
                val selected = i == middle
                Text(
                    labels[i % count],
                    Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT)
                        .clickable { scope.launch { state.animateScrollToItem(i - VISIBLE_ITEMS / 2) } }
                        .wrapContentHeight(),
                    textAlign = TextAlign.Center,
                    style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                )
            }
        }
    }
}
