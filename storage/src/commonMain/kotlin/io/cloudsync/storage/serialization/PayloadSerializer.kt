package io.cloudsync.storage.serialization

import io.cloudsync.core.InternalCloudSyncApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.encodeToString

/**
 * Handles serialization and deserialization of configuration payloads.
 *
 * Supports:
 * - JSON serialization (default)
 * - Compression (future: snappy, gzip)
 * - Encryption (future: AES-256-GCM)
 */
@InternalCloudSyncApi
public class PayloadSerializer(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = false
        prettyPrint = false
    }
) {
    /**
     * Serializes a generic value to a JSON string.
     */
    public inline fun <reified T> serialize(value: T): String {
        return json.encodeToString(value)
    }

    /**
     * Deserializes a JSON string to a generic value.
     */
    public inline fun <reified T> deserialize(payload: String): T {
        return json.decodeFromString(payload)
    }

    /**
     * Computes the byte size of a serialized payload.
     */
    public fun byteSize(payload: String): Long {
        return payload.encodeToByteArray().size.toLong()
    }
}
