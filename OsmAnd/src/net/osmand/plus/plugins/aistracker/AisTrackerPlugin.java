package net.osmand.plus.plugins.aistracker;

//import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_AISTRACKER;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
*   This plugin receives AIS positions and other AIS data via network (NMEA protocol)
*   from an AIS receiver/decoder and displays symbols at the map at the vessel position
*/
public class AisTrackerPlugin extends OsmandPlugin {

    private AisTrackerLayer aisTrackerLayer = null;

    public static final String COMPONENT = "net.osmand.aistrackerPlugin";
    public final CommonPreference<Integer> AIS_NMEA_PROTOCOL;
    public static final int AIS_NMEA_PROTOCOL_UDP = 0;
    public static final int AIS_NMEA_PROTOCOL_TCP = 1;
    public final CommonPreference<String> AIS_NMEA_IP_ADDRESS;
    private static final String AIS_NMEA_DEFAULT_IP = "192.168.200.16";
    public final CommonPreference<Integer> AIS_NMEA_TCP_PORT;
    public static final Integer AIS_NMEA_DEFAULT_TCP_PORT = 4001;
    public final CommonPreference<Integer> AIS_NMEA_UDP_PORT;
    public static final Integer AIS_NMEA_DEFAULT_UDP_PORT = 10110;

    public AisTrackerPlugin(OsmandApplication app) {
        super(app);
        /* "ais_nmea_protocol" etc. is a reference to the content of ais_settings.xml */
        AIS_NMEA_PROTOCOL = registerIntPreference("ais_nmea_protocol", AIS_NMEA_PROTOCOL_UDP);
        AIS_NMEA_IP_ADDRESS = registerStringPreference("ais_address_nmea_server", AIS_NMEA_DEFAULT_IP);
        AIS_NMEA_TCP_PORT = registerIntPreference("ais_port_nmea_server", AIS_NMEA_DEFAULT_TCP_PORT);
        AIS_NMEA_UDP_PORT = registerIntPreference("ais_port_nmea_local", AIS_NMEA_DEFAULT_UDP_PORT);

        Log.d("AisTrackerPlugin", "constructor");
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
    public CharSequence getDescription(boolean linksEnabled) {
        return app.getString(R.string.plugin_aistracker_description);
    }

    @Override
    public String getName() {
        return app.getString(R.string.plugin_aistracker_name);
    }

    @Override
    //public int getLogoResourceId() { return R.drawable.ic_plugin_nautical_map; }
    public int getLogoResourceId() {
        return R.drawable.mm_sport_sailing;
    }

    @Override
    public Drawable getAssetResourceImage() {
        return app.getUIUtilities().getIcon(R.drawable.ais_map);
    }

    @Override
    public List<ApplicationMode> getAddedAppModes() {
        //return Collections.singletonList(ApplicationMode.BOAT);
        return Arrays.asList(ApplicationMode.BOAT, ApplicationMode.DEFAULT);
    }

    @Override
    public List<String> getRendererNames() {
        return Collections.singletonList(RendererRegistry.NAUTICAL_RENDER);
    }

    @Override
    public String getId() {
        return "osmand.aistracker";
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
    public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
        OsmandMapTileView mapView = app.getOsmandMap().getMapView();
        if (isActive()) {
            if (aisTrackerLayer == null) {
                Log.d("AisTrackerPlugin", "call registerLayers()");
                registerLayers(context, mapActivity);
            }
            if (!mapView.getLayers().contains(aisTrackerLayer)) {
                mapView.addLayer(aisTrackerLayer, 3.5f);
            }
        } else {
            if (aisTrackerLayer != null) {
                mapView.removeLayer(aisTrackerLayer);
                aisTrackerLayer.cleanup();
                aisTrackerLayer = null;
                mapView.refreshMap();
            }
        }
    }

    @Override
    public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
        if (aisTrackerLayer == null) {
            Log.d("AisTrackerPlugin", "new AisTrackerLayer");
            aisTrackerLayer = new AisTrackerLayer(context, this);
            app.getOsmandMap().getMapView().addLayer(aisTrackerLayer, 3.5f);
        } else {
            Log.d("AisTrackerPlugin", "AisTrackerLayer already exists");
            OsmandMapTileView mapView = app.getOsmandMap().getMapView();
            if (!mapView.getLayers().contains(aisTrackerLayer)) {
                mapView.addLayer(aisTrackerLayer, 3.5f);
            }
        }
    }

    public AisTrackerLayer getLayer() { return aisTrackerLayer; }
}
