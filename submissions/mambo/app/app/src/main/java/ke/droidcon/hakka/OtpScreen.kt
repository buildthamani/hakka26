package ke.droidcon.hakka

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OtpScreen() {
    val digits = remember { mutableStateListOf("", "", "", "", "", "") }
    var focused by remember { mutableStateOf(-1) }
    var message by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val firstBox = remember { FocusRequester() }

    LaunchedEffect(Unit) { firstBox.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Enter your code", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(8.dp))
        Text("A 6-digit code has been sent to your phone.", fontSize = 15.sp, color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until 6) {
                val digit = digits[i]
                val bg = if (digit != "") Color(0xFF12361B) else if (focused == i) Color.White else Color(0xFFEEEEEE)
                val outline = if (focused == i && digit == "") Color.Black else bg
                BasicTextField(
                    value = digit,
                    onValueChange = { text ->
                        val newDigit = text.filter { it.isDigit() }.takeLast(1)
                        digits[i] = newDigit
                        if (newDigit != "" && i < 5) focusManager.moveFocus(FocusDirection.Next)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .size(46.dp)
                        .then(if (i == 0) Modifier.focusRequester(firstBox) else Modifier)
                        .testTag("otp_digit_${i + 1}")
                        .onFocusChanged { if (it.isFocused) focused = i }
                        .background(bg, CircleShape)
                        .border(2.dp, outline, CircleShape),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (digit == "") Text("-", color = Color.Gray, fontSize = 20.sp)
                            inner()
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Didn't receive the code?", fontSize = 14.sp, color = Color.Gray)
            TextButton(onClick = { message = "" }, modifier = Modifier.testTag("otp_resend")) {
                Text("Resend", color = Color(0xFF12361B), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(message, modifier = Modifier.testTag("otp_status"), color = Color.Black)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { message = checkCode(digits.joinToString("")) },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF12361B)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("otp_submit"),
        ) {
            Text("Submit", fontSize = 16.sp)
        }
    }
}
