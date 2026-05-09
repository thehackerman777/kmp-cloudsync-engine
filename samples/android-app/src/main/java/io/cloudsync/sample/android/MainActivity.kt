package io.cloudsync.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.cloudsync.domain.model.Configuration
import io.cloudsync.domain.identity.*
import io.cloudsync.domain.repository.IConfigurationRepository
import io.cloudsync.core.result.SyncResult
import io.cloudsync.core.result.SyncErrorCode
import io.cloudsync.auth.provider.MockAuthProvider
import io.cloudsync.auth.secure.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.Uuid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CloudSyncSample()
                }
            }
        }
    }
}

@Composable
fun CloudSyncSample() {
    val scope = rememberCoroutineScope()
    var statusLog by remember { mutableStateOf(listOf("🚀 App lista")) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var configs by remember { mutableStateOf<List<Configuration>>(emptyList()) }
    var isInit by remember { mutableStateOf(false) }

    fun log(msg: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        statusLog = statusLog + "[$time] $msg"
    }

    // In-memory repo for the sample
    val repo = remember {
        object : IConfigurationRepository {
            val store = mutableMapOf<String, Configuration>()
            override suspend fun getById(id: String) = store[id]?.let { SyncResult.success(it) }
                ?: SyncResult.error(SyncErrorCode.STORAGE_IO_ERROR, "not found")
            override suspend fun getByNamespace(ns: String) = SyncResult.success(store.values.filter { it.namespace == ns })
            override suspend fun save(config: Configuration) = SyncResult.success(config).also { store[config.id] = config }
            override suspend fun delete(id: String) = SyncResult.success(Unit).also { store.remove(id) }
            override fun observeAll(): Flow<List<Configuration>> = emptyFlow()
            override fun observeByNamespace(ns: String): Flow<List<Configuration>> = emptyFlow()
            override suspend fun getPendingSync() = SyncResult.success(store.values.filter { !it.synced })
            override suspend fun getVersionHistory(id: String, limit: Int): SyncResult<List<io.cloudsync.domain.model.SyncMetadata>> = SyncResult.success(emptyList())
            override suspend fun resolveConflict(resolution: io.cloudsync.domain.model.ConflictResolution) =
                SyncResult.success(resolution.resolved)
            override suspend fun count(): Long = store.size.toLong()
            override suspend fun clearLocal() { store.clear() }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("☁️ CloudSync Engine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Modo: MOCK (desarrollo)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        
        Spacer(modifier = Modifier.height(8.dp))

        // Profile card
        profile?.let { p ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("👤 ${p.displayName ?: "Usuario"}", fontWeight = FontWeight.Bold)
                    if (p.email != null) Text("📧 $p.email", style = MaterialTheme.typography.bodySmall)
                    Text("🆔 ${p.canonicalUserId.take(20)}...", style = MaterialTheme.typography.bodySmall)
                    Text("🔐 ${p.provider.name} | ${if (p.isAuthenticated) "✅ Autenticado" else "🟡 Anónimo"}", 
                         style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    log("Inicializando identidad...")
                    val identityManager = IdentityManager(repo)
                    val result = identityManager.initialize(DevicePlatform.ANDROID, "1.0.0")
                    when (result) {
                        is IdentityResult.Created -> {
                            log("✅ Identidad creada: ${result.profile.localUserId.take(12)}...")
                            profile = result.profile
                        }
                        is IdentityResult.Restored -> {
                            log("✅ Identidad restaurada: ${result.profile.localUserId.take(12)}...")
                            profile = result.profile
                        }
                        is IdentityResult.Error -> log("❌ ${result.message}")
                    }

                    // Link mock auth
                    log("Autenticando con MOCK...")
                    val mockProvider = MockAuthProvider(
                        mockDisplayName = "Dev Android"
                    )
                    val linkedProfile = identityManager.linkAuthenticatedIdentity(
                        providerUserId = mockProvider.getMockProfile().providerUserId!!,
                        email = mockProvider.getMockProfile().email,
                        displayName = mockProvider.getMockProfile().displayName,
                        avatarUrl = null,
                        provider = IdentityProvider.GOOGLE
                    )
                    profile = linkedProfile
                    log("✅ Autenticado como ${linkedProfile.email}")

                    // Add test configs
                    val now = Clock.System.now().toEpochMilliseconds()
                    val userId = linkedProfile.canonicalUserId
                    listOf(
                        Configuration("pref-theme", "app", """{"theme":"dark"}""", 1, "", now, now, false, userId = userId),
                        Configuration("pref-lang", "app", """{"lang":"es"}""", 1, "", now, now, false, userId = userId),
                        Configuration("backup-db", "data", """{"version":3}""", 3, "", now, now, true, userId = userId)
                    ).forEach { repo.save(it) }
                    
                    configs = repo.store.values.toList()
                    log("📝 ${configs.size} configuraciones añadidas (2 pendientes)")
                    isInit = true
                }
            }, enabled = !isInit) {
                Text("🚀 Init + Mock Auth")
            }

            Button(onClick = {
                scope.launch {
                    log("🔄 Marcando pendientes como sincronizadas...")
                    repo.store.replaceAll { _, v -> v.copy(synced = true, version = v.version + 1) }
                    configs = repo.store.values.toList()
                    log("✅ ${configs.size} configuraciones sincronizadas (MOCK)")
                }
            }, enabled = isInit) {
                Text("🔄 Sync Now")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Config list
        Text("📋 Configuraciones (${configs.count { it.synced }}/${configs.size} sincronizadas)", 
             style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        
        configs.forEach { cfg ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                 colors = CardDefaults.cardColors(containerColor = if (cfg.synced) 
                     MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (cfg.synced) "✅" else "⏳")
                        Spacer(Modifier.width(8.dp))
                        Text("${cfg.id} v${cfg.version}", fontWeight = FontWeight.SemiBold)
                    }
                    Text("Espacio: ${cfg.namespace}", style = MaterialTheme.typography.bodySmall)
                    Text("Usuario: ${cfg.userId?.take(16)}...", style = MaterialTheme.typography.bodySmall)
                    Text("Payload: ${cfg.payload.take(50)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("📜 Log", style = MaterialTheme.typography.titleSmall)
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(statusLog.reversed()) { entry ->
                Text(entry, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}
