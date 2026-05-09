package io.cloudsync.sample.desktop

import io.cloudsync.auth.secure.SecureStorage
import io.cloudsync.core.result.SyncErrorCode
import io.cloudsync.core.result.SyncResult
import io.cloudsync.domain.identity.DevicePlatform
import io.cloudsync.domain.identity.IdentityManager
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.model.ConflictResolution
import io.cloudsync.domain.model.SyncMetadata
import io.cloudsync.domain.repository.IConfigurationRepository
import io.cloudsync.engine.CloudSyncInitializer
import io.cloudsync.engine.InitState
import io.cloudsync.engine.SyncMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

fun main() = runBlocking {
    println("╔══════════════════════════════════════════════════════════════╗")
    println("║        CloudSync Engine — Desktop Sample (Mock Mode)       ║")
    println("╚══════════════════════════════════════════════════════════════╝")
    println()

    // ── 1. Infrastructure ──────────────────────────────────────────────
    println("📦 [1/5] Initializing infrastructure...")

    val secureStorage = SecureStorage()
    val configRepository = MemoryConfigurationRepository()
    val identityManager = IdentityManager(configRepository)

    println("   ✅ SecureStorage: initialized")
    println("   ✅ ConfigRepository: in-memory")
    println("   ✅ IdentityManager: created")

    // ── 2. Engine Initializer ──────────────────────────────────────────
    println()
    println("🚀 [2/5] Creating CloudSyncInitializer...")

    val initializer = CloudSyncInitializer(
        identityManager = identityManager,
        configurationRepository = configRepository,
        secureStorage = secureStorage
    )

    println("   ✅ CloudSyncInitializer: created")

    // ── 3. Bootstrap ───────────────────────────────────────────────────
    println()
    println("🔧 [3/5] Bootstrapping engine (MOCK mode)...")
    println("   Platform: DESKTOP")
    println("   Mode:     MOCK")
    println("   Version:  0.1.0")
    println()

    val initResult = initializer.initialize(
        platform = DevicePlatform.DESKTOP,
        mode = SyncMode.MOCK,
        appVersion = "0.1.0"
    )

    when (initResult) {
        is SyncResult.Success -> println("   ✅ Engine initialized successfully!")
        is SyncResult.Error -> {
            println("   ❌ Engine initialization FAILED:")
            println("      Code:    ${initResult.reason.code}")
            println("      Message: ${initResult.reason.message}")
            println("      Cause:   ${initResult.reason.throwableMessage ?: "none"}")
            return@runBlocking
        }
        else -> {
            println("   ⚠️ Unexpected result: $initResult")
            return@runBlocking
        }
    }

    // ── 4. User Profile ────────────────────────────────────────────────
    println()
    println("👤 [4/5] User profile:")

    val profile = identityManager.currentProfile.value
    if (profile != null) {
        println("   ├─ Local ID:   ${profile.localUserId}")
        println("   ├─ Provider:   ${profile.provider}")
        println("   ├─ Name:       ${profile.displayName ?: "(anonymous)"}")
        println("   ├─ Email:      ${profile.email ?: "(no email)"}")
        println("   ├─ Auth:       ${if (profile.isAuthenticated) "✅ Authenticated" else "⛔ Anonymous"}")
        println("   └─ Canonical:  ${profile.canonicalUserId}")
    } else {
        println("   ⚠️ No profile loaded")
    }

    val deviceInfo = identityManager.deviceInfo.value
    if (deviceInfo != null) {
        println()
        println("   Device info:")
        println("   ├─ Device ID:   ${deviceInfo.deviceId.take(8)}...")
        println("   ├─ Name:        ${deviceInfo.deviceName}")
        println("   ├─ Platform:    ${deviceInfo.platform}")
        println("   └─ OS Version:  ${deviceInfo.osVersion}")
    }

    // ── 5. Save some sample configs ────────────────────────────────────
    println()
    println("💾 [5/5] Saving sample configurations...")

    val sampleConfigs = listOf(
        Configuration(
            id = "sample:theme",
            namespace = "sample",
            payload = """{"theme":"dark","primaryColor":"#6200EE"}""",
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            createdAt = Clock.System.now().toEpochMilliseconds()
        ),
        Configuration(
            id = "sample:language",
            namespace = "sample",
            payload = """{"language":"es","region":"CO"}""",
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            createdAt = Clock.System.now().toEpochMilliseconds()
        ),
        Configuration(
            id = "sample:notifications",
            namespace = "sample",
            payload = """{"pushEnabled":true,"syncInterval":300}""",
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
    )

    for (config in sampleConfigs) {
        val saveResult = configRepository.save(config)
        when (saveResult) {
            is SyncResult.Success -> println("   ✅ Saved: ${config.id}")
            is SyncResult.Error -> println("   ❌ Failed: ${config.id} — ${saveResult.reason.message}")
            else -> {}
        }
    }

    // ── 6. List saved configs ──────────────────────────────────────────
    println()
    println("📋 Saved configurations:")

    val allConfigs = configRepository.getByNamespace("sample")
    when (allConfigs) {
        is SyncResult.Success -> {
            if (allConfigs.data.isEmpty()) {
                println("   (none)")
            } else {
                allConfigs.data.forEachIndexed { i, cfg ->
                    val icon = if (cfg.hasPendingChanges) "🔄" else "✅"
                    println("   $icon  [${i + 1}] ${cfg.id} v${cfg.version}")
                    println("        payload: ${cfg.payload.take(60)}${if (cfg.payload.length > 60) "..." else ""}")
                }
            }
        }
        is SyncResult.Error -> println("   ⚠️ Could not list configs: ${allConfigs.reason.message}")
        else -> {}
    }

    // ── 7. Init state check ────────────────────────────────────────────
    println()
    println("📊 Initialization state: ${initializer.initState.value}")

    // ── 8. Cleanup ─────────────────────────────────────────────────────
    println()
    println("🧹 Cleaning up...")
    configRepository.clearLocal()
    secureStorage.clearCredentials()
    println("   ✅ Done")

    println()
    println("╔══════════════════════════════════════════════════════════════╗")
    println("║        Desktop Sample Completed Successfully 🎉             ║")
    println("╚══════════════════════════════════════════════════════════════╝")
}

// ─────────────────────────────────────────────────────────────────────────────
//  In-memory implementation of IConfigurationRepository for sample purposes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Simple in-memory [IConfigurationRepository] that stores [Configuration] entries
 * in a concurrent map. Suitable for sample/demo use only — not for production.
 */
private class MemoryConfigurationRepository : IConfigurationRepository {

    private val store = mutableMapOf<String, Configuration>()
    private val _allFlow = MutableStateFlow<List<Configuration>>(emptyList())

    private fun notifyChange() {
        _allFlow.value = store.values.toList().sortedByDescending { it.updatedAt }
    }

    override suspend fun getById(id: String): SyncResult<Configuration> {
        val config = store[id]
        return if (config != null) {
            SyncResult.success(config)
        } else {
            SyncResult.error(SyncErrorCode.NOT_FOUND, "Configuration not found: $id")
        }
    }

    override suspend fun getByNamespace(namespace: String): SyncResult<List<Configuration>> {
        val results = store.values.filter { it.namespace == namespace }
        return SyncResult.success(results)
    }

    override suspend fun save(configuration: Configuration): SyncResult<Configuration> {
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = store[configuration.id]
        val version = (existing?.version ?: 0) + 1
        val updated = configuration.copy(
            version = version,
            updatedAt = now,
            createdAt = existing?.createdAt ?: configuration.createdAt.let { if (it == 0L) now else it }
        )
        store[updated.id] = updated
        notifyChange()
        return SyncResult.success(updated)
    }

    override suspend fun delete(id: String): SyncResult<Unit> {
        store.remove(id)
        notifyChange()
        return SyncResult.success(Unit)
    }

    override fun observeAll(): Flow<List<Configuration>> = _allFlow.asStateFlow()

    override fun observeByNamespace(namespace: String): Flow<List<Configuration>> {
        return MutableStateFlow(
            store.values.filter { it.namespace == namespace }
        ).asStateFlow()
    }

    override suspend fun getPendingSync(): SyncResult<List<Configuration>> {
        return SyncResult.success(store.values.filter { it.hasPendingChanges })
    }

    override suspend fun getVersionHistory(id: String, limit: Int): SyncResult<List<SyncMetadata>> {
        return SyncResult.success(emptyList())
    }

    override suspend fun resolveConflict(resolution: ConflictResolution): SyncResult<Configuration> {
        store[resolution.resolved.id] = resolution.resolved
        notifyChange()
        return SyncResult.success(resolution.resolved)
    }

    override suspend fun count(): Long = store.size.toLong()

    override suspend fun clearLocal() {
        store.clear()
        notifyChange()
    }
}
