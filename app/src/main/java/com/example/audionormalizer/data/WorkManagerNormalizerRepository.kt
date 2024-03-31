package com.example.audionormalizer.data

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.audionormalizer.AUDIO_NORMALIZER_WORK_NAME
import com.example.audionormalizer.TAG_OUTPUT
import com.example.audionormalizer.workers.NormalizerWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class WorkManagerNormalizerRepository(context: Context) : NormalizerRepository {

    private val workManager = WorkManager.getInstance(context)

    override val outputWorkInfo: Flow<WorkInfo> =
        workManager.getWorkInfosByTagLiveData(TAG_OUTPUT).asFlow().mapNotNull {
            if (it.isNotEmpty()) it.first() else null
        }

    override fun normalizeAudio(audioSessionId: Int, audioLevel: String) {
        val normalizerWorkRequest =
            OneTimeWorkRequestBuilder<NormalizerWorker>()
                .addTag(TAG_OUTPUT)
                .setInputData(workDataOf("SESSION_ID" to audioSessionId, "AUDIO_LEVEL" to audioLevel))
                .build()

        workManager.enqueueUniqueWork(
            AUDIO_NORMALIZER_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            normalizerWorkRequest
        )
    }

    override fun cancelWork() {
        workManager.cancelUniqueWork(AUDIO_NORMALIZER_WORK_NAME)
    }
}