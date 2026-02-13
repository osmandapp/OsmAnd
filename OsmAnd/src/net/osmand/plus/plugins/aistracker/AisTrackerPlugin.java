package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.settings.fragments.SettingsScreenType.AIS_SETTINGS;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.aistracker.AisMessageListener.AisDataListener;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
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
    public static final Boolean AIS_RECEIVE_IN_BACKGROUND_DEFAULT = true;

	/* timestamp of last AIS message received for all instances: */
	private long lastMessageReceived = 0;
	private Location fakeOwnPosition = null; // used for test purposes to fake own position

	private final StateChangedListener<String> addrPrefListener = change -> restartNetworkListener();
	private final StateChangedListener<Integer> protocolPortPrefListener = change -> restartNetworkListener();

	public class AisDataManager implements AisDataListener {

		private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(AisDataManager.class);

		private static final int AIS_OBJECT_LIST_COUNTER_MAX = 200;
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
				obj = new AisObject(AisTrackerPlugin.this, ais);
				objects.put(ais.getMmsi(), obj);
			}
			if (objects.size() >= AIS_OBJECT_LIST_COUNTER_MAX) {
				removeOldestAisObject(objects);
			}
			AisTrackerPlugin.this.onAisObjectReceived(obj);
		}

		@NonNull
		public synchronized List<AisObject> getAisObjects() {
			return new ArrayList<>(objects.values());
		}

		public synchronized void removeLostObjects() {
			for (Iterator<Map.Entry<Integer, AisObject>> iterator = objects.entrySet().iterator(); iterator.hasNext(); ) {
				AisObject obj = iterator.next().getValue();
				if (obj.checkObjectAge()) {
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
		AIS_NMEA_IP_ADDRESS.addListener(addrPrefListener);
		AIS_NMEA_PROTOCOL.addListener(protocolPortPrefListener);
		AIS_NMEA_TCP_PORT.addListener(protocolPortPrefListener);
		AIS_NMEA_UDP_PORT.addListener(protocolPortPrefListener);
	}

	@Override
	public boolean isMarketPlugin() {
		return true;
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
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
        if (!AIS_RECEIVE_IN_BACKGROUND.get()) {
            stopAisListener();
        }
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (isActive()) {
			if (layer == null) {
				Log.d("AisTrackerPlugin", "call updateLayers()");
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
	}

	private void stopAisListener() {
		if (aisListener != null) {
			aisListener.stopListener();
			aisListener = null;
		}
		aisDataManager.stopUpdates();
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
