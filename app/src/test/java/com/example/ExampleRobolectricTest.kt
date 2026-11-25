package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.Formatters
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GuruDesk", appName)
  }

  @Test
  fun `indian rupee formatting uses lakhs and thousands grouping`() {
    assertEquals("₹0", Formatters.formatInr(0.0))
    assertEquals("₹2,500", Formatters.formatInr(2500.0))
    assertEquals("₹1,25,000", Formatters.formatInr(125000.0))
  }

  @Test
  fun `whatsapp helper normalizes phone and formats reminder`() {
    assertEquals("919820112233", WhatsAppHelper.normalizeIndianPhone("98201 12233"))
    val msg = WhatsAppHelper.buildFeeReminderMessage(
      studentName = "Aarav Sharma",
      batchName = "Class 10th Science",
      monthDisplay = "November 2026",
      pendingAmount = 2500.0,
      instituteName = "VidyaSagar Coaching Academy",
    )
    assertTrue(msg.contains("Aarav Sharma"))
    assertTrue(msg.contains("₹2,500"))
    assertTrue(msg.contains("Class 10th Science"))
  }
}
