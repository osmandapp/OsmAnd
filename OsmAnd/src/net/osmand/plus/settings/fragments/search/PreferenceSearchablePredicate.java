package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.R;
import net.osmand.plus.settings.fragments.MainSettingsFragment;

import java.util.Set;

class PreferenceSearchablePredicate implements de.KnollFrank.lib.settingssearch.provider.PreferenceSearchablePredicate {

	private static final Set<Integer> NON_SEARCHABLE_LAYOUT_RESIDS =
			Set.of(
					R.layout.simple_divider_item,
					R.layout.list_item_divider,
					R.layout.card_bottom_divider,
					R.layout.divider_half_item,
					R.layout.divider_half_item_with_background,
					R.layout.divider_item_with_background_56,
					R.layout.divider,
					R.layout.drawer_divider);

	@Override
	public boolean isPreferenceSearchable(final Preference preference, final PreferenceFragmentCompat hostOfPreference) {
		return !NON_SEARCHABLE_LAYOUT_RESIDS.contains(preference.getLayoutResource()) && !MainSettingsFragment.SELECTED_PROFILE.equals(preference.getKey());
	}
}
