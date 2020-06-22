package net.osmand.plus.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.OsmandSettings.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings.ListStringPreference;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.transport.TransportLinesMenu;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.wikipedia.WikipediaPoiMenu;
import net.osmand.render.RenderingRule;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.SunriseSunset;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.APP_PROFILES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CUSTOM_RENDERING_ITEMS_ID_SCHEME;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DETAILS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.FAVORITES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.GPX_FILES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.HIDE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_LANGUAGE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_MAGNIFIER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_MARKERS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_MODE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_RENDERING_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_SOURCE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_STYLE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.POI_OVERLAY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.POI_OVERLAY_LABELS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ROAD_STYLE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ROUTES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.SHOW_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TEXT_SIZE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.TRANSPORT_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.WIKIPEDIA_ID;
import static net.osmand.plus.ContextMenuAdapter.makeDeleteAction;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_DENSITY_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_SCHEME_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_WIDTH_ATTR;
import static net.osmand.plus.transport.TransportLinesMenu.RENDERING_CATEGORY_TRANSPORT;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_DETAILS;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_HIDE;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_ROUTES;

public class ConfigureMapMenu {
	private static final Log LOG = PlatformUtil.getLog(ConfigureMapMenu.class);
	public static final String HIKING_ROUTES_OSMC_ATTR = "hikingRoutesOSMC";
	public static final String CURRENT_TRACK_COLOR_ATTR = "currentTrackColor";
	public static final String CURRENT_TRACK_WIDTH_ATTR = "currentTrackWidth";
	public static final String COLOR_ATTR = "color";
	public static final String ROAD_STYLE_ATTR = "roadStyle";

	private int hikingRouteOSMCValue;
	private int selectedLanguageIndex;
	private boolean transliterateNames;

	public interface OnClickListener {
		void onClick();
	}

