package net.osmand;

/**
 * A listener interface for handling state changes with a specific change value.
 * Implementations of this interface are notified when a state changes
 * and receive additional details about the change.
 *
 * @param <T> The type of the change being tracked (e.g., String, Boolean, custom object).
 */
public interface StateChangedListener<T> {
	/**
	 * Called when a state change occurs with additional information about the change.
	 *
	 * @param change The new state or value representing the change.
	 */
	void stateChanged(T change);
}
