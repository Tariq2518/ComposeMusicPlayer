package com.tariq.composemusicplayer.ui.music

import android.graphics.Color
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.tariq.composemusicplayer.data.model.AudioItem
import com.tariq.composemusicplayer.ui.theme.ComposeMusicPlayerTheme
import kotlin.math.floor


val dummyMusicList = listOf(
    AudioItem(
        uri = "".toUri(),
        displayName = "Kotlin Programming",
        id = 0L,
        artist = "Test",
        data = "",
        duration = 12345,
        title = "Android Programming"
    ),
    AudioItem(
        uri = "".toUri(),
        displayName = "Kotlin Programming",
        id = 0L,
        artist = "Lab",
        data = "",
        duration = 25678,
        title = "Android Programming"
    ),
    AudioItem(
        uri = "".toUri(),
        displayName = "Kotlin Programming",
        id = 0L,
        artist = "Android Lab",
        data = "",
        duration = 8765454,
        title = "Android Programming"
    ),
    AudioItem(
        uri = "".toUri(),
        displayName = "Kotlin Programming",
        id = 0L,
        artist = "Kotlin Lab",
        data = "",
        duration = 23456,
        title = "Android Programming"
    ),
    AudioItem(
        uri = "".toUri(),
        displayName = "Kotlin Programming",
        id = 0L,
        artist = "Test Lab",
        data = "",
        duration = 65788,
        title = "Android Programming"
    ),
    AudioItem(
        uri = "".toUri(),
        displayName = "Kotlin Programming",
        id = 0L,
        artist = "Test Lab",
        data = "",
        duration = 234567,
        title = "Android Programming"
    ),

    )

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    isMusicPlaying: Boolean,
    musicList: List<AudioItem>,
    currentPlayingMusic: AudioItem?,
    onStart: (AudioItem) -> Unit,
    onItemClicked: (AudioItem) -> Unit,
    onNext: () -> Unit


) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val animatedHeight by animateDpAsState(
        targetValue = if (currentPlayingMusic == null) 0.dp
        else BottomSheetScaffoldDefaults.SheetPeekHeight
    )
    BottomSheetScaffold(
        sheetContent = {
            currentPlayingMusic?.let { currentPlayingMusic ->
                BottomBarPlayer(
                    progress = progress,
                    onProgressChanged = onProgressChanged,
                    music = currentPlayingMusic,
                    isAudioPlaying = isMusicPlaying,
                    onStart = { onStart.invoke(currentPlayingMusic) },
                    onNext = { onNext.invoke() },

                    )

            }
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = animatedHeight
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 56.dp)
        ) {
            items(musicList) { music: AudioItem ->
                MusicItem(
                    music = music,
                    onItemClicked = { onItemClicked.invoke(music) }
                )

            }

        }

    }
}

@Composable
fun MusicItem(
    music: AudioItem,
    onItemClicked: (id: Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                Log.i("CLick", "MusicItem: ${music.title}")
                onItemClicked.invoke(music.id)
            },
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),

        ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,

            ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),

                ) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = music.displayName,
                    style = MaterialTheme.typography.h6,
                    overflow = TextOverflow.Clip,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = music.artist,
                    style = MaterialTheme.typography.subtitle1,
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),

                    )

            }

            Text(text = timeStampToDuration(music.duration.toLong()))
            Spacer(modifier = Modifier.size(8.dp))

        }

    }

}

private fun timeStampToDuration(position: Long): String {
    val totalSeconds = floor(position / 1E3).toInt()
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds - (minutes * 60)
    return if (position < 0) "--:--" else "%d:%02d".format(minutes, remainingSeconds)

}

@Composable
fun BottomBarPlayer(
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    music: AudioItem,
    isAudioPlaying: Boolean,
    onStart: () -> Unit,
    onNext: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtistInfo(
                music = music,
                modifier = Modifier.weight(1f),

                )
            PlayerController(
                isMusicPlaying = isAudioPlaying,
                onStart = { onStart.invoke() },
                onNext = { onNext.invoke() },

                )

        }
        Slider(
            value = progress,
            onValueChange = { onProgressChanged.invoke(it) },
            valueRange = 0f..100f,

            )
    }
}


@Composable
fun PlayerController(
    isMusicPlaying: Boolean,
    onStart: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(56.dp)
            .padding(4.dp),


        ) {
        PlayerIcon(
            icons = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            backgroundColor = MaterialTheme.colors.primary
        ) {
            onStart.invoke()
        }
        Spacer(modifier = Modifier.size(8.dp))
        Icon(
            imageVector = Icons.Default.SkipNext,
            null,
            modifier = Modifier.clickable {
                onNext.invoke()
            }
        )
    }

}


@Composable
fun ArtistInfo(
    modifier: Modifier = Modifier,
    music: AudioItem
) {
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerIcon(
            icons = Icons.Default.MusicNote,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface
            )
        ) {}
        Spacer(modifier = Modifier.size(4.dp))
        Column {
            Text(
                text = music.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = music.artist,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.subtitle1,
                overflow = TextOverflow.Clip,
                maxLines = 1
            )

        }
    }
}


@Composable
fun PlayerIcon(
    modifier: Modifier = Modifier,
    icons: ImageVector,
    border: BorderStroke? = null,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colors.surface,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colors.onSurface,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        border = border,
        modifier = Modifier
            .clip(CircleShape)
            .clickable {
                onClick.invoke()
            },
        contentColor = color,
        color = backgroundColor
    ) {

        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icons, contentDescription = null)

        }
    }

}



@Composable
fun MiniPlayer() {
    ComposeMusicPlayerTheme {
        BottomBarPlayer(
            progress = 50f,
            onProgressChanged = {},
            music = dummyMusicList[0],
            isAudioPlaying = true,
            onStart = {
/*TODO*/
 }) {

        }

    }
}

@Preview(showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    ComposeMusicPlayerTheme {
        HomeScreen(
            progress = 0f,
            onProgressChanged = {},
            isMusicPlaying = false,
            musicList = dummyMusicList,
            currentPlayingMusic = null,
            onStart = {},
            onItemClicked = {}
        ) {

        }
    }
}

