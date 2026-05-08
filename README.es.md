# ⚡ KMP CloudSync Engine

<p align="center">
  <strong>Framework de Sincronización Privada para Kotlin Multiplatform</strong>
  <br>
  <em>Offline-First · Cloud-First · Seguro por Diseño</em>
</p>

<p align="center">
  <a href="https://github.com/thehackerman777/kmp-cloudsync-engine/actions/workflows/ci.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/thehackerman777/kmp-cloudsync-engine/ci.yml?branch=main&label=CI&logo=github" alt="CI">
  </a>
  <a href="https://github.com/thehackerman777/kmp-cloudsync-engine/releases">
    <img src="https://img.shields.io/github/v/release/thehackerman777/kmp-cloudsync-engine?include_prereleases&logo=semver" alt="Versión">
  </a>
  <a href="https://github.com/thehackerman777/kmp-cloudsync-engine/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/licencia-Apache%202.0-blue.svg" alt="Licencia">
  </a>
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-purple?logo=kotlin" alt="Kotlin">
</p>

---

## 🇪🇸 ¿Qué es KMP CloudSync Engine?

**KMP CloudSync Engine** es un SDK profesional para sincronizar configuraciones y datos privados entre dispositivos Android, Desktop y Web, todo desde Kotlin Multiplatform.

Usa el **carpeta oculta appDataFolder de Google Drive** como backend en la nube — sin carpetas visibles, sin impacto en la cuota del usuario, sin exposición accidental.

### 🎯 ¿Para qué sirve?

- Sincronizar configuraciones de app entre dispositivos
- Backup automático de datos privados
- Multi-dispositivo sin fricción
- Restauración de estado en nuevas instalaciones
- Sincronización enterprise de configuraciones

---

## 🏗️ Arquitectura en Una Imagen

```
App ──▶ Domain (reglas de negocio)
            │
       ┌────▼────┐
       │  Data    │  ← SQLDelight (local) + Drive API (remoto)
       └────┬────┘
            │
       ┌────▼────┐
       │  Sync   │  ← Motor de sincronización bidireccional
       └─────────┘
```

**Principio rector:** *Local-First* — siempre leemos de la base local. La nube es el respaldo, no la fuente primaria.

---

## 🚀 Inicio Rápido

### 1. Agregar dependencia

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("io.cloudsync:kmp-cloudsync-engine:0.1.0")
}
```

### 2. Inicializar

```kotlin
// Configuración mínima
val engine = CloudSyncEngine.configure(
    clientId = "TU_CLIENT_ID.apps.googleusercontent.com",
    applicationName = "MiApp"
)

// Iniciar motor de sincronización
scope.launch {
    engine.start()
}
```

### 3. Guardar y leer configuraciones

```kotlin
// Guardar
engine.save(Configuration(
    id = "preferencias-usuario",
    payload = """{"tema":"oscuro","idioma":"es"}"""
))

// Observar cambios en tiempo real
engine.observeAll().collect { configs ->
    println("Configuraciones: ${configs.size}")
}
```

---

## 📦 ¿Qué Incluye?

| Módulo | Propósito |
|--------|-----------|
| `core` | Tipos base, dispatchers, inyección de dependencias |
| `domain` | Modelos de negocio, contratos, casos de uso |
| `data` | SQLDelight (local) + Google Drive API (remoto) |
| `network` | Cliente Ktor, interceptores, reintentos |
| `auth` | OAuth2 PKCE, tokens, almacenamiento seguro |
| `sync` | Motor de sincronización, conflictos, versiones |
| `storage` | Drivers de base de datos, serialización |

---

## 🔐 Seguridad

- **Cifrado en tránsito:** TLS 1.3 forzado
- **Cifrado en reposo:** AES-256 via keychain del SO
- **OAuth2 PKCE:** Sin secretos expuestos en el código
- **appDataFolder:** Archivos invisibles para el usuario
- **Checksums SHA-256:** Integridad verificada en cada operación
- **Tokens efímeros:** Refresco automático antes de expirar

---

## 🔄 Flujo de Sincronización

```
1. App escribe → Base local (instantáneo) ✅
2. Background sync → Sube cambios a Drive ☁️
3. Drive notifica cambios → Baja al dispositivo 📥
4. Compara versiones → Si conflicto → Last Write Wins ⚔️
5. Actualiza metadata → Checksums + timestamps 📝
```

Todo esto ocurre automáticamente. El desarrollador solo llama a `save()` y `observeAll()`.

---

## ⚙️ CI/CD Pipeline (GitHub Actions)

| Pipeline | Disparador | Qué hace |
|----------|------------|----------|
| **CI** | Push/PR | Lint, tests, build Android/Desktop/JS |
| **CD** | Tag v*.*.* | Release automático con artefactos |
| **Security** | Semanal | CodeQL, escaneo de secretos |
| **Nightly** | Diario | Build completo, escaneo de dependencias |

El pipeline usa un **runner self-hosted** (AWS t3.large, 7.6GB RAM) para compilación acelerada.

---

## 🤝 ¿Cómo Contribuir?

Lee [CONTRIBUTING.md](docs/CONTRIBUTING.md) en español simplificado:

1. Fork el repo
2. Crea tu rama: `git checkout -b feature/mi-mejora`
3. Commit con [Conventional Commits](https://www.conventionalcommits.org/)
4. Push y PR a `develop`

---

## 🗺️ Roadmap

| Fase | Funcionalidad | Estado |
|------|---------------|--------|
| P0 | Motor de sincronización base | ✅ Listo |
| P0 | Google Drive appDataFolder | ✅ Listo |
| P0 | OAuth2 PKCE | ✅ Listo |
| P0 | SQLDelight local | ✅ Listo |
| P1 | iOS (Swift/KMP) | 🚧 En progreso |
| P1 | Compresión gzip | 📋 Planeado |
| P2 | Cifrado E2E | 📋 Planeado |
| P2 | Multi-cloud (Firebase, S3) | 📋 Planeado |
| P3 | IA para predicción de conflictos | 🔮 Futuro |

---

## 📄 Licencia

```
Apache 2.0 — Copyright 2024 CloudSync Contributors
```

---

## 🌐 Enlaces

- [README en Inglés](README.md)
- [Documentación de Arquitectura](docs/ARCHITECTURE.md)
- [Modelo de Seguridad](docs/SECURITY.md)
- [Guía de Contribución](docs/CONTRIBUTING.md)
- [Roadmap](docs/ROADMAP.md)

---

<p align="center">
  <sub>Hecho con ❤️ por el equipo de CloudSync</sub>
</p>
