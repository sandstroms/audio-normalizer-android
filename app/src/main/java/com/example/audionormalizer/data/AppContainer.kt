package com.example.audionormalizer.data

import android.content.Context

interface AppContainer {
    val normalizerRepository: NormalizerRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val normalizerRepository = WorkManagerNormalizerRepository(context)
}