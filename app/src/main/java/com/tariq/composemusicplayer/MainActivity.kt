package com.tariq.composemusicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tariq.composemusicplayer.ui.music.HomeScreen
import com.tariq.composemusicplayer.ui.music.MusicViewModel
import com.tariq.composemusicplayer.ui.theme.ComposeMusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    var isPermissionGranted = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeMusicPlayerTheme {
                val getPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {
                    isPermissionGranted = it
                }

                val lifeCycleOwner = LocalLifecycleOwner.current
                DisposableEffect(key1 = lifeCycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            getPermission.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                    lifeCycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifeCycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {

                        val musicViewModel = viewModel(
                            modelClass = MusicViewModel::class.java
                        )

                        val musicList = musicViewModel.musicList

                        HomeScreen(
                            progress = musicViewModel.currentMusicProgress.value,
                            onProgressChanged = {
                                musicViewModel.seekTo(it)
                            },
                            isMusicPlaying = musicViewModel.isMusicPlaying,
                            musicList =musicList,
                            currentPlayingMusic =musicViewModel.currentPlayingMusic.value,
                            onStart ={
                                     musicViewModel.playMusic(it)
                            },
                            onItemClicked ={
                                musicViewModel.playMusic(it)
                            },
                            onNext = {
                                musicViewModel.skipToNext()
                            }
                        )


                }

            }

        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeMusicPlayerTheme {
        Greeting("Android")
    }
}