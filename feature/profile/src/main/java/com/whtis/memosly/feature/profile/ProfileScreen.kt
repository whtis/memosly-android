package com.whtis.memosly.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.whtis.memosly.core.network.SessionPreferences
import com.whtis.memosly.core.ui.R as UiR
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whtis.memosly.core.ui.component.ErrorContent
import com.whtis.memosly.core.ui.component.LoadingContent
import com.whtis.memosly.core.ui.theme.MemosShapes
import com.whtis.memosly.core.ui.theme.NotionRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileScreen(
    onBack: (() -> Unit)?,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navMode by viewModel.navModeFlow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.profile)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> LoadingContent(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorContent(
                message = uiState.error!!,
                onRetry = viewModel::loadProfile,
                modifier = Modifier.padding(padding),
            )
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Profile header
                    Surface(
                        shape = MemosShapes.Card,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = stringResource(UiR.string.avatar),
                                    modifier = Modifier.padding(16.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            uiState.user?.let { user ->
                                Text(
                                    text = user.nickname.ifBlank { user.username },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                if (user.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = user.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }

                    // Statistics
                    uiState.stats?.let { stats ->
                        ProfileCard {
                            Text(text = stringResource(UiR.string.statistics), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(UiR.string.total_memos, stats.memoDisplayTimestamps.size),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(UiR.string.tags_used, stats.tagCount.size),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    // Language
                    ProfileCard {
                        Text(text = stringResource(UiR.string.language), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LanguagePicker()
                    }

                    // Navigation Mode
                    ProfileCard {
                        Text(text = stringResource(UiR.string.nav_mode), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        NavModePicker(
                            currentMode = navMode,
                            onModeChange = viewModel::setNavMode,
                        )
                    }

                    // Access Tokens
                    ProfileCard {
                        Text(text = stringResource(UiR.string.access_tokens), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.accessTokens.isEmpty()) {
                            Text(
                                text = "\u2014",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        uiState.accessTokens.forEach { token ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = token.description, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = stringResource(UiR.string.issued_at, token.issuedAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteAccessToken(token.accessToken) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(UiR.string.delete), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Webhooks
                    ProfileCard {
                        Text(text = stringResource(UiR.string.webhooks), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.webhooks.isEmpty()) {
                            Text(
                                text = "\u2014",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        uiState.webhooks.forEach { webhook ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = webhook.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = webhook.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteWebhook(webhook.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(UiR.string.delete), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Admin section
                    if (uiState.isAdmin) {
                        ProfileCard {
                            Text(text = stringResource(UiR.string.instance), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.workspaceProfile?.let { profile ->
                                Text(
                                    text = stringResource(UiR.string.version_label, profile.version),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = stringResource(UiR.string.mode_label, profile.mode),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(UiR.string.identity_providers),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (uiState.identityProviders.isEmpty()) {
                                Text(
                                    text = stringResource(UiR.string.no_identity_providers),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                uiState.identityProviders.forEach { idp ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Text(text = idp.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = stringResource(UiR.string.type_label, idp.type.name),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // About - Support Author
                    val context = LocalContext.current
                    Surface(
                        shape = MemosShapes.Card,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.logSupportAuthorClick()
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/whtis/memosly-android"),
                                )
                                context.startActivity(intent)
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = NotionRed,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(UiR.string.support_author),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = stringResource(UiR.string.support_author_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Check for Updates
                    ProfileCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(UiR.string.check_for_updates),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(UiR.string.current_version, uiState.appVersion),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            when (uiState.updateCheckState) {
                                is UpdateCheckState.Idle -> {
                                    OutlinedButton(onClick = viewModel::checkForUpdate) {
                                        Text(stringResource(UiR.string.check))
                                    }
                                }
                                is UpdateCheckState.Checking -> {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                                is UpdateCheckState.UpToDate -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(UiR.string.up_to_date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                is UpdateCheckState.Error -> {
                                    TextButton(onClick = viewModel::checkForUpdate) {
                                        Text(stringResource(UiR.string.retry))
                                    }
                                }
                                is UpdateCheckState.UpdateAvailable -> {}
                            }
                        }

                        if (uiState.updateCheckState is UpdateCheckState.UpdateAvailable) {
                            val updateState = uiState.updateCheckState as UpdateCheckState.UpdateAvailable
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(UiR.string.update_available, updateState.latestVersion),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (updateState.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = updateState.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 10,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=com.whtis.memosly"),
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(UiR.string.download_update))
                            }
                        }

                        if (uiState.updateCheckState is UpdateCheckState.Error) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(UiR.string.update_check_failed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    // Sign out
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.signOut { onSignOut() } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(UiR.string.sign_out))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MemosShapes.Card,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            content()
        }
    }
}

private data class NavModeOption(
    val mode: String,
    val labelResId: Int,
)

private val navModeOptions = listOf(
    NavModeOption(SessionPreferences.NAV_MODE_DRAWER, UiR.string.nav_mode_drawer),
    NavModeOption(SessionPreferences.NAV_MODE_TABS, UiR.string.nav_mode_tabs),
)

@Composable
private fun NavModePicker(
    currentMode: String,
    onModeChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentOption = navModeOptions.find { it.mode == currentMode }
        ?: navModeOptions.first()

    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(currentOption.labelResId))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            navModeOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelResId)) },
                    onClick = {
                        expanded = false
                        onModeChange(option.mode)
                    },
                    trailingIcon = if (option.mode == currentMode) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                )
            }
        }
    }
}

private data class LanguageOption(
    val tag: String,
    val labelResId: Int,
)

private val languageOptions = listOf(
    LanguageOption("", UiR.string.language_system_default),
    LanguageOption("en", UiR.string.language_english),
    LanguageOption("zh", UiR.string.language_chinese),
)

@Composable
private fun LanguagePicker() {
    var expanded by remember { mutableStateOf(false) }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (currentLocales.isEmpty) "" else currentLocales.toLanguageTags()

    val currentOption = languageOptions.find { it.tag == currentTag }
        ?: languageOptions.first()

    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(currentOption.labelResId))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            languageOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelResId)) },
                    onClick = {
                        expanded = false
                        val localeList = if (option.tag.isEmpty()) {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(option.tag)
                        }
                        AppCompatDelegate.setApplicationLocales(localeList)
                    },
                    trailingIcon = if (option.tag == currentTag) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                )
            }
        }
    }
}
