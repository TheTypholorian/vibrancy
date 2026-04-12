package net.typho.vibrancy.util

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object VibrancyThreadPool : ThreadPoolExecutor(2, 2, 10L, TimeUnit.MINUTES, LinkedBlockingQueue())