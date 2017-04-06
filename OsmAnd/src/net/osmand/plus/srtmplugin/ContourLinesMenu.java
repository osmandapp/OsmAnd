package net.osmand.plus.srtmplugin;

import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.RouteLayer;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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

		final RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		final RenderingRuleProperty colorSchemeProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_SCHEME_ATTR);
		if (plugin == null || contourLinesProp == null || colorSchemeProp == null) {
			return;
		}

		final String contourWidthName;
		final String contourDensityName;
		final OsmandSettings.CommonPreference<String> widthPref;
		final OsmandSettings.CommonPreference<String> densityPref;
		final RenderingRuleProperty contourWidthProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_WIDTH_ATTR);
		if (contourWidthProp != null) {
			contourWidthName = SettingsActivity.getStringPropertyName(app, contourWidthProp.getAttrName(),
					contourWidthProp.getName());
			widthPref = settings.getCustomRenderProperty(contourWidthProp.getAttrName());
		} else {
			contourWidthName = null;
			widthPref = null;
		}
		final RenderingRuleProperty contourDensityProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_DENSITY_ATTR);
		if (contourDensityProp != null) {
			contourDensityName = SettingsActivity.getStringPropertyName(app, contourDensityProp.getAttrName(),
					contourDensityProp.getName());
			densityPref = settings.getCustomRenderProperty(contourDensityProp.getAttrName());
		} else {
			contourDensityName = null;
			densityPref = null;
		}

		final OsmandSettings.CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
		final OsmandSettings.CommonPreference<String> colorPref = settings.getCustomRenderProperty(colorSchemeProp.getAttrName());

		final boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);
		final int toggleActionStringId = selected ? R.string.shared_string_enabled : R.string.shared_string_disabled;
		final int showZoomLevelStringId = R.string.show_from_zoom_level;
		final int colorSchemeStringId = R.string.srtm_color_scheme;

		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
										  View view, int itemId, int pos) {
				/*
				if (itemId == showZoomLevelStringId) {
					if (selected) {
					}
					return false;
				}
				*/
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
											  final int itemId, final int pos, final boolean isChecked) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							plugin.toggleContourLines(mapActivity, isChecked, new Runnable() {
								@Override
								public void run() {
									mapActivity.getDashboard().refreshContent(true);
									refreshMapComplete(mapActivity);
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
							refreshMapComplete(mapActivity);
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
							refreshMapComplete(mapActivity);
						}
					});
				} else if (itemId == R.string.srtm_plugin_name) {
					Intent intent = new Intent(mapActivity, PluginActivity.class);
					intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, plugin.getId());
					mapActivity.startActivity(intent);
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
							refreshMapComplete(mapActivity);
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
							refreshMapComplete(mapActivity);
						}
					});
				}
				return false;
			}
		};

		boolean light = settings.isLightContent();
		int toggleIconColorId;
		int toggleIconId;
		if (selected) {
			toggleIconId = R.drawable.ic_action_view;
			toggleIconColorId = light ?
					R.color.color_dialog_buttons_light : R.color.color_dialog_buttons_dark;
		} else {
			toggleIconId = R.drawable.ic_action_hide;
			toggleIconColorId = light ? R.color.icon_color : 0;
		}
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(toggleActionStringId, mapActivity)
				.setIcon(toggleIconId)
				.setColor(toggleIconColorId)
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
					List<IndexItem> srtms = findSrtmIndexItems(app, mapActivity.getMapLocation());
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
									.setDescription(app.getString(R.string.srtm_plugin_name) + " • " + indexItem.getSizeDescription(app))
									.setIcon(R.drawable.ic_plugin_srtm)
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

	public static void closeDashboard(MapActivity mapActivity) {
		mapActivity.getDashboard().hideDashboard(false);
	}

	public static void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		GPXLayer gpx = activity.getMapView().getLayerByClass(GPXLayer.class);
		if (gpx != null) {
			gpx.updateLayerStyle();
		}
		RouteLayer rte = activity.getMapView().getLayerByClass(RouteLayer.class);
		if (rte != null) {
			rte.updateLayerStyle();
		}
		activity.getMapView().refreshMap(true);
	}

	public static List<IndexItem> findSrtmIndexItems(OsmandApplication app, LatLon latLon) throws IOException {

		List<IndexItem> res = new ArrayList<>();
		OsmandRegions regions = app.getRegions();
		DownloadIndexesThread downloadThread = app.getDownloadThread();

		int point31x = MapUtils.get31TileNumberX(latLon.getLongitude());
		int point31y = MapUtils.get31TileNumberY(latLon.getLatitude());

		List<BinaryMapDataObject> mapDataObjects;
		try {
			mapDataObjects = regions.queryBbox(point31x, point31x, point31y, point31y);
		} catch (IOException e) {
			throw new IOException("Error while calling queryBbox");
		}
		if (mapDataObjects != null) {
			Iterator<BinaryMapDataObject> it = mapDataObjects.iterator();
			while (it.hasNext()) {
				BinaryMapDataObject o = it.next();
				if (o.getTypes() != null) {
					boolean isRegion = true;
					for (int i = 0; i < o.getTypes().length; i++) {
						BinaryMapIndexReader.TagValuePair tp = o.getMapIndex().decodeType(o.getTypes()[i]);
						if ("boundary".equals(tp.value)) {
							isRegion = false;
							break;
						}
					}
					WorldRegion downloadRegion = regions.getRegionData(regions.getFullName(o));
					if (downloadRegion == null || !isRegion || !regions.contain(o, point31x, point31y)) {
						it.remove();
					}
					List<IndexItem> otherIndexItems = new ArrayList<>(downloadThread.getIndexes().getIndexItems(downloadRegion));
					for (IndexItem indexItem : otherIndexItems) {
						if (indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE
								&& !res.contains(indexItem)) {
							if (indexItem.isDownloaded()) {
								res.clear();
								return res;
							}
							res.add(indexItem);
						}
					}
				}
			}
		}
		return res;
	}
}
