package cz.b2brental.presentation.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import org.koin.androidx.compose.koinViewModel

/**
 * Obrazovka přihlášení — e-mail, heslo, tlačítko přihlásit, odkaz na registraci.
 * Chyby se mapují z [LoginErrorType] na string resources.
 * @param onNavigateToRegister callback pro přechod na obrazovku registrace
 * @param viewModel ViewModel obrazovky přihlášení
 */
@Composable
public fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.auth_login_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(stringResource(R.string.auth_login_email)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.auth_login_password)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.errorType != null || uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val errorText = when {
                uiState.errorType == LoginErrorType.EMPTY ->
                    stringResource(R.string.auth_login_error_empty)
                uiState.errorType == LoginErrorType.UNKNOWN ->
                    stringResource(R.string.auth_login_error_unknown)
                uiState.errorMessage != null -> uiState.errorMessage ?: ""
                else -> ""
            }
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val submitDescription = stringResource(R.string.auth_login_submit_description)
        Button(
            onClick = viewModel::login,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = submitDescription },
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(stringResource(R.string.auth_login_submit))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text(stringResource(R.string.auth_login_register))
        }
    }
}
