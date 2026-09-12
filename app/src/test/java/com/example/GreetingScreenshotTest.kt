package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.live.LiveFeatureSnapshot
import com.example.data.live.LivePredictionEngine
import com.example.data.live.LivePredictionLogRecord
import com.example.ui.ConnectionState
import com.example.ui.LiveSignalRadarCard
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun live_signal_card_screenshot() {
    val sampleSnapshot = LiveFeatureSnapshot(
        timestampMs = System.currentTimeMillis(),
        spotPrice = 42535.0,
        strikePrice = 42500.0,
        deltaToStrike = 35.0,
        rsi = 58.4,
        momentum = 8.2,
        ema9 = 42530.0,
        ema21 = 42525.5,
        emaSpread = 4.5,
        volatility = 45.0,
        dataFreshness = ConnectionState.CONNECTED,
        isStale = false
    )

    val samplePrediction = LivePredictionEngine.evaluate(
        features = sampleSnapshot,
        secondsRemaining = 222,
        historyTickCount = 30
    )

    composeTestRule.setContent {
      MaterialTheme {
        Surface(color = Color(0xFF121212)) {
          Box(modifier = Modifier.padding(12.dp)) {
            LiveSignalRadarCard(
                liveSignal = "UP",
                livePrediction = samplePrediction,
                deltaToStrike = 35.0
            )
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/live_signal_card.png")
  }
}


