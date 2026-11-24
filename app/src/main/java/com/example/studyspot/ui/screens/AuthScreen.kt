package com.example.studyspot.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.studyspot.data.model.*
import com.example.studyspot.viewmodel.StudySpotUiState
import com.example.studyspot.assistant.SeatAssistantSheet
import java.text.DateFormat
import java.util.Date

@Composable fun Auth(register: Boolean, submit: (String, String, String) -> Unit, link: () -> Unit) {
 var name by remember { mutableStateOf("") }
 var email by remember { mutableStateOf("") }
 var password by remember { mutableStateOf("") }
 var confirm by remember { mutableStateOf("") }
 var visible by remember { mutableStateOf(false) }
 val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center) {
  Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(60.dp)) {
   Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EventSeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
  }
  Text("StudySpot", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
  Text("Find your seat. Focus on studying.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 30.dp))
  Text(if (register) "Create your account" else "Welcome back", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
  Text(if (register) "Set up your local StudySpot account." else "Sign in to see your saved study spaces.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
  if (register) {
   OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(14.dp))
   Spacer(Modifier.height(12.dp))
  }
  OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) }, shape = RoundedCornerShape(14.dp), isError = register && email.isNotBlank() && !emailValid)
  Spacer(Modifier.height(12.dp))
  OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(if (register) "Password · 8+ characters" else "Password") }, singleLine = true, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) }, trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (visible) "Hide password" else "Show password") } }, shape = RoundedCornerShape(14.dp))
  if (register) {
   Spacer(Modifier.height(12.dp))
   OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label = { Text("Confirm password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) }, shape = RoundedCornerShape(14.dp), isError = confirm.isNotEmpty() && confirm != password)
  }
  Spacer(Modifier.height(20.dp))
  Button(onClick = { submit(if (register) name else "", email, password) }, Modifier.fillMaxWidth().height(52.dp), enabled = if (register) name.isNotBlank() && emailValid && password.length >= 8 && password == confirm else emailValid && password.isNotBlank(), shape = RoundedCornerShape(14.dp)) {
   Text(if (register) "Create account" else "Sign in")
  }
  TextButton(onClick = link, Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)) { Text(if (register) "Already have an account? Sign in" else "New to StudySpot? Create an account") }
  Text("Local demo account · data stays on this device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp))
 }
}

