package net.osmand.plus.widgets.alert;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.Collection;
import java.util.Map;

import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.InitializePreferenceFragmentWithFragmentBeforeOnCreate;

public class MultiSelectionDialogFragment extends SelectionDialogFragment {

	public MultiSelectionDialogFragment(final AlertDialog alertDialog,
										final AlertDialogData alertDialogData,
										final Map<String, CharSequence> itemByKey,
										final SelectionDialogAdapter adapter) {
		super(alertDialog, alertDialogData, itemByKey, adapter);
	}

	public static class PreferenceFragment extends PreferenceFragmentCompat implements InitializePreferenceFragmentWithFragmentBeforeOnCreate<MultiSelectionDialogFragment> {

		private MultiSelectionDialogFragment multiSelectionDialogFragment;

		@Override
		public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final MultiSelectionDialogFragment multiSelectionDialogFragment) {
			this.multiSelectionDialogFragment = multiSelectionDialogFragment;
		}

		public MultiSelectionDialogFragment getPrincipal() {
			return multiSelectionDialogFragment;
		}

		@Override
		public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
			setPreferenceScreen(asPreferenceScreen(asPreferences(multiSelectionDialogFragment.itemByKey)));
		}

		private Collection<Preference> asPreferences(final Map<String, CharSequence> itemByKey) {
			return new TitleByKey2PreferencesConverter(getContext()).asPreferences(itemByKey);
		}

		private PreferenceScreen asPreferenceScreen(final Collection<Preference> preferences) {
			return new PreferenceScreenFactory(this).asPreferenceScreen(preferences);
		}
	}
}
