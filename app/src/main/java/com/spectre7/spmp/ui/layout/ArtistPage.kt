@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectre7.spmp.ui.layout

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.R
import com.spectre7.spmp.model.Artist
import com.spectre7.spmp.model.MediaItem
import com.spectre7.spmp.ui.component.MediaItemGrid
import com.spectre7.spmp.ui.component.MediaItemLayout
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.utils.*
import kotlinx.coroutines.*
import java.util.regex.Pattern
import kotlin.concurrent.thread

@Composable
fun ArtistPage(
    pill_menu: PillMenu,
    artist: Artist,
    player: PlayerViewContext,
    close: () -> Unit
) {
    var show_info by remember { mutableStateOf(false) }

    val share_intent = remember(artist.url, artist.name) {
        Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, artist.name)
            putExtra(Intent.EXTRA_TEXT, artist.url)
            type = "text/plain"
        }, null)
    }
    val open_intent: Intent? = remember(artist.url) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(artist.url))
        if (intent.resolveActivity(MainActivity.context.packageManager) == null) {
            null
        }
        else {
            intent
        }
    }

    val gradient_size = 0.35f
    val background_colour = MainActivity.theme.getBackground(false)
    var accent_colour by remember { mutableStateOf(Color.Unspecified) }

    var artist_rows_loaded: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(artist.id) {
        artist_rows_loaded = false

        thread {
            runBlocking {
                withContext(Dispatchers.IO) { coroutineScope {
                    for (row in artist.feed_rows) {
                        for (item in row.items.withIndex()) {
                            launch {
                                val new_item = item.value.loadData()
                                if (new_item != item.value) {
                                    synchronized(row.items) {
                                        row.items[item.index] = new_item
                                    }
                                }
                            }
                        }
                    }
                }}

                for (row in artist.feed_rows) {
                    row.items.removeAll {
                        if (!it.is_valid) {
                            println("REMOVE ${it.id} | ${it.type}")
                        }
                        !it.is_valid
                    }
                }

                artist_rows_loaded = true
            }
        }
    }

    LaunchedEffect(accent_colour) {
        if (!accent_colour.isUnspecified) {
            pill_menu.setBackgroundColourOverride(accent_colour)
        }
    }

    BackHandler(onBack = close)

    if (show_info) {
        InfoDialog(artist) { show_info = false }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {

        // Artist image
        Crossfade(artist.getThumbnail(MediaItem.ThumbnailQuality.HIGH)) { thumbnail ->
            if (thumbnail != null) {
                if (accent_colour.isUnspecified) {
                    accent_colour = MediaItem.getDefaultPaletteColour(artist.thumbnail_palette!!, MainActivity.theme.getAccent())
                }

                Image(
                    thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(
                            Brush.verticalGradient(
                                0f to background_colour,
                                gradient_size to Color.Transparent
                            )
                        )
                )
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {

            // Image spacing
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                        .background(
                            Brush.verticalGradient(
                                1f - gradient_size to Color.Transparent,
                                1f to background_colour
                            )
                        )
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Marquee(false) {
                        Text(artist.name, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 40.sp, softWrap = false)
                    }
                }
            }

            val content_padding = 10.dp

            // Action bar
            item {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .background(background_colour),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = content_padding)
                ) {

                    fun chip(text: String, icon: ImageVector, onClick: () -> Unit) {
                        item {
                            ElevatedAssistChip(
                                onClick,
                                { Text(text, style = MaterialTheme.typography.labelLarge) },
                                leadingIcon = {
                                    Icon(icon, null, tint = accent_colour)
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = background_colour,
                                    labelColor = MainActivity.theme.getOnBackground(false),
                                    leadingIconContentColor = accent_colour
                                )
                            )
                        }
                    }

                    chip(getString(R.string.artist_chip_shuffle), Icons.Outlined.Shuffle) { TODO() }
                    chip(getString(R.string.action_share), Icons.Outlined.Share) { MainActivity.context.startActivity(share_intent) }
                    chip(getString(R.string.artist_chip_open), Icons.Outlined.OpenInNew) { MainActivity.context.startActivity(open_intent) }
                    chip(getString(R.string.artist_chip_details), Icons.Outlined.Info) { show_info = !show_info }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().background(background_colour).padding(start = 20.dp, bottom = 10.dp)) {
                    @Composable
                    fun Btn(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
                        OutlinedButton(onClick = onClick, modifier.height(45.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(icon, null, tint = accent_colour)
                                Text(text, softWrap = false, color = MainActivity.theme.getOnBackground(false))
                            }
                        }
                    }

                    Btn(getString(R.string.artist_chip_play), Icons.Outlined.PlayArrow, Modifier.fillMaxWidth(0.5f).weight(1f)) { TODO() }
                    Spacer(Modifier.requiredWidth(20.dp))
                    Btn(getString(R.string.artist_chip_radio), Icons.Outlined.Radio, Modifier.fillMaxWidth(1f).weight(1f)) { TODO() }

                    Crossfade(artist.subscribed) { subscribed ->
                        if (subscribed == null) {
                            Spacer(Modifier.requiredWidth(20.dp))
                        }
                        else {
                            Row {
                                Spacer(Modifier.requiredWidth(10.dp))
                                OutlinedIconButton(
                                    {
                                        artist.toggleSubscribe(
                                            toggle_before_fetch = true,
                                            notify_failure = true
                                        )
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (subscribed) accent_colour else background_colour,
                                        contentColor = if (subscribed) accent_colour.getContrasted() else MainActivity.theme.getOnBackground(false)
                                    )
                                ) {
                                    Icon(if (subscribed) Icons.Outlined.Person else Icons.Outlined.PersonAdd, null)
                                }
                            }
                        }
                    }
                }
            }

            // Loaded items
            item {
                Crossfade(artist_rows_loaded) { loaded ->
                    if (!loaded) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(background_colour)
                                .padding(content_padding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent_colour)
                        }
                    }
                    else {
                        Column(
                            Modifier
                                .background(background_colour)
                                .fillMaxSize()
                                .padding(content_padding),
                            verticalArrangement = Arrangement.spacedBy(30.dp)
                        ) {
                            for (row in artist.feed_rows) {
                                MediaItemGrid(MediaItemLayout(row.title, null, items = row.items), player)
                            }

                            val description = artist.description
                            if (description?.isNotBlank() == true) {

                                var expanded by remember { mutableStateOf(false) }
                                var can_expand by remember { mutableStateOf(false) }
                                val small_text_height = 200.dp
                                val small_text_height_px = with ( LocalDensity.current ) { small_text_height.toPx().toInt() }

                                ElevatedCard(
                                    Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MainActivity.theme.getOnBackground(false).setAlpha(0.05)
                                    )
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            AssistChip(
                                                { show_info = !show_info },
                                                {
                                                    Text(getString(R.string.artist_info_label), style = MaterialTheme.typography.labelLarge)
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Outlined.Info, null)
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = background_colour,
                                                    labelColor = MainActivity.theme.getOnBackground(false),
                                                    leadingIconContentColor = accent_colour
                                                )
                                            )

                                            if (can_expand) {
                                                NoRipple {
                                                    IconButton(
                                                        { expanded = !expanded }
                                                    ) {
                                                        Icon(if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,null)
                                                    }
                                                }
                                            }
                                        }

                                        LinkifyText(
                                            description,
                                            MainActivity.theme.getOnBackground(false).setAlpha(0.8),
                                            MainActivity.theme.getOnBackground(false),
                                            MaterialTheme.typography.bodyMedium,
                                            Modifier
                                                .onSizeChanged { size ->
                                                    if (size.height == small_text_height_px) {
                                                        can_expand = true
                                                    }
                                                }
                                                .animateContentSize()
                                                .then(
                                                    if (expanded) Modifier else Modifier.height(200.dp)
                                                )
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.requiredHeight(50.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(artist: Artist, close: () -> Unit) {
    AlertDialog(
        close,
        confirmButton = {
            FilledTonalButton(
                close
            ) {
                Text("Close")
            }
        },
        title = { Text("Artist info") },
        text = {
            @Composable
            fun InfoValue(name: String, value: String) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)) {
                        Text(name, style = MaterialTheme.typography.labelLarge)
                        Box(Modifier.fillMaxWidth()) {
                            Marquee(false) {
                                Text(value, softWrap = false)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End) {
                        val clipboard = LocalClipboardManager.current
                        IconButton({
                            clipboard.setText(AnnotatedString(value))
                            sendToast("Copied ${name.lowercase()} to clipboard")
                        }) {
                            Icon(Icons.Filled.ContentCopy, null, Modifier.size(20.dp))
                        }

                        val share_intent = Intent.createChooser(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, value)
                            type = "text/plain"
                        }, null)
                        IconButton({
                            MainActivity.context.startActivity(share_intent)
                        }) {
                            Icon(Icons.Filled.Share, null, Modifier.size(20.dp))
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                InfoValue("Name", artist.name)
                InfoValue("Id", artist.id)
                InfoValue("Url", artist.url)
            }
        }
    )
}