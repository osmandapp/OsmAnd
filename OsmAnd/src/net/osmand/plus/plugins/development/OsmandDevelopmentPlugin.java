package net.osmand.plus.plugins.development;

import android.content.Context;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.github.mikephil.charting.charts.LineChart;

import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser;
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
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.AutoZoomBySpeedHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.ZoomLevelWidgetState;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
	private final StateChangedListener<Boolean> useRasterSQLiteDbListener;
	private final StateChangedListener<Boolean> symbolsDebugInfoListener;
	private final StateChangedListener<Boolean> batterySavingModeListener;

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
		pluginPreferences.add(settings.BATTERY_SAVING_MODE);
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

		useRasterSQLiteDbListener = change -> {
			SRTMPlugin plugin = getSrtmPlugin();
			if (plugin != null && plugin.isTerrainLayerEnabled()) {
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

		batterySavingModeListener = change -> {
			OsmandMapTileView mapView = app.getOsmandMap().getMapView();
			MapRendererView mapRenderer = mapView.getMapRenderer();
			if (mapRenderer != null) {
				mapView.applyBatterySavingModeSetting(mapRenderer);
			}
		};
		settings.BATTERY_SAVING_MODE.addListener(batterySavingModeListener);
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
				ZoomLevelWidgetState zoomLevelWidgetState = new ZoomLevelWidgetState(app, customId);
				return new ZoomLevelWidget(mapActivity, zoomLevelWidgetState, customId, widgetsPanel);
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
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		super.init(app, activity);
		avgStatsEnabled = true;
		avgStatsCollector();
		return true;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		OsmEditingPlugin osmPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmPlugin != null && osmPlugin.OSM_USE_DEV_URL.get()) {
			osmPlugin.OSM_USE_DEV_URL.set(false);
			app.getOsmOAuthHelper().resetAuthorization();
		}
		avgStatsEnabled = false;
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
	public void getAvailableGPXDataSetTypes(@NonNull GpxTrackAnalysis analysis, @NonNull List<GPXDataSetType[]> availableTypes) {
		AutoZoomBySpeedHelper.addAvailableGPXDataSetTypes(app, analysis, availableTypes);
	}

	@Nullable
	@Override
	public OrderedLineDataSet getOrderedLineDataSet(@NonNull LineChart chart, @NonNull GpxTrackAnalysis analysis, @NonNull GPXDataSetType graphType, @NonNull GPXDataSetAxisType chartAxisType, boolean calcWithoutGaps, boolean useRightAxis) {
		return AutoZoomBySpeedHelper.getOrderedLineDataSet(app, chart, analysis, graphType, chartAxisType,
				calcWithoutGaps, useRightAxis);
	}

	@Nullable
	@Override
	protected TrackPointsAnalyser getTrackPointsAnalyser() {
		return AutoZoomBySpeedHelper.getTrackPointsAnalyser(app);
	}

	private boolean avgStatsEnabled = false;
	private final int AVG_STATS_INTERVAL_SECONDS = 10;
	private final int AVG_STATS_LIFETIME_MINUTES = 15;
	private Handler avgStatsHandler = new Handler(Looper.getMainLooper());
	private List<AvgStatsEntry> avgStats = new ArrayList<>();

	protected class AvgStatsEntry {
		private long timestamp;
		protected float energyConsumption;
		protected float batteryLevel;
		protected float cpuBasic;
		protected float fps1k;
		protected float idle1k;
		protected float gpu1k;

		private AvgStatsEntry(OsmandApplication app) {
			MapRendererView renderer = app.getOsmandMap().getMapView().getMapRenderer();
			if (renderer != null) {
				this.timestamp = System.currentTimeMillis();

				this.fps1k = renderer.getFrameRateLast1K();
				this.idle1k = renderer.getIdleTimePartLast1K();
				this.gpu1k = renderer.getGPUWaitTimePartLast1K();

				float cpuBasic = renderer.getBasicThreadsCPULoad();
				this.cpuBasic = cpuBasic > 0 ? cpuBasic : 0; // NaN

				Intent batteryIntent;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
					batteryIntent = app.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED);
				} else {
					batteryIntent = app.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
				}
				if (batteryIntent != null) {
					int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
					int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
					this.batteryLevel = level != -1 && scale > 0 ? (float) (level * 100) / scale : 0;
				}

				final int EMULATOR_CURRENT_NOW_STUB = 900000;
				BatteryManager mBatteryManager = (BatteryManager) app.getSystemService(Context.BATTERY_SERVICE);
				int mBatteryCurrent = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
				this.energyConsumption = mBatteryCurrent == EMULATOR_CURRENT_NOW_STUB ? 0 : mBatteryCurrent;
			}
		}

		private AvgStatsEntry(List<AvgStatsEntry> allEntries, int periodMinutes) {
			if (!allEntries.isEmpty()) {
				this.batteryLevel = minuteBatteryUsage(allEntries, periodMinutes);
				this.fps1k = avgFloat(allEntries, periodMinutes, entry -> entry.fps1k);
				this.gpu1k = avgFloat(allEntries, periodMinutes, entry -> entry.gpu1k);
				this.idle1k = avgFloat(allEntries, periodMinutes, entry -> entry.idle1k);
				this.cpuBasic = avgFloat(allEntries, periodMinutes, entry -> entry.cpuBasic);
				this.energyConsumption = avgFloat(allEntries, periodMinutes, entry -> entry.energyConsumption);
			}
		}

		private float avgFloat(List<AvgStatsEntry> allEntries, int periodMinutes, Function<AvgStatsEntry, Float> getter) {
			long earliestTimestamp = System.currentTimeMillis() - periodMinutes * 60 * 1000;
			final float[] pairSumCounter = { 0, 0 }; // sum, counter
			allEntries.forEach(entry -> {
				if (entry.timestamp > 0 && entry.timestamp >= earliestTimestamp) {
					pairSumCounter[0] += getter.apply(entry);
					pairSumCounter[1]++;
				}
			});
			return pairSumCounter[1] > 0 ? (pairSumCounter[0] / pairSumCounter[1]) : 0;
		}

		private float minuteBatteryUsage(List<AvgStatsEntry> allEntries, int periodMinutes) {
			long earliestTimestamp = System.currentTimeMillis() - periodMinutes * 60 * 1000;
			for (int i = 0; i < allEntries.size(); i++) {
				long nowTimestamp = System.currentTimeMillis();
				long timestamp = allEntries.get(i).timestamp;
				if (timestamp > 0 && timestamp >= earliestTimestamp && nowTimestamp > timestamp) {
					float pastBattery = allEntries.get(i).batteryLevel;
					float freshBattery = allEntries.get(allEntries.size() - 1).batteryLevel;
					return (float) ((double) (freshBattery - pastBattery) / (double) (nowTimestamp - timestamp) * 1000 * 60);
				}
			}
			return 0;
		}
	}

	private void avgStatsCleanup() {
		long expirationTimestamp = System.currentTimeMillis() - (long)(AVG_STATS_LIFETIME_MINUTES * 60 * 1000);
		long delayedCleanupTimestamp = System.currentTimeMillis() - (long)(AVG_STATS_LIFETIME_MINUTES * 60 * 1000 * 2);
		if (!avgStats.isEmpty() && avgStats.get(0).timestamp < delayedCleanupTimestamp) {
			avgStats = avgStats.stream().filter(entry -> entry.timestamp >= expirationTimestamp).collect(Collectors.toList());
		}
	}

	private void avgStatsCollector() {
		if (avgStatsEnabled) {
			avgStatsCleanup();

			List<AvgStatsEntry> nextAvgStats = new ArrayList<>(avgStats);
			nextAvgStats.add(new AvgStatsEntry(app));
			avgStats = nextAvgStats;

			avgStatsHandler.postDelayed(this::avgStatsCollector, AVG_STATS_INTERVAL_SECONDS * 1000);
		}
	}

	protected AvgStatsEntry getAvgStats(int periodMinutes) {
		return new AvgStatsEntry(avgStats, periodMinutes);
	}
}
