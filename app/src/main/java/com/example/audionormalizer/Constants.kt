package com.example.audionormalizer

// Name of Notification Channel for verbose notifications of background work
val VERBOSE_NOTIFICATION_CHANNEL_NAME: CharSequence =
    "Verbose WorkManager Notifications"
const val VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION =
    "Shows notifications whenever work starts"
const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
val NOTIFICATION_TITLE: CharSequence = "WorkRequest Starting"
const val NOTIFICATION_ID = 1
const val AUDIO_NORMALIZER_WORK_NAME = "audio_normalizer_work"
const val TAG_OUTPUT = "OUTPUT"