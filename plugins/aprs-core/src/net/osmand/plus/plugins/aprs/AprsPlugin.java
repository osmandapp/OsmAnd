package net.osmand.plus.plugins.aprs;

import static net.osmand.plus.settings.fragments.SettingsScreenType.APRS_SETTINGS;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.Collections;
import java.util.List;

public class AprsPlugin extends OsmandPlugin {

	public static final String APRS_ID = "osmand.aprs";
	private static final String COMPONENT = "net.osmand.aprsPlugin";

	public static final String APRS_FREQUENCY_ID = "aprs_frequency_mhz";
	public static final String APRS_CUSTOM_FREQUENCY_ID = "aprs_custom_frequency_mhz";
	public static final String APRS_EXPIRY_MINUTES_ID = "aprs_expiry_minutes";
	public static final String APRS_RADIUS_KM_ID = "aprs_radius_km";
	public static final String APRS_HAMLIB_HOST_ID = "aprs_hamlib_host";
	public static final String APRS_HAMLIB_PORT_ID = "aprs_hamlib_port";
	public static final String APRS_HAMLIB_ENABLED_ID = "aprs_hamlib_enabled";

	public final CommonPreference<Float> APRS_FREQUENCY;
	public final CommonPreference<Float> APRS_CUSTOM_FREQUENCY;
	public final CommonPreference<Integer> APRS_EXPIRY_MINUTES;
	public final CommonPreference<Integer> APRS_RADIUS_KM;
	public final CommonPreference<String> APRS_HAMLIB_HOST;
	public final CommonPreference<Integer> APRS_HAMLIB_PORT;
	public final CommonPreference<Boolean> APRS_HAMLIB_ENABLED;

	private final AprsDataManager dataManager;
	private final AprsSymbolResolver symbolResolver;
	private final AprsHamlibBridge hamlibBridge;
	private AprsLayer layer;

	public AprsPlugin(@NonNull OsmandApplication app) {
		super(app);
		dataManager = new AprsDataManager(app);
		symbolResolver = new AprsSymbolResolver(app);
		hamlibBridge = new AprsHamlibBridge();

		APRS_FREQUENCY = registerFloatPreference(APRS_FREQUENCY_ID, 144.800f);
		APRS_CUSTOM_FREQUENCY = registerFloatPreference(APRS_CUSTOM_FREQUENCY_ID, 144.800f);
		APRS_EXPIRY_MINUTES = registerIntPreference(APRS_EXPIRY_MINUTES_ID, 30);
		APRS_RADIUS_KM = registerIntPreference(APRS_RADIUS_KM_ID, 300);
		APRS_HAMLIB_HOST = registerStringPreference(APRS_HAMLIB_HOST_ID, "localhost");
		APRS_HAMLIB_PORT = registerIntPreference(APRS_HAMLIB_PORT_ID, 4532);
		APRS_HAMLIB_ENABLED = registerBooleanPreference(APRS_HAMLIB_ENABLED_ID, false);

		dataManager.setListener(new AprsDataManager.StationListener() {
			@Override
			public void onStationReceived(@NonNull AprsStation station) {
				AprsLayer l = layer;
				if (l != null) {
					l.onStationReceived(station);
				}
			}

			@Override
			public void onStationRemoved(@NonNull AprsStation station) {
				AprsLayer l = layer;
				if (l != null) {
					l.onStationRemoved(station);
				}
			}
		});
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
		return "net.osmand.dev";
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.plugin_aprs_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.plugin_aprs_name);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_aprs;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.ic_plugin_aprs);
	}

	@Override
	public List<ApplicationMode> getAddedAppModes() {
		return Collections.singletonList(ApplicationMode.DEFAULT);
	}

	@Override
	public String getId() {
		return APRS_ID;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			applySettingsToManagers();
			dataManager.startUpdates();
			updateLayers(app, null);
			app.getOsmandMap().getMapView().refreshMap();
		} else {
			dataManager.stopUpdates();
			dataManager.cleanupResources();
			updateLayers(app, null);
		}
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		dataManager.stopListener();
		hamlibBridge.shutdown();
		super.disable(app);
	}

	private void applySettingsToManagers() {
		dataManager.setExpiryMinutes(APRS_EXPIRY_MINUTES.get());
		dataManager.setRadiusKm(APRS_RADIUS_KM.get());
		hamlibBridge.setHost(APRS_HAMLIB_HOST.get());
		hamlibBridge.setPort(APRS_HAMLIB_PORT.get());
		hamlibBridge.setEnabled(APRS_HAMLIB_ENABLED.get());
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return APRS_SETTINGS;
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.plugin_aprs_settings_description);
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (isActive()) {
			if (layer == null) {
				registerLayers(context, mapActivity);
			}
			if (layer != null && !mapView.getLayers().contains(layer)) {
				mapView.addLayer(layer, 3.6f);
			}
		} else if (layer != null) {
			mapView.removeLayer(layer);
			layer = null;
			mapView.refreshMap();
		}
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (layer == null) {
			layer = new AprsLayer(context);
			app.getOsmandMap().getMapView().addLayer(layer, 3.6f);
		}
	}

	@NonNull
	public OsmandApplication getApplication() {
		return app;
	}

	@NonNull
	public AprsDataManager getDataManager() {
		return dataManager;
	}

	@NonNull
	public AprsSymbolResolver getSymbolResolver() {
		return symbolResolver;
	}

	@NonNull
	public AprsHamlibBridge getHamlibBridge() {
		return hamlibBridge;
	}

	@Nullable
	public AprsLayer getLayer() {
		return layer;
	}

	/**
	 * Register an external packet source (aprs-driver, KISS, test harness).
	 */
	public void registerMessageListener(@NonNull AprsMessageListener listener) {
		// External listeners push frames via dataManager.onAx25Frame()
	}

	public void ingestAx25Frame(@NonNull byte[] frame) {
		dataManager.onAx25Frame(frame);
	}

	public void ingestParsedStation(@NonNull AprsStation station) {
		dataManager.ingestStation(station);
	}
}
