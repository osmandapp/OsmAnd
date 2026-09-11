package net.osmand.plus.plugins.aistracker;

import net.osmand.plus.render.RendererRegistry;
import net.osmand.shared.aistracker.AisObject;

import static net.osmand.plus.NavigationService.USED_BY_AIS;
import static net.osmand.plus.notifications.OsmandNotification.NotificationType.AIS;
import static net.osmand.plus.settings.fragments.SettingsScreenType.AIS_SETTINGS;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.shared.aistracker.AisMessageListener;
import net.osmand.shared.aistracker.AisDataListener;
import net.osmand.shared.aistracker.AisLocation;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRuleProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/*
 *   This plugin receives AIS positions and other AIS data via network (NMEA protocol)
 *   from an AIS receiver/decoder and displays symbols at the map at the vessel position
 */
public class AisTrackerPlugin extends OsmandPlugin {

	static private final int SIMULATED_LATENCY_TIME_MS = 100;

	private final AisImagesCache aisImagesCache;
	private final AisSimulationProvider simulationProvider = new AisSimulationProvider(this);
	private AisTrackerLayer layer = null;
	private AisMessageListener aisListener;
	private final AisDataManager aisDataManager = new AisDataManager();

	private static final String COMPONENT = "net.osmand.aistrackerPlugin";
	public static final String AISTRACKER_ID = "osmand.aistracker";
	public static final String AIS_NMEA_PROTOCOL_ID = "ais_nmea_protocol"; // see xml/ais_settings.xml
	public static final String AIS_NMEA_IP_ADDRESS_ID = "ais_address_nmea_server"; // see xml/ais_settings.xml
	public static final String AIS_NMEA_TCP_PORT_ID = "ais_port_nmea_server"; // see xml/ais_settings.xml
	public static final String AIS_NMEA_UDP_PORT_ID = "ais_port_nmea_local"; // see xml/ais_settings.xml
	public static final String AIS_OBJ_LOST_TIMEOUT_ID = "ais_object_lost_timeout"; // see xml/ais_settings.xml
	public static final String AIS_SHIP_LOST_TIMEOUT_ID = "ais_ship_lost_timeout"; // see xml/ais_settings.xml
	public static final String AIS_CPA_WARNING_TIME_ID = "ais_cpa_warning_time"; // see xml/ais_settings.xml
	public static final String AIS_CPA_WARNING_DISTANCE_ID = "ais_cpa_warning_distance"; // see xml/ais_settings.xml
	public static final String AIS_OWN_MMSI_ID = "ais_own_mmsi"; // see xml/ais_settings.xml
    public static final String AIS_DISPLAY_OWN_POSITION_ID = "ais_display_own_position"; // see xml/ais_settings.xml
    public static final String AIS_RECEIVE_IN_BACKGROUND_ID = "ais_receive_in_background"; // see xml/ais_settings.xml
	public final CommonPreference<Integer> AIS_NMEA_PROTOCOL;
	public static final int AIS_NMEA_PROTOCOL_UDP = 0;
	public static final int AIS_NMEA_PROTOCOL_TCP = 1;
	public final CommonPreference<String> AIS_NMEA_IP_ADDRESS;
	private static final String AIS_NMEA_DEFAULT_IP = "192.168.200.16";
	public final CommonPreference<Integer> AIS_NMEA_TCP_PORT;
	private static final Integer AIS_NMEA_DEFAULT_TCP_PORT = 4001;
	public final CommonPreference<Integer> AIS_NMEA_UDP_PORT;
	private static final Integer AIS_NMEA_DEFAULT_UDP_PORT = 10110;
	/* after this time of missing AIS signal the object is outdated and can be removed: */
	public final CommonPreference<Integer> AIS_OBJ_LOST_TIMEOUT;
	public static final Integer AIS_OBJ_LOST_DEFAULT_TIMEOUT = 7;
	/* after this time of missing AIS signal the vessel symbol can change to mark "lost": */
	public final CommonPreference<Integer> AIS_SHIP_LOST_TIMEOUT;
	public static final Integer AIS_SHIP_LOST_DEFAULT_TIMEOUT = 4;
	public final CommonPreference<Integer> AIS_CPA_WARNING_TIME; // in minutes
	public static final Integer AIS_CPA_DEFAULT_WARNING_TIME = 0;
	public final CommonPreference<Float> AIS_CPA_WARNING_DISTANCE; // in miles
	public static final Float AIS_CPA_WARNING_DEFAULT_DISTANCE = 1.0f;
	public final CommonPreference<Integer> AIS_OWN_MMSI;
	public static final Integer AIS_DEFAULT_OWN_MMSI = 0;
	public final CommonPreference<Boolean> AIS_DISPLAY_OWN_POSITION;
	public static final Boolean AIS_DISPLAY_OWN_POSITION_DEFAULT = false;
    public final CommonPreference<Boolean> AIS_RECEIVE_IN_BACKGROUND;
    public static final Boolean AIS_RECEIVE_IN_BACKGROUND_DEFAULT = false;
	public static final String AIS_URL_SOURCES_ID = "ais_url_sources"; // see xml/ais_settings.xml
	public static final String AIS_SHOW_SHIPS_ID = "ais_show_ships"; // see xml/ais_settings.xml
	public static final String AIS_SHOW_PLANES_ID = "ais_show_planes"; // see xml/ais_settings.xml
	public final CommonPreference<String> AIS_URL_SOURCES;
	public final CommonPreference<Boolean> AIS_SHOW_SHIPS;
	public final CommonPreference<Boolean> AIS_SHOW_PLANES;
	// Anonymous OpenSky access is capped at ~400 requests/day; keep this conservative by default
	// until a user-run aggregator with its own key/limits is configured.
	private static final long PLANE_POLL_INTERVAL_MS = 60_000L;
	private Timer planePollTimer;

