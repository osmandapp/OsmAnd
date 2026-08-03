package net.osmand.plus.profiles.data;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

import java.util.List;

public class ProfilesGroup {

	protected CharSequence title;
	protected CharSequence description;
	protected List<RoutingDataObject> profiles;

	public ProfilesGroup(@NonNull String title,
	                     @NonNull List<RoutingDataObject> profiles) {
		this.title = title;
		this.profiles = profiles;
	}

	public CharSequence getTitle() {
		return title;
	}

	public CharSequence getDescription(@NonNull OsmandApplication ctx,
	                                   boolean nightMode) {
		return description;
	}

	public void setDescription(CharSequence description) {
		this.description = description;
	}

	public List<RoutingDataObject> getProfiles() {
		return profiles;
	}
}
