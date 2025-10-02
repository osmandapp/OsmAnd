package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_DENSITY_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_DISABLED_VALUE;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_SCHEME_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_WIDTH_ATTR;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.SelectIndexesHelper;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.callback.ProgressListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DateFormat;
import java.util.List;

public class ContourLinesMenu {

	private static final Log LOG = PlatformUtil.getLog(ContourLinesMenu.class);
	private static final String TAG = "ContourLinesMenu";

	public static ContextMenuAdapter createListAdapter(MapActivity mapActivity) {
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		PluginsHelper.enablePluginIfNeeded(mapActivity, mapActivity.getApp(), plugin, true);
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity.getApp());
		createLayersItems(adapter, mapActivity);
		return adapter;
	}

	private static void createLayersItems(@NonNull ContextMenuAdapter contextMenuAdapter,
	                                      @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		OsmandSettings settings = app.getSettings();
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		boolean srtmEnabled = PluginsHelper.isActive(SRTMPlugin.class) || InAppPurchaseUtils.isContourLinesAvailable(app);

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		RenderingRuleProperty colorSchemeProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_SCHEME_ATTR);
		if (plugin == null || contourLinesProp == null || colorSchemeProp == null) {
			return;
		}

		String contourWidthName;
		String contourDensityName;
		CommonPreference<String> widthPref;
		CommonPreference<String> densityPref;
		RenderingRuleProperty contourWidthProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_WIDTH_ATTR);
		if (contourWidthProp != null) {
			contourWidthName = AndroidUtils.getRenderingStringPropertyName(app, contourWidthProp.getAttrName(),
					contourWidthProp.getName());
			widthPref = settings.getCustomRenderProperty(contourWidthProp.getAttrName());
		} else {
			contourWidthName = null;
			widthPref = null;
		}
		RenderingRuleProperty contourDensityProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_DENSITY_ATTR);
		if (contourDensityProp != null) {
			contourDensityName = AndroidUtils.getRenderingStringPropertyName(app, contourDensityProp.getAttrName(),
					contourDensityProp.getName());
			densityPref = settings.getCustomRenderProperty(contourDensityProp.getAttrName());
		} else {
			contourDensityName = null;
			densityPref = null;
		}

		CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
		CommonPreference<String> colorPref = settings.getCustomRenderProperty(colorSchemeProp.getAttrName());

		boolean selected = !pref.get().equals(CONTOUR_LINES_DISABLED_VALUE);
		int toggleActionStringId = R.string.download_srtm_maps;
		final int showZoomLevelStringId = R.string.show_from_zoom_level;
		final int colorSchemeStringId = R.string.srtm_color_scheme;

		OnRowItemClick l = new OnRowItemClick() {

			@Override
			public boolean onContextMenuClick(@NonNull OnDataChangeUiAdapter uiAdapter,
			                                  @Nullable View view, @NotNull ContextMenuItem item,
			                                  boolean isChecked) {
				int itemId = item.getTitleId();
				if (itemId == toggleActionStringId) {
					app.runInUIThread(() -> plugin.toggleContourLines(mapActivity, isChecked, () -> {
						mapActivity.getDashboard().refreshContent(true);
						mapActivity.refreshMapComplete();
					}));
				} else if (itemId == showZoomLevelStringId) {
					plugin.selectPropertyValue(mapActivity, contourLinesProp, pref,
							() -> onPropertyValueSelected(uiAdapter, item, contourLinesProp));
				} else if (itemId == colorSchemeStringId) {
					plugin.selectPropertyValue(mapActivity, colorSchemeProp, colorPref,
							() -> onPropertyValueSelected(uiAdapter, item, colorSchemeProp));
				} else if (itemId == R.string.download_srtm_maps) {
					ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.TERRAIN);
					closeDashboard(mapActivity);
				} else if (contourWidthProp != null && itemId == contourWidthName.hashCode()) {
					plugin.selectPropertyValue(mapActivity, contourWidthProp, widthPref,
							() -> onPropertyValueSelected(uiAdapter, item, contourWidthProp));
				} else if (contourDensityProp != null && itemId == contourDensityName.hashCode()) {
					plugin.selectPropertyValue(mapActivity, contourDensityProp, densityPref,
							() -> onPropertyValueSelected(uiAdapter, item, contourDensityProp));
				}
				return false;
			}

			private void onPropertyValueSelected(@NonNull OnDataChangeUiAdapter uiAdapter,
			                                     @NonNull ContextMenuItem item,
			                                     @NonNull RenderingRuleProperty property) {
				item.setDescription(AndroidUtils.getRenderingStringPropertyValue(app, property));
				uiAdapter.onDataSetChanged();
				mapActivity.refreshMapComplete();
			}
		};

		boolean nightMode = mapActivity.getApp().getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
		int toggleIconColorId;
		int toggleIconId;
		if (selected) {
			toggleIconId = R.drawable.ic_action_view;
			toggleIconColorId = ColorUtilities.getActiveColorId(nightMode);
		} else {
			toggleIconId = R.drawable.ic_action_hide;
			toggleIconColorId = ContextMenuItem.INVALID_ID;
		}
		String summary = mapActivity.getString(selected ? R.string.shared_string_enabled : R.string.shared_string_disabled);
		contextMenuAdapter.addItem(new ContextMenuItem(null)
				.setTitleId(toggleActionStringId, mapActivity)
				.setDescription(summary)
				.setIcon(toggleIconId)
				.setColor(app, toggleIconColorId)
				.setListener(l)
				.setSelected(selected));
		if (selected) {
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(showZoomLevelStringId, mapActivity)
					.setLayout(R.layout.list_item_single_line_descrition_narrow)
					.setIcon(R.drawable.ic_action_map_magnifier)
					.setDescription(AndroidUtils.getRenderingStringPropertyValue(app, contourLinesProp))
					.setListener(l));
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(colorSchemeStringId, mapActivity)
					.setLayout(R.layout.list_item_single_line_descrition_narrow)
					.setIcon(R.drawable.ic_action_appearance)
					.setDescription(AndroidUtils.getRenderingStringPropertyValue(app, colorSchemeProp))
					.setListener(l));
			if (contourWidthProp != null) {
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setTitle(contourWidthName)
						.setLayout(R.layout.list_item_single_line_descrition_narrow)
						.setIcon(R.drawable.ic_action_gpx_width_thin)
						.setDescription(AndroidUtils.getRenderingStringPropertyValue(app, contourWidthProp))
						.setListener(l));
			}
			if (contourDensityProp != null) {
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setTitle(contourDensityName)
						.setLayout(R.layout.list_item_single_line_descrition_narrow)
						.setIcon(R.drawable.ic_plugin_srtm)
						.setDescription(AndroidUtils.getRenderingStringPropertyValue(app, contourDensityProp))
						.setListener(l));
			}
		}

		if (!srtmEnabled) {
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setCategory(true)
					.setTitleId(R.string.srtm_purchase_header, mapActivity)
					.setLayout(R.layout.list_group_title_with_switch_light));
			contextMenuAdapter.addItem(new ContextMenuItem(null)
					.setTitleId(R.string.download_srtm_maps, mapActivity)
					.setLayout(R.layout.list_item_icon_and_right_btn)
					.setIcon(R.drawable.ic_plugin_srtm)
					.setColor(app, R.color.osmand_orange)
					.setDescription(app.getString(R.string.shared_string_plugin))
					.setListener(l));
		} else {
			DownloadIndexesThread downloadThread = app.getDownloadThread();
			if (!downloadThread.getIndexes().isDownloadedFromInternet) {
				if (settings.isInternetConnectionAvailable()) {
					downloadThread.runReloadIndexFiles();
				}
			}

			if (downloadThread.shouldDownloadIndexes()) {
				contextMenuAdapter.addItem(createDownloadSrtmMapsItem(mapActivity));
				contextMenuAdapter.addItem(new ContextMenuItem(null)
						.setLayout(R.layout.list_item_icon_and_download)
						.setTitleId(R.string.downloading_list_indexes, mapActivity)
						.setLoading(true)
						.setListener(l));
			} else {
				try {
					List<IndexItem> srtms = DownloadResources.findIndexItemsAt(
							app, mapActivity.getMapLocation(), DownloadActivityType.SRTM_COUNTRY_FILE,
							false, 1, true);
					SrtmDownloadItem srtmDownloadItem = convertToSrtmDownloadItem(app, srtms);
					if (srtmDownloadItem != null) {
						contextMenuAdapter.addItem(createDownloadSrtmMapsItem(mapActivity));
						contextMenuAdapter.addItem(createSrtmDownloadItem(mapActivity, srtmDownloadItem));
					}
				} catch (IOException e) {
					LOG.error(e);
				}
			}
		}

		contextMenuAdapter.addItem(new ContextMenuItem(null)
				.setLayout(R.layout.card_bottom_divider)
				);
	}

	private static ContextMenuItem createDownloadSrtmMapsItem(MapActivity mapActivity) {
		return new ContextMenuItem(null)
				.setCategory(true)
				.setTitleId(R.string.shared_string_download_map, mapActivity)
				.setDescription(mapActivity.getString(R.string.srtm_menu_download_descr))
				.setLayout(R.layout.list_group_title_with_descr);
	}

	@Nullable
	private static SrtmDownloadItem convertToSrtmDownloadItem(@NonNull OsmandApplication app, List<IndexItem> srtms) {
		if (Algorithms.isEmpty(srtms)) {
			return null;
		}
		List<DownloadItem> individualResources = srtms.get(0).getRelatedGroup().getIndividualDownloadItems();
		for (DownloadItem downloadItem : individualResources) {
			if (downloadItem instanceof SrtmDownloadItem srtmDownloadItem) {
				srtmDownloadItem.updateMetric(app);
				return srtmDownloadItem;
			}
		}
		return null;
	}

	private static ContextMenuItem createSrtmDownloadItem(MapActivity mapActivity, SrtmDownloadItem srtmDownloadItem) {
		OsmandApplication app = mapActivity.getApp();
		DownloadIndexesThread downloadThread = app.getDownloadThread();

		ContextMenuItem item = new ContextMenuItem(null)
				.setLayout(R.layout.list_item_icon_and_download)
				.setTitle(srtmDownloadItem.getVisibleName(app, app.getRegions(), false))
				.setDescription(DownloadActivityType.SRTM_COUNTRY_FILE.getString(app))
				.setHideDivider(true)
				.setIcon(DownloadActivityType.SRTM_COUNTRY_FILE.getIconResource())
				.setListener(getOnSrtmItemClickListener(mapActivity, srtmDownloadItem))
				.setProgressListener(getSrtmItemProgressListener(srtmDownloadItem, downloadThread));

		if (srtmDownloadItem.isCurrentlyDownloading(downloadThread)) {
			item.setLoading(true)
					.setProgress((int) downloadThread.getCurrentDownloadProgress())
					.setSecondaryIcon(R.drawable.ic_action_remove_dark);
		} else {
			item.setSecondaryIcon(R.drawable.ic_action_import);
		}

		return item;
	}

	private static ItemClickListener getOnSrtmItemClickListener(MapActivity mapActivity,
	                                                            SrtmDownloadItem srtmDownloadItem) {
		return (uiAdapter, view, item, isChecked) -> {

			OsmandApplication app = mapActivity.getApp();
			DownloadIndexesThread downloadThread = app.getDownloadThread();
			IndexItem indexItem = srtmDownloadItem.getIndexItem(downloadThread);

			if (downloadThread.isDownloading(indexItem)) {
				downloadThread.cancelDownload(indexItem);
				item.setProgress(ContextMenuItem.INVALID_ID);
				item.setLoading(false);
				item.setSecondaryIcon(R.drawable.ic_action_import);
				uiAdapter.onDataSetChanged();
			} else {
				DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(mapActivity);
				SelectIndexesHelper.showDialog(srtmDownloadItem, mapActivity, dateFormat, true, items -> {
					IndexItem[] toDownload = new IndexItem[items.size()];
					new DownloadValidationManager(app).startDownload(mapActivity, items.toArray(toDownload));
					item.setProgress(ContextMenuItem.INVALID_ID);
					item.setLoading(true);
					item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
					uiAdapter.onDataSetChanged();
				});
			}

			return false;
		};
	}

	private static ProgressListener getSrtmItemProgressListener(SrtmDownloadItem srtmDownloadItem,
	                                                            DownloadIndexesThread downloadThread) {
		return (progressObject, progress, adapter, itemId, position) -> {
			if (progressObject instanceof IndexItem) {
				IndexItem progressItem = (IndexItem) progressObject;
				if (srtmDownloadItem.getIndexItem(downloadThread).compareTo(progressItem) == 0) {
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
		};
	}

	public static void closeDashboard(MapActivity mapActivity) {
		mapActivity.getDashboard().hideDashboard(false);
	}
}
