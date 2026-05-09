package io.cloudsync.domain.identity

import kotlinx.serialization.Serializable

/**
 * Device-level identity used for tracking sync sources and multi-device resolution.
 * Generated once per installation and persisted in encrypted local storage.
 */
@Serializable
public data class DeviceInfo(
    val deviceId: String,  // UUID v4
    val deviceName: String, // User-visible name (e.g., "Pepe's Pixel 9")
    val platform: DevicePlatform,
    val createdAt: Long,
    val lastSeenAt: Long,
    val osVersion: String = "",
    val appVersion: String = ""
)

@Serializable
public enum class DevicePlatform {
    ANDROID, DESKTOP, WEB, IOS
}
