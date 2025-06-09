package com.ernestoyaquello.dragdropswipelazycolumn.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun ThemedPreview(
    content: @Composable () -> Unit,
) {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Preview(name = "A (Default)")
@Preview(name = "B (Dark theme)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "C (Bigger font)", fontScale = 1.75f)
internal annotation class MultiPreview