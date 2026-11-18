package dev.antomanc.tameki.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.antomanc.tameki.R
import dev.antomanc.tameki.core.ui.theme.Hanken

/** Warm paper used by the feature graphic (no alpha). */
internal val StorePaper = Color(0xFFF2F0EA)

/** Launcher background from the adaptive icon (`tameki_launcher_background`). */
internal val StoreIconBackground = Color(0xFF2E6B4A)

/** M3 large shape — same radius family as cards/sheets. */
private val AppIconShape = RoundedCornerShape(20.dp)

/**
 * Play Store icon: adaptive-icon foreground drawn on its leaf background, full square.
 * Play applies its own mask — no extra rounding here.
 */
@Composable
internal fun StoreIcon() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StoreIconBackground),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * Feature graphic: calm paper, sapling mark as an app-icon rounded square, name, one tagline.
 */
@Composable
internal fun StoreFeatureGraphic(tagline: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StorePaper)
            .padding(horizontal = 96.dp, vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(AppIconShape)
                    .background(StoreIconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Tameki",
                color = Color(0xFF1C1D1A),
                fontFamily = Hanken,
                fontWeight = FontWeight.Light,
                fontSize = 64.sp,
                letterSpacing = (-0.02).sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = tagline,
                color = Color(0xFF1C1D1A).copy(alpha = 0.72f),
                fontFamily = Hanken,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
            )
        }
    }
}
