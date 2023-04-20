package net.osmand.plus.track.helpers.savegpx;

public interface SaveGpxListener {

	default void onSaveGpxStarted() { }

	void onSaveGpxFinished(Exception errorMessage);
}
