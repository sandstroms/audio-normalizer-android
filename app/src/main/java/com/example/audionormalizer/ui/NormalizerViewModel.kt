package com.example.audionormalizer.ui

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
            when {
                info.state.isFinished -> {
                    NormalizerUiState.Complete
                }
                info.state == WorkInfo.State.CANCELLED -> {
                    NormalizerUiState.Default
                }
                else -> NormalizerUiState.Recording
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_0000),
            initialValue = NormalizerUiState.Default
        )

    fun normalizeAudio() {
        normalizerRepository.normalizeAudio()
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
    data object Recording : NormalizerUiState
    data object Complete : NormalizerUiState
}