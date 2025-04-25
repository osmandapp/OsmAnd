package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.settings.fragments.SettingsScreenType.AIS_SETTINGS;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.Collections;
import java.util.List;

/*
 *   This plugin receives AIS positions and other AIS data via network (NMEA protocol)
 *   from an AIS receiver/decoder and displays symbols at the map at the vessel position
 */
public class AisTrackerPlugin extends OsmandPlugin {

	private AisTrackerLayer layer = null;
	private final AisSimulationProvider simulationProvider = new AisSimulationProvider(this);

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
	public final CommonPreference<Integer> AIS_NMEA_PROTOCOL;
	public static final int AIS_NMEA_PROTOCOL_UDP = 0;
	public static final int AIS_NMEA_PROTOCOL_TCP = 1;
	public final CommonPreference<String> AIS_NMEA_IP_ADDRESS;
	private static final String AIS_NMEA_DEFAULT_IP = "192.168.200.16";
	public final CommonPreference<Integer> AIS_NMEA_TCP_PORT;
	private static final Integer AIS_NMEA_DEFAULT_TCP_PORT = 4001;
	public final CommonPreference<Integer> AIS_NMEA_UDP_PORT;
	private static final Integer AIS_NMEA_DEFAULT_UDP_PORT = 10110;
	public final CommonPreference<Integer> AIS_OBJ_LOST_TIMEOUT;
	public static final Integer AIS_OBJ_LOST_DEFAULT_TIMEOUT = 7;
	public final CommonPreference<Integer> AIS_SHIP_LOST_TIMEOUT;
	public static final Integer AIS_SHIP_LOST_DEFAULT_TIMEOUT = 4;
	public final CommonPreference<Integer> AIS_CPA_WARNING_TIME;
	public static final Integer AIS_CPA_DEFAULT_WARNING_TIME = 0;
	public final CommonPreference<Float> AIS_CPA_WARNING_DISTANCE;
	public static final Float AIS_CPA_WARNING_DEFAULT_DISTANCE = 1.0f;

	public AisTrackerPlugin(@NonNull OsmandApplication app) {
		super(app);
		/* "ais_nmea_protocol" etc. is a reference to the content of xml/ais_settings.xml */
		AIS_NMEA_PROTOCOL = registerIntPreference(AIS_NMEA_PROTOCOL_ID, AIS_NMEA_PROTOCOL_UDP);
		AIS_NMEA_IP_ADDRESS = registerStringPreference(AIS_NMEA_IP_ADDRESS_ID, AIS_NMEA_DEFAULT_IP);
		AIS_NMEA_TCP_PORT = registerIntPreference(AIS_NMEA_TCP_PORT_ID, AIS_NMEA_DEFAULT_TCP_PORT);
		AIS_NMEA_UDP_PORT = registerIntPreference(AIS_NMEA_UDP_PORT_ID, AIS_NMEA_DEFAULT_UDP_PORT);
		AIS_OBJ_LOST_TIMEOUT = registerIntPreference(AIS_OBJ_LOST_TIMEOUT_ID, AIS_OBJ_LOST_DEFAULT_TIMEOUT);
		AIS_SHIP_LOST_TIMEOUT = registerIntPreference(AIS_SHIP_LOST_TIMEOUT_ID, AIS_SHIP_LOST_DEFAULT_TIMEOUT);
		AIS_CPA_WARNING_TIME = registerIntPreference(AIS_CPA_WARNING_TIME_ID, AIS_CPA_DEFAULT_WARNING_TIME);
		AIS_CPA_WARNING_DISTANCE = registerFloatPreference(AIS_CPA_WARNING_DISTANCE_ID, AIS_CPA_WARNING_DEFAULT_DISTANCE);
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
		if (layer != null) {
			layer.checkTcpConnection();
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

	@NonNull
	public AisSimulationProvider getSimulationProvider() {
		return simulationProvider;
	}
}
