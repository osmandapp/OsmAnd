package net.osmand.plus.widgets.alert;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;

public class MapLayerSelectionDialogFragment extends SelectionDialogFragment {

	public MapLayerSelectionDialogFragment(final AlertDialog alertDialog,
										   final AlertDialogData alertDialogData,
										   final Map<String, CharSequence> itemByKey,
										   final SelectionDialogAdapter adapter) {
		super(alertDialog, alertDialogData, itemByKey, adapter);
	}

	public static class MapLayerSelectionDialogFragmentProxy extends SelectionDialogFragmentProxy<MapLayerSelectionDialogFragment> {
	}
}
