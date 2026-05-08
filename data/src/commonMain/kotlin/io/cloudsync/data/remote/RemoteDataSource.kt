package io.cloudsync.data.remote

import io.cloudsync.core.result.SyncError
import io.cloudsync.core.result.SyncErrorCode
import io.cloudsync.core.result.SyncResult
import io.cloudsync.domain.model.Configuration
import io.cloudsync.network.dto.DriveFile
import io.cloudsync.network.dto.DriveFileContent
import io.cloudsync.network.dto.DriveFileList
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Remote data source using Google Drive appDataFolder API.
 *
 * Uses the invisible appDataFolder for private configuration storage:
 * - Files are NOT visible to the user in Drive UI
 * - No user-visible folder structure
 * - No quota consumption against user storage
 * - API-based access only
 *
 * API Reference: https://developers.google.com/drive/api/reference/rest/v3/files
 */
public class RemoteDataSource(
    private val httpClient: HttpClient,
    private val config: RemoteConfig = RemoteConfig()
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Lists all files in the Drive appDataFolder.
     */
    public suspend fun listFiles(): SyncResult<List<DriveFile>> {
        return try {
            val response = httpClient.get("${config.baseUrl}/drive/v3/files") {
                parameter("spaces", "appDataFolder")
                parameter("fields", "files(id,name,mimeType,size,modifiedTime,version,md5Checksum,appProperties)")
                parameter("pageSize", 100)
                parameterOrder("modifiedTime desc")
            }

            if (response.status == HttpStatusCode.OK) {
                val fileList = response.body<DriveFileList>()
                SyncResult.success(fileList.files)
            } else {
                SyncResult.error(
                    SyncErrorCode.NETWORK_IO_ERROR,
                    "Failed to list files: HTTP ${response.status}"
                )
            }
        } catch (e: Exception) {
            SyncResult.error(
                SyncErrorCode.NETWORK_IO_ERROR,
                "Network error listing files",
                e
            )
        }
    }

    /**
     * Downloads a file from Drive appDataFolder.
     */
    public suspend fun download(fileId: String): SyncResult<Configuration> {
        return try {
            // Get file metadata
            val metaResponse = httpClient.get("${config.baseUrl}/drive/v3/files/$fileId") {
                parameter("fields", "id,name,mimeType,modifiedTime,version,md5Checksum,appProperties,size")
                parameter("spaces", "appDataFolder")
            }

            if (metaResponse.status != HttpStatusCode.OK) {
                return SyncResult.error(
                    SyncErrorCode.NETWORK_IO_ERROR,
                    "Failed to get file metadata: HTTP ${metaResponse.status}"
                )
            }

            val file = metaResponse.body<DriveFile>()

            // Download file content
            val contentResponse = httpClient.get("${config.baseUrl}/drive/v3/files/$fileId") {
                parameter("alt", "media")
            }

            if (contentResponse.status != HttpStatusCode.OK) {
                return SyncResult.error(
                    SyncErrorCode.NETWORK_IO_ERROR,
                    "Failed to download file content: HTTP ${contentResponse.status}"
                )
            }

            val payload = contentResponse.bodyAsText()
            val version = file.appProperties?.get("configVersion")?.toLongOrNull() ?: file.version

            SyncResult.success(Configuration(
                id = file.id,
                namespace = file.appProperties?.get("namespace") ?: "default",
                payload = payload,
                version = version,
                checksum = file.md5Checksum,
                updatedAt = parseRfc3339(file.modifiedTime),
                sizeBytes = file.size
            ))
        } catch (e: Exception) {
            SyncResult.error(
                SyncErrorCode.NETWORK_IO_ERROR,
                "Error downloading file $fileId",
                e
            )
        }
    }

    /**
     * Uploads a configuration to Drive appDataFolder.
     */
    public suspend fun upload(configuration: Configuration): SyncResult<Configuration> {
        return try {
            // Check if file already exists
            val existing = findFileByName(configuration.id)

            if (existing != null) {
                // Update existing file
                updateFile(existing.id, configuration)
            } else {
                // Create new file
                createFile(configuration)
            }

            SyncResult.success(configuration)
        } catch (e: Exception) {
            SyncResult.error(
                SyncErrorCode.NETWORK_IO_ERROR,
                "Error uploading ${configuration.id}",
                e
            )
        }
    }

    /**
     * Deletes a file from Drive appDataFolder.
     */
    public suspend fun delete(fileId: String): SyncResult<Unit> {
        return try {
            val response = httpClient.delete("${config.baseUrl}/drive/v3/files/$fileId")

            if (response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK) {
                SyncResult.success(Unit)
            } else {
                SyncResult.error(
                    SyncErrorCode.NETWORK_IO_ERROR,
                    "Failed to delete file: HTTP ${response.status}"
                )
            }
        } catch (e: Exception) {
            SyncResult.error(
                SyncErrorCode.NETWORK_IO_ERROR,
                "Error deleting file $fileId",
                e
            )
        }
    }

    /**
     * Gets the version of a remote file.
     */
    public suspend fun getVersion(fileId: String): Long? {
        return try {
            val response = httpClient.get("${config.baseUrl}/drive/v3/files/$fileId") {
                parameter("fields", "version,appProperties")
                parameter("spaces", "appDataFolder")
            }
            if (response.status == HttpStatusCode.OK) {
                val file = response.body<DriveFile>()
                file.appProperties?.get("configVersion")?.toLongOrNull() ?: file.version
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks connectivity to the Drive API.
     */
    public suspend fun checkConnectivity(): Boolean {
        return try {
            val response = httpClient.get("${config.baseUrl}/drive/v3/about") {
                parameter("fields", "user")
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun findFileByName(name: String): DriveFile? {
        val response = httpClient.get("${config.baseUrl}/drive/v3/files") {
            parameter("spaces", "appDataFolder")
            parameter("q", "name='$name'")
            parameter("fields", "files(id,name,version)")
        }
        if (response.status == HttpStatusCode.OK) {
            val fileList = response.body<DriveFileList>()
            return fileList.files.firstOrNull()
        }
        return null
    }

    private suspend fun createFile(configuration: Configuration) {
        httpClient.post("${config.baseUrl}/drive/v3/files") {
            contentType(ContentType.Application.Json)
            setBody(DriveFileContent(
                name = configuration.id,
                description = "CloudSync configuration: ${configuration.id}",
                appProperties = mapOf(
                    "cloudSyncVersion" to "1.0",
                    "configVersion" to configuration.version.toString(),
                    "checksum" to configuration.checksum,
                    "namespace" to configuration.namespace
                )
            ))
        }
        // Upload content via multipart
        httpClient.patch("${config.baseUrl}/upload/drive/v3/files/${configuration.id}") {
            parameter("uploadType", "media")
            contentType(ContentType.Application.Json)
            setBody(configuration.payload)
        }
    }

    private suspend fun updateFile(fileId: String, configuration: Configuration) {
        // Update metadata
        httpClient.patch("${config.baseUrl}/drive/v3/files/$fileId") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "description" to "CloudSync configuration: ${configuration.id}",
                "appProperties" to mapOf(
                    "cloudSyncVersion" to "1.0",
                    "configVersion" to configuration.version.toString(),
                    "checksum" to configuration.checksum,
                    "namespace" to configuration.namespace
                )
            ))
        }
        // Update content
        httpClient.patch("${config.baseUrl}/upload/drive/v3/files/$fileId") {
            parameter("uploadType", "media")
            contentType(ContentType.Application.Json)
            setBody(configuration.payload)
        }
    }

    private fun parseRfc3339(dateString: String): Long {
        return try {
            // Simplified: convert ISO 8601 string to epoch millis
            kotlinx.datetime.Instant.parse(dateString).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }
}

public data class RemoteConfig(
    val baseUrl: String = "https://www.googleapis.com",
    val maxRetries: Int = 3,
    val pageSize: Int = 100
)
