package com.whtis.memosly.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import com.whtis.memosly.core.common.SharedMediaBuffer
import com.whtis.memosly.core.network.SessionPreferences
import com.whtis.memosly.core.ui.theme.MemosTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var sessionPreferences: SessionPreferences
    @Inject lateinit var sharedMediaBuffer: SharedMediaBuffer

    private val sharedTextFlow = MutableStateFlow<String?>(null)
    private val sharedMediaFlow = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleShareIntent(intent)

        setContent {
            val sharedText by sharedTextFlow.asStateFlow().collectAsState()
            val hasSharedMedia by sharedMediaFlow.asStateFlow().collectAsState()
            MemosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MemosNavHost(
                        sessionPreferences = sessionPreferences,
                        sharedText = sharedText,
                        onSharedTextConsumed = { sharedTextFlow.value = null },
                        hasSharedMedia = hasSharedMedia,
                        onSharedMediaConsumed = { sharedMediaFlow.value = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /**
     * A share is either text or media, never both — media shares ignore any EXTRA_TEXT the
     * sender tacked on.
     */
    private fun handleShareIntent(intent: Intent?) {
        val media = extractSharedMedia(intent)
        if (media.isNotEmpty()) {
            sharedMediaBuffer.put(media)
            sharedMediaFlow.value = true
            return
        }
        extractSharedText(intent)?.let { sharedTextFlow.value = it }
    }

    private fun extractSharedMedia(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val type = intent.type ?: return emptyList()
        if (!type.startsWith("image/") && !type.startsWith("video/")) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND ->
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.filterNotNull()
                    .orEmpty()
            else -> emptyList()
        }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("text/") != true) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        // Many share sources (browsers, news apps) put the title in EXTRA_SUBJECT and
        // the URL in EXTRA_TEXT. Combine them so the captured memo keeps both pieces.
        return when {
            text.isEmpty() && subject.isEmpty() -> null
            text.isEmpty() -> subject
            subject.isEmpty() || subject == text || text.contains(subject) -> text
            else -> "$subject\n$text"
        }
    }
}
