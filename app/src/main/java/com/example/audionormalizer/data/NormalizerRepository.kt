package com.example.audionormalizer.data

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow

interface NormalizerRepository {
    val outputWorkInfo: Flow<WorkInfo>
    fun normalizeAudio()
    fun cancelWork()
}