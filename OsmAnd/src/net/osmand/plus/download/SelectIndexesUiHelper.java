package net.osmand.plus.download;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MultipleSelectionBottomSheet;
import net.osmand.plus.base.MultipleSelectionBottomSheet.SelectionUpdateListener;
import net.osmand.plus.base.ModeSelectionBottomSheet;
import net.osmand.plus.base.MultipleSelectionWithModeBottomSheet;
import net.osmand.plus.base.SelectionBottomSheet;
import net.osmand.plus.base.SelectionBottomSheet.OnApplySelectionListener;
import net.osmand.plus.base.SelectionBottomSheet.OnUiInitializedAdapter;
import net.osmand.plus.base.SelectionBottomSheet.SelectableItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.download.MultipleDownloadItem.getIndexItem;

public class SelectIndexesUiHelper {

	private final OsmandApplication app;
	private final AppCompatActivity activity;

	private final ItemsToDownloadSelectedListener listener;
	private final DateFormat dateFormat;
	private final boolean showRemoteDate;
	private final DownloadItem downloadItem;

	private SelectionBottomSheet dialog;

	private SelectIndexesUiHelper(@NonNull DownloadItem downloadItem,
	                              @NonNull AppCompatActivity activity,
	                              @NonNull DateFormat dateFormat,
	                              boolean showRemoteDate,
	                              @NonNull ItemsToDownloadSelectedListener listener) {
		this.app = (OsmandApplication) activity.getApplicationContext();
		this.activity = activity;
		this.downloadItem = downloadItem;
		this.dateFormat = dateFormat;
		this.showRemoteDate = showRemoteDate;
		this.listener = listener;
	}

	public static void showDialog(@NonNull DownloadItem i,
	                              @NonNull AppCompatActivity a,
	                              @NonNull DateFormat df,
	                              boolean showRemoteDate,
	                              @NonNull ItemsToDownloadSelectedListener l) {

		new SelectIndexesUiHelper(i, a, df, showRemoteDate, l).showDialogInternal();
	}

	private void showDialogInternal() {
		if (downloadItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
			if (downloadItem instanceof MultipleDownloadItem) {
				showSrtmMultipleSelectionDialog();
			} else {
				showSrtmModeSelectionDialog();
			}
		} else if (downloadItem instanceof MultipleDownloadItem) {
			showMultipleSelectionDialog();
		}
	}

	private void showMultipleSelectionDialog() {
		List<SelectableItem> allItems = new ArrayList<>();
		List<SelectableItem> selectedItems = new ArrayList<>();
		prepareItems(allItems, selectedItems);

		MultipleSelectionBottomSheet msDialog = MultipleSelectionBottomSheet.showInstance(
				activity, allItems, selectedItems, true);
		this.dialog = msDialog;

		msDialog.setOnUiInitializedAdapter(new OnUiInitializedAdapter() {
			@Override
			public void onUiInitialized() {
				dialog.setTitle(app.getString(R.string.welmode_download_maps));
			}
		});

		msDialog.setSelectionUpdateListener(new SelectionUpdateListener() {
			@Override
			public void onSelectionUpdate() {
				updateSize();
			}
		});

		msDialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	private void showSrtmMultipleSelectionDialog() {
		List<SelectableItem> allItems = new ArrayList<>();
		List<SelectableItem> selectedItems = new ArrayList<>();
		prepareItems(allItems, selectedItems);

		SrtmDownloadItem srtmItem = (SrtmDownloadItem) ((MultipleDownloadItem)downloadItem).getAllItems().get(0);
		final int selectedModeOrder = srtmItem.isUseMetric() ? 0 : 1;
		final List<RadioItem> radioItems = createSrtmRadioItems();

		MultipleSelectionBottomSheet msDialog = MultipleSelectionWithModeBottomSheet.showInstance(
				activity, allItems, selectedItems, radioItems, true);
		this.dialog = msDialog;

		msDialog.setOnUiInitializedAdapter(new OnUiInitializedAdapter() {
			@Override
			public void onUiInitialized() {
				dialog.setTitle(app.getString(R.string.welmode_download_maps));
				dialog.setSelectedMode(radioItems.get(selectedModeOrder));
				dialog.setSecondaryDescription(app.getString(R.string.srtm_download_list_help_message));
			}
		});

		msDialog.setSelectionUpdateListener(new SelectionUpdateListener() {
			@Override
			public void onSelectionUpdate() {
				updateSize();
			}
		});

		msDialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	private void showSrtmModeSelectionDialog() {
		SrtmDownloadItem srtmItem = (SrtmDownloadItem) downloadItem;
		final int selectedModeOrder = srtmItem.isUseMetric() ? 0 : 1;

		final List<RadioItem> radioItems = createSrtmRadioItems();
		SelectableItem preview = createSelectableItem(srtmItem);

		dialog = ModeSelectionBottomSheet.showInstance(activity, preview, radioItems, true);

		dialog.setOnUiInitializedAdapter(new OnUiInitializedAdapter() {
			@Override
			public void onUiInitialized() {
				ModeSelectionBottomSheet dialog = (ModeSelectionBottomSheet) SelectIndexesUiHelper.this.dialog;
				dialog.setTitle(app.getString(R.string.srtm_unit_format));
				dialog.setPrimaryDescription(app.getString(R.string.srtm_download_single_help_message));
				updateSize();
				dialog.setSelectedMode(radioItems.get(selectedModeOrder));
			}
		});

		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	private void prepareItems(List<SelectableItem> allItems,
	                          List<SelectableItem> selectedItems) {
		final MultipleDownloadItem multipleDownloadItem = (MultipleDownloadItem) downloadItem;
		final List<DownloadItem> itemsToDownload = getItemsToDownload(multipleDownloadItem);
		for (DownloadItem downloadItem : multipleDownloadItem.getAllItems()) {
			SelectableItem selectableItem = createSelectableItem(downloadItem);
			allItems.add(selectableItem);

			if (itemsToDownload.contains(downloadItem)) {
				selectedItems.add(selectableItem);
			}
		}
	}

	private List<RadioItem> createSrtmRadioItems() {
		List<RadioItem> radioItems = new ArrayList<>();
		radioItems.add(createSrtmRadioBtn(R.string.shared_string_meters, true));
		radioItems.add(createSrtmRadioBtn(R.string.shared_string_feet, false));
		return radioItems;
	}

	private RadioItem createSrtmRadioBtn(int titleId,
	                                     final boolean useMeters) {
		String title = Algorithms.capitalizeFirstLetter(app.getString(titleId));
		RadioItem radioItem = new TextRadioItem(title);
		radioItem.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				updateDialogListItems(useMeters);
				updateSize();
				return true;
			}
		});
		return radioItem;
	}

	private void updateDialogListItems(boolean useMeters) {
		List<SelectableItem> items = new ArrayList<>(dialog.getAllItems());
		for (SelectableItem item : items) {
			DownloadItem downloadItem = (DownloadItem) item.getObject();
			if (downloadItem instanceof SrtmDownloadItem) {
				((SrtmDownloadItem) downloadItem).setUseMetric(useMeters);
				updateSelectableItem(item, downloadItem);
			}
		}
		dialog.setItems(items);
	}

	private SelectableItem createSelectableItem(DownloadItem item) {
		SelectableItem selectableItem = new SelectableItem();
		updateSelectableItem(selectableItem, item);
		return selectableItem;
	}

	private void updateSelectableItem(SelectableItem selectableItem,
	                                  DownloadItem downloadItem) {
		selectableItem.setTitle(downloadItem.getVisibleName(app, app.getRegions(), false));

		String size = downloadItem.getSizeDescription(app);
		String addDescr = downloadItem.getAdditionalDescription(app);
		if (addDescr != null) {
			size += " " + addDescr;
		}
		String date = downloadItem.getDate(dateFormat, showRemoteDate);
		String description = app.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, date);
		selectableItem.setDescription(description);

		selectableItem.setIconId(downloadItem.getType().getIconResource());
		selectableItem.setObject(downloadItem);
	}

	private OnApplySelectionListener getOnApplySelectionListener(final ItemsToDownloadSelectedListener listener) {
		return new OnApplySelectionListener() {
			@Override
			public void onSelectionApplied(List<SelectableItem> selectedItems) {
				List<IndexItem> indexes = new ArrayList<>();
				for (SelectableItem item : selectedItems) {
					IndexItem index = getIndexItem((DownloadItem) item.getObject());
					if (index != null) {
						indexes.add(index);
					}
				}
				listener.onItemsToDownloadSelected(indexes);
			}
		};
	}

	private void updateSize() {
		double sizeToDownload = getDownloadSizeInMb(dialog.getSelectedItems());
		String size = DownloadItem.getFormattedMb(app, sizeToDownload);
		String total = app.getString(R.string.shared_string_total);
		String description = app.getString(R.string.ltr_or_rtl_combine_via_colon, total, size);
		dialog.setTitleDescription(description);
		String btnTitle = app.getString(R.string.shared_string_download);
		if (sizeToDownload > 0) {
			btnTitle = app.getString(R.string.ltr_or_rtl_combine_via_dash, btnTitle, size);
		}
		dialog.setApplyButtonTitle(btnTitle);
	}

	private double getDownloadSizeInMb(@NonNull List<SelectableItem> selectableItems) {
		double totalSizeMb = 0.0d;
		for (SelectableItem i : selectableItems) {
			Object obj = i.getObject();
			if (obj instanceof DownloadItem) {
				totalSizeMb += ((DownloadItem) obj).getSizeToDownloadInMb();
			}
		}
		return totalSizeMb;
	}

	private static List<DownloadItem> getItemsToDownload(MultipleDownloadItem md) {
		if (md.hasActualDataToDownload()) {
			// download left regions
			return md.getItemsToDownload();
		} else {
			// download all regions again
			return md.getAllItems();
		}
	}

	public interface ItemsToDownloadSelectedListener {
		void onItemsToDownloadSelected(List<IndexItem> items);
	}

}
