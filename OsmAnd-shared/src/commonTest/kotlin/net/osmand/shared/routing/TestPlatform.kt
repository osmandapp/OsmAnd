package net.osmand.shared.routing

/** An environment variable of the test process, or null; how a test binary is told where files are. */
expect fun testEnvironment(name: String): String?

/** The platform a test ran on, for the numbers it prints. */
expect fun testPlatformName(): String

/** The bytes of the objects alive after a full collection of garbage, for what a test keeps. */
expect fun testHeapAfterGc(): Long

/**
 * [block] as one call of an app. On iOS the Objective-C objects it makes, the strings the collator and the normalization
 * of names create, are freed as the run loop of an app frees them: the autoreleased ones when the call returns, the ones
 * the collector releases on the main thread at the end of a later call. A test binary runs on a main thread without a
 * run loop and kept all of them, gigabytes over the queries of a test.
 */
expect fun <T> testAppCall(block: () -> T): T
