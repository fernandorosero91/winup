package com.winup.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winup.data.WinUpData
import com.winup.data.WinUpSession
import com.winup.data.WithdrawalResult
import com.winup.money.formatCopWhole
import com.winup.ui.theme.LocalWinUpColors
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val quickWithdrawals = listOf(
    BigDecimal("20000"),
    BigDecimal("50000"),
    BigDecimal("100000"),
    BigDecimal("200000"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSheet(
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val extras = LocalWinUpColors.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("20000") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val available = WinUpData.balance
    val amount = amountText.toBigDecimalOrNull()
    val valid = amount != null && amount >= BigDecimal("5000") && amount <= available

    ModalBottomSheet(
        onDismissRequest = { if (!loading) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = extras.muted.copy(alpha = 0.45f))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WithdrawHeader(enabled = !loading, onDismiss = onDismiss)

            Text(
                text = "Elige el monto a retirar",
                color = extras.muted,
                fontSize = 11.sp,
            )

            AmountRow(values = quickWithdrawals, selected = amount, onSelect = { amountText = it })

            OutlinedTextField(
                value = amountText,
                onValueChange = { value -> amountText = value.filter(Char::isDigit).take(6) },
                label = { Text("Importe (COP)") },
                prefix = { Text("$", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                isError = amountText.isNotBlank() && !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Mínimo $ 5.000 · Disponible ${formatCopWhole(available)}",
                color = extras.muted,
                fontSize = 9.sp,
            )

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        when (val result = WinUpData.withdraw(WinUpSession.api, amount ?: BigDecimal.ZERO)) {
                            is WithdrawalResult.Done -> {
                                loading = false
                                onMessage(result.message)
                            }
                            is WithdrawalResult.Failed -> {
                                loading = false
                                error = result.message
                            }
                        }
                    }
                },
                enabled = valid && !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = if (loading) {
                        "Procesando..."
                    } else {
                        "Retirar ${formatCopWhole(amount ?: BigDecimal.ZERO)}"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            TextButton(
                onClick = onDismiss,
                enabled = !loading,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Cancelar",
                    color = extras.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
