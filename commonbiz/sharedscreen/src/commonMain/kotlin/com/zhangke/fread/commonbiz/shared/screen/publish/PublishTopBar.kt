package com.zhangke.fread.commonbiz.shared.screen.publish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhangke.framework.composable.SimpleIconButton
import com.zhangke.framework.composable.Toolbar
import com.zhangke.fread.localization.LocalizedString
import org.jetbrains.compose.resources.stringResource

@Composable
fun PublishTopBar(
    publishing: Boolean,
    publishEnabled: Boolean,
    onBackClick: () -> Unit,
    onPublishClick: () -> Unit,
    progress: Float? = null,
    progressLabel: String? = null,
) {
    Toolbar(
        title = stringResource(LocalizedString.sharedPublishBlogTitle),
        onBackClick = onBackClick,
        actions = {
            if (publishing) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (progressLabel != null) {
                        Text(
                            text = progressLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                        )
                    }
                    if (progress != null) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                SimpleIconButton(
                    onClick = onPublishClick,
                    enabled = publishEnabled,
                    tint = MaterialTheme.colorScheme.primary,
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Publish",
                )
            }
        },
    )
}
