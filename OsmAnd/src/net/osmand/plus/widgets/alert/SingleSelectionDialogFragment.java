package net.osmand.plus.widgets.alert;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;

public class SingleSelectionDialogFragment extends SelectionDialogFragment {

	public SingleSelectionDialogFragment(final AlertDialog alertDialog,
										 final AlertDialogData alertDialogData,
										 final Map<String, CharSequence> itemByKey,
										 final SelectionDialogAdapter adapter) {
		super(alertDialog, alertDialogData, itemByKey, adapter);
	}

	public static class SingleSelectionDialogFragmentProxy extends SelectionDialogFragment.PreferenceFragment<SingleSelectionDialogFragment> {
	}
}
