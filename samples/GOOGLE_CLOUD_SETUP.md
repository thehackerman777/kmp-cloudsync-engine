# Google Cloud Console Setup Guide

## 1. Crear un proyecto en Google Cloud Console

1. Ve a https://console.cloud.google.com/
2. Crea un proyecto nuevo o selecciona uno existente
3. Anota el **Project ID** (lo necesitarás después)

## 2. Habilitar Google Drive API

1. En el menú → "APIs & Services" → "Library"
2. Busca "Google Drive API"
3. Haz clic y presiona "Enable"

## 3. Configurar OAuth Consent Screen

1. "APIs & Services" → "OAuth consent screen"
2. Elige "External" (o "Internal" si es solo para tu organización)
3. Llena:
   - **App name**: "KMP CloudSync Dev" (o el nombre de tu app)
   - **User support email**: tu email
   - **Developer contact**: tu email
4. **Scopes**: Agrega `.../auth/drive.appdata`
5. **Test users**: Agrega los emails de las cuentas que usarán la app

## 4. Crear OAuth 2.0 Client IDs

Necesitas crear UN Client ID por cada plataforma que quieras soportar:

### Android
- **Application type**: Android
- **Package name**: `io.cloudsync.sample.android` (o el de tu app)
- **SHA-1 fingerprint**: 
  ```bash
  cd samples/android-app
  keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep SHA1
  ```

### Desktop (JVM)
- **Application type**: Desktop
- **No necesita más configuración**
- Redirect URI: Se usa `http://localhost:8090` automáticamente

### Web
- **Application type**: Web application
- **Authorized JavaScript origins**: `http://localhost:8080` (o tu URL)
- **Authorized redirect URIs**: `http://localhost:8080/oauth2callback`

### iOS (próximamente)
- **Application type**: iOS
- **Bundle ID**: `io.cloudsync.sample.ios`

## 5. Descargar credenciales

Android NO necesita descargar nada si usas Credential Manager (recomendado).
Para Desktop y Web, anota el **Client ID** y **Client Secret**.

## 6. Configurar en tu app

### Desktop
```kotlin
val engine = CloudSyncEngine.configure("""
{
  "clientId": "YOUR_DESKTOP_CLIENT_ID.apps.googleusercontent.com",
  "clientSecret": "YOUR_CLIENT_SECRET",
  "configName": "prod-sync",
  "serverUrl": "https://www.googleapis.com"
}
""")
```

### Desarrollo (sin credenciales reales)
Usa `SyncMode.MOCK`:
```kotlin
val initializer = CloudSyncInitializer(identityManager, configRepo)
initializer.initialize(platform = DevicePlatform.DESKTOP, mode = SyncMode.MOCK)
```

## Troubleshooting

### "Error 403: Access Not Configured"
→ Habilita Google Drive API en el proyecto

### "Redirect URI Mismatch"
→ Verifica que el redirect URI en el código coincida exactamente con el registrado

### "Token expired"
→ El AuthManager refresca automáticamente si hay refresh token

### Android: "App not verified"
→ En desarrollo, los test users pueden usar la app aunque no esté verificada
