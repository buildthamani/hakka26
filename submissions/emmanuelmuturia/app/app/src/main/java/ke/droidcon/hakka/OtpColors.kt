package ke.droidcon.hakka

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Palette from assets/colours.json ("otp" block). */
internal class OtpColors(
    val sheet: Color, val scrim: Color, val textPrimary: Color, val textSecondary: Color,
    val filledBg: Color, val filledText: Color, val focusedBg: Color, val focusedOutline: Color,
    val emptyBg: Color, val emptyOutline: Color, val placeholder: Color,
    val buttonBg: Color, val buttonText: Color, val buttonDisabledBg: Color,
    val link: Color, val success: Color, val error: Color,
)

internal val LightColors = OtpColors(
    Color(0xFFFFFFFF), Color(0x99000000), Color(0xFF111111), Color(0xFF5F6368),
    Color(0xFF12361B), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF111111),
    Color(0xFFF1F1F1), Color(0xFFE0E0E0), Color(0xFF9E9E9E),
    Color(0xFF12361B), Color(0xFFFFFFFF), Color(0xFFC9D1CB),
    Color(0xFF12361B), Color(0xFF1B7F3A), Color(0xFFC62828),
)

internal val DarkColors = OtpColors(
    Color(0xFF161A17), Color(0xCC000000), Color(0xFFF5F5F5), Color(0xFFB0B5B1),
    Color(0xFF2E7D45), Color(0xFFFFFFFF), Color(0xFF161A17), Color(0xFFF5F5F5),
    Color(0xFF262B27), Color(0xFF353B36), Color(0xFF6F7570),
    Color(0xFF2E7D45), Color(0xFFFFFFFF), Color(0xFF2A302B),
    Color(0xFF7BC795), Color(0xFF7BC795), Color(0xFFEF9A9A),
)

