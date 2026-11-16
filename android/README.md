# WINUP Android

App Android de WINUP con Kotlin y Jetpack Compose. Paquete: `com.winup`.

## Requisitos

- Android Studio con Android SDK 35.
- JDK 21 para Gradle.
- Emulador o dispositivo con Android 8.0 o superior.

## Abrir el proyecto

1. Abrir esta carpeta en Android Studio.
2. Sincronizar Gradle.
3. Crear `local.properties` si no existe:

```properties
sdk.dir=/ruta/a/Android/sdk
```

4. Ejecutar la configuración `app`.

## Compilar desde terminal

```bash
./gradlew :app:assembleDebug
```

## Estructura

```text
app/src/main/java/com/winup/
├── MainActivity.kt
├── data/
│   ├── ApiClient.kt
│   ├── ApiModels.kt
│   ├── RealtimeClient.kt
│   ├── TokenStore.kt
│   ├── WinUpApiService.kt
│   ├── WinUpData.kt
│   └── WinUpSession.kt
└── ui/
    ├── auth/
    │   └── AuthScreen.kt
    ├── WinUpRoot.kt
    ├── components/
    │   ├── LiveBadge.kt
    │   ├── MatchCard.kt
    │   └── WinUpLogo.kt
    ├── home/
    │   └── HomeScreen.kt
    ├── bets/
    │   └── BetsScreen.kt
    ├── wallet/
    │   └── WalletScreen.kt
    ├── profile/
    │   └── ProfileScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## Ejecutar contra el backend

El backend debe estar levantado en `http://localhost:8080`. Para el celular por USB:

```bash
adb reverse tcp:8080 tcp:8080
./gradlew :app:installDebug
adb shell am start -n com.winup/.MainActivity
```

La app abre en login. Crea una cuenta desde `Regístrate` o usa una cuenta existente. El token se guarda cifrado con `EncryptedSharedPreferences`.

Para emulador, compila con:

```bash
./gradlew :app:assembleDebug -PapiBaseUrl=http://10.0.2.2:8080/
```

## Estado

- Login y registro con JWT contra el backend.
- Partidos, apuestas, cartera, movimientos y perfil con datos reales.
- Token cifrado y cierre de sesión con confirmación.
- Actualización por WebSocket y sondeo cada 10 segundos.
- Modo oscuro y claro con selector en Perfil.
- El cliente HTTP es Retrofit + OkHttp + kotlinx.serialization.

## Stack

- AGP 8.9.2
- Gradle 8.11.1
- Kotlin 2.1.20
- Compose BOM 2025.04.01
- Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization
- `compileSdk` 35, `minSdk` 26, `targetSdk` 35
