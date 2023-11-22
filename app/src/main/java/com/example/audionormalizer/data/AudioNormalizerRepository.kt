package com.example.audionormalizer.data

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow

interface AudioNormalizerRepository {
    val outputWorkInfo: Flow<WorkInfo>
    fun normalizeAudio()
    fun cancelWork()
}