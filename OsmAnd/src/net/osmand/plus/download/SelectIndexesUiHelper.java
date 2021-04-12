package net.osmand.plus.download;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.IndexConstants;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet.SelectionUpdateListener;
import net.osmand.plus.base.SelectModeBottomSheet;
import net.osmand.plus.base.SelectMultipleWithModeBottomSheet;
import net.osmand.plus.base.SelectionBottomSheet;
import net.osmand.plus.base.SelectionBottomSheet.OnApplySelectionListener;
import net.osmand.plus.base.SelectionBottomSheet.OnUiInitializedListener;
import net.osmand.plus.base.SelectionBottomSheet.SelectableItem;
import net.osmand.plus.helpers.enums.MetricsConstants;
import net.osmand.plus.widgets.MultiStateToggleButton.OnRadioItemClickListener;
import net.osmand.plus.widgets.MultiStateToggleButton.RadioItem;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT_ZIP;
import static net.osmand.plus.download.DownloadActivityType.SRTM_COUNTRY_FILE;

public class SelectIndexesUiHelper {

	public static void showDialog(@NonNull DownloadItem item,
	                              @NonNull AppCompatActivity activity,
	                              @NonNull final OsmandApplication app,
	                              @NonNull DateFormat dateFormat,
	                              boolean showRemoteDate,
	                              @NonNull final SelectItemsToDownloadListener listener) {
		if (item.getType() == SRTM_COUNTRY_FILE) {
			if (item instanceof MultipleIndexItem) {
				showMultipleSrtmDialog(item, activity, app, dateFormat, showRemoteDate, listener);
			} else {
				showSingleSrtmDialog(item, activity, app, dateFormat, showRemoteDate, listener);
			}
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

		dialog.setUiInitializedListener(new OnUiInitializedListener() {
			@Override
			public void onUiInitialized() {
				dialog.setTitle(app.getString(R.string.welmode_download_maps));
			}
		});

		dialog.setSelectionUpdateListener(new SelectionUpdateListener() {
			@Override
			public void onSelectionUpdate() {
				updateSize(app, dialog, true);
			}
		});
		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	public static void showSingleSrtmDialog(@NonNull final DownloadItem downloadItem,
	                                        @NonNull AppCompatActivity activity,
	                                        @NonNull final OsmandApplication app,
	                                        @NonNull final DateFormat dateFormat,
	                                        final boolean showRemoteDate,
	                                        @NonNull final SelectItemsToDownloadListener listener) {
		List<IndexItem> allIndexes = getIndexesToDownload(downloadItem);
		boolean baseSRTM = isBaseSRTMMetricSystem(app);

		List<RadioItem> radioItems = new ArrayList<>();
		final RadioItem meters = new RadioItem(Algorithms.capitalizeFirstLetter(app.getString(R.string.shared_string_meters)));
		RadioItem feet = new RadioItem(Algorithms.capitalizeFirstLetter(app.getString(R.string.shared_string_feets)));
		radioItems.add(meters);
		radioItems.add(feet);
		SelectableItem meterItem = null;
		SelectableItem feetItem = null;
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
				meterItem = selectableItem;
			} else {
				feetItem = selectableItem;
			}
		}

		final SelectableItem initMeter = meterItem;
		final SelectableItem initFeet = feetItem;

		final SelectModeBottomSheet dialog = SelectModeBottomSheet.showInstance(activity,
				baseSRTM ? meterItem : feetItem, radioItems, true);

		meters.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				dialog.setPreviewItem(initMeter);
				updateSize(app, dialog, false);
				return true;
			}
		});

		feet.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				dialog.setPreviewItem(initFeet);

				double sizeToDownload = getDownloadSizeInMb(Collections.singletonList(initFeet));
				String size = DownloadItem.getFormattedMb(app, sizeToDownload);
				String btnTitle = app.getString(R.string.shared_string_download);
				if (sizeToDownload > 0) {
					btnTitle = app.getString(R.string.ltr_or_rtl_combine_via_dash, btnTitle, size);
				}
				dialog.setApplyButtonTitle(btnTitle);
				return true;
			}
		});

		final RadioItem initRadio = baseSRTM ? meters : feet;
		dialog.setUiInitializedListener(new OnUiInitializedListener() {
			@Override
			public void onUiInitialized() {
				dialog.setTitle(app.getString(R.string.srtm_unit_format));
				dialog.setDescription(app.getString(R.string.srtm_download_single_help_message));
				double sizeToDownload = getDownloadSizeInMb(Collections.singletonList(initFeet));
				String size = DownloadItem.getFormattedMb(app, sizeToDownload);
				String btnTitle = app.getString(R.string.shared_string_download);
				if (sizeToDownload > 0) {
					btnTitle = app.getString(R.string.ltr_or_rtl_combine_via_dash, btnTitle, size);
				}
				dialog.setApplyButtonTitle(btnTitle);
				dialog.setSelectedMode(initRadio);
			}
		});

		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	public static void showMultipleSrtmDialog(@NonNull final DownloadItem downloadItem,
	                                          @NonNull AppCompatActivity activity,
	                                          @NonNull final OsmandApplication app,
	                                          @NonNull final DateFormat dateFormat,
	                                          final boolean showRemoteDate,
	                                          @NonNull final SelectItemsToDownloadListener listener) {
		List<SelectableItem> selectedItems = new ArrayList<>();
		final List<SelectableItem> meterItems = new ArrayList<>();
		final List<SelectableItem> feetItems = new ArrayList<>();
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
				meterItems.add(selectableItem);
			} else {
				feetItems.add(selectableItem);
			}

			if (indexesToDownload.contains(indexItem)
					&& (baseSRTM && baseItem || !baseSRTM && !baseItem)) {
				selectedItems.add(selectableItem);
			}
		}

		List<RadioItem> radioItems = new ArrayList<>();
		RadioItem meters = new RadioItem(Algorithms.capitalizeFirstLetter(app.getString(R.string.shared_string_meters)));
		RadioItem feet = new RadioItem(Algorithms.capitalizeFirstLetter(app.getString(R.string.shared_string_feets)));
		final RadioItem selectedMode = isBaseSRTMItem(downloadItem) ? meters : feet;
		radioItems.add(meters);
		radioItems.add(feet);

		final SelectMultipleWithModeBottomSheet dialog = SelectMultipleWithModeBottomSheet.showInstance(
				activity, baseSRTM ? meterItems : feetItems, selectedItems, radioItems, true);

		meters.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
