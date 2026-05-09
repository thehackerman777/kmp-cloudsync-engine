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
    this < 1024L * 1024 * 1024 -> "${formatWithDecimals(this.toDouble() / (1024 * 1024), 1)} MB"
    else -> "${formatWithDecimals(this.toDouble() / (1024 * 1024 * 1024), 2)} GB"
}

/** Cross-platform decimal formatter. */
private fun formatWithDecimals(value: Double, decimals: Int): String {
    val factor = when (decimals) { 1 -> 10; 2 -> 100; else -> 1000 }
    val scaled = (value * factor).toLong()
    val intPart = scaled / factor
    val decPart = (scaled % factor).toInt()
    return "$intPart.${decPart.toString().padStart(decimals, '0')}"
}
