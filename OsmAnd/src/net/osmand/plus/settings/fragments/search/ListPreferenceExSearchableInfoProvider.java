package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SearchableInfoProviderHelper.addIfNonNull;
import static net.osmand.plus.settings.fragments.search.SearchableInfoProviderHelper.join;
import static net.osmand.plus.settings.fragments.search.SearchableInfoProviderHelper.nullToEmpty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import net.osmand.plus.settings.preferences.ListPreferenceEx;

import java.util.Arrays;
import java.util.List;

import de.KnollFrank.lib.preferencesearch.search.provider.SearchableInfoProvider;

class ListPreferenceExSearchableInfoProvider implements SearchableInfoProvider<ListPreferenceEx> {

	@Override
	public String getSearchableInfo(final ListPreferenceEx preference) {
		return join(
				", ",
				getSearchableInfos(
						preference.getEntries(),
						preference.getDescription()));
	}

	static List<CharSequence> getSearchableInfos(final CharSequence[] entries, final String description) {
		final Builder<CharSequence> builder = ImmutableList.builder();
		builder.addAll(Arrays.asList(nullToEmpty(entries)));
		addIfNonNull(builder, description);
		return builder.build();
	}
}
