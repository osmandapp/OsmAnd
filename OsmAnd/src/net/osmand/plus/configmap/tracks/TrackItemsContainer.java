package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;

import java.util.Set;

public interface TrackItemsContainer {
	void updateContent();
	void updateItems(@NonNull Set<TrackItem> trackItems);
}