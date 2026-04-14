package net.typho.vibrancy.util

import net.typho.vibrancy.Vibrancy
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object VibrancyThreadPool : ThreadPoolExecutor(
    Vibrancy.config.asyncThreads,
    Vibrancy.config.asyncThreads,
    10L,
    TimeUnit.MINUTES,
    LinkedBlockingQueue()
)