package net.osmand.shared.util

/** Collects the garbage now, so the memory is back before the next allocation-heavy step. */
expect fun runGarbageCollector()
