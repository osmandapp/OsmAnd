package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.download.DownloadActivityType.GEOTIFF_FILE;
import static net.osmand.plus.download.DownloadActivityType.HILLSHADE_FILE;
import static net.osmand.plus.download.DownloadActivityType.SLOPE_FILE;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class DownloadMapsCard {

	private static final Log LOG = PlatformUtil.getLog(DownloadMapsCard.class.getSimpleName());

	private final OsmandSettings settings;
	private final OsmandApplication app;
	private final SRTMPlugin srtmPlugin;
	private final boolean nightMode;

	private final LinearLayout downloadContainer;
	private final View downloadTopDivider;
	private final View downloadBottomDivider;
	private final ObservableListView observableListView;
	private ContextMenuListAdapter listAdapter;

	public DownloadMapsCard(@NonNull OsmandApplication app, @NonNull SRTMPlugin srtmPlugin, @NonNull View downloadMapsCardView, boolean nightMode) {
		this.app = app;
		settings = app.getSettings();
		this.srtmPlugin = srtmPlugin;
		this.nightMode = nightMode;

		downloadContainer = downloadMapsCardView.findViewById(R.id.download_container);
		downloadTopDivider = downloadMapsCardView.findViewById(R.id.download_container_top_divider);
		downloadBottomDivider = downloadMapsCardView.findViewById(R.id.download_container_bottom_divider);
		observableListView = downloadMapsCardView.findViewById(R.id.list_view);
	}

	public void downloadInProgress() {
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		IndexItem downloadIndexItem = downloadThread.getCurrentDownloadingItem();
		if (downloadIndexItem != null && listAdapter != null) {
			int downloadProgress = (int) downloadThread.getCurrentDownloadProgress();
			ArrayAdapter<ContextMenuItem> adapter = listAdapter;
			for (int i = 0; i < adapter.getCount(); i++) {
				ContextMenuItem item = adapter.getItem(i);
				if (item != null && item.getProgressListener() != null) {
					item.getProgressListener().onProgressChanged(
							downloadIndexItem, downloadProgress, adapter, (int) adapter.getItemId(i), i);
				}
			}
		}
	}

	public void updateDownloadSection(@Nullable MapActivity mapActivity) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);

		if (mapActivity == null) {
			return;
		}
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);

		DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			if (settings.isInternetConnectionAvailable()) {
				downloadThread.runReloadIndexFiles();
			}
		}

		if (downloadThread.shouldDownloadIndexes()) {
			adapter.addItem(new ContextMenuItem(null)
					.setLayout(R.layout.list_item_icon_and_download)
					.setTitleId(R.string.downloading_list_indexes, mapActivity)
					.setLoading(true));
		} else {
			try {
				DownloadActivityType type = getDownloadActivityType();
				IndexItem currentDownloadingItem = downloadThread.getCurrentDownloadingItem();
				int currentDownloadingProgress = (int) downloadThread.getCurrentDownloadProgress();
				List<IndexItem> terrainItems = DownloadResources.findIndexItemsAt(app,
						mapActivity.getMapLocation(), type, false, -1, true);
				if (terrainItems.size() > 0) {
					downloadContainer.setVisibility(View.VISIBLE);
					downloadTopDivider.setVisibility(View.VISIBLE);
					downloadBottomDivider.setVisibility(View.VISIBLE);
					for (IndexItem indexItem : terrainItems) {
						ContextMenuItem _item = new ContextMenuItem(null)
								.setLayout(R.layout.list_item_icon_and_download)
								.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
								.setDescription(type.getString(app) + " • " + indexItem.getSizeDescription(app))
								.setIcon(type.getIconResource())
								.setListener((uiAdapter, view, item, isChecked) -> {
									MapActivity mapActivity1 = mapActivityRef.get();
									if (mapActivity1 != null && !mapActivity1.isFinishing()) {
										if (downloadThread.isDownloading(indexItem)) {
											downloadThread.cancelDownload(indexItem);
											item.setProgress(ContextMenuItem.INVALID_ID);
											item.setLoading(false);
											item.setSecondaryIcon(R.drawable.ic_action_import);
											uiAdapter.onDataSetChanged();
										} else {
											new DownloadValidationManager(app).startDownload(mapActivity1, indexItem);
											item.setProgress(ContextMenuItem.INVALID_ID);
											item.setLoading(true);
											item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
											uiAdapter.onDataSetChanged();
										}
									}
									return false;
								})
								.setProgressListener((progressObject, progress, adptr, itemId, position) -> {
									if (progressObject instanceof IndexItem) {
										IndexItem progressItem = (IndexItem) progressObject;
										if (indexItem.compareTo(progressItem) == 0) {
											ContextMenuItem item = adptr.getItem(position);
											if (item != null) {
												item.setProgress(progress);
												item.setLoading(true);
												item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
												adptr.notifyDataSetChanged();
											}
											return true;
										}
									}
									return false;
								});

						if (indexItem == currentDownloadingItem) {
							_item.setLoading(true)
									.setProgress(currentDownloadingProgress)
									.setSecondaryIcon(R.drawable.ic_action_remove_dark);
						} else {
							_item.setSecondaryIcon(R.drawable.ic_action_import);
						}
						adapter.addItem(_item);
					}
				} else {
					downloadContainer.setVisibility(View.GONE);
					downloadTopDivider.setVisibility(View.GONE);
					downloadBottomDivider.setVisibility(View.GONE);
				}
			} catch (IOException e) {
				LOG.error(e);
			}
		}

		ApplicationMode appMode = settings.getApplicationMode();
		ViewCreator viewCreator = new ViewCreator(mapActivity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		viewCreator.setCustomControlsColor(appMode.getProfileColor(nightMode));

		listAdapter = adapter.toListAdapter(mapActivity, viewCreator);
		observableListView.setAdapter(listAdapter);
		observableListView.setOnItemClickListener((parent, view, position, id) -> {
			ContextMenuItem item = adapter.getItem(position);
			ItemClickListener click = item.getItemClickListener();
			if (click != null) {
				click.onContextMenuClick(listAdapter, view, item, false);
			}
		});
	}

	@NonNull
	private DownloadActivityType getDownloadActivityType() {
		OsmandDevelopmentPlugin plugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
		if (plugin != null && plugin.generateTerrainFrom3DMaps()) {
			return GEOTIFF_FILE;
		} else {
			return srtmPlugin.getTerrainMode().isHillshade() ? HILLSHADE_FILE : SLOPE_FILE;
		}
	}

}