	public ContextMenuAdapter createListAdapter(final MapActivity ma) {
		OsmandApplication app = ma.getMyApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(APP_PROFILES_ID)
				.setTitleId(R.string.app_modes_choose, ma)
				.setLayout(R.layout.mode_toggles).createItem());
		adapter.setChangeAppModeListener(new OnClickListener() {
			@Override
			public void onClick() {
				ma.getDashboard().updateListAdapter(createListAdapter(ma));
			}
		});
		List<RenderingRuleProperty> customRules = getCustomRules(app,
				RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN, RENDERING_CATEGORY_TRANSPORT);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);
		createLayersItems(customRules, adapter, ma, themeRes, nightMode);
		createRenderingAttributeItems(customRules, adapter, ma, themeRes, nightMode);
		return adapter;
	}

	public static List<RenderingRuleProperty> getCustomRules(OsmandApplication app, String... skipCategories) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer == null) {
			return new ArrayList<>();
		}
		List<RenderingRuleProperty> customRules = new ArrayList<>();
		boolean useDepthContours = app.getResourceManager().hasDepthContours()
				&& (InAppPurchaseHelper.isSubscribedToLiveUpdates(app)
				|| InAppPurchaseHelper.isDepthContoursPurchased(app));
		for (RenderingRuleProperty p : renderer.PROPS.getCustomRules()) {
			if (useDepthContours || !"depthContours".equals(p.getAttrName())) {
				boolean skip = false;
				if (skipCategories != null) {
					for (String category : skipCategories) {
						if (category.equals(p.getCategory())) {
							skip = true;
							break;
						}
					}
				}
				if (!skip) {
					customRules.add(p);
				}
			}
		}
		return customRules;
	}

	private final class LayerMenuListener extends OnRowItemClick {
		private MapActivity ma;
		private ContextMenuAdapter cm;

		private LayerMenuListener(MapActivity ma, ContextMenuAdapter cm) {
			this.ma = ma;
			this.cm = cm;
		}

		private List<String> getAlreadySelectedGpx() {
			GpxSelectionHelper selectedGpxHelper = ma.getMyApplication().getSelectedGpxHelper();
			List<GpxSelectionHelper.SelectedGpxFile> selectedGpxFiles = selectedGpxHelper.getSelectedGPXFiles();

			List<String> files = new ArrayList<>();
			for (GpxSelectionHelper.SelectedGpxFile file : selectedGpxFiles) {
				files.add(file.getGpxFile().path);
			}
			if (selectedGpxFiles.isEmpty()) {
				Map<GPXUtilities.GPXFile, Long> fls = selectedGpxHelper.getSelectedGpxFilesBackUp();
				for(Map.Entry<GPXUtilities.GPXFile, Long> f : fls.entrySet()) {
					if(!Algorithms.isEmpty(f.getKey().path)) {
						File file = new File(f.getKey().path);
						if(file.exists() && !file.isDirectory()) {
							files.add(f.getKey().path);
						}
					}
				}
			}

			return files;
		}

		@Override
		public boolean onRowItemClick(final ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int pos) {
			if (itemId == R.string.layer_poi) {
				showPoiFilterDialog(adapter, adapter.getItem(pos));
				return false;
			} else if (itemId == R.string.layer_gpx_layer && cm.getItem(pos).getSelected()) {
				showGpxSelectionDialog(adapter, adapter.getItem(pos));
				return false;
			} else if (itemId == R.string.rendering_category_transport) {
				final ContextMenuItem item = adapter.getItem(pos);
				TransportLinesMenu.showTransportsDialog(ma, new CallbackWithObject<Boolean>() {
					@Override
					public boolean processResult(Boolean result) {
						if (item != null) {
							item.setSelected(result);
							item.setColorRes(result ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
							adapter.notifyDataSetChanged();
						}
						return true;
					}
				});
				boolean selected = TransportLinesMenu.isShowLines(ma.getMyApplication());
				if (!selected && item != null) {
					item.setSelected(true);
					item.setColorRes(R.color.osmand_orange);
					adapter.notifyDataSetChanged();
				}
				return false;
			} else {
				CompoundButton btn = (CompoundButton) view.findViewById(R.id.toggle_item);
				if (btn != null && btn.getVisibility() == View.VISIBLE) {
					btn.setChecked(!btn.isChecked());
					cm.getItem(pos).setColorRes(btn.isChecked() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					adapter.notifyDataSetChanged();
					return false;
				} else {
					return onContextMenuClick(adapter, itemId, pos, false, null);
				}
			}
		}

		@Override
		public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter, int itemId,
		                                  final int pos, boolean isChecked, int[] viewCoordinates) {
			final OsmandSettings settings = ma.getMyApplication().getSettings();
			final PoiFiltersHelper poiFiltersHelper = ma.getMyApplication().getPoiFilters();
			final ContextMenuItem item = cm.getItem(pos);
			if (item.getSelected() != null) {
				item.setColorRes(isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
			}
			if (itemId == R.string.layer_poi) {
				PoiUIFilter wiki = poiFiltersHelper.getTopWikiPoiFilter();
				poiFiltersHelper.clearSelectedPoiFilters(wiki);
				if (isChecked) {
					showPoiFilterDialog(adapter, adapter.getItem(pos));
				} else {
					adapter.getItem(pos).setDescription(
							poiFiltersHelper.getSelectedPoiFiltersName(wiki));
				}
			} else if (itemId == R.string.layer_amenity_label) {
				settings.SHOW_POI_LABEL.set(isChecked);
			} else if (itemId == R.string.shared_string_favorites) {
				settings.SHOW_FAVORITES.set(isChecked);
			} else if (itemId == R.string.layer_gpx_layer) {
				final GpxSelectionHelper selectedGpxHelper = ma.getMyApplication().getSelectedGpxHelper();
				if (selectedGpxHelper.isShowingAnyGpxFiles()) {
					selectedGpxHelper.clearAllGpxFilesToShow(true);
					adapter.getItem(pos).setDescription(selectedGpxHelper.getGpxDescription());
				} else {
					showGpxSelectionDialog(adapter, adapter.getItem(pos));
				}
			} else if (itemId == R.string.rendering_category_transport) {
				boolean selected = TransportLinesMenu.isShowLines(ma.getMyApplication());
				TransportLinesMenu.toggleTransportLines(ma, !selected, new CallbackWithObject<Boolean>() {
					@Override
					public boolean processResult(Boolean result) {
						item.setSelected(result);
						item.setColorRes(result ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						adapter.notifyDataSetChanged();
						return true;
					}
				});
			} else if (itemId == R.string.map_markers) {
				settings.SHOW_MAP_MARKERS.set(isChecked);
			} else if (itemId == R.string.layer_map) {
				if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
					Intent intent = new Intent(ma, PluginActivity.class);
					intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, OsmandRasterMapsPlugin.ID);
					ma.startActivity(intent);
				} else {
					ContextMenuItem it = adapter.getItem(pos);
					ma.getMapLayers().selectMapLayer(ma.getMapView(), it, adapter);
				}
			}
			adapter.notifyDataSetChanged();
			ma.getMapLayers().updateLayers(ma.getMapView());
			ma.getMapView().refreshMap();
			return false;
		}

		private void showGpxSelectionDialog(final ArrayAdapter<ContextMenuItem> adapter,
											final ContextMenuItem item) {
			AlertDialog dialog = ma.getMapLayers().showGPXFileLayer(getAlreadySelectedGpx(),
					ma.getMapView());
			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					OsmandApplication app = ma.getMyApplication();
					boolean selected = app.getSelectedGpxHelper().isShowingAnyGpxFiles();
					item.setSelected(selected);
					item.setDescription(app.getSelectedGpxHelper().getGpxDescription());
					item.setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
					adapter.notifyDataSetChanged();
				}
			});
		}

		protected void showPoiFilterDialog(final ArrayAdapter<ContextMenuItem> adapter,
										   final ContextMenuItem item) {
			final PoiFiltersHelper poiFiltersHelper = ma.getMyApplication().getPoiFilters();
			final PoiUIFilter wiki = poiFiltersHelper.getTopWikiPoiFilter();
			MapActivityLayers.DismissListener dismissListener =
					new MapActivityLayers.DismissListener() {
						@Override
						public void dismiss() {
							PoiFiltersHelper pf = ma.getMyApplication().getPoiFilters();
							boolean selected = pf.isShowingAnyPoi(wiki);
							item.setSelected(selected);
							item.setDescription(pf.getSelectedPoiFiltersName(wiki));
							item.setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
							adapter.notifyDataSetChanged();
						}
					};
			if (poiFiltersHelper.isShowingAnyPoi(wiki)) {
				ma.getMapLayers().showMultichoicePoiFilterDialog(ma.getMapView(),
						dismissListener);
			} else {
				ma.getMapLayers().showSingleChoicePoiFilterDialog(ma.getMapView(),
						dismissListener);
			}
		}
	}

	private void createLayersItems(List<RenderingRuleProperty> customRules, ContextMenuAdapter adapter,
	                               final MapActivity activity, final int themeRes, final boolean nightMode) {
		final OsmandApplication app = activity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final int selectedProfileColorRes = settings.getApplicationMode().getIconColorInfo().getColor(nightMode);
		final int selectedProfileColor = ContextCompat.getColor(app, selectedProfileColorRes);
		LayerMenuListener l = new LayerMenuListener(activity, adapter);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(SHOW_CATEGORY_ID)
				.setTitleId(R.string.shared_string_show, activity)
				.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());
		// String appMode = " [" + settings.getApplicationMode().toHumanString(view.getApplication()) +"] ";
		boolean selected = settings.SHOW_FAVORITES.get();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(FAVORITES_ID)
				.setTitleId(R.string.shared_string_favorites, activity)
				.setSelected(settings.SHOW_FAVORITES.get())
				.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_favorite)
				.setItemDeleteAction(makeDeleteAction(settings.SHOW_FAVORITES))
				.setListener(l)
				.createItem());
		PoiUIFilter wiki = app.getPoiFilters().getTopWikiPoiFilter();
		selected = app.getPoiFilters().isShowingAnyPoi(wiki);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(POI_OVERLAY_ID)
				.setTitleId(R.string.layer_poi, activity)
				.setSelected(selected)
				.setDescription(app.getPoiFilters().getSelectedPoiFiltersName(wiki))
				.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_info_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(l).createItem());
		selected = settings.SHOW_POI_LABEL.get();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(POI_OVERLAY_LABELS_ID)
				.setTitleId(R.string.layer_amenity_label, activity)
				.setSelected(settings.SHOW_POI_LABEL.get())
				.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_text_dark)
				.setItemDeleteAction(makeDeleteAction(settings.SHOW_POI_LABEL))
				.setListener(l).createItem());

		selected = TransportLinesMenu.isShowLines(app);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(TRANSPORT_ID)
				.setTitleId(R.string.rendering_category_transport, activity)
				.setIcon(R.drawable.ic_action_transport_bus)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setSelected(selected)
				.setColor(selected ? selectedProfileColorRes : ContextMenuItem.INVALID_ID)
				.setListener(l).createItem());

		selected = app.getSelectedGpxHelper().isShowingAnyGpxFiles();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(GPX_FILES_ID)
				.setTitleId(R.string.layer_gpx_layer, activity)
				.setSelected(app.getSelectedGpxHelper().isShowingAnyGpxFiles())
				.setDescription(app.getSelectedGpxHelper().getGpxDescription())
				.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_polygom_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(l).createItem());

		selected = settings.SHOW_MAP_MARKERS.get();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(MAP_MARKERS_ID)
				.setTitleId(R.string.map_markers, activity)
				.setSelected(selected)
				.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_flag)
				.setItemDeleteAction(makeDeleteAction(settings.SHOW_MAP_MARKERS))
				.setListener(l).createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(MAP_SOURCE_ID)
				.setTitleId(R.string.layer_map, activity)
				.setIcon(R.drawable.ic_world_globe_dark)
				.setDescription(settings.MAP_ONLINE_DATA.get() ? settings.MAP_TILE_SOURCES.get() : null)
				.setItemDeleteAction(makeDeleteAction(settings.MAP_ONLINE_DATA, settings.MAP_TILE_SOURCES))
				.setListener(l).createItem());

		OsmandPlugin.registerLayerContextMenu(activity.getMapView(), adapter, activity);
		boolean srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null
				&& !InAppPurchaseHelper.isSubscribedToLiveUpdates(app);
		if (srtmDisabled) {
			SRTMPlugin srtmPlugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
			if (srtmPlugin != null) {
				srtmPlugin.registerLayerContextMenuActions(activity.getMapView(), adapter, activity);
			}
		}
		app.getAidlApi().registerLayerContextMenu(adapter, activity);
	}

	public static void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		activity.getMapView().refreshMap(true);
	}

	private void createRenderingAttributeItems(List<RenderingRuleProperty> customRules,
											   final ContextMenuAdapter adapter, final MapActivity activity,
	                                           final int themeRes, final boolean nightMode) {
		final OsmandApplication app = activity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final int selectedProfileColorRes = settings.APPLICATION_MODE.get().getIconColorInfo().getColor(nightMode);
		final int selectedProfileColor = ContextCompat.getColor(app, selectedProfileColorRes);

		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_widget_map_rendering, activity)
				.setId(MAP_RENDERING_CATEGORY_ID)
				.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_widget_renderer, activity)
				.setId(MAP_STYLE_ID)
				.setDescription(getRenderDescr(activity)).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_map).setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
													  final int pos, boolean isChecked, int[] viewCoordinates) {
						new SelectMapStyleBottomSheetDialogFragment().show(activity.getSupportFragmentManager(),
								SelectMapStyleBottomSheetDialogFragment.TAG);
						return false;
					}
				})
				.setItemDeleteAction(makeDeleteAction(settings.RENDERER))
				.createItem());

		String description = "";
		SunriseSunset sunriseSunset = activity.getMyApplication().getDaynightHelper().getSunriseSunset();
		if (sunriseSunset != null && sunriseSunset.getSunrise() != null && sunriseSunset.getSunset() != null) {
			DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
			String sunriseTime = dateFormat.format(sunriseSunset.getSunrise());
			String sunsetTime = dateFormat.format(sunriseSunset.getSunset());
			OsmandSettings.DayNightMode dayNightMode = activity.getMyApplication().getSettings().DAYNIGHT_MODE.get();
			if (dayNightMode.isDay() || dayNightMode.isNight()) {
				if (sunriseSunset.isDaytime()) {
					description = String.format(app.getString(R.string.sunset_at), sunsetTime);
				} else {
					description = String.format(app.getString(R.string.sunrise_at), sunriseTime);
				}
			} else if (dayNightMode.isAuto() || dayNightMode.isSensor()) {
				description = String.format(app.getString(R.string.ltr_or_rtl_combine_via_slash), sunriseTime, sunsetTime);
			}
			description = String.format(app.getString(R.string.ltr_or_rtl_combine_via_bold_point), getDayNightDescr(activity), description);
		} else {
			description = getDayNightDescr(activity);
		}
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_mode, activity)
				.setId(MAP_MODE_ID)
				.setDescription(description)
				.setIcon(getDayNightIcon(activity)).setListener(new ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
													  final int pos, boolean isChecked, int[] viewCoordinates) {
						if (!AndroidUtils.isActivityNotDestroyed(activity)) {
							return false;
						}
						final OsmandMapTileView view = activity.getMapView();
						AlertDialog.Builder bld = new AlertDialog.Builder(new ContextThemeWrapper(view.getContext(), themeRes));
						bld.setTitle(R.string.daynight);
						final String[] items = new String[OsmandSettings.DayNightMode.values().length];
						for (int i = 0; i < items.length; i++) {
							items[i] = OsmandSettings.DayNightMode.values()[i].toHumanString(app);
						}
						int i = view.getSettings().DAYNIGHT_MODE.get().ordinal();
						bld.setNegativeButton(R.string.shared_string_dismiss, null);
						DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
								items, nightMode, i, app, selectedProfileColor, themeRes, new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										int which = (int) v.getTag();
										view.getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.values()[which]);
										refreshMapComplete(activity);
										activity.getDashboard().refreshContent(true);
										// adapter.getItem(pos).setDescription(s, getDayNightDescr(activity));
										// ad.notifyDataSetInvalidated();
									}
								}
						);
						bld.setAdapter(dialogAdapter, null);
						dialogAdapter.setDialog(bld.show());
						return false;
					}
				})
				.setItemDeleteAction(makeDeleteAction(settings.DAYNIGHT_MODE))
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(MAP_MAGNIFIER_ID)
				.setTitleId(R.string.map_magnifier, activity)
				.setDescription(
						String.format(Locale.UK, "%.0f",
								100f * activity.getMyApplication().getSettings().MAP_DENSITY.get())
								+ " %").setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_magnifier).setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
													  final int pos, boolean isChecked, int[] viewCoordinates) {
						if (!AndroidUtils.isActivityNotDestroyed(activity)) {
							return false;
						}
						final OsmandMapTileView view = activity.getMapView();
						final OsmandSettings.OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
						AlertDialog.Builder bld = new AlertDialog.Builder(new ContextThemeWrapper(view.getContext(), themeRes));
						int p = (int) (mapDensity.get() * 100);
						final TIntArrayList tlist = new TIntArrayList(new int[]{25, 33, 50, 75, 100, 125, 150, 200, 300, 400});
						final List<String> values = new ArrayList<>();
						int i = -1;
						for (int k = 0; k <= tlist.size(); k++) {
							final boolean end = k == tlist.size();
							if (i == -1) {
								if ((end || p < tlist.get(k))) {
									values.add(p + " %");
									i = k;
								} else if (p == tlist.get(k)) {
									i = k;
								}
							}
							if (k < tlist.size()) {
								values.add(tlist.get(k) + " %");
							}
						}
						if (values.size() != tlist.size()) {
							tlist.insert(i, p);
						}

						bld.setTitle(R.string.map_magnifier);
						bld.setNegativeButton(R.string.shared_string_dismiss, null);
						DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
								values.toArray(new String[values.size()]), nightMode, i, app, selectedProfileColor, themeRes, new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										int which = (int) v.getTag();
										int p = tlist.get(which);
										mapDensity.set(p / 100.0f);
										view.setComplexZoom(view.getZoom(), view.getSettingsMapDensity());
										MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
										if (mapContext != null) {
											mapContext.updateMapSettings();
										}
										adapter.getItem(pos).setDescription(
												String.format(Locale.UK, "%.0f", 100f * activity.getMyApplication()
														.getSettings().MAP_DENSITY.get())
														+ " %");
										ad.notifyDataSetInvalidated();
									}
								}
						);
						bld.setAdapter(dialogAdapter, null);
						dialogAdapter.setDialog(bld.show());
						return false;
					}
				})
				.setItemDeleteAction(makeDeleteAction(settings.MAP_DENSITY))
				.createItem());

		ContextMenuItem props;
		props = createRenderingProperty(customRules, adapter, activity, R.drawable.ic_action_intersection, ROAD_STYLE_ATTR, ROAD_STYLE_ID, app, selectedProfileColor, nightMode, themeRes);
		if (props != null) {
			adapter.addItem(props);
		}

		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.text_size, activity)
				.setId(TEXT_SIZE_ID)
				.setDescription(getScale(activity)).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_text_size).setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
													  final int pos, boolean isChecked, int[] viewCoordinates) {
						if (!AndroidUtils.isActivityNotDestroyed(activity)) {
							return false;
						}
						final OsmandMapTileView view = activity.getMapView();
						AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(view.getContext(), themeRes));
						// test old descr as title
						b.setTitle(R.string.text_size);
						final Float[] txtValues = new Float[]{0.33f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f};
						int selected = -1;
						final String[] txtNames = new String[txtValues.length];
						for (int i = 0; i < txtNames.length; i++) {
							txtNames[i] = (int) (txtValues[i] * 100) + " %";
							if (Math.abs(view.getSettings().TEXT_SCALE.get() - txtValues[i]) < 0.1f) {
								selected = i;
							}
						}
						DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
								txtNames, nightMode, selected, app, selectedProfileColor, themeRes, new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										int which = (int) v.getTag();
										view.getSettings().TEXT_SCALE.set(txtValues[which]);
										refreshMapComplete(activity);
										adapter.getItem(pos).setDescription(getScale(activity));
										ad.notifyDataSetInvalidated();
									}
								});
						b.setAdapter(dialogAdapter, null);
						b.setNegativeButton(R.string.shared_string_dismiss, null);
						dialogAdapter.setDialog(b.show());
						return false;
					}
				})
				.setItemDeleteAction(makeDeleteAction(settings.TEXT_SCALE))
				.createItem());

		String localeDescr = activity.getMyApplication().getSettings().MAP_PREFERRED_LOCALE.get();
		localeDescr = localeDescr == null || localeDescr.equals("") ? activity.getString(R.string.local_map_names)
				: localeDescr;
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_locale, activity)
				.setId(MAP_LANGUAGE_ID)
				.setDescription(localeDescr).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_language)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
													  final int pos, boolean isChecked, int[] viewCoordinates) {
						if (!AndroidUtils.isActivityNotDestroyed(activity)) {
							return false;
						}
						final OsmandMapTileView view = activity.getMapView();
						AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(view.getContext(), themeRes));

						b.setTitle(activity.getString(R.string.map_locale));

						final String[] txtIds = getSortedMapNamesIds(activity, mapNamesIds,
								getMapNamesValues(activity, mapNamesIds));
						final String[] txtValues = getMapNamesValues(activity, txtIds);
						int selected = -1;
						for (int i = 0; i < txtIds.length; i++) {
							if (view.getSettings().MAP_PREFERRED_LOCALE.get().equals(txtIds[i])) {
								selected = i;
								break;
							}
						}
						selectedLanguageIndex = selected;
						transliterateNames = view.getSettings().MAP_TRANSLITERATE_NAMES.get();

						final OnCheckedChangeListener translitChangdListener = new OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								transliterateNames = isChecked;
							}
						};

						final ArrayAdapter<CharSequence> singleChoiceAdapter = new ArrayAdapter<CharSequence>(new ContextThemeWrapper(view.getContext(), themeRes), R.layout.single_choice_switch_item, R.id.text1, txtValues) {
							@NonNull
							@Override
							public View getView(int position, View convertView, ViewGroup parent) {
								View v = super.getView(position, convertView, parent);
								AppCompatCheckedTextView checkedTextView = (AppCompatCheckedTextView) v.findViewById(R.id.text1);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
									UiUtilities.setupCompoundButtonDrawable(app, nightMode, selectedProfileColor, checkedTextView.getCheckMarkDrawable());
								}
								if (position == selectedLanguageIndex && position > 0) {
									checkedTextView.setChecked(true);
									v.findViewById(R.id.topDivider).setVisibility(View.VISIBLE);
									v.findViewById(R.id.bottomDivider).setVisibility(View.VISIBLE);
									v.findViewById(R.id.switchLayout).setVisibility(View.VISIBLE);
									TextView switchText = (TextView) v.findViewById(R.id.switchText);
									switchText.setText(activity.getString(R.string.translit_name_if_miss, txtValues[position]));
									SwitchCompat check = (SwitchCompat) v.findViewById(R.id.check);
									check.setChecked(transliterateNames);
									check.setOnCheckedChangeListener(translitChangdListener);
									UiUtilities.setupCompoundButton(nightMode, selectedProfileColor, check);
								} else {
									checkedTextView.setChecked(position == selectedLanguageIndex);
									v.findViewById(R.id.topDivider).setVisibility(View.GONE);
									v.findViewById(R.id.bottomDivider).setVisibility(View.GONE);
									v.findViewById(R.id.switchLayout).setVisibility(View.GONE);
								}
								return v;
							}
						};

						b.setAdapter(singleChoiceAdapter, null);
						b.setSingleChoiceItems(txtValues, selected, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								selectedLanguageIndex = which;
								((AlertDialog) dialog).getListView().setSelection(which);
								singleChoiceAdapter.notifyDataSetChanged();
							}
						});

						b.setNegativeButton(R.string.shared_string_cancel, null);
						b.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								view.getSettings().MAP_TRANSLITERATE_NAMES.set(selectedLanguageIndex > 0 && transliterateNames);
								AlertDialog dlg = (AlertDialog) dialog;
								int index = dlg.getListView().getCheckedItemPosition();
								view.getSettings().MAP_PREFERRED_LOCALE.set(
										txtIds[index]);
								refreshMapComplete(activity);
								String localeDescr = txtIds[index];
								localeDescr = localeDescr == null || localeDescr.equals("") ? activity
										.getString(R.string.local_map_names) : localeDescr;
								adapter.getItem(pos).setDescription(localeDescr);
								ad.notifyDataSetInvalidated();
							}
						});
						b.show();
						return false;
					}
				})
				.setItemDeleteAction(makeDeleteAction(settings.MAP_PREFERRED_LOCALE))
				.createItem());

		props = createProperties(customRules, null, R.string.rendering_category_details, R.drawable.ic_action_layers,
				UI_CATEGORY_DETAILS, null, adapter, activity, true, DETAILS_ID, themeRes, nightMode, selectedProfileColor);
		if (props != null) {
			adapter.addItem(props);
		}
		props = createProperties(customRules, null, R.string.rendering_category_hide, R.drawable.ic_action_hide,
				UI_CATEGORY_HIDE, null, adapter, activity, true, HIDE_ID, themeRes, nightMode, selectedProfileColor);
		if (props != null) {
			adapter.addItem(props);
		}
		List<RenderingRuleProperty> customRulesIncluded = new ArrayList<>();
		for (RenderingRuleProperty p : customRules) {
			if (p.getAttrName().equals(HIKING_ROUTES_OSMC_ATTR)) {
				customRulesIncluded.add(p);
				break;
			}
		}
		props = createProperties(customRules, customRulesIncluded, R.string.rendering_category_routes, R.drawable.ic_action_map_routes,
				UI_CATEGORY_ROUTES, null, adapter, activity, true, ROUTES_ID, themeRes, nightMode, selectedProfileColor);
		if (props != null) {
			adapter.addItem(props);
		}

		if (getCustomRenderingPropertiesSize(customRules) > 0) {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.rendering_category_others, activity)
					.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());
			createCustomRenderingProperties(adapter, activity, customRules, app, selectedProfileColor, nightMode, themeRes);
		}
	}

	public static String[] mapNamesIds = new String[]{"", "en", "af", "als", "ar", "az", "be", "ber", "bg", "bn", "bpy", "br", "bs", "ca", "ceb", "cs", "cy", "da", "de", "el", "eo", "es", "et", "eu", "fa", "fi", "fr", "fy", "ga", "gl", "he", "hi", "hsb", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "ka", "kab", "ko", "ku", "la", "lb", "lo", "lt", "lv", "mk", "ml", "mr", "ms", "nds", "new", "nl", "nn", "no", "nv", "oc", "os", "pl", "pms", "pt", "ro", "ru", "sc", "sh", "sk", "sl", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "vi", "vo", "zh"};

	public static String[] getSortedMapNamesIds(Context ctx, String[] ids, String[] values) {
		final Map<String, String> mp = new HashMap<>();
		for (int i = 0; i < ids.length; i++) {
			mp.put(ids[i], values[i]);
		}
		ArrayList<String> lst = new ArrayList<>(mp.keySet());
		final String systemLocale = ctx.getString(R.string.system_locale) + " (" + ctx.getString(R.string.system_locale_no_translate) + ")";
		final String englishLocale = ctx.getString(R.string.lang_en);
		Collections.sort(lst, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				int i1 = Algorithms.isEmpty(lhs) ? 0 : (lhs.equals("en") ? 1 : 2);
				int i2 = Algorithms.isEmpty(rhs) ? 0 : (rhs.equals("en") ? 1 : 2);
				if (i1 != i2) {
					return i1 < i2 ? -1 : 1;
				}
				i1 = systemLocale.equals(lhs) ? 0 : (englishLocale.equals(lhs) ? 1 : 2);
				i2 = systemLocale.equals(rhs) ? 0 : (englishLocale.equals(rhs) ? 1 : 2);
				if (i1 != i2) {
					return i1 < i2 ? -1 : 1;
				}
				return mp.get(lhs).compareTo(mp.get(rhs));
			}
		});
		return lst.toArray(new String[lst.size()]);
	}

	public static String[] getMapNamesValues(Context ctx, String[] ids) {
		String[] translates = new String[ids.length];
		for (int i = 0; i < translates.length; i++) {
			if (Algorithms.isEmpty(ids[i])) {
				translates[i] = ctx.getString(R.string.local_map_names);
			} else {
				translates[i] = ((OsmandApplication) ctx.getApplicationContext()).getLangTranslation(ids[i]);
			}
		}

		return translates;
	}

	private ContextMenuItem createProperties(List<RenderingRuleProperty> customRules,
											 final List<RenderingRuleProperty> customRulesIncluded,
											 @StringRes final int strId,
											 @DrawableRes final int icon,
											 final String category,
											 final ListStringPreference defaultSettings,
											 final ContextMenuAdapter adapter,
											 final MapActivity activity,
											 final boolean useDescription,
											 final String id,
											 final int themeRes,
											 final boolean nightMode,
											 @ColorInt final int selectedProfileColor) {

		final List<RenderingRuleProperty> ps = new ArrayList<>();
		final List<OsmandSettings.CommonPreference<Boolean>> prefs = new ArrayList<>();
		Iterator<RenderingRuleProperty> it = customRules.iterator();

		while (it.hasNext()) {
			RenderingRuleProperty p = it.next();
			if (category.equals(p.getCategory()) && p.isBoolean()) {
				ps.add(p);
				final OsmandSettings.CommonPreference<Boolean> pref = activity.getMyApplication().getSettings()
						.getCustomRenderBooleanProperty(p.getAttrName());
				prefs.add(pref);
				it.remove();
			}
		}
		if (prefs.size() > 0) {
			final List<OsmandSettings.CommonPreference<String>> includedPrefs = new ArrayList<>();
			if (customRulesIncluded != null) {
				for (RenderingRuleProperty p : customRulesIncluded) {
					if (!p.isBoolean()) {
						final OsmandSettings.CommonPreference<String> pref = activity.getMyApplication().getSettings()
								.getCustomRenderProperty(p.getAttrName());
						includedPrefs.add(pref);
					}
				}
			}
			final ItemClickListener clickListener = new ContextMenuAdapter.ItemClickListener() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> a, int itemId, int pos,
												  boolean isChecked, int[] viewCoordinates) {
					if (!isChecked && !useDescription) {
						if (defaultSettings != null) {
							defaultSettings.set("");
							for (int i = 0; i < prefs.size(); i++) {
								if (prefs.get(i).get()) {
									defaultSettings.addValue(prefs.get(i).getId());
								}
							}
						}
						for (int i = 0; i < prefs.size(); i++) {
							prefs.get(i).set(false);
						}
						adapter.getItem(pos).setColorRes(ContextMenuItem.INVALID_ID);
						a.notifyDataSetInvalidated();
						refreshMapComplete(activity);
						activity.getMapLayers().updateLayers(activity.getMapView());
					} else {
						if (UI_CATEGORY_DETAILS.equals(category)) {
							DetailsBottomSheet.showInstance(activity.getSupportFragmentManager(), ps, prefs, a, adapter, pos);
						} else {
							showPreferencesDialog(adapter, a, pos, activity, activity.getString(strId), ps, prefs,
									useDescription, defaultSettings, true, customRulesIncluded, themeRes, nightMode, selectedProfileColor);
						}
					}
					return false;
				}

			};
			ContextMenuItem.ItemBuilder builder = new ContextMenuItem.ItemBuilder().setTitleId(strId, activity)
					.setId(id)
					.setIcon(icon).setListener(clickListener);
			boolean selected = false;
			for (OsmandSettings.CommonPreference<Boolean> p : prefs) {
				if (p.get()) {
					selected = true;
					break;
				}
			}
			if (!selected &&  includedPrefs.size() > 0) {
				for (OsmandSettings.CommonPreference<String> p : includedPrefs) {
					if (!Algorithms.isEmpty(p.get())) {
						selected = true;
						break;
					}
				}
			}
			builder.setColor(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
			if (useDescription) {
				final String descr = getDescription(prefs, includedPrefs);
				builder.setDescription(descr);
				builder.setLayout(R.layout.list_item_single_line_descrition_narrow);
			} else {
				builder.setListener(new OnRowItemClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> a, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						return clickListener.onContextMenuClick(a, itemId, pos, isChecked, null);
					}

					@Override
					public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> a, View view, int itemId,
												  int pos) {
						showPreferencesDialog(adapter, a, pos, activity, activity.getString(strId), ps, prefs,
								useDescription, defaultSettings, false, customRulesIncluded, themeRes, nightMode, selectedProfileColor);
						return false;
					}
				});
				builder.setSecondaryIcon(R.drawable.ic_action_additional_option);
				builder.setSelected(selected);
			}
			builder.setItemDeleteAction(makeDeleteAction(prefs));
			return builder.createItem();
