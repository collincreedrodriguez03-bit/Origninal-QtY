package com.example

import com.example.data.live.LiveContractWindow
import com.example.data.live.LiveFeatureSnapshot
import com.example.data.live.LivePredictionEngine
import com.example.data.live.NetworkDiagnostics
import com.example.data.live.NetworkFailureRecoveryManager
import com.example.ui.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkFailureRecoveryManagerTest {

    private var mockCurrentTime = 1700000000000L
    private lateinit var recoveryManager: NetworkFailureRecoveryManager

    @Before
    fun setUp() {
        mockCurrentTime = 1700000000000L
        recoveryManager = NetworkFailureRecoveryManager(
            clock = { mockCurrentTime }
        )
    }

    @Test
    fun testInitialState_isConnectedAndZeroErrors() {
        val diag = recoveryManager.diagnostics
        assertEquals(ConnectionState.CONNECTED, diag.connectionState)
        assertEquals(0, diag.retryAttemptCount)
        assertTrue(diag.affectedSources.isEmpty())
        assertNull(diag.lastFailureTimeMs)
        assertNull(diag.lastRecoveryTimeMs)
    }

    @Test
    fun testSuccessfulFetch_updatesTimestampAndPreservesConnectedState() {
        mockCurrentTime = 1700000000000L
        val diag = recoveryManager.onFetchResult(
            coinbaseSuccess = true,
            krakenSuccess = true,
            kalshiSuccess = true,
            errorMessage = ""
        )

        assertEquals(ConnectionState.CONNECTED, diag.connectionState)
        assertEquals(0, diag.retryAttemptCount)
        assertEquals(mockCurrentTime, diag.latestValidMarketDataTimestamp)
        assertTrue(diag.affectedSources.isEmpty())
    }

    @Test
    fun testPartialOutage_coinbaseDownKrakenUp_maintainsValidFeed() {
        mockCurrentTime = 1700000000000L
        val diag = recoveryManager.onFetchResult(
            coinbaseSuccess = false,
            krakenSuccess = true,
            kalshiSuccess = true,
            errorMessage = "Coinbase timeout"
        )

        // Kraken is still alive, so market data is valid, but affected source is logged
        assertEquals(ConnectionState.CONNECTED, diag.connectionState)
        assertEquals(mockCurrentTime, diag.latestValidMarketDataTimestamp)
        assertTrue(diag.affectedSources.contains("Coinbase"))
        assertFalse(diag.affectedSources.contains("Kraken"))
        assertEquals(0, diag.retryAttemptCount) // Still have authoritative price via Kraken
    }

    @Test
    fun testFullOutage_transitionsToReconnectingAndIncrementsRetry() {
        mockCurrentTime = 1700000000000L
        val diag1 = recoveryManager.onFetchResult(
            coinbaseSuccess = false,
            krakenSuccess = false,
            kalshiSuccess = false,
            errorMessage = "No internet connection"
        )

        assertEquals(ConnectionState.RECONNECTING, diag1.connectionState)
        assertEquals(1, diag1.retryAttemptCount)
        assertTrue(diag1.affectedSources.contains("Coinbase"))
        assertTrue(diag1.affectedSources.contains("Kraken"))
        assertTrue(diag1.affectedSources.contains("Kalshi"))
        assertEquals(mockCurrentTime, diag1.lastFailureTimeMs)
    }

    @Test
    fun testRepeatedFailures_progressionThroughReconnectingStaleAndDisconnected() {
        val baseTime = 1700000000000L

        // Failure 1 (consecutive=1) -> RECONNECTING
        mockCurrentTime = baseTime
        val d1 = recoveryManager.onFetchResult(false, false, false, "Err")
        assertEquals(ConnectionState.RECONNECTING, d1.connectionState)
        assertEquals(1, d1.retryAttemptCount)

        // Failure 2 (consecutive=2) -> RECONNECTING
        mockCurrentTime = baseTime + 10_000L
        val d2 = recoveryManager.onFetchResult(false, false, false, "Err")
        assertEquals(ConnectionState.RECONNECTING, d2.connectionState)
        assertEquals(2, d2.retryAttemptCount)

        // Failure 3 (consecutive=3) -> DATA_STALE
        mockCurrentTime = baseTime + 20_000L
        val d3 = recoveryManager.onFetchResult(false, false, false, "Err")
        assertEquals(ConnectionState.DATA_STALE, d3.connectionState)
        assertEquals(3, d3.retryAttemptCount)

        // Failure 6 (consecutive=6) -> DISCONNECTED
        mockCurrentTime = baseTime + 30_000L
        recoveryManager.onFetchResult(false, false, false, "Err")
        mockCurrentTime = baseTime + 40_000L
        recoveryManager.onFetchResult(false, false, false, "Err")
        mockCurrentTime = baseTime + 50_000L
        val d6 = recoveryManager.onFetchResult(false, false, false, "Err")
        assertEquals(ConnectionState.DISCONNECTED, d6.connectionState)
        assertEquals(6, d6.retryAttemptCount)
    }

    @Test
    fun testRecoveryAfterOutage_transitionsToRecoveredThenConnected() {
        val baseTime = 1700000000000L

        // Step 1: Outage occurs
        mockCurrentTime = baseTime
        recoveryManager.onFetchResult(false, false, false, "DNS failure")
        mockCurrentTime = baseTime + 10_000L
        recoveryManager.onFetchResult(false, false, false, "DNS failure")
        assertEquals(ConnectionState.RECONNECTING, recoveryManager.diagnostics.connectionState)

        // Step 2: Feeds recover
        val recoveryTime = baseTime + 20_000L
        mockCurrentTime = recoveryTime
        val recoveredDiag = recoveryManager.onFetchResult(true, true, true, "")

        assertEquals(ConnectionState.RECOVERED, recoveredDiag.connectionState)
        assertEquals(0, recoveredDiag.retryAttemptCount)
        assertEquals(recoveryTime, recoveredDiag.lastRecoveryTimeMs)
        assertEquals(recoveryTime, recoveredDiag.latestValidMarketDataTimestamp)
        assertTrue(recoveredDiag.affectedSources.isEmpty())

        // Step 3: Subsequent cycle stabilizes to CONNECTED
        mockCurrentTime = recoveryTime + 10_000L
        val nextDiag = recoveryManager.onFetchResult(true, true, true, "")
        assertEquals(ConnectionState.CONNECTED, nextDiag.connectionState)
        assertEquals(recoveryTime, nextDiag.lastRecoveryTimeMs) // Preserves last recovery timestamp
    }

    @Test
    fun testCheckDataFreshness_detectsStaleFeedsWhenTimeElapsed() {
        val baseTime = 1700000000000L
        mockCurrentTime = baseTime
        recoveryManager.onFetchResult(true, true, true, "")

        // 10 seconds later: Still fresh (threshold is 20s)
        mockCurrentTime = baseTime + 10_000L
        val freshState = recoveryManager.checkDataFreshness(20_000L)
        assertEquals(ConnectionState.CONNECTED, freshState)

        // 25 seconds later: Exceeds threshold -> DATA_STALE
        mockCurrentTime = baseTime + 25_000L
        val staleState = recoveryManager.checkDataFreshness(20_000L)
        assertEquals(ConnectionState.DATA_STALE, staleState)
    }

    @Test
    fun testLivePredictionEngine_failClosedOnDisconnectedOrStaleState() {
        val features = LiveFeatureSnapshot(
            timestampMs = 1700000000000L,
            spotPrice = 65000.0,
            strikePrice = 65000.0,
            deltaToStrike = 0.0,
            rsi = 50.0,
            momentum = 0.0,
            ema9 = 65000.0,
            ema21 = 65000.0,
            emaSpread = 0.0,
            volatility = 0.001,
            dataFreshness = ConnectionState.CONNECTED,
            isStale = false
        )

        // Case A: CONNECTED & Fresh -> Valid evaluation
        val liveResult = LivePredictionEngine.evaluate(
            features = features,
            secondsRemaining = 450,
            sampleSizeN = 0,
            historyTickCount = 30
        )
        assertFalse(liveResult.isStale)
        assertFalse(liveResult.direction.isEmpty())

        // Case B: Stale flag set -> Fail-closed NO TRADE
        val staleResult = LivePredictionEngine.evaluate(
            features = features.copy(isStale = true, dataFreshness = ConnectionState.DATA_STALE),
            secondsRemaining = 450,
            sampleSizeN = 0,
            historyTickCount = 30
        )
        assertTrue(staleResult.isStale)
        assertEquals("NO TRADE", staleResult.direction)
        assertTrue(staleResult.reasoning.contains("DATA STALE"))

        // Case C: DISCONNECTED state -> Fail-closed NO TRADE
        val disconnectedResult = LivePredictionEngine.evaluate(
            features = features.copy(isStale = true, dataFreshness = ConnectionState.DISCONNECTED),
            secondsRemaining = 450,
            sampleSizeN = 0,
            historyTickCount = 30
        )
        assertTrue(disconnectedResult.isStale)
        assertEquals("NO TRADE", disconnectedResult.direction)

        // Case D: RECOVERED state -> Restores normal evaluation without corrupting model state
        val recoveredResult = LivePredictionEngine.evaluate(
            features = features.copy(isStale = false, dataFreshness = ConnectionState.RECOVERED),
            secondsRemaining = 450,
            sampleSizeN = 0,
            historyTickCount = 30
        )
        assertFalse(recoveredResult.isStale)
        assertFalse(recoveredResult.direction.isEmpty())
    }
}
