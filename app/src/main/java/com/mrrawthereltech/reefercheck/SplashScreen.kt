package com.mrrawthereltech.reefercheck

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    // Dark mode colors
    val backgroundColor = Color(0xFF0D1117)
    val containerOutlineColor = Color(0xFF00BFFF)
    val checkmarkColor = Color(0xFF39FF14)
    val textColor = Color.White
    val subTextColor = Color(0xFFA0A0A0)

    var containerVisible by remember { mutableStateOf(false) }
    var checkmarkAlpha by remember { mutableStateOf(0f) }
    var showTitle by remember { mutableStateOf(false) }
    var subTextAlpha by remember { mutableStateOf(0f) }

    val containerOffset by animateDpAsState(
        targetValue = if (containerVisible) 0.dp else (-300).dp,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "containerOffset"
    )

    val animatedCheckmarkAlpha by animateFloatAsState(
        targetValue = checkmarkAlpha,
        animationSpec = tween(durationMillis = 800),
        label = "checkmarkAlpha"
    )

    val animatedSubTextAlpha by animateFloatAsState(
        targetValue = subTextAlpha,
        animationSpec = tween(durationMillis = 1000),
        label = "subTextAlpha"
    )

    LaunchedEffect(Unit) {
        delay(300L)
        delay(1100L)
        delay(800L)
        showTitle = true
        delay(1500L) // Wait for typewriter
        delay(2000L)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = containerOffset)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Container outline
                    val path = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.4f)
                        lineTo(size.width * 0.5f, size.height * 0.3f)
                        lineTo(size.width * 0.8f, size.height * 0.4f)
                        lineTo(size.width * 0.5f, size.height * 0.5f)
                        close()

                        moveTo(size.width * 0.2f, size.height * 0.4f)
                        lineTo(size.width * 0.2f, size.height * 0.7f)
                        lineTo(size.width * 0.5f, size.height * 0.8f)
                        lineTo(size.width * 0.5f, size.height * 0.5f)

                        moveTo(size.width * 0.8f, size.height * 0.4f)
                        lineTo(size.width * 0.8f, size.height * 0.7f)
                        lineTo(size.width * 0.5f, size.height * 0.8f)
                    }
                    drawPath(
                        path = path,
                        color = containerOutlineColor,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Checkmark
                    if (animatedCheckmarkAlpha > 0f) {
                        val checkPath = Path().apply {
                            moveTo(size.width * 0.35f, size.height * 0.55f)
                            lineTo(size.width * 0.5f, size.height * 0.7f)
                            lineTo(size.width * 0.75f, size.height * 0.35f)
                        }
                        drawPath(
                            path = checkPath,
                            color = checkmarkColor.copy(alpha = animatedCheckmarkAlpha),
                            style = Stroke(width = 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (showTitle) {
                TypewriterText(
                    text = "Reefer Check",
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
                // Invisible placeholder to keep layout stable
                Text("Reefer Check", style = MaterialTheme.typography.displaySmall, color = Color.Transparent)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Powered by M.R.Rawther",
                color = subTextColor.copy(alpha = animatedSubTextAlpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TypewriterText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    delayMillis: Long = 100
) {
    var textToDisplay by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        text.forEachIndexed { index, _ ->
            textToDisplay = text.substring(0, index + 1)
            delay(delayMillis)
        }
    }

    Text(
        text = textToDisplay,
        style = style
    )
}
