package io.cloudsync.storage.serialization

import io.cloudsync.core.InternalCloudSyncApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.KSerializer

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
    public fun serialize(value: @kotlinx.serialization.Serializable Any): String {
        return json.encodeToString(value)
    }

    public fun <T> deserialize(payload: String, serializer: KSerializer<T>): T {
        return json.decodeFromString(serializer, payload)
    }

    public fun byteSize(payload: String): Long {
        return payload.encodeToByteArray().size.toLong()
    }
}
