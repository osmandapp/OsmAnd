package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.Set;
import java.util.stream.Collectors;

class PreferencePathDisplayerFactory {

	public static de.KnollFrank.lib.settingssearch.results.recyclerview.PreferencePathDisplayer createPreferencePathDisplayer(final Context context) {
		return new PreferencePathDisplayer(context, getApplicationModeKeys());
	}

	public static Set<String> getApplicationModeKeys() {
		return ApplicationMode
				.allPossibleValues()
				.stream()
				.map(ApplicationMode::getStringKey)
				.collect(Collectors.toSet());
	}
}
