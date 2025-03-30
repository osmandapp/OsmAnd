package net.osmand.plus.widgets.alert;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;

public class RoadStyleSelectionDialogFragment extends SelectionDialogFragment {

	public RoadStyleSelectionDialogFragment(final AlertDialog alertDialog,
											final AlertDialogData alertDialogData,
											final Map<String, CharSequence> itemByKey,
											final SelectionDialogAdapter adapter) {
		super(alertDialog, alertDialogData, itemByKey, adapter);
	}

	public static class RoadStyleSelectionDialogFragmentProxy extends SelectionDialogFragmentProxy<RoadStyleSelectionDialogFragment> {
	}
}
