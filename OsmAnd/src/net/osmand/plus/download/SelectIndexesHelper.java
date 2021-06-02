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
import net.osmand.plus.base.SelectionBottomSheet.DialogStateListener;
import net.osmand.plus.base.SelectionBottomSheet.SelectableItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.osmand.plus.download.MultipleDownloadItem.getIndexItem;

public class SelectIndexesHelper {

	private final OsmandApplication app;
	private final AppCompatActivity activity;

	private final ItemsToDownloadSelectedListener listener;
	private final DateFormat dateFormat;
	private final boolean showRemoteDate;
	private final List<DownloadItem> itemsToDownload;
	private final DownloadItem downloadItem;
	private final boolean useMetricByDefault;

	private SelectionBottomSheet dialog;

	private SelectIndexesHelper(@NonNull DownloadItem downloadItem,
	                            @NonNull AppCompatActivity activity,
	                            @NonNull DateFormat dateFormat,
	                            boolean showRemoteDate,
	                            @NonNull ItemsToDownloadSelectedListener listener) {
		this.app = (OsmandApplication) activity.getApplicationContext();
		this.activity = activity;
		this.dateFormat = dateFormat;
		this.showRemoteDate = showRemoteDate;
		this.listener = listener;
		this.downloadItem = downloadItem;
		this.itemsToDownload = getItemsToDownload(downloadItem);
		this.useMetricByDefault = SrtmDownloadItem.isUseMetricByDefault(app);
	}

	public static void showDialog(@NonNull DownloadItem di,
	                              @NonNull AppCompatActivity a,
	                              @NonNull DateFormat df,
	                              boolean showRemoteDate,
	                              @NonNull ItemsToDownloadSelectedListener l) {

		SelectIndexesHelper h = new SelectIndexesHelper(di, a, df, showRemoteDate, l);
		if (di.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
			if (di instanceof MultipleDownloadItem) {
				h.showSrtmMultipleSelectionDialog();
			} else {
				h.showSrtmTypeSelectionDialog();
			}
		} else if (di instanceof MultipleDownloadItem) {
			h.showMultipleSelectionDialog();
		}
	}

