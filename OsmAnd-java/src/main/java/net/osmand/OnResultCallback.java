package net.osmand;

/**
 * Simple callback interface to indicate about
 * completion and return result of some process
 */
public interface OnResultCallback<T> {
	void onResult(T result);
}
