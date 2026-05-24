package com.whtis.memosly.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.whtis.memosly.core.network.SessionPreferences
import com.whtis.memosly.core.ui.theme.MemosTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var sessionPreferences: SessionPreferences

    private val sharedTextFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        sharedTextFlow.value = extractSharedText(intent)

        setContent {
            val sharedText by sharedTextFlow.asStateFlow().collectAsState()
            MemosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MemosNavHost(
                        sessionPreferences = sessionPreferences,
                        sharedText = sharedText,
                        onSharedTextConsumed = { sharedTextFlow.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedText(intent)?.let { sharedTextFlow.value = it }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("text/") != true) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        return when {
            text.isEmpty() && subject.isEmpty() -> null
            text.isEmpty() -> subject
            subject.isEmpty() || subject == text -> text
            else -> "$subject\n$text"
        }
    }
}
