package net.osmand.plus.routepreparationmenu.cards;

import androidx.annotation.NonNull;

import net.osmand.map.WorldRegion;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MultipleSelectionBottomSheet;
import net.osmand.plus.base.SelectionBottomSheet;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.download.MultipleDownloadItem.getIndexItem;

public class SuggestMapsDownloadWarningCards extends WarningCard {
	private SelectionBottomSheet dialog;

	public SuggestMapsDownloadWarningCards(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		imageId = R.drawable.ic_map;
		title = mapActivity.getString(R.string.offline_maps);
		linkText = mapActivity.getString(R.string.welmode_download_maps);
	}

	private void showMultipleSelectionDialog() {
		List<DownloadItem> downloadMapsList = getMapsList();
		List<SelectionBottomSheet.SelectableItem> allItems = new ArrayList<>();
		List<SelectionBottomSheet.SelectableItem> selectedItems = new ArrayList<>();

		for (DownloadItem di : downloadMapsList) {
			boolean isStandardMap = di.getType().getTag().equals("map");
			SelectionBottomSheet.SelectableItem si = createSelectableItem(di);
			if (isStandardMap) {
				allItems.add(si);
				selectedItems.add(si);
			}
		}

		MultipleSelectionBottomSheet msDialog =
				MultipleSelectionBottomSheet.showInstance(mapActivity, allItems, selectedItems, true);
		this.dialog = msDialog;

		msDialog.setDialogStateListener(new SelectionBottomSheet.DialogStateListener() {
			@Override
			public void onDialogCreated() {
				dialog.setTitle(app.getString(R.string.welmode_download_maps));
			}

			@Override
			public void onCloseDialog() {

			}
		});

		msDialog.setSelectionUpdateListener(new MultipleSelectionBottomSheet.SelectionUpdateListener() {
			@Override
			public void onSelectionUpdate() {
				updateSize();
			}
		});

		msDialog.setOnApplySelectionListener(new SelectionBottomSheet.OnApplySelectionListener() {
			@Override
			public void onSelectionApplied(List<SelectionBottomSheet.SelectableItem> selectedItems) {
				List<IndexItem> indexes = new ArrayList<>();
				for (SelectionBottomSheet.SelectableItem item : selectedItems) {
					IndexItem index = getIndexItem((DownloadItem) item.getObject());
					if (index != null) {
						indexes.add(index);
					}
				}
				IndexItem[] indexesArray = new IndexItem[indexes.size()];
				new DownloadValidationManager(app).startDownload(mapActivity, indexes.toArray(indexesArray));
			}
		});
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

	private double getDownloadSizeInMb(@NonNull List<SelectionBottomSheet.SelectableItem> selectableItems) {
		double totalSizeMb = 0.0d;
		for (SelectionBottomSheet.SelectableItem i : selectableItems) {
			Object obj = i.getObject();
			totalSizeMb += ((DownloadItem) obj).getSizeToDownloadInMb();
		}
		return totalSizeMb;
	}


	public List<DownloadItem> getMapsList() {
		if (!app.getDownloadThread().getIndexes().isDownloadedFromInternet && app.getSettings().isInternetConnectionAvailable()) {
			app.getDownloadThread().runReloadIndexFiles();
		}

		boolean downloadIndexes = app.getSettings().isInternetConnectionAvailable()
				&& !app.getDownloadThread().getIndexes().isDownloadedFromInternet
				&& !app.getDownloadThread().getIndexes().downloadFromInternetFailed;

		Set<WorldRegion> isStandardMap = mapActivity.getRoutingHelper().getSuggestedOfflineMaps();
		List<DownloadItem> suggestedMaps = new ArrayList<>();
		if (!downloadIndexes) {
			for (WorldRegion temp : isStandardMap) {
				suggestedMaps.addAll(0, app.getDownloadThread().getIndexes().getDownloadItems(temp));
			}
		}
		return suggestedMaps;
	}

	private SelectionBottomSheet.SelectableItem createSelectableItem(DownloadItem item) {
		SelectionBottomSheet.SelectableItem selectableItem = new SelectionBottomSheet.SelectableItem();
		updateSelectableItem(selectableItem, item);
		selectableItem.setObject(item);
		return selectableItem;
	}

	private void updateSelectableItem(SelectionBottomSheet.SelectableItem selectableItem,
									  DownloadItem downloadItem) {
		selectableItem.setTitle(downloadItem.getVisibleName(app, app.getRegions(), true, true));
		DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(app);
		String size = downloadItem.getSizeDescription(app);
		String addDescr = downloadItem.getAdditionalDescription(app);
		if (addDescr != null) {
			size += " " + addDescr;
		}
		String date = downloadItem.getDate(dateFormat, true);
		String description = app.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, date);
		selectableItem.setDescription(description);

		selectableItem.setIconId(downloadItem.getType().getIconResource());
	}

	@Override
	protected void onLinkClicked() {
		showMultipleSelectionDialog();
	}

}
