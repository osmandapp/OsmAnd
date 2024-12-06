package net.osmand.plus.routepreparationmenu;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.routepreparationmenu.CalculateMissingMapsOnlineTask.CalculateMissingMapsOnlineListener;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.router.MissingMapsCalculationResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequiredMapsController implements IDialogController, DownloadEvents {

	public static final String PROCESS_ID = "download_missing_maps";

	private final OsmandApplication app;

	private List<DownloadItem> mapsToDownload = new ArrayList<>();
	private List<DownloadItem> missingMaps = new ArrayList<>();
	private List<DownloadItem> usedMaps = new ArrayList<>();
	private boolean usedMapsPresent;
	private final ItemsSelectionHelper<DownloadItem> itemsSelectionHelper = new ItemsSelectionHelper<>();

	private boolean loadingMapsInProgress = false;
	private boolean onlineCalculationRequested = false;
	private CalculateMissingMapsOnlineTask onlineCalculationTask = null;

	public RequiredMapsController(@NonNull OsmandApplication app) {
		this.app = app;
		initContent();
	}

	public void initContent() {
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			if (isInternetConnectionAvailable()) {
				downloadThread.runReloadIndexFiles();
				loadingMapsInProgress = true;
			}
		}
		updateSelectionHelper();
	}

	private void updateSelectionHelper() {
		if (!loadingMapsInProgress) {
			updateMapsToDownload();
			itemsSelectionHelper.setAllItems(mapsToDownload);
			itemsSelectionHelper.setSelectedItems(missingMaps);
		}
	}

	@NonNull
	public List<DownloadItem> getMapsToDownload() {
		return mapsToDownload;
	}

	@NonNull
	public List<DownloadItem> getUsedMaps() {
		return usedMaps;
	}

	public boolean isItemSelected(@NonNull DownloadItem downloadItem) {
		return itemsSelectionHelper.isItemSelected(downloadItem);
	}

	private void updateMapsToDownload() {
		RouteCalculationResult route = app.getRoutingHelper().getRoute();
		MissingMapsCalculationResult result = route.getMissingMapsCalculationResult();
		this.mapsToDownload = collectMapsForRegions(result.getMapsToDownload());
		this.missingMaps = collectMapsForRegions(result.getMissingMaps());
		List<WorldRegion> usedMapRegions = result.getUsedMaps();
		this.usedMapsPresent = !Algorithms.isEmpty(usedMapRegions);
		this.usedMaps = collectMapsForRegions(usedMapRegions);
	}

	private List<DownloadItem> collectMapsForRegions(@NonNull List<WorldRegion> regions) {
		List<DownloadItem> result = new ArrayList<>();
		DownloadResources resources = app.getDownloadThread().getIndexes();
		if (!Algorithms.isEmpty(regions)) {
			for (WorldRegion missingRegion : regions) {
				for (DownloadItem downloadItem : resources.getDownloadItems(missingRegion)) {
					if (downloadItem.getType() == DownloadActivityType.NORMAL_FILE) {
						result.add(downloadItem);
					}
				}
			}
		}
		return result;
	}

	public void onIgnoreMissingMapsButtonClicked() {
		app.getSettings().IGNORE_MISSING_MAPS = true;
		app.getRoutingHelper().onSettingsChanged(true);
	}

	public void onCalculateOnlineButtonClicked() {
		onlineCalculationRequested = true;
		loadingMapsInProgress = true;
		askRefreshDialog();

		onlineCalculationTask = CalculateMissingMapsOnlineTask.execute(app, new CalculateMissingMapsOnlineListener() {
			@Override
			public void onSuccess() {
				loadingMapsInProgress = false;
				updateSelectionHelper();
				app.runInUIThread(() -> askRefreshDialog());
			}

			@Override
			public void onError(@Nullable String error) {
				loadingMapsInProgress = false;
				app.runInUIThread(() -> {
					if (!Algorithms.isEmpty(error)) {
						app.showToastMessage(error);
					}
					askRefreshDialog();
				});
			}
		});
	}

	@NonNull
	public String getDownloadButtonTitle() {
		double downloadSizeMb = 0.0d;
		for (DownloadItem downloadItem : itemsSelectionHelper.getSelectedItems()) {
			downloadSizeMb += downloadItem.getSizeToDownloadInMb();
		}
		String size = DownloadItem.getFormattedMb(app, downloadSizeMb);
		String btnTitle = app.getString(R.string.shared_string_download);
		boolean displaySize = !loadingMapsInProgress && downloadSizeMb > 0;
		return displaySize ? app.getString(R.string.ltr_or_rtl_combine_via_dash, btnTitle, size) : btnTitle;
	}

	public boolean isDownloadButtonEnabled() {
		return itemsSelectionHelper.hasSelectedItems() && !loadingMapsInProgress;
	}

	public void onDownloadButtonClicked(@NonNull MapActivity mapActivity) {
		mapActivity.getMapLayers().getMapActionsHelper().stopNavigationWithoutConfirm();
		mapActivity.getMapRouteInfoMenu().resetRouteCalculation();

		List<IndexItem> indexes = new ArrayList<>();
		for (DownloadItem item : itemsSelectionHelper.getSelectedItems()) {
			if (item instanceof IndexItem) {
				IndexItem index = (IndexItem) item;
				indexes.add(index);
			}
		}
		IndexItem[] indexesArray = new IndexItem[indexes.size()];
		new DownloadValidationManager(app).startDownload(mapActivity, indexes.toArray(indexesArray));

		Intent newIntent = new Intent(mapActivity, app.getAppCustomization().getDownloadActivity());
		newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		mapActivity.startActivity(newIntent);
	}

	public void onItemClicked(@NonNull DownloadItem downloadItem) {
		boolean selected = itemsSelectionHelper.isItemSelected(downloadItem);
		itemsSelectionHelper.onItemsSelected(Collections.singleton(downloadItem), !selected);
	}

	public void onSelectAllClicked() {
		if (itemsSelectionHelper.isAllItemsSelected()) {
			itemsSelectionHelper.clearSelectedItems();
		} else {
			itemsSelectionHelper.selectAllItems();
		}
	}

	public boolean isAllItemsSelected() {
		return itemsSelectionHelper.isAllItemsSelected();
	}

	public boolean isLoadingInProgress() {
		return loadingMapsInProgress;
	}

	public void askCancelOnlineCalculation() {
		if (onlineCalculationTask != null) {
			onlineCalculationTask.cancel(false);
			onlineCalculationTask = null;
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		loadingMapsInProgress = false;
		updateSelectionHelper();
		askRefreshDialog();
	}

	public void askRefreshDialog() {
		app.getDialogManager().askRefreshDialogCompletely(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new RequiredMapsController(app));
		RequiredMapsFragment.showInstance(activity.getSupportFragmentManager());
	}

	public boolean shouldShowOnlineCalculationBanner() {
		return !onlineCalculationRequested && !isLoadingInProgress() && isInternetConnectionAvailable();
	}

	public boolean shouldShowUseDownloadedMapsBanner() {
		return usedMapsPresent;
	}

	private boolean isInternetConnectionAvailable() {
		return app.getSettings().isInternetConnectionAvailable();
	}
}
