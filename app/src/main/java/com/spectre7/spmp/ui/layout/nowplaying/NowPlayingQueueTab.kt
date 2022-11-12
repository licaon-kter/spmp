package com.spectre7.spmp.ui.layout.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.android.exoplayer2.C
import com.spectre7.spmp.PlayerHost
import com.spectre7.spmp.model.Song
import com.spectre7.utils.getContrasted
import com.spectre7.utils.vibrate
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import kotlin.math.roundToInt
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.ui.component.PillMenu

@Composable
fun QueueTab(weight_modifier: Modifier) {

    var key_inc by remember { mutableStateOf(0) }
    val v_removed = remember { mutableStateListOf<Int>() }
    val undo_list = remember { mutableStateListOf<() -> Unit>() }

    data class Item(val song: Song, val key: Int) {

        @OptIn(ExperimentalMaterialApi::class)
        @Composable
        fun QueueElement(handle_modifier: Modifier, current: Boolean, index: Int, colour: Color, on_remove_request: () -> Unit) {

            val swipe_state = rememberSwipeableState(1)
            val max_offset = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
            val anchors = mapOf(-max_offset to 0, 0f to 1, max_offset to 2)

            LaunchedEffect(swipe_state.currentValue) {
                if (swipe_state.currentValue != 1) {
                    on_remove_request()
                }
            }

            Box((if (current) Modifier.background(colour, RoundedCornerShape(45)) else Modifier).offset {
                IntOffset(swipe_state.offset.value.roundToInt(), 0)
            }) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp, end = 20.dp)
                ) {
                    val content_colour = if (current) colour.getContrasted() else colour
                    song.PreviewBasic(
                        false,
                        Modifier
                            .weight(1f)
                            .clickable {
                                PlayerHost.player.seekTo(index, C.TIME_UNSET)
                            }
                            .swipeable(
                                swipe_state,
                                anchors,
                                Orientation.Horizontal,
                                thresholds = { _, _ -> FractionalThreshold(0.2f) }
                            ),
                        content_colour
                    )

                    // Drag handle
                    Image(rememberVectorPainter(Icons.Filled.Menu), "", modifier = handle_modifier
                        .requiredSize(25.dp),
                        colorFilter = ColorFilter.tint(content_colour)
                    )
                }
            }
        }
    }

    var song_items by remember { mutableStateOf(
        List(PlayerHost.status.m_queue.size) {
            Item(PlayerHost.status.m_queue[it], key_inc++)
        }
    ) }

    val queue_listener = remember {
        object : PlayerHost.PlayerQueueListener {
            override fun onSongAdded(song: Song, index: Int) {
                song_items = song_items.toMutableList().apply {
                    add(index, Item(song, key_inc++))
                }
            }
            override fun onSongRemoved(song: Song, index: Int) {
                val i = v_removed.indexOf(index)
                if (i != -1) {
                    v_removed.removeAt(i)
                }
                else {
                    song_items = song_items.toMutableList().apply {
                        removeAt(index)
                    }
                }
            }
            override fun onCleared() {
                song_items = emptyList()
            }
        }
    }

    var playing_key by remember { mutableStateOf<Int?>(null) }

    // TODO
    // LaunchedEffect(p_status.index) {
    //     playing_key =
    // }

    DisposableEffect(Unit) {
        PlayerHost.service.addQueueListener(queue_listener)
        onDispose {
            PlayerHost.service.removeQueueListener(queue_listener)
        }
    }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            song_items = song_items.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { from, to ->
            if (from != to) {
                PlayerHost.player.moveMediaItem(from, to)
                playing_key = null
            }
        }
    )

    Box(Modifier.fillMaxSize()) {

        LazyColumn(
            state = state.listState,
            modifier = weight_modifier
                .reorderable(state)
                .align(Alignment.TopCenter)
        ) {
            items(song_items.size, { song_items[it].key }) { index ->
                val item = song_items[index]
                ReorderableItem(state, item.key) { is_dragging ->
                    LaunchedEffect(is_dragging) {
                        if (is_dragging) {
                            vibrate(0.01)
                            playing_key = song_items[PlayerHost.status.index].key
                        }
                    }

                    val current = if (playing_key != null) playing_key == item.key else PlayerHost.status.m_index == index
                    item.QueueElement(Modifier.detectReorder(state), current, index, MainActivity.theme.getOnBackground(true)) {
                        v_removed.add(index)

                        val song = song_items[index].song
                        undo_list.add({
                            PlayerHost.service.addToQueue(song, index)
                        })

                        song_items = song_items.toMutableList().apply {
                            removeAt(index)
                        }
                        PlayerHost.service.removeFromQueue(index)
                    }
                }
            }
        }

        Row(Modifier.align(Alignment.BottomCenter).padding(10.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val removed: List<Pair<Song, Int>> = PlayerHost.service.clearQueue(PlayerHost.status.m_queue.size > 1)
                        undo_list.add {
//                            val before = mutableListOf<Song>()
//
//                            val current = PlayerHost.service.getCurrentSong()
//                            if (current != null) {
//
//                            }

                            for (song in removed) {
                                PlayerHost.service.addToQueue(song.first, song.second)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MainActivity.theme.getOnBackground(true)
                    )
                ) {
                    Text(
                        text = "Clear",
                        color = MainActivity.theme.getOnBackground(true).getContrasted()
                    )
                }

                Button(
                    onClick = {
                        // TODO
        //                PlayerHost.service.
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MainActivity.theme.getOnBackground(true)
                    )
                ) {
                    Text(
                        text = "Shuffle",
                        color = MainActivity.theme.getOnBackground(true).getContrasted()
                    )
                }

            }

            AnimatedVisibility(undo_list.isNotEmpty()) {
                IconButton({
                    undo_list.removeLast()()
                }, Modifier.background(MainActivity.theme.getOnBackground(true), CircleShape).size(40.dp)) {
                    Icon(Icons.Filled.Undo, null, tint = MainActivity.theme.getOnBackground(true).getContrasted())
                }
            }
        }
    }
}