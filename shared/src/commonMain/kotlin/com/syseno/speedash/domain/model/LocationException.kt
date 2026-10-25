package com.syseno.speedash.domain.model

/**
 * Thrown by location sources when location permission is not granted, denied, or restricted.
 *
 * Lives in the domain layer so the presentation layer can react to it without depending on `data`.
 */
class LocationPermissionException(message: String) : IllegalStateException(message)

/** Thrown by location sources when the platform location provider fails to deliver updates. */
class LocationUnavailableException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)
