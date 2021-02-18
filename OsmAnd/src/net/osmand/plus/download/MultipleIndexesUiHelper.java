package net.osmand.plus.download;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet.OnApplySelectionListener;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet.SelectableItem;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet.SelectionUpdateListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class MultipleIndexesUiHelper {

	public static void showDialog(@NonNull MultipleIndexItem multipleIndexItem,
	                              @NonNull AppCompatActivity activity,
	                              @NonNull final OsmandApplication app,
	                              @NonNull DateFormat dateFormat,
	                              boolean showRemoteDate,
	                              @NonNull final SelectItemsToDownloadListener listener) {
		List<IndexItem> indexesToDownload = getIndexesToDownload(multipleIndexItem);
		List<SelectableItem> allItems = new ArrayList<>();
		List<SelectableItem> selectedItems = new ArrayList<>();
		OsmandRegions osmandRegions = app.getRegions();
		for (IndexItem indexItem : multipleIndexItem.getAllIndexes()) {
			SelectableItem selectableItem = new SelectableItem();
			selectableItem.setTitle(indexItem.getVisibleName(app, osmandRegions, false));

			String size = indexItem.getSizeDescription(app);
			String date = indexItem.getDate(dateFormat, showRemoteDate);
			String description = app.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, date);
			selectableItem.setDescription(description);

			selectableItem.setIconId(indexItem.getType().getIconResource());
			selectableItem.setObject(indexItem);
			allItems.add(selectableItem);

			if (indexesToDownload.contains(indexItem)) {
				selectedItems.add(selectableItem);
			}
		}

		final SelectMultipleItemsBottomSheet dialog =
				SelectMultipleItemsBottomSheet.showInstance(activity, allItems, selectedItems, true);

		dialog.setSelectionUpdateListener(new SelectionUpdateListener() {
			@Override
			public void onSelectionUpdate() {
				dialog.setTitle(app.getString(R.string.welmode_download_maps));
				String total = app.getString(R.string.shared_string_total);
				double sizeToDownload = getDownloadSizeInMb(dialog.getSelectedItems());
				String size = DownloadItem.getFormattedMb(app, sizeToDownload);
				String description =
						app.getString(R.string.ltr_or_rtl_combine_via_colon, total, size);
				dialog.setDescription(description);
				String btnTitle = app.getString(R.string.shared_string_download);
				if (sizeToDownload > 0) {
					btnTitle = app.getString(R.string.ltr_or_rtl_combine_via_dash, btnTitle, size);
				}
				dialog.setConfirmButtonTitle(btnTitle);
			}
		});

		dialog.setOnApplySelectionListener(new OnApplySelectionListener() {
			@Override
			public void onSelectionApplied(List<SelectableItem> selectedItems) {
				List<IndexItem> indexItems = new ArrayList<>();
				for (SelectableItem item : selectedItems) {
					Object obj = item.getObject();
					if (obj instanceof IndexItem) {
						indexItems.add((IndexItem) obj);
					}
				}
				listener.onItemsToDownloadSelected(indexItems);
			}
		});
	}

	private static List<IndexItem> getIndexesToDownload(MultipleIndexItem multipleIndexItem) {
		if (multipleIndexItem.hasActualDataToDownload()) {
			// download left regions
			return multipleIndexItem.getIndexesToDownload();
		} else {
			// download all regions again
			return multipleIndexItem.getAllIndexes();
		}
	}

	private static double getDownloadSizeInMb(@NonNull List<SelectableItem> selectableItems) {
		List<IndexItem> indexItems = new ArrayList<>();
		for (SelectableItem i : selectableItems) {
			Object obj = i.getObject();
			if (obj instanceof IndexItem) {
				indexItems.add((IndexItem) obj);
			}
		}
		double totalSizeMb = 0.0d;
		for (IndexItem item : indexItems) {
			totalSizeMb += item.getSizeToDownloadInMb();
		}
		return totalSizeMb;
	}

	public interface SelectItemsToDownloadListener {
		void onItemsToDownloadSelected(List<IndexItem> items);
	}

}
