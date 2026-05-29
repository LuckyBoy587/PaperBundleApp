package com.example.ui.screens.login

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.util.LocalizedStrings
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.curLanguage.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            val name = account.displayName ?: "User"
            val email = account.email ?: ""
            val photoUrl = account.photoUrl?.toString() ?: ""
            
            viewModel.loginWithGoogleProfile(context, name, email, photoUrl, idToken) {
                Toast.makeText(context, "Logged in securely", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            }
        } catch (e: ApiException) {
            Log.e("PAPER_BUNDLE", "GoogleLoginScreen: Sign-In failed with status code: ${e.statusCode}", e)
            val friendlyError = when (e.statusCode) {
                7 -> "Network error. Please check your internet connection."
                10 -> "Developer configuration error (status code 10). Please verify signing certificate SHA-1 and Client ID match."
                12500 -> "Sign-in failed (status code 12500). Please check Google Play Services."
                12501 -> "Sign-in cancelled."
                else -> "Sign-In failed: ${e.localizedMessage ?: "Error code ${e.statusCode}"}"
            }
            viewModel.authError.value = friendlyError
        } catch (e: Exception) {
            Log.e("PAPER_BUNDLE", "GoogleLoginScreen: Unexpected error during Sign-In", e)
            viewModel.authError.value = "Unexpected error: ${e.localizedMessage}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, StitchBorder), RoundedCornerShape(32.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon / Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(colors = listOf(StitchIndigo, StitchPurple))),
                contentAlignment = Alignment.Center
            ) {
                Text("🏡", fontSize = 38.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Baski Family",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = StitchSlate800
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = LocalizedStrings.get("app_desc", language),
                fontSize = 14.sp,
                color = StitchSlate500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (authLoading) {
                CircularProgressIndicator(color = StitchIndigo)
            } else {
                Button(
                    onClick = {
                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        )
                            .requestEmail()
                            .requestIdToken("676948202486-o9k8m2rkth5d70vr573c5jhmk706l5ce.apps.googleusercontent.com")
                            .requestProfile()
                            .build()
                        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchIndigo)
                ) {
                    Text(
                        text = LocalizedStrings.get("sign_in_google", language),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            authError?.let { err ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = err, color = Color.Red, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
