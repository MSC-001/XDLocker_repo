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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xdlocker.viewmodel.AppLockViewModel

@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    // You might want to collect some state from the ViewModel, e.g., error messages
    // val uiState by viewModel.uiState.collectAsState()

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it) // Apply padding from Scaffold
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter PIN to Unlock")
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            // if (uiState.errorMessage != null) {
            //     Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            //     Spacer(modifier = Modifier.height(8.dp))
            // }
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
