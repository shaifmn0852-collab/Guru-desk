package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppHelper {

    fun normalizeIndianPhone(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits"
            digits.startsWith("0") && digits.length == 11 -> "91${digits.drop(1)}"
            else -> digits
        }
    }

    fun buildFeeReminderMessage(
        studentName: String,
        batchName: String,
        monthDisplay: String,
        pendingAmount: Double,
        instituteName: String,
    ): String {
        val amountStr = Formatters.formatInr(pendingAmount)
        return buildString {
            append("Namaste,\n\n")
            append("This is a polite fee reminder from *$instituteName*.\n")
            append("Student: *$studentName*\n")
            append("Batch: $batchName\n")
            append("Month: $monthDisplay\n")
            append("Pending Fee Due: *$amountStr*\n\n")
            append("Kindly clear the dues at your earliest convenience. Please ignore if already paid.\n\n")
            append("Thank you,\n$instituteName")
        }
    }

    fun buildAbsentAlertMessage(
        studentName: String,
        batchName: String,
        dateDisplay: String,
        instituteName: String,
    ): String {
        return buildString {
            append("Namaste,\n\n")
            append("Attendance Update from *$instituteName*:\n")
            append("Your ward *$studentName* ($batchName) was marked *Absent* today ($dateDisplay).\n\n")
            append("Please let us know in case of any leave or concern.\n\n")
            append("Regards,\n$instituteName")
        }
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        val normalized = normalizeIndianPhone(phone)
        if (normalized.length < 10) {
            Toast.makeText(context, "Invalid WhatsApp phone number", Toast.LENGTH_SHORT).show()
            return
        }
        val encoded = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$normalized?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Could not open WhatsApp or browser on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