//				dialog.recreateList(meterItems);
				return true;
			}
		});

		feet.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
//				dialog.recreateList(feetItems);
				return true;
			}
		});

		dialog.setUiInitializedListener(new OnUiInitializedListener() {
			@Override
			public void onUiInitialized() {
				dialog.setTitle(app.getString(R.string.welmode_download_maps));
				dialog.setSelectedMode(selectedMode);
				dialog.setSecondaryDescription(app.getString(R.string.srtm_download_list_help_message));
			}
		});

		dialog.setSelectionUpdateListener(new SelectionUpdateListener() {
			@Override
			public void onSelectionUpdate() {
				updateSize(app, dialog, true);
			}
		});
		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
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

	private static void updateSize(OsmandApplication app,
	                               SelectionBottomSheet dialog,
	                               boolean updateDescription) {
		double sizeToDownload = getDownloadSizeInMb(dialog.getSelection());
		String size = DownloadItem.getFormattedMb(app, sizeToDownload);
		if (updateDescription) {
			String total = app.getString(R.string.shared_string_total);
			String description = app.getString(R.string.ltr_or_rtl_combine_via_colon, total, size);
			dialog.setDescription(description);
		}
		String btnTitle = app.getString(R.string.shared_string_download);
		if (sizeToDownload > 0) {
			btnTitle = app.getString(R.string.ltr_or_rtl_combine_via_dash, btnTitle, size);
		}
		dialog.setApplyButtonTitle(btnTitle);
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

}
