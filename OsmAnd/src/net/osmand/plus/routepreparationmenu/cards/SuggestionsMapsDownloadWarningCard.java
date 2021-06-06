package net.osmand.plus.routepreparationmenu.cards;

import androidx.annotation.NonNull;

import net.osmand.map.WorldRegion;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MultipleSelectionBottomSheet;
import net.osmand.plus.base.SelectionBottomSheet;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.download.MultipleDownloadItem.getIndexItem;

public class SuggestionsMapsDownloadWarningCard extends WarningCard {
	boolean isNavigationEnable;
	private SelectionBottomSheet dialog;
	private List<WorldRegion> suggestedMaps;

	public SuggestionsMapsDownloadWarningCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		MapRouteInfoMenu mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
		isNavigationEnable = !Algorithms.isEmpty(mapRouteInfoMenu.getSuggestedMissingMaps());
		if (isNavigationEnable) {
			imageId = R.drawable.ic_action_time_span;
			title = mapRouteInfoMenu.isOnlineCheckNeeded() ? mapActivity.getString(R.string.online_maps_required_descr) : mapActivity.getString(R.string.direct_line_maps_required_descr);
			linkText = mapActivity.getString(R.string.online_direct_line_maps_link);
		} else {
			imageId = R.drawable.ic_map;
			title = mapActivity.getString(R.string.offline_maps_required_descr);
			linkText = mapActivity.getString(R.string.welmode_download_maps);
		}
	}

	private void showMultipleSelectionDialog() {
		List<DownloadItem> downloadMapsList = getMapsList();
		List<SelectionBottomSheet.SelectableItem> allItems = new ArrayList<>();
		List<SelectionBottomSheet.SelectableItem> selectedItems = new ArrayList<>();

		for (DownloadItem di : downloadMapsList) {
			boolean isSuggestedDownloadsMaps = di.getType().getTag().equals("map");
			SelectionBottomSheet.SelectableItem si = createSelectableItem(di);
			if (isSuggestedDownloadsMaps) {
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

		msDialog.setSelectionUpdateListener(this::updateSize);

		msDialog.setOnApplySelectionListener(selectedItems1 -> {
			List<IndexItem> indexes = new ArrayList<>();
			for (SelectionBottomSheet.SelectableItem item : selectedItems1) {
				IndexItem index = getIndexItem((DownloadItem) item.getObject());
				if (index != null) {
					indexes.add(index);
				}
			}
			IndexItem[] indexesArray = new IndexItem[indexes.size()];
			new DownloadValidationManager(app).startDownload(mapActivity, indexes.toArray(indexesArray));
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
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			if (mapActivity.getMyApplication().getSettings().isInternetConnectionAvailable()) {
				downloadThread.runReloadIndexFiles();
			}
		}

		boolean downloadIndexes = app.getSettings().isInternetConnectionAvailable()
				&& !downloadThread.getIndexes().isDownloadedFromInternet
				&& !downloadThread.getIndexes().downloadFromInternetFailed;

		if (isNavigationEnable) {
			suggestedMaps = mapActivity.getMapRouteInfoMenu().getSuggestedMissingMaps();
		} else {
			RouteProvider.setMissingMapsListener(missingMaps -> suggestedMaps = missingMaps);
		}
		List<DownloadItem> suggestedDownloadsMaps = new ArrayList<>();
		if (!downloadIndexes && !Algorithms.isEmpty(suggestedMaps)) {
			for (WorldRegion suggestedDownloadMap : suggestedMaps) {
				suggestedDownloadsMaps.addAll(app.getDownloadThread().getIndexes().getDownloadItems(suggestedDownloadMap));
			}
		}
		return suggestedDownloadsMaps;
	}

	private SelectionBottomSheet.SelectableItem createSelectableItem(DownloadItem item) {
		SelectionBottomSheet.SelectableItem selectableItem = new SelectionBottomSheet.SelectableItem();
		updateSelectableItem(selectableItem, item);
		selectableItem.setObject(item);
		return selectableItem;
	}

	private void updateSelectableItem(SelectionBottomSheet.SelectableItem selectableItem,
									  DownloadItem downloadItem) {
		selectableItem.setTitle(app.getRegions().getLocaleName(downloadItem.getBasename()));
		DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(app);
		String size = downloadItem.getSizeDescription(app);
		String additionalDescription = downloadItem.getAdditionalDescription(app);
		if (additionalDescription != null) {
			size += " " + additionalDescription;
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

