package cz.b2brental.presentation.feature.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import cz.b2brental.R
import org.koin.androidx.compose.koinViewModel

/**
 * Obrazovka registrace firmy — formulář s validací IČO, e-mailu, telefonu a hesla.
 * Chyby se mapují z [RegisterErrorType] na string resources.
 * @param onNavigateToLogin callback pro přechod na přihlašovací obrazovku
 * @param viewModel ViewModel obrazovky registrace
 */
@Composable
public fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.auth_register_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.companyName,
            onValueChange = viewModel::onCompanyNameChange,
            label = { Text(stringResource(R.string.auth_register_company_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.inn,
            onValueChange = viewModel::onInnChange,
            label = { Text(stringResource(R.string.auth_register_inn)) },
            singleLine = true,
            isError = uiState.fieldErrors.containsKey(RegisterErrorType.INVALID_INN),
            supportingText = if (uiState.fieldErrors.containsKey(RegisterErrorType.INVALID_INN)) {
                { Text(stringResource(R.string.auth_register_error_inn)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.address,
            onValueChange = viewModel::onAddressChange,
            label = { Text(stringResource(R.string.auth_register_address)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        val emailHasError = uiState.fieldErrors.containsKey(RegisterErrorType.INVALID_EMAIL) ||
                uiState.fieldErrors.containsKey(RegisterErrorType.EMPTY_EMAIL)
        val emailErrorText = when {
            uiState.fieldErrors.containsKey(RegisterErrorType.EMPTY_EMAIL) ->
                stringResource(R.string.auth_register_error_email_empty)
            uiState.fieldErrors.containsKey(RegisterErrorType.INVALID_EMAIL) ->
                stringResource(R.string.auth_register_error_email)
            else -> ""
        }
        OutlinedTextField(
            value = uiState.adminEmail,
            onValueChange = viewModel::onAdminEmailChange,
            label = { Text(stringResource(R.string.auth_register_email)) },
            singleLine = true,
            isError = emailHasError,
            supportingText = if (emailHasError) { { Text(emailErrorText) } } else null,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.auth_register_password)) },
            singleLine = true,
            isError = uiState.fieldErrors.containsKey(RegisterErrorType.SHORT_PASSWORD),
            supportingText = if (uiState.fieldErrors.containsKey(RegisterErrorType.SHORT_PASSWORD)) {
                { Text(stringResource(R.string.auth_register_error_password)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.phone,
            onValueChange = viewModel::onPhoneChange,
            label = { Text(stringResource(R.string.auth_register_phone)) },
            singleLine = true,
            isError = uiState.fieldErrors.containsKey(RegisterErrorType.INVALID_PHONE),
            supportingText = if (uiState.fieldErrors.containsKey(RegisterErrorType.INVALID_PHONE)) {
                { Text(stringResource(R.string.auth_register_error_phone)) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.error ?: return,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (uiState.fieldErrors.containsKey(RegisterErrorType.UNKNOWN)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_register_error_unknown),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = viewModel::register,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(stringResource(R.string.auth_register_submit))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(R.string.auth_register_login))
        }
    }
}
