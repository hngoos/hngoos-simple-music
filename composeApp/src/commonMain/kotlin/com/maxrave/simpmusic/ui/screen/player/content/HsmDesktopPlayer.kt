package com.maxrave.simpmusic.ui.screen.player.content

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.AppleMusicPlaybackControls
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.rememberAppleMusicTypography
import com.maxrave.simpmusic.ui.theme.ForceDarkContent
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_icon
import simpmusic.composeapp.generated.resources.hsm_album_artwork
import simpmusic.composeapp.generated.resources.hsm_close_player
import simpmusic.composeapp.generated.resources.hsm_lyrics_empty
import simpmusic.composeapp.generated.resources.hsm_lyrics_loading
import simpmusic.composeapp.generated.resources.hsm_now_playing
import simpmusic.composeapp.generated.resources.hsm_queue
import simpmusic.composeapp.generated.resources.hsm_volume

/** Desktop listening room: one playback state, a persistent sleeve and a live lyric column. */
@Composable
internal fun HsmDesktopPlayer(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    ForceDarkContent {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(Modifier.fillMaxSize().background(Color(0xFF202024))) {
                AppleMusicArtworkBackdrop(
                    state = state,
                    actions = actions,
                    seedColor = state.startColor.value.takeUnless { it == Color.Black } ?: seed,
                )
                // A fixed veil keeps lyrics readable even over a pale or white album sleeve.
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
                Column(Modifier.fillMaxSize()) {
                    HsmPlayerHeader(state, actions)
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val wide = maxWidth >= 900.dp
                        val wideArtworkSize = minOf(maxHeight - 260.dp, 440.dp).coerceAtLeast(160.dp)
                        val compactArtworkSize = minOf(maxWidth - 88.dp, 320.dp).coerceAtLeast(120.dp)
                        if (wide) {
                            Row(
                                Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HsmSleeve(
                                    state, actions,
                                    Modifier.weight(0.46f).fillMaxHeight(),
                                    wideArtworkSize,
                                )
                                HsmLyrics(state, actions, Modifier.weight(0.54f).fillMaxHeight())
                            }
                        } else {
                            // Scroll the whole compact layout; short windows never lose transport.
                            Column(
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                HsmSleeve(state, actions, Modifier.fillMaxWidth(), compactArtworkSize, scrollable = false)
                                Spacer(Modifier.height(32.dp))
                                HsmLyrics(state, actions, Modifier.fillMaxWidth().height(420.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HsmPlayerHeader(state: NowPlayingContentState, actions: NowPlayingContentActions) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onDismiss) {
            Icon(state.dismissIcon, stringResource(Res.string.hsm_close_player), tint = Color.White)
        }
        Image(painterResource(Res.drawable.app_icon), null, Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text("HSM", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(Res.string.hsm_now_playing), fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
        Spacer(Modifier.weight(1f))
        Icon(SimpIcons.VolumeUp, stringResource(Res.string.hsm_volume), Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.width(8.dp))
        Slider(
            value = state.controllerState.volume.coerceIn(0f, 1f),
            onValueChange = { actions.onUIEvent(UIEvent.UpdateVolume(it)) },
            modifier = Modifier.width(112.dp),
        )
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = actions.onShowQueue) {
            Icon(SimpIcons.QueueMusic, stringResource(Res.string.hsm_queue), tint = Color.White)
        }
    }
}

@Composable
private fun HsmSleeve(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    modifier: Modifier,
    artworkSize: androidx.compose.ui.unit.Dp,
    scrollable: Boolean = true,
) {
    val typography = rememberAppleMusicTypography().let {
        it.copy(
            mainTitle = it.mainTitle.copy(fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White),
            mainArtist = it.mainArtist.copy(fontSize = 17.sp),
        )
    }
    Column(
        modifier.then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        var artwork by remember(state.screenData.thumbnailURL) { mutableStateOf(state.screenData.thumbnailURL) }
        Crossfade(targetState = artwork, animationSpec = tween(280), label = "albumArtwork") { url ->
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(280).build(),
                placeholder = painterResource(Res.drawable.app_icon),
                error = painterResource(Res.drawable.app_icon),
                onSuccess = { actions.onArtworkBitmap(it.result.image.toImageBitmap()) },
                onError = {
                    val fallback = url?.replace("maxresdefault", "hqdefault")
                    if (fallback != null && fallback != url) artwork = fallback
                },
                contentDescription = stringResource(Res.string.hsm_album_artwork),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(artworkSize).shadow(28.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)),
            )
        }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
            AppleMusicMainTitleRow(state, actions, typography)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                AppleMusicPlaybackControls(state, actions, typography, showShuffleAndRepeat = true)
            }
            state.audioQualityLabel?.let { quality ->
                Spacer(Modifier.height(16.dp))
                Text(quality, modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f))
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HsmLyrics(state: NowPlayingContentState, actions: NowPlayingContentActions, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        val lyrics = state.screenData.lyricsData
        if (lyrics != null) {
            LyricsView(
                lyricsData = lyrics,
                timeLine = state.timelineFlow,
                onLineClick = { actions.onUIEvent(UIEvent.UpdateProgress(it)) },
                modifier = Modifier.fillMaxSize(),
                showScrollShadows = true,
                backgroundColor = state.startColor.value,
            )
        } else {
            Text(
                stringResource(if (state.screenData.lyricsNotFound) Res.string.hsm_lyrics_empty else Res.string.hsm_lyrics_loading),
                Modifier.padding(32.dp),
                fontSize = 28.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}