	/* timestamp of last AIS message received for all instances: */
	private long lastMessageReceived = 0;
	private Location fakeOwnPosition = null; // used for test purposes to fake own position

	private final StateChangedListener<String> addrPrefListener = change -> restartNetworkListener();
	private final StateChangedListener<Integer> protocolPortPrefListener = change -> restartNetworkListener();
	private final StateChangedListener<Boolean> receiveInBackgroundPrefListener = enabled -> {
		if (enabled) {
			updateAisBackgroundService();
		} else {
			stopAisBackgroundService();
			if (!settings.MAP_ACTIVITY_ENABLED) {
				stopAisListener();
			}
		}
	};

	public class AisDataManager implements AisDataListener {

		private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(AisDataManager.class);

		private static final int AIS_OBJECT_LIST_COUNTER_MAX = 20_000;
		private final Map<Integer, AisObject> objects = new HashMap<>();
		private Timer cleanupTimer;

		public interface AisObjectListener {
			void onAisObjectReceived(@NonNull AisObject ais);
			void onAisObjectRemoved(@NonNull AisObject ais);
		}


		private void initTimer() {
			TimerTask timerTask = new TimerTask() {
				@Override
				public void run() {
					removeLostObjects();
				}
			};
			this.cleanupTimer = new Timer();
			cleanupTimer.schedule(timerTask, 20000, 30000);
		}

		private void deinitTimer() {
			if (cleanupTimer != null) {
				cleanupTimer.cancel();
				cleanupTimer = null;
			}
		}

		private void reinitTimer() {
			deinitTimer();
			initTimer();
		}

		public void startUpdates() {
			reinitTimer();
		}

		public void stopUpdates() {
			deinitTimer();
		}

		public synchronized void cleanupResources() {
			deinitTimer();
			objects.clear();
		}

		@Override
		public synchronized void onAisObjectReceived(@NonNull AisObject ais) {
			AisObject obj = objects.get(ais.getMmsi());
			if (obj != null) {
				obj.set(ais);
			} else {
				obj = new AisObject(ais);
				objects.put(ais.getMmsi(), obj);
			}
			if (objects.size() > AIS_OBJECT_LIST_COUNTER_MAX) {
				removeOldestAisObject(objects);
			}
			if (objects.get(obj.getMmsi()) == obj) {
				AisTrackerPlugin.this.onAisObjectReceived(obj);
			}
		}

		@Override
		public void onNmeaLocationReceived(@NonNull AisLocation location) {
		}

		@NonNull
		public synchronized List<AisObject> getAisObjects() {
			return new ArrayList<>(objects.values());
		}

		public synchronized void removeLostObjects() {
			for (Iterator<Map.Entry<Integer, AisObject>> iterator = objects.entrySet().iterator(); iterator.hasNext(); ) {
				AisObject obj = iterator.next().getValue();
				if (obj.isLost(AisTrackerPlugin.this.getMaxObjectAgeInMinutes())) {
					LOG.debug("Remove AIS object with MMSI " + obj.getMmsi());
					iterator.remove();
					AisTrackerPlugin.this.onAisObjectRemoved(obj);
				}
			}
		}

