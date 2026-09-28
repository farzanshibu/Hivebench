package com.hivebench.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hivebench.app.ui.neo.MaterialTheme
import com.hivebench.app.ui.neo.Text

/** The Hivebench wordmark: a yellow "HIVE" pill followed by "BENCH". */
@Composable
fun HivebenchWordmark(modifier: Modifier = Modifier, fontSize: TextUnit = 18.sp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TitlePill("HIVE", NeoLime, fontSize = fontSize)
        Text("BENCH", fontWeight = FontWeight.Black, fontSize = fontSize, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** A loud uppercase label on a coloured pill with an ink outline and hard shadow (screen titles, brand). */
@Composable
fun TitlePill(text: String, color: Color, modifier: Modifier = Modifier, fontSize: TextUnit = 18.sp) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, cornerRadius = 8.dp)
            .background(color, shape)
            .border(2.dp, NeoBlack, shape)
            .padding(horizontal = 9.dp, vertical = 2.dp),
    ) {
        Text(text.uppercase(), fontWeight = FontWeight.Black, fontSize = fontSize, color = NeoBlack, letterSpacing = 0.5.sp)
    }
}

/** Small coloured square that keys a section header to its accent colour. */
@Composable
fun SectionSquare(color: Color, modifier: Modifier = Modifier) {
    val outline = if (isSystemInDarkTheme()) NeoCream else NeoBlack
    Box(
        modifier
            .size(16.dp)
            .background(color, RoundedCornerShape(3.dp))
            .border(1.5.dp, outline, RoundedCornerShape(3.dp)),
    )
}

/** Section header: coloured square + bold title, as used on settings-style cards. */
@Composable
fun SectionHeader(title: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionSquare(color)
        Text(title, fontWeight = FontWeight.Black, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
