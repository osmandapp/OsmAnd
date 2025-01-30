package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.RoutingDataObject;

import java.util.List;
import java.util.stream.Collectors;

public class SearchableInfoHelper {

	public static List<String> getProfileDescriptions(final List<? extends ProfileDataObject> profiles) {
		return profiles
				.stream()
				.map(SearchableInfoHelper::getProfileDescription)
				.collect(Collectors.toList());
	}

	public static List<String> getProfileNames(final List<RoutingDataObject> profilesList) {
		return profilesList
				.stream()
				.map(ProfileDataObject::getName)
				.collect(Collectors.toList());
	}

	private static String getProfileDescription(final ProfileDataObject profile) {
		return String.format("%s (%s)", profile.getName(), profile.getDescription());
	}
}
