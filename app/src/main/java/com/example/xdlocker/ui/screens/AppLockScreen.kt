package com.example.xdlocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme // Added import
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xdlocker.viewmodel.AppLockViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Ensured import is present

@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter PIN to Unlock")
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { newValue -> pin = newValue }, // Clarified lambda parameter
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.errorMessage != null) {
                 Text(text = uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                 Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    viewModel.verifyPin(pin, onUnlockSuccess)
                }
            ) {
                Text("Unlock")
            }
        }
    }
}
