package net.osmand.plus.widgets.alert;

import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Map;

public class MultiSelectionDialogFragment extends SelectionDialogFragment {

	private final ApplicationMode appMode;

	public MultiSelectionDialogFragment(final AlertDialog alertDialog,
										final AlertDialogData alertDialogData,
										final Map<String, CharSequence> itemByKey,
										final SelectionDialogAdapter adapter,
										final ApplicationMode appMode) {
		super(alertDialog, alertDialogData, itemByKey, adapter);
		this.appMode = appMode;
	}

	public static class MultiSelectionDialogFragmentProxy extends SelectionDialogFragmentProxy<MultiSelectionDialogFragment> {

		@Override
		public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final MultiSelectionDialogFragment selectionDialogFragment) {
			super.initializePreferenceFragmentWithFragmentBeforeOnCreate(selectionDialogFragment);
			setArguments(BaseSettingsFragment.buildArguments(selectionDialogFragment.appMode));
		}
	}
}
