package net.osmand.plus.widgets.alert;

import android.view.View;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;

public class SelectionDialogFragmentFactory {

	public static MapLayerSelectionDialogFragment createMapLayerSelectionDialogFragment(
			final AlertDialogData data,
			final Map<String, CharSequence> itemByKey,
			final int selectedEntryIndex,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				itemByKey,
				selectedEntryIndex,
				itemClickListener,
				MapLayerSelectionDialogFragment::new);
	}

	public static RoadStyleSelectionDialogFragment createRoadStyleSelectionDialogFragment(
			final AlertDialogData data,
			final Map<String, CharSequence> itemByKey,
			final int selectedEntryIndex,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				itemByKey,
				selectedEntryIndex,
				itemClickListener,
				RoadStyleSelectionDialogFragment::new);
	}

	@FunctionalInterface
	private interface _SelectionDialogFragmentFactory<F extends SelectionDialogFragment> {

		F create(AlertDialog alertDialog,
				 AlertDialogData alertDialogData,
				 Map<String, CharSequence> itemByKey,
				 SelectionDialogAdapter adapter);
	}

	private static <F extends SelectionDialogFragment> F createSelectionDialogFragment(
			final AlertDialogData data,
			final Map<String, CharSequence> itemByKey,
			final int selectedEntryIndex,
			final View.OnClickListener itemClickListener,
			final _SelectionDialogFragmentFactory<F> selectionDialogFragmentFactory) {
		final SelectionDialogAdapter adapter =
				new SelectionDialogAdapter(
						data.getContext(),
						itemByKey.values().toArray(new CharSequence[0]),
						selectedEntryIndex,
						null,
						data.getControlsColor(),
						data.isNightMode(),
						itemClickListener,
						false);
		final AlertDialog alertDialog =
				CustomAlert
						.createAlertDialogBuilder(data)
						.setAdapter(adapter, null)
						.create();
		adapter.setDialog(alertDialog);
		return selectionDialogFragmentFactory.create(alertDialog, data, itemByKey, adapter);
	}
}
