package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.Fragment;

import de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoByPreferenceDialogProvider;

public abstract class ShowableSearchablePreferenceDialog<T extends Fragment & SearchablePreferenceDialog> {

	public final T searchablePreferenceDialog;

	public ShowableSearchablePreferenceDialog(final T searchablePreferenceDialog) {
		this.searchablePreferenceDialog = searchablePreferenceDialog;
	}

	public void show() {
		show(searchablePreferenceDialog);
	}

	public PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<T> asPreferenceDialogAndSearchableInfoByPreferenceDialogProvider() {
		return new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<>(
				searchablePreferenceDialog,
				SearchablePreferenceDialog::getSearchableInfo);
	}

	protected abstract void show(final T searchablePreferenceDialog);
}
