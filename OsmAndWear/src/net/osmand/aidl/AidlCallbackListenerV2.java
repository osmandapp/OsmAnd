package net.osmand.aidl;

import net.osmand.aidlapi.IOsmAndAidlCallback;

import java.util.Map;

public interface AidlCallbackListenerV2 {

	/**
	 * Add AidlCallbackListener to OsmandAidlService's map of listeners. Key is unique to each AIDL
	 * method that wants to register callback and used to access only "own" callbacks.
	 *
	 * @param callback
	 * @param key      - every AIDL method which uses that register callbacks in service need to use its own bit key
	 *                 1 - key for registerForUpdates(...)
	 *                 2 - key for registerForNavigationUpdates(...)
	 *                 4 - key for onContextMenuButtonClicked(...)
	 *                 8 - key for... future use
	 *                 16 - key for... future use
	 * @return long - unique id of callback. Could be used for unregistering callback
	 */
	long addAidlCallback(IOsmAndAidlCallback callback, int key);

	/**
	 * Unregister AidlCallbackListener from OsmandAidlService's map
	 *
	 * @param id - unique id of callback
	 * @return - true if callback successfully unregistered
	 */
	boolean removeAidlCallback(long id);

	/**
	 * @return map of all callbacks. AidlCallbackParams contains method key and callback.
	 */
	Map<Long, OsmandAidlServiceV2.AidlCallbackParams> getAidlCallbacks();
}