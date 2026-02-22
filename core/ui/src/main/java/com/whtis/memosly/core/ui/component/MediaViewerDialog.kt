package com.whtis.memosly.core.ui.component

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.whtis.memosly.core.ui.R

data class ViewableMedia(
    val url: String,
    val isVideo: Boolean,
    val filename: String,
    val size: Long = 0,
)

@Composable
fun MediaViewerDialog(
    media: ViewableMedia,
    onDismiss: () -> Unit,
    headers: Map<String, String> = emptyMap(),
) {
    val context = LocalContext.current
    val whiteButton = IconButtonDefaults.iconButtonColors(contentColor = Color.White)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            // Content
            if (media.isVideo) {
                VideoPlayer(
                    url = media.url,
                    headers = headers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                )
            } else {
                AsyncImage(
                    model = media.url,
                    contentDescription = media.filename,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 64.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            // Close button (top-right)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp),
                colors = whiteButton,
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back))
            }

            // Bottom bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // File info
                val sizeText = formatSize(media.size)
                if (sizeText.isNotEmpty()) {
                    Text(
                        text = sizeText,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                    )
                } else {
                    Box(modifier = Modifier)
                }

                // Download button
                IconButton(
                    onClick = { downloadMedia(context, media, headers) },
                    colors = whiteButton,
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.save))
                }
            }
        }
    }
}

private fun downloadMedia(
    context: Context,
    media: ViewableMedia,
    headers: Map<String, String>,
) {
    try {
        val request = DownloadManager.Request(Uri.parse(media.url))
            .setTitle(media.filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, media.filename)
        headers.forEach { (key, value) ->
            request.addRequestHeader(key, value)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Downloading ${media.filename}", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.0f KB".format(kb)
        else -> "$bytes B"
    }
}
