package net.osmand.plus.routepreparationmenu.cards;

import static net.osmand.plus.download.MultipleDownloadItem.getIndexItem;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.map.WorldRegion;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MultipleSelectionBottomSheet;
import net.osmand.plus.base.SelectionBottomSheet.DialogStateListener;
import net.osmand.plus.base.SelectionBottomSheet.SelectableItem;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class SuggestionsMapsDownloadWarningCard extends WarningCard implements DownloadEvents {
	private MultipleSelectionBottomSheet<DownloadItem> dialog;
	private final List<WorldRegion> suggestedMaps;
	private final boolean hasPrecalculatedMissingMaps;
	private final boolean suggestedMapsOnlineSearch;
	private final boolean missingMapsOnlineSearching;

	public SuggestionsMapsDownloadWarningCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		MapRouteInfoMenu mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
		List<WorldRegion> precalculatedMissingMaps = mapRouteInfoMenu.getSuggestedMaps();
		List<WorldRegion> calculatedMissingMaps = app.getRoutingHelper().getRoute().getMissingMaps();
		hasPrecalculatedMissingMaps = !Algorithms.isEmpty(precalculatedMissingMaps);
		suggestedMapsOnlineSearch = mapRouteInfoMenu.isSuggestedMapsOnlineSearch();
		missingMapsOnlineSearching = app.getRoutingHelper().isMissingMapsOnlineSearching();
		if (missingMapsOnlineSearching) {
			this.suggestedMaps = null;
			imageId = R.drawable.ic_action_time_span;
			title = mapActivity.getString(R.string.online_maps_searching_descr);
			linkText = "";
		} else if (suggestedMapsOnlineSearch) {
			this.suggestedMaps = precalculatedMissingMaps;
			imageId = R.drawable.ic_action_time_span;
			title = mapActivity.getString(!hasPrecalculatedMissingMaps
					? R.string.online_maps_required_descr : R.string.direct_line_maps_required_descr);
			linkText = mapActivity.getString(!hasPrecalculatedMissingMaps
					? R.string.online_direct_line_maps_link : R.string.welmode_download_maps);
		} else {
			this.suggestedMaps = hasPrecalculatedMissingMaps ? precalculatedMissingMaps : calculatedMissingMaps;
			imageId = R.drawable.ic_map;
			title = mapActivity.getString(R.string.offline_maps_required_descr);
			linkText = mapActivity.getString(R.string.welmode_download_maps);
		}
	}

	private void showMultipleSelectionDialog() {
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		boolean internetConnectionAvailable = mapActivity.getMyApplication().getSettings().isInternetConnectionAvailable();
		boolean downloadIndexes = false;
		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			if (internetConnectionAvailable) {
				downloadThread.runReloadIndexFiles();
				downloadIndexes = true;
			}
		}
		List<SelectableItem<DownloadItem>> allItems = new ArrayList<>();
		List<SelectableItem<DownloadItem>> selectedItems = new ArrayList<>();
		if (!downloadIndexes) {
			List<SelectableItem<DownloadItem>> mapItems = getSelectableMaps();
			allItems.addAll(mapItems);
			selectedItems.addAll(mapItems);
		}
		MultipleSelectionBottomSheet<DownloadItem> msDialog =
				MultipleSelectionBottomSheet.showInstance(mapActivity, allItems, selectedItems, true);
		this.dialog = msDialog;
		boolean downloadingIndexes = downloadIndexes;
		msDialog.setDialogStateListener(new DialogStateListener() {
			@Override
			public void onDialogCreated() {
				dialog.setTitle(app.getString(R.string.welmode_download_maps));
				if (!internetConnectionAvailable) {
					LayoutInflater inflater = UiUtilities.getInflater(dialog.getContext(), nightMode);
					View view = inflater.inflate(R.layout.bottom_sheet_no_internet_connection_view, null);
					DialogButton tryAgainButton = view.findViewById(R.id.try_again_button);
					tryAgainButton.setOnClickListener(v -> {
						if (!downloadThread.getIndexes().isDownloadedFromInternet) {
							if (mapActivity.getMyApplication().getSettings().isInternetConnectionAvailable()) {
								downloadThread.runReloadIndexFiles();
								setupDownloadingIndexesView();
							}
						}
					});
					AndroidUiHelper.updateVisibility(tryAgainButton, true);
					dialog.setCustomView(view);
				} else if (downloadingIndexes) {
					setupDownloadingIndexesView();
				}
			}

			private void setupDownloadingIndexesView() {
				LayoutInflater inflater = UiUtilities.getInflater(dialog.getContext(), nightMode);
				View view = inflater.inflate(R.layout.bottom_sheet_with_progress_bar, null);
				TextView title = view.findViewById(R.id.title);
				title.setVisibility(View.GONE);
				TextView description = view.findViewById(R.id.description);
				description.setText(R.string.downloading_list_indexes);
				view.findViewById(R.id.progress_bar).setVisibility(View.GONE);
				ProgressBar progressBar = view.findViewById(R.id.progress_bar_top);
				progressBar.setIndeterminate(true);
				progressBar.setVisibility(View.VISIBLE);
				dialog.setCustomView(view);
			}
		});
		msDialog.setSelectionUpdateListener(this::updateSize);
		msDialog.setOnApplySelectionListener(selItems -> {
			mapActivity.getMapLayers().getMapControlsLayer().stopNavigationWithoutConfirm();
			mapActivity.getMapRouteInfoMenu().resetRouteCalculation();

			List<IndexItem> indexes = new ArrayList<>();
			for (SelectableItem<DownloadItem> item : selItems) {
				IndexItem index = getIndexItem(item.getObject());
				if (index != null) {
					indexes.add(index);
				}
			}
			IndexItem[] indexesArray = new IndexItem[indexes.size()];
			new DownloadValidationManager(app).startDownload(mapActivity, indexes.toArray(indexesArray));

			Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization().getDownloadActivity());
			newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			mapActivity.startActivity(newIntent);
		});
	}

	@NonNull
	private List<SelectableItem<DownloadItem>> getSelectableMaps() {
		List<SelectableItem<DownloadItem>> res = new ArrayList<>();
		List<DownloadItem> downloadItems;
		downloadItems = getSuggestedMapDownloadItems();
		for (DownloadItem di : downloadItems) {
			boolean isMap = di.getType().getTag().equals("map");
			SelectableItem<DownloadItem> si = createSelectableItem(di);
			if (isMap) {
				res.add(si);
			}
		}
		return res;
	}

	private void updateSize() {
		if (dialog.isAdded()) {
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
	}

	private double getDownloadSizeInMb(@NonNull List<SelectableItem<DownloadItem>> selectableItems) {
		double totalSizeMb = 0.0d;
		for (SelectableItem<DownloadItem> i : selectableItems) {
			DownloadItem downloadItem = i.getObject();
			totalSizeMb += downloadItem.getSizeToDownloadInMb();
		}
		return totalSizeMb;
	}

	@NonNull
	public List<DownloadItem> getSuggestedMapDownloadItems() {
		List<DownloadItem> suggestedDownloadsMaps = new ArrayList<>();
		if (!Algorithms.isEmpty(suggestedMaps)) {
			for (WorldRegion suggestedDownloadMap : suggestedMaps) {
				suggestedDownloadsMaps.addAll(app.getDownloadThread().getIndexes().getDownloadItems(suggestedDownloadMap));
			}
		}
		return suggestedDownloadsMaps;
	}

	@NonNull
	private SelectableItem<DownloadItem> createSelectableItem(@NonNull DownloadItem item) {
		SelectableItem<DownloadItem> selectableItem = new SelectableItem<>();
		updateSelectableItem(selectableItem, item);
		selectableItem.setObject(item);
		return selectableItem;
	}

	private void updateSelectableItem(@NonNull SelectableItem<DownloadItem> selectableItem,
	                                  @NonNull DownloadItem downloadItem) {
		selectableItem.setTitle(app.getRegions().getLocaleName(downloadItem.getBasename(), true, true));
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
		if (suggestedMapsOnlineSearch) {
			if (!hasPrecalculatedMissingMaps) {
				app.getRoutingHelper().startMissingMapsOnlineSearch();
				mapActivity.getMapRouteInfoMenu().updateMenu();
			} else {
				showMultipleSelectionDialog();
			}
		} else if (!missingMapsOnlineSearching) {
			showMultipleSelectionDialog();
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		List<SelectableItem<DownloadItem>> allItems = new ArrayList<>();
		if (!app.getDownloadThread().shouldDownloadIndexes()) {
			List<SelectableItem<DownloadItem>> mapItems = getSelectableMaps();
			allItems.addAll(mapItems);
		}
		if (dialog != null && dialog.isAdded()) {
			dialog.setCustomView(null);
			dialog.setSelectedItems(allItems);
			dialog.setItems(allItems);
			dialog.onSelectedItemsChanged();
			updateSize();
		}
	}

	@Override
	public void downloadInProgress() {
	}

	@Override
	public void downloadHasFinished() {
	}
}