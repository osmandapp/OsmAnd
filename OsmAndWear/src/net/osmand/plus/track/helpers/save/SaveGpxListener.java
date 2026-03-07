package net.osmand.plus.track.helpers.save;

public interface SaveGpxListener {

	default void onSaveGpxStarted() {
	}

	void onSaveGpxFinished(Exception errorMessage);
}
