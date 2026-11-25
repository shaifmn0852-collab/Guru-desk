package com.example

import com.example.util.DateUtils
import com.example.util.Formatters
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun indianRupeeFormatting_usesIndianNumbering() {
    assertEquals("₹0", Formatters.formatInr(0.0))
    assertEquals("₹2,500", Formatters.formatInr(2500.0))
    assertEquals("₹1,25,000", Formatters.formatInr(125000.0))
  }

  @Test
  fun whatsappHelper_normalizesPhoneAndBuildsMessages() {
    assertEquals("919820112233", WhatsAppHelper.normalizeIndianPhone("98201 12233"))
    assertEquals("919820112233", WhatsAppHelper.normalizeIndianPhone("09820112233"))
    val feeMsg = WhatsAppHelper.buildFeeReminderMessage(
      studentName = "Aarav Sharma",
      batchName = "Class 10th Science",
      monthDisplay = "November 2026",
      pendingAmount = 2500.0,
      instituteName = "VidyaSagar Coaching Academy",
    )
    assertTrue(feeMsg.contains("Aarav Sharma"))
    assertTrue(feeMsg.contains("₹2,500"))
    assertTrue(feeMsg.contains("Class 10th Science"))

    val absMsg = WhatsAppHelper.buildAbsentAlertMessage(
      studentName = "Diya Patel",
      batchName = "Class 10th Science",
      dateDisplay = "25 Nov 2026",
      instituteName = "VidyaSagar Coaching Academy",
    )
    assertTrue(absMsg.contains("Diya Patel"))
    assertTrue(absMsg.contains("Absent"))
  }

  @Test
  fun dateUtils_shiftsMonthKeysCorrectly() {
    assertEquals("2026-10", DateUtils.shiftMonthKey("2026-11", -1))
    assertEquals("2027-01", DateUtils.shiftMonthKey("2026-11", 2))
    assertEquals(6, DateUtils.recentMonthKeys(6, "2026-11").size)
  }
}
