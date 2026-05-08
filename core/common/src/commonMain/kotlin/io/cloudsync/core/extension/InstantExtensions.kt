package io.cloudsync.core.extension

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Extension functions for [Instant] to simplify sync-related time operations.
 */
public fun Instant.Companion.now(): Instant = Clock.System.now()

public fun Instant.Companion.epoch(): Instant = Instant.fromEpochMilliseconds(0)

public fun Instant.ageInMillis(): Long = Clock.System.now().toEpochMilliseconds() - this.toEpochMilliseconds()

public fun Instant.isExpired(ttlMs: Long): Boolean = ageInMillis() > ttlMs

public fun Instant.isRecent(windowMs: Long): Boolean = ageInMillis() <= windowMs

public fun Instant.toHumanReadable(): String {
    val local = this.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-" +
        "${local.dayOfMonth.toString().padStart(2, '0')} " +
        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
