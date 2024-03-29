package com.example.audionormalizer.ui

import android.provider.MediaStore.Audio
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkInfo
import androidx.work.workDataOf
import com.example.audionormalizer.AudioNormalizerApplication
import com.example.audionormalizer.data.NormalizerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NormalizerViewModel(private val normalizerRepository: NormalizerRepository) : ViewModel() {
    val normalizerUiState: StateFlow<NormalizerUiState> = normalizerRepository.outputWorkInfo
        .map { info ->
            if (info.state == WorkInfo.State.RUNNING) NormalizerUiState.Normalizing
            else if (info.state == WorkInfo.State.FAILED && info.outputData == workDataOf("ERROR" to "AUDIO_LEVEL_ERROR")) NormalizerUiState.Error
            else NormalizerUiState.Default
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_0000),
            initialValue = NormalizerUiState.Default
        )

    fun normalizeAudio(audioLevel: AudioLevel) {
        normalizerRepository.normalizeAudio(audioLevel.textDescription)
    }

    fun cancelWork() {
        normalizerRepository.cancelWork()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val audioNormalizerRepository =
                    (this[APPLICATION_KEY] as AudioNormalizerApplication).container.normalizerRepository
                NormalizerViewModel(
                    normalizerRepository = audioNormalizerRepository
                )
            }
        }
    }
}

sealed interface NormalizerUiState {
    data object Default : NormalizerUiState
    data object Normalizing : NormalizerUiState
    data object Error : NormalizerUiState
}

enum class AudioLevel(val textDescription: String) {
    LOW("Low"), MEDIUM("Medium"), HIGH("High"), DYNAMIC("Dynamic")
}