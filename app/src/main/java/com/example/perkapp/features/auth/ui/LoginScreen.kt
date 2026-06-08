package com.example.perkapp.features.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.perkapp.core.Injection
import com.example.perkapp.features.auth.api.LoginRequest

/**
 * LoginScreen — Halaman untuk masuk (login) ke dalam aplikasi.
 *
 * Di sini user menginput Email dan Password. Jika berhasil, akan dilempar ke Home.
 * Jika salah, akan muncul pesan error (misal: "Email salah" atau "Password salah").
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    // Kita minta Android membuatkan AuthViewModel yang bertugas mengurus logika login
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(Injection.provideAuthRepository(LocalContext.current))
    )
) {
    val context = LocalContext.current
    // Mengamati (observe) status login saat ini (Idle, Loading, Success, atau Error)
    val loginState by viewModel.loginState.collectAsState()

    // Variabel state untuk menyimpan ketikan user di kolom input
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Mengecek apakah tombol harus memunculkan animasi berputar (spinner loading)
    val isLoading = loginState is AuthState.Loading

    // Menangani kejadian (effect) jika status berubah menjadi Success
    LaunchedEffect(loginState) {
        if (loginState is AuthState.Success) {
            onLoginSuccess() // Pindah ke Home
        }
    }

    // Mengatur warna khusus untuk kolom teks agar senada dengan warna utama (primary) aplikasi
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    // Wadah paling luar untuk membungkus seluruh layar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Judul Halaman ---
            Text(
                text = "perkapp",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Silakan login untuk melanjutkan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // --- Menampilkan Pesan Error (Jika Ada) ---
            if (loginState is AuthState.Error) {
                Text(
                    text = (loginState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // --- Kolom Input Email ---
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- Kolom Input Password ---
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(), // Menyamarkan teks jadi bintang/titik
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Tombol Login ---
            Button(
                onClick = {
                    // Minta ViewModel untuk memproses data yang diinput
                    viewModel.login(LoginRequest(email = username, password = password))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !isLoading // Nonaktifkan tombol (tidak bisa diklik) jika sedang loading
            ) {
                if (isLoading) {
                    // Jika sedang loading, tampilkan indikator berputar
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        "Login",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- Tombol Pindah ke Halaman Daftar ---
            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Belum punya akun? Daftar di sini",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
