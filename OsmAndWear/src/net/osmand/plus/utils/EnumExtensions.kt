package net.osmand.plus.utils

inline fun <reified T : Enum<T>> T.next(): T {
	val values = enumValues<T>()
	return values[(ordinal + 1) % values.size]
}

inline fun <reified T : Enum<T>> T.previous(): T {
	val values = enumValues<T>()
	return values[(ordinal - 1 + values.size) % values.size]
}
