package com.cochat.app.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cochat.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(authRepository: AuthRepository, onLoggedIn: () -> Unit, onGoToRegister: () -> Unit) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("CoChat", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Sign in to your team workspace", color = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.padding(bottom = 24.dp))

        error?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Email or mobile number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (identifier.isBlank() || password.isBlank()) {
                    error = "Enter your email/mobile and password."
                    return@Button
                }
                loading = true
                error = null
                scope.launch {
                    try {
                        authRepository.login(identifier.trim(), password)
                        onLoggedIn()
                    } catch (e: Exception) {
                        error = "Login failed. Check your credentials."
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(if (loading) "Signing in…" else "Sign in")
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? ", color = androidx.compose.ui.graphics.Color.Gray)
            Text(
                "Create one",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onGoToRegister),
            )
        }
    }
}

@Composable
fun RegisterScreen(authRepository: AuthRepository, onRegistered: () -> Unit, onGoToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var useEmail by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("CoChat", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Create your team account", color = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.padding(bottom = 24.dp))

        error?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(fullName, { fullName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentedChoice("Email", useEmail, { useEmail = true }, Modifier.weight(1f))
            SegmentedChoice("Mobile", !useEmail, { useEmail = false }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        if (useEmail) {
            OutlinedTextField(email, { email = it }, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        } else {
            OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile number") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(designation, { designation = it }, label = { Text("Designation (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (fullName.isBlank() || password.length < 6) {
                    error = "Full name is required and password must be at least 6 characters."
                    return@Button
                }
                if (useEmail && email.isBlank()) { error = "Enter your email address."; return@Button }
                if (!useEmail && mobile.isBlank()) { error = "Enter your mobile number."; return@Button }

                loading = true
                error = null
                scope.launch {
                    try {
                        authRepository.register(
                            fullName.trim(),
                            if (useEmail) email.trim() else null,
                            if (!useEmail) mobile.trim() else null,
                            password,
                            designation.trim(),
                        )
                        onRegistered()
                    } catch (e: Exception) {
                        error = "Registration failed."
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(if (loading) "Creating account…" else "Create account")
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? ", color = androidx.compose.ui.graphics.Color.Gray)
            Text("Sign in", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onGoToLogin))
        }
    }
}

@Composable
private fun SegmentedChoice(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = modifier)
}