	private void showMultipleSelectionDialog() {
		MultipleDownloadItem mdi = (MultipleDownloadItem) downloadItem;
		List<SelectableItem> allItems = new ArrayList<>();
		List<SelectableItem> selectedItems = new ArrayList<>();

		for (DownloadItem di : mdi.getAllItems()) {
			SelectableItem si = createSelectableItem(di);
			allItems.add(si);
			if (itemsToDownload.contains(di)) {
				selectedItems.add(si);
			}
		}

		MultipleSelectionBottomSheet msDialog =
				MultipleSelectionBottomSheet.showInstance(activity, allItems, selectedItems, true);
		this.dialog = msDialog;

		msDialog.setDialogStateListener(new DialogStateListener() {
			@Override
			public void onDialogCreated() {
				dialog.setTitle(app.getString(R.string.welmode_download_maps));
			}

			@Override
			public void onCloseDialog() { }
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
		MultipleDownloadItem mdi = (MultipleDownloadItem) downloadItem;
		List<SelectableItem> allItems = new ArrayList<>();
		List<SelectableItem> selectedItems = new ArrayList<>();

		for (DownloadItem di : mdi.getAllItems()) {
			SelectableItem si = createSrtmSelectableItem((SrtmDownloadItem) di);
			allItems.add(si);
			if (itemsToDownload.contains(di)) {
				selectedItems.add(si);
			}
		}

		final RadioItem meterBtn = createSrtmRadioBtn(true);
		final RadioItem feetBtn = createSrtmRadioBtn(false);
		List<RadioItem> radioItems = new ArrayList<>();
		radioItems.add(meterBtn);
		radioItems.add(feetBtn);

		MultipleSelectionBottomSheet msDialog = MultipleSelectionWithModeBottomSheet.showInstance(
				activity, allItems, selectedItems, radioItems, true);
		this.dialog = msDialog;

		msDialog.setDialogStateListener(new DialogStateListener() {
			@Override
			public void onDialogCreated() {
				dialog.setTitle(app.getString(R.string.welmode_download_maps));
				dialog.setSelectedMode(useMetricByDefault ? meterBtn : feetBtn);
				dialog.setSecondaryDescription(app.getString(R.string.srtm_download_list_help_message));
			}

			@Override
			public void onCloseDialog() {
				resetUseMeters();
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

	private void showSrtmTypeSelectionDialog() {
		SrtmDownloadItem srtmItem = (SrtmDownloadItem) downloadItem;

		final RadioItem meterBtn = createSrtmRadioBtn(true);
		final RadioItem feetBtn = createSrtmRadioBtn(false);
		List<RadioItem> radioItems = new ArrayList<>();
		radioItems.add(meterBtn);
		radioItems.add(feetBtn);

		SelectableItem preview = createSrtmSelectableItem(srtmItem);

		dialog = ModeSelectionBottomSheet.showInstance(activity, preview, radioItems, true);

		dialog.setDialogStateListener(new DialogStateListener() {
			@Override
			public void onDialogCreated() {
				ModeSelectionBottomSheet dialog = (ModeSelectionBottomSheet) SelectIndexesHelper.this.dialog;
				dialog.setTitle(app.getString(R.string.srtm_unit_format));
				dialog.setPrimaryDescription(app.getString(R.string.srtm_download_single_help_message));
				updateSize();
				dialog.setSelectedMode(useMetricByDefault ? meterBtn : feetBtn);
			}

			@Override
			public void onCloseDialog() {
				resetUseMeters();
			}
		});

		dialog.setOnApplySelectionListener(getOnApplySelectionListener(listener));
	}

	private RadioItem createSrtmRadioBtn(final boolean useMeters) {
		int titleId = useMeters ? R.string.shared_string_meters : R.string.shared_string_feet;
		String title = Algorithms.capitalizeFirstLetter(app.getString(titleId));
		RadioItem radioItem = new TextRadioItem(title);
		radioItem.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				setUseMetersForAllItems(useMeters);
				updateListItems();
				updateSize();
				return true;
			}
		});
		return radioItem;
	}

	private SelectableItem createSelectableItem(DownloadItem item) {
		SelectableItem selectableItem = new SelectableItem();
		updateSelectableItem(selectableItem, item);
		selectableItem.setObject(item);
		return selectableItem;
	}

	private SelectableItem createSrtmSelectableItem(SrtmDownloadItem item) {
		SelectableItem selectableItem = new SelectableItem();
		updateSelectableItem(selectableItem, item.getDefaultIndexItem());
		selectableItem.setObject(item);
		return selectableItem;
	}

	private void updateListItems() {
		List<SelectableItem> items = new ArrayList<>(dialog.getAllItems());
		for (SelectableItem selectableItem : items) {
			DownloadItem di = (DownloadItem) selectableItem.getObject();
			if (di instanceof SrtmDownloadItem) {
				di = ((SrtmDownloadItem) di).getDefaultIndexItem();
			}
			updateSelectableItem(selectableItem, di);
		}
		dialog.setItems(items);
	}

	private void resetUseMeters() {
		boolean useMeters = SrtmDownloadItem.isUseMetricByDefault(app);
		setUseMetersForAllItems(useMeters);
	}

	private void setUseMetersForAllItems(boolean useMeters) {
		for (SelectableItem item : dialog.getAllItems()) {
			DownloadItem downloadItem = (DownloadItem) item.getObject();
			if (downloadItem instanceof SrtmDownloadItem) {
				SrtmDownloadItem srtmItem = (SrtmDownloadItem) downloadItem;
				srtmItem.setUseMetric(useMeters);
			}
		}
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
			if (obj instanceof SrtmDownloadItem) {
				SrtmDownloadItem srtm = (SrtmDownloadItem) obj;
				totalSizeMb += srtm.getDefaultIndexItem().getSizeToDownloadInMb();
			} else if (obj instanceof DownloadItem) {
				totalSizeMb += ((DownloadItem) obj).getSizeToDownloadInMb();
			}
		}
		return totalSizeMb;
	}

	private static List<DownloadItem> getItemsToDownload(DownloadItem di) {
		if (di instanceof MultipleDownloadItem) {
			return getItemsToDownload((MultipleDownloadItem) di);
		}
		return Collections.emptyList();
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
