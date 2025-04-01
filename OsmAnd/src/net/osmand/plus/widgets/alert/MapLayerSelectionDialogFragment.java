package net.osmand.plus.widgets.alert;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;

// FK-FIXME: suche nach CyclOSM, klicke auf eines der Suchergebnisse, dann wird der Dialog fehlerhafterweise nicht geöffnet und folglich wird auch CyclOSM nicht gehighlightet, vielleicht weil CyclOSM ein Fahrradsymbol hat?
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
