package net.osmand.plus.settings.fragments.search;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import java.util.List;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.converter.IdGenerator;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferencePOJO;

class POJOTestFactory {

	private static final IdGenerator idGenerator = new IdGenerator();

	public static SearchablePreferencePOJO createSearchablePreferencePOJO(
			final String title,
			final Class<? extends PreferenceFragmentCompat> host) {
		final SearchablePreferencePOJO searchablePreferencePOJO =
				new SearchablePreferencePOJO(
						idGenerator.nextId(),
						Optional.of(title),
						Optional.empty(),
						0,
						Optional.empty(),
						Optional.of(title),
						0,
						Optional.empty(),
						true,
						Optional.empty(),
						new Bundle(),
						List.of());
		searchablePreferencePOJO.setHost(host);
		return searchablePreferencePOJO;
	}

	public static SearchablePreferencePOJO copy(final SearchablePreferencePOJO preference) {
		return createSearchablePreferencePOJO(
				preference.getTitle().orElseThrow(),
				preference.getHost());
	}
}
