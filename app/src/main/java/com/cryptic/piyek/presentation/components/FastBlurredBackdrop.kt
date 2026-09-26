package com.cryptic.piyek.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.cryptic.piyek.MyApplication
import com.cryptic.piyek.domain.image.GetBlurredImageUseCase

@Composable
fun FastBlurredBackdrop(
    imageUrl: String,
    modifier: Modifier = Modifier,
    useCase: GetBlurredImageUseCase = rememberBlurredImageUseCase()
) {
    var currentBitmap by remember(imageUrl) {
        mutableStateOf(useCase.getSynchronouslyIfCached(imageUrl))
    }

    LaunchedEffect(imageUrl) {
        if (currentBitmap == null && imageUrl.isNotBlank()) {
            currentBitmap = useCase(imageUrl)
        }
    }

    Box(modifier = modifier) {
        currentBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun rememberBlurredImageUseCase(): GetBlurredImageUseCase {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        (context as MyApplication).container.getBlurredImageUseCase
    }
}
