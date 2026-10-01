package net.osmand.shared.routing

/** An environment variable of the test process, or null; how a test binary is told where files are. */
expect fun testEnvironment(name: String): String?

/** The platform a test ran on, for the numbers it prints. */
expect fun testPlatformName(): String
