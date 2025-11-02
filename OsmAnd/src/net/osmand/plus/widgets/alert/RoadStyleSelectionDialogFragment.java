package net.osmand.plus.widgets.alert;

import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Map;

public class RoadStyleSelectionDialogFragment extends SelectionDialogFragment {

	private final ApplicationMode appMode;

	public RoadStyleSelectionDialogFragment(final AlertDialog alertDialog,
											final AlertDialogData alertDialogData,
											final Map<String, CharSequence> itemByKey,
											final SelectionDialogAdapter adapter,
											final ApplicationMode appMode) {
		super(alertDialog, alertDialogData, itemByKey, adapter);
		this.appMode = appMode;
	}

	public static class RoadStyleSelectionDialogFragmentProxy extends SelectionDialogFragmentProxy<RoadStyleSelectionDialogFragment> {

		@Override
		public void initializePreferenceFragmentWithFragmentBeforeOnCreate(final RoadStyleSelectionDialogFragment roadStyleSelectionDialogFragment) {
			super.initializePreferenceFragmentWithFragmentBeforeOnCreate(roadStyleSelectionDialogFragment);
			setArguments(BaseSettingsFragment.buildArguments(roadStyleSelectionDialogFragment.appMode));
		}
	}
}
