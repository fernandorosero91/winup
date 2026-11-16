package com.winup.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.winup.R
import com.winup.data.ErrorDto
import com.winup.data.LoginRequest
import com.winup.data.RegisterRequest
import com.winup.data.WinUpSession
import com.winup.money.formatCop
import com.winup.ui.components.WinUpLogo
import com.winup.ui.theme.LocalWinUpColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Composable
fun AuthScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAuthenticated: () -> Unit,
) {
    val extras = LocalWinUpColors.current
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (darkTheme) Color(0xFF101B2B) else MaterialTheme.colorScheme.background,
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        Image(
            painter = painterResource(R.drawable.banner_futbol_accion),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (darkTheme) 1f else 0.92f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (darkTheme) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xF0101B2B),
                                Color(0xB8101B2B),
                                Color(0x8C101B2B),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xE6F8F9FA),
                                Color(0x80FFFFFF),
                                Color(0x66F8F9FA),
                                Color(0xCCF8F9FA),
                            ),
                        )
                    },
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WinUpLogo(
                large = true,
                onDark = darkTheme,
            )
            Text(
                text = if (register) "Crea tu cuenta" else "Inicia sesión",
                color = if (darkTheme) Color.White else MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                text = if (register) {
                    "Recibe 100.000 COP de saldo ficticio y empieza a practicar."
                } else {
                    "Consulta tus apuestas, tu cartera y los partidos en vivo."
                },
                color = if (darkTheme) Color(0xFFCBD5E1) else extras.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 22.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(20.dp),
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(20.dp),
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (register) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                    )
                }

                Button(
                    onClick = {
                        if (loading) return@Button
                        loading = true
                        error = null
                        scope.launch {
                            try {
                                val response = if (register) {
                                    WinUpSession.api.register(
                                        RegisterRequest(
                                            email = email.trim(),
                                            password = password,
                                            displayName = name.trim(),
                                        ),
                                    )
                                } else {
                                    WinUpSession.api.login(
                                        LoginRequest(email.trim(), password),
                                    )
                                }
                                WinUpSession.save(response)
                                onAuthenticated()
                            } catch (exception: Exception) {
                                error = authError(exception)
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(2.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = if (register) "Crear cuenta" else "Entrar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                TextButton(
                    onClick = {
                        register = !register
                        error = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = if (register) {
                            "¿Ya tienes cuenta? Inicia sesión"
                        } else {
                            "¿No tienes cuenta? Regístrate"
                        },
                        color = extras.muted,
                        fontSize = 11.sp,
                    )
                }
            }

            Text(
                text = "WINUP · Cada segundo importa",
                color = if (darkTheme) Color(0xFFCBD5E1) else extras.subtle,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .zIndex(2f),
        ) {
            Icon(
                imageVector = if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = if (darkTheme) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                tint = if (darkTheme) Color.White else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

private fun authError(exception: Exception): String {
    val http = exception as? HttpException
    if (http != null) {
        val body = http.response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            runCatching {
                return Json { ignoreUnknownKeys = true }
                    .decodeFromString<ErrorDto>(body)
                    .message
            }
        }
        if (http.code() == 401) {
            return "Correo o contraseña incorrectos"
        }
    }
    return "No se pudo conectar con el servidor"
}