		private void removeOldestAisObject(@NonNull Map<Integer, AisObject> objects) {
			LOG.debug("Remove oldest ais object");
			long oldestTimeStamp = System.currentTimeMillis();
			AisObject oldest = null;
			for (AisObject ais : objects.values()) {
				long timeStamp = ais.getLastUpdate();
				if (timeStamp <= oldestTimeStamp) {
					oldestTimeStamp = timeStamp;
					oldest = ais;
				}
			}
			if (oldest != null) {
				LOG.debug("Remove AIS object with MMSI " + oldest.getMmsi());
				objects.remove(oldest.getMmsi(), oldest);
				AisTrackerPlugin.this.onAisObjectRemoved(oldest);
			}
		}
	}

	public AisTrackerPlugin(@NonNull OsmandApplication app) {
		super(app);
		aisImagesCache = new AisImagesCache(app);

		/* "ais_nmea_protocol" etc. is a reference to the content of xml/ais_settings.xml */
		AIS_NMEA_PROTOCOL = registerIntPreference(AIS_NMEA_PROTOCOL_ID, AIS_NMEA_PROTOCOL_UDP);
		AIS_NMEA_IP_ADDRESS = registerStringPreference(AIS_NMEA_IP_ADDRESS_ID, AIS_NMEA_DEFAULT_IP);
		AIS_NMEA_TCP_PORT = registerIntPreference(AIS_NMEA_TCP_PORT_ID, AIS_NMEA_DEFAULT_TCP_PORT);
		AIS_NMEA_UDP_PORT = registerIntPreference(AIS_NMEA_UDP_PORT_ID, AIS_NMEA_DEFAULT_UDP_PORT);
		AIS_OBJ_LOST_TIMEOUT = registerIntPreference(AIS_OBJ_LOST_TIMEOUT_ID, AIS_OBJ_LOST_DEFAULT_TIMEOUT);
		AIS_SHIP_LOST_TIMEOUT = registerIntPreference(AIS_SHIP_LOST_TIMEOUT_ID, AIS_SHIP_LOST_DEFAULT_TIMEOUT);
		AIS_CPA_WARNING_TIME = registerIntPreference(AIS_CPA_WARNING_TIME_ID, AIS_CPA_DEFAULT_WARNING_TIME);
		AIS_CPA_WARNING_DISTANCE = registerFloatPreference(AIS_CPA_WARNING_DISTANCE_ID, AIS_CPA_WARNING_DEFAULT_DISTANCE);
		AIS_OWN_MMSI = registerIntPreference(AIS_OWN_MMSI_ID, AIS_DEFAULT_OWN_MMSI);
		AIS_DISPLAY_OWN_POSITION = registerBooleanPreference(AIS_DISPLAY_OWN_POSITION_ID, AIS_DISPLAY_OWN_POSITION_DEFAULT);
		AIS_RECEIVE_IN_BACKGROUND = registerBooleanPreference(AIS_RECEIVE_IN_BACKGROUND_ID, AIS_RECEIVE_IN_BACKGROUND_DEFAULT);
		AIS_URL_SOURCES = registerStringPreference(AIS_URL_SOURCES_ID, "").makeGlobal();
		AIS_SHOW_SHIPS = registerBooleanPreference(AIS_SHOW_SHIPS_ID, true).makeGlobal();
		AIS_SHOW_PLANES = registerBooleanPreference(AIS_SHOW_PLANES_ID, true).makeGlobal();
		AIS_NMEA_IP_ADDRESS.addListener(addrPrefListener);
		AIS_NMEA_PROTOCOL.addListener(protocolPortPrefListener);
		AIS_NMEA_TCP_PORT.addListener(protocolPortPrefListener);
		AIS_NMEA_UDP_PORT.addListener(protocolPortPrefListener);
		AIS_RECEIVE_IN_BACKGROUND.addListener(receiveInBackgroundPrefListener);
	}

	@Override
	public boolean isMarketPlugin() {
		return true;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		stopAisListener();
		super.disable(app);
	}

	@Override
	public String getComponentId1() {
		return COMPONENT;
	}

