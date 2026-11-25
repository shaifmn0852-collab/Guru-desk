package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TestEntity
import com.example.ui.theme.BatchAccentPalette
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.viewmodel.StudentOverview
import com.example.util.DateUtils
import com.example.util.Formatters
import com.example.util.ReceiptData
import com.example.util.ReceiptHelper

@Composable
fun BatchFormDialog(
    initialBatch: BatchEntity? = null,
    onDismiss: () -> Unit,
    onSave: (BatchEntity) -> Unit,
) {
    var name by remember { mutableStateOf(initialBatch?.name ?: "") }
    var subject by remember { mutableStateOf(initialBatch?.subject ?: "") }
    var timing by remember { mutableStateOf(initialBatch?.timing ?: "") }
    var feeText by remember {
        mutableStateOf(initialBatch?.monthlyFee?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var teacher by remember { mutableStateOf(initialBatch?.assignedTeacher ?: "") }
    var colorIndex by remember { mutableIntStateOf(initialBatch?.colorIndex ?: 0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBatch == null) "Create New Batch" else "Edit Batch") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Batch Name *") },
                    placeholder = { Text("e.g., Class 10th Science") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("batch_name_input"),
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject(s)") },
                    placeholder = { Text("e.g., Physics & Chemistry") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("batch_subject_input"),
                )
                OutlinedTextField(
                    value = timing,
                    onValueChange = { timing = it },
                    label = { Text("Timing") },
                    placeholder = { Text("e.g., 4:00 PM – 5:30 PM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("batch_timing_input"),
                )
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it.filter { ch -> ch.isDigit() || ch == '.' }; errorMsg = null },
                    label = { Text("Monthly Fee (₹) *") },
                    placeholder = { Text("e.g., 2500") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("batch_fee_input"),
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Assigned Teacher") },
                    placeholder = { Text("e.g., Prof. Rajesh Sharma") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("batch_teacher_input"),
                )
                Text(
                    text = "Batch Color Tag",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BatchAccentPalette.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (colorIndex == idx) 3.dp else 0.dp,
                                    color = if (colorIndex == idx) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .clickable { colorIndex = idx },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (colorIndex == idx) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected color",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fee = feeText.toDoubleOrNull()
                    when {
                        name.isBlank() -> errorMsg = "Please enter a batch name"
                        fee == null || fee < 0 -> errorMsg = "Please enter a valid monthly fee"
                        else -> onSave(
                            BatchEntity(
                                id = initialBatch?.id ?: 0L,
                                name = name.trim(),
                                subject = subject.trim().ifEmpty { "General" },
                                timing = timing.trim().ifEmpty { "Flexible" },
                                monthlyFee = fee,
                                assignedTeacher = teacher.trim().ifEmpty { "Faculty" },
                                colorIndex = colorIndex,
                                createdAt = initialBatch?.createdAt ?: System.currentTimeMillis(),
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_batch_button"),
            ) {
                Text(if (initialBatch == null) "Create Batch" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormDialog(
    batches: List<BatchEntity>,
    initialStudent: StudentEntity? = null,
    onDismiss: () -> Unit,
    onSave: (StudentEntity) -> Unit,
) {
    var name by remember { mutableStateOf(initialStudent?.name ?: "") }
    var fatherName by remember { mutableStateOf(initialStudent?.fatherName ?: "") }
    var phone by remember { mutableStateOf(initialStudent?.phone ?: "") }
    var selectedBatchId by remember {
        mutableStateOf(initialStudent?.batchId ?: batches.firstOrNull()?.id ?: 0L)
    }
    var joiningDate by remember {
        mutableStateOf(initialStudent?.joiningDate ?: DateUtils.todayIso())
    }
    var discountText by remember {
        mutableStateOf(
            initialStudent?.monthlyDiscount?.takeIf { it > 0 }?.toLong()?.toString() ?: ""
        )
    }
    var batchExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedBatch = batches.find { it.id == selectedBatchId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialStudent == null) "Add New Student" else "Edit Student") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Student Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_name_input"),
                )
                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_father_input"),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }; errorMsg = null },
                    label = { Text("Phone / WhatsApp Number *") },
                    placeholder = { Text("10-digit mobile number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_phone_input"),
                )
                ExposedDropdownMenuBox(
                    expanded = batchExpanded,
                    onExpandedChange = { batchExpanded = !batchExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedBatch?.let { "${it.name} (${Formatters.formatInr(it.monthlyFee)})" }
                            ?: "Select Batch",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Batch *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batchExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("student_batch_dropdown"),
                    )
                    ExposedDropdownMenu(
                        expanded = batchExpanded,
                        onDismissRequest = { batchExpanded = false },
                    ) {
                        batches.forEach { batch ->
                            DropdownMenuItem(
                                text = { Text("${batch.name} • ${Formatters.formatInr(batch.monthlyFee)}/mo") },
                                onClick = {
                                    selectedBatchId = batch.id
                                    batchExpanded = false
                                },
                            )
                        }
                    }
                }
                DatePickerField(
                    label = "Joining Date",
                    dateIso = joiningDate,
                    onDateSelected = { joiningDate = it },
                    testTag = "student_joining_date_input",
                )
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monthly Fee Discount (Optional)") },
                    prefix = { Text("₹ ") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_discount_input"),
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val discount = discountText.toDoubleOrNull() ?: 0.0
                    when {
                        name.isBlank() -> errorMsg = "Enter student name"
                        fatherName.isBlank() -> errorMsg = "Enter father's name"
                        phone.filter { it.isDigit() }.length < 10 -> errorMsg = "Enter valid 10-digit WhatsApp number"
                        selectedBatchId == 0L -> errorMsg = "Create/select a batch first"
                        else -> onSave(
                            StudentEntity(
                                id = initialStudent?.id ?: 0L,
                                name = name.trim(),
                                fatherName = fatherName.trim(),
                                phone = phone.trim(),
                                batchId = selectedBatchId,
                                joiningDate = joiningDate,
                                monthlyDiscount = discount.coerceAtLeast(0.0),
                                isActive = initialStudent?.isActive ?: true,
                                createdAt = initialStudent?.createdAt ?: System.currentTimeMillis(),
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_student_button"),
            ) {
                Text(if (initialStudent == null) "Enroll Student" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectFeeDialog(
    studentOverview: StudentOverview,
    monthKey: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, mode: String, paidDate: String, notes: String) -> Unit,
) {
    val defaultDue = studentOverview.pendingSelectedMonth.takeIf { it > 0 }
        ?: studentOverview.netMonthlyFee
    var amountText by remember { mutableStateOf(defaultDue.toLong().toString()) }
    var paymentMode by remember { mutableStateOf("UPI") }
    var paidDate by remember { mutableStateOf(DateUtils.todayIso()) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val modes = listOf("UPI", "Cash", "Bank Transfer")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collect Monthly Fee") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = studentOverview.student.name,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "${studentOverview.batch?.name ?: "Batch"} • ${DateUtils.formatMonthDisplay(monthKey)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Net Fee: ${Formatters.formatInr(studentOverview.netMonthlyFee)}", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "Pending: ${Formatters.formatInr(studentOverview.pendingSelectedMonth)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' }; errorMsg = null },
                    label = { Text("Amount Paying Now (₹) *") },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("collect_fee_amount_input"),
                )

                Text(
                    text = "Payment Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode) },
                            modifier = Modifier.testTag("payment_mode_${mode.lowercase().replace(" ", "_")}"),
                        )
                    }
                }

                DatePickerField(
                    label = "Payment Date",
                    dateIso = paidDate,
                    onDateSelected = { paidDate = it },
                    testTag = "collect_fee_date_input",
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Remarks (Optional)") },
                    placeholder = { Text("e.g., UPI Ref #4829") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMsg = "Enter a valid amount greater than ₹0"
                    } else {
                        onConfirm(amt, paymentMode, paidDate, notes.trim())
                    }
                },
                modifier = Modifier.testTag("confirm_collect_fee_button"),
            ) {
                Text("Record & Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun FeeReceiptDialog(
    receipt: ReceiptData,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val semantic = LocalSemanticColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(18.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = receipt.institute.logoText,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondary,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = receipt.institute.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Text(
                                        text = receipt.institute.contactNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = "RECEIPT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                        if (receipt.institute.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = receipt.institute.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Receipt No", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                receipt.receiptNumber,
                                style = MonoNumberStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Payment Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(DateUtils.formatDateDisplay(receipt.paidDate), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    ReceiptLine("Student", receipt.studentName)
                    ReceiptLine("Father's Name", receipt.fatherName)
                    ReceiptLine("Batch", receipt.batchName)
                    ReceiptLine("Fee Month", DateUtils.formatMonthDisplay(receipt.monthKey))
                    ReceiptLine("Payment Mode", receipt.paymentMode)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    ReceiptLine("Monthly Batch Fee", Formatters.formatInr(receipt.baseFee))
                    if (receipt.discount > 0) {
                        ReceiptLine("Discount", "- ${Formatters.formatInr(receipt.discount)}")
                    }
                    ReceiptLine("Net Monthly Fee", Formatters.formatInr(receipt.netMonthlyFee))

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = semantic.successContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Amount Paid",
                                style = MaterialTheme.typography.titleSmall,
                                color = semantic.onSuccessContainer,
                            )
                            Text(
                                Formatters.formatInr(receipt.amountPaidThisReceipt),
                                style = MonoNumberStyle.copy(
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = semantic.onSuccessContainer,
                            )
                        }
                    }

                    ReceiptLine("Remaining Balance", Formatters.formatInr(receipt.balanceRemaining))

                    if (receipt.notes.isNotBlank()) {
                        Text(
                            text = "Note: ${receipt.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { ReceiptHelper.printReceipt(context, receipt) },
                            modifier = Modifier.weight(1f).testTag("print_receipt_button"),
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print / PDF")
                        }
                        Button(
                            onClick = { ReceiptHelper.shareReceiptPdf(context, receipt) },
                            modifier = Modifier.weight(1f).testTag("share_receipt_button"),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF")
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestFormDialog(
    batches: List<BatchEntity>,
    initialTest: TestEntity? = null,
    onDismiss: () -> Unit,
    onSave: (TestEntity) -> Unit,
) {
    var name by remember { mutableStateOf(initialTest?.name ?: "") }
    var selectedBatchId by remember {
        mutableStateOf(initialTest?.batchId ?: batches.firstOrNull()?.id ?: 0L)
    }
    var dateIso by remember { mutableStateOf(initialTest?.date ?: DateUtils.todayIso()) }
    var totalMarksText by remember {
        mutableStateOf(initialTest?.totalMarks?.toLong()?.toString() ?: "100")
    }
    var expanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val selectedBatch = batches.find { it.id == selectedBatchId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTest == null) "Create New Test" else "Edit Test") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Test Name *") },
                    placeholder = { Text("e.g., Unit Test 1 - Mechanics") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("test_name_input"),
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedBatch?.name ?: "Select Batch",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Batch *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        batches.forEach { batch ->
                            DropdownMenuItem(
                                text = { Text(batch.name) },
                                onClick = {
                                    selectedBatchId = batch.id
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                DatePickerField(
                    label = "Test Date",
                    dateIso = dateIso,
                    onDateSelected = { dateIso = it },
                )
                OutlinedTextField(
                    value = totalMarksText,
                    onValueChange = { totalMarksText = it.filter { ch -> ch.isDigit() || ch == '.' }; errorMsg = null },
                    label = { Text("Total Marks *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("test_total_marks_input"),
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = totalMarksText.toDoubleOrNull()
                    when {
                        name.isBlank() -> errorMsg = "Enter test name"
                        selectedBatchId == 0L -> errorMsg = "Select a batch"
                        total == null || total <= 0 -> errorMsg = "Total marks must be > 0"
                        else -> onSave(
                            TestEntity(
                                id = initialTest?.id ?: 0L,
                                batchId = selectedBatchId,
                                name = name.trim(),
                                date = dateIso,
                                totalMarks = total,
                                createdAt = initialTest?.createdAt ?: System.currentTimeMillis(),
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_test_button"),
            ) {
                Text(if (initialTest == null) "Create Test" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
