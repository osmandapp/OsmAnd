package net.osmand.plus.configmap.tracks;

import java.util.Set;

public interface TrackItemsContainer {
	void updateContent();
	void onTrackItemsSelected(Set<TrackItem> trackItems);
}