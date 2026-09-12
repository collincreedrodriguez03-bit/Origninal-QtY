package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.example.data.live.LiveObservationDao
import com.example.data.live.LiveObservationEntity
import kotlinx.coroutines.launch

@Database(
    entities = [
        StrategyNote::class,
        ResearchPredictionEntity::class,
        LiveObservationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun strategyNoteDao(): StrategyNoteDao
    abstract fun researchLedgerDao(): ResearchLedgerDao
    abstract fun liveObservationDao(): LiveObservationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.strategyNoteDao()
                    // Populate default templates
                    dao.insertNote(
                        StrategyNote(
                            title = "Kalshi 15m Scalping Rule",
                            content = "1. Focus on the 'At-The-Money' (ATM) strike price.\n2. In the first 5 minutes of the 15-minute window, observe the trend direction.\n3. Between minutes 5 and 11, if the trend is strong and price moves away from the strike, look for cheap opposite contracts (priced at $0.10-$0.25) as hedges, or ride the trend.\n4. Avoid trading in the last 2 minutes due to extreme time decay (theta) unless playing a breakout.",
                            category = "Strategy"
                        )
                    )
                    dao.insertNote(
                        StrategyNote(
                            title = "ChatGPT / Claude Prompt for Predictions",
                            content = "Copy and use this prompt in external chats:\n\n\"I am trading 15-minute binary options on Kalshi for Bitcoin. Current BTC price: [INSERT_PRICE], strike price is [INSERT_STRIKE]. Time remaining in window: [INSERT_MINUTES] minutes. Recent 1-minute candle close directions: [INSERT_TREND]. Calculate the risk-reward ratio of buying YES at $[INSERT_YES_PRICE] vs NO at $[INSERT_NO_PRICE] assuming a normal distribution volatility.\"",
                            category = "AI Prompt"
                        )
                    )
                    dao.insertNote(
                        StrategyNote(
                            title = "Risk Management Formula",
                            content = "1. Never size more than 2-3% of your portfolio on a single 15-minute contract.\n2. Always secure profits if a contract you bought at $0.35 reaches $0.75+ before the final 2 minutes.\n3. Keep a log of winning vs. losing windows to analyze whether your win rate is higher in high-volatility or low-volatility environments.",
                            category = "Trade Plan"
                        )
                    )
                }
            }
        }
    }
}