	@Override
	public String getComponentId2() {
		return "net.osmand.dev"; // for test purposes to enable logcat at adb connected physical device
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.plugin_ais_tracker_description).concat("\n\n")
				.concat(app.getString(R.string.plugin_ais_tracker_disclaimer));
	}

	@Override
	public String getName() {
		return app.getString(R.string.plugin_ais_tracker_name);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.mm_sport_sailing;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.ais_map);
	}

	@Override
	public List<ApplicationMode> getAddedAppModes() {
		return Collections.singletonList(ApplicationMode.BOAT);
	}

	@Override
	public List<String> getRendererNames() {
		return Collections.singletonList(RendererRegistry.NAUTICAL_RENDER);
	}

	@Override
	public String getId() {
		return AISTRACKER_ID;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			startAisNetworkListener();
		} else {
			stopAisListener();
		}
	}

	@NonNull
	public AisImagesCache getAisImagesCache() {
		return aisImagesCache;
	}

	public int getMaxObjectAgeInMinutes() {
		return AIS_OBJ_LOST_TIMEOUT.get();
	}

	public int getVesselLostTimeoutInMinutes() {
		return AIS_SHIP_LOST_TIMEOUT.get();
	}

	public int getCpaWarningTime() {
		return AIS_CPA_WARNING_TIME.get();
	}

	public float getCpaWarningDistance() {
		return AIS_CPA_WARNING_DISTANCE.get();
	}

	public Location getOwnPosition() { // used to calculate distances, CPA etc.
		return fakeOwnPosition != null ? fakeOwnPosition : app.getLocationProvider().getLastKnownLocation();
	}

	public void fakeOwnPosition(Location fakePosition) { // used for test purposes
		fakeOwnPosition = fakePosition;
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return AIS_SETTINGS;
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.ais_address_settings_description);
	}

	@Override
	public void mapActivityResume(@NonNull MapActivity activity) {
		if (!restartStalledTcpConnection()) {
			if (aisListener == null) {
				startAisNetworkListener();
			}
		}
		updateAisBackgroundService();
		if (AIS_RECEIVE_IN_BACKGROUND.get()) {
			AndroidUtils.requestNotificationPermissionIfNeeded(activity);
		}
		startPlanePolling();
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
		if (!AIS_RECEIVE_IN_BACKGROUND.get()) {
			stopAisListener();
		} else {
			updateAisBackgroundService();
			app.runInUIThread(this::stopAisListenerIfBackgroundServiceFailed, 1500);
		}
		stopPlanePolling();
	}

	@NonNull
	public List<AisUrlSource> getUrlSources() {
		return AisUrlSource.parseList(AIS_URL_SOURCES.get());
	}

	public void addOrUpdateUrlSource(@NonNull AisUrlSource source) {
		List<AisUrlSource> sources = getUrlSources();
		boolean replaced = false;
		for (int i = 0; i < sources.size(); i++) {
			if (sources.get(i).id.equals(source.id)) {
				sources.set(i, source);
				replaced = true;
				break;
			}
		}
		if (!replaced) {
			sources.add(source);
		}
		AIS_URL_SOURCES.set(AisUrlSource.serializeList(sources));
		restartPlanePolling();
	}

	public void removeUrlSource(@NonNull String id) {
		List<AisUrlSource> sources = getUrlSources();
		sources.removeIf(source -> source.id.equals(id));
		AIS_URL_SOURCES.set(AisUrlSource.serializeList(sources));
		restartPlanePolling();
	}

	public void setSourceEnabled(@NonNull String id, boolean enabled) {
		List<AisUrlSource> sources = getUrlSources();
		for (int i = 0; i < sources.size(); i++) {
			if (sources.get(i).id.equals(id)) {
				sources.set(i, sources.get(i).withEnabled(enabled));
				break;
			}
		}
		AIS_URL_SOURCES.set(AisUrlSource.serializeList(sources));
		restartPlanePolling();
		AisTrackerLayer currentLayer = layer;
		if (currentLayer != null) {
			currentLayer.refreshTypeFilter();
		}
	}

	@NonNull
	private List<AisUrlSource> getPlaneSources() {
		List<AisUrlSource> planes = new ArrayList<>();
		for (AisUrlSource source : getUrlSources()) {
			if (source.type == AisUrlSource.Type.PLANES && source.enabled) {
				planes.add(source);
			}
		}
		return planes;
	}

	public void feedExternalAisObject(@NonNull AisObject ais) {
		aisDataManager.onAisObjectReceived(ais);
	}

	public void restartPlanePolling() {
		stopPlanePolling();
		startPlanePolling();
	}

	private void startPlanePolling() {
		stopPlanePolling();
		if (getPlaneSources().isEmpty()) {
			return;
		}
		planePollTimer = new Timer();
		planePollTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				pollPlaneSources();
			}
		}, 0, PLANE_POLL_INTERVAL_MS);
	}

	private void stopPlanePolling() {
		if (planePollTimer != null) {
			planePollTimer.cancel();
			planePollTimer = null;
		}
	}

	private void pollPlaneSources() {
		if (!AIS_SHOW_PLANES.get()) {
			return;
		}
		List<AisObject> received = new ArrayList<>();
		for (AisUrlSource source : getPlaneSources()) {
			received.addAll(AisPlaneDataFetcher.fetch(app, source));
		}
		if (!received.isEmpty()) {
			app.runInUIThread(() -> {
				for (AisObject ais : received) {
					feedExternalAisObject(ais);
				}
			});
		}
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (isActive()) {
			if (layer == null) {
				Log.d("AisTrackerPlugin", "call registerLayers()");
				registerLayers(context, mapActivity);
			}
			if (!mapView.getLayers().contains(layer)) {
				mapView.addLayer(layer, 3.5f);
			}
		} else {
			if (layer != null) {
				mapView.removeLayer(layer);
				layer = null;
				mapView.refreshMap();
			}
		}
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (layer == null) {
			Log.d("AisTrackerPlugin", "new AisTrackerLayer");
			layer = new AisTrackerLayer(context);
			app.getOsmandMap().getMapView().addLayer(layer, 3.5f);
		} else {
			Log.d("AisTrackerPlugin", "AisTrackerLayer already exists");
			OsmandMapTileView mapView = app.getOsmandMap().getMapView();
			if (!mapView.getLayers().contains(layer)) {
				mapView.addLayer(layer, 3.5f);
			}
		}
	}

	@Nullable
	public AisTrackerLayer getLayer() {
		return layer;
	}

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter,
	                                               @NonNull MapActivity mapActivity,
	                                               List<RenderingRuleProperty> customRules) {
		if (!isEnabled()) {
			return;
		}
		addTypeToggleItem(adapter, mapActivity, AISTRACKER_ID + ".show_ships",
				R.string.ais_show_ships, R.drawable.mm_sport_sailing, AIS_SHOW_SHIPS);
		addTypeToggleItem(adapter, mapActivity, AISTRACKER_ID + ".show_planes",
				R.string.ais_show_planes, R.drawable.ic_action_aircraft, AIS_SHOW_PLANES);
		if (!getUrlSources().isEmpty()) {
			addSourcesPickerItem(adapter, mapActivity);
		}
	}

	private void addSourcesPickerItem(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity) {
		OnRowItemClick listener = new OnRowItemClick() {
			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
			                              @NonNull View view, @NonNull ContextMenuItem item) {
				showSourcesPickerDialog(mapActivity);
				return false;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter, @Nullable View view,
			                                  @NonNull ContextMenuItem item, boolean isChecked) {
				return false;
			}
		};
		adapter.addItem(new ContextMenuItem(AISTRACKER_ID + ".sources_picker")
				.setTitleId(R.string.ais_url_sources, mapActivity)
				.setIcon(R.drawable.ic_action_layers)
				.setListener(listener));
	}

	private void showSourcesPickerDialog(@NonNull MapActivity mapActivity) {
		List<AisUrlSource> sources = getUrlSources();
		CharSequence[] items = new CharSequence[sources.size()];
		boolean[] checked = new boolean[sources.size()];
		for (int i = 0; i < sources.size(); i++) {
			AisUrlSource source = sources.get(i);
			items[i] = source.name + " (" + source.type.name().toLowerCase() + ")";
			checked[i] = source.enabled;
		}
		Context themedContext = UiUtilities.getThemedContext(mapActivity, mapActivity.isNightMode());
		new AlertDialog.Builder(themedContext)
				.setTitle(R.string.ais_url_sources)
				.setMultiChoiceItems(items, checked, (dialog, which, isChecked) ->
						setSourceEnabled(sources.get(which).id, isChecked))
				.setPositiveButton(R.string.shared_string_ok, null)
				.show();
	}

	private void addTypeToggleItem(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity,
	                               @NonNull String id, int titleId, int iconId,
	                               @NonNull CommonPreference<Boolean> pref) {
		ItemClickListener listener = new ItemClickListener() {
			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter, @Nullable View view,
			                                  @NonNull ContextMenuItem item, boolean isChecked) {
				pref.set(!pref.get());
				AisTrackerLayer currentLayer = layer;
				if (currentLayer != null) {
					currentLayer.refreshTypeFilter();
				}
				item.setSelected(pref.get());
				item.setColor(app, pref.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
				if (uiAdapter != null) {
					uiAdapter.onDataSetChanged();
				}
				return false;
			}
		};
		adapter.addItem(new ContextMenuItem(id)
				.setTitleId(titleId, mapActivity)
				.setSelected(pref.get())
				.setColor(app, pref.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(iconId)
				.setListener(listener));
	}

	public void onAisObjectReceived(@NonNull AisObject ais) {
		lastMessageReceived = ais.getLastUpdate();
		AisTrackerLayer layer = this.layer;
		if (layer != null) {
			layer.onAisObjectReceived(ais);
		}
	}

	public void onAisObjectRemoved(@NonNull AisObject ais) {
		AisTrackerLayer layer = this.layer;
		if (layer != null) {
			layer.onAisObjectRemoved(ais);
		}
	}

	public void startAisSimulation(@NonNull File file) {
		stopAisListener();
		aisDataManager.cleanupResources();
		aisListener = new AisMessageSimulationListener(aisDataManager, file, SIMULATED_LATENCY_TIME_MS);
		aisDataManager.startUpdates();
		updateAisBackgroundService();
	}

	private void startAisNetworkListener() {
		int proto = AIS_NMEA_PROTOCOL.get();
		if (proto == AisTrackerPlugin.AIS_NMEA_PROTOCOL_UDP) {
			aisDataManager.stopUpdates();
			aisListener = new AisMessageListener(aisDataManager, AIS_NMEA_UDP_PORT.get());
			aisDataManager.startUpdates();
		} else if (proto == AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP) {
			aisDataManager.stopUpdates();
			aisListener = new AisMessageListener(aisDataManager, AIS_NMEA_IP_ADDRESS.get(), AIS_NMEA_TCP_PORT.get());
			aisDataManager.startUpdates();
		}
		updateAisBackgroundService();
	}

	private void stopAisListener() {
		if (aisListener != null) {
			aisListener.stopListener();
			aisListener = null;
		}
		aisDataManager.stopUpdates();
		stopAisBackgroundService();
	}

	private void updateAisBackgroundService() {
		if (isActive() && AIS_RECEIVE_IN_BACKGROUND.get() && aisListener != null) {
			app.startNavigationService(USED_BY_AIS);
			app.getNotificationHelper().refreshNotification(AIS);
		} else {
			stopAisBackgroundService();
		}
	}

	private void stopAisBackgroundService() {
		NavigationService navigationService = app.getNavigationService();
		if (navigationService != null && navigationService.isUsedBy(USED_BY_AIS)) {
			navigationService.stopIfNeeded(app, USED_BY_AIS);
		}
	}

	private void stopAisListenerIfBackgroundServiceFailed() {
		if (!settings.MAP_ACTIVITY_ENABLED && AIS_RECEIVE_IN_BACKGROUND.get()
				&& aisListener != null && !isAisBackgroundServiceRunning()) {
			stopAisListener();
		}
	}

	private boolean isAisBackgroundServiceRunning() {
		NavigationService navigationService = app.getNavigationService();
		return navigationService != null && navigationService.isUsedBy(USED_BY_AIS);
	}

	/* this method restarts the TCP listeners after a "resume" event (the smartphone resumed
	 *  from sleep or from switched off state): in this case the TCP connection might be broken,
	 *  but the sockets are still (logically) open.
	 *  as additional indication of a broken TCP connection it is checked whether any AIS message
	 *  was received in the last 20 seconds  */
	private boolean restartStalledTcpConnection() {
		if (aisListener != null) {
			if (aisListener.checkTcpSocket()) {
				if (((System.currentTimeMillis() - getAndUpdateLastMessageReceived()) / 1000) > 20) {
					Log.d("AisTrackerLayer", "restartStalledTcpConnection(): restart TCP socket");
					restartNetworkListener();
					return true;
				}
			}
		}
		return false;
	}

	public void restartNetworkListener() {
		stopAisListener();
		startAisNetworkListener();
	}

	@NonNull
	public List<AisObject> getAisObjects() {
		return aisDataManager.getAisObjects();
	}

	public long getLastMessageReceived() {
		return lastMessageReceived;
	}

	public long getAndUpdateLastMessageReceived() {
		long timestamp = getLastMessageReceived();
		lastMessageReceived = System.currentTimeMillis();
		return timestamp;
	}
}
