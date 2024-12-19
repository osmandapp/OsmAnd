package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.getCpa;
import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.knotsToMeterPerSecond;
import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.meterToMiles;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.*;
import static net.osmand.plus.utils.OsmAndFormatter.FORMAT_MINUTES;

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

import net.osmand.Location;
import net.osmand.LocationConvert;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AisTrackerLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
    private static final int START_ZOOM = 10;
    private final AisTrackerPlugin plugin;
    private ConcurrentMap<Integer, AisObject> aisObjectList;
    private static final int aisObjectListCounterMax = 200;
    private final Context context;
    private final Paint bitmapPaint;
    private Timer timer;
    private AisMessageListener listener;
    public AisTrackerLayer(@NonNull Context context, @NonNull AisTrackerPlugin plugin) {
        super(context);
        this.plugin = plugin;
        this.context = context;
        this.listener = null;

        this.aisObjectList = new ConcurrentHashMap<>();
        this.bitmapPaint = new Paint();
        this.bitmapPaint.setAntiAlias(true);
        this.bitmapPaint.setFilterBitmap(true);
        this.bitmapPaint.setStrokeWidth(4);
        this.bitmapPaint.setColor(Color.DKGRAY);

        AisObject.setCpaWarningTime(plugin.AIS_CPA_WARNING_TIME.get());
        AisObject.setCpaWarningDistance(plugin.AIS_CPA_WARNING_DISTANCE.get());

        initTimer();
        startNetworkListener();

        // for test purposes: remove/disable later...
        //initTestObject1();
        //initTestObject2();
        //initTestObject3();
        //initTestObject4();
        //initTestObject5();
        //testCrossingTimes();
        //testCpa();
        //initFakePosition();
    }

    private void testCrossingTimes() {
        // here some tests for the geo (CPA) calculation
        // intention is to test the function to calculate times of two objects with crossing courses
        // define 12 positions on two courses: position a1 ... a6 at course line A (course 90°)
        //   and position b1 ... b6 at course line B (course 45°)
        // the positions are taken from a (paper) map
        // for coordinate transformation see https://www.koordinaten-umrechner.de
        AisTrackerHelper.Cpa cpa = new AisTrackerHelper.Cpa();
        Location a1 = new Location("test", 49.5d, -3.266667d); // 49°30'N, 3°16'W
        Location a2 = new Location("test", 49.5d, -3.166667d); // 49°30'N, 3°10'W
        Location a3 = new Location("test", 49.5d, -3.116667d); // 49°30'N, 3°7'W
        Location a4 = new Location("test", 49.5d, -3.093333d); // 49°30'N, 3°5.6'W
        Location a5 = new Location("test", 49.5d, -3.05d); // 49°30'N, 3°3'W
        Location a6 = new Location("test", 49.5d, -3.016667d); // 49°30'N, 3°1'W
        Location b1 = new Location("test", 49.395d, -3.25d); // 49°23.7'N, 3°15'W
        Location b2 = new Location("test", 49.441667d, -3.183333d); // 49°26.5'N, 3°11'W
        Location b3 = new Location("test", 49.47d, -3.133333d); // 49°28.2'N, 3°8'W
        Location b4 = new Location("test", 49.5d, -3.093333d); // 49°30'N, 3°5.6'W
        Location b5 = new Location("test", 49.513333d, -3.066667d); // 49°30.8'N, 3°4'W
        Location b6 = new Location("test", 49.538333d, -3.033333d); // 49°32.3'N, 3°2'W
        a1.setSpeed(knotsToMeterPerSecond(1.0f)); a1.setBearing(90.0f);
        a2.setSpeed(knotsToMeterPerSecond(1.0f)); a2.setBearing(90.0f);
        a3.setSpeed(knotsToMeterPerSecond(1.0f)); a3.setBearing(90.0f);
        a4.setSpeed(knotsToMeterPerSecond(1.0f)); a4.setBearing(90.0f);
        a5.setSpeed(knotsToMeterPerSecond(1.0f)); a5.setBearing(90.0f);
        a6.setSpeed(knotsToMeterPerSecond(1.0f)); a6.setBearing(90.0f);
        b1.setSpeed(knotsToMeterPerSecond(1.0f)); b1.setBearing(45.0f);
        b2.setSpeed(knotsToMeterPerSecond(1.0f)); b2.setBearing(45.0f);
        b3.setSpeed(knotsToMeterPerSecond(1.0f)); b3.setBearing(45.0f);
        b4.setSpeed(knotsToMeterPerSecond(1.0f)); b4.setBearing(45.0f);
        b5.setSpeed(knotsToMeterPerSecond(1.0f)); b5.setBearing(45.0f);
        b6.setSpeed(knotsToMeterPerSecond(1.0f)); b6.setBearing(45.0f);
        // now trigger the calculations:
        cpa.reset(); getCpa(a3, b2, cpa); // expected: t1>0, t2>0
        Log.d("AisTrackerLayer", "# test(a3, b2): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a3, b3, cpa); // expected: t1>0, t2>0
        Log.d("AisTrackerLayer", "# test(a3, b3): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a3, b4, cpa); // expected: t1>0, t2->0
        Log.d("AisTrackerLayer", "# test(a3, b4): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a3, b5, cpa); // expected: t1>0, t2<0
        Log.d("AisTrackerLayer", "# test(a3, b5): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a3, b6, cpa); // expected: t1>0, t2<0
        Log.d("AisTrackerLayer", "# test(a3, b6): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a4, b2, cpa); // expected: t1->0, t2>0
        Log.d("AisTrackerLayer", "# test(a4, b2): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a4, b3, cpa); // expected: t1->0, t2>0
        Log.d("AisTrackerLayer", "# test(a4, b3): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a4, b4, cpa); // expected: t1->0, t2->0
        Log.d("AisTrackerLayer", "# test(a4, b4): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a4, b5, cpa); // expected: t1->0, t2<0
        Log.d("AisTrackerLayer", "# test(a4, b5): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a4, b6, cpa); // expected: t1->0, t2<0
        Log.d("AisTrackerLayer", "# test(a4, b6): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a5, b2, cpa); // expected: t1<0, t2>0
        Log.d("AisTrackerLayer", "# test(a5, b2): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a5, b3, cpa); // expected: t1<0, t2>0
        Log.d("AisTrackerLayer", "# test(a5, b3): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a5, b4, cpa); // expected: t1<0, t2->0
        Log.d("AisTrackerLayer", "# test(a5, b4): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a5, b5, cpa); // expected: t1<0, t2<0
        Log.d("AisTrackerLayer", "# test(a5, b5): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());
        cpa.reset(); getCpa(a5, b6, cpa); // expected: t1<0, t2<0
        Log.d("AisTrackerLayer", "# test(a5, b6): t1->" + cpa.getCrossingTime1() +
                ", t2->" + cpa.getCrossingTime2() + ", CPA-Dist-> " + cpa.getCpaDist() +
                ", TCPA ->" + cpa.getTcpa());

    }

    private void testCpa() {
        // here some tests for the geo (CPA) calculation
        // define 3 (vessel) objects
        // for coordinate transformation see https://www.koordinaten-umrechner.de
        Location x1 = new Location("test", 49.5d, -1.0d); // 49°30'N, 1°00'W
        Location x2 = new Location("test", 49.916667d, 0.416667d); // 49°55'N, 0°25'E
        Location x3 = new Location("test", 49.666667d, -0.75d); // 49°40'N, 0°45'W
        Location x4 = new Location("test", 49.5d, -4.0d); // 49°30'N, 4°00'W
        Location x5 = new Location("test", 50.0d, -3.75d); // 50°00'N, 3°45'W
        // taken from marine chart: distances: x1 - x3: 13.8 nm, x2 - x3: 47,2 nm, x4 - x5: 31.4 nm
        Location y1, y2, y3, y4, y5;
        Log.d("AisTrackerLayer", "# test0: position 1 after 0 hours: "
                + LocationConvert.convertLatitude(x1.getLatitude(), FORMAT_MINUTES, true)
                +  ", " + LocationConvert.convertLongitude(x1.getLongitude(), FORMAT_MINUTES, true));
        Log.d("AisTrackerLayer", "# test0: position 2 after 0 hours: "
                + LocationConvert.convertLatitude(x2.getLatitude(), FORMAT_MINUTES, true)
                +  ", " + LocationConvert.convertLongitude(x2.getLongitude(), FORMAT_MINUTES, true));
        Log.d("AisTrackerLayer", "# test0: position 3 after 0 hours: "
                + LocationConvert.convertLatitude(x3.getLatitude(), FORMAT_MINUTES, true)
                +  ", " + LocationConvert.convertLongitude(x3.getLongitude(), FORMAT_MINUTES, true));
        Log.d("AisTrackerLayer", "# test0: position 4 after 0 hours: "
                + LocationConvert.convertLatitude(x4.getLatitude(), FORMAT_MINUTES, true)
                +  ", " + LocationConvert.convertLongitude(x4.getLongitude(), FORMAT_MINUTES, true));
        Log.d("AisTrackerLayer", "# test0: position 5 after 0 hours: "
                + LocationConvert.convertLatitude(x5.getLatitude(), FORMAT_MINUTES, true)
                +  ", " + LocationConvert.convertLongitude(x5.getLongitude(), FORMAT_MINUTES, true));

        // test case: x1: course 0°, speed 5kn, x3: course 270°, speed 10kn, time: 1h, 1.5h
        // taken from marine chart:
        //  position after 1h: x1: 49°35'N, 1°00'W, x3: 49°40'N, 1°0.5'W, distance: 5.0nm
        //  position after 1.5h: x1: 49°37.5'N, 1°00'W, x3: 49°40'N, 1°8.5'W, distance: 6.0nm
        x1.setSpeed(knotsToMeterPerSecond(5.0f));
        x1.setBearing(0.0f);
        x3.setSpeed(knotsToMeterPerSecond(10.0f));
        x3.setBearing(270.0f);
        AisTrackerHelper.Cpa cpa1 = new AisTrackerHelper.Cpa();
        getCpa(x1, x3, cpa1);
        Log.d("AisTrackerLayer", "# test1: tcpa(x1, x3): " + cpa1.getTcpa());
        Log.d("AisTrackerLayer", "# test1: dist at tcpa: " + cpa1.getCpaDist());
        Log.d("AisTrackerLayer", "# test1: dist0: " + meterToMiles(x1.distanceTo(x3)));
        y1 = AisTrackerHelper.getNewPosition(x1, 1.0);
        y3 = AisTrackerHelper.getNewPosition(x3, 1.0);
        if ((y1 != null) && (y3 != null)) {
            Log.d("AisTrackerLayer", "# test1: position 1 after 1 hour: "
                    + LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test1: position 3 after 1 hour: "
                    + LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test1: dist1: " + meterToMiles(y1.distanceTo(y3)));
        }
        y1 = AisTrackerHelper.getNewPosition(x1, 1.18);
        y3 = AisTrackerHelper.getNewPosition(x3, 1.18);
        if ((y1 != null) && (y3 != null)) {
            Log.d("AisTrackerLayer", "# test1: position 1 after 1.18 hours: "
                    + LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test1: position 3 after 1.18 hours: "
                    + LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test1: dist1: " + meterToMiles(y1.distanceTo(y3)));
        }
        y1 = AisTrackerHelper.getNewPosition(x1, 1.5);
        y3 = AisTrackerHelper.getNewPosition(x3, 1.5);
        if ((y1 != null) && (y3 != null)) {
            Log.d("AisTrackerLayer", "# test1: position 1 after 1.5 hours: "
                    + LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test1: position 3 after 1.5 hours: "
                    + LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test1: dist1: " + meterToMiles(y1.distanceTo(y3)));
        }

        // test case: x1: course 0°, speed 5kn, x3: course 270°, speed 5kn, time 1h, 1.5h, 2h
        // taken from marine chart:
        //  position after 1h: x1: 49°35'N, 1°00'W, x3: 49°40'N, 0°52.7'W, distance: 6.8nm
        //  position after 1.5h: x1: 49°37.5'N, 1°00'W, x3: 49°40'N, 0°56.7'W, distance: 3.1nm
        //  position after 2h: x1: 49°40'N, 1°00'W, x3: 49°40'N, 1°0.5'W, distance: 0.3nm
        x1.setSpeed(knotsToMeterPerSecond(5.0f));
        x1.setBearing(0.0f);
        x3.setSpeed(knotsToMeterPerSecond(5.0f));
        x3.setBearing(270.0f);
        cpa1.reset();
        getCpa(x1, x3, cpa1);
        Log.d("AisTrackerLayer", "# test2: tcpa(x1, x3): " + cpa1.getTcpa());
        Log.d("AisTrackerLayer", "# test2: dist at tcpa: " + cpa1.getCpaDist());
        Log.d("AisTrackerLayer", "# test2: dist0: " + meterToMiles(x1.distanceTo(x3)));
        y1 = AisTrackerHelper.getNewPosition(x1, 1.0);
        y3 = AisTrackerHelper.getNewPosition(x3, 1.0);
        if ((y1 != null) && (y3 != null)) {
            Log.d("AisTrackerLayer", "# test2: position 1 after 1 hour: "
                    + LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test2: position 3 after 1 hour: "
                    + LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test2: dist1: " + meterToMiles(y1.distanceTo(y3)));
        }
        y1 = AisTrackerHelper.getNewPosition(x1, 1.5);
        y3 = AisTrackerHelper.getNewPosition(x3, 1.5);
        if ((y1 != null) && (y3 != null)) {
            Log.d("AisTrackerLayer", "# test2: position 1 after 1.5 hours: "
                    + LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test2: position 3 after 1.5 hours: "
                    + LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test2: dist1: " + meterToMiles(y1.distanceTo(y3)));
        }
        y1 = AisTrackerHelper.getNewPosition(x1, 2.0);
        y3 = AisTrackerHelper.getNewPosition(x3, 2.0);
        if ((y1 != null) && (y3 != null)) {
            Log.d("AisTrackerLayer", "# test2: position 1 after 2 hours: "
                    + LocationConvert.convertLatitude(y1.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y1.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test2: position 3 after 2 hours: "
                    + LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test2: dist1: " + meterToMiles(y1.distanceTo(y3)));
        }

        // test case: x2: course 270°, speed 5kn, x3: course 45°, speed 5kn, time 5h
        // taken from marine chart:
        //  position after 5h: x2: 49°55'N, 0°14.1'W, x3: 49°57.8'N, 0°17.5'W, distance: 3.5nm
        x2.setSpeed(knotsToMeterPerSecond(5.0f));
        x2.setBearing(270.0f);
        x3.setSpeed(knotsToMeterPerSecond(5.0f));
        x3.setBearing(45.0f);
        cpa1.reset();
        getCpa(x2, x3, cpa1);
        Log.d("AisTrackerLayer", "# test3: tcpa(x1, x3): " + cpa1.getTcpa());
        Log.d("AisTrackerLayer", "# test3: dist at tcpa: " + cpa1.getCpaDist());
        Log.d("AisTrackerLayer", "# test3: dist0: " + meterToMiles(x2.distanceTo(x3)));
        y2 = AisTrackerHelper.getNewPosition(x2, 5.0);
        y3 = AisTrackerHelper.getNewPosition(x3, 5.0);
        if ((y2 != null) && (y3 != null)) {
            Log.d("AisTrackerLayer", "# test3: position 2 after 5 hours: "
                    + LocationConvert.convertLatitude(y2.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y2.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test3: position 3 after 5 hours: "
                    + LocationConvert.convertLatitude(y3.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y3.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test3: dist1: " + meterToMiles(y2.distanceTo(y3)));
        }

        // test case: x4: course 45°, speed 10kn, x5: course 70°, speed 5kn, time 6h
        // taken from marine chart:
        //  position after 6h: x4: 50°12.1'N, 2°54.4'W, x5: 50°10.1'N, 3°1.5'W, distance: 5nm
        x4.setSpeed(knotsToMeterPerSecond(10.0f));
        x4.setBearing(45.0f);
        x5.setSpeed(knotsToMeterPerSecond(5.0f));
        x5.setBearing(70.0f);
        cpa1.reset();
        getCpa(x4, x5, cpa1);
        Log.d("AisTrackerLayer", "# test4: tcpa(x4, x5): " + cpa1.getTcpa());
        Log.d("AisTrackerLayer", "# test4: dist at tcpa: " + cpa1.getCpaDist());
        Log.d("AisTrackerLayer", "# test4: dist0: " + meterToMiles(x4.distanceTo(x5)));
        y4 = AisTrackerHelper.getNewPosition(x4, 6.0);
        y5 = AisTrackerHelper.getNewPosition(x5, 6.0);
        if ((y4 != null) && (y5 != null)) {
            Log.d("AisTrackerLayer", "# test4: position 4 after 6 hours: "
                    + LocationConvert.convertLatitude(y4.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y4.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test4: position 5 after 6 hours: "
                    + LocationConvert.convertLatitude(y5.getLatitude(), FORMAT_MINUTES, true)
                    +  ", " + LocationConvert.convertLongitude(y5.getLongitude(), FORMAT_MINUTES, true));
            Log.d("AisTrackerLayer", "# test4: dist1: " + meterToMiles(y4.distanceTo(y5)));
        }
    }

    private void initFakePosition() {
        // fake the own position, course and speed to a (fixed) hard coded value
        double fakeLat = 50.76077d;
        double fakeLon = 7.08747d;
        float fakeCOG = 340.0f;
        //float fakeCOG = 100.0f;
        float fakeSOG = 3.0f;
        Location fake = new Location("test", fakeLat, fakeLon);
        fake.setBearing(fakeCOG);
        fake.setSpeed(knotsToMeterPerSecond(fakeSOG));
        AisObject.fakeOwnPosition(fake);
        Log.d("AisTrackerLayer", "initFakePosition: fake: " + fake.toString());
        // in order to visualize this faked (own) position on the map, create an AIS object at this location...
        AisObject ais = new AisObject(324578, 18, 20, AisObjectConstants.INVALID_NAV_STATUS,
                AisObjectConstants.INVALID_MANEUVER_INDICATOR,
                (int)fakeCOG, fakeCOG, fakeSOG, fakeLat, fakeLon, AisObjectConstants.INVALID_ROT);
        updateAisObjectList(ais);
        ais = new AisObject(324578, 24, 0, "callsign", "fake", 60, 56,
                65, 8, 12, AisObjectConstants.INVALID_DRAUGHT,
                "home", AisObjectConstants.INVALID_ETA, AisObjectConstants.INVALID_ETA,
                AisObjectConstants.INVALID_ETA_HOUR, AisObjectConstants.INVALID_ETA_MIN);
        updateAisObjectList(ais);
        //AisObject ais = new AisObject(324578, 1, 20, 0, 1, (int)fakeCOG,
        //        fakeCOG, fakeSOG, fakeLat, fakeLon, 0.0);
        //updateAisObjectList(ais);
        //ais = new AisObject(324578, 5, 0, "own-position", "fake", 60 /* passenger */, 56,
        //        65, 8, 12, 2,
        //        "home", 8, 15, 22, 5);
        //updateAisObjectList(ais);
    }

    private void initTestObject1() {
        // passenger ship
        AisObject ais = new AisObject(34568, 1, 20, 0, 1, 320,
                320.0, 8.4, 50.738d, 7.099d, 0.0);
        updateAisObjectList(ais);
        ais = new AisObject(34568, 5, 0, "TEST-CALLSIGN1", "TEST-Ship", 60 /* passenger */, 56,
                65, 8, 12, 2,
                "Potsdam", 8, 15, 22, 5);
        updateAisObjectList(ais);
    }
    private void initTestObject2() {
        // sailing boat
        AisObject ais = new AisObject(454011, 18, 20, AisObjectConstants.INVALID_NAV_STATUS,
                AisObjectConstants.INVALID_MANEUVER_INDICATOR,
                125, 125.0, 4.4, 50.737d, 7.098d, AisObjectConstants.INVALID_ROT);
        updateAisObjectList(ais);
        ais = new AisObject(454011, 24, 0, "TEST-CALLSIGN2", "TEST-Sailor", 36 /* sailing  */, 0,
                0, 0, 0, AisObjectConstants.INVALID_DRAUGHT,
                "home", AisObjectConstants.INVALID_ETA, AisObjectConstants.INVALID_ETA,
                AisObjectConstants.INVALID_ETA_HOUR, AisObjectConstants.INVALID_ETA_MIN);
        updateAisObjectList(ais);
    }
    private void initTestObject3() {
        // land station
        AisObject ais = new AisObject(878121, 4, 50.736d, 7.100d);
        updateAisObjectList(ais);
        // AIDS
        ais = new AisObject( 521077, 21, 50.735d, 7.101d, 1,
                0, 0, 0, 0);
        updateAisObjectList(ais);
    }
    private void initTestObject4() {
        // aircraft
        AisObject ais = new AisObject(910323, 9, 15, 65, 180.5, 55.0, 50.734d, 7.102d);
        updateAisObjectList(ais);
    }
    private void initTestObject5() {
        // law enforcement
        AisObject ais = new AisObject(34569, 1, 20, 5 /* moored */, 1, 15,
                25.0, 8.4, 50.739d, 7.0931d, 0.0);
        updateAisObjectList(ais);
        ais = new AisObject(34569, 5, 0, "TEST-CALLSIGN3",
                "Mecklenburg Vorpommern", 55 /* law enforcement */, 26,
                5, 8, 4, 1,
                "Potsdam", 8, 15, 22, 5);
        updateAisObjectList(ais);
    }

    private void initTimer() {
        TimerTask taskCheckAisObjectList;
        taskCheckAisObjectList = new TimerTask() {
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

    /* this method restarts the TCP listeners after a "resume" event (the smartphone resumed
    *  from sleep or from switched off state): in this case the TCP connection might be broken,
    *  but the sockets are still (logically) open.
    *  as additional indication of a broken TCP connection it is checked whether any AIS message
    *  was received in the last 20 seconds  */
    public void checkTcpConnection() {
        if (listener != null) {
            if (listener.checkTcpSocket()) {
                if (((System.currentTimeMillis() - AisObject.getAndUpdateLastMessageReceived()) / 1000) > 20) {
                    Log.d("AisTrackerLayer", "checkTcpConnection(): restart TCP socket");
                    restartNetworkListener();
                }
            }
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
            if (aisObjectList.size() >= aisObjectListCounterMax) {
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
        AisObject.setOwnPosition(getApplication().getLocationProvider().getLastKnownLocation());
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
            AisObjectConstants.AisObjType objectClass = ais.getObjectClass();
            if (ais.getShipName() != null) {
                return new PointDescription("AIS object", ais.getShipName() +
                        (ais.getSignalLostState() ? " (signal lost)" : ""));
            } else if (objectClass == AIS_LANDSTATION) {
                return new PointDescription("AIS object", "Land Station with MMSI " + ais.getMmsi());
            } else if (objectClass == AIS_AIRPLANE) {
                return new PointDescription("AIS object", "Airplane with MMSI " +
                        ais.getMmsi() + (ais.getSignalLostState() ? " (signal lost)" : ""));
            } else if ((objectClass == AIS_ATON) || (objectClass == AIS_ATON_VIRTUAL)) {
                return new PointDescription("AIS object", "Aid to Navigation");
            } else if (objectClass == AIS_SART) {
                return new PointDescription("AIS object", "SART (Search and Rescue Transmitter)");
            }
            return new PointDescription("AIS object",
                    "AIS object with MMSI " + ais.getMmsi() +
                            (ais.getSignalLostState() ? " (signal lost)" : ""));
        }
        return null;
    }
}
