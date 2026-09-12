package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class KalshiMarketDto(
    @Json(name = "ticker") val ticker: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "yes_bid") val yesBid: Int? = null,
    @Json(name = "yes_ask") val yesAsk: Int? = null,
    @Json(name = "no_bid") val noBid: Int? = null,
    @Json(name = "no_ask") val noAsk: Int? = null,
    @Json(name = "last_price") val lastPrice: Int? = null,
    @Json(name = "floor_strike") val floorStrike: Double? = null,
    @Json(name = "cap_strike") val capStrike: Double? = null,
    @Json(name = "strike_price") val strikePrice: Double? = null,
    @Json(name = "expiration_time") val expirationTime: String? = null,
    @Json(name = "close_time") val closeTime: String? = null
)

@JsonClass(generateAdapter = true)
data class KalshiMarketsResponse(
    @Json(name = "markets") val markets: List<KalshiMarketDto>? = null,
    @Json(name = "cursor") val cursor: String? = null
)

interface KalshiService {
    @GET("trade-api/v2/markets")
    suspend fun getBtc15mMarkets(
        @Query("series_ticker") seriesTicker: String = "KXBTC15M",
        @Query("status") status: String = "open"
    ): KalshiMarketsResponse
}

object KalshiClient {
    private const val BASE_URL = "https://api.elections.kalshi.com/"

    val service: KalshiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(KalshiService::class.java)
    }
}
