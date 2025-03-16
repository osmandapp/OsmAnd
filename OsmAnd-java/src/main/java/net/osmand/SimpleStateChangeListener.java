package net.osmand;

/**
 * A simplified listener for handling state changes without receiving additional data.
 * This is useful when the occurrence of a change is important,
 * but the specific details of the change are not needed.
 */
public interface SimpleStateChangeListener extends IStateChangeListener {
	/**
	 * Called when a state change occurs.
	 * Unlike {@link StateChangedListener}, this method does not provide details about the change.
	 */
	void onStateChanged();
}
