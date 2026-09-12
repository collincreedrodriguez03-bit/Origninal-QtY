package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class CoinbaseSpotPriceResponse(
    @Json(name = "data") val data: CoinbasePriceData
)

@JsonClass(generateAdapter = true)
data class CoinbasePriceData(
    @Json(name = "base") val base: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "amount") val amount: String
)

interface CoinbaseService {
    @GET("v2/prices/BTC-USD/spot")
    suspend fun getBtcSpotPrice(): CoinbaseSpotPriceResponse
}

object CoinbaseClient {
    private const val BASE_URL = "https://api.coinbase.com/"

    val service: CoinbaseService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(CoinbaseService::class.java)
    }
}
