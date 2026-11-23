package net.osmand.plus.plugins.srtm;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.*;
import static net.osmand.plus.download.DownloadActivityType.GEOTIFF_FILE;
import static net.osmand.plus.plugins.srtm.CollectColorPalletTask.CollectColorPalletListener;
import static net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem.INVALID_ID;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import net.osmand.IndexConstants;
import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.dashboard.DashboardType;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.openseamaps.NauticalMapsPlugin;
import net.osmand.plus.plugins.srtm.building.Building3DDetailLevel;
import net.osmand.plus.plugins.srtm.building.Buildings3DColorType;
import net.osmand.plus.plugins.srtm.building.SunLight;
import net.osmand.plus.plugins.srtm.building.SunLightMode;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.shared.palette.data.PaletteChangeEvent;
import net.osmand.shared.palette.domain.PaletteItem;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SRTMPlugin extends OsmandPlugin {

	private static final String SRTM_PLUGIN_COMPONENT_PAID = "net.osmand.srtmPlugin.paid";
	private static final String SRTM_PLUGIN_COMPONENT = "net.osmand.srtmPlugin";

	public static final String CONTOUR_PREFIX = "contour";
	public static final String CONTOUR_LINES_ATTR = "contourLines";
	public static final String CONTOUR_LINES_SCHEME_ATTR = "contourColorScheme";
	public static final String CONTOUR_LINES_DISABLED_VALUE = "disabled";
	public static final String CONTOUR_WIDTH_ATTR = "contourWidth";
	public static final String CONTOUR_DENSITY_ATTR = "contourDensity";
	public static final String CONTOUR_LABEL_DIRECTION_ATTR = "contourLabelDirection";
	public static final String ELEVATION_UNITS_ATTR = "eleUnits";
	public static final String ELEVATION_UNITS_FEET_VALUE = "feet";
	public static final String ELEVATION_UNITS_METERS_VALUE = "meters";
	public static final String BUILDINGS_3D = "3D Buildings";

	public static final int TERRAIN_MIN_SUPPORTED_ZOOM = 4;
	public static final int TERRAIN_MAX_SUPPORTED_ZOOM = 19;

	public static final int DEFAULT_HILLSHADE_SUN_ANGLE = 45;
	public static final int DEFAULT_HILLSHADE_SUN_AZIMUTH = 315;

	public static final float MIN_VERTICAL_EXAGGERATION = 1.0f;
	public static final float MAX_VERTICAL_EXAGGERATION = 3.0f;

	public static final int MIN_TERRAIN_SHADOWS_INTENSITY = 10;
	public static final int MAX_TERRAIN_SHADOWS_INTENSITY = 100;
	public static final int DEFAULT_TERRAIN_SHADOWS_INTENSITY = 40;


	public final CommonPreference<Boolean> TERRAIN;
	public final CommonPreference<String> TERRAIN_MODE;
	public final CommonPreference<Integer> TERRAIN_SHADOWS_INTENSITY;
	public final OsmandPreference<Boolean> BUILDINGS_3D_DETAIL_LEVEL;
	public final OsmandPreference<Boolean> BUILDINGS_3D_ENABLE_COLORING;
	public final CommonPreference<String> CONTOUR_LINES_ZOOM;
	public final CommonPreference<Integer> HILLSHADE_SUN_ANGLE;
	public final CommonPreference<Integer> HILLSHADE_SUN_AZIMUTH;
	public final CommonPreference<SunLightMode> SUN_LIGHT_MODE;
	public final CommonPreference<Long> SUN_LIGHT_TIME;

	private final StateChangedListener<Boolean> enable3DMapsListener;
	private final StateChangedListener<Boolean> terrainListener;
	private final StateChangedListener<Integer> hillshadeSunAngleListener;
	private final StateChangedListener<Integer> hillshadeSunAzimuthListener;
	private final StateChangedListener<Boolean> castShadowsListener;
	private final StateChangedListener<SunLightMode> sunLightModeListener;
	private final StateChangedListener<Long> sunLightTimeListener;
	private final StateChangedListener<String> terrainModeListener;
	private final StateChangedListener<Integer> terrainShadowsIntensityListener;
	private final StateChangedListener<Float> verticalExaggerationListener;
	private final StateChangedListener<MetricsConstants> metricSystemListener;
	private final StateChangedListener<Boolean> map3DObjectsListener;


	private final SunLight sunLight;
	private TerrainLayer terrainLayer;

	@Override
	public String getId() {
		return PLUGIN_SRTM;
	}

	public SRTMPlugin(OsmandApplication app) {
		super(app);

		BUILDINGS_3D_DETAIL_LEVEL = settings.getCustomRenderBooleanProperty("show3DbuildingParts");
		BUILDINGS_3D_ENABLE_COLORING = settings.getCustomRenderBooleanProperty("useDefaultBuildingColor");

		TERRAIN = registerBooleanPreference("terrain_layer", true).makeProfile();
		TerrainMode[] tms = TerrainMode.values(app);
		TERRAIN_MODE = registerStringPreference("terrain_mode", tms.length == 0 ? "" : tms[0].getKeyName()).makeProfile();
		TERRAIN_SHADOWS_INTENSITY = registerIntPreference("terrain_shadows_intensity", DEFAULT_TERRAIN_SHADOWS_INTENSITY).makeProfile().cache();

		CONTOUR_LINES_ZOOM = registerStringPreference("contour_lines_zoom", null).makeProfile().cache();

		HILLSHADE_SUN_ANGLE = registerIntPreference("hillshade_sun_angle", DEFAULT_HILLSHADE_SUN_ANGLE).makeGlobal().cache();
		HILLSHADE_SUN_AZIMUTH = registerIntPreference("hillshade_sun_azimuth", DEFAULT_HILLSHADE_SUN_AZIMUTH).makeGlobal().cache();
		SUN_LIGHT_MODE = registerEnumStringPreference("sun_light_mode", SunLightMode.MANUAL, SunLightMode.values(), SunLightMode.class).makeGlobal().cache();
		SUN_LIGHT_TIME = registerLongPreference("sun_light_time", 0).makeGlobal().cache();
		sunLight = new SunLight(app, this);

		enable3DMapsListener = change -> app.runInUIThread(() -> {
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null) {
				// Terrain shadows are drawn only on the 3D relief
				mapContext.updateElevationConfiguration();
				mapContext.recreateHeightmapProvider();
			}
		});
		settings.ENABLE_3D_MAPS.addListener(enable3DMapsListener);

		terrainListener = change -> app.runInUIThread(this::updateTerrainRendering);
		TERRAIN.addListener(terrainListener);

		terrainModeListener = change -> app.runInUIThread(this::updateTerrainRendering);
		TERRAIN_MODE.addListener(terrainModeListener);

		terrainShadowsIntensityListener = change -> app.runInUIThread(this::updateElevationConfiguration);
		TERRAIN_SHADOWS_INTENSITY.addListener(terrainShadowsIntensityListener);
		verticalExaggerationListener = scale -> {
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null) {
				mapContext.updateVerticalExaggerationScale();
			}
		};
		app.getSettings().VERTICAL_EXAGGERATION_SCALE.addListener(verticalExaggerationListener);

		metricSystemListener = constants -> app.getOsmandMap().getMapView().refreshMapComplete();
		app.getSettings().METRIC_SYSTEM.addListener(metricSystemListener);

		map3DObjectsListener = enabled -> {
			MapRendererContext ctx = net.osmand.plus.views.corenative.NativeCoreContext.getMapRendererContext();
			if (ctx != null) {
				if (Boolean.TRUE.equals(enabled)) {
					ctx.recreate3DObjectsProvider();
				} else {
					ctx.reset3DObjectsProvider();
				}
			}
		};
		settings.ENABLE_3D_MAP_OBJECTS.addListener(map3DObjectsListener);

		hillshadeSunAngleListener = change -> app.runInUIThread(this::updateElevationConfiguration);
		HILLSHADE_SUN_ANGLE.addListener(hillshadeSunAngleListener);

		hillshadeSunAzimuthListener = change -> app.runInUIThread(this::updateElevationConfiguration);
		HILLSHADE_SUN_AZIMUTH.addListener(hillshadeSunAzimuthListener);

		castShadowsListener = change -> app.runInUIThread(() -> {
			MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
			if (mapContext != null) {
				mapContext.updateShadows();
			}
		});
		settings.CAST_SHADOWS.addListener(castShadowsListener);

		sunLightModeListener = change -> app.runInUIThread(() -> {
			// A turned off plugin gets no car session stop that would end the updates
			if (isEnabled()) {
				sunLight.updateLiveUpdates();
			}
			updateElevationConfiguration();
		});
		SUN_LIGHT_MODE.addListener(sunLightModeListener);

		sunLightTimeListener = change -> app.runInUIThread(this::updateElevationConfiguration);
		SUN_LIGHT_TIME.addListener(sunLightTimeListener);
	}

	@NonNull
	public SunLight getSunLight() {
		return sunLight;
	}

	@Override
	public void mapActivityResume(@NonNull MapActivity activity) {
		sunLight.setMapVisible(true);
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
		sunLight.setMapVisible(false);
	}

	@Override
	public void onCarNavigationSessionStart() {
		sunLight.updateLiveUpdates();
	}

	@Override
	public void onCarNavigationSessionStop() {
		sunLight.updateLiveUpdates();
	}

	public void updateElevationConfiguration() {
		MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
		if (mapContext != null) {
			mapContext.updateElevationConfiguration();
		}
	}

	private void updateTerrainRendering() {
		MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
		if (mapContext != null) {
			mapContext.updateElevationConfiguration();
			if (mapContext.isHeightmapsActive() != is3DMapsEnabled()) {
				mapContext.recreateHeightmapProvider();
				// 3D tracks take their heights from the heightmaps, as on an ENABLE_3D_MAPS change in MapLayers
				app.getOsmandMap().getMapLayers().getGpxLayer().setInvalidated(true);
				app.getOsmandMap().getMapView().refreshMap();
			}
		}
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_srtm;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.contour_lines);
	}

	@Override
	public boolean needsInstallation() {
		return super.needsInstallation() && !InAppPurchaseUtils.isContourLinesAvailable(app);
	}

	@Override
	protected boolean isAvailable(OsmandApplication app) {
		return super.isAvailable(app) || InAppPurchaseUtils.isContourLinesAvailable(app);
	}

	@Override
	public boolean isMarketPlugin() {
		return true;
	}

	@Override
	public boolean isPaid() {
		return true;
	}

	@Override
	public boolean isEnableByDefault() {
		return true;
	}

	@Override
	public String getComponentId1() {
		return SRTM_PLUGIN_COMPONENT_PAID;
	}

	@Override
	public String getComponentId2() {
		return SRTM_PLUGIN_COMPONENT;
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		String docsUrl = app.getString(R.string.docs_plugin_srtm);
		String description = app.getString(R.string.srtm_plugin_description, docsUrl);
		return linksEnabled ? UiUtilities.createUrlSpannable(app, description, docsUrl) : description;
	}

	@Override
	public String getName() {
		return app.getString(R.string.srtm_plugin_name);
	}

	@Nullable
	@Override
	public OsmAndFeature getOsmAndFeature() {
		return OsmAndFeature.TERRAIN;
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, Activity activity) {
		OsmandSettings settings = app.getSettings();
		settings.getCustomRenderProperty(CONTOUR_LINES_ATTR).setDefaultValue("13");
		MapRendererContext ctx = net.osmand.plus.views.corenative.NativeCoreContext.getMapRendererContext();
		if (ctx != null) {
			if (Boolean.TRUE.equals(settings.ENABLE_3D_MAP_OBJECTS.get())) {
				ctx.recreate3DObjectsProvider();
			} else {
				ctx.reset3DObjectsProvider();
			}
		}
		NativeCoreContext.updateContourLinesDirectory(app, true);
		// Turned on while a map is on screen: no resume or car session start will follow
		if (activity instanceof MapActivity mapActivity
				&& AndroidUtils.isStateAtLeast(mapActivity, Lifecycle.State.RESUMED)) {
			sunLight.setMapVisible(true);
		} else {
			sunLight.updateLiveUpdates();
		}
		return true;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		super.disable(app);
		sunLight.stopLiveUpdates();
		NativeCoreContext.updateContourLinesDirectory(app, false);
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		if (terrainLayer != null) {
			app.getOsmandMap().getMapView().removeLayer(terrainLayer);
		}
		if (TERRAIN.get()) {
			terrainLayer = new TerrainLayer(context, this);
			app.getOsmandMap().getMapView().addLayer(terrainLayer, 0.6f);
			// The new layer passes its tiles to the map renderer only on a map refresh
			app.getOsmandMap().getMapView().refreshMap();
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (mapRendererContext != null) {
			updateMapPresentationEnvironment(mapRendererContext);
		}
	}

	// If enabled, map should be rendered with elevation data (in 3D)
	public boolean is3DMapsEnabled() {
		return is3DReliefAllowed() && settings.ENABLE_3D_MAPS.get();
	}

	public boolean is3DReliefAllowed() {
		return app.useOpenGlRenderer() && InAppPurchaseUtils.is3dMapsAvailable(app);
	}

	public boolean isTerrainLayerEnabled() {
		return TERRAIN.get();
	}

	public void setTerrainLayerEnabled(boolean enabled) {
		TERRAIN.set(enabled);
	}


	// Terrain shadows shade the 3D relief, unless the Hillshade layer already shades it
	public boolean isTerrainShadowsEnabled() {
		return is3DMapsEnabled() && !isReliefShadedByHillshade();
	}

	// The Terrain layer is drawn only while the plugin is active, see updateLayers
	public boolean isReliefShadedByHillshade() {
		return isActive() && isTerrainLayerEnabled() && getTerrainMode().isHillshade();
	}

	public int getTerrainShadowsIntensity() {
		int intensity = TERRAIN_SHADOWS_INTENSITY.get();
		return Math.max(MIN_TERRAIN_SHADOWS_INTENSITY, Math.min(MAX_TERRAIN_SHADOWS_INTENSITY, intensity));
	}

	public TerrainMode getTerrainMode() {
		return TerrainMode.getByKey(TERRAIN_MODE.get());
	}

	public void setTerrainMode(TerrainMode mode) {
		TERRAIN_MODE.set(mode.getKeyName());
	}

	public void setTerrainTransparency(int transparency, TerrainMode mode) {
		mode.setTransparency(transparency);
	}

	public void setTerrainZoomValues(int minZoom, int maxZoom, TerrainMode mode) {
		mode.setZoomValues(minZoom, maxZoom);
	}

	public static float normalizeVerticalExaggerationScale(float scale) {
		return Math.max(MIN_VERTICAL_EXAGGERATION, Math.min(MAX_VERTICAL_EXAGGERATION, scale));
	}

	public float getVerticalExaggerationScale() {
		return normalizeVerticalExaggerationScale(app.getSettings().VERTICAL_EXAGGERATION_SCALE.get());
	}

	public void setVerticalExaggerationScale(float scale) {
		app.getSettings().VERTICAL_EXAGGERATION_SCALE.set(normalizeVerticalExaggerationScale(scale));
	}

	public int getTerrainTransparency() {
		return getTerrainMode().getTransparency();
	}

	public void resetZoomLevelsToDefault() {
		getTerrainMode().resetZoomsToDefault();
	}

	public void resetTransparencyToDefault() {
		getTerrainMode().resetTransparencyToDefault();
	}

	public void resetVerticalExaggerationToDefault() {
		app.getSettings().VERTICAL_EXAGGERATION_SCALE.resetToDefault();
	}

	public int getTerrainMinZoom() {
		int minSupportedZoom = TERRAIN_MIN_SUPPORTED_ZOOM;
		return Math.max(minSupportedZoom, getTerrainMode().getMinZoom());
	}

	public int getTerrainMaxZoom() {
		int maxSupportedZoom = TERRAIN_MAX_SUPPORTED_ZOOM;
		return Math.min(maxSupportedZoom, getTerrainMode().getMaxZoom());
	}

	public static boolean isContourLinesLayerEnabled(OsmandApplication app) {
		boolean contourLinesEnabled = false;

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(contourLinesProp.getAttrName());
			if (!Algorithms.isEmpty(pref.get())) {
				contourLinesEnabled = !CONTOUR_LINES_DISABLED_VALUE.equals(pref.get());
			} else {
				contourLinesEnabled = !CONTOUR_LINES_DISABLED_VALUE.equals(contourLinesProp.getDefaultValueDescription());
			}
		}
		return contourLinesEnabled;
	}

	public void updateLayers(@NonNull MapActivity mapActivity) {
		updateLayers(mapActivity, mapActivity);
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (TERRAIN.get() && isActive()) {
			removeTerrainLayer(mapView);
			registerLayers(context, mapActivity);
		} else {
			removeTerrainLayer(mapView);
		}
	}

	private void removeTerrainLayer(@NonNull OsmandMapTileView mapView) {
		if (terrainLayer != null) {
			mapView.removeLayer(terrainLayer);
			terrainLayer = null;
			mapView.refreshMap();
		}
	}

	@Override
	protected void registerConfigureMapCategoryActions(@NonNull ContextMenuAdapter adapter,
	                                                   @NonNull MapActivity mapActivity,
	                                                   @NonNull List<RenderingRuleProperty> customRules) {
		if (isEnabled()) {
			adapter.addItem(new ContextMenuItem(TERRAIN_CATEGORY_ID)
					.setCategory(true)
					.setTitle(app.getString(R.string.srtm_plugin_name))
					.setLayout(R.layout.list_group_title_with_switch));

			if (isLocked()) {
				addTerrainDescriptionItem(adapter, mapActivity);
				addBuildin3DItem(adapter, mapActivity);
			} else {
				createContextMenuItems(adapter, mapActivity);
			}
			NauticalMapsPlugin nauticalPlugin = PluginsHelper.getPlugin(NauticalMapsPlugin.class);
			if (nauticalPlugin != null) {
				nauticalPlugin.createAdapterItem(TERRAIN_DEPTH_CONTOURS, adapter, mapActivity, customRules);
			}
			createSphericalMapItem(adapter, mapActivity);
		}
	}

	private void addTerrainDescriptionItem(@NonNull ContextMenuAdapter adapter,
	                                       @NonNull MapActivity activity) {
		if (app.useOpenGlRenderer()) {
			adapter.addItem(new ContextMenuItem(TERRAIN_DESCRIPTION_ID)
					.setLayout(R.layout.list_item_terrain_description)
					.setClickable(false)
					.setListener((uiAdapter, view, item, isChecked) -> {
						ChoosePlanFragment.showInstance(activity, OsmAndFeature.TERRAIN);
						return true;
					}));
		} else {
			PurchasingUtils.createPromoItem(adapter, activity, OsmAndFeature.TERRAIN,
					TERRAIN_PROMO_ID,
					R.string.srtm_plugin_name,
					R.string.contour_lines_hillshade_slope);
		}
	}

	private void createContextMenuItems(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity) {
		ItemClickListener listener = new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter, @NonNull View view, @NonNull ContextMenuItem item) {
				int[] viewCoordinates = AndroidUtils.getCenterViewCoordinates(view);
				int itemId = item.getTitleId();
				if (itemId == R.string.download_srtm_maps) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONTOUR_LINES, viewCoordinates);
					return false;
				} else if (itemId == R.string.shared_string_terrain) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.TERRAIN, viewCoordinates);
					return false;
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(@NonNull OnDataChangeUiAdapter uiAdapter, @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
				int itemId = item.getTitleId();
				if (itemId == R.string.download_srtm_maps) {
					toggleContourLines(mapActivity, isChecked, () -> {
						RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
						if (contourLinesProp != null) {
							boolean selected = !settings.getRenderPropertyValue(contourLinesProp).equals(CONTOUR_LINES_DISABLED_VALUE);

							SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
							PluginsHelper.enablePluginIfNeeded(mapActivity, mapActivity.getApp(), plugin, true);

							item.setDescription(app.getString(R.string.display_zoom_level,
									AndroidUtils.getRenderingStringPropertyValue(app, contourLinesProp)));
							item.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
							item.setSelected(selected);
							uiAdapter.onDataSetChanged();
							mapActivity.refreshMapComplete();
						}
					});
				} else if (itemId == R.string.shared_string_terrain) {
					toggleTerrain(isChecked, () -> {
						boolean selected = TERRAIN.get();
						SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
						if (selected) {
							PluginsHelper.enablePluginIfNeeded(mapActivity, mapActivity.getApp(), plugin, true);
						}
						item.setColor(app, selected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
						item.setSelected(selected);
						uiAdapter.onDataSetChanged();
						// The 3D Relief row says whether the Hillshade layer turns its Terrain shadows off
						uiAdapter.onRefreshItem(RELIEF_3D_ID);
						updateLayers(mapActivity, mapActivity);
						mapActivity.refreshMapComplete();
					});
				}
				return true;
			}
		};

		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			boolean contourLinesSelected = isContourLinesLayerEnabled(app);
			String descr = AndroidUtils.getRenderingStringPropertyValue(app, contourLinesProp);
			adapter.addItem(new ContextMenuItem(CONTOUR_LINES)
					.setTitleId(R.string.download_srtm_maps, mapActivity)
					.setSelected(contourLinesSelected)
					.setIcon(R.drawable.ic_plugin_srtm)
					.setDescription(app.getString(R.string.display_zoom_level, descr))
					.setColor(app, contourLinesSelected ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
					.setItemDeleteAction(CONTOUR_LINES_ZOOM)
					.setSecondaryIcon(R.drawable.ic_action_additional_option)
					.setListener(listener));
		}
		boolean terrainEnabled = TERRAIN.get();
		TerrainMode terrainMode = TerrainMode.getByKey(TERRAIN_MODE.get());
		adapter.addItem(new ContextMenuItem(TERRAIN_ID)
				.setTitleId(R.string.shared_string_terrain, mapActivity)
				.setDescription(terrainMode.getType().getName(app))
				.setSelected(terrainEnabled)
				.setColor(app, terrainEnabled ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_action_hillshade_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setItemDeleteAction(TERRAIN, TERRAIN_MODE)
				.setListener(listener)

		);

		add3DItems(adapter, mapActivity);
	}

	/** The rows of 3D Relief and 3D buildings of Configure map; the Terrain screen shows the same two. */
	public void add3DItems(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity) {
		if (app.useOpenGlRenderer()) {
			add3DReliefItem(adapter, mapActivity);
		}
		addBuildin3DItem(adapter, mapActivity);
	}

	/** The switch raises the relief, the row opens the 3D Relief screen. Without OsmAnd Pro it offers the plan. */
	private void add3DReliefItem(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity activity) {
		ContextMenuItem item = new ContextMenuItem(RELIEF_3D_ID)
				.setTitleId(R.string.relief_3d, activity)
				.setIcon(R.drawable.ic_action_3d_relief);

		if (!InAppPurchaseUtils.is3dMapsAvailable(app)) {
			item.setUseNaturalSecondIconColor(true)
					.setSecondaryIcon(PurchasingUtils.getProFeatureIconId(isNightMode(activity, app)))
					.setListener((uiAdapter, view, clicked, isChecked) -> {
						ChoosePlanFragment.showInstance(activity, OsmAndFeature.RELIEF_3D);
						return false;
					});
			adapter.addItem(item);
			return;
		}
		boolean enabled = settings.ENABLE_3D_MAPS.get();
		item.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setRefreshCallback(refreshable -> refreshable.setDescription(get3DReliefDescription()))
				.setSelected(enabled)
				.setColor(app, enabled ? R.color.osmand_orange : INVALID_ID)
				.setListener(new OnRowItemClick() {
					@Override
					public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
					                              @NonNull View view, @NonNull ContextMenuItem item) {
						int[] viewCoordinates = AndroidUtils.getCenterViewCoordinates(view);
						activity.getDashboard().setDashboardVisibility(true, DashboardType.RELIEF_3D, viewCoordinates);
						return false;
					}

					@Override
					public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
					                                  @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
						settings.ENABLE_3D_MAPS.set(isChecked);
						item.setSelected(isChecked);
						item.setColor(app, isChecked ? R.color.osmand_orange : INVALID_ID);
						item.setDescription(get3DReliefDescription());
						if (uiAdapter != null) {
							uiAdapter.onDataSetChanged();
						}
						app.runInUIThread(() -> app.getOsmandMap().getMapLayers().getMapInfoLayer().recreateAllControls(activity));
						return true;
					}
				});
		adapter.addItem(item);
	}

	/** Names what shades the relief: Terrain shadows with their intensity, or the Hillshade layer. */
	@NonNull
	private String get3DReliefDescription() {
		if (!settings.ENABLE_3D_MAPS.get()) {
			return app.getString(R.string.shared_string_off);
		}
		String shadows = isReliefShadedByHillshade()
				? app.getString(R.string.shared_string_off)
				: getTerrainShadowsIntensity() + "%";
		return app.getString(R.string.ltr_or_rtl_combine_via_bold_point, app.getString(R.string.shared_string_on),
				app.getString(R.string.ltr_or_rtl_combine_via_space, app.getString(R.string.terrain_shadows), shadows));
	}

	private void addBuildin3DItem(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity) {
		boolean enabled = settings.ENABLE_3D_MAP_OBJECTS.get();
		adapter.addItem(new ContextMenuItem(TERRAIN_3D_MAP_OBJECTS)
				.setTitleId(R.string.enable_3d_objects, mapActivity)
				.setIcon(R.drawable.ic_action_3d_buildings)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setDescription(app.getString(enabled ? get3DBuildingDetailLvlDescription() : R.string.shared_string_off))
				.setSelected(enabled)
				.setColor(app, enabled ? R.color.osmand_orange : INVALID_ID)
				.setListener(new OnRowItemClick() {

					@Override
					public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
					                              @NonNull View view, @NonNull ContextMenuItem item) {
						int[] viewCoordinates = AndroidUtils.getCenterViewCoordinates(view);
						int itemId = item.getTitleId();
						if (itemId == R.string.enable_3d_objects) {
							mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.BUILDINGS_3D, viewCoordinates);
							return false;
						}
						return true;
					}

					@Override
					public boolean onContextMenuClick(@NonNull OnDataChangeUiAdapter uiAdapter,
					                                  @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
						int itemId = item.getTitleId();
						if (itemId == R.string.enable_3d_objects) {
							settings.ENABLE_3D_MAP_OBJECTS.set(isChecked);
							item.setColor(app, isChecked ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
							item.setSelected(isChecked);
							item.setDescription(app.getString(isChecked ? get3DBuildingDetailLvlDescription() : R.string.shared_string_off));
							uiAdapter.onDataSetChanged();
						}
						return true;
					}
				}).setItemDeleteAction(settings.ENABLE_3D_MAP_OBJECTS));
	}

	private int get3DBuildingDetailLvlDescription() {
		boolean level = BUILDINGS_3D_DETAIL_LEVEL.get();
		return Building3DDetailLevel.Companion.fromValue(level).getLabelResId();
	}

	private void createSphericalMapItem(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity activity) {
		adapter.addItem(new ContextMenuItem(TERRAIN_SPHERICAL_MAP)
				.setTitleId(R.string.show_spherical_map, activity)
				.setIcon(R.drawable.ic_world_globe_dark)
				.setDescription(app.getString(R.string.show_spherical_map_description))
				.setSelected(settings.SPHERICAL_MAP.get())
				.setColor(app, settings.SPHERICAL_MAP.get() ? R.color.osmand_orange : INVALID_ID)
				.setListener(new OnRowItemClick() {
					@Override
					public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
					                                  @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
						settings.SPHERICAL_MAP.set(isChecked);
						item.setColor(app, settings.SPHERICAL_MAP.get() ? R.color.osmand_orange : INVALID_ID);
						if (uiAdapter != null) {
							uiAdapter.onDataSetChanged();
						}
						return true;
					}
				}).setItemDeleteAction(settings.SPHERICAL_MAP));
	}

	@Nullable
	@Override
	protected String getRenderPropertyPrefix() {
		return CONTOUR_PREFIX;
	}

	@Override
	public List<IndexItem> getSuggestedMaps() {
		List<IndexItem> suggestedMaps = new ArrayList<>();

		DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet && settings.isInternetConnectionAvailable()) {
			downloadThread.runReloadIndexFiles();
		}

		if (!downloadThread.shouldDownloadIndexes()) {
			LatLon latLon = app.getMapViewTrackingUtilities().getMapLocation();
			suggestedMaps.addAll(getMapsForType(latLon, DownloadActivityType.SRTM_COUNTRY_FILE));

			OsmandDevelopmentPlugin plugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
			if (!app.useOpenGlRenderer() || plugin != null && plugin.USE_RASTER_SQLITEDB.get()) {
				suggestedMaps.addAll(getMapsForType(latLon, DownloadActivityType.HILLSHADE_FILE));
				suggestedMaps.addAll(getMapsForType(latLon, DownloadActivityType.SLOPE_FILE));
			} else {
				suggestedMaps.addAll(getMapsForType(latLon, GEOTIFF_FILE));
			}
		}

		return suggestedMaps;
	}

	public void toggleContourLines(MapActivity activity, boolean isChecked, Runnable callback) {
		RenderingRuleProperty contourLinesProp = app.getRendererRegistry().getCustomRenderingRuleProperty(CONTOUR_LINES_ATTR);
		if (contourLinesProp != null) {
			CommonPreference<String> pref = settings.getCustomRenderProperty(contourLinesProp.getAttrName());
			CommonPreference<String> zoomSetting = CONTOUR_LINES_ZOOM;
			if (!isChecked) {
				zoomSetting.set(pref.get());
				pref.set(CONTOUR_LINES_DISABLED_VALUE);
				if (callback != null) {
					callback.run();
				}
			} else if (zoomSetting.get() != null && !zoomSetting.get().equals(CONTOUR_LINES_DISABLED_VALUE)) {
				pref.set(zoomSetting.get());
				if (callback != null) {
					callback.run();
				}
			} else {
				selectPropertyValue(activity, contourLinesProp, pref, callback);
			}
		}
	}

	public void toggleTerrain(boolean isChecked, Runnable callback) {
		TERRAIN.set(isChecked);
		if (callback != null) {
			callback.run();
		}
	}

	public void selectPropertyValue(@NonNull MapActivity activity,
	                                @NonNull RenderingRuleProperty property,
	                                @NonNull CommonPreference<String> preference, @Nullable Runnable callback) {
		List<String> possibleValues = new ArrayList<>(Arrays.asList(property.getPossibleValues()));

		int index = AndroidUtils.getRenderPropertySelectedValueIndex(app, property);
		if (possibleValues.remove(CONTOUR_LINES_DISABLED_VALUE)) {
			index = possibleValues.indexOf(preference.get());
			if (index >= 0) {
				index++;
			} else if (Algorithms.isEmpty(preference.get())) {
				index = 0;
			}
		}

		String[] values = possibleValues.toArray(new String[0]);
		String[] names = new String[values.length + 1];
		names[0] = AndroidUtils.getRenderingStringPropertyValue(activity, property.getDefaultValueDescription());

		for (int i = 0; i < values.length; i++) {
			names[i + 1] = AndroidUtils.getRenderingStringPropertyValue(activity, values[i]);
		}

		boolean nightMode = isNightMode(activity, app);
		String title = AndroidUtils.getRenderingStringPropertyDescription(app, property.getAttrName(), property.getName());
		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(title)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode))
				.setNegativeButton(R.string.shared_string_dismiss, null)
				.setOnDismissListener(dialog -> {
					if (callback != null) {
						callback.run();
					}
				});

		CustomAlert.showSingleSelection(dialogData, names, index, v -> {
			int which = (int) v.getTag();
			if (which == 0) {
				preference.set("");
			} else {
				preference.set(values[which - 1]);
			}
			activity.refreshMapComplete();
		});
	}

	private static boolean isNightMode(Activity activity, OsmandApplication app) {
		if (activity == null || app == null) {
			return false;
		}
		boolean usedOnMap = activity instanceof MapActivity;
		return app.getDaynightHelper().isNightMode(ThemeUsageContext.valueOf(usedOnMap));
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(ContourLinesAction.TYPE);
		quickActionTypes.add(TerrainAction.TYPE);
		quickActionTypes.add(TerrainColorSchemeAction.TYPE);
		return quickActionTypes;
	}

	@Override
	protected CommonPreference<String> registerRenderingPreference(@NonNull RenderingRuleProperty property) {
		String attrName = property.getAttrName();
		String defValue = CONTOUR_LINES_ATTR.equals(attrName) ? CONTOUR_LINES_DISABLED_VALUE : "";
		return registerRenderingPreference(attrName, defValue);
	}

	public void onIndexItemDownloaded(@NonNull IndexItem item, boolean updatingFile) {
		if (item.getType() == GEOTIFF_FILE) {
			updateHeightmap(updatingFile, item.getTargetFile(app).getAbsolutePath());
		}
	}

	private void updateHeightmap(boolean overwriteExistingFile, @NonNull String filePath) {
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (mapRendererContext != null) {
			if (overwriteExistingFile) {
				mapRendererContext.removeCachedHeightmapTiles(filePath);
			} else {
				mapRendererContext.updateCachedHeightmapTiles();
			}
		}
	}

	public void onPaletteChanged(@NonNull PaletteChangeEvent event) {
		if (event instanceof PaletteChangeEvent.Updated updated
				&& updated.getItem() instanceof PaletteItem.Gradient gradient) {
			// Terrain tiles are cached by palette file name, so an in-place edit must drop them
			String fileName = gradient.getSource().getFileName();
			MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
			if (mapRendererContext != null) {
				File file = new File(app.getAppPath(IndexConstants.CLR_PALETTE_DIR), fileName);
				mapRendererContext.removeCachedHeightmapTiles(file.getAbsolutePath());
			}
			if (fileName.equals(getTerrainMode().getMainFileName())) {
				app.runInUIThread(() -> updateLayers(app, null));
			}
		}
	}

	@Override
	public void updateMapPresentationEnvironment(@NonNull MapRendererContext mapRendererContext) {
		mapRendererContext.updateElevationConfiguration();
		mapRendererContext.recreateHeightmapProvider();
		MapRendererView rendererView = mapRendererContext.getMapRendererView();
		if (rendererView != null) {
			rendererView.setFlatEarth(!settings.SPHERICAL_MAP.get());
			rendererView.set3DBuildingsAlpha(settings.BUILDINGS_3D_ALPHA.get());
			rendererView.set3DBuildingsDetalization(settings.BUILDINGS_3D_VIEW_DISTANCE.get());
		}
		if (settings.ENABLE_3D_MAP_OBJECTS.get()) {
			mapRendererContext.recreate3DObjectsProvider();
		} else {
			mapRendererContext.reset3DObjectsProvider();
		}
	}

	public void getTerrainModeIcon(@NonNull String modeKey, @NonNull CollectColorPalletListener listener) {
		TerrainMode mode = TerrainMode.getByKey(modeKey);
		String fileName = mode.getMainFileName();
		CollectColorPalletTask.execute(app, fileName, listener);
	}

	public void apply3DBuildingsDetalization() {
		MapRendererContext ctx = NativeCoreContext.getMapRendererContext();
		if (ctx != null) {
			MapRendererView rendererView = ctx.getMapRendererView();
			if (rendererView != null) {
				rendererView.set3DBuildingsDetalization(settings.BUILDINGS_3D_VIEW_DISTANCE.get());
			}
		}
	}

	public void apply3DBuildingsAlpha(float alpha) {
		MapRendererContext ctx = NativeCoreContext.getMapRendererContext();
		if (ctx != null) {
			MapRendererView rendererView = ctx.getMapRendererView();
			if (rendererView != null) {
				rendererView.set3DBuildingsAlpha(alpha);
			}
		}
	}

	public void apply3DBuildingsColorStyle(Buildings3DColorType style) {
		BUILDINGS_3D_ENABLE_COLORING.set(false);
		settings.BUILDINGS_3D_COLOR_STYLE.set(style.getId());
		updateMapPresentationEnvironment();
	}

	@NonNull
	public Buildings3DColorType get3DBuildingsColorStyle() {
		int styleId = settings.BUILDINGS_3D_COLOR_STYLE.get();
		return Buildings3DColorType.Companion.getById(styleId);
	}

	public void apply3DBuildingsCustomColor(boolean nightMode, @ColorInt int color) {
		if (nightMode) {
			settings.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.set(color);
		} else {
			settings.BUILDINGS_3D_CUSTOM_DAY_COLOR.set(color);
		}
		updateMapPresentationEnvironment();
	}

	public int getBuildings3dCustomColor(boolean nightMode) {
		return nightMode
				? settings.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.get()
				: settings.BUILDINGS_3D_CUSTOM_DAY_COLOR.get();
	}

	private void updateMapPresentationEnvironment() {
		MapRendererContext mapRenderer = NativeCoreContext.getMapRendererContext();
		if (mapRenderer != null) {
			updateMapPresentationEnvironment(mapRenderer);
		}
	}
}
