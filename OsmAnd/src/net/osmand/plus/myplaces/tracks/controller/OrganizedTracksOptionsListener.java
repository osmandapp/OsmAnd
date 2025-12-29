package net.osmand.plus.myplaces.tracks.controller;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.data.OrganizedTracks;

public interface OrganizedTracksOptionsListener {

	default void showOrganizedTracksDetails(@NonNull OrganizedTracks organizedTracks) {
	}

	default void showOrganizedTracksOnMap(@NonNull OrganizedTracks organizedTracks) {
	}

	default void showExportDialog(@NonNull OrganizedTracks organizedTracks) {
	}

}
