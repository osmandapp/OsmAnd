package net.osmand.plus.profiles;

import net.osmand.plus.profiles.data.ProfileDataObject;

import java.util.List;
import java.util.stream.Collectors;

class SearchableInfoHelper {

	public static List<String> getProfileDescriptions(final List<? extends ProfileDataObject> profiles) {
		return profiles
				.stream()
				.map(SearchableInfoHelper::getProfileDescription)
				.collect(Collectors.toList());
	}

	private static String getProfileDescription(final ProfileDataObject profile) {
		return String.format("%s (%s)", profile.getName(), profile.getDescription());
	}
}
