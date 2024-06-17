package net.osmand.plus.plugins.aistracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AisTrackerLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
    private static final int START_ZOOM = 10;
    private final AisTrackerPlugin plugin;
    private Map<Integer, AisObject> aisObjectList;
    private final int aisObjectListCounterMax = 100;
    private final Context context;
    private Paint bitmapPaint;
    private Timer timer;
    private TimerTask taskCheckAisObjectList;
    private AisMessageListener listener;
    public AisTrackerLayer(@NonNull Context context, @NonNull AisTrackerPlugin plugin) {
        super(context);
        this.plugin = plugin;
        this.context = context;
        this.listener = null;

        this.aisObjectList = new HashMap<>();
        this.bitmapPaint = new Paint();
        this.bitmapPaint.setAntiAlias(true);
        this.bitmapPaint.setFilterBitmap(true);
        this.bitmapPaint.setStrokeWidth(4);
        this.bitmapPaint.setColor(Color.DKGRAY);

        initTimer();
        startNetworkListener();

        initTestObjects();        // for test purposes:
    }

    private void initTestObjects() {
        // passenger ship
        AisObject ais = new AisObject(34568, 1, 20, 0, 1, 320,
                320.0, 8.4, 50.738d, 7.099d, 0.0);
        updateAisObjectList(ais);
        ais = new AisObject(34568, 5, 0, "TEST-CALLSIGN1", "TEST-Ship", 60 /* passenger */, 56,
                65, 8, 12, 2,
                "Potsdam", 8, 15, 22, 5);
        updateAisObjectList(ais);
        // sailing boat
        ais = new AisObject(454011, 1, 20, 8, 0, 120,
                125.0, 4.4, 50.737d, 7.098d, 0.0);
        updateAisObjectList(ais);
        ais = new AisObject(454011, 5, 0, "TEST-CALLSIGN2", "TEST-Sailor", 36 /* sailing  */, 0,
                0, 0, 0, 0,
                "", 0, 0, 0, 0);
        updateAisObjectList(ais);
        // land station
        ais = new AisObject(878121, 4, 50.736d, 7.100d);
        updateAisObjectList(ais);
        // AIDS
        ais = new AisObject( 521077, 21, 50.735d, 7.101d, 2,
                0, 0, 0, 0);
        updateAisObjectList(ais);
        // aircraft
        ais = new AisObject(910323, 9, 15, 65, 180.5, 55.0, 50.734d, 7.102d);
        updateAisObjectList(ais);

        //removeOldestAisObjectListEntry();
        //removeLostAisObjects();
    }
    private void initTimer() {
        this.taskCheckAisObjectList = new TimerTask() {
            @Override
            public void run() {
                Log.d("AisTrackerLayer", "timer task taskCheckAisObjectList running");
                removeLostAisObjects();
            }
        };
        this.timer = new Timer();
        timer.schedule(taskCheckAisObjectList, 20000, 30000);
    }
    private void startNetworkListener() {
        int proto = plugin.AIS_NMEA_PROTOCOL.get();
        if (proto == AisTrackerPlugin.AIS_NMEA_PROTOCOL_UDP) {
            this.listener = new AisMessageListener(plugin.AIS_NMEA_UDP_PORT.get(), this);
        } else if (proto == AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP) {
            this.listener = new AisMessageListener(plugin.AIS_NMEA_IP_ADDRESS.get(), plugin.AIS_NMEA_TCP_PORT.get(), this);
        }
    }
    private void stopNetworkListener() {
        if (this.listener != null) {
            this.listener.stopListener();
            this.listener = null;
        }
    }
    public void restartNetworkListener() {
        stopNetworkListener();
        startNetworkListener();
    }
    public void cleanup() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
        if (this.aisObjectList != null) {
            this.aisObjectList.clear();
            this.aisObjectList = null;
        }
        stopNetworkListener();
    }
    private void removeLostAisObjects() {
        for (Iterator<Map.Entry<Integer, AisObject>> iterator = aisObjectList.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, AisObject> entry = iterator.next();
            if (entry.getValue().checkObjectAge()) {
                Log.d("AisTrackerLayer", "remove AIS object with MMSI " + entry.getValue().getMmsi());
                iterator.remove();
            }
        }
        // aisObjectList.entrySet().removeIf(entry -> entry.getValue().checkObjectAge());
    }
    private void removeOldestAisObjectListEntry() {
        Log.d("AisTrackerLayer", "removeOldestAisObjectListEntry() called");
        long oldestTimeStamp = System.currentTimeMillis();
        AisObject oldest = null;
        for (AisObject ais : aisObjectList.values()) {
            long timeStamp = ais.getLastUpdate();
            if (timeStamp <= oldestTimeStamp) {
                oldestTimeStamp = timeStamp;
                oldest = ais;
            }
        }
        if (oldest != null) {
            Log.d("AisTrackerLayer", "remove AIS object with MMSI " + oldest.getMmsi());
            aisObjectList.remove(oldest.getMmsi(), oldest);
        }
    }

    /* add new AIS object to list, or (if already exist) update its value */
    public void updateAisObjectList(@NonNull AisObject ais) {
        int mmsi = ais.getMmsi();
        AisObject obj = aisObjectList.get(mmsi);
        if (obj == null) {
            Log.d("AisTrackerLayer", "add AIS object with MMSI " + ais.getMmsi());
            aisObjectList.put(mmsi, new AisObject(ais));
            if (aisObjectList.size() >= this.aisObjectListCounterMax) {
                this.removeOldestAisObjectListEntry();
            }
        } else {
            obj.set(ais);
        }
    }

    @Nullable
    public Bitmap getBitmap(@DrawableRes int drawable) { return getScaledBitmap(drawable); }

    @NonNull
    public OsmandApplication getApplication() {
        return (OsmandApplication) context.getApplicationContext();
    }
    public boolean isLocationVisible(RotatedTileBox tileBox, LatLon coordinates) {
        //noinspection SimplifiableIfStatement
        if (tileBox == null || coordinates == null) {
            return false;
        }
        return tileBox.containsLatLon(coordinates);
    }

    @Override
    public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
        for (AisObject ais : aisObjectList.values()) {
            if (isLocationVisible(tileBox, ais.getPosition())) {
                ais.draw(this, bitmapPaint, canvas, tileBox);
            }
        }
    }

    @Override
    public boolean drawInScreenPixels() {
        return true;
    }

    @Override
    public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects,
                                        boolean unknownLocation, boolean excludeUntouchableObjects) {
        if (tileBox.getZoom() >= START_ZOOM) {
            getAisObjectsFromPoint(point, tileBox, objects);
        }
    }
    public void getAisObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<? super AisObject> aisList) {
        if (aisObjectList.isEmpty()) {
            return;
        }

        MapRendererView mapRenderer = getMapRenderer();
        float radius = getScaledTouchRadius(getApplication(), tileBox.getDefaultRadiusPoi()) * TOUCH_RADIUS_MULTIPLIER;
        List<PointI> touchPolygon31 = null;
        if (mapRenderer != null) {
            touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius);
            if (touchPolygon31 == null) {
                return;
            }
        }

        for (AisObject ais : aisObjectList.values()) {
            LatLon pos = ais.getPosition();
            if (pos != null) {
                double lat = pos.getLatitude();
                double lon = pos.getLongitude();

                boolean add = mapRenderer != null
                        ? NativeUtilities.isPointInsidePolygon(lat, lon, touchPolygon31)
                        : tileBox.isLatLonNearPixel(lat, lon, point.x, point.y, radius);
                if (add) {
                    aisList.add(ais);
                }
            }
        }
    }

    @Override
    public LatLon getObjectLocation(Object o) {
        if (o instanceof AisObject) {
            LatLon pos = ((AisObject) o).getPosition();
            if (pos != null) {
                return new LatLon(pos.getLatitude(), pos.getLongitude());
            }
        }
        return null;
    }

    @Override
    public PointDescription getObjectName(Object o) {
        if (o instanceof AisObject) {
            AisObject ais = ((AisObject) o);
            if (ais.getShipName() != null) {
                return new PointDescription("AIS object", ais.getShipName());
            }
            return new PointDescription("AIS object",
                    "AIS object with MMSI " + ais.getMmsi());
        }
        return null;
    }
}
