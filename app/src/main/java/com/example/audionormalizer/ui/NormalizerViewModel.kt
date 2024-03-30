package com.example.audionormalizer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkInfo
import com.example.audionormalizer.AudioNormalizerApplication
import com.example.audionormalizer.data.NormalizerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NormalizerViewModel(private val normalizerRepository: NormalizerRepository) : ViewModel() {
    val normalizerUiState: StateFlow<NormalizerUiState> = normalizerRepository.outputWorkInfo
        .map { info ->
            when (info.state) {
                WorkInfo.State.RUNNING -> NormalizerUiState.Normalizing
                else -> NormalizerUiState.Default
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_0000),
            initialValue = NormalizerUiState.Default
        )

    var selectedOption by mutableStateOf(AudioLevel.MEDIUM)
        private set

    fun updateSelectedOption(updatedOption: AudioLevel) {
        selectedOption = updatedOption
    }

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
}

enum class AudioLevel(val textDescription: String) {
    VERY_QUIET("Very quiet"), QUIET("Quiet"), MEDIUM("Medium"), LOUD("Loud"), DYNAMIC("Dynamic")
}