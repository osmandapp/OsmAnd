package net.osmand.plus.srtmplugin;

import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.AndroidUtils;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.render.RenderingRuleProperty;

import java.io.IOException;
import java.util.List;

import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_DENSITY_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_DISABLED_VALUE;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_SCHEME_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_WIDTH_ATTR;

public class ContourLinesMenu {
	private static final String TAG = "ContourLinesMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity) {
		SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		if (plugin != null && !plugin.isActive() && !plugin.needsInstallation()) {
			OsmandPlugin.enablePlugin(mapActivity, mapActivity.getMyApplication(), plugin, true);
		}
		boolean nightMode = isNightMode(mapActivity.getMyApplication());
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity.getMyApplication());
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);
		createLayersItems(adapter, mapActivity);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
										  final MapActivity mapActivity) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final SRTMPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		final boolean srtmEnabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null || InAppPurchaseHelper.isContourLinesPurchased(app);

		final RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		final RenderingRuleProperty colorSchemeProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_SCHEME_ATTR);
		if (plugin == null || contourLinesProp == null || colorSchemeProp == null) {
			return;
		}

		final String contourWidthName;
		final String contourDensityName;
		final CommonPreference<String> widthPref;
		final CommonPreference<String> densityPref;
		final RenderingRuleProperty contourWidthProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_WIDTH_ATTR);
		if (contourWidthProp != null) {
			contourWidthName = AndroidUtils.getRenderingStringPropertyName(app, contourWidthProp.getAttrName(),
					contourWidthProp.getName());
			widthPref = settings.getCustomRenderProperty(contourWidthProp.getAttrName());
		} else {
			contourWidthName = null;
			widthPref = null;
		}
		final RenderingRuleProperty contourDensityProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_DENSITY_ATTR);
		if (contourDensityProp != null) {
			contourDensityName = AndroidUtils.getRenderingStringPropertyName(app, contourDensityProp.getAttrName(),
					contourDensityProp.getName());
			densityPref = settings.getCustomRenderProperty(contourDensityProp.getAttrName());
		} else {
			contourDensityName = null;
			densityPref = null;
		}

		final CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
		final CommonPreference<String> colorPref = settings.getCustomRenderProperty(colorSchemeProp.getAttrName());

		final boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);
		final int toggleActionStringId = selected ? R.string.shared_string_on : R.string.shared_string_off;
		final int showZoomLevelStringId = R.string.show_from_zoom_level;
		final int colorSchemeStringId = R.string.srtm_color_scheme;

		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
										  View view, int itemId, int pos) {
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
											  final int itemId, final int pos, final boolean isChecked, int[] viewCoordinates) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							plugin.toggleContourLines(mapActivity, isChecked, new Runnable() {
								@Override
								public void run() {
									mapActivity.getDashboard().refreshContent(true);
									mapActivity.refreshMapComplete();
								}
							});
						}
					});
				} else if (itemId == showZoomLevelStringId) {
					plugin.selectPropertyValue(mapActivity, contourLinesProp, pref, new Runnable() {
						@Override
						public void run() {
							ContextMenuItem item = adapter.getItem(pos);
							if (item != null) {
								item.setDescription(plugin.getPrefDescription(app, contourLinesProp, pref));
								adapter.notifyDataSetChanged();
							}
							mapActivity.refreshMapComplete();
						}
					});
				} else if (itemId == colorSchemeStringId) {
					plugin.selectPropertyValue(mapActivity, colorSchemeProp, colorPref, new Runnable() {
						@Override
						public void run() {
							ContextMenuItem item = adapter.getItem(pos);
							if (item != null) {
								item.setDescription(plugin.getPrefDescription(app, colorSchemeProp, colorPref));
								adapter.notifyDataSetChanged();
							}
							mapActivity.refreshMapComplete();
						}
					});
				} else if (itemId == R.string.srtm_plugin_name) {
					ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.TERRAIN);
					closeDashboard(mapActivity);
				} else if (contourWidthProp != null && itemId == contourWidthName.hashCode()) {
					plugin.selectPropertyValue(mapActivity, contourWidthProp, widthPref, new Runnable() {
						@Override
						public void run() {
							ContextMenuItem item = adapter.getItem(pos);
							if (item != null) {
								item.setDescription(plugin.getPrefDescription(app, contourWidthProp, widthPref));
								adapter.notifyDataSetChanged();
							}
							mapActivity.refreshMapComplete();
						}
					});
				} else if (contourDensityProp != null && itemId == contourDensityName.hashCode()) {
					plugin.selectPropertyValue(mapActivity, contourDensityProp, densityPref, new Runnable() {
						@Override
						public void run() {
							ContextMenuItem item = adapter.getItem(pos);
							if (item != null) {
								item.setDescription(plugin.getPrefDescription(app, contourDensityProp, densityPref));
								adapter.notifyDataSetChanged();
							}
							mapActivity.refreshMapComplete();
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
					R.color.active_color_primary_dark : R.color.active_color_primary_light;
		} else {
			toggleIconId = R.drawable.ic_action_hide;
			toggleIconColorId = ContextMenuItem.INVALID_ID;
		}
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(toggleActionStringId, mapActivity)
				.setIcon(toggleIconId)
				.setColor(app, toggleIconColorId)
				.setListener(l)
				.setSelected(selected).createItem());
		if (selected) {
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(showZoomLevelStringId, mapActivity)
					.setLayout(R.layout.list_item_single_line_descrition_narrow)
					.setIcon(R.drawable.ic_action_map_magnifier)
					.setDescription(plugin.getPrefDescription(app, contourLinesProp, pref))
					.setListener(l).createItem());
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(colorSchemeStringId, mapActivity)
					.setLayout(R.layout.list_item_single_line_descrition_narrow)
					.setIcon(R.drawable.ic_action_appearance)
					.setDescription(plugin.getPrefDescription(app, colorSchemeProp, colorPref))
					.setListener(l).createItem());
			if (contourWidthProp != null) {
				contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitle(contourWidthName)
						.setLayout(R.layout.list_item_single_line_descrition_narrow)
						.setIcon(R.drawable.ic_action_gpx_width_thin)
						.setDescription(plugin.getPrefDescription(app, contourWidthProp, widthPref))
						.setListener(l).createItem());
			}
			if (contourDensityProp != null) {
				contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitle(contourDensityName)
						.setLayout(R.layout.list_item_single_line_descrition_narrow)
						.setIcon(R.drawable.ic_plugin_srtm)
						.setDescription(plugin.getPrefDescription(app, contourDensityProp, densityPref))
						.setListener(l).createItem());
			}
		}

		if (!srtmEnabled) {
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.srtm_purchase_header, mapActivity)
					.setCategory(true).setLayout(R.layout.list_group_title_with_switch_light).createItem());
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.srtm_plugin_name, mapActivity)
					.setLayout(R.layout.list_item_icon_and_right_btn)
					.setIcon(R.drawable.ic_plugin_srtm)
					.setColor(app, R.color.osmand_orange)
					.setDescription(app.getString(R.string.shared_string_plugin))
					.setListener(l).createItem());
		} else {
			final DownloadIndexesThread downloadThread = app.getDownloadThread();
			if (!downloadThread.getIndexes().isDownloadedFromInternet) {
				if (settings.isInternetConnectionAvailable()) {
					downloadThread.runReloadIndexFiles();
				}
			}

			if (downloadThread.shouldDownloadIndexes()) {
				contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitleId(R.string.shared_string_download_map, mapActivity)
						.setDescription(app.getString(R.string.srtm_menu_download_descr))
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
					List<IndexItem> srtms = DownloadResources.findIndexItemsAt(
							app, mapActivity.getMapLocation(), DownloadActivityType.SRTM_COUNTRY_FILE);
					if (srtms.size() > 0) {
						contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
								.setTitleId(R.string.shared_string_download_map, mapActivity)
								.setDescription(app.getString(R.string.srtm_menu_download_descr))
								.setCategory(true)
								.setLayout(R.layout.list_group_title_with_descr).createItem());
						for (final IndexItem indexItem : srtms) {
							ContextMenuItem.ItemBuilder itemBuilder = new ContextMenuItem.ItemBuilder()
									.setLayout(R.layout.list_item_icon_and_download)
									.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
									.setDescription(DownloadActivityType.SRTM_COUNTRY_FILE.getString(app) + " • " + indexItem.getSizeDescription(app))
									.setIcon(DownloadActivityType.SRTM_COUNTRY_FILE.getIconResource())
									.setListener(new ContextMenuAdapter.ItemClickListener() {
										@Override
										public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
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
	
	public static boolean isNightMode(OsmandApplication app) {
		if (app == null) {
			return false;
		}
		return app.getDaynightHelper().isNightModeForMapControls();
	}

	public static void closeDashboard(MapActivity mapActivity) {
		mapActivity.getDashboard().hideDashboard(false);
	}
}
