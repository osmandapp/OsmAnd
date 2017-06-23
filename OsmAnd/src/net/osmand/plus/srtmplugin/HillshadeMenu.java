package net.osmand.plus.srtmplugin;

import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;

import java.io.IOException;
import java.util.List;

public class HillshadeMenu {
	private static final String TAG = "HillshadeMenu";


	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		if (plugin != null && !plugin.isActive() && !plugin.needsInstallation()) {
			OsmandPlugin.enablePlugin(mapActivity, mapActivity.getMyApplication(), plugin, true);
		}
		ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		createLayersItems(adapter, mapActivity);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
										  final MapActivity mapActivity) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		final boolean srtmEnabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null;
		if (plugin == null) {
			return;
		}

		final boolean selected = plugin.isHillShadeLayerEnabled();
		final int toggleActionStringId = selected ? R.string.shared_string_enabled : R.string.shared_string_disabled;

		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
										  View view, int itemId, int pos) {
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
											  final int itemId, final int pos, final boolean isChecked) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							plugin.toggleHillshade(mapActivity, isChecked, new Runnable() {
								@Override
								public void run() {
									mapActivity.getDashboard().refreshContent(true);
									plugin.updateLayers(mapActivity.getMapView(), mapActivity);
									SRTMPlugin.refreshMapComplete(mapActivity);
								}
							});
						}
					});
				}
				return false;
			}
		};

		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		int toggleIconColorId;
		int toggleIconId;
		if (selected) {
			toggleIconId = R.drawable.ic_action_view;
			toggleIconColorId = nightMode ?
					R.color.color_dialog_buttons_dark : R.color.color_dialog_buttons_light;
		} else {
			toggleIconId = R.drawable.ic_action_hide;
			toggleIconColorId = nightMode ? 0 : R.color.icon_color;
		}
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(toggleActionStringId, mapActivity)
				.setIcon(toggleIconId)
				.setColor(toggleIconColorId)
				.setListener(l)
				.setSelected(selected).createItem());

		if (!srtmEnabled) {
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.hillshade_purchase_header, mapActivity)
					.setCategory(true).setLayout(R.layout.list_group_title_with_switch_light).createItem());
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.srtm_plugin_name, mapActivity)
					.setLayout(R.layout.list_item_icon_and_right_btn)
					.setIcon(R.drawable.ic_plugin_srtm)
					.setColor(R.color.osmand_orange)
					.setDescription(app.getString(R.string.shared_string_plugin))
					.setListener(l).createItem());
		} else {
			final DownloadIndexesThread downloadThread = app.getDownloadThread();
			if (!downloadThread.getIndexes().isDownloadedFromInternet) {
				if (settings.isInternetConnectionAvailable()) {
					downloadThread.runReloadIndexFiles();
				}
			}
			final boolean downloadIndexes = settings.isInternetConnectionAvailable()
					&& !downloadThread.getIndexes().isDownloadedFromInternet
					&& !downloadThread.getIndexes().downloadFromInternetFailed;

			if (downloadIndexes) {
				contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitleId(R.string.shared_string_download_map, mapActivity)
						.setDescription(app.getString(R.string.hillshade_menu_download_descr))
						.setCategory(true)
						.setLayout(R.layout.list_group_title_with_descr).createItem());
				contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
						.setLayout(R.layout.list_item_icon_and_download)
						.setTitleId(R.string.downloading_list_indexes, mapActivity)
						.setLoading(true)
						.setListener(l).createItem());
			} else {
				try {
					IndexItem currentDownloadingItem = downloadThread.getCurrentDownloadingItem();
					int currentDownloadingProgress = downloadThread.getCurrentDownloadingItemProgress();
					List<IndexItem> hillshadeItems = DownloadResources.findIndexItemsAt(
							app, mapActivity.getMapLocation(), DownloadActivityType.HILLSHADE_FILE);
					if (hillshadeItems.size() > 0) {
						contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
								.setTitleId(R.string.shared_string_download_map, mapActivity)
								.setDescription(app.getString(R.string.hillshade_menu_download_descr))
								.setCategory(true)
								.setLayout(R.layout.list_group_title_with_descr).createItem());
						for (final IndexItem indexItem : hillshadeItems) {
							ContextMenuItem.ItemBuilder itemBuilder = new ContextMenuItem.ItemBuilder()
									.setLayout(R.layout.list_item_icon_and_download)
									.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
									.setDescription(DownloadActivityType.HILLSHADE_FILE.getString(app) + " • " + indexItem.getSizeDescription(app))
									.setIcon(DownloadActivityType.HILLSHADE_FILE.getIconResource())
									.setListener(new ContextMenuAdapter.ItemClickListener() {
										@Override
										public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked) {
											ContextMenuItem item = adapter.getItem(position);
											if (downloadThread.isDownloading(indexItem)) {
												downloadThread.cancelDownload(indexItem);
												if (item != null) {
													item.setProgress(ContextMenuItem.INVALID_ID);
													item.setLoading(false);
													item.setSecondaryIcon(R.drawable.ic_action_import);
													adapter.notifyDataSetChanged();
												}
											} else {
												new DownloadValidationManager(app).startDownload(mapActivity, indexItem);
												if (item != null) {
													item.setProgress(ContextMenuItem.INVALID_ID);
													item.setLoading(true);
													item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
													adapter.notifyDataSetChanged();
												}
											}
											return false;
										}
									})
									.setProgressListener(new ContextMenuAdapter.ProgressListener() {
										@Override
										public boolean onProgressChanged(Object progressObject, int progress,
																		 ArrayAdapter<ContextMenuItem> adapter,
																		 int itemId, int position) {
											if (progressObject != null && progressObject instanceof IndexItem) {
												IndexItem progressItem = (IndexItem) progressObject;
												if (indexItem.compareTo(progressItem) == 0) {
													ContextMenuItem item = adapter.getItem(position);
													if (item != null) {
														item.setProgress(progress);
														item.setLoading(true);
														item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
														adapter.notifyDataSetChanged();
													}
													return true;
												}
											}
											return false;
										}
									});

							if (indexItem == currentDownloadingItem) {
								itemBuilder.setLoading(true)
										.setProgress(currentDownloadingProgress)
										.setSecondaryIcon(R.drawable.ic_action_remove_dark);
							} else {
								itemBuilder.setSecondaryIcon(R.drawable.ic_action_import);
							}
							contextMenuAdapter.addItem(itemBuilder.createItem());
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
