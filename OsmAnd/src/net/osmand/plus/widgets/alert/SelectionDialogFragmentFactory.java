package net.osmand.plus.widgets.alert;

import android.view.View;

import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.settings.fragments.search.Collectors;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.common.Lists;

public class SelectionDialogFragmentFactory {

	public record DialogData(List<String> keys,
							 List<CharSequence> items,
							 Optional<boolean[]> checkedItems,
							 int selectedItemIndex) {

		public DialogData {
			if (keys.size() != items.size()) {
				throw new IllegalArgumentException("keys and items must have the same size");
			}
			if (checkedItems.isPresent() && checkedItems.orElseThrow().length != keys.size()) {
				throw new IllegalArgumentException("checkedItems must have the same size as keys");
			}
		}

		public Map<String, CharSequence> orderedItemByKey() {
			return Lists
					.zip(keys(), items())
					.stream()
					.collect(
							Collectors.toOrderedMap(
									keyItemPair -> keyItemPair.first,
									keyItemPair -> keyItemPair.second));

		}
	}

	public static MapLayerSelectionDialogFragment createMapLayerSelectionDialogFragment(
			final AlertDialogData data,
			final DialogData dialogData,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				dialogData,
				itemClickListener,
				false,
				MapLayerSelectionDialogFragment::new);
	}

	public static RoadStyleSelectionDialogFragment createRoadStyleSelectionDialogFragment(
			final AlertDialogData data,
			final DialogData dialogData,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				dialogData,
				itemClickListener,
				false,
				RoadStyleSelectionDialogFragment::new);
	}

	public static MultiSelectionDialogFragment createMultiSelectionDialogFragment(
			final AlertDialogData data,
			final DialogData dialogData,
			final View.OnClickListener itemClickListener) {
		return createSelectionDialogFragment(
				data,
				dialogData,
				itemClickListener,
				true,
				MultiSelectionDialogFragment::new);
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
			final DialogData dialogData,
			final View.OnClickListener itemClickListener,
			final boolean useMultiSelection,
			final _SelectionDialogFragmentFactory<F> selectionDialogFragmentFactory) {
		final SelectionDialogAdapter adapter =
				new SelectionDialogAdapter(
						data.getContext(),
						dialogData.items().toArray(new CharSequence[0]),
						dialogData.selectedItemIndex(),
						dialogData.checkedItems().orElse(null),
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
				dialogData.orderedItemByKey(),
				adapter);
	}
}
