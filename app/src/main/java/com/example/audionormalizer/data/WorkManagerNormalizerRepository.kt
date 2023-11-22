package com.example.audionormalizer.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.audionormalizer.AUDIO_NORMALIZER_WORK_NAME
import com.example.audionormalizer.TAG_OUTPUT
import kotlinx.coroutines.flow.Flow
import java.time.Duration

class WorkManagerNormalizerRepository(context: Context) : AudioNormalizerRepository {

    private val workManager = WorkManager.getInstance(context)

    override val outputWorkInfo: Flow<WorkInfo> =
        workManager.getWorkInfosByTagLiveData(TAG_OUTPUT).asFlow().mapNotNull {
            if (it.isNotEmpty()) it.first() else null
        }

    override fun normalizeAudio(currentRms: Int) {
        val continuation = workManager
            .beginUniqueWork(
                "normalize_audio_work",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.Companion.from(AudioNormalizerWorker::class.java)
            )

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val normalizerBuilder = PeriodicWorkRequestBuilder<AudioNormalizerWorker>(Duration.ofMillis(500))

        normalizerBuilder.setConstraints(constraints)

        continuation.enqueue()
    }

    override fun cancelWork() {
        workManager.cancelUniqueWork(AUDIO_NORMALIZER_WORK_NAME)
    }
}