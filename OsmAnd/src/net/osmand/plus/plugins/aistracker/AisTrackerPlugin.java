package net.osmand.plus.plugins.aistracker;

import net.osmand.plus.render.RendererRegistry;
import net.osmand.shared.aistracker.AisObject;

import static net.osmand.plus.NavigationService.USED_BY_AIS;
import static net.osmand.plus.notifications.OsmandNotification.NotificationType.AIS;
import static net.osmand.plus.settings.fragments.SettingsScreenType.AIS_SETTINGS;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.shared.aistracker.AisConnectionListener;
import net.osmand.shared.aistracker.AisMessageListener;
import net.osmand.shared.aistracker.AisDataListener;
import net.osmand.shared.aistracker.AisLocation;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;
import net.osmand.plus.views.OsmandMapTileView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 *   This plugin receives AIS positions and other AIS data via network (NMEA protocol)
 *   from an AIS receiver/decoder and displays symbols at the map at the vessel position
 */
public class AisTrackerPlugin extends OsmandPlugin {

	static private final int SIMULATED_LATENCY_TIME_MS = 100;
	/* no NMEA sentence within this time while the socket is open means "No data received": */
	private static final long AIS_NO_DATA_TIMEOUT_MS = 60_000;
	private static final long AIS_NO_DATA_CHECK_INTERVAL_MS = 5_000;
	/* the phone GPS stays ignored for this long after every position from the NMEA stream, so a
	 * stream that stops hands the position back instead of freezing it: */
	private static final long IGNORE_PHONE_LOCATION_TIMEOUT_MS = 10_000;
	/* no position sentence within this time means the stream carries no position at all: */
	private static final long NMEA_POSITION_TIMEOUT_MS = 30_000;
	/* GGA and GLL carry no speed and course, RMC does; a receiver interleaves them, so the last
	 * speed and course are kept for this long instead of dropping them every other sentence: */
	private static final long NMEA_MOTION_KEEP_MS = 3_000;
	/* every fix runs the whole location pipeline (track recording, routing, plugins), and a
	 * receiver sends several position sentences per fix: */
	private static final long NMEA_LOCATION_MIN_INTERVAL_MS = 1_000;
	public static final String NMEA_LOCATION_PROVIDER = "nmea";

	private final AisImagesCache aisImagesCache;
	private final AisSimulationProvider simulationProvider = new AisSimulationProvider(this);
	private AisTrackerLayer layer = null;
	private AisMessageListener aisListener;
	private final AisDataManager aisDataManager = new AisDataManager();

	private static final String COMPONENT = "net.osmand.aistrackerPlugin";
	public static final String AISTRACKER_ID = "osmand.aistracker";
	public static final String AIS_NMEA_PROTOCOL_ID = "ais_nmea_protocol";
	public static final String AIS_NMEA_IP_ADDRESS_ID = "ais_address_nmea_server";
	public static final String AIS_NMEA_TCP_PORT_ID = "ais_port_nmea_server";
	public static final String AIS_NMEA_UDP_PORT_ID = "ais_port_nmea_local";
	public static final String AIS_OBJ_LOST_TIMEOUT_ID = "ais_object_lost_timeout";
	public static final String AIS_SHIP_LOST_TIMEOUT_ID = "ais_ship_lost_timeout";
	public static final String AIS_CPA_WARNING_TIME_ID = "ais_cpa_warning_time";
	public static final String AIS_CPA_WARNING_DISTANCE_ID = "ais_cpa_warning_distance";
	public static final String AIS_OWN_MMSI_ID = "ais_own_mmsi";
    public static final String AIS_DISPLAY_OWN_POSITION_ID = "ais_display_own_position";
    public static final String AIS_RECEIVE_IN_BACKGROUND_ID = "ais_receive_in_background";
	public static final String AIS_CONNECTION_ENABLED_ID = "ais_connection_enabled";
	public static final String AIS_CPA_ENABLED_ID = "ais_cpa_enabled";
	public static final String AIS_USE_NMEA_LOCATION_ID = "ais_use_nmea_location";
	public final CommonPreference<Integer> AIS_NMEA_PROTOCOL;
	public static final int AIS_NMEA_PROTOCOL_UDP = 0;
	public static final int AIS_NMEA_PROTOCOL_TCP = 1;
	public final CommonPreference<String> AIS_NMEA_IP_ADDRESS;
	public static final String AIS_NMEA_DEFAULT_IP = "";
	public final CommonPreference<Integer> AIS_NMEA_TCP_PORT;
	public static final Integer AIS_NMEA_DEFAULT_TCP_PORT = 4001;
	public final CommonPreference<Integer> AIS_NMEA_UDP_PORT;
	public static final Integer AIS_NMEA_DEFAULT_UDP_PORT = 10110;
	/* after this time of missing AIS signal the object is outdated and can be removed: */
	public final CommonPreference<Integer> AIS_OBJ_LOST_TIMEOUT;
	public static final Integer AIS_OBJ_LOST_DEFAULT_TIMEOUT = 7;
	/* after this time of missing AIS signal the vessel symbol can change to mark "lost": */
	public final CommonPreference<Integer> AIS_SHIP_LOST_TIMEOUT;
	public static final Integer AIS_SHIP_LOST_DEFAULT_TIMEOUT = 4;
	public final CommonPreference<Integer> AIS_CPA_WARNING_TIME; // in minutes
	public static final Integer AIS_CPA_DEFAULT_WARNING_TIME = 1;
	public final CommonPreference<Float> AIS_CPA_WARNING_DISTANCE; // in miles
	public static final Float AIS_CPA_WARNING_DEFAULT_DISTANCE = 0.02f;
	public final CommonPreference<Integer> AIS_OWN_MMSI;
	public static final Integer AIS_DEFAULT_OWN_MMSI = 0;
	public final CommonPreference<Boolean> AIS_DISPLAY_OWN_POSITION;
	public static final Boolean AIS_DISPLAY_OWN_POSITION_DEFAULT = false;
    public final CommonPreference<Boolean> AIS_RECEIVE_IN_BACKGROUND;
    public static final Boolean AIS_RECEIVE_IN_BACKGROUND_DEFAULT = false;
	/* set to false while the user keeps the connection closed from the plugin screen: */
	public final CommonPreference<Boolean> AIS_CONNECTION_ENABLED;
	public final CommonPreference<Boolean> AIS_CPA_ENABLED;
	/* replaces the phone position with the one the NMEA stream carries: */
	public final CommonPreference<Boolean> AIS_USE_NMEA_LOCATION;

	/* timestamp of the last NMEA sentence (AIS message or position) received for all instances,
	 * written by the network thread and read by the UI: */
	private volatile long lastMessageReceived = 0;
	/* written by the network thread and by the UI, see setConnectionState(): */
	@NonNull
	private volatile AisConnectionState connectionState = AisConnectionState.NOT_CONNECTED;
	private final List<AisConnectionStateListener> connectionStateListeners = new CopyOnWriteArrayList<>();
	private Location fakeOwnPosition = null; // used for test purposes to fake own position
	@Nullable
	private volatile Location nmeaLocation = null;
	/* touched by the network thread only: */
	private float nmeaSpeed;
	private long nmeaSpeedTime;
	private float nmeaBearing;
	private long nmeaBearingTime;
	private long nmeaLocationPublishedTime;

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
			AisTrackerPlugin.this.onNmeaLocationReceived(location);
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

