package com.example.audionormalizer.data

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.audionormalizer.AUDIO_NORMALIZER_WORK_NAME
import com.example.audionormalizer.TAG_OUTPUT
import com.example.audionormalizer.workers.NormalizerWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.util.concurrent.TimeUnit

class WorkManagerNormalizerRepository(context: Context) : NormalizerRepository {

    private val workManager = WorkManager.getInstance(context)

    override val outputWorkInfo: Flow<WorkInfo> =
        workManager.getWorkInfosByTagLiveData(TAG_OUTPUT).asFlow().mapNotNull {
            if (it.isNotEmpty()) it.first() else null
        }

    override fun normalizeAudio() {
        val normalizerWorkRequest =
            PeriodicWorkRequestBuilder<NormalizerWorker>(15, TimeUnit.MINUTES)
                .addTag(TAG_OUTPUT)
                .build()

        workManager.enqueueUniquePeriodicWork(
            AUDIO_NORMALIZER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            normalizerWorkRequest
        )
    }

    override fun cancelWork() {
        workManager.cancelUniqueWork(AUDIO_NORMALIZER_WORK_NAME)
    }
}