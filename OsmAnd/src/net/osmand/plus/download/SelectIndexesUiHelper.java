package net.osmand.plus.download;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet;
import net.osmand.plus.base.SelectMultipleItemsBottomSheet.SelectionUpdateListener;
import net.osmand.plus.base.SelectModeBottomSheet;
import net.osmand.plus.base.SelectMultipleWithModeBottomSheet;
import net.osmand.plus.base.SelectionBottomSheet;
import net.osmand.plus.base.SelectionBottomSheet.OnApplySelectionListener;
import net.osmand.plus.base.SelectionBottomSheet.OnUiInitializedListener;
import net.osmand.plus.base.SelectionBottomSheet.SelectableItem;
import net.osmand.plus.widgets.MultiStateToggleButton.OnRadioItemClickListener;
import net.osmand.plus.widgets.MultiStateToggleButton.RadioItem;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.download.DownloadActivityType.SRTM_COUNTRY_FILE;

public class SelectIndexesUiHelper {

	private OsmandApplication app;
	private AppCompatActivity activity;

	private DateFormat dateFormat;
	private boolean showRemoteDate;
	private DownloadItem downloadItem;
	private ItemsToDownloadSelectedListener listener;
	private SelectionBottomSheet dialog;

	private SelectIndexesUiHelper(@NonNull DownloadItem item,
	                              @NonNull AppCompatActivity activity,
	                              @NonNull DateFormat dateFormat,
	                              boolean showRemoteDate,
	                              @NonNull ItemsToDownloadSelectedListener listener) {
		this.activity = activity;
		this.app = (OsmandApplication) activity.getApplicationContext();
		this.downloadItem = item;
		this.dateFormat = dateFormat;
		this.showRemoteDate = showRemoteDate;
		this.listener = listener;
	}

	public static void showDialog(@NonNull DownloadItem item,
	                              @NonNull AppCompatActivity activity,
	                              @NonNull DateFormat dateFormat,
	                              boolean showRemoteDate,
	                              @NonNull ItemsToDownloadSelectedListener listener) {
		SelectIndexesUiHelper helper =
				new SelectIndexesUiHelper(item, activity, dateFormat, showRemoteDate, listener);
		helper.showDialogInternal();
	}

	private void showDialogInternal() {
		if (downloadItem.getType() == SRTM_COUNTRY_FILE) {
			if (downloadItem instanceof MultipleIndexItem) {
				showMultipleSrtmDialog();
			} else {
				showSingleSrtmDialog();
			}
		} else if (downloadItem instanceof MultipleIndexItem) {
			showBaseDialog();
		}
	}

	private void showBaseDialog() {
		MultipleIndexItem multipleIndexItem = (MultipleIndexItem) downloadItem;
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
				updateSize(dialog, true);
			}
		});
		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	private void showSingleSrtmDialog() {
		boolean baseSRTM = SrtmDownloadItem.shouldUseMetersByDefault(app);
		SrtmDownloadItem srtmItem = (SrtmDownloadItem) downloadItem;

		SelectableItem meterItem = createSrtmSelectableItem(srtmItem.getMeterItem());
		SelectableItem feetItem = createSrtmSelectableItem(srtmItem.getFeetItem());

		List<RadioItem> radioItems = new ArrayList<>();
		RadioItem meters = createRadioItem(meterItem, R.string.shared_string_meters);
		RadioItem feet = createRadioItem(feetItem, R.string.shared_string_feets);
		radioItems.add(meters);
		radioItems.add(feet);

		dialog = SelectModeBottomSheet.showInstance(activity,
				baseSRTM ? meterItem : feetItem, radioItems, true);

		final RadioItem initRadio = baseSRTM ? meters : feet;
		final SelectableItem initItem = baseSRTM ? meterItem : feetItem;
		dialog.setUiInitializedListener(new OnUiInitializedListener() {
			@Override
			public void onUiInitialized() {
				SelectModeBottomSheet dialog = (SelectModeBottomSheet) SelectIndexesUiHelper.this.dialog;
				dialog.setTitle(app.getString(R.string.srtm_unit_format));
				dialog.setPrimaryDescription(app.getString(R.string.srtm_download_single_help_message));
				updateSize(dialog, false);
				dialog.setSelectedMode(initRadio);
			}
		});

		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	private SelectableItem createSrtmSelectableItem(IndexItem indexItem) {
		boolean baseItem = SrtmDownloadItem.isMetersItem(indexItem);
		SelectableItem selectableItem = new SelectableItem();
		selectableItem.setTitle(indexItem.getVisibleName(app, app.getRegions(), false));
		String size = indexItem.getSizeDescription(app);
		size += " " + SrtmDownloadItem.getAbbreviationInScopes(app, baseItem);
		String date = indexItem.getDate(dateFormat, showRemoteDate);
		String description = app.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, date);
		selectableItem.setDescription(description);
		selectableItem.setIconId(indexItem.getType().getIconResource());
		selectableItem.setObject(indexItem);
		return selectableItem;
	}

	private RadioItem createRadioItem(final SelectableItem selectableItem, int titleId) {
		String title = Algorithms.capitalizeFirstLetter(app.getString(titleId));
		RadioItem radioItem = new RadioItem(title);
		radioItem.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				((SelectModeBottomSheet)dialog).setPreviewItem(selectableItem);
				updateSize(dialog, false);
				return true;
			}
		});
		return radioItem;
	}

	private void showMultipleSrtmDialog() {
		List<SelectableItem> selectedItems = new ArrayList<>();
		final List<SelectableItem> meterItems = new ArrayList<>();
		final List<SelectableItem> feetItems = new ArrayList<>();
		List<IndexItem> indexesToDownload = getIndexesToDownload(downloadItem);
		boolean baseSRTM = SrtmDownloadItem.shouldUseMetersByDefault(app);

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
			boolean baseItem = SrtmDownloadItem.isMetersItem(indexItem);
			SelectableItem selectableItem = new SelectableItem();
			selectableItem.setTitle(indexItem.getVisibleName(app, app.getRegions(), false));
			String size = indexItem.getSizeDescription(app);
			size += " " + SrtmDownloadItem.getAbbreviationInScopes(app, baseItem);
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
		final RadioItem selectedMode = SrtmDownloadItem.isMetersItem(downloadItem) ? meters : feet;
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
				updateSize(dialog, true);
			}
		});
		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	private OnApplySelectionListener getOnApplySelectionListener(final ItemsToDownloadSelectedListener listener) {
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

	private void updateSize(SelectionBottomSheet dialog,
	                        boolean updateDescription) {
		double sizeToDownload = getDownloadSizeInMb(dialog.getSelection());
		String size = DownloadItem.getFormattedMb(app, sizeToDownload);
		if (updateDescription) {
			String total = app.getString(R.string.shared_string_total);
			String description = app.getString(R.string.ltr_or_rtl_combine_via_colon, total, size);
			dialog.setTitleDescription(description);
		}
		String btnTitle = app.getString(R.string.shared_string_download);
		if (sizeToDownload > 0) {
			btnTitle = app.getString(R.string.ltr_or_rtl_combine_via_dash, btnTitle, size);
		}
		dialog.setApplyButtonTitle(btnTitle);
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

	public interface ItemsToDownloadSelectedListener {
		void onItemsToDownloadSelected(List<IndexItem> items);
	}

}
