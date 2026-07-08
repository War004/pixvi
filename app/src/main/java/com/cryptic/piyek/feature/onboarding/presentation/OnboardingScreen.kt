package com.cryptic.piyek.feature.onboarding.presentation

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.cryptic.piyek.core.DominantColorExtractor
import com.cryptic.piyek.core.theme.getContrastTextColor
import com.cryptic.piyek.core.theme.getTonalButtonColor
import com.cryptic.piyek.core.content.data.model.Artwork
import kotlinx.coroutines.launch


@Preview
@Composable
fun MyScreen(){

}
@Composable
fun OnboardingScreen(
    innerPadding: PaddingValues,
    onboardingViewModel: OnboardingViewModel,
    colorExtractor: DominantColorExtractor
) {
    val currentArtwork by onboardingViewModel.currentArtwork.collectAsStateWithLifecycle()
    val defaultBg = MaterialTheme.colorScheme.background

    var currentFrame by remember { mutableStateOf(CarouselFrame(color = defaultBg)) }

    val clipboard: Clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { colorExtractor.clearCache() }
    }

    val context = LocalContext.current

    LaunchedEffect(currentArtwork) {
        val artwork = currentArtwork ?: return@LaunchedEffect

        val request = ImageRequest.Builder(context)
            .data(artwork.quality.firstPage.original)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)

        var newColor = defaultBg
        if (result is coil3.request.SuccessResult) {
            try {
                val bitmap = (result.image as? coil3.BitmapImage)?.bitmap
                if (bitmap != null) {
                    val colorInt = colorExtractor.extract(
                        input = bitmap, stride = 4, startYPercent = 0.90f, endYPercent = 1.0f
                    )
                    if (colorInt != android.graphics.Color.BLACK) newColor = Color(colorInt)
                }
            } catch (_: Exception) { /* Keep defaultBg */ }
        }

        currentFrame = CarouselFrame(artwork = artwork, color = newColor)
    }

    val masterTransition = updateTransition(targetState = currentFrame, label = "CarouselSync")

    val animatedBgColor by masterTransition.animateColor(
        transitionSpec = { tween(3000) },
        label = "BgColor"
    ) { it.color }

    // 1. Calculate the dynamic text and button colors based on the animated background
    val dynamicTextColor by masterTransition.animateColor(
        transitionSpec = { tween(3000) },
        label = "TextColor"
    ) { targetFrame ->
        // Calculate based on the TARGET frame, not the mid-animation color
        getContrastTextColor(targetFrame.color)
    }
    val dynamicButtonColor by masterTransition.animateColor(
        transitionSpec = { tween(3000) },
        label = "ButtonColor"
    ) { targetFrame ->
        val targetText = getContrastTextColor(targetFrame.color)
        getTonalButtonColor(targetFrame.color, targetText)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Box(modifier = Modifier.fillMaxWidth().weight(0.65f)) {
            masterTransition.AnimatedContent(
                transitionSpec = { fadeIn(tween(3000)) togetherWith fadeOut(tween(3000)) },
                contentKey = { frame -> frame.artwork?.id }
            ) { targetFrame ->
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onBackground)) {
                    if (targetFrame.artwork != null) {
                        AsyncImage(
                            model = targetFrame.artwork.quality.firstPage.original,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 16.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .clickable(
                                    onClick = {
                                        scope.launch {
                                            val clipData: ClipData =
                                                ClipData.newPlainText("User Name", targetFrame.artwork.user.userProfile)
                                            clipboard.setClipEntry(ClipEntry(clipData))
                                        }
                                    }
                                )
                        ) {
                            Text(
                                text = "@${targetFrame.artwork.user.userProfile}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        OnboardingControls(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .background(animatedBgColor),
            textColor = dynamicTextColor,
            buttonColor = dynamicButtonColor,
            onLoginClick = { onboardingViewModel.launchLoginPage() }
        )
    }
}



@Composable
fun OnboardingControls(
    modifier: Modifier = Modifier,
    textColor: Color,
    buttonColor: Color,
    onLoginClick: () -> String,
) {

    val context = LocalContext.current
    val customTabsIntent = remember { CustomTabsIntent.Builder().build() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter Piyek",
            style = MaterialTheme.typography.headlineLarge.copy(
                letterSpacing = 1.5.sp, // Premium spacing
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.15f), // Subtle depth
                    offset = Offset(0f, 6f),
                    blurRadius = 8f
                )
            ),
            fontWeight = FontWeight.Black, // The thickest standard weight
            color = textColor,
            modifier = Modifier.padding(top = 24.dp)
        )

        Text(
            text = "Using Pixiv",
            style = MaterialTheme.typography.titleMedium.copy(
                letterSpacing = 3.sp // Wider spacing for subtitles looks highly stylized
            ),
            fontWeight = FontWeight.SemiBold,
            color = textColor.copy(alpha = 0.6f), // Softer contrast
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.fillMaxWidth(0.5f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PRIMARY BUTTON (High Contrast)
                Button(
                    onClick = {
                        val redirectUri = onLoginClick().toUri()
                        try {
                            customTabsIntent.launchUrl(context, redirectUri)
                        }catch(e: ActivityNotFoundException){
                            Log.e("LoginScreen","Browser with custom tabs not available: ${e.message}")
                            try{
                                context.startActivity(Intent(Intent.ACTION_VIEW, redirectUri))
                            }catch (e2: ActivityNotFoundException) {
                                Log.e("LoginScreen", "Fallback browser also not found: ${e2.message}")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = buttonColor,
                            spotColor = buttonColor
                        ),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = textColor
                    )
                ) {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "OR",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.7f)
                )

                //Spacer(modifier = Modifier.height(16.dp))

                // SECONDARY BUTTON (Tonal Blend)
                OutlinedButton( // Changed from standard Button to OutlinedButton
                    onClick = {
                        //use the actual redirect link rather than a general endpoint
                        val redirectUri = "https://accounts.pixiv.net/signup".toUri()
                        val intent = Intent(Intent.ACTION_VIEW, redirectUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        }catch(e: ActivityNotFoundException){
                            Log.e("LoginScreen","No browser or app found to handle URL: ${e.message}")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, buttonColor.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textColor
                    )
                ) {
                    Text(
                        text = "+ new account",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class CarouselFrame(
    val artwork: Artwork? = null,
    val color: Color
)