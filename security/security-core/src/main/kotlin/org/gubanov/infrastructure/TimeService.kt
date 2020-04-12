package org.gubanov.infrastructure

import java.time.Duration

interface TimeService {
    fun millisFromEpoch(): Long
    fun nanosFromJvm(): Long
}

class SystemTimeService : TimeService {
    override fun millisFromEpoch(): Long {
        return System.currentTimeMillis()
    }

    override fun nanosFromJvm(): Long {
        return System.nanoTime()
    }
}

class TestTimeService : TimeService {
    private var time: Duration = Duration.ZERO

    override fun millisFromEpoch(): Long {
        return time.toMillis()
    }

    override fun nanosFromJvm(): Long {
        return time.toNanos()
    }

    fun advance(duration: Duration) {
        time = time.plus(duration)
    }
}