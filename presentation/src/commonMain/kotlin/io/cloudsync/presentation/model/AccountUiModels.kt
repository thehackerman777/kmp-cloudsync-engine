package io.cloudsync.presentation.model

import io.cloudsync.domain.account.AccountProfile
import io.cloudsync.domain.account.AuthStatus
import io.cloudsync.domain.account.CloudProvider
import io.cloudsync.domain.account.ProviderConnectionHealth

/**
 * UI-ready representation of a cloud provider account profile.
 *
 * Transforms domain [AccountProfile] data into visual-friendly properties
 * for rendering in Compose or other UI frameworks.
 *
 * @property id Matches [AccountProfile.id].
 * @property providerIcon A platform-agnostic icon token for the provider.
 * @property displayName Human-readable account name.
 * @property email Provider account email.
 * @property statusBadge Visual badge descriptor (color + label) for auth status.
 * @property connectionHealthMessage Human-readable connection health description.
 * @property lastSyncFormatted Human-readable "last synced" text.
 */
public data class UiAccountProfile(
    val id: String,
    val providerIcon: String,
    val displayName: String,
    val email: String,
    val statusBadge: UiStatusBadge,
    val connectionHealthMessage: String,
    val lastSyncFormatted: String = ""
) {
    public companion object {
        /**
         * Maps a [CloudProvider] to an icon token string.
         * Consumers should resolve these to their platform-specific icons.
         */
        private fun providerToIcon(provider: CloudProvider): String = when (provider) {
            CloudProvider.GOOGLE_DRIVE -> "google_drive"
            CloudProvider.DROPBOX -> "dropbox"
            CloudProvider.ONEDRIVE -> "onedrive"
        }

        /**
         * Creates a [UiAccountProfile] from a domain [AccountProfile] and health info.
         */
        public fun fromDomain(
            profile: AccountProfile,
            health: ProviderConnectionHealth? = null
        ): UiAccountProfile {
            return UiAccountProfile(
                id = profile.id,
                providerIcon = providerToIcon(profile.provider),
                displayName = profile.displayName,
                email = profile.email,
                statusBadge = UiStatusBadge.fromAuthStatus(profile.authStatus),
                connectionHealthMessage = formatConnectionHealth(health, profile.authStatus),
                lastSyncFormatted = formatLastSync(profile.lastSyncAt)
            )
        }

        private fun formatConnectionHealth(
            health: ProviderConnectionHealth?,
            authStatus: AuthStatus
        ): String = when (health) {
            is ProviderConnectionHealth.Connected -> "Conectado"
            is ProviderConnectionHealth.ExpiringSoon -> "Token expira pronto"
            is ProviderConnectionHealth.Disconnected -> {
                if (health.isRetryable) "Desconectado (reintentando...)"
                else "Desconectado: ${health.reason}"
            }
            is ProviderConnectionHealth.Unconfigured -> when (authStatus) {
                AuthStatus.PENDING -> "Por configurar"
                AuthStatus.REVOKED -> "Acceso revocado"
                else -> "No configurado"
            }
            null -> when (authStatus) {
                AuthStatus.CONNECTED -> "Conectado"
                AuthStatus.EXPIRED -> "Token expirado"
                AuthStatus.REVOKED -> "Acceso revocado"
                AuthStatus.PENDING -> "Por configurar"
            }
        }

        private fun formatLastSync(lastSyncAt: Long?): String {
            if (lastSyncAt == null) return "Nunca sincronizado"
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val diff = now - lastSyncAt
            return when {
                diff < 60_000 -> "Hace segundos"
                diff < 3_600_000 -> "Hace ${diff / 60_000} min"
                diff < 86_400_000 -> "Hace ${diff / 3_600_000} h"
                diff < 604_800_000 -> "Hace ${diff / 86_400_000} d\u00edas"
                else -> {
                    val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(lastSyncAt)
                    instant.toString().substringBefore("T")
                }
            }
        }
    }
}

/**
 * Visual badge for displaying authentication status in the UI.
 *
 * @property label Human-readable status text.
 * @property colorRes Resource token for the badge color.
 * @property isWarning Whether this status should draw attention.
 */
public data class UiStatusBadge(
    val label: String,
    val colorRes: String,
    val isWarning: Boolean = false
) {
    public companion object {
        /**
         * Maps an [AuthStatus] to a [UiStatusBadge].
         */
        public fun fromAuthStatus(status: AuthStatus): UiStatusBadge = when (status) {
            AuthStatus.CONNECTED -> UiStatusBadge(
                label = "Conectado",
                colorRes = "badge_green",
                isWarning = false
            )
            AuthStatus.EXPIRED -> UiStatusBadge(
                label = "Expirado",
                colorRes = "badge_yellow",
                isWarning = true
            )
            AuthStatus.REVOKED -> UiStatusBadge(
                label = "Revocado",
                colorRes = "badge_red",
                isWarning = true
            )
            AuthStatus.PENDING -> UiStatusBadge(
                label = "Pendiente",
                colorRes = "badge_gray",
                isWarning = false
            )
        }
    }
}
