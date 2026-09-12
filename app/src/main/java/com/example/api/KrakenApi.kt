package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class KrakenTickerResult(
    @Json(name = "c") val lastTrade: List<String>? = null, // [price, lot volume]
    @Json(name = "a") val ask: List<String>? = null,
    @Json(name = "b") val bid: List<String>? = null,
    @Json(name = "v") val volume: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class KrakenTickerResponse(
    @Json(name = "error") val error: List<String>? = null,
    @Json(name = "result") val result: Map<String, KrakenTickerResult>? = null
)

interface KrakenService {
    @GET("0/public/Ticker")
    suspend fun getBtcTicker(@Query("pair") pair: String = "XBTUSD"): KrakenTickerResponse
}

object KrakenClient {
    private const val BASE_URL = "https://api.kraken.com/"

    val service: KrakenService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(KrakenService::class.java)
    }
}
