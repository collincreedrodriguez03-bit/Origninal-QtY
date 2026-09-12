package com.example.data.live

import com.example.ui.ConnectionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Encapsulates point-in-time network and exchange feed health diagnostics.
 * Preserves full auditability of network outages, affected endpoints, and recovery timestamps.
 */
data class NetworkDiagnostics(
    val connectionState: ConnectionState = ConnectionState.CONNECTED,
    val lastSuccessTimeMs: Long = System.currentTimeMillis(),
    val lastFailureTimeMs: Long? = null,
    val lastRecoveryTimeMs: Long? = null,
    val affectedSources: List<String> = emptyList(), // e.g. ["Coinbase", "Kraken", "Kalshi"]
    val retryAttemptCount: Int = 0,
    val consecutiveFailures: Int = 0,
    val lastErrorMessage: String = "",
    val latestValidMarketDataTimestamp: Long = System.currentTimeMillis()
) {
    val formattedLastSuccessTime: String
        get() = if (lastSuccessTimeMs > 0) SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(lastSuccessTimeMs)) else "--:--:--"

    val formattedLastFailureTime: String
        get() = lastFailureTimeMs?.let { SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(it)) } ?: "None"

    val formattedLastRecoveryTime: String
        get() = lastRecoveryTimeMs?.let { SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(it)) } ?: "None"

    val formattedLatestMarketDataTime: String
        get() = if (latestValidMarketDataTimestamp > 0) SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(latestValidMarketDataTimestamp)) else "--:--:--"

    val isRecoveredRecent: Boolean
        get() = lastRecoveryTimeMs != null && (System.currentTimeMillis() - lastRecoveryTimeMs < 30_000L)
}

/**
 * State machine managing exchange feed health, auto-reconnection progression,
 * and failure/recovery diagnostics.
 */
class NetworkFailureRecoveryManager(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var currentState = NetworkDiagnostics(lastSuccessTimeMs = clock(), latestValidMarketDataTimestamp = clock())

    val diagnostics: NetworkDiagnostics
        get() = currentState

    /**
     * Ingests the result of a multi-source market fetch.
     */
    fun onFetchResult(
        coinbaseSuccess: Boolean,
        krakenSuccess: Boolean,
        kalshiSuccess: Boolean,
        errorMessage: String = ""
    ): NetworkDiagnostics {
        val now = clock()
        val affected = mutableListOf<String>()
        if (!coinbaseSuccess) affected.add("Coinbase")
        if (!krakenSuccess) affected.add("Kraken")
        if (!kalshiSuccess) affected.add("Kalshi")

        val spotSuccess = coinbaseSuccess || krakenSuccess

        currentState = if (spotSuccess) {
            val wasFailing = currentState.connectionState == ConnectionState.DISCONNECTED ||
                    currentState.connectionState == ConnectionState.RECONNECTING ||
                    currentState.connectionState == ConnectionState.DATA_STALE ||
                    currentState.connectionState == ConnectionState.API_ERROR ||
                    currentState.consecutiveFailures > 0

            val recoveryTime = if (wasFailing) now else currentState.lastRecoveryTimeMs
            val nextConnState = if (wasFailing) ConnectionState.RECOVERED else ConnectionState.CONNECTED

            currentState.copy(
                connectionState = nextConnState,
                lastSuccessTimeMs = now,
                lastRecoveryTimeMs = recoveryTime,
                affectedSources = affected,
                retryAttemptCount = 0,
                consecutiveFailures = 0,
                lastErrorMessage = if (affected.isNotEmpty()) errorMessage else "",
                latestValidMarketDataTimestamp = now
            )
        } else {
            val consecutive = currentState.consecutiveFailures + 1
            val retries = currentState.retryAttemptCount + 1
            val failureTime = currentState.lastFailureTimeMs ?: now

            val nextState = when {
                consecutive <= 2 -> ConnectionState.RECONNECTING
                consecutive <= 5 -> ConnectionState.DATA_STALE
                else -> ConnectionState.DISCONNECTED
            }

            currentState.copy(
                connectionState = nextState,
                lastFailureTimeMs = failureTime,
                affectedSources = affected,
                retryAttemptCount = retries,
                consecutiveFailures = consecutive,
                lastErrorMessage = errorMessage.ifEmpty { "Spot feeds unreachable" }
            )
        }
        return currentState
    }

    /**
     * Checks if market data age exceeds stale threshold (default: 20s).
     */
    fun checkDataFreshness(staleThresholdMs: Long = 20_000L): ConnectionState {
        val now = clock()
        val age = now - currentState.latestValidMarketDataTimestamp
        if (age > staleThresholdMs && (currentState.connectionState == ConnectionState.CONNECTED || currentState.connectionState == ConnectionState.RECOVERED)) {
            currentState = currentState.copy(connectionState = ConnectionState.DATA_STALE)
        }
        return currentState.connectionState
    }
}
