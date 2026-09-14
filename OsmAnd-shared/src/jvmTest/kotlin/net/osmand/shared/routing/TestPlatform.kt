package net.osmand.shared.routing

actual fun testEnvironment(name: String): String? = System.getenv(name)

actual fun testPlatformName(): String = "jvm " + System.getProperty("java.version")
