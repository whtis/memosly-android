package com.whtis.memosly.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.whtis.memosly.core.network.ServerVersion
import com.whtis.memosly.core.ui.R as UiR
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whtis.memosly.core.ui.component.LoadingContent
import com.whtis.memosly.core.ui.theme.MemosShapes

@Composable
internal fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.setLoginSuccessCallback(onLoginSuccess)
    }

    if (uiState.isRestoringSession) {
        LoadingContent(message = stringResource(UiR.string.restoring_session))
        return
    }

    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App icon
            Image(
                painter = painterResource(id = UiR.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(UiR.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(UiR.string.sign_in_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MemosShapes.Card,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Login mode selector
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LoginMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = uiState.loginMode == mode,
                                onClick = { viewModel.setLoginMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = LoginMode.entries.size,
                                ),
                            ) {
                                Text(
                                    when (mode) {
                                        LoginMode.PASSWORD -> stringResource(UiR.string.password)
                                        LoginMode.ACCESS_TOKEN -> stringResource(UiR.string.access_token_label)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.serverUrl,
                        onValueChange = viewModel::updateServerUrl,
                        label = { Text(stringResource(UiR.string.server_url)) },
                        placeholder = { Text(stringResource(UiR.string.server_url_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MemosShapes.Input,
                        supportingText = {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                ServerVersion.entries.forEachIndexed { index, version ->
                                    SegmentedButton(
                                        selected = uiState.serverVersion == version,
                                        onClick = { viewModel.updateServerVersion(version) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = ServerVersion.entries.size,
                                        ),
                                    ) {
                                        Text(
                                            when (version) {
                                                ServerVersion.V024 -> "v0.24"
                                                ServerVersion.V025 -> "v0.25"
                                                ServerVersion.V026 -> "v0.26+"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    if (uiState.loginMode == LoginMode.PASSWORD) {
                        OutlinedTextField(
                            value = uiState.username,
                            onValueChange = viewModel::updateUsername,
                            label = { Text(stringResource(UiR.string.username)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MemosShapes.Input,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::updatePassword,
                            label = { Text(stringResource(UiR.string.password)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MemosShapes.Input,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { viewModel.signIn() }),
                        )
                    } else {
                        OutlinedTextField(
                            value = uiState.accessToken,
                            onValueChange = viewModel::updateAccessToken,
                            label = { Text(stringResource(UiR.string.access_token_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MemosShapes.Input,
                            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                    Icon(
                                        imageVector = if (tokenVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (tokenVisible) "Hide token" else "Show token",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { viewModel.signIn() }),
                        )
                    }
                }
            }

            // Error message
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
            ) {
                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign in button
            Button(
                onClick = viewModel::signIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !uiState.isLoading,
                shape = MemosShapes.Card,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = stringResource(UiR.string.sign_in),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}
