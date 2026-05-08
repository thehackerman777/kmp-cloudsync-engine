package io.cloudsync.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Google Drive API DTOs for appDataFolder operations.
 *
 * Maps to the Drive API v3 specification:
 * https://developers.google.com/drive/api/reference/rest/v3/files
 */

@Serializable
public data class DriveFile(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("mimeType") val mimeType: String = "application/json",
    @SerialName("description") val description: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("createdTime") val createdTime: String = "",
    @SerialName("modifiedTime") val modifiedTime: String = "",
    @SerialName("md5Checksum") val md5Checksum: String = "",
    @SerialName("parents") val parents: List<String>? = null,
    @SerialName("appProperties") val appProperties: Map<String, String>? = null,
    @SerialName("properties") val properties: Map<String, String>? = null,
    @SerialName("version") val version: Long = 0
)

@Serializable
public data class DriveFileList(
    @SerialName("kind") val kind: String = "drive#fileList",
    @SerialName("nextPageToken") val nextPageToken: String? = null,
    @SerialName("files") val files: List<DriveFile> = emptyList()
)

@Serializable
public data class DriveFileContent(
    @SerialName("name") val name: String,
    @SerialName("mimeType") val mimeType: String = "application/json",
    @SerialName("parents") val parents: List<String>? = listOf("appDataFolder"),
    @SerialName("appProperties") val appProperties: Map<String, String>? = null,
    @SerialName("description") val description: String? = null
)

@Serializable
public data class DriveError(
    @SerialName("error") val error: DriveErrorDetail
)

@Serializable
public data class DriveErrorDetail(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("errors") val errors: List<DriveErrorReason> = emptyList()
)

@Serializable
public data class DriveErrorReason(
    @SerialName("domain") val domain: String,
    @SerialName("reason") val reason: String,
    @SerialName("message") val message: String
)

/**
 * Sync metadata encoded as Drive appProperties.
 */
@Serializable
public data class SyncAppProperties(
    val cloudSyncVersion: String = "1.0",
    val configVersion: String = "0",
    val checksum: String = "",
    val timestamp: String = "",
    val deviceId: String = "",
    val namespace: String = "",
    val compressed: String = "false",
    val encrypted: String = "false"
)
