package net.osmand.plus.widgets.alert;

import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Map;

public class InstallMapLayersDialogFragment extends SelectionDialogFragment {

	private final ApplicationMode appMode;

	public InstallMapLayersDialogFragment(final AlertDialog alertDialog,
										  final AlertDialogData alertDialogData,
										  final Map<String, CharSequence> itemByKey,
										  final SelectionDialogAdapter adapter,
										  final ApplicationMode appMode) {
		super(alertDialog, alertDialogData, itemByKey, adapter);
		this.appMode = appMode;
	}

	public static class InstallMapLayersDialogFragmentProxy extends SelectionDialogFragmentProxy<InstallMapLayersDialogFragment> {

		@Override
		public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final InstallMapLayersDialogFragment selectionDialogFragment) {
			super.initializePreferenceFragmentWithFragmentBeforeOnCreate(selectionDialogFragment);
			setArguments(BaseSettingsFragment.buildArguments(selectionDialogFragment.appMode));
		}
	}
}
