package net.osmand.plus.widgets.alert;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.Collection;
import java.util.Map;

import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.InitializePreferenceFragmentWithFragmentBeforeOnCreate;
import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighterProvider;

public class MultiSelectionDialogFragment extends DialogFragment implements SettingHighlighterProvider {

	private final SelectionDialogFragment delegate;

	public MultiSelectionDialogFragment(final AlertDialog alertDialog,
										final AlertDialogData alertDialogData,
										final Map<String, CharSequence> itemByKey,
										final SelectionDialogAdapter adapter) {
		delegate = new SelectionDialogFragment(this, alertDialog, alertDialogData, itemByKey, adapter);
	}

	public Map<String, CharSequence> getItemByKey() {
		return delegate.itemByKey;
	}

	public void show(final FragmentManager fragmentManager) {
		delegate.show(fragmentManager);
	}

	public void showNow(final FragmentManager fragmentManager) {
		delegate.showNow(fragmentManager);
	}

	public void setSelectedIndex(final int selectedIndex) {
		delegate.setSelectedIndex(selectedIndex);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		return delegate.onCreateDialog();
	}

	@Override
	public SettingHighlighter getSettingHighlighter() {
		return delegate.getSettingHighlighter();
	}

	public ListView getListView() {
		return delegate.getListView();
	}

	public int getIndexedOf(final Setting setting) {
		return delegate.getIndexedOf(setting);
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
			setPreferenceScreen(asPreferenceScreen(asPreferences(multiSelectionDialogFragment.getItemByKey())));
		}

		private Collection<Preference> asPreferences(final Map<String, CharSequence> itemByKey) {
			return new TitleByKey2PreferencesConverter(getContext()).asPreferences(itemByKey);
		}

		private PreferenceScreen asPreferenceScreen(final Collection<Preference> preferences) {
			return new PreferenceScreenFactory(this).asPreferenceScreen(preferences);
		}
	}
}
