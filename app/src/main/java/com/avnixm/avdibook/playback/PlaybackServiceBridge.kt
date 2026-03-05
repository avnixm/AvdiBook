package com.avnixm.avdibook.playback

object PlaybackServiceBridge {
    @Volatile
    var controller: Controller? = null

    interface Controller {
        fun setSleepTimerDuration(minutes: Int)
        fun setSleepTimerEndOfTrack()
        fun clearSleepTimer()
        fun getSleepTimerState(): SleepTimerState
    }
}
