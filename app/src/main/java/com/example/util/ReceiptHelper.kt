package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.InstituteProfile
import java.io.File
import java.io.FileOutputStream

data class ReceiptData(
    val receiptNumber: String,
    val paidDate: String,
    val monthKey: String,
    val studentName: String,
    val fatherName: String,
    val phone: String,
    val batchName: String,
    val baseFee: Double,
    val discount: Double,
    val netMonthlyFee: Double,
    val amountPaidThisReceipt: Double,
    val totalPaidForMonth: Double,
    val balanceRemaining: Double,
    val paymentMode: String,
    val notes: String,
    val institute: InstituteProfile,
)

object ReceiptHelper {

    fun printReceipt(context: Context, receipt: ReceiptData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Printing is not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val jobName = "Fee_Receipt_${receipt.receiptNumber}"
        printManager.print(jobName, ReceiptPrintAdapter(context, receipt), PrintAttributes.Builder().build())
    }

    fun shareReceiptPdf(context: Context, receipt: ReceiptData) {
        try {
            val cacheDir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val pdfFile = File(cacheDir, "Receipt_${receipt.receiptNumber}.pdf")
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            drawReceiptOnCanvas(page.canvas, 595f, 842f, receipt)
            doc.finishPage(page)
            FileOutputStream(pdfFile).use { out -> doc.writeTo(out) }
            doc.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Fee Receipt ${receipt.receiptNumber} - ${receipt.studentName}"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Fee Receipt from ${receipt.institute.name} for ${receipt.studentName} (${DateUtils.formatMonthDisplay(receipt.monthKey)}): ${Formatters.formatInr(receipt.amountPaidThisReceipt)} paid via ${receipt.paymentMode}."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Fee Receipt"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share receipt: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private class ReceiptPrintAdapter(
        private val context: Context,
        private val receipt: ReceiptData,
    ) : PrintDocumentAdapter() {
        private var pdfDocument: PrintedPdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            pdfDocument = PrintedPdfDocument(context, newAttributes)
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("Receipt_${receipt.receiptNumber}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            val doc = pdfDocument ?: return
            val page = doc.startPage(0)
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                doc.close()
                pdfDocument = null
                return
            }
            drawReceiptOnCanvas(
                page.canvas,
                page.info.pageWidth.toFloat(),
                page.info.pageHeight.toFloat(),
                receipt
            )
            doc.finishPage(page)
            try {
                FileOutputStream(destination.fileDescriptor).use { out ->
                    doc.writeTo(out)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.toString())
            } finally {
                doc.close()
                pdfDocument = null
            }
        }
    }

    private fun drawReceiptOnCanvas(
        canvas: android.graphics.Canvas,
        width: Float,
        height: Float,
        receipt: ReceiptData,
    ) {
        val margin = 44f
        val contentWidth = width - margin * 2

        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#D8D3C8")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(margin - 12f, margin - 12f, width - margin + 12f, 680f, borderPaint)

        // Header Navy Bar
        val headerPaint = Paint().apply { color = Color.parseColor("#1B365D") }
        canvas.drawRect(margin - 12f, margin - 12f, width - margin + 12f, margin + 78f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.parseColor("#DBEAFE")
            textSize = 11f
            isAntiAlias = true
        }

        canvas.drawText(receipt.institute.name, margin + 6f, margin + 18f, titlePaint)
        canvas.drawText(receipt.institute.address, margin + 6f, margin + 38f, subPaint)
        canvas.drawText("Contact: ${receipt.institute.contactNumber}", margin + 6f, margin + 55f, subPaint)

        val badgePaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("FEE RECEIPT", width - margin, margin + 22f, badgePaint)

        val labelPaint = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 11.5f
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }

        var y = margin + 110f
        canvas.drawText("Receipt No: ${receipt.receiptNumber}", margin + 6f, y, valuePaint)
        val rightValue = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Date: ${DateUtils.formatDateDisplay(receipt.paidDate)}", width - margin, y, rightValue)

        y += 18f
        canvas.drawLine(margin, y, width - margin, y, dividerPaint)

        fun drawRow(label: String, value: String) {
            y += 26f
            canvas.drawText(label, margin + 6f, y, labelPaint)
            canvas.drawText(value, width - margin, y, rightValue)
        }

        drawRow("Student Name", receipt.studentName)
        drawRow("Father's Name", receipt.fatherName)
        drawRow("Phone", receipt.phone)
        drawRow("Batch", receipt.batchName)
        drawRow("Fee Month", DateUtils.formatMonthDisplay(receipt.monthKey))

        y += 18f
        canvas.drawLine(margin, y, width - margin, y, dividerPaint)

        drawRow("Batch Monthly Fee", Formatters.formatInr(receipt.baseFee))
        if (receipt.discount > 0.0) {
            drawRow("Monthly Discount", "- ${Formatters.formatInr(receipt.discount)}")
        }
        drawRow("Net Monthly Fee", Formatters.formatInr(receipt.netMonthlyFee))
        drawRow("Payment Mode", receipt.paymentMode)

        y += 22f
        val highlightBg = Paint().apply { color = Color.parseColor("#ECFDF5") }
        canvas.drawRect(margin, y, width - margin, y + 40f, highlightBg)
        val paidLabel = Paint(valuePaint).apply { color = Color.parseColor("#065F46") }
        val paidValue = Paint(rightValue).apply {
            color = Color.parseColor("#065F46")
            textSize = 16f
        }
        canvas.drawText("Amount Paid (This Receipt)", margin + 10f, y + 25f, paidLabel)
        canvas.drawText(Formatters.formatInr(receipt.amountPaidThisReceipt), width - margin - 10f, y + 25f, paidValue)

        y += 58f
        drawRow("Total Paid for ${DateUtils.formatMonthShort(receipt.monthKey)}", Formatters.formatInr(receipt.totalPaidForMonth))
        drawRow("Remaining Balance", Formatters.formatInr(receipt.balanceRemaining))

        if (receipt.notes.isNotBlank()) {
            y += 24f
            canvas.drawText("Notes: ${receipt.notes}", margin + 6f, y, labelPaint)
        }

        y = 640f
        canvas.drawLine(width - margin - 150f, y, width - margin, y, dividerPaint)
        val sigPaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Authorized Signatory", width - margin, y + 16f, sigPaint)
        canvas.drawText("Generated by GuruDesk", margin + 6f, y + 16f, labelPaint)
    }
}
