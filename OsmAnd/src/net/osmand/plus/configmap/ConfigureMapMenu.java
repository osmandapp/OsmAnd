package net.osmand.plus.configmap;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.*;
import static net.osmand.osm.OsmRouteType.ALPINE;
import static net.osmand.osm.OsmRouteType.HIKING;
import static net.osmand.osm.OsmRouteType.SKI;
import static net.osmand.plus.configmap.ConfigureMapUtils.getPropertyForAttr;
import static net.osmand.plus.configmap.routes.RouteUtils.CYCLE_NODE_NETWORK_ROUTES_ATTR;
import static net.osmand.plus.configmap.routes.RouteUtils.SHOW_MTB_SCALE;
import static net.osmand.plus.configmap.routes.RouteUtils.SHOW_MTB_SCALE_IMBA_TRAILS;
import static net.osmand.plus.configmap.routes.RouteUtils.SHOW_MTB_SCALE_UPHILL;
import static net.osmand.plus.configmap.routes.RouteUtils.TRAVEL_ROUTES;
import static net.osmand.plus.dashboard.DashboardType.MAP_ROUTES;
import static net.osmand.plus.plugins.openseamaps.NauticalDepthContourFragment.DEPTH_CONTOUR_COLOR_SCHEME;
import static net.osmand.plus.plugins.openseamaps.NauticalDepthContourFragment.DEPTH_CONTOUR_WIDTH;
import static net.osmand.plus.plugins.osmedit.OsmEditingPlugin.RENDERING_CATEGORY_OSM_ASSISTANT;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_DENSITY_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_SCHEME_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_WIDTH_ATTR;
import static net.osmand.plus.transport.TransportLinesMenu.RENDERING_CATEGORY_TRANSPORT;
import static net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem.INVALID_ID;
import static net.osmand.render.RenderingRuleStorageProperties.A_APP_MODE;
import static net.osmand.render.RenderingRuleStorageProperties.A_BASE_APP_MODE;
import static net.osmand.render.RenderingRuleStorageProperties.A_ENGINE_V1;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_DETAILS;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_HIDE;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.OnResultCallback;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.routes.RouteLayersHelper;
import net.osmand.plus.configmap.routes.RouteUtils;
import net.osmand.plus.configmap.routes.MapRoutesFragment;
import net.osmand.plus.dashboard.DashboardType;
import net.osmand.plus.dialogs.DetailsBottomSheet;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.transport.TransportLinesMenu;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.CoordinatesGridSettings;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;
import net.osmand.util.SunriseSunset;

