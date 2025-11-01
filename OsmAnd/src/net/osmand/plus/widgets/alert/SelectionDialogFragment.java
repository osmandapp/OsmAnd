package net.osmand.plus.widgets.alert;

import static net.osmand.plus.configmap.ConfigureMapDialogs.MapLanguageDialog.getViewByPosition;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import net.osmand.plus.configmap.ViewOfSettingHighlighter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.InitializePreferenceFragmentWithFragmentBeforeOnCreate;
import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighterProvider;

public class SelectionDialogFragment extends DialogFragment implements SettingHighlighterProvider {

	private final AlertDialog alertDialog;
	private final AlertDialogData alertDialogData;
	public final Map<String, CharSequence> itemByKey;
	private final SelectionDialogAdapter adapter;

	public SelectionDialogFragment(final AlertDialog alertDialog,
								   final AlertDialogData alertDialogData,
								   final Map<String, CharSequence> itemByKey,
								   final SelectionDialogAdapter adapter) {
		this.alertDialog = alertDialog;
		this.alertDialogData = alertDialogData;
		this.itemByKey = itemByKey;
		this.adapter = adapter;
	}

	public void show(final FragmentManager fragmentManager) {
		show(fragmentManager, null);
		CustomAlert.applyAdditionalParameters(alertDialog, alertDialogData);
	}

	public void showNow(final FragmentManager fragmentManager) {
		showNow(fragmentManager, null);
		CustomAlert.applyAdditionalParameters(alertDialog, alertDialogData);
	}

	public void setSelectedIndex(final int selectedIndex) {
		adapter.setSelectedIndex(selectedIndex);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		return alertDialog;
	}

	@Override
	public SettingHighlighter getSettingHighlighter() {
		return new ViewOfSettingHighlighter(
				this::getView,
				Duration.ofSeconds(1));
	}

	public ListView getListView() {
		return ((AlertDialog) getDialog()).getListView();
	}

	public int getIndexedOf(final Setting setting) {
		return getKeys().indexOf(setting.getKey());
	}

	private View getView(final Setting setting) {
		return getViewByPosition(getListView(), getIndexedOf(setting));
	}

	private List<String> getKeys() {
		return new ArrayList<>(itemByKey.keySet());
	}

	public static class SelectionDialogFragmentProxy<Principal extends SelectionDialogFragment> extends PreferenceFragmentCompat implements InitializePreferenceFragmentWithFragmentBeforeOnCreate<Principal> {

		private Principal selectionDialogFragment;

		public Principal getPrincipal() {
			return selectionDialogFragment;
		}

		@Override
		public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final Principal selectionDialogFragment) {
			this.selectionDialogFragment = selectionDialogFragment;
		}

		@Override
		public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
			setPreferenceScreen(asPreferenceScreen(asPreferences(selectionDialogFragment.itemByKey)));
		}

		private Collection<Preference> asPreferences(final Map<String, CharSequence> itemByKey) {
			return new TitleByKey2PreferencesConverter(requireContext()).asPreferences(itemByKey);
		}

		private PreferenceScreen asPreferenceScreen(final Collection<Preference> preferences) {
			return new PreferenceScreenFactory(this).asPreferenceScreen(preferences);
		}
	}
}
