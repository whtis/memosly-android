package com.whtis.memosly.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.whtis.memosly.core.network.SessionPreferences
import com.whtis.memosly.core.ui.theme.MemosTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var sessionPreferences: SessionPreferences

    private val sharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        sharedText.value = extractSharedText(intent)

        setContent {
            MemosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MemosNavHost(
                        sessionPreferences = sessionPreferences,
                        sharedTextState = sharedText,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedText(intent)?.let { sharedText.value = it }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        // Many share sources (browsers, news apps) put the title in EXTRA_SUBJECT and
        // the URL in EXTRA_TEXT. Combine them so the captured memo keeps both pieces.
        val combined = when {
            subject.isEmpty() -> text
            text.isEmpty() -> subject
            text.contains(subject) -> text
            else -> "$subject\n$text"
        }
        return combined.takeIf { it.isNotBlank() }
    }
}
