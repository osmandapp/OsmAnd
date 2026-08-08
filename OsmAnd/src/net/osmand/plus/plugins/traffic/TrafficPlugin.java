package net.osmand.plus.plugins.traffic;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.engine.EngineType;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class TrafficPlugin extends OsmandPlugin implements OsmAndLocationListener {

	public static final String ID = "osmand.traffic";

	private static final String ROUTING_ENGINE_KEY = OnlineRoutingEngine.ONLINE_ROUTING_ENGINE_PREFIX + "tomtom_traffic";
	private static final float ZORDER_TRAFFIC = 0.65f;
	// leading dot keeps the layer out of the user-facing map source list (it is an overlay only, toggled via this plugin)
	private static final String OVERLAY_NAME = ".TomTomTraffic";
	// relative0 leaves free-flowing roads transparent and colours only slow segments, so the base map stays visible
	private static final String FLOW_TILE_URL =
			"https://api.tomtom.com/traffic/map/4/tile/flow/relative0/{0}/{1}/{2}.png?tileSize=256&key=";
	private static final int EXPIRATION_MINUTES = 3;
	private static final long REROUTE_INTERVAL = 2 * 60 * 1000;

	public final CommonPreference<Boolean> TRAFFIC_ENABLED;
	public final CommonPreference<Boolean> TRAFFIC_ROUTING;
	public final CommonPreference<Boolean> TRAFFIC_AUTO_REROUTE;
	public final CommonPreference<String> TOMTOM_API_KEY;

	private final StateChangedListener<Boolean> enabledListener = change -> refreshOverlay();
	private final StateChangedListener<String> apiKeyListener = change -> refreshOverlay();

	private MapTileLayer trafficLayer;
	private String appliedKey;
	private long lastRerouteTime;
	private boolean locationListenerRegistered;

	public TrafficPlugin(@NonNull OsmandApplication app) {
		super(app);
		TRAFFIC_ENABLED = registerBooleanPreference("traffic_overlay_enabled", false).makeProfile();
		TRAFFIC_ROUTING = registerBooleanPreference("traffic_routing", false).makeProfile();
		TRAFFIC_AUTO_REROUTE = registerBooleanPreference("traffic_auto_reroute", true).makeProfile();
		TOMTOM_API_KEY = registerStringPreference("tomtom_api_key", "").makeGlobal().makeShared();
		TRAFFIC_ENABLED.addListener(enabledListener);
		TOMTOM_API_KEY.addListener(apiKeyListener);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return app.getString(R.string.traffic_plugin_name);
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.traffic_plugin_description);
	}

	@Override
	public boolean isEnableByDefault() {
		return false;
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.TRAFFIC_SETTINGS;
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		if (!locationListenerRegistered) {
			app.getLocationProvider().addLocationListener(this);
			locationListenerRegistered = true;
		}
		return true;
	}

	@Override
	public void updateLocation(Location location) {
		if (!isActive() || !TRAFFIC_AUTO_REROUTE.get()) {
			return;
		}
		RoutingHelper routingHelper = app.getRoutingHelper();
		ApplicationMode mode = routingHelper.getAppMode();
		if (mode == null || !mode.getRouteService().isOnline() || !routingHelper.isFollowingMode()) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastRerouteTime >= REROUTE_INTERVAL) {
			lastRerouteTime = now;
			routingHelper.onSettingsChanged(mode, true);
		}
	}

	@Nullable
	public OnlineRoutingEngine getTrafficEngine(@NonNull ApplicationMode mode) {
		if (!isActive() || !TRAFFIC_ROUTING.getModeValue(mode) || Algorithms.isEmpty(TOMTOM_API_KEY.get())) {
			return null;
		}
		Map<String, String> params = new HashMap<>();
		params.put(EngineParameter.KEY.name(), ROUTING_ENGINE_KEY);
		params.put(EngineParameter.API_KEY.name(), TOMTOM_API_KEY.get());
		params.put(EngineParameter.VEHICLE_KEY.name(), "car");
		return EngineType.TOMTOM_TYPE.newInstance(params);
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (trafficLayer != null) {
			mapView.removeLayer(trafficLayer);
		}
		trafficLayer = new MapTileLayer(context, false);
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (trafficLayer == null) {
			registerLayers(context, mapActivity);
		}
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		String apiKey = TOMTOM_API_KEY.get();
		boolean show = isActive() && TRAFFIC_ENABLED.get() && !Algorithms.isEmpty(apiKey);
		if (show) {
			boolean changed = false;
			if (!mapView.isLayerExists(trafficLayer)) {
				mapView.addLayer(trafficLayer, ZORDER_TRAFFIC);
				changed = true;
			}
			if (!apiKey.equals(appliedKey) || trafficLayer.getMap() == null) {
				trafficLayer.setMap(buildTrafficTileSource(apiKey));
				trafficLayer.setAlpha(255);
				appliedKey = apiKey;
				changed = true;
			}
			if (changed) {
				mapView.refreshMap();
			}
		} else if (mapView.isLayerExists(trafficLayer)) {
			mapView.removeLayer(trafficLayer);
			trafficLayer.setMap(null);
			appliedKey = null;
			mapView.refreshMap();
		}
	}

	private void refreshOverlay() {
		app.runInUIThread(() -> updateLayers(app, null));
	}

	@NonNull
	private static ITileSource buildTrafficTileSource(@NonNull String apiKey) {
		TileSourceTemplate template = new TileSourceTemplate(OVERLAY_NAME,
				FLOW_TILE_URL + apiKey, ".png", 22, 0, 256, 16, 18000);
		template.setExpirationTimeMinutes(EXPIRATION_MINUTES);
		return template;
	}
}
