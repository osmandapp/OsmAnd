package net.osmand.shared.api

/**
 * Abstract listener represents state changed for a particular object
 */
interface KStateChangedListener<T> {
	fun stateChanged(change: T)
}
