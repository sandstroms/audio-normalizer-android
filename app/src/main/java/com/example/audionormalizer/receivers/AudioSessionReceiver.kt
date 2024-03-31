package com.example.audionormalizer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.util.Log
import com.example.audionormalizer.ui.NormalizerViewModel

class AudioSessionReceiver(private val viewModel: NormalizerViewModel) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val id: Int = intent?.getIntExtra(Equalizer.EXTRA_AUDIO_SESSION, -1) ?: -1
        val packageName: String = intent?.getStringExtra(Equalizer.EXTRA_PACKAGE_NAME) ?: ""
        viewModel.updateAudioSessionId(id)
    }
}