package net.osmand.plus.plugins.development;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.github.mikephil.charting.charts.LineChart;

import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXTrackAnalysis.TrackPointsAnalyser;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.widget.CameraDistanceWidget;
import net.osmand.plus.plugins.development.widget.CameraTiltWidget;
import net.osmand.plus.plugins.development.widget.FPSTextInfoWidget;
import net.osmand.plus.plugins.development.widget.MemoryInfoWidget;
import net.osmand.plus.plugins.development.widget.TargetDistanceWidget;
import net.osmand.plus.plugins.development.widget.ZoomLevelWidget;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.actions.LocationSimulationAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.simulation.DashSimulateFragment;
import net.osmand.plus.views.AutoZoomBySpeedHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_BUILDS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_OSMAND_DEV;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_CAMERA_DISTANCE;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_CAMERA_TILT;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_FPS;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_MEMORY;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_TARGET_DISTANCE;
import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_ZOOM_LEVEL;

public class OsmandDevelopmentPlugin extends OsmandPlugin {

	public static final String DOWNLOAD_BUILD_NAME = "osmandToInstall.apk";

	public final OsmandPreference<Boolean> USE_RASTER_SQLITEDB;
	public final OsmandPreference<Boolean> SAVE_BEARING_TO_GPX;
	public final OsmandPreference<Boolean> SAVE_HEADING_TO_GPX;
	public final OsmandPreference<Boolean> SHOW_SYMBOLS_DEBUG_INFO;
	public final OsmandPreference<Boolean> ALLOW_SYMBOLS_DISPLAY_ON_TOP;
	public final OsmandPreference<Boolean> RAISE_ROUTES_ABOVE_RELIEF;
	public final OsmandPreference<Boolean> SHOW_TRANSPARENT_TRACES;
	private final StateChangedListener<Boolean> useRasterSQLiteDbListener;
	private final StateChangedListener<Boolean> symbolsDebugInfoListener;
	private final StateChangedListener<Boolean> routesAboveReliefListener;

	public OsmandDevelopmentPlugin(@NonNull OsmandApplication app) {
		super(app);

		ApplicationMode[] noAppMode = {};
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_FPS, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_MEMORY, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_CAMERA_TILT, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_CAMERA_DISTANCE, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_ZOOM_LEVEL, noAppMode);
		WidgetsAvailabilityHelper.regWidgetVisibility(DEV_TARGET_DISTANCE, noAppMode);

		pluginPreferences.add(settings.SAFE_MODE);
		pluginPreferences.add(settings.APPROX_SAFE_MODE);
		pluginPreferences.add(settings.DEBUG_RENDERING_INFO);
		pluginPreferences.add(settings.SHOULD_SHOW_FREE_VERSION_BANNER);
		pluginPreferences.add(settings.TRANSPARENT_STATUS_BAR);
		pluginPreferences.add(settings.MEMORY_ALLOCATED_FOR_ROUTING);
		pluginPreferences.add(settings.SHOW_INFO_ABOUT_PRESSED_KEY);

		USE_RASTER_SQLITEDB = registerBooleanPreference("use_raster_sqlitedb", false).makeGlobal().makeShared().cache();
		SAVE_BEARING_TO_GPX = registerBooleanPreference("save_bearing_to_gpx", false).makeGlobal().makeShared().cache();
		SAVE_HEADING_TO_GPX = registerBooleanPreference("save_heading_to_gpx", true).makeGlobal().makeShared().cache();
		SHOW_SYMBOLS_DEBUG_INFO = registerBooleanPreference("show_symbols_debug_info", false).makeGlobal().makeShared().cache();
		ALLOW_SYMBOLS_DISPLAY_ON_TOP = registerBooleanPreference("allow_symbols_display_on_top", false).makeGlobal().makeShared().cache();
		RAISE_ROUTES_ABOVE_RELIEF = registerBooleanPreference("raise_routes_above_relief", false).makeGlobal().makeShared().cache();
		SHOW_TRANSPARENT_TRACES = registerBooleanPreference("show_transparent_traces", false).makeGlobal().makeShared().cache();

		useRasterSQLiteDbListener = change -> {
			SRTMPlugin plugin = getSrtmPlugin();
			if (plugin != null && plugin.isTerrainLayerEnabled() && (plugin.isHillshadeMode() || plugin.isSlopeMode())) {
				plugin.updateLayers(app, null);
			}
		};
		USE_RASTER_SQLITEDB.addListener(useRasterSQLiteDbListener);

		symbolsDebugInfoListener = change -> {
			OsmandMapTileView mapView = app.getOsmandMap().getMapView();
			MapRendererView mapRenderer = mapView.getMapRenderer();
			if (mapRenderer != null) {
				mapView.applyDebugSettings(mapRenderer);
			}
		};
		SHOW_SYMBOLS_DEBUG_INFO.addListener(symbolsDebugInfoListener);
		ALLOW_SYMBOLS_DISPLAY_ON_TOP.addListener(symbolsDebugInfoListener);

