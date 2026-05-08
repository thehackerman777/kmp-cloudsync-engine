package io.cloudsync.core

/**
 * Marks declarations that are **internal** to the CloudSync framework.
 *
 * Public API surfaces annotated with this annotation are not intended for
 * consumer code and may change without notice between minor versions.
 * The Binary Compatibility Validator uses this annotation to exclude
 * these declarations from API dump validation.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal CloudSync API. It may be changed or removed " +
        "without notice. Do not use in consumer code."
)
public annotation class InternalCloudSyncApi