//			createCustomRenderingProperties(adapter, activity, ps);
		}
		return null;
	}

	protected String getDescription(final List<OsmandSettings.CommonPreference<Boolean>> prefs,
									final List<OsmandSettings.CommonPreference<String>> includedPrefs) {
		int count = 0;
		int enabled = 0;
		for (OsmandSettings.CommonPreference<Boolean> p : prefs) {
			count++;
			if (p.get()) {
				enabled++;
			}
		}
		for (OsmandSettings.CommonPreference<String> p : includedPrefs) {
			count++;
			if (!Algorithms.isEmpty(p.get())) {
				enabled++;
			}
		}
		return enabled + "/" + count;
	}

	protected void showPreferencesDialog(final ContextMenuAdapter adapter,
										 final ArrayAdapter<?> a,
										 final int pos,
										 final MapActivity activity,
										 String category,
										 List<RenderingRuleProperty> ps,
										 final List<CommonPreference<Boolean>> prefs,
										 final boolean useDescription,
										 ListStringPreference defaultSettings,
										 boolean useDefault,
										 final List<RenderingRuleProperty> customRulesIncluded,
	                                     final int themeRes,
	                                     final boolean nightMode,
	                                     @ColorInt final int selectedProfileColor) {
		if (!AndroidUtils.isActivityNotDestroyed(activity)) {
			return;
		}
		AlertDialog.Builder bld = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		boolean[] checkedItems = new boolean[prefs.size()];
		final boolean[] tempPrefs = new boolean[prefs.size()];
		for (int i = 0; i < prefs.size(); i++) {
			tempPrefs[i] = checkedItems[i] = defaultSettings != null && useDefault ?
					defaultSettings.containsValue(prefs.get(i).getId()) : prefs.get(i).get();
		}
		final String[] vals = new String[ps.size()];
		for (int i = 0; i < ps.size(); i++) {
			RenderingRuleProperty p = ps.get(i);
			String propertyName = SettingsActivity.getStringPropertyName(activity, p.getAttrName(),
					p.getName());
			vals[i] = propertyName;
		}

		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createMultiChoiceAdapter(
				vals, nightMode, checkedItems, activity.getMyApplication(), selectedProfileColor, themeRes, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						int which = (int) v.getTag();
						tempPrefs[which] = !tempPrefs[which];
					}
				}
		);
		bld.setAdapter(dialogAdapter, null);

		bld.setTitle(category);

		bld.setNegativeButton(R.string.shared_string_cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				boolean selected = false;
				for (int i = 0; i < prefs.size(); i++) {
					selected |= prefs.get(i).get();
				}
				adapter.getItem(pos).setSelected(selected);
				adapter.getItem(pos).setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				a.notifyDataSetInvalidated();
			}
		});

		bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				boolean selected = false;
				for (int i = 0; i < prefs.size(); i++) {
					prefs.get(i).set(tempPrefs[i]);
					selected |= tempPrefs[i];
				}
				final List<OsmandSettings.CommonPreference<String>> includedPrefs = new ArrayList<>();
				if (customRulesIncluded != null) {
					for (RenderingRuleProperty p : customRulesIncluded) {
						if (p.getAttrName().equals(HIKING_ROUTES_OSMC_ATTR)) {
							final OsmandSettings.CommonPreference<String> pref = activity.getMyApplication().getSettings()
									.getCustomRenderProperty(p.getAttrName());
							includedPrefs.add(pref);
							if (hikingRouteOSMCValue == 0) {
								pref.set("");
							} else {
								pref.set(p.getPossibleValues()[hikingRouteOSMCValue - 1]);
								selected = true;
							}
							break;
						}
					}
				}
				if (adapter != null) {
					if (useDescription) {
						adapter.getItem(pos).setDescription(getDescription(prefs, includedPrefs));
					} else {
						adapter.getItem(pos).setSelected(selected);
					}
					adapter.getItem(pos).setColorRes(selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				}
				a.notifyDataSetInvalidated();
				refreshMapComplete(activity);
				activity.getMapLayers().updateLayers(activity.getMapView());
			}
		});

		final AlertDialog dialog = bld.create();

		dialogAdapter.setDialog(dialog);

		if (customRulesIncluded != null) {
			for (RenderingRuleProperty p : customRulesIncluded) {
				if (!p.isBoolean()) {
					final OsmandSettings.CommonPreference<String> pref = activity.getMyApplication().getSettings()
							.getCustomRenderProperty(p.getAttrName());

					View spinnerView = View.inflate(new ContextThemeWrapper(activity, themeRes), R.layout.spinner_rule_layout, null);
					TextView title = (TextView) spinnerView.findViewById(R.id.title);
					final Spinner spinner = (Spinner) spinnerView.findViewById(R.id.spinner);
					TextView description = (TextView) spinnerView.findViewById(R.id.description);

					String propertyName = SettingsActivity.getStringPropertyName(activity, p.getAttrName(),
							p.getName());
					String propertyDescr = SettingsActivity.getStringPropertyDescription(activity,
							p.getAttrName(), p.getName());

					title.setText(propertyName);
					description.setText(propertyDescr);

					int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());
					if (i >= 0) {
						i++;
					} else if (Algorithms.isEmpty(pref.get())) {
						i = 0;
					}

					String[] possibleValuesString = new String[p.getPossibleValues().length + 1];
					possibleValuesString[0] = SettingsActivity.getStringPropertyValue(activity,
							p.getDefaultValueDescription());

					for (int j = 0; j < p.getPossibleValues().length; j++) {
						possibleValuesString[j + 1] = SettingsActivity.getStringPropertyValue(activity,
								p.getPossibleValues()[j]);
					}

					StringSpinnerArrayAdapter arrayAdapter = new StringSpinnerArrayAdapter(activity, nightMode);
					for (String val : possibleValuesString) {
						arrayAdapter.add(val);
					}
					spinner.setAdapter(arrayAdapter);
					hikingRouteOSMCValue = i;
					spinner.setSelection(i);
					spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
							hikingRouteOSMCValue = position;
						}

						@Override
						public void onNothingSelected(AdapterView<?> parent) {
						}
					});

					dialog.getListView().addFooterView(spinnerView);
				}
			}
		}
		dialog.show();
	}

	protected String getRenderDescr(final MapActivity activity) {
		RendererRegistry rr = activity.getMyApplication().getRendererRegistry();
		RenderingRulesStorage storage = rr.getCurrentSelectedRenderer();
		if (storage == null) {
			return "";
		}
		String translation = RendererRegistry.getTranslatedRendererName(activity, storage.getName());
		return translation == null ? storage.getName() : translation;
	}

	protected String getDayNightDescr(final MapActivity activity) {
		return activity.getMyApplication().getSettings().DAYNIGHT_MODE.get().toHumanString(activity);
	}

	@DrawableRes
	protected int getDayNightIcon(final MapActivity activity) {
		return activity.getMyApplication().getSettings().DAYNIGHT_MODE.get().getIconRes();
	}

	protected String getScale(final MapActivity activity) {
		int scale = (int) (activity.getMyApplication().getSettings().TEXT_SCALE.get() * 100);
		return scale + " %";
	}

	private boolean isPropertyAccepted(RenderingRuleProperty p) {
		return !(p.getAttrName().equals(RenderingRuleStorageProperties.A_APP_MODE) ||
				p.getAttrName().equals(RenderingRuleStorageProperties.A_BASE_APP_MODE) ||
				p.getAttrName().equals(RenderingRuleStorageProperties.A_ENGINE_V1) ||
				p.getAttrName().equals(HIKING_ROUTES_OSMC_ATTR) ||
				p.getAttrName().equals(ROAD_STYLE_ATTR) ||
				p.getAttrName().equals(CONTOUR_WIDTH_ATTR) ||
				p.getAttrName().equals(CONTOUR_DENSITY_ATTR) ||
				p.getAttrName().equals(CONTOUR_LINES_ATTR) ||
				p.getAttrName().equals(CONTOUR_LINES_SCHEME_ATTR) ||
				p.getAttrName().equals(CURRENT_TRACK_COLOR_ATTR) ||
				p.getAttrName().equals(CURRENT_TRACK_WIDTH_ATTR));
	}

	private void createCustomRenderingProperties(final ContextMenuAdapter adapter, final MapActivity activity,
												 List<RenderingRuleProperty> customRules, final OsmandApplication app, final int currentProfileColor,
												 final boolean nightMode, final int themeRes) {
		for (final RenderingRuleProperty p : customRules) {
			if (isPropertyAccepted(p)) {
				adapter.addItem(createRenderingProperty(adapter, activity, 0, p, CUSTOM_RENDERING_ITEMS_ID_SCHEME + p.getName(), app, currentProfileColor, nightMode, themeRes));
			}
		}
	}

	private int getCustomRenderingPropertiesSize(List<RenderingRuleProperty> customRules) {
		int size = 0;
		for (final RenderingRuleProperty p : customRules) {
			if (isPropertyAccepted(p)) {
				size++;
			}
		}
		return size;
	}

	private ContextMenuItem createRenderingProperty(final List<RenderingRuleProperty> customRules,
													final ContextMenuAdapter adapter, final MapActivity activity,
													@DrawableRes final int icon, final String attrName, String id,
	                                                final OsmandApplication app, final int currentProfileColor, final boolean nightMode, final int themeRes) {
		for (final RenderingRuleProperty p : customRules) {
			if (p.getAttrName().equals(attrName)) {
				return createRenderingProperty(adapter, activity, icon, p, id, app, currentProfileColor, nightMode, themeRes);
			}
		}
		return null;
	}

	private ContextMenuItem createRenderingProperty(final ContextMenuAdapter adapter, final MapActivity activity,
										 @DrawableRes final int icon, final RenderingRuleProperty p, final String id,
	                                                final OsmandApplication app, final int currentProfileColor, final boolean nightMode, final int themeRes) {
		final OsmandMapTileView view = activity.getMapView();
		String propertyName = SettingsActivity.getStringPropertyName(view.getContext(), p.getAttrName(),
				p.getName());

		final String propertyDescr = SettingsActivity.getStringPropertyDescription(view.getContext(),
				p.getAttrName(), p.getName());
		if (p.isBoolean()) {
			final OsmandSettings.CommonPreference<Boolean> pref = view.getApplication().getSettings()
					.getCustomRenderBooleanProperty(p.getAttrName());
			return ContextMenuItem.createBuilder(propertyName)
					.setId(id)
					.setListener(new ContextMenuAdapter.ItemClickListener() {

						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
							pref.set(!pref.get());
							refreshMapComplete(activity);
							return false;
						}
					})
					.setSelected(pref.get())
					.createItem();
		} else {
			final OsmandSettings.CommonPreference<String> pref = view.getApplication().getSettings()
					.getCustomRenderProperty(p.getAttrName());
			final String descr;
			if (!Algorithms.isEmpty(pref.get())) {
				descr = SettingsActivity.getStringPropertyValue(activity, pref.get());
			} else {
				descr = SettingsActivity.getStringPropertyValue(view.getContext(),
						p.getDefaultValueDescription());
			}
			ContextMenuItem.ItemBuilder builder = ContextMenuItem.createBuilder(propertyName)
					.setId(id)
					.setListener(new ContextMenuAdapter.ItemClickListener() {

						@Override
						public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad,
						                                  final int itemId, final int pos, boolean isChecked, int[] viewCoordinates) {
							if (!AndroidUtils.isActivityNotDestroyed(activity)) {
								return false;
							}
							AlertDialog.Builder b = new AlertDialog.Builder(new ContextThemeWrapper(view.getContext(), themeRes));
							// test old descr as title
							b.setTitle(propertyDescr);

							int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());
							if (i >= 0) {
								i++;
							} else if (Algorithms.isEmpty(pref.get())) {
								i = 0;
							}

							String[] possibleValuesString = new String[p.getPossibleValues().length + 1];
							possibleValuesString[0] = SettingsActivity.getStringPropertyValue(view.getContext(),
									p.getDefaultValueDescription());

							for (int j = 0; j < p.getPossibleValues().length; j++) {
								possibleValuesString[j + 1] = SettingsActivity.getStringPropertyValue(view.getContext(),
										p.getPossibleValues()[j]);
							}
							DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
									possibleValuesString, nightMode, i, app, currentProfileColor, themeRes, new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											int which = (int) v.getTag();
											if (which == 0) {
												pref.set("");
											} else {
												pref.set(p.getPossibleValues()[which - 1]);
											}
											refreshMapComplete(activity);
											String description = SettingsActivity.getStringPropertyValue(activity, pref.get());
											adapter.getItem(pos).setDescription(description);
										}
									}
							);
							b.setNegativeButton(R.string.shared_string_dismiss, null);
							b.setAdapter(dialogAdapter, null);
							dialogAdapter.setDialog(b.show());
							return false;
						}
					})
					.setDescription(descr)
					.setItemDeleteAction(makeDeleteAction(pref))
					.setLayout(R.layout.list_item_single_line_descrition_narrow);
			if (icon != 0) {
				builder.setIcon(icon);
			}

			return builder.createItem();
		}
	}

	private class StringSpinnerArrayAdapter extends ArrayAdapter<String> {

		private boolean nightMode;

		public StringSpinnerArrayAdapter(Context context, boolean nightMode) {
			super(context, android.R.layout.simple_spinner_item);
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			OsmandApplication app = (OsmandApplication )getContext().getApplicationContext();
			this.nightMode = nightMode;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView label = (TextView) super.getView(position, convertView, parent);

			String text = getItem(position);
			label.setText(text);
			label.setTextColor(nightMode ?
					ContextCompat.getColorStateList(getContext(), R.color.text_color_primary_dark) : ContextCompat.getColorStateList(getContext(), R.color.text_color_primary_light));
			return label;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			TextView label = (TextView) super.getDropDownView(position, convertView, parent);

			String text = getItem(position);
			label.setText(text);
			label.setTextColor(nightMode ?
						ContextCompat.getColorStateList(getContext(), R.color.text_color_primary_dark) : ContextCompat.getColorStateList(getContext(), R.color.text_color_primary_light));

			return label;
		}
	}

	public static class GpxAppearanceAdapter extends ArrayAdapter<AppearanceListItem> {

		private OsmandApplication app;
		private int currentColor;
		private GpxAppearanceAdapterType adapterType = GpxAppearanceAdapterType.TRACK_WIDTH_COLOR;

		public enum GpxAppearanceAdapterType {
			TRACK_WIDTH,
			TRACK_COLOR,
			TRACK_WIDTH_COLOR
		}

		public GpxAppearanceAdapter(Context context, String currentColorValue, GpxAppearanceAdapterType adapterType) {
			super(context, R.layout.rendering_prop_menu_item);
			this.app = (OsmandApplication) getContext().getApplicationContext();
			this.adapterType = adapterType;
			RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
			this.currentColor = parseTrackColor(renderer, currentColorValue);
			init();
		}

		public GpxAppearanceAdapter(Context context, int currentColor, GpxAppearanceAdapterType adapterType) {
			super(context, R.layout.rendering_prop_menu_item);
			this.app = (OsmandApplication) getContext().getApplicationContext();
			this.adapterType = adapterType;
			this.currentColor = currentColor;
			init();
		}

		public void init() {
			RenderingRuleProperty trackWidthProp = null;
			RenderingRuleProperty trackColorProp = null;
			RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
			if (renderer != null) {
				if (adapterType == GpxAppearanceAdapterType.TRACK_WIDTH || adapterType == GpxAppearanceAdapterType.TRACK_WIDTH_COLOR) {
					trackWidthProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_WIDTH_ATTR);
				}
				if (adapterType == GpxAppearanceAdapterType.TRACK_COLOR || adapterType == GpxAppearanceAdapterType.TRACK_WIDTH_COLOR) {
					trackColorProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_COLOR_ATTR);
				}
			}

			if (trackWidthProp != null) {
				AppearanceListItem item = new AppearanceListItem(CURRENT_TRACK_WIDTH_ATTR, "",
						SettingsActivity.getStringPropertyValue(getContext(), trackWidthProp.getDefaultValueDescription()));
				add(item);
				for (int j = 0; j < trackWidthProp.getPossibleValues().length; j++) {
					item = new AppearanceListItem(CURRENT_TRACK_WIDTH_ATTR,
							trackWidthProp.getPossibleValues()[j],
							SettingsActivity.getStringPropertyValue(getContext(), trackWidthProp.getPossibleValues()[j]));
					add(item);
				}
				item.setLastItem(true);
			}
			if (trackColorProp != null) {
				AppearanceListItem item = new AppearanceListItem(CURRENT_TRACK_COLOR_ATTR, "",
						SettingsActivity.getStringPropertyValue(getContext(), trackColorProp.getDefaultValueDescription()),
						parseTrackColor(renderer, ""));
				add(item);
				for (int j = 0; j < trackColorProp.getPossibleValues().length; j++) {
					item = new AppearanceListItem(CURRENT_TRACK_COLOR_ATTR,
							trackColorProp.getPossibleValues()[j],
							SettingsActivity.getStringPropertyValue(getContext(), trackColorProp.getPossibleValues()[j]),
							parseTrackColor(renderer, trackColorProp.getPossibleValues()[j]));
					add(item);
				}
				item.setLastItem(true);
			}
		}

		public static int parseTrackColor(RenderingRulesStorage renderer, String colorName) {
			int defaultColor = -1;
			RenderingRule gpxRule = null;
			if (renderer != null) {
				gpxRule = renderer.getRenderingAttributeRule("gpx");
			}
			if (gpxRule != null && gpxRule.getIfElseChildren().size() > 0) {
				List<RenderingRule> rules = gpxRule.getIfElseChildren().get(0).getIfElseChildren();
				for (RenderingRule r : rules) {
					String cName = r.getStringPropertyValue(CURRENT_TRACK_COLOR_ATTR);
					if (!Algorithms.isEmpty(cName) && cName.equals(colorName)) {
						return r.getIntPropertyValue(COLOR_ATTR);
					}
					if (cName == null && defaultColor == -1) {
						defaultColor = r.getIntPropertyValue(COLOR_ATTR);
					}
				}
			}
			return defaultColor;
		}

		public static String parseTrackColorName(RenderingRulesStorage renderer, int color) {
			RenderingRule gpxRule = null;
			if (renderer != null) {
				gpxRule = renderer.getRenderingAttributeRule("gpx");
			}
			if (gpxRule != null && gpxRule.getIfElseChildren().size() > 0) {
				List<RenderingRule> rules = gpxRule.getIfElseChildren().get(0).getIfElseChildren();
				for (RenderingRule r : rules) {
					String cName = r.getStringPropertyValue(CURRENT_TRACK_COLOR_ATTR);
					if (!Algorithms.isEmpty(cName) && color == r.getIntPropertyValue(COLOR_ATTR)) {
						return cName;
					}
				}
			}
			return Algorithms.colorToString(color);
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			AppearanceListItem item = getItem(position);
			View v = convertView;
			if (v == null) {
				v = LayoutInflater.from(getContext()).inflate(R.layout.rendering_prop_menu_item, null);
			}
			if (item != null) {
				TextView textView = (TextView) v.findViewById(R.id.text1);
				textView.setText(item.localizedValue);
				if (item.attrName == CURRENT_TRACK_WIDTH_ATTR) {
					int iconId;
					if (item.value.equals("bold")) {
						iconId = R.drawable.ic_action_gpx_width_bold;
					} else if (item.value.equals("medium")) {
						iconId = R.drawable.ic_action_gpx_width_medium;
					} else {
						iconId = R.drawable.ic_action_gpx_width_thin;
					}
					textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
							app.getUIUtilities().getPaintedIcon(iconId, currentColor), null);
				} else {
					if (item.color == -1) {
						textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
								app.getUIUtilities().getThemedIcon(R.drawable.ic_action_circle), null);
					} else {
						textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
								app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, item.color), null);
					}
				}
				textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(getContext(), 10f));
				v.findViewById(R.id.divider).setVisibility(item.lastItem
						&& position < getCount() - 1 ? View.VISIBLE : View.GONE);
			}
			return v;
		}
	}

	public static class AppearanceListItem {
		private String attrName;
		private String value;
		private String localizedValue;
		private int color;
		private boolean lastItem;

		public AppearanceListItem(String attrName, String value, String localizedValue) {
			this.attrName = attrName;
			this.value = value;
			this.localizedValue = localizedValue;
		}

		public AppearanceListItem(String attrName, String value, String localizedValue, int color) {
			this.attrName = attrName;
			this.value = value;
			this.localizedValue = localizedValue;
			this.color = color;
		}

		public String getAttrName() {
			return attrName;
		}

		public String getValue() {
			return value;
		}

		public String getLocalizedValue() {
			return localizedValue;
		}

		public int getColor() {
			return color;
		}

		public boolean isLastItem() {
			return lastItem;
		}

		public void setLastItem(boolean lastItem) {
			this.lastItem = lastItem;
		}
	}
}