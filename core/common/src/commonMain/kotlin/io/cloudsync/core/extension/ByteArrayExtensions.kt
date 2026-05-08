package io.cloudsync.core.extension

import kotlinx.serialization.Serializable

/**
 * Cryptographic and checksum utilities for data integrity verification.
 */
@Serializable
public data class Checksum(
    val algorithm: ChecksumAlgorithm,
    val value: String
)

@Serializable
public enum class ChecksumAlgorithm {
    SHA256,
    MD5,
    CRC32
}

/**
 * Compute a SHA-256 hex checksum from a byte array.
 * Platform-specific implementations handle actual hashing.
 */
public expect fun ByteArray.sha256Hex(): String

/**
 * Compute a CRC32 checksum from a byte array.
 */
public expect fun ByteArray.crc32(): Long

/**
 * Format bytes into a human-readable string (KB, MB, GB).
 */
public fun Long.toHumanReadableSize(): String = when {
    this < 1024 -> "$this B"
    this < 1024 * 1024 -> "${this / 1024} KB"
    this < 1024 * 1024 * 1024 -> "%.1f MB".format(this.toDouble() / (1024 * 1024))
    else -> "%.2f GB".format(this.toDouble() / (1024 * 1024 * 1024))
}
