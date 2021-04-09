package net.osmand.plus.download;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.IndexConstants;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet.OnApplySelectionListener;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet.OnRadioButtonSelectListener;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet.SelectableItem;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet.SelectionUpdateListener;
import net.osmand.plus.helpers.enums.MetricsConstants;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT_ZIP;
import static net.osmand.plus.download.DownloadActivityType.SRTM_COUNTRY_FILE;

public class MultipleIndexesUiHelper {

	public static void showDialog(@NonNull DownloadItem item,
	                              @NonNull AppCompatActivity activity,
	                              @NonNull final OsmandApplication app,
	                              @NonNull DateFormat dateFormat,
	                              boolean showRemoteDate,
	                              @NonNull final SelectItemsToDownloadListener listener) {
		if (item.getType() == SRTM_COUNTRY_FILE) {
			showSRTMDialog(item, activity, app, dateFormat, showRemoteDate, listener);
		} else if (item instanceof MultipleIndexItem) {
			showBaseDialog((MultipleIndexItem) item, activity, app, dateFormat, showRemoteDate, listener);
		}
	}

	public static void showBaseDialog(@NonNull MultipleIndexItem multipleIndexItem,
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
				updateSize(app, dialog);
			}
		});
		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	public static void showSRTMDialog(@NonNull final DownloadItem downloadItem,
	                                  @NonNull AppCompatActivity activity,
	                                  @NonNull final OsmandApplication app,
	                                  @NonNull final DateFormat dateFormat,
	                                  final boolean showRemoteDate,
	                                  @NonNull final SelectItemsToDownloadListener listener) {
		List<SelectableItem> selectedItems = new ArrayList<>();
		final List<SelectableItem> leftItems = new ArrayList<>();
		final List<SelectableItem> rightItems = new ArrayList<>();
		List<IndexItem> indexesToDownload = getIndexesToDownload(downloadItem);
		boolean baseSRTM = isBaseSRTMMetricSystem(app);

		List<IndexItem> allIndexes = new ArrayList<>();
		if (downloadItem instanceof MultipleIndexItem) {
			allIndexes.addAll(((MultipleIndexItem) downloadItem).getAllIndexes());
		} else {
			for (IndexItem indexItem : downloadItem.getRelatedGroup().getIndividualResources()) {
				if (indexItem.getType() == SRTM_COUNTRY_FILE) {
					allIndexes.add(indexItem);
				}
			}
		}

		for (IndexItem indexItem : allIndexes) {
			boolean baseItem = isBaseSRTMItem(indexItem);
			SelectableItem selectableItem = new SelectableItem();
			selectableItem.setTitle(indexItem.getVisibleName(app, app.getRegions(), false));
			String size = indexItem.getSizeDescription(app);
			size += " (" + getSRTMAbbrev(app, baseItem) + ")";
			String date = indexItem.getDate(dateFormat, showRemoteDate);
			String description = app.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, date);
			selectableItem.setDescription(description);
			selectableItem.setIconId(indexItem.getType().getIconResource());
			selectableItem.setObject(indexItem);

			if (baseItem) {
				leftItems.add(selectableItem);
			} else {
				rightItems.add(selectableItem);
			}

			if (indexesToDownload.contains(indexItem)
					&& (baseSRTM && baseItem || !baseSRTM && !baseItem)) {
				selectedItems.add(selectableItem);
			}
		}

		String addDescription = app.getString(isListDialog(app, leftItems, rightItems)
				? R.string.srtm_download_list_help_message : R.string.srtm_download_single_help_message);
		final SelectMultipleItemsBottomSheet dialog = SelectMultipleItemsBottomSheet.showInstance(
				activity, baseSRTM ? leftItems : rightItems, selectedItems, true,
				addDescription, true, baseSRTM,
				Algorithms.capitalizeFirstLetter(app.getString(R.string.shared_string_meters)),
				Algorithms.capitalizeFirstLetter(app.getString(R.string.shared_string_feets)));

		dialog.setSelectionUpdateListener(new SelectionUpdateListener() {
			@Override
			public void onSelectionUpdate() {
				updateSize(app, dialog);
			}
		});
		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
		dialog.setOnRadioButtonSelectListener(new OnRadioButtonSelectListener() {
			@Override
			public void onSelect(boolean leftButton) {
				dialog.recreateList(leftButton ? leftItems : rightItems);
				updateSize(app, dialog);
			}
		});
		dialog.setSelectedItemsListener(new SelectedItemsListener() {
			@Override
			public List<SelectableItem> createSelectedItems(List<SelectableItem> currentAllItems, boolean baseSRTM) {
				List<IndexItem> indexesToDownload = getIndexesToDownload(downloadItem);
				List<SelectableItem> selectedItems = new ArrayList<>();

				for (SelectableItem currentItem : currentAllItems) {
					IndexItem indexItem = (IndexItem) currentItem.getObject();
					boolean baseItem = isBaseSRTMItem(indexItem);
					if (indexesToDownload.contains(indexItem)
							&& (baseSRTM && baseItem || !baseSRTM && !baseItem)) {
						selectedItems.add(currentItem);
					}
				}
				return selectedItems;
			}
		});
	}

	private static OnApplySelectionListener getOnApplySelectionListener(final SelectItemsToDownloadListener listener) {
		return new OnApplySelectionListener() {
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
		};
	}

	private static void updateSize(OsmandApplication app, SelectMultipleItemsBottomSheet dialog) {
		boolean isListDialog = dialog.isMultipleItem();
		dialog.setTitle(app.getString(isListDialog ? R.string.welmode_download_maps : R.string.srtm_unit_format));
		double sizeToDownload = getDownloadSizeInMb(dialog.getSelectedItems());
		String size = DownloadItem.getFormattedMb(app, sizeToDownload);
		if (isListDialog) {
			String total = app.getString(R.string.shared_string_total);
			String description = app.getString(R.string.ltr_or_rtl_combine_via_colon, total, size);
			dialog.setDescription(description);
		}
		String btnTitle = app.getString(R.string.shared_string_download);
		if (sizeToDownload > 0) {
			btnTitle = app.getString(R.string.ltr_or_rtl_combine_via_dash, btnTitle, size);
		}
		dialog.setConfirmButtonTitle(btnTitle);
	}

	private static boolean isListDialog(OsmandApplication app,
	                                    List<SelectableItem> leftItems, List<SelectableItem> rightItems) {
		return (isBaseSRTMMetricSystem(app) ? leftItems : rightItems).size() > 1;
	}

	public static String getSRTMAbbrev(Context context, boolean base) {
		return context.getString(base ? R.string.m : R.string.foot);
	}

	public static String getSRTMExt(IndexItem indexItem) {
		return isBaseSRTMItem(indexItem)
				? IndexConstants.BINARY_SRTM_MAP_INDEX_EXT : IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT;
	}

	public static boolean isBaseSRTMItem(Object item) {
		if (item instanceof IndexItem) {
			return ((IndexItem) item).getFileName().endsWith(BINARY_SRTM_MAP_INDEX_EXT_ZIP);
		} else if (item instanceof LocalIndexInfo) {
			return ((LocalIndexInfo) item).getFileName().endsWith(BINARY_SRTM_MAP_INDEX_EXT);
		}
		return false;
	}

	public static boolean isBaseSRTMMetricSystem(OsmandApplication app) {
		return app.getSettings().METRIC_SYSTEM.get() != MetricsConstants.MILES_AND_FEET;
	}

	private static List<IndexItem> getIndexesToDownload(DownloadItem downloadItem) {
		if (downloadItem instanceof MultipleIndexItem) {
			if (downloadItem.hasActualDataToDownload()) {
				// download left regions
				return ((MultipleIndexItem) downloadItem).getIndexesToDownload();
			} else {
				// download all regions again
				return ((MultipleIndexItem) downloadItem).getAllIndexes();
			}
		} else {
			List<IndexItem> indexesToDownload = new ArrayList<>();
			for (IndexItem indexItem : downloadItem.getRelatedGroup().getIndividualResources()) {
				if (indexItem.getType() == SRTM_COUNTRY_FILE && indexItem.hasActualDataToDownload()) {
					indexesToDownload.add(indexItem);
				}
			}
			return indexesToDownload;
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

	public interface SelectedItemsListener {
		List<SelectableItem> createSelectedItems(List<SelectableItem> currentAllItems, boolean base);
	}
}
