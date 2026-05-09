package io.cloudsync.storage.serialization

import io.cloudsync.core.InternalCloudSyncApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Handles serialization and deserialization of configuration payloads.
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
    public fun serialize(value: Any): String {
        return json.encodeToString(value)
    }

    public fun <T> deserialize(payload: String): T {
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromString(payload) as T
    }

    public fun byteSize(payload: String): Long {
        return payload.encodeToByteArray().size.toLong()
    }
}