import org.apache.commons.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ConfigureMapMenu {

	private static final Log LOG = PlatformUtil.getLog(ConfigureMapMenu.class);

	public static final String ALPINE_HIKING_SCALE_SCHEME_ATTR = "alpineHikingScaleScheme";

	public static final String CURRENT_TRACK_COLOR_ATTR = "currentTrackColor";
	public static final String CURRENT_TRACK_WIDTH_ATTR = "currentTrackWidth";
	public static final String COLOR_ATTR = "color";
	public static final String ROAD_STYLE_ATTR = "roadStyle";
	public static final String OTHER_MAP_ATTRIBUTES_CATEGORY = "otherMapAttributesCategory";

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public ConfigureMapMenu(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	@NonNull
	public ContextMenuAdapter createListAdapter(@NonNull MapActivity mapActivity) {
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);

		ContextMenuAdapter adapter = new ContextMenuAdapter(app);

		adapter.addItem(new ContextMenuItem(APP_PROFILES_ID)
				.setTitleId(R.string.app_modes_choose, mapActivity)
				.setLayout(R.layout.mode_toggles));

		List<RenderingRuleProperty> customRules = ConfigureMapUtils.getCustomRules(app,
				UI_CATEGORY_HIDDEN, RENDERING_CATEGORY_TRANSPORT);

		createLayersItems(customRules, adapter, mapActivity, nightMode);
		PluginsHelper.registerConfigureMapCategory(adapter, mapActivity, customRules);
		createRouteAttributeItems(customRules, adapter, mapActivity, nightMode);
		createRenderingAttributeItems(customRules, adapter, mapActivity, nightMode);

		return adapter;
	}

	private void createLayersItems(@NonNull List<RenderingRuleProperty> customRules,
	                               @NonNull ContextMenuAdapter adapter,
	                               @NonNull MapActivity activity,
	                               boolean nightMode) {
		int selectedProfileColor = settings.getApplicationMode().getProfileColor(nightMode);
		MapLayerMenuListener listener = new MapLayerMenuListener(activity);

		adapter.addItem(new ContextMenuItem(SHOW_CATEGORY_ID)
				.setCategory(true)
				.setLayout(R.layout.list_group_title_with_switch)
				.setTitleId(R.string.shared_string_show, activity));

		boolean selected = settings.SHOW_FAVORITES.get();
		adapter.addItem(new ContextMenuItem(FAVORITES_ID)
				.setTitleId(R.string.shared_string_favorites, activity)
				.setSelected(settings.SHOW_FAVORITES.get())
				.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
				.setIcon(R.drawable.ic_action_favorite)
				.setItemDeleteAction(settings.SHOW_FAVORITES)
				.setListener(listener));

		ResourceManager resourceManager = app.getResourceManager();
		boolean hasPoiData = !Algorithms.isEmpty(resourceManager.getAmenityRepositories())
				|| resourceManager.hasTravelRepositories();
		if (hasPoiData) {
			PoiFiltersHelper poiFilters = app.getPoiFilters();
			selected = poiFilters.isShowingAnyGeneralPoi();
			adapter.addItem(new ContextMenuItem(POI_OVERLAY_ID)
					.setTitleId(R.string.layer_poi, activity)
					.setSelected(selected)
					.setDescription(poiFilters.getGeneralSelectedPoiFiltersName())
					.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
					.setIcon(R.drawable.ic_action_info_dark)
					.setSecondaryIcon(R.drawable.ic_action_additional_option)
					.setListener(listener));
		}
		selected = settings.SHOW_POI_LABEL.get();
		adapter.addItem(new ContextMenuItem(POI_OVERLAY_LABELS_ID)
				.setTitleId(R.string.layer_amenity_label, activity)
				.setSelected(settings.SHOW_POI_LABEL.get())
				.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
				.setIcon(R.drawable.ic_action_text_dark)
				.setItemDeleteAction(settings.SHOW_POI_LABEL)
				.setListener(listener));

		TransportLinesMenu transportLinesMenu = new TransportLinesMenu(app);
		selected = transportLinesMenu.isShowAnyTransport();
		adapter.addItem(new ContextMenuItem(TRANSPORT_ID)
				.setTitleId(R.string.rendering_category_transport, activity)
				.setIcon(R.drawable.ic_action_transport_bus)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setSelected(selected)
				.setColor(selected ? selectedProfileColor : null)
				.setListener(listener));

		adapter.addItem(new ContextMenuItem(GPX_FILES_ID)
				.setTitleId(R.string.layer_gpx_layer, activity)
				.setIcon(R.drawable.ic_action_polygom_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setRefreshCallback(item -> {
					GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();
					boolean hasSelectedGpx = gpxHelper.isAnyGpxFileSelected();
					item.setSelected(hasSelectedGpx);
					item.setDescription(gpxHelper.getGpxDescription());
					item.setColor(app, hasSelectedGpx ? R.color.osmand_orange : INVALID_ID);
				})
				.setListener(listener));

		selected = settings.SHOW_MAP_MARKERS.get();
		adapter.addItem(new ContextMenuItem(MAP_MARKERS_ID)
				.setTitleId(R.string.map_markers, activity)
				.setSelected(selected)
				.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
				.setIcon(R.drawable.ic_action_flag)
				.setItemDeleteAction(settings.SHOW_MAP_MARKERS)
				.setListener(listener));

		String mapSourceTitle = settings.getSelectedMapSourceTitle();
		adapter.addItem(new ContextMenuItem(MAP_SOURCE_ID)
				.setTitleId(R.string.layer_map, activity)
				.setIcon(R.drawable.ic_world_globe_dark)
				.setDescription(mapSourceTitle)
				.setItemDeleteAction(settings.MAP_ONLINE_DATA, settings.MAP_TILE_SOURCES)
				.setListener(listener));

		PluginsHelper.registerLayerContextMenu(adapter, activity, customRules);
		app.getAidlApi().registerLayerContextMenu(adapter, activity);

		selected = settings.SHOW_BORDERS_OF_DOWNLOADED_MAPS.get();
		adapter.addItem(new ContextMenuItem(MAP_BORDERS_ID)
				.setTitleId(R.string.show_borders_of_downloaded_maps, activity)
				.setSelected(selected)
				.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
				.setIcon(R.drawable.ic_action_map_download)
				.setItemDeleteAction(settings.SHOW_BORDERS_OF_DOWNLOADED_MAPS)
				.setListener(listener));

		if (CoordinatesGridSettings.isGridSupported(app)) {
			CoordinatesGridSettings gridSettings = new CoordinatesGridSettings(app);
			selected = gridSettings.isEnabled();
			adapter.addItem(new ContextMenuItem(COORDINATES_GRID_ID)
					.setTitleId(R.string.layer_coordinates_grid, activity)
					.setSelected(selected)
					.setColor(app, selected ? R.color.osmand_orange : INVALID_ID)
					.setIcon(CoordinatesGridController.getStateIcon(selected))
					.setSecondaryIcon(R.drawable.ic_action_additional_option)
					.setItemDeleteAction(settings.SHOW_COORDINATES_GRID)
					.setListener(listener));
		}
	}

	private void createRouteAttributeItems(@NonNull List<RenderingRuleProperty> customRules,
			@NonNull ContextMenuAdapter adapter, @NonNull MapActivity activity, boolean nightMode) {
		adapter.addItem(new ContextMenuItem(ROUTES_CATEGORY_ID)
				.setCategory(true)
				.setTitleId(R.string.rendering_category_routes, activity)
				.setLayout(R.layout.list_group_title_with_switch));

		for (String attrName : RouteUtils.getRoutesAttrsNames(customRules)) {
			RenderingRuleProperty property = getPropertyForAttr(customRules, attrName);
			if (ALPINE.getRenderingPropertyAttr().equals(attrName)) {
				RenderingRuleProperty alpineProperty = app.getRendererRegistry().getCustomRenderingRuleProperty(ALPINE_HIKING_SCALE_SCHEME_ATTR);
				adapter.addItem(createRoutesItem(activity, attrName, alpineProperty, nightMode));
			} else if (MapRoutesFragment.shouldShow(app, attrName)) {
				adapter.addItem(createRoutesItem(activity, attrName, property, nightMode));
			} else {
				String id = ROUTES_ITEMS_ID_SCHEME + attrName;
				int iconId = RouteUtils.getIconIdForAttr(attrName);
				String name = AndroidUtils.getRenderingStringPropertyName(activity, attrName, property != null ? property.getName() : attrName);
				CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(attrName);
				ContextMenuItem item = createBooleanRenderingProperty(activity, attrName, name, id, property, iconId, nightMode, result -> {
					if (property == null) {
						RouteUtils.showRendererSnackbarForAttr(activity, attrName, nightMode, pref);
					}
				});
				adapter.addItem(item);
			}
			customRules.remove(property);
		}
		if (PluginsHelper.isDevelopment()) {
			adapter.addItem(createTravelRoutesItem(activity, nightMode));
		}
	}

	@NonNull
	private ContextMenuItem createRoutesItem(@NonNull MapActivity activity,
			@NonNull String attrName, @Nullable RenderingRuleProperty property, boolean nightMode) {
		RouteLayersHelper routeLayersHelper = app.getRouteLayersHelper();
		boolean enabled = routeLayersHelper.isRoutesTypeEnabled(attrName);

		return new ContextMenuItem(ROUTES_ITEMS_ID_SCHEME + attrName)
				.setTitle(AndroidUtils.getRenderingStringPropertyName(app, attrName, property != null ? property.getName() : attrName))
				.setIcon(RouteUtils.getIconIdForAttr(attrName))
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setSelected(enabled)
				.setColor(enabled ? settings.getApplicationMode().getProfileColor(nightMode) : null)
				.setDescription(routeLayersHelper.getRoutesTypeDescription(attrName))
				.setListener(new OnRowItemClick() {

					@Override
					public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter adapter,
							@NonNull View view, @NonNull ContextMenuItem item) {
						if (property != null || SKI.getRenderingPropertyAttr().equals(attrName)) {
							routeLayersHelper.setSelectedAttrName(attrName);
							activity.getDashboard().setDashboardVisibility(true, MAP_ROUTES, AndroidUtils.getCenterViewCoordinates(view));
						} else {
							RouteUtils.showRendererSnackbarForAttr(activity, attrName, nightMode, null);
						}
						return false;
					}

					@Override
					public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter adapter,
							@Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
						routeLayersHelper.toggleRoutesType(attrName);

						item.setSelected(isChecked);
						item.setColor(activity, isChecked ? R.color.osmand_orange : INVALID_ID);
						item.setDescription(routeLayersHelper.getRoutesTypeDescription(attrName));
						if (adapter != null) {
							adapter.onDataSetChanged();
						}
						if (property != null) {
							activity.refreshMapComplete();
							activity.updateLayers();
						} else {
							RouteUtils.showRendererSnackbarForAttr(activity, attrName, nightMode, null);
						}
						return false;
					}
				});
	}

	@NonNull
	private ContextMenuItem createTravelRoutesItem(@NonNull MapActivity activity, boolean nightMode) {
		boolean selected = settings.SHOW_TRAVEL.get();
		return new ContextMenuItem(ROUTES_ITEMS_ID_SCHEME + TRAVEL_ROUTES)
				.setTitle(activity.getString(R.string.travel_routes))
				.setIcon(RouteUtils.getIconIdForAttr(TRAVEL_ROUTES))
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setSelected(selected)
				.setColor(selected ? settings.APPLICATION_MODE.get().getProfileColor(nightMode) : null)
				.setDescription(activity.getString(selected ? R.string.shared_string_enabled : R.string.shared_string_disabled))
				.setListener(new OnRowItemClick() {

					@Override
					public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
					                              @NonNull View view, @NonNull ContextMenuItem item) {
						activity.getDashboard().setDashboardVisibility(true, DashboardType.TRAVEL_ROUTES, AndroidUtils.getCenterViewCoordinates(view));
						return false;
					}

					@Override
					public boolean onContextMenuClick(@NonNull OnDataChangeUiAdapter uiAdapter,
					                                  @NonNull View view, @NonNull ContextMenuItem item,
					                                  boolean isChecked) {
						settings.SHOW_TRAVEL.set(isChecked);
						item.setSelected(isChecked);
						item.setColor(activity, isChecked ? R.color.osmand_orange : INVALID_ID);
						item.setDescription(activity.getString(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled));
						uiAdapter.onDataSetChanged();
						activity.refreshMap();
						activity.updateLayers();
						return false;
					}
				});
	}

	private void createRenderingAttributeItems(List<RenderingRuleProperty> customRules,
	                                           ContextMenuAdapter adapter, MapActivity activity,
	                                           boolean nightMode) {
		adapter.addItem(new ContextMenuItem(MAP_RENDERING_CATEGORY_ID)
				.setCategory(true)
				.setLayout(R.layout.list_group_title_with_switch)
				.setTitleId(R.string.map_widget_map_rendering, activity));

		adapter.addItem(new ContextMenuItem(MAP_STYLE_ID)
				.setTitleId(R.string.map_widget_renderer, activity)
				.setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_map)
				.setListener((uiAdapter, view, item, isChecked) -> {
					SelectMapStyleBottomSheetDialogFragment.showInstance(activity.getSupportFragmentManager());
					return false;
				})
				.setItemDeleteAction(settings.RENDERER)
				.setRefreshCallback(item -> {
					String renderDesc = ConfigureMapUtils.getRenderDescr(app);
					item.setDescription(renderDesc);
				}));

		String description = "";
		DayNightMode dayNightMode = settings.DAYNIGHT_MODE.get();
		SunriseSunset sunriseSunset = app.getDaynightHelper().getSunriseSunset();
		if (!dayNightMode.isAppTheme() && sunriseSunset != null && sunriseSunset.getSunrise() != null && sunriseSunset.getSunset() != null) {
			DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
			String sunriseTime = dateFormat.format(sunriseSunset.getSunrise());
			String sunsetTime = dateFormat.format(sunriseSunset.getSunset());
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
		adapter.addItem(new ContextMenuItem(MAP_MODE_ID)
				.setTitleId(R.string.map_mode, activity)
				.setDescription(description)
				.setIcon(ConfigureMapUtils.getDayNightIcon(activity))
				.setListener((uiAdapter, view, item, isChecked) -> {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						MapModeController.showDialog(activity);
					}
					return true;
				})
				.setItemDeleteAction(settings.DAYNIGHT_MODE));

		String magnifierDesc = String.format(Locale.UK, "%.0f", 100f * settings.MAP_DENSITY.get()) + " %";
		adapter.addItem(new ContextMenuItem(MAP_MAGNIFIER_ID)
				.setTitleId(R.string.map_magnifier, activity)
				.setDescription(magnifierDesc)
				.setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_magnifier)
				.setListener((uiAdapter, view, item, isChecked) -> {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						ConfigureMapDialogs.showMapMagnifierDialog(activity, nightMode, item, uiAdapter);
					}
					return false;
				})
				.setItemDeleteAction(settings.MAP_DENSITY));

		ContextMenuItem props = createRenderingProperty(customRules, activity,
				R.drawable.ic_action_intersection, ROAD_STYLE_ATTR, ROAD_STYLE_ID, nightMode);
		if (props != null) {
			adapter.addItem(props);
		}

		adapter.addItem(new ContextMenuItem(TEXT_SIZE_ID)
				.setTitleId(R.string.text_size, activity)
				.setDescription(ConfigureMapUtils.getScale(activity))
				.setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_text_size)
				.setListener((uiAdapter, view, item, isChecked) -> {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						ConfigureMapDialogs.showTextSizeDialog(activity, nightMode, item, uiAdapter);
					}
					return false;
				})
				.setItemDeleteAction(settings.TEXT_SCALE));

		String localeDescr = settings.MAP_PREFERRED_LOCALE.get();
		localeDescr = localeDescr == null || localeDescr.isEmpty() ? activity.getString(R.string.local_map_names)
				: localeDescr;
		adapter.addItem(new ContextMenuItem(MAP_LANGUAGE_ID)
				.setTitleId(R.string.map_locale, activity)
				.setDescription(localeDescr).setLayout(R.layout.list_item_single_line_descrition_narrow)
				.setIcon(R.drawable.ic_action_map_language)
				.setListener((uiAdapter, view, item, isChecked) -> {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						ConfigureMapDialogs.showMapLanguageDialog(activity, nightMode, item, uiAdapter);
					}
					return false;
				})
				.setItemDeleteAction(settings.MAP_PREFERRED_LOCALE));

		props = createProperties(customRules, R.string.rendering_category_details, R.drawable.ic_action_layers,
				UI_CATEGORY_DETAILS, activity, DETAILS_ID, nightMode);
		if (props != null) {
			adapter.addItem(props);
		}
		props = createProperties(customRules, R.string.rendering_category_hide, R.drawable.ic_action_hide,
				UI_CATEGORY_HIDE, activity, HIDE_ID, nightMode);
		if (props != null) {
			adapter.addItem(props);
		}

		if (getCustomRenderingPropertiesSize(customRules) > 0) {
			adapter.addItem(new ContextMenuItem(OTHER_MAP_ATTRIBUTES_CATEGORY)
					.setCategory(true)
					.setTitleId(R.string.rendering_category_others, activity)
					.setLayout(R.layout.list_group_title_with_switch));
			createCustomRenderingProperties(adapter, activity, customRules, nightMode);
		}
	}

	private ContextMenuItem createProperties(List<RenderingRuleProperty> customRules,
	                                         @StringRes int strId,
	                                         @DrawableRes int icon,
	                                         String category,
	                                         MapActivity activity,
	                                         String id,
	                                         boolean nightMode) {
		List<RenderingRuleProperty> properties = new ArrayList<>();
		List<CommonPreference<Boolean>> preferences = new ArrayList<>();
		Iterator<RenderingRuleProperty> it = customRules.iterator();

		while (it.hasNext()) {
			RenderingRuleProperty p = it.next();
			if (category.equals(p.getCategory()) && p.isBoolean()) {
				properties.add(p);
				preferences.add(settings.getCustomRenderBooleanProperty(p.getAttrName()));
				it.remove();
			}
		}
		if (!preferences.isEmpty()) {
			ItemClickListener clickListener = (uiAdapter, view, item, isChecked) -> {
				if (UI_CATEGORY_DETAILS.equals(category)) {
					DetailsBottomSheet.showInstance(activity.getSupportFragmentManager(), properties, preferences, uiAdapter, item);
				} else {
					ConfigureMapDialogs.showPreferencesDialog(uiAdapter, item, activity,
							activity.getString(strId), properties, preferences, nightMode);
				}
				return false;
			};
			ContextMenuItem item = new ContextMenuItem(id)
					.setTitleId(strId, activity)
					.setIcon(icon).setListener(clickListener);
			item.setRefreshCallback(refreshableItem -> {
				boolean selected = false;
				for (CommonPreference<Boolean> p : preferences) {
					if (p.get()) {
						selected = true;
						break;
					}
				}
				refreshableItem.setColor(activity, selected ? R.color.osmand_orange : INVALID_ID);
				refreshableItem.setDescription(ConfigureMapUtils.getDescription(settings, preferences));
			});
			item.setLayout(R.layout.list_item_single_line_descrition_narrow);
			OsmandPreference<?>[] prefArray = new OsmandPreference[preferences.size()];
			item.setItemDeleteAction(preferences.toArray(prefArray));
			return item;
		}
		return null;
	}

	private boolean isPropertyAccepted(RenderingRuleProperty property) {
		String category = property.getCategory();
		String attrName = property.getAttrName();
		return !(A_APP_MODE.equals(attrName)
				|| A_BASE_APP_MODE.equals(attrName)
				|| A_ENGINE_V1.equals(attrName)
				|| HIKING.getRenderingPropertyAttr().equals(attrName)
				|| ROAD_STYLE_ATTR.equals(attrName)
				|| CONTOUR_WIDTH_ATTR.equals(attrName)
				|| CONTOUR_DENSITY_ATTR.equals(attrName)
				|| CONTOUR_LINES_ATTR.equals(attrName)
				|| CONTOUR_LINES_SCHEME_ATTR.equals(attrName)
				|| CURRENT_TRACK_COLOR_ATTR.equals(attrName)
				|| CURRENT_TRACK_WIDTH_ATTR.equals(attrName)
				|| CYCLE_NODE_NETWORK_ROUTES_ATTR.equals(attrName)
				|| SHOW_MTB_SCALE_IMBA_TRAILS.equals(attrName)
				|| SHOW_MTB_SCALE.equals(attrName)
				|| SHOW_MTB_SCALE_UPHILL.equals(attrName)
				|| RENDERING_CATEGORY_OSM_ASSISTANT.equals(category)
				|| DEPTH_CONTOUR_WIDTH.equals(attrName)
				|| DEPTH_CONTOUR_COLOR_SCHEME.equals(attrName)
				|| ALPINE.getRenderingPropertyAttr().equals(attrName)
				|| ALPINE_HIKING_SCALE_SCHEME_ATTR.equals(attrName)
		);
	}

	private void createCustomRenderingProperties(ContextMenuAdapter adapter, MapActivity activity,
	                                             List<RenderingRuleProperty> customRules,
	                                             boolean nightMode) {
		for (RenderingRuleProperty p : customRules) {
			if (isPropertyAccepted(p)) {
				adapter.addItem(createRenderingProperty(activity, INVALID_ID, p, CUSTOM_RENDERING_ITEMS_ID_SCHEME + p.getName(), nightMode));
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
	                                                MapActivity activity,
	                                                @DrawableRes int icon, String attrName, String id,
	                                                boolean nightMode) {
		for (RenderingRuleProperty p : customRules) {
			if (p.getAttrName().equals(attrName)) {
				return createRenderingProperty(activity, icon, p, id, nightMode);
			}
		}
		return null;
	}

	public static ContextMenuItem createRenderingProperty(MapActivity activity, @DrawableRes int icon,
	                                                      RenderingRuleProperty p, String id,
	                                                      boolean nightMode) {
		OsmandApplication app = activity.getApp();
		OsmandSettings settings = app.getSettings();
		if (p.isBoolean()) {
			String name = AndroidUtils.getRenderingStringPropertyName(activity, p.getAttrName(), p.getName());
			return createBooleanRenderingProperty(activity, p.getAttrName(), name, id, p, icon, nightMode, null);
		} else {
			CommonPreference<String> pref = settings.getCustomRenderProperty(p.getAttrName());
			String description = AndroidUtils.getRenderingStringPropertyValue(app, p);
			String propertyName = AndroidUtils.getRenderingStringPropertyName(app, p.getAttrName(), p.getName());
			ContextMenuItem item = new ContextMenuItem(id)
					.setTitle(propertyName)
					.setListener((uiAdapter, view, _item, isChecked) -> {
						if (AndroidUtils.isActivityNotDestroyed(activity)) {
							ConfigureMapDialogs.showRenderingPropertyDialog(activity, p, pref, _item, uiAdapter, nightMode);
						}
						return false;
					})
					.setDescription(description)
					.setItemDeleteAction(pref)
					.setLayout(R.layout.list_item_single_line_descrition_narrow);
			if (icon != INVALID_ID) {
				item.setIcon(icon);
			}
			return item;
		}
	}

	@NonNull
	public static ContextMenuItem createBooleanRenderingProperty(@NonNull MapActivity activity,
	                                                             @NonNull String attrName,
	                                                             @NonNull String name,
	                                                             @NonNull String id,
	                                                             @Nullable RenderingRuleProperty property,
	                                                             @DrawableRes int icon,
	                                                             boolean nightMode,
	                                                             @Nullable OnResultCallback<Boolean> callback) {
		OsmandApplication app = activity.getApp();
		OsmandSettings settings = app.getSettings();

		CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(attrName);
		return new ContextMenuItem(id)
				.setTitle(name)
				.setSelected(pref.get())
				.setColor(pref.get() ? settings.getApplicationMode().getProfileColor(nightMode) : null)
				.setDescription(app.getString(pref.get() ? R.string.shared_string_enabled : R.string.shared_string_disabled))
				.setIcon(icon)
				.setListener((uiAdapter, view, item, isChecked) -> {
					if (property != null) {
						pref.set(isChecked);
						activity.refreshMapComplete();
						activity.updateLayers();
					} else {
						isChecked = pref.get();
					}
					if (callback != null) {
						callback.onResult(isChecked);
					}
					item.setSelected(pref.get());
					item.setColor(activity, isChecked ? R.color.osmand_orange : INVALID_ID);
					item.setDescription(app.getString(isChecked ? R.string.shared_string_enabled : R.string.shared_string_disabled));
					uiAdapter.onDataSetChanged();
					return false;
				});
	}
}
