package io.cloudsync.core.extension

import java.security.MessageDigest
import java.util.zip.CRC32

public actual fun ByteArray.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(this)
    return hash.joinToString("") { "%02x".format(it) }
}

public actual fun ByteArray.crc32(): Long {
    val crc = CRC32()
    crc.update(this)
    return crc.value
}
