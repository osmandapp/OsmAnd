package net.osmand.plus.settings.fragments;

import androidx.fragment.app.Fragment;

import net.osmand.plus.settings.fragments.search.SearchablePreferenceDialog;

public record SearchablePreferenceDialogFragmentHolder<T extends Fragment & SearchablePreferenceDialog>(
		T searchablePreferenceDialogFragment) {

	public static <T extends Fragment & SearchablePreferenceDialog> SearchablePreferenceDialogFragmentHolder<T> of(final T searchablePreferenceDialogFragment) {
		return new SearchablePreferenceDialogFragmentHolder<>(searchablePreferenceDialogFragment);
	}
}
