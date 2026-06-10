package com.example.perkapp.ui.screens

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
import com.example.perkapp.di.Injection
import com.example.perkapp.network.LoginRequest
import com.example.perkapp.viewmodel.AuthViewModel
import com.example.perkapp.viewmodel.AuthViewModelFactory
import com.example.perkapp.viewmodel.AuthState

/**
 * FUNGSI: LoginScreen
 * TUJUAN: Menyediakan antarmuka visual (UI) bagi pengguna untuk memasukkan kredensial 
 * (Email dan Password) guna memverifikasi identitas mereka.
 * 
 * ALUR LOGIKA PENGERJAAN:
 * 1. Layar ini "berlangganan" (subscribe) ke `loginState` milik `AuthViewModel`.
 * 2. Ia mengumpulkan teks dari dua kolom input (`OutlinedTextField`).
 * 3. Ketika tombol Login diklik, nilai dari kedua kolom diserahkan ke ViewModel.
 * 4. Jika ViewModel mengubah status menjadi `Loading`, tombol di-disable dan Spinner berputar.
 * 5. Jika ViewModel mengubah status menjadi `Success`, *LaunchedEffect* terpanggil dan 
 *    menjalankan fungsi `onLoginSuccess` (pindah halaman).
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
