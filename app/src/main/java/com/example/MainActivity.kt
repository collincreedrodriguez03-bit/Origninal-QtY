package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.StrategyNoteRepository
import com.example.ui.TradingDashboard
import com.example.ui.TradingViewModel
import com.example.ui.TradingViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize database & repository
    val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
    val dao = database.strategyNoteDao()
    val obsDao = database.liveObservationDao()
    val repository = StrategyNoteRepository(dao, obsDao)

    // Instantiate ViewModel
    val viewModel = ViewModelProvider(
        this,
        TradingViewModelFactory(repository)
    )[TradingViewModel::class.java]

    setContent {
      MyApplicationTheme {
        TradingDashboard(viewModel = viewModel)
      }
    }
  }
}
