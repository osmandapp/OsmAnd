package net.osmand.plus.widgets.alert;

import android.view.View;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;

public class SelectionDialogFragmentFactory {

	public static MapLayerSelectionDialogFragment createMapLayerSelectionDialogFragment(
			final AlertDialogData data,
			final SelectionDialogFragmentData selectionDialogFragmentData,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				selectionDialogFragmentData,
				itemClickListener,
				false,
				MapLayerSelectionDialogFragment::new);
	}

	public static RoadStyleSelectionDialogFragment createRoadStyleSelectionDialogFragment(
			final AlertDialogData data,
			final SelectionDialogFragmentData selectionDialogFragmentData,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				selectionDialogFragmentData,
				itemClickListener,
				false,
				RoadStyleSelectionDialogFragment::new);
	}

	public static MultiSelectionDialogFragment createMultiSelectionDialogFragment(
			final AlertDialogData data,
			final SelectionDialogFragmentData selectionDialogFragmentData,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				selectionDialogFragmentData,
				itemClickListener,
				true,
				MultiSelectionDialogFragment::new);
	}

	public static InstallMapLayersDialogFragment createInstallMapLayersDialogFragment(
			final AlertDialogData data,
			final SelectionDialogFragmentData selectionDialogFragmentData,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				selectionDialogFragmentData,
				itemClickListener,
				true,
				InstallMapLayersDialogFragment::new);
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
			final SelectionDialogFragmentData selectionDialogFragmentData,
			final View.OnClickListener itemClickListener,
			final boolean useMultiSelection,
			final _SelectionDialogFragmentFactory<F> selectionDialogFragmentFactory) {
		final SelectionDialogAdapter adapter =
				new SelectionDialogAdapter(
						data.getContext(),
						selectionDialogFragmentData.items().toArray(new CharSequence[0]),
						selectionDialogFragmentData.selectedItemIndex(),
						selectionDialogFragmentData.checkedItems().orElse(null),
						data.getControlsColor(),
						data.isNightMode(),
						itemClickListener,
						useMultiSelection);
		final AlertDialog alertDialog =
				CustomAlert
						.createAlertDialogBuilder(data)
						.setAdapter(adapter, null)
						.create();
		adapter.setDialog(alertDialog);
		return selectionDialogFragmentFactory.create(
				alertDialog,
				data,
				selectionDialogFragmentData.orderedItemByKey(),
				adapter);
	}
}
