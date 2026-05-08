package io.cloudsync.core

import kotlinx.serialization.Serializable

/**
 * Represents the operational state of the sync engine state machine.
 *
 * Transitions follow a deterministic lifecycle:
 * IDLE → INITIALIZING → AUTHENTICATING → SYNCING → IDLE
 * IDLE → ERROR → RETRYING → SYNCING → IDLE
 */
@Serializable
public enum class SyncState {
    /** Engine is initialized and waiting. */
    IDLE,

    /** Engine is bootstrapping internal components. */
    INITIALIZING,

    /** Authenticating with the cloud provider. */
    AUTHENTICATING,

    /** Actively synchronizing data. */
    SYNCING,

    /** Synchronization paused due to network unavailability. */
    PAUSED,

    /** A recoverable error occurred; retry scheduled. */
    RETRYING,

    /** Irrecoverable error; manual intervention required. */
    ERROR,

    /** Engine is performing a graceful shutdown. */
    SHUTTING_DOWN
}
