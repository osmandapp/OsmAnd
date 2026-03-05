package net.osmand.plus.myplaces.tracks.controller;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.data.OrganizedTracksGroup;

public interface OrganizedTracksOptionsListener {

	default void showOrganizedTracksDetails(@NonNull OrganizedTracksGroup organizedTracks) {
	}

	default void showOrganizedTracksOnMap(@NonNull OrganizedTracksGroup organizedTracks) {
	}

	default void showExportDialog(@NonNull OrganizedTracksGroup organizedTracks) {
	}

}
