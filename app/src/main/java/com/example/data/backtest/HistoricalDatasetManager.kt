package com.example.data.backtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.sin

object HistoricalDatasetManager {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Sanitizes raw candle list by:
     * 1. Deduplicating by timestamp.
     * 2. Sorting in ascending chronological order (t_i < t_{i+1}).
     * 3. Detecting gaps (> 60,000 ms).
     */
    fun sanitizeAndProcessCandles(
        rawCandles: List<HistoricalCandle>,
        requestedStartMs: Long,
        requestedEndMs: Long,
        sourceType: DatasetSourceType = DatasetSourceType.REAL_COINBASE_API
    ): Pair<List<HistoricalCandle>, HistoricalDatasetMetadata> {
        // Structural Safeguard: Fail-closed if synthetic data is passed under REAL_COINBASE_API
        if (sourceType == DatasetSourceType.REAL_COINBASE_API) {
            val syntheticCount = rawCandles.count { it.isSynthetic }
            if (syntheticCount > 0) {
                throw IllegalStateException("CRITICAL DATA INTEGRITY FAILURE: Attempted to process $syntheticCount synthetic candles under REAL_COINBASE_API source type.")
            }
        }

        // 1. Deduplicate by timestamp
        val deduped = rawCandles.distinctBy { it.timestamp }

        // 2. Sort strictly ascending
        val sorted = deduped.sortedBy { it.timestamp }

        // 3. Count missing candle gaps
        var missingCount = 0
        for (i in 0 until sorted.size - 1) {
            val deltaMs = sorted[i + 1].timestamp - sorted[i].timestamp
            if (deltaMs > 60_000L) {
                val missedInGap = ((deltaMs / 60_000L) - 1).toInt()
                missingCount += missedInGap
            }
        }

        val actualStart = sorted.firstOrNull()?.timestamp ?: requestedStartMs
        val actualEnd = sorted.lastOrNull()?.timestamp ?: requestedEndMs
        val checksum = HistoricalDatasetMetadata.computeSha256(sorted)

        val metadata = HistoricalDatasetMetadata(
            sourceType = sourceType,
            source = sourceType.displayName,
            symbol = "BTC-USD",
            resolution = "1m",
            requestedStartEpochMs = requestedStartMs,
            requestedEndEpochMs = requestedEndMs,
            actualStartEpochMs = actualStart,
            actualEndEpochMs = actualEnd,
            totalCandlesCount = sorted.size,
            missingCandlesCount = missingCount,
            retrievalTimestampEpochMs = System.currentTimeMillis(),
            datasetSha256Checksum = checksum
        )

        return Pair(sorted, metadata)
    }

