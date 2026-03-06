package com.avnixm.avdibook.debug

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object DebugCrashReporter {
    private const val PREFS = "debug_crash"
    private const val KEY_CRASH = "last_crash"
    private const val TAG = "AvdiBook_CRASH"

    fun install(context: Context) {
        val prefs = prefs(context)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val report = buildReport(thread, throwable)
            Log.e(TAG, report)
            prefs.edit().putString(KEY_CRASH, report).apply()
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun consumeLastCrash(context: Context): String? {
        val prefs = prefs(context)
        val crash = prefs.getString(KEY_CRASH, null) ?: return null
        prefs.edit().remove(KEY_CRASH).apply()
        return crash
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val sb = StringBuilder()
        sb.appendLine("Thread: ${thread.name}")
        sb.appendLine("Exception: ${throwable::class.qualifiedName}")
        sb.appendLine("Message: ${throwable.message}")
        sb.appendLine()
        sb.appendLine("Stack trace:")
        throwable.stackTrace.take(20).forEach { frame ->
            sb.appendLine("  at $frame")
        }
        var cause = throwable.cause
        var depth = 0
        while (cause != null && depth < 3) {
            sb.appendLine()
            sb.appendLine("Caused by: ${cause::class.qualifiedName}: ${cause.message}")
            cause.stackTrace.take(10).forEach { frame ->
                sb.appendLine("  at $frame")
            }
            cause = cause.cause
            depth++
        }
        return sb.toString()
    }
}
