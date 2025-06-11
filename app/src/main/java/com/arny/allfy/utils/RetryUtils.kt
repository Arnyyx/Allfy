package com.arny.allfy.utils

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.delay
import java.io.IOException

data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000L,
    val backoffFactor: Float = 2f,
    val retryableExceptions: List<Class<out Exception>> = listOf(
        FirebaseNetworkException::class.java,
        IOException::class.java
    )
)

suspend fun <T> retry(
    config: RetryConfig = RetryConfig(),
    block: suspend () -> T
): T {
    var currentDelay = config.initialDelayMs
    repeat(config.maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (config.retryableExceptions.any { it.isInstance(e) }) {
                Log.w("RetryUtils", "Attempt ${attempt + 1} failed: $e")
                delay(currentDelay)
                currentDelay = (currentDelay * config.backoffFactor).toLong()
            } else {
                throw e
            }
        }
    }
    return block()
}