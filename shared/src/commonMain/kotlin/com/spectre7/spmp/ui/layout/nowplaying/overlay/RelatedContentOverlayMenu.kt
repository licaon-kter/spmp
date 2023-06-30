package com.spectre7.spmp.ui.layout.nowplaying.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.spectre7.spmp.model.mediaitem.Song
import com.spectre7.spmp.ui.component.PillMenu
import com.spectre7.spmp.ui.layout.SongRelatedPage
import com.spectre7.spmp.ui.theme.Theme
import com.spectre7.utils.setAlpha

class RelatedContentOverlayMenu : OverlayMenu() {
    override fun closeOnTap(): Boolean = false

    @Composable
    override fun Menu(
        getSong: () -> Song,
        getExpansion: () -> Float,
        openMenu: (OverlayMenu?) -> Unit,
        getSeekState: () -> Any,
        getCurrentSongThumb: () -> ImageBitmap?
    ) {
        val pill_menu = remember { PillMenu(
            _background_colour = Theme.current.accent_provider
        ) }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            SongRelatedPage(
                pill_menu,
                getSong(),
                Modifier.fillMaxSize(),
                title_text_style = MaterialTheme.typography.headlineSmall,
                description_text_style = MaterialTheme.typography.bodyMedium,
                close = { openMenu(null) },
                padding = PaddingValues(10.dp),
                accent_colour = LocalContentColor.current.setAlpha(0.75f)
            )

            pill_menu.PillMenu(
                1,
                { index, _ ->
                    when (index) {
                        0 -> 
                            ActionButton(Icons.Filled.Close) {
                                openMenu(null)
                            }
                        else -> throw NotImplementedError(index.toString())
                    }
                }
            )
        }
    }
}