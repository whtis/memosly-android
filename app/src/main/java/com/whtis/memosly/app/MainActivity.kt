package com.whtis.memosly.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.whtis.memosly.core.network.SessionPreferences
import com.whtis.memosly.core.ui.theme.MemosTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var sessionPreferences: SessionPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MemosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MemosNavHost(sessionPreferences = sessionPreferences)
                }
            }
        }
    }
}