    /**
     * Fetch authentic historical 1-minute candles from Coinbase REST API with pagination.
     *
     * FAIL-CLOSED RESEARCH INTEGRITY RULE:
     * If real market data cannot be retrieved from Coinbase (network unavailable, rate limit, HTTP error),
     * this function FAILS CLOSED and throws [RealMarketDataUnavailableException].
     * Synthetic data is NEVER silently substituted in research mode.
     */
    suspend fun fetchCoinbaseCandles(
        startMs: Long,
        endMs: Long,
        onProgress: ((progressPercent: Int, status: String) -> Unit)? = null
    ): Pair<List<HistoricalCandle>, HistoricalDatasetMetadata> = withContext(Dispatchers.IO) {
        val rawCandles = mutableListOf<HistoricalCandle>()
        val chunkDurationMs = 300 * 60_000L // 300 1-minute candles per request max
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        var currentStart = startMs
        val totalSpan = (endMs - startMs).coerceAtLeast(1L)
        var networkFailureOccurred = false
        var failureReason = ""

        while (currentStart < endMs) {
            val currentEnd = (currentStart + chunkDurationMs).coerceAtMost(endMs)
            val startIso = isoFormat.format(Date(currentStart))
            val endIso = isoFormat.format(Date(currentEnd))

            val progress = (((currentStart - startMs).toDouble() / totalSpan.toDouble()) * 100.0).toInt().coerceIn(0, 95)
            onProgress?.invoke(progress, "Downloading Coinbase 1m OHLCV ($startIso to $endIso)...")

            try {
                val url = "https://api.exchange.coinbase.com/products/BTC-USD/candles?granularity=60&start=$startIso&end=$endIso"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "CryptoKalshiAI-Android")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val jsonArray = JSONArray(body)
                            // Coinbase format: [ [ time, low, high, open, close, volume ], ... ]
                            for (i in 0 until jsonArray.length()) {
                                val item = jsonArray.getJSONArray(i)
                                val timeSec = item.getLong(0)
                                val low = item.getDouble(1)
                                val high = item.getDouble(2)
                                val open = item.getDouble(3)
                                val close = item.getDouble(4)
                                val volume = item.getDouble(5)

                                rawCandles.add(
                                    HistoricalCandle(
                                        timestamp = timeSec * 1000L,
                                        open = open,
                                        high = high,
                                        low = low,
                                        close = close,
                                        volume = volume,
                                        isSynthetic = false
                                    )
                                )
                            }
                        }
                    } else {
                        networkFailureOccurred = true
                        failureReason = "HTTP ${response.code}: ${response.message}"
                    }
                }
            } catch (e: Exception) {
                networkFailureOccurred = true
                failureReason = e.message ?: "Network timeout/connection failure"
            }

            currentStart += chunkDurationMs
            kotlinx.coroutines.delay(120) // Respect rate limit
        }

        // FAIL-CLOSED AUDIT ENFORCEMENT: Never silently fallback to synthetic data
        if (rawCandles.isEmpty()) {
            throw RealMarketDataUnavailableException(
                "DATA UNAVAILABLE: Failed to retrieve authentic 1-minute BTC-USD candles from Coinbase ($failureReason). " +
                "Backtest aborted. No research records or metrics generated (Fail-closed research mode active)."
            )
        }

        // Structural Safeguard: Verify all retrieved candles are non-synthetic
        if (rawCandles.any { it.isSynthetic }) {
            throw IllegalStateException("CRITICAL DATA INTEGRITY FAILURE: Synthetic candles detected in Coinbase API ingestion pipeline.")
        }

        onProgress?.invoke(100, "Processing & hashing historical dataset...")
        sanitizeAndProcessCandles(rawCandles, startMs, endMs, sourceType = DatasetSourceType.REAL_COINBASE_API)
    }

    /**
     * Explicit Dev/Test Synthetic Benchmark Dataset Generator.
     * MUST ONLY be called when synthetic test mode is explicitly requested.
     * All output candles have isSynthetic = true and sourceType = SYNTHETIC_TEST_BENCHMARK.
     */
    fun generateSyntheticBenchmarkDataset(
        startMs: Long,
        endMs: Long,
        initialPrice: Double = 96420.0,
        seed: Long = 42L
    ): Pair<List<HistoricalCandle>, HistoricalDatasetMetadata> {
        val candles = mutableListOf<HistoricalCandle>()
        var currentPrice = initialPrice
        var currentMs = startMs
        val random = Random(seed)

        var minuteIndex = 0
        while (currentMs <= endMs) {
            val intradayCycle = sin(minuteIndex * 0.015) * 12.0
            val noise = (random.nextGaussian() * 8.5)
            val open = currentPrice
            val close = (open + intradayCycle + noise).coerceAtLeast(1000.0)
            val high = maxOf(open, close) + random.nextDouble() * 14.0
            val low = minOf(open, close) - random.nextDouble() * 14.0
            val volume = 15.0 + random.nextDouble() * 45.0

            candles.add(
                HistoricalCandle(
                    timestamp = currentMs,
                    open = Math.round(open * 100.0) / 100.0,
                    high = Math.round(high * 100.0) / 100.0,
                    low = Math.round(low * 100.0) / 100.0,
                    close = Math.round(close * 100.0) / 100.0,
                    volume = Math.round(volume * 100.0) / 100.0,
                    isSynthetic = true
                )
            )

            currentPrice = close
            currentMs += 60_000L
            minuteIndex++
        }

        return sanitizeAndProcessCandles(candles, startMs, endMs, sourceType = DatasetSourceType.SYNTHETIC_TEST_BENCHMARK)
    }
}
