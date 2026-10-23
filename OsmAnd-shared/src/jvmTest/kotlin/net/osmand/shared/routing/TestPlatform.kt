package net.osmand.shared.routing

actual fun testEnvironment(name: String): String? = System.getenv(name)

actual fun testPlatformName(): String = "jvm " + System.getProperty("java.version")

actual fun testHeapAfterGc(): Long {
	val runtime = Runtime.getRuntime()
	System.gc()
	return runtime.totalMemory() - runtime.freeMemory()
}

actual fun <T> testAppCall(block: () -> T): T = block()
