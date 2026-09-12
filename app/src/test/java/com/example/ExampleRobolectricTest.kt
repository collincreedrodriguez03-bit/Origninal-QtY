package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.PricePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BTC Co-Pilot 2.0", appName)
  }

  @Test
  fun `test price point data structure`() {
    val point = PricePoint(
      timeMillis = System.currentTimeMillis(),
      price = 70500.0
    )
    assertEquals(70500.0, point.price, 0.001)
    assertTrue(point.timeMillis > 0L)
  }
}
