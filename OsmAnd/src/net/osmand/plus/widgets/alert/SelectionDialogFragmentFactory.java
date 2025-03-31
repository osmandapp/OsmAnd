package net.osmand.plus.widgets.alert;

import android.view.View;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;
import java.util.function.Function;

public class SelectionDialogFragmentFactory {

	public static MapLayerSelectionDialogFragment createMapLayerSelectionDialogFragment(
			final AlertDialogData data,
			final Map<String, CharSequence> itemByKey,
			final int selectedEntryIndex,
			final View.OnClickListener itemClickListener,
			final Function<AlertDialogData, AlertDialog.Builder> createAlertDialogBuilder) {
		final DialogAndAdapter dialogAndAdapter =
				createDialogAndAdapter(
						data,
						itemByKey,
						selectedEntryIndex,
						itemClickListener,
						createAlertDialogBuilder);
		return new MapLayerSelectionDialogFragment(
				dialogAndAdapter.dialog(),
				data,
				itemByKey,
				dialogAndAdapter.adapter());
	}

	public static RoadStyleSelectionDialogFragment createRoadStyleSelectionDialogFragment(
			final AlertDialogData data,
			final Map<String, CharSequence> itemByKey,
			final int selectedEntryIndex,
			final View.OnClickListener itemClickListener,
			final Function<AlertDialogData, AlertDialog.Builder> createAlertDialogBuilder) {
		final DialogAndAdapter dialogAndAdapter =
				createDialogAndAdapter(
						data,
						itemByKey,
						selectedEntryIndex,
						itemClickListener,
						createAlertDialogBuilder);
		return new RoadStyleSelectionDialogFragment(
				dialogAndAdapter.dialog(),
				data,
				itemByKey,
				dialogAndAdapter.adapter());
	}

	private record DialogAndAdapter(AlertDialog dialog, SelectionDialogAdapter adapter) {
	}

	private static DialogAndAdapter createDialogAndAdapter(final AlertDialogData data,
														   final Map<String, CharSequence> itemByKey,
														   final int selectedEntryIndex,
														   final View.OnClickListener itemClickListener,
														   final Function<AlertDialogData, AlertDialog.Builder> createAlertDialogBuilder) {
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
				createAlertDialogBuilder
						.apply(data)
						.setAdapter(adapter, null)
						.create();
		adapter.setDialog(alertDialog);
		return new DialogAndAdapter(alertDialog, adapter);
	}
}