		/* preference ids are kept as they were, so existing settings survive the redesign */
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
		AIS_CONNECTION_ENABLED = registerBooleanPreference(AIS_CONNECTION_ENABLED_ID, true);
		AIS_CPA_ENABLED = registerBooleanPreference(AIS_CPA_ENABLED_ID, false);
		AIS_USE_NMEA_LOCATION = registerBooleanPreference(AIS_USE_NMEA_LOCATION_ID, false);
		/* the connection preferences have no listeners: the Connection screen saves them together
		 * through applyConnectionSettings(), which reopens the socket once */
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
		return AIS_CPA_ENABLED.get() ? AIS_CPA_WARNING_TIME.get() : 0;
	}

	public float getCpaWarningDistance() {
		return AIS_CPA_WARNING_DISTANCE.get();
	}

	public interface AisConnectionStateListener {
		void onAisConnectionStateChanged(@NonNull AisConnectionState state);
	}

	public void addConnectionStateListener(@NonNull AisConnectionStateListener listener) {
		if (!connectionStateListeners.contains(listener)) {
			connectionStateListeners.add(listener);
		}
	}

	public void removeConnectionStateListener(@NonNull AisConnectionStateListener listener) {
		connectionStateListeners.remove(listener);
	}

	/**
	 * The connection is set up once the user saved it on the Connection screen at least once -
	 * a user of the old preference screen has one of these preferences set as well - and, for
	 * TCP, named a host. Until then the plugin does not open a socket on its own.
	 */
	public boolean isConnectionConfigured() {
		return isConnectionConfigured(settings.getApplicationMode());
	}

	public boolean isConnectionConfigured(@NonNull ApplicationMode mode) {
		boolean saved = AIS_NMEA_PROTOCOL.isSetForMode(mode) || AIS_NMEA_IP_ADDRESS.isSetForMode(mode)
				|| AIS_NMEA_TCP_PORT.isSetForMode(mode) || AIS_NMEA_UDP_PORT.isSetForMode(mode);
		return saved && (AIS_NMEA_PROTOCOL.getModeValue(mode) != AIS_NMEA_PROTOCOL_TCP
				|| !Algorithms.isEmpty(AIS_NMEA_IP_ADDRESS.getModeValue(mode)));
	}

	/** The socket follows the settings of the active profile only. */
	public boolean isActiveMode(@NonNull ApplicationMode mode) {
		return mode == settings.getApplicationMode();
	}

	@NonNull
	public AisConnectionState getConnectionState() {
		return getConnectionState(settings.getApplicationMode());
	}

	/**
	 * The live state for the active profile. Another profile has no socket, so it is only ever
	 * "not set up" or "not connected".
	 */
	@NonNull
	public AisConnectionState getConnectionState(@NonNull ApplicationMode mode) {
		if (!isConnectionConfigured(mode)) {
			return AisConnectionState.NOT_SET_UP;
		}
		return isActiveMode(mode) ? connectionState : AisConnectionState.NOT_CONNECTED;
	}

	/**
	 * Called from the network thread and from the UI, so the compare and the assignment are one
	 * step; the listeners are always notified on the UI thread.
	 */
	private synchronized void setConnectionState(@NonNull AisConnectionState state) {
		if (connectionState != state) {
			connectionState = state;
			AisConnectionState reported = getConnectionState();
			app.runInUIThread(() -> {
				for (AisConnectionStateListener listener : connectionStateListeners) {
					listener.onAisConnectionStateChanged(reported);
				}
			});
		}
	}

	/** Every NMEA sentence counts as data - an AIS message as well as a position of the stream. */
	private void onDataReceived() {
		lastMessageReceived = System.currentTimeMillis();
		if (connectionState == AisConnectionState.NO_DATA) {
			setConnectionState(AisConnectionState.CONNECTED);
		}
	}

	/* runs on the UI thread every AIS_NO_DATA_CHECK_INTERVAL_MS while the socket is open: */
	private final Runnable noDataCheck = new Runnable() {
		@Override
		public void run() {
			AisConnectionState state = connectionState;
			if (state == AisConnectionState.CONNECTED
					&& System.currentTimeMillis() - lastMessageReceived > AIS_NO_DATA_TIMEOUT_MS) {
				setConnectionState(AisConnectionState.NO_DATA);
			}
			if (state == AisConnectionState.CONNECTED || state == AisConnectionState.NO_DATA) {
				app.runInUIThread(this, AIS_NO_DATA_CHECK_INTERVAL_MS);
			}
		}
	};

	private void startNoDataCheck() {
		app.getUiHandler().removeCallbacks(noDataCheck);
		app.runInUIThread(noDataCheck, AIS_NO_DATA_CHECK_INTERVAL_MS);
	}

	private void stopNoDataCheck() {
		app.getUiHandler().removeCallbacks(noDataCheck);
	}

	/** Opens the connection of the profile and remembers that the user wants it open. */
	public void connect(@NonNull ApplicationMode mode) {
		AIS_CONNECTION_ENABLED.setModeValue(mode, true);
		if (isActiveMode(mode)) {
			restartNetworkListener();
		}
	}

	/** Closes the connection and keeps it closed until the user asks for it again. */
	public void disconnect(@NonNull ApplicationMode mode) {
		AIS_CONNECTION_ENABLED.setModeValue(mode, false);
		if (isActiveMode(mode)) {
			stopAisListener();
		}
	}

	/**
	 * Stores what the user saved on the Connection screen for the profile. An open socket would
	 * no longer match the saved values, so it is dropped and reopened with them - once, not per
	 * preference. A connection the user closed stays closed.
	 *
	 * @param port the port of the given protocol; the port of the other protocol is kept.
	 */
	public void applyConnectionSettings(@NonNull ApplicationMode mode, int protocol,
	                                    @Nullable String host, int port) {
		AIS_NMEA_PROTOCOL.setModeValue(mode, protocol);
		if (protocol == AIS_NMEA_PROTOCOL_TCP) {
			AIS_NMEA_IP_ADDRESS.setModeValue(mode, host);
			AIS_NMEA_TCP_PORT.setModeValue(mode, port);
		} else {
			AIS_NMEA_UDP_PORT.setModeValue(mode, port);
		}
		if (isActiveMode(mode)) {
			restartNetworkListener();
		}
	}

	/** Resets the connection settings only; the profile is back in the "not set up" state. */
	public void resetConnectionSettings(@NonNull ApplicationMode mode) {
		AIS_NMEA_PROTOCOL.resetModeToDefault(mode);
		AIS_NMEA_IP_ADDRESS.resetModeToDefault(mode);
		AIS_NMEA_TCP_PORT.resetModeToDefault(mode);
		AIS_NMEA_UDP_PORT.resetModeToDefault(mode);
		if (isActiveMode(mode)) {
			restartNetworkListener();
		}
	}

	/** Resets every setting of the plugin for the profile, the connection and own vessel included. */
	public void resetSettings(@NonNull ApplicationMode mode) {
		for (OsmandPreference<?> preference : getPreferences()) {
			preference.resetModeToDefault(mode);
		}
		if (isActiveMode(mode)) {
			restartNetworkListener();
			AisTrackerLayer layer = this.layer;
			if (layer != null) {
				layer.refreshOwnObjectVisibility();
			}
		}
	}

	public int getVesselsCount() {
		return aisDataManager.getAisObjects().size();
	}

	public boolean isCpaEnabled() {
		return AIS_CPA_ENABLED.get();
	}

	/**
	 * Turns a position sentence of the NMEA stream into a location and, while the user asked for
	 * it, feeds it to the app as the current position. Every fix keeps the phone GPS ignored for
	 * a short while, so a stream that goes silent hands the position back instead of freezing it.
	 */
	private void onNmeaLocationReceived(@NonNull AisLocation location) {
		onDataReceived();
		long now = System.currentTimeMillis();
		Location result = new Location(NMEA_LOCATION_PROVIDER,
				location.getLatitude(), location.getLongitude());
		result.setTime(now);
		if (location.getHasSpeed()) {
			nmeaSpeed = location.getSpeed();
			nmeaSpeedTime = now;
		}
		if (location.getHasBearing()) {
			nmeaBearing = location.getBearing();
			nmeaBearingTime = now;
		}
		/* a GGA or GLL right after a RMC keeps its speed and course, otherwise the position marker
		 * flips between the course arrow and the plain dot with every sentence */
		if (now - nmeaSpeedTime <= NMEA_MOTION_KEEP_MS) {
			result.setSpeed(nmeaSpeed);
		}
		if (now - nmeaBearingTime <= NMEA_MOTION_KEEP_MS) {
			result.setBearing(nmeaBearing);
		}
		nmeaLocation = result;

		if (now - nmeaLocationPublishedTime < NMEA_LOCATION_MIN_INTERVAL_MS) {
			return;
		}
		nmeaLocationPublishedTime = now;
		if (AIS_USE_NMEA_LOCATION.get()) {
			app.runInUIThread(() -> {
				/* checked again: the user may have turned the setting off in the meantime */
				if (AIS_USE_NMEA_LOCATION.get()) {
					app.getLocationProvider().setCustomLocation(result, IGNORE_PHONE_LOCATION_TIMEOUT_MS);
				}
			});
		}
	}

	/** true while the stream actually carries position sentences, not only vessel reports. */
	public boolean isReceivingPosition() {
		Location location = nmeaLocation;
		return location != null
				&& System.currentTimeMillis() - location.getTime() <= NMEA_POSITION_TIMEOUT_MS;
	}

	public Location getOwnPosition() { // used to calculate distances, CPA etc.
		if (fakeOwnPosition != null) {
			return fakeOwnPosition;
		}
		if (AIS_USE_NMEA_LOCATION.get() && isReceivingPosition()) {
			return nmeaLocation;
		}
		return app.getLocationProvider().getLastKnownLocation();
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
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
		if (!AIS_RECEIVE_IN_BACKGROUND.get()) {
			stopAisListener();
		} else {
			updateAisBackgroundService();
			app.runInUIThread(this::stopAisListenerIfBackgroundServiceFailed, 1500);
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

	public void onAisObjectReceived(@NonNull AisObject ais) {
		onDataReceived();
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

	private final AisConnectionListener networkStateListener = new AisConnectionListener() {

		@Override
		public void onAisConnecting() {
			/* after a failure the listener retries on its own; the row keeps saying "Connection
			 * failed" with its Connect action instead of switching to Connecting / Cancel every
			 * few seconds. Connect restarts the listener, which goes through NOT_CONNECTED and
			 * shows the new attempt. */
			if (connectionState != AisConnectionState.FAILED) {
				setConnectionState(AisConnectionState.CONNECTING);
			}
		}

		@Override
		public void onAisConnected() {
			/* the socket is open, but nothing arrived yet - the check turns this into NO_DATA
			 * once the silence gets too long */
			lastMessageReceived = System.currentTimeMillis();
			setConnectionState(AisConnectionState.CONNECTED);
			startNoDataCheck();
		}

		@Override
		public void onAisConnectionFailed(@Nullable String message) {
			setConnectionState(AisConnectionState.FAILED);
		}
	};

	private void startAisNetworkListener() {
		if (!AIS_CONNECTION_ENABLED.get() || !isConnectionConfigured()) {
			setConnectionState(AisConnectionState.NOT_CONNECTED);
			return;
		}
		int proto = AIS_NMEA_PROTOCOL.get();
		if (proto == AisTrackerPlugin.AIS_NMEA_PROTOCOL_UDP) {
			aisDataManager.stopUpdates();
			aisListener = new AisMessageListener(aisDataManager, AIS_NMEA_UDP_PORT.get(), networkStateListener);
			aisDataManager.startUpdates();
		} else if (proto == AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP) {
			aisDataManager.stopUpdates();
			aisListener = new AisMessageListener(aisDataManager, AIS_NMEA_IP_ADDRESS.get(),
					AIS_NMEA_TCP_PORT.get(), networkStateListener);
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
		stopNoDataCheck();
		setConnectionState(AisConnectionState.NOT_CONNECTED);
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
