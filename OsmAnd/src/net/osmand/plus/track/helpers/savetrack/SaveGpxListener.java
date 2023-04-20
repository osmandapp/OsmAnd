package net.osmand.plus.track.helpers.savetrack;

public interface SaveGpxListener {

	default void onSaveGpxStarted() { }

	void onSaveGpxFinished(Exception errorMessage);
}
