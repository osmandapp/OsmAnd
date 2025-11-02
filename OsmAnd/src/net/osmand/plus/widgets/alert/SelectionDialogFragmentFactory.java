package net.osmand.plus.widgets.alert;

import android.view.View;

import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.Map;
import java.util.Optional;

public class SelectionDialogFragmentFactory {

	public static MapLayerSelectionDialogFragment createMapLayerSelectionDialogFragment(
			final Optional<InstallMapLayersDialogFragment> installMapLayersDialogFragment,
			final AlertDialogData data,
			final SelectionDialogFragmentData selectionDialogFragmentData,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				selectionDialogFragmentData,
				itemClickListener,
				false,
				(alertDialog, alertDialogData, itemByKey, adapter) ->
						new MapLayerSelectionDialogFragment(
								installMapLayersDialogFragment,
								alertDialog,
								alertDialogData,
								itemByKey,
								adapter));
	}

	public static RoadStyleSelectionDialogFragment createRoadStyleSelectionDialogFragment(
			final AlertDialogData data,
			final SelectionDialogFragmentData selectionDialogFragmentData,
			final View.OnClickListener itemClickListener,
			final ApplicationMode appMode) {
		return createSelectionDialogFragment(
				data,
				selectionDialogFragmentData,
				itemClickListener,
				false,
				new _SelectionDialogFragmentFactory<>() {

					@Override
					public RoadStyleSelectionDialogFragment create(final AlertDialog alertDialog,
																   final AlertDialogData alertDialogData,
																   final Map<String, CharSequence> itemByKey,
																   final SelectionDialogAdapter adapter) {
						return new RoadStyleSelectionDialogFragment(alertDialog, alertDialogData, itemByKey, adapter, appMode);
					}
				});
	}

	public static MultiSelectionDialogFragment createMultiSelectionDialogFragment(
			final AlertDialogData data,
			final SelectionDialogFragmentData selectionDialogFragmentData,
			final View.OnClickListener itemClickListener,
			final ApplicationMode appMode) {
		return createSelectionDialogFragment(
				data,
				selectionDialogFragmentData,
				itemClickListener,
				true,
				new _SelectionDialogFragmentFactory<>() {

					@Override
					public MultiSelectionDialogFragment create(final AlertDialog alertDialog,
															   final AlertDialogData alertDialogData,
															   final Map<String, CharSequence> itemByKey,
															   final SelectionDialogAdapter adapter) {
						return new MultiSelectionDialogFragment(alertDialog, alertDialogData, itemByKey, adapter, appMode);
					}
				});
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