		routesAboveReliefListener = change -> {
			OsmandMapTileView mapView = app.getOsmandMap().getMapView();
			MapActivity mapActivity = mapView.getMapActivity();
			if (mapActivity != null)
				mapActivity.restart();
		};
		RAISE_ROUTES_ABOVE_RELIEF.addListener(routesAboveReliefListener);
		SHOW_TRANSPARENT_TRACES.addListener(routesAboveReliefListener);

	}

	@Override
	public String getId() {
		return PLUGIN_OSMAND_DEV;
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.osmand_development_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.debugging_and_development);
	}

	@Override
	public void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
		if (Version.isDeveloperVersion(mapActivity.getMyApplication())) {
			Class<?> contributionVersionActivityClass = null;
			try {
				ClassLoader classLoader = OsmandDevelopmentPlugin.class.getClassLoader();
				if (classLoader != null) {
					contributionVersionActivityClass = classLoader.loadClass("net.osmand.plus.activities.ContributionVersionActivity");
				}
			} catch (ClassNotFoundException ignore) {
			}
			Class<?> activityClass = contributionVersionActivityClass;
			if (activityClass != null) {
				helper.addItem(new ContextMenuItem(DRAWER_BUILDS_ID)
						.setTitleId(R.string.version_settings, mapActivity)
						.setIcon(R.drawable.ic_action_apk)
						.setListener((uiAdapter, view, item, isChecked) -> {
							Intent mapIntent = new Intent(mapActivity, activityClass);
							mapActivity.startActivityForResult(mapIntent, 0);
							return true;
						}));
			}
		}
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos, @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);

		MapWidget fpsWidget = createMapWidgetForParams(mapActivity, DEV_FPS);
		widgetsInfos.add(creator.createWidgetInfo(fpsWidget));

		MapWidget cameraTiltWidget = createMapWidgetForParams(mapActivity, DEV_CAMERA_TILT);
		widgetsInfos.add(creator.createWidgetInfo(cameraTiltWidget));

		MapWidget cameraDistanceWidget = createMapWidgetForParams(mapActivity, DEV_CAMERA_DISTANCE);
		widgetsInfos.add(creator.createWidgetInfo(cameraDistanceWidget));

		MapWidget zoomLevelWidget = createMapWidgetForParams(mapActivity, DEV_ZOOM_LEVEL);
		widgetsInfos.add(creator.createWidgetInfo(zoomLevelWidget));

		MapWidget targetDistanceWidget = createMapWidgetForParams(mapActivity, DEV_TARGET_DISTANCE);
		widgetsInfos.add(creator.createWidgetInfo(targetDistanceWidget));

		MapWidget memoryWidget = createMapWidgetForParams(mapActivity, DEV_MEMORY);
		widgetsInfos.add(creator.createWidgetInfo(memoryWidget));
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		switch (widgetType) {
			case DEV_FPS:
				return new FPSTextInfoWidget(mapActivity, customId, widgetsPanel);
			case DEV_CAMERA_TILT:
				return new CameraTiltWidget(mapActivity, customId, widgetsPanel);
			case DEV_CAMERA_DISTANCE:
				return new CameraDistanceWidget(mapActivity, customId, widgetsPanel);
			case DEV_ZOOM_LEVEL:
				return new ZoomLevelWidget(mapActivity, customId, widgetsPanel);
			case DEV_TARGET_DISTANCE:
				return new TargetDistanceWidget(mapActivity, customId, widgetsPanel);
			case DEV_MEMORY:
				return new MemoryInfoWidget(mapActivity, customId, widgetsPanel);
		}
		return null;
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.DEVELOPMENT_SETTINGS;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_laptop;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.osmand_development);
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashSimulateFragment.FRAGMENT_DATA;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		OsmEditingPlugin osmPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmPlugin != null && osmPlugin.OSM_USE_DEV_URL.get()) {
			osmPlugin.OSM_USE_DEV_URL.set(false);
			app.getOsmOAuthHelper().resetAuthorization();
		}
		super.disable(app);
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(LocationSimulationAction.TYPE);
		return quickActionTypes;
	}

	public boolean generateTerrainFrom3DMaps() {
		return app.useOpenGlRenderer() && !USE_RASTER_SQLITEDB.get();
	}

	@Nullable
	private SRTMPlugin getSrtmPlugin() {
		return PluginsHelper.getEnabledPlugin(SRTMPlugin.class);
	}

	@Override
	public void getAvailableGPXDataSetTypes(@NonNull GPXTrackAnalysis analysis, @NonNull List<GPXDataSetType[]> availableTypes) {
		AutoZoomBySpeedHelper.addAvailableGPXDataSetTypes(app, analysis, availableTypes);
	}

	@Nullable
	@Override
	public OrderedLineDataSet getOrderedLineDataSet(@NonNull LineChart chart, @NonNull GPXTrackAnalysis analysis, @NonNull GPXDataSetType graphType, @NonNull GPXDataSetAxisType chartAxisType, boolean calcWithoutGaps, boolean useRightAxis) {
		return AutoZoomBySpeedHelper.getOrderedLineDataSet(app, chart, analysis, graphType, chartAxisType,
				calcWithoutGaps, useRightAxis);
	}

	@Nullable
	@Override
	protected TrackPointsAnalyser getTrackPointsAnalyser() {
		return AutoZoomBySpeedHelper.getTrackPointsAnalyser(app);
	}
}