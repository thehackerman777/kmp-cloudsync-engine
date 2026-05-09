package io.cloudsync.core.extension

public actual fun ByteArray.sha256Hex(): String {
    // Pure Kotlin SHA-256 implementation for JS target
    val k = longArrayOf(
        0x428A2F98L, 0x71374491L, 0xB5C0FBCFL, 0xE9B5DBA5L,
        0x3956C25BL, 0x59F111F1L, 0x923F82A4L, 0xAB1C5ED5L,
        0xD807AA98L, 0x12835B01L, 0x243185BEL, 0x550C7DC3L,
        0x72BE5D74L, 0x80DEB1FEL, 0x9BDC06A7L, 0xC19BF174L,
        0xE49B69C1L, 0xEFBE4786L, 0x0FC19DC6L, 0x240CA1CCL,
        0x2DE92C6FL, 0x4A7484AAL, 0x5CB0A9DCL, 0x76F988DAL,
        0x983E5152L, 0xA831C66DL, 0xB00327C8L, 0xBF597FC7L,
        0xC6E00BF3L, 0xD5A79147L, 0x06CA6351L, 0x14292967L,
        0x27B70A85L, 0x2E1B2138L, 0x4D2C6DFCL, 0x53380D13L,
        0x650A7354L, 0x766A0ABBL, 0x81C2C92EL, 0x92722C85L,
        0xA2BFE8A1L, 0xA81A664BL, 0xC24B8B70L, 0xC76C51A3L,
        0xD192E819L, 0xD6990624L, 0xF40E3585L, 0x106AA070L,
        0x19A4C116L, 0x1E376C08L, 0x2748774CL, 0x34B0BCB5L,
        0x391C0CB3L, 0x4ED8AA4AL, 0x5B9CCA4FL, 0x682E6FF3L,
        0x748F82EEL, 0x78A5636FL, 0x84C87814L, 0x8CC70208L,
        0x90BEFFFAL, 0xA4506CEBL, 0xBEF9A3F7L, 0xC67178F2L
    )

    val ml = size.toLong() * 8
    val paddedSize = ((size + 9 + 63) / 64) * 64
    val padded = ByteArray(paddedSize)
    this.copyInto(padded, 0, 0, size)
    padded[size] = 0x80.toByte()

    // Write length in big-endian at end
    var idx = paddedSize - 8
    while (idx < paddedSize) {
        padded[idx] = (ml ushr (8 * (7 - (idx - (paddedSize - 8))))).toByte()
        idx++
    }

    var a = 0x6A09E667L; var b = 0xBB67AE85L; var c = 0x3C6EF372L; var d = 0xA54FF53AL
    var e = 0x510E527FL; var f = 0x9B05688CL; var g = 0x1F83D9ABL; var h = 0x5BE0CD19L

    for (block in 0 until paddedSize / 64) {
        val w = LongArray(64)
        for (t in 0 until 16) {
            val bi = block * 64 + t * 4
            w[t] = ((padded[bi].toLong() and 0xFFL) shl 24) or
                    ((padded[bi + 1].toLong() and 0xFFL) shl 16) or
                    ((padded[bi + 2].toLong() and 0xFFL) shl 8) or
                    (padded[bi + 3].toLong() and 0xFFL)
        }
        for (t in 16 until 64) {
            val s0 = ((w[t - 15] ushr 7) or (w[t - 15] shl 25)) xor
                     ((w[t - 15] ushr 18) or (w[t - 15] shl 14)) xor
                     (w[t - 15] ushr 3)
            val s1 = ((w[t - 2] ushr 17) or (w[t - 2] shl 15)) xor
                     ((w[t - 2] ushr 19) or (w[t - 2] shl 13)) xor
                     (w[t - 2] ushr 10)
            w[t] = (w[t - 16] + s0 + w[t - 7] + s1) and 0xFFFFFFFFL
        }

        var ta = a; var tb = b; var tc = c; var td = d
        var te = e; var tf = f; var tg = g; var th = h

        for (t in 0 until 64) {
            val s1 = ((te ushr 6) or (te shl 26)) xor
                     ((te ushr 11) or (te shl 21)) xor
                     ((te ushr 25) or (te shl 7))
            val ch = (te and tf) xor (te.inv() and tg)
            val temp1 = (th + s1 + ch + k[t] + w[t]) and 0xFFFFFFFFL
            val s0 = ((ta ushr 2) or (ta shl 30)) xor
                     ((ta ushr 13) or (ta shl 19)) xor
                     ((ta ushr 22) or (ta shl 10))
            val maj = (ta and tb) xor (ta and tc) xor (tb and tc)
            val temp2 = (s0 + maj) and 0xFFFFFFFFL

            th = tg; tg = tf; tf = te
            te = (td + temp1) and 0xFFFFFFFFL
            td = tc; tc = tb; tb = ta
            ta = (temp1 + temp2) and 0xFFFFFFFFL
        }

        a = (a + ta) and 0xFFFFFFFFL; b = (b + tb) and 0xFFFFFFFFL
        c = (c + tc) and 0xFFFFFFFFL; d = (d + td) and 0xFFFFFFFFL
        e = (e + te) and 0xFFFFFFFFL; f = (f + tf) and 0xFFFFFFFFL
        g = (g + tg) and 0xFFFFFFFFL; h = (h + th) and 0xFFFFFFFFL
    }

    return longToHex(a) + longToHex(b) + longToHex(c) + longToHex(d) +
           longToHex(e) + longToHex(f) + longToHex(g) + longToHex(h)
}

private fun longToHex(v: Long): String {
    val chars = CharArray(8) { '0' }
    var value = v
    for (i in 7 downTo 0) {
        val nibble = (value and 0xFL).toInt()
        chars[i] = if (nibble < 10) '0' + nibble else 'a' + (nibble - 10)
        value = value ushr 4
    }
    return chars.concatToString()
}

public actual fun ByteArray.crc32(): Long {
    var crc = 0xFFFFFFFFL
    for (byte in this) {
        crc = crc xor byte.toLong() and 0xFFFFFFFFL
        for (i in 0 until 8) {
            crc = if ((crc and 1L) != 0L) {
                (crc ushr 1) xor 0xEDB88320L
            } else {
                crc ushr 1
            }
        }
    }
    return crc xor 0xFFFFFFFFL
}
