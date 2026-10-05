package net.osmand.shared.routing

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun testEnvironment(name: String): String? = getenv(name)?.toKString()

actual fun testPlatformName(): String = "kotlin/native ios"
