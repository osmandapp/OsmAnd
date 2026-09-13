package net.osmand.shared.util

/**
 * Marks a type the platform may serialize.
 *
 * On the JVM this is `java.io.Serializable`, which the Android OSM editor relies on to keep a
 * partially edited opening hours rule across configuration changes. Elsewhere it carries no
 * behaviour, it only lets shared classes declare the capability without depending on the JVM.
 */
expect interface PlatformSerializable
