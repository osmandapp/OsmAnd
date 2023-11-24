package net.osmand.plus.track.data;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.configmap.tracks.TrackItem;

import java.util.List;

public interface TracksGroup {

	@NonNull
	String getName(@NonNull Context context);

	@NonNull
	List<TrackItem> getTrackItems();
}
