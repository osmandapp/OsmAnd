package net.osmand.shared.util

/**
 * Returns a shared instance of [value] where the platform keeps a string pool.
 *
 * Every loaded region repeats the same routing tags and values, so pooling them keeps
 * one copy per tag instead of one per region.
 */
expect fun internString(value: String): String
