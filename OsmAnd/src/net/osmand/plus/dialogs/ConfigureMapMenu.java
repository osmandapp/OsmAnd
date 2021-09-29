package net.osmand.plus.dialogs;

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
import static net.osmand.plus.ContextMenuAdapter.makeDeleteAction;
import static net.osmand.plus.ContextMenuItem.INVALID_ID;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_DENSITY_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_LINES_SCHEME_ATTR;
import static net.osmand.plus.srtmplugin.SRTMPlugin.CONTOUR_WIDTH_ATTR;
import static net.osmand.plus.transport.TransportLinesMenu.RENDERING_CATEGORY_TRANSPORT;
import static net.osmand.render.RenderingRuleStorageProperties.A_APP_MODE;
import static net.osmand.render.RenderingRuleStorageProperties.A_BASE_APP_MODE;
import static net.osmand.render.RenderingRuleStorageProperties.A_ENGINE_V1;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_DETAILS;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_HIDE;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_ROUTES;

import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuAdapter.OnRowItemClick;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.ContextMenuItem.ItemBuilder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.helpers.enums.DayNightMode;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.transport.TransportLinesMenu;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.SunriseSunset;

import org.apache.commons.logging.Log;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigureMapMenu {

	private static final Log LOG = PlatformUtil.getLog(ConfigureMapMenu.class);

	public static final String HORSE_ROUTES_ATTR = "horseRoutes";
	public static final String PISTE_ROUTES_ATTR = "pisteRoutes";
	public static final String ALPINE_HIKING_ATTR = "alpineHiking";
	public static final String SHOW_MTB_ROUTES_ATTR = "showMtbRoutes";
	public static final String SHOW_CYCLE_ROUTES_ATTR = "showCycleRoutes";
	public static final String WHITE_WATER_SPORTS_ATTR = "whiteWaterSports";
	public static final String HIKING_ROUTES_OSMC_ATTR = "hikingRoutesOSMC";
	public static final String CYCLE_NODE_NETWORK_ROUTES_ATTR = "showCycleNodeNetworkRoutes";

	public static final String CURRENT_TRACK_COLOR_ATTR = "currentTrackColor";
	public static final String CURRENT_TRACK_WIDTH_ATTR = "currentTrackWidth";
	public static final String COLOR_ATTR = "color";
	public static final String ROAD_STYLE_ATTR = "roadStyle";
	public static final String TRAVEL_ROUTES = "travel_routes";

	public interface OnClickListener {
		void onClick();
	}

	public ContextMenuAdapter createListAdapter(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();

		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(APP_PROFILES_ID)
				.setTitleId(R.string.app_modes_choose, mapActivity)
				.setLayout(R.layout.mode_toggles).createItem());

		List<RenderingRuleProperty> customRules = ConfigureMapUtils.getCustomRules(app,
				UI_CATEGORY_HIDDEN, RENDERING_CATEGORY_TRANSPORT);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);
		createLayersItems(mapActivity, adapter, nightMode);
		createRouteAttributeItems(customRules, adapter, mapActivity, nightMode);
		createRenderingAttributeItems(customRules, adapter, mapActivity, nightMode);
		return adapter;
	}

	@StyleRes
	protected static int getThemeRes(boolean nightMode) {
		return nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	private void createLayersItems(@NonNull MapActivity activity, @NonNull ContextMenuAdapter adapter, boolean nightMode) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		int selectedProfileColor = settings.getApplicationMode().getProfileColor(nightMode);
		MapLayerMenuListener listener = new MapLayerMenuListener(activity, adapter);

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(SHOW_CATEGORY_ID)
				.setTitleId(R.string.shared_string_show, activity)
				.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());

		boolean selected = settings.SHOW_FAVORITES.get();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(FAVORITES_ID)
				.setTitleId(R.string.shared_string_favorites, activity)
				.setSelected(settings.SHOW_FAVORITES.get())
				.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
				.setIcon(R.drawable.ic_action_favorite)
				.setItemDeleteAction(makeDeleteAction(settings.SHOW_FAVORITES))
				.setListener(listener)
				.createItem());

		ResourceManager resourceManager = app.getResourceManager();
		boolean hasPoiData = !Algorithms.isEmpty(resourceManager.getAmenityRepositories())
				|| !Algorithms.isEmpty(resourceManager.getTravelRepositories());
		if (hasPoiData) {
			PoiUIFilter wiki = app.getPoiFilters().getTopWikiPoiFilter();
			selected = app.getPoiFilters().isShowingAnyPoi(wiki);
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setId(POI_OVERLAY_ID)
					.setTitleId(R.string.layer_poi, activity)
					.setSelected(selected)
					.setDescription(app.getPoiFilters().getSelectedPoiFiltersName(wiki))
					.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
					.setIcon(R.drawable.ic_action_info_dark)
					.setSecondaryIcon(R.drawable.ic_action_additional_option)
					.setListener(listener).createItem());
		}
		selected = settings.SHOW_POI_LABEL.get();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(POI_OVERLAY_LABELS_ID)
				.setTitleId(R.string.layer_amenity_label, activity)
				.setSelected(settings.SHOW_POI_LABEL.get())
				.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
				.setIcon(R.drawable.ic_action_text_dark)
				.setItemDeleteAction(makeDeleteAction(settings.SHOW_POI_LABEL))
				.setListener(listener).createItem());

		selected = TransportLinesMenu.isShowLines(app);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(TRANSPORT_ID)
				.setTitleId(R.string.rendering_category_transport, activity)
				.setIcon(R.drawable.ic_action_transport_bus)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setSelected(selected)
				.setColor(selected ? selectedProfileColor : null)
				.setListener(listener).createItem());

		selected = app.getSelectedGpxHelper().isShowingAnyGpxFiles();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(GPX_FILES_ID)
				.setTitleId(R.string.layer_gpx_layer, activity)
				.setSelected(app.getSelectedGpxHelper().isShowingAnyGpxFiles())
				.setDescription(app.getSelectedGpxHelper().getGpxDescription())
				.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
				.setIcon(R.drawable.ic_action_polygom_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(listener).createItem());

		selected = settings.SHOW_MAP_MARKERS.get();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(MAP_MARKERS_ID)
				.setTitleId(R.string.map_markers, activity)
				.setSelected(selected)
				.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
				.setIcon(R.drawable.ic_action_flag)
				.setItemDeleteAction(makeDeleteAction(settings.SHOW_MAP_MARKERS))
				.setListener(listener).createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(MAP_SOURCE_ID)
				.setTitleId(R.string.layer_map, activity)
				.setIcon(R.drawable.ic_world_globe_dark)
				.setDescription(settings.MAP_ONLINE_DATA.get() ? settings.MAP_TILE_SOURCES.get().replace(IndexConstants.SQLITE_EXT, "") : null)
				.setItemDeleteAction(makeDeleteAction(settings.MAP_ONLINE_DATA, settings.MAP_TILE_SOURCES))
				.setListener(listener).createItem());

		OsmandPlugin.registerLayerContextMenu(adapter, activity);
		app.getAidlApi().registerLayerContextMenu(adapter, activity);
	}

	private void createRouteAttributeItems(List<RenderingRuleProperty> customRules,
										   ContextMenuAdapter adapter, MapActivity activity,
										   boolean nightMode) {
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.rendering_category_routes, activity)
				.setId(ROUTES_ID)
				.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());

		for (String attrName : getRoutesDefaultAttrs().keySet()) {
			RenderingRuleProperty property = getPropertyForAttr(customRules, attrName);
			if (SHOW_CYCLE_ROUTES_ATTR.equals(attrName)) {
				adapter.addItem(createCycleRoutesItem(activity, attrName, property, nightMode));
				customRules.remove(getPropertyForAttr(customRules, CYCLE_NODE_NETWORK_ROUTES_ATTR));
			} else if (HIKING_ROUTES_OSMC_ATTR.equals(attrName)) {
				adapter.addItem(createHikingRoutesItem(activity, attrName, property, nightMode));
			} else {
				String id = ROUTES_ID + attrName;
				int drawableId = getIconIdForAttr(attrName);
				String name = AndroidUtils.getRenderingStringPropertyName(activity, attrName, property != null ? property.getName() : attrName);
				ContextMenuItem item = createBooleanRenderingProperty(activity, attrName, name, id, property, drawableId, nightMode);
				if (item != null) {
					adapter.addItem(item);
				}
			}
			customRules.remove(property);
		}
		OsmandApplication app = activity.getMyApplication();
		ResourceManager manager = app.getResourceManager();
		if (OsmandPlugin.isDevelopment() && !Algorithms.isEmpty(manager.getTravelMapRepositories())) {
			adapter.addItem(createTravelRoutesItem(activity, nightMode));
		}
	}

	private ContextMenuItem createCycleRoutesItem(@NonNull MapActivity activity, @NonNull String attrName,
												  @Nullable RenderingRuleProperty property, boolean nightMode) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(attrName);

		return new ContextMenuItem.ItemBuilder()
				.setId(ROUTES_ID + attrName)
				.setTitle(AndroidUtils.getRenderingStringPropertyName(app, attrName, property != null ? property.getName() : attrName))
				.setIcon(getIconIdForAttr(attrName))
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setSelected(pref.get())
				.setColor(pref.get() ? settings.getApplicationMode().getProfileColor(nightMode) : null)
				.setDescription(app.getString(pref.get() ? R.string.shared_string_enabled : R.string.shared_string_disabled))
				.setListener(new OnRowItemClick() {

					@Override
					public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
						if (property != null) {
							activity.getDashboard().setDashboardVisibility(true, DashboardType.CYCLE_ROUTES, AndroidUtils.getCenterViewCoordinates(view));
						} else {
							showRendererSnackbarForAttr(activity, attrName, nightMode);
						}
						return false;
					}

					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						pref.set(isChecked);
						ContextMenuItem item = adapter.getItem(position);
						if (item != null) {
							item.setColor(activity, isChecked ? R.color.osmand_orange : INVALID_ID);
							item.setDescription(app.getString(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled));
							adapter.notifyDataSetChanged();
						}
						if (property != null) {
							activity.refreshMap();
							activity.updateLayers();
						} else {
							showRendererSnackbarForAttr(activity, attrName, nightMode);
						}
						return false;
					}
				}).createItem();
	}

	private ContextMenuItem createHikingRoutesItem(@NonNull MapActivity activity, @NonNull String attrName, @Nullable RenderingRuleProperty property, boolean nightMode) {
		OsmandApplication app = activity.getMyApplication();
		CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(attrName);

		boolean enabled = property != null && Arrays.asList(property.getPossibleValues()).contains(pref.get());
		String previousValue = enabled || property == null ? pref.get() : property.getPossibleValues()[0];
		String propertyName = AndroidUtils.getRenderingStringPropertyName(activity, attrName, property != null ? property.getName() : attrName);

		return new ContextMenuItem.ItemBuilder()
				.setId(ROUTES_ID + attrName)
				.setTitle(propertyName)
				.setIcon(getIconIdForAttr(attrName))
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setSelected(enabled)
				.setDescription(app.getString(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled))
				.setColor(enabled ? app.getSettings().getApplicationMode().getProfileColor(nightMode) : null)
				.setListener(new OnRowItemClick() {

					@Override
					public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
						activity.getDashboard().setDashboardVisibility(true, DashboardType.HIKING_ROUTES, AndroidUtils.getCenterViewCoordinates(view));
						return false;
					}

					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						pref.set(isChecked ? previousValue : "");
						ContextMenuItem item = adapter.getItem(position);
						if (item != null) {
							item.setColor(activity, isChecked ? R.color.osmand_orange : INVALID_ID);
							item.setDescription(app.getString(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled));
							adapter.notifyDataSetChanged();
						}
						activity.refreshMap();
						activity.updateLayers();
						return false;
					}
				}).createItem();
	}

	private ContextMenuItem createTravelRoutesItem(@NonNull MapActivity activity, boolean nightMode) {
		OsmandSettings settings = activity.getMyApplication().getSettings();
		boolean selected = settings.SHOW_TRAVEL.get();
		return new ContextMenuItem.ItemBuilder()
				.setId(ROUTES_ID + TRAVEL_ROUTES)
				.setTitle(activity.getString(R.string.travel_routes))
				.setIcon(getIconIdForAttr(TRAVEL_ROUTES))
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setSelected(selected)
				.setColor(selected ? settings.APPLICATION_MODE.get().getProfileColor(nightMode) : null)
				.setDescription(activity.getString(selected ? R.string.shared_string_enabled : R.string.shared_string_disabled))
				.setListener(new OnRowItemClick() {

					@Override
					public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
						activity.getDashboard().setDashboardVisibility(true, DashboardType.TRAVEL_ROUTES, AndroidUtils.getCenterViewCoordinates(view));
						return false;
					}

					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
						settings.SHOW_TRAVEL.set(isChecked);
						ContextMenuItem item = adapter.getItem(position);
						if (item != null) {
							item.setSelected(isChecked);
							item.setColor(activity, isChecked ? R.color.osmand_orange : INVALID_ID);
							item.setDescription(activity.getString(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled));
							adapter.notifyDataSetChanged();
						}
						activity.refreshMap();
						activity.updateLayers();
						return false;
					}
				}).createItem();
	}

	private static Map<String, String> getRoutesDefaultAttrs() {
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put(SHOW_CYCLE_ROUTES_ATTR, RendererRegistry.DEFAULT_RENDER);
		attrs.put(SHOW_MTB_ROUTES_ATTR, RendererRegistry.DEFAULT_RENDER);
		attrs.put(HIKING_ROUTES_OSMC_ATTR, RendererRegistry.DEFAULT_RENDER);
		attrs.put(ALPINE_HIKING_ATTR, RendererRegistry.DEFAULT_RENDER);
		attrs.put(PISTE_ROUTES_ATTR, RendererRegistry.WINTER_SKI_RENDER);
		attrs.put(HORSE_ROUTES_ATTR, RendererRegistry.DEFAULT_RENDER);
		attrs.put(WHITE_WATER_SPORTS_ATTR, RendererRegistry.DEFAULT_RENDER);
		return attrs;
	}

	@Nullable
	private RenderingRuleProperty getPropertyForAttr(@NonNull List<RenderingRuleProperty> customRules, @NonNull String attrName) {
		for (RenderingRuleProperty property : customRules) {
			if (Algorithms.stringsEqual(property.getAttrName(), attrName)) {
				return property;
			}
		}
		return null;
	}

	@Nullable
	private String getRendererForAttr(@NonNull String attrName) {
		return getRoutesDefaultAttrs().get(attrName);
	}

	@DrawableRes
	private int getIconIdForAttr(@NonNull String attrName) {
		switch (attrName) {
			case SHOW_CYCLE_ROUTES_ATTR:
				return R.drawable.ic_action_bicycle_dark;
			case SHOW_MTB_ROUTES_ATTR:
				return R.drawable.ic_action_mountain_bike;
			case WHITE_WATER_SPORTS_ATTR:
				return R.drawable.ic_action_kayak;
			case HORSE_ROUTES_ATTR:
				return R.drawable.ic_action_horse;
			case HIKING_ROUTES_OSMC_ATTR:
			case ALPINE_HIKING_ATTR:
				return R.drawable.ic_action_trekking_dark;
			case PISTE_ROUTES_ATTR:
				return R.drawable.ic_action_skiing;
			case TRAVEL_ROUTES:
				return R.drawable.mm_routes;
		}
		return INVALID_ID;
	}

	private void createRenderingAttributeItems(List<RenderingRuleProperty> customRules,
											   final ContextMenuAdapter adapter, final MapActivity activity,
											   final boolean nightMode) {
		final OsmandApplication app = activity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final int selectedProfileColor = settings.APPLICATION_MODE.get().getProfileColor(nightMode);
		final int themeRes = getThemeRes(nightMode);

		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_widget_map_rendering, activity)
				.setId(MAP_RENDERING_CATEGORY_ID)
				.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(MAP_STYLE_ID)
				.setTitleId(R.string.map_widget_renderer, activity)
				.setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_map)
				.setListener(new ContextMenuAdapter.ItemClickListener() {
					@Override
					public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> ad, int itemId,
													  final int pos, boolean isChecked, int[] viewCoordinates) {
						SelectMapStyleBottomSheetDialogFragment.showInstance(activity.getSupportFragmentManager());
						return false;
					}
				})
				.setItemDeleteAction(makeDeleteAction(settings.RENDERER))
				.setOnUpdateCallback(new ContextMenuItem.OnUpdateCallback() {
					@Override
					public void onUpdateMenuItem(ContextMenuItem item) {
						String renderDescr = ConfigureMapUtils.getRenderDescr(app);
						item.setDescription(renderDescr);
					}
				})
				.createItem());

		String description = "";
		SunriseSunset sunriseSunset = activity.getMyApplication().getDaynightHelper().getSunriseSunset();
		if (sunriseSunset != null && sunriseSunset.getSunrise() != null && sunriseSunset.getSunset() != null) {
			DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
			String sunriseTime = dateFormat.format(sunriseSunset.getSunrise());
			String sunsetTime = dateFormat.format(sunriseSunset.getSunset());
			DayNightMode dayNightMode = activity.getMyApplication().getSettings().DAYNIGHT_MODE.get();
			if (dayNightMode.isDay() || dayNightMode.isNight()) {
				if (sunriseSunset.isDaytime()) {
					description = String.format(app.getString(R.string.sunset_at), sunsetTime);
				} else {
					description = String.format(app.getString(R.string.sunrise_at), sunriseTime);
				}
			} else if (dayNightMode.isAuto() || dayNightMode.isSensor()) {
				description = String.format(app.getString(R.string.ltr_or_rtl_combine_via_slash), sunriseTime, sunsetTime);
			}
			description = String.format(app.getString(R.string.ltr_or_rtl_combine_via_bold_point), ConfigureMapUtils.getDayNightDescr(activity), description);
		} else {
			description = ConfigureMapUtils.getDayNightDescr(activity);
		}
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_mode, activity)
				.setId(MAP_MODE_ID)
				.setDescription(description)
				.setIcon(ConfigureMapUtils.getDayNightIcon(activity)).setListener((ad, itemId, pos, isChecked, viewCoordinates) -> {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						ConfigureMapDialogs.showMapModeDialog(activity, themeRes, nightMode);
					}
					return false;
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
				.setIcon(R.drawable.ic_action_map_magnifier).setListener((ad, itemId, pos, isChecked, viewCoordinates) -> {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						ConfigureMapDialogs.showMapMagnifierDialog(activity, adapter, themeRes, nightMode, pos, ad);
					}
					return false;
				})
				.setItemDeleteAction(makeDeleteAction(settings.MAP_DENSITY))
				.createItem());

		ContextMenuItem props = createRenderingProperty(customRules, adapter, activity,
				R.drawable.ic_action_intersection, ROAD_STYLE_ATTR, ROAD_STYLE_ID, nightMode);
		if (props != null) {
			adapter.addItem(props);
		}

		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.text_size, activity)
				.setId(TEXT_SIZE_ID)
				.setDescription(ConfigureMapUtils.getScale(activity)).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_text_size).setListener((ad, itemId, pos, isChecked, viewCoordinates) -> {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						ConfigureMapDialogs.showTextSizeDialog(activity, adapter, themeRes, nightMode, pos, ad);
					}
					return false;
				})
				.setItemDeleteAction(makeDeleteAction(settings.TEXT_SCALE))
				.createItem());

		String localeDescr = activity.getMyApplication().getSettings().MAP_PREFERRED_LOCALE.get();
		localeDescr = localeDescr == null || localeDescr.isEmpty() ? activity.getString(R.string.local_map_names)
				: localeDescr;
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.map_locale, activity)
				.setId(MAP_LANGUAGE_ID)
				.setDescription(localeDescr).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_language)
				.setListener((ad, itemId, pos, isChecked, viewCoordinates) -> {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						ConfigureMapDialogs.showMapLanguageDialog(activity, adapter, themeRes, nightMode, pos, ad);
					}
					return false;
				})
				.setItemDeleteAction(makeDeleteAction(settings.MAP_PREFERRED_LOCALE))
				.createItem());

		props = createProperties(customRules, R.string.rendering_category_details, R.drawable.ic_action_layers,
				UI_CATEGORY_DETAILS, adapter, activity, true, DETAILS_ID, nightMode, selectedProfileColor);
		if (props != null) {
			adapter.addItem(props);
		}
		props = createProperties(customRules, R.string.rendering_category_hide, R.drawable.ic_action_hide,
				UI_CATEGORY_HIDE, adapter, activity, true, HIDE_ID, nightMode, selectedProfileColor);
		if (props != null) {
			adapter.addItem(props);
		}
		props = createProperties(customRules, R.string.rendering_category_routes, R.drawable.ic_action_map_routes,
				UI_CATEGORY_ROUTES, adapter, activity, true,
				CUSTOM_RENDERING_ITEMS_ID_SCHEME + UI_CATEGORY_ROUTES, nightMode, selectedProfileColor);
		if (props != null) {
			adapter.addItem(props);
		}

		if (getCustomRenderingPropertiesSize(customRules) > 0) {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.rendering_category_others, activity)
					.setCategory(true).setLayout(R.layout.list_group_title_with_switch).createItem());
			createCustomRenderingProperties(adapter, activity, customRules, nightMode);
		}
	}

	private ContextMenuItem createProperties(List<RenderingRuleProperty> customRules,
											 @StringRes final int strId,
											 @DrawableRes final int icon,
											 final String category,
											 final ContextMenuAdapter adapter,
											 final MapActivity activity,
											 final boolean useDescription,
											 final String id,
											 final boolean nightMode,
											 @ColorInt final int selectedProfileColor) {

		final List<RenderingRuleProperty> ps = new ArrayList<>();
		final List<CommonPreference<Boolean>> prefs = new ArrayList<>();
		Iterator<RenderingRuleProperty> it = customRules.iterator();

		while (it.hasNext()) {
			RenderingRuleProperty p = it.next();
			if (category.equals(p.getCategory()) && p.isBoolean()) {
				ps.add(p);
				final CommonPreference<Boolean> pref = activity.getMyApplication().getSettings()
						.getCustomRenderBooleanProperty(p.getAttrName());
				prefs.add(pref);
				it.remove();
			}
		}
		if (prefs.size() > 0) {
			final ItemClickListener clickListener = new ContextMenuAdapter.ItemClickListener() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> a, int itemId, int pos,
												  boolean isChecked, int[] viewCoordinates) {
					if (!isChecked && !useDescription) {
						for (int i = 0; i < prefs.size(); i++) {
							prefs.get(i).set(false);
						}
						a.notifyDataSetInvalidated();
						activity.refreshMapComplete();
						activity.getMapLayers().updateLayers(activity);
					} else {
						if (UI_CATEGORY_DETAILS.equals(category)) {
							DetailsBottomSheet.showInstance(activity.getSupportFragmentManager(), ps, prefs, a, adapter, pos);
						} else {
							ConfigureMapDialogs.showPreferencesDialog(adapter, a, pos, activity,
									activity.getString(strId), ps, prefs, nightMode, selectedProfileColor);
						}
					}
					return false;
				}
			};
			ContextMenuItem.ItemBuilder builder = new ContextMenuItem.ItemBuilder().setTitleId(strId, activity)
					.setId(id)
					.setIcon(icon).setListener(clickListener);
			boolean selected = false;
			for (CommonPreference<Boolean> p : prefs) {
				if (p.get()) {
					selected = true;
					break;
				}
			}
			builder.setColor(activity, selected ? R.color.osmand_orange : INVALID_ID);
			if (useDescription) {
				builder.setDescription(ConfigureMapUtils.getDescription(prefs));
				builder.setLayout(R.layout.list_item_single_line_descrition_narrow);
			} else {
				builder.setListener(new OnRowItemClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> a, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						return clickListener.onContextMenuClick(a, itemId, pos, isChecked, null);
					}

					@Override
					public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> a, View view, int itemId, int pos) {
						ConfigureMapDialogs.showPreferencesDialog(adapter, a, pos, activity,
								activity.getString(strId), ps, prefs, nightMode, selectedProfileColor);
						return false;
					}
				});
				builder.setSecondaryIcon(R.drawable.ic_action_additional_option);
				builder.setSelected(selected);
			}
			builder.setItemDeleteAction(makeDeleteAction(prefs));
			return builder.createItem();
		}
		return null;
	}

	private boolean isPropertyAccepted(RenderingRuleProperty property) {
		String attrName = property.getAttrName();
		return !(A_APP_MODE.equals(attrName)
				|| A_BASE_APP_MODE.equals(attrName)
				|| A_ENGINE_V1.equals(attrName)
				|| HIKING_ROUTES_OSMC_ATTR.equals(attrName)
				|| ROAD_STYLE_ATTR.equals(attrName)
				|| CONTOUR_WIDTH_ATTR.equals(attrName)
				|| CONTOUR_DENSITY_ATTR.equals(attrName)
				|| CONTOUR_LINES_ATTR.equals(attrName)
				|| CONTOUR_LINES_SCHEME_ATTR.equals(attrName)
				|| CURRENT_TRACK_COLOR_ATTR.equals(attrName)
				|| CURRENT_TRACK_WIDTH_ATTR.equals(attrName)
				|| CYCLE_NODE_NETWORK_ROUTES_ATTR.equals(attrName)
		);
	}

	private void createCustomRenderingProperties(ContextMenuAdapter adapter, MapActivity activity,
												 List<RenderingRuleProperty> customRules,
												 boolean nightMode) {
		for (RenderingRuleProperty p : customRules) {
			if (isPropertyAccepted(p)) {
				adapter.addItem(createRenderingProperty(adapter, activity, INVALID_ID, p, CUSTOM_RENDERING_ITEMS_ID_SCHEME + p.getName(), nightMode));
			}
		}
	}

	private int getCustomRenderingPropertiesSize(List<RenderingRuleProperty> customRules) {
		int size = 0;
		for (RenderingRuleProperty p : customRules) {
			if (isPropertyAccepted(p)) {
				size++;
			}
		}
		return size;
	}

	private ContextMenuItem createRenderingProperty(List<RenderingRuleProperty> customRules,
													ContextMenuAdapter adapter, MapActivity activity,
													@DrawableRes int icon, String attrName, String id,
													boolean nightMode) {
		for (RenderingRuleProperty p : customRules) {
			if (p.getAttrName().equals(attrName)) {
				return createRenderingProperty(adapter, activity, icon, p, id, nightMode);
			}
		}
		return null;
	}

	private ContextMenuItem createRenderingProperty(ContextMenuAdapter adapter, MapActivity activity,
													@DrawableRes int icon, RenderingRuleProperty p, String id,
													boolean nightMode) {
		OsmandApplication app = activity.getMyApplication();
		if (p.isBoolean()) {
			String name = AndroidUtils.getRenderingStringPropertyName(activity, p.getAttrName(), p.getName());
			return createBooleanRenderingProperty(activity, p.getAttrName(), name, id, p, icon, nightMode);
		} else {
			final CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(p.getAttrName());
			String descr;
			if (!Algorithms.isEmpty(pref.get())) {
				descr = AndroidUtils.getRenderingStringPropertyValue(activity, pref.get());
			} else {
				descr = AndroidUtils.getRenderingStringPropertyValue(app, p.getDefaultValueDescription());
			}
			String propertyName = AndroidUtils.getRenderingStringPropertyName(app, p.getAttrName(), p.getName());
			ItemBuilder builder = ContextMenuItem.createBuilder(propertyName)
					.setId(id)
					.setListener((ad, itemId, pos, isChecked, viewCoordinates) -> {
						if (AndroidUtils.isActivityNotDestroyed(activity)) {
							ConfigureMapDialogs.showRenderingPropertyDialog(activity, adapter, p, pref, pos, nightMode);
						}
						return false;
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

	private ContextMenuItem createBooleanRenderingProperty(@NonNull MapActivity activity,
														   @NonNull String attrName,
														   @NonNull String name,
														   @NonNull String id,
														   @Nullable RenderingRuleProperty property,
														   @DrawableRes int icon,
														   boolean nightMode) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();

		CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(attrName);
		return ContextMenuItem.createBuilder(name)
				.setId(id)
				.setListener(new ItemClickListener() {

					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						if (property != null) {
							pref.set(isChecked);
							activity.refreshMap();
							activity.updateLayers();
						} else {
							isChecked = pref.get();
							showRendererSnackbarForAttr(activity, attrName, nightMode);
						}
						ContextMenuItem item = adapter.getItem(pos);
						if (item != null) {
							item.setSelected(pref.get());
							item.setColor(activity, isChecked ? R.color.osmand_orange : INVALID_ID);
							item.setDescription(app.getString(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled));
							adapter.notifyDataSetChanged();
						}
						return false;
					}
				})
				.setSelected(pref.get())
				.setColor(pref.get() ? settings.getApplicationMode().getProfileColor(nightMode) : null)
				.setDescription(app.getString(pref.get() ? R.string.shared_string_enabled : R.string.shared_string_disabled))
				.setIcon(icon)
				.createItem();
	}

	private void showRendererSnackbarForAttr(@NonNull MapActivity activity, @NonNull String attrName, boolean nightMode) {
		String renderer = getRendererForAttr(attrName);
		if (renderer != null) {
			OsmandApplication app = activity.getMyApplication();
			String rendererName = RendererRegistry.getRendererName(app, renderer);
			String text = app.getString(R.string.setting_supported_by_style, rendererName);
			Snackbar snackbar = Snackbar.make(activity.getLayout(), text, Snackbar.LENGTH_LONG)
					.setAction(R.string.shared_string_change, new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(renderer);
							if (loaded != null) {
								app.getSettings().RENDERER.set(renderer);
								app.getRendererRegistry().setCurrentSelectedRender(loaded);
								activity.refreshMapComplete();
								activity.getDashboard().updateListAdapter(createListAdapter(activity));
							} else {
								app.showShortToastMessage(R.string.renderer_load_exception);
							}
						}
					});
			UiUtilities.setupSnackbar(snackbar, nightMode);
			snackbar.show();
		}
	}
}
