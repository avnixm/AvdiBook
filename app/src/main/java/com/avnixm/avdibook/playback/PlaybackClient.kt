package com.avnixm.avdibook.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object PlaybackClient {
    private val directExecutor = Executor { runnable -> runnable.run() }

    @Volatile
    private var controller: MediaController? = null

    @Volatile
    private var controllerFuture: ListenableFuture<MediaController>? = null

    suspend fun connect(context: Context): MediaController {
        controller?.takeIf { it.isConnected }?.let { return it }
        controller = null

        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        val future = controllerFuture?.takeIf { !it.isDone || !it.isCancelled }
            ?: MediaController.Builder(context, token).buildAsync().also {
                controllerFuture = it
            }

        return runCatching { future.await() }
            .onSuccess { controller = it }
            .onFailure { controllerFuture = null }
            .getOrThrow()
    }

    fun release() {
        controller?.release()
        controller = null
        controllerFuture?.cancel(true)
        controllerFuture = null
    }

    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (throwable: Throwable) {
                    continuation.resumeWithException(throwable)
                }
            },
            directExecutor
        )

        continuation.invokeOnCancellation {
            cancel(true)
        }
    }
}
