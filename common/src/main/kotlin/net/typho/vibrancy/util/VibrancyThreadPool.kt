package net.typho.vibrancy.util

import net.typho.vibrancy.VibrancyConfig
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object VibrancyThreadPool : ThreadPoolExecutor(
    VibrancyConfig.asyncThreads,
    VibrancyConfig.asyncThreads,
    10L,
    TimeUnit.MINUTES,
    LinkedBlockingQueue()
)