package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisObjType.*;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.*;
import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.getCpa;
import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.getNewPosition;
import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_CPA_DEFAULT_WARNING_TIME;
import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_CPA_WARNING_DEFAULT_DISTANCE;
import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_OBJ_LOST_DEFAULT_TIMEOUT;
import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_SHIP_LOST_DEFAULT_TIMEOUT;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.core.jni.ColorARGB;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.SingleSkImage;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.VectorLine;
import net.osmand.core.jni.VectorLineBuilder;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class AisObject {
    /* variable names starting with "ais_" belong to values received via an AIS message,
    *  its values may differ from the received values: they can be scaled,
    *  see https://gpsd.gitlab.io/gpsd/AIVDM.html */
    private int ais_msgType;
    private int ais_mmsi;
    private int ais_timeStamp = 0;
    private int ais_imo = 0;
    private int ais_heading = INVALID_HEADING;
    private int ais_navStatus = INVALID_NAV_STATUS;
    private int ais_manInd = INVALID_MANEUVER_INDICATOR;
    private int ais_shipType = INVALID_SHIP_TYPE;
    private int ais_dimensionToBow = INVALID_DIMENSION;
    private int ais_dimensionToStern = INVALID_DIMENSION;
    private int ais_dimensionToPort = INVALID_DIMENSION;
    private int ais_dimensionToStarboard = INVALID_DIMENSION;
    private int ais_etaMon = INVALID_ETA;
    private int ais_etaDay = INVALID_ETA;
    private int ais_etaHour = INVALID_ETA_HOUR;
    private int ais_etaMin = INVALID_ETA_MIN;
    private int ais_altitude = INVALID_ALTITUDE;
    private int ais_aidType = UNSPECIFIED_AID_TYPE;
    private double ais_draught = INVALID_DRAUGHT;
    private double ais_cog = INVALID_COG;
    private double ais_sog = INVALID_SOG;
    private double ais_rot = INVALID_ROT;
    private LatLon ais_position = null;
    private String ais_callSign = null;
    private String ais_shipName = null;
    private String ais_destination = null;
    private String countryCode = null;
    private SortedSet<Integer> msgTypes = null;
    /* timestamp of last AIS message received for the current instance: */
    private long lastUpdate = 0;
    /* timestamp of last AIS message received for all instances: */
    private static long lastMessageReceived = 0;
    /* after this time of missing AIS signal the object is outdated and can be removed: */
    private static int maxObjectAgeInMinutes = AIS_OBJ_LOST_DEFAULT_TIMEOUT;
    /* after this time of missing AIS signal the vessel symbol can change to mark "lost": */
    private static int vesselLostTimeoutInMinutes = AIS_SHIP_LOST_DEFAULT_TIMEOUT;
    private static int cpaWarningTime = AIS_CPA_DEFAULT_WARNING_TIME; // in minutes
    private static float cpaWarningDistance = AIS_CPA_WARNING_DEFAULT_DISTANCE; // in miles
    private static Location ownPosition = null; // used to calculate distances, CPA etc.
    private static boolean ownPositionFaked = false; // used for test purposes to fake own position
    private AisObjType objectClass;
    private Bitmap bitmap = null;
    private boolean bitmapValid = false;
    private int bitmapColor;
    private AisTrackerHelper.Cpa cpa;
    private long lastCpaUpdate = 0;
    private boolean vesselAtRest = false; // if true, draw a circle instead of a bitmap
    private MapMarker activeMarker;
    private MapMarker restMarker;
    private MapMarker lostMarker;
    private VectorLine directionLine;

    public AisObject(int mmsi, int msgType, double lat, double lon) {
        initObj(mmsi, msgType);
        initLatLon(lat, lon);
        initObjectClass();
    }
    public AisObject(int mmsi, int msgType, int timeStamp, int navStatus, int manInd, int heading,
                     double cog, double sog, double lat, double lon, double rot) {
        initObj(mmsi, msgType);
        initLatLon(lat, lon);
        this.ais_timeStamp = timeStamp;
        this.ais_navStatus = navStatus;
        this.ais_manInd = manInd;
        this.ais_heading = heading;
        this.ais_cog = cog;
        this.ais_sog = sog;
        this.ais_rot = rot;
        initObjectClass();
    }
    public AisObject(int mmsi, int msgType, int timeStamp, int altitude,
                     double cog, double sog, double lat, double lon) {
        initObj(mmsi, msgType);
        initLatLon(lat, lon);
        this.ais_timeStamp = timeStamp;
        this.ais_altitude = altitude;
        this.ais_cog = cog;
        this.ais_sog = sog;
        initObjectClass();
    }
    public AisObject(int mmsi, int msgType, int timeStamp, int heading,
                     double cog, double sog, double lat, double lon,
                     int shipType, int dimensionToBow, int dimensionToStern,
                     int dimensionToPort, int dimensionToStarboard) {
        initObj(mmsi, msgType);
        initLatLon(lat, lon);
        initDimensions(dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard);
        this.ais_timeStamp = timeStamp;
        this.ais_heading = heading;
        this.ais_cog = cog;
        this.ais_sog = sog;
        this.ais_shipType = shipType;
        initObjectClass();
    }
    public AisObject(int mmsi, int msgType, int imo, @Nullable String callSign, @Nullable String shipName,
                     int shipType, int dimensionToBow, int dimensionToStern,
                     int dimensionToPort, int dimensionToStarboard,
                     double draught, @Nullable String destination, int etaMon,
                     int etaDay, int etaHour, int etaMin) {
        initObj(mmsi, msgType);
        initDimensions(dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard);
        this.ais_shipType = shipType;
        this.ais_draught = draught;
        this.ais_callSign = callSign;
        this.ais_shipName = shipName;
        if (destination != null) {
            if (!destination.matches("^@+$")) { // string consisting of only "@" characters is invalid
                this.ais_destination = destination;
            }
        }
        this.ais_etaMon = etaMon;
        this.ais_etaDay = etaDay;
        this.ais_etaHour = etaHour;
        this.ais_etaMin = etaMin;
        this.ais_imo = imo;
        initObjectClass();
    }

    public AisObject(int mmsi, int msgType, double lat, double lon, int aidType,
                     int dimensionToBow, int dimensionToStern,
                     int dimensionToPort, int dimensionToStarboard) {
        initObj(mmsi, msgType);
        initLatLon(lat, lon);
        initDimensions(dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard);
        this.ais_aidType = aidType;
        initObjectClass();
    }
    public AisObject(@NonNull AisObject ais) {
        this.set(ais);
    }
    private String getCountryCode(Integer mmsi) {
        String mmsiString = mmsi.toString();

        if (mmsiString.length() > 2) {
            String countryCode = mmsiString.substring(0, 3);
            mmsiString = COUNTRY_CODES.get(Integer.parseInt(countryCode));
            if (mmsiString != null) {
                return mmsiString;
            }
        }
        return "";
    }
    /* to be called only by a contructor! */
    private void initObj(int mmsi, int msgType) {
        this.msgTypes = new TreeSet<>();
        this.cpa = new AisTrackerHelper.Cpa();
        this.ais_mmsi = mmsi;
        this.ais_msgType = msgType;
        this.countryCode = getCountryCode(this.ais_mmsi);
        this.msgTypes.add(ais_msgType);
        this.lastUpdate = System.currentTimeMillis();
        lastMessageReceived = this.lastUpdate;
    }
    private void initLatLon(double lat, double lon) {
        if ((lat != INVALID_LAT) && (lon != INVALID_LON)) {
            ais_position = new LatLon(lat, lon);
        }
    }

    private void initDimensions(int dimensionToBow, int dimensionToStern,
                                int dimensionToPort, int dimensionToStarboard) {
        this.ais_dimensionToBow = dimensionToBow;
        this.ais_dimensionToStern = dimensionToStern;
        this.ais_dimensionToPort = dimensionToPort;
        this.ais_dimensionToStarboard = dimensionToStarboard;
    }

    private void initObjectClass() {
        switch (this.ais_shipType) {
            case INVALID_SHIP_TYPE: // not initialized
                break;

            case 20: // Wing in ground (WIG)
            case 21: // WIG, Hazardous category A
            case 22: // WIG, Hazardous category B
            case 23: // WIG, Hazardous category C
            case 24: // WIG, Hazardous category D
            case 40: // High Speed Craft (HSC)
            case 41: // HSC, Hazardous category A
            case 42: // HSC, Hazardous category B
            case 43: // HSC, Hazardous category C
            case 44: // HSC, Hazardous category D
            case 49: // HSC, No additional information
                this.objectClass = AIS_VESSEL_FAST;
                break;

            case 30: // Fishing
            case 31: // Towing
            case 32: // Towing
            case 33: // Dredging
            case 34: // Diving ops
            case 50: // Pilot Vessel
            case 52: // Tug
            case 53: // Port Tender
            case 54: // Anti-pollution equipment
            case 56: // Spare - Local Vessel
            case 57: // Spare - Local Vessel
            case 59: // Noncombatant ship according to RR Resolution No. 18
                this.objectClass = AIS_VESSEL_COMMERCIAL;
                break;

            case 35: // Military ops
            case 55: // Law Enforcement
                this.objectClass = AIS_VESSEL_AUTHORITIES;
                break;

            case 51: // Search and Rescue vessel
            case 58: // Medical Transport
                this.objectClass = AIS_VESSEL_SAR;
                break;

            case 36: // Sailing
            case 37: // Pleasure Craft
                this.objectClass = AIS_VESSEL_SPORT;
                break;

            case 60: // Passenger, all ships of this type
            case 61: // Passenger, Hazardous category A
            case 62: // Passenger, Hazardous category B
            case 63: // Passenger, Hazardous category C
            case 64: // Passenger, Hazardous category D
            case 69: // Passenger, No additional information
                this.objectClass = AIS_VESSEL_PASSENGER;
                break;

            case 70: // Cargo, all ships of this type
            case 71: // Cargo, Hazardous category A
            case 72: // Cargo, Hazardous category B
            case 73: // Cargo, Hazardous category C
            case 74: // Cargo, Hazardous category D
            case 79: // Cargo, No additional information
            case 80: // Tanker, all ships of this type
            case 81: // Tanker, Hazardous category A
            case 82: // Tanker, Hazardous category B
            case 83: // Tanker, Hazardous category C
            case 84: // Tanker, Hazardous category D
            case 89: // Tanker, No additional information
                this.objectClass = AIS_VESSEL_FREIGHT;
                break;

            case 90: // Other Type, all ships of this type
            case 91: // Other Type, Hazardous category A
            case 92: // Other Type, Hazardous category B
            case 93: // Other Type, Hazardous category C
            case 94: // Other Type, Hazardous category D
            case 99: // Other Type, no additional information
            default:
                this.objectClass = AIS_VESSEL_OTHER;
                break;
        }
        /* for the case that no ship type was transmitted... */
        if (ais_shipType == INVALID_SHIP_TYPE) {
            if (msgTypes.contains(9)) { // aircraft
                this.objectClass = AIS_AIRPLANE;
            } else if (msgTypes.contains(4)) { // base station
                this.objectClass = AIS_LANDSTATION;
            } else if (msgTypes.contains(21)) { // aids to navigation
                switch (ais_aidType) {
                    // see https://gpsd.gitlab.io/gpsd/AIVDM.html#_type_21_aid_to_navigation_report
                    case 29: // Safe Water
                    case 30: // Special Mark
                        this.objectClass = AIS_ATON_VIRTUAL;
                        break;
                    default:
                        this.objectClass = AIS_ATON;
                }
            } else if (msgTypes.contains(18)) {
                this.objectClass = AIS_VESSEL;
            } else {
                switch (ais_navStatus) {
                    // see https://gpsd.gitlab.io/gpsd/AIVDM.html#_types_1_2_and_3_position_report_class_a
                    case 0: // Under way using engine
                    case 1: // At anchor
                    case 2: // Not under command
                    case 3: // Restricted manoeuverability
                    case 4: // Constrained by her draught
                    case 5: // Moored
                    case 6: // Aground
                    case 8: // Under way sailing
                    case 11: // Power-driven vessel towing astern (regional use)
                    case 12: // Power-driven vessel pushing ahead or towing alongside (regional use).
                        this.objectClass = AIS_VESSEL;
                        break;

                    case 7: // Engaged in Fishing
                        this.objectClass = AIS_VESSEL_COMMERCIAL;
                        break;

                    case 14: // AIS-SART is active
                        this.objectClass = AIS_SART;
                        break;

                    case INVALID_NAV_STATUS: // no valid value
                    default:
                        this.objectClass = AIS_INVALID;
                }
            }
        }
    }

    private void invalidateBitmap() {
        this.bitmapValid = false;
    }

    public void set(@NonNull AisObject ais) {
        /* attention: this method does not produce an exact copy of the given object */
        this.ais_mmsi = ais.getMmsi();
        this.ais_msgType = ais.getMsgType();
        if (ais.getTimestamp() != 0) { this.ais_timeStamp = ais.getTimestamp(); }
        if (ais.getImo() != 0 ) { this.ais_imo = ais.getImo(); }
        if (ais.getShipType() != INVALID_SHIP_TYPE ) { this.ais_shipType = ais.getShipType(); }
        if (ais.getDimensionToBow() != INVALID_DIMENSION ) { this.ais_dimensionToBow = ais.getDimensionToBow(); }
        if (ais.getDimensionToStern() != INVALID_DIMENSION ) { this.ais_dimensionToStern = ais.getDimensionToStern(); }
        if (ais.getDimensionToPort() != INVALID_DIMENSION ) { this.ais_dimensionToPort = ais.getDimensionToPort(); }
        if (ais.getDimensionToStarboard() != INVALID_DIMENSION ) { this.ais_dimensionToStarboard = ais.getDimensionToStarboard(); }
        if (ais.getEtaMon() != INVALID_ETA ) { this.ais_etaMon = ais.getEtaMon(); }
        if (ais.getEtaDay() != INVALID_ETA ) { this.ais_etaDay = ais.getEtaDay(); }
        if (ais.getEtaHour() != INVALID_ETA_HOUR ) { this.ais_etaHour = ais.getEtaHour(); }
        if (ais.getEtaMin() != INVALID_ETA_MIN ) { this.ais_etaMin = ais.getEtaMin(); }
        if (ais.getAltitude() != INVALID_ALTITUDE) { this.ais_altitude = ais.getAltitude(); }
        if (ais.getAidType() != UNSPECIFIED_AID_TYPE) { this.ais_aidType = ais.getAidType(); }
        if (ais.getDraught() != INVALID_DRAUGHT) { this.ais_draught = ais.getDraught(); }
        if (ais.getPosition() != null) { this.ais_position = ais.getPosition(); }
        if (ais.getCallSign() != null) { this.ais_callSign = ais.getCallSign(); }
        if (ais.getShipName() != null) { this.ais_shipName = ais.getShipName(); }
        if (ais.getDestination() != null ) { this.ais_destination = ais.getDestination(); }

        /* the following values may change its value from VALID to INVALID,
           hence overwriting with INVALID is accepted in some cases... */
        final List<Integer> msgListHeading = Arrays.asList(1, 2, 3, 18, 19, 27);
        final List<Integer> msgListStatus = Arrays.asList(1, 2, 3, 27);
        final List<Integer> msgListCourse = Arrays.asList(1, 2, 3, 9, 18, 19, 27);
        if (msgListHeading.contains(ais_msgType)) {
            this.ais_heading = ais.getHeading();
        }
        if (msgListStatus.contains(ais_msgType)) {
            this.ais_navStatus = ais.getNavStatus();
            this.ais_manInd = ais.getManInd();
            this.ais_rot = ais.getRot();
        }
        if (msgListCourse.contains(ais_msgType)) {
            this.ais_cog = ais.getCog();
            this.ais_sog = ais.getSog();
        }

        this.countryCode = ais.getCountryCode();
        this.lastUpdate = System.currentTimeMillis();
        lastMessageReceived = this.lastUpdate; // lastMessageReceived is a static variable for the entire AisObject class
        if (this.msgTypes == null) {
            this.msgTypes = new TreeSet<>();
        }
        this.msgTypes.add(ais_msgType);
        if (this.cpa == null) {
            cpa = new AisTrackerHelper.Cpa();
        }
        this.initObjectClass();
        this.invalidateBitmap();
        this.bitmapColor = 0;
    }

    public static int selectBitmap(@NonNull AisObjType type) {
        return switch (type) {
            case AIS_VESSEL, AIS_VESSEL_SPORT, AIS_VESSEL_FAST, AIS_VESSEL_PASSENGER,
                 AIS_VESSEL_FREIGHT, AIS_VESSEL_COMMERCIAL, AIS_VESSEL_AUTHORITIES, AIS_VESSEL_SAR,
                 AIS_VESSEL_OTHER, AIS_INVALID -> R.drawable.mm_ais_vessel;
            case AIS_LANDSTATION -> R.drawable.mm_ais_land;
            case AIS_AIRPLANE -> R.drawable.mm_ais_plane;
            case AIS_SART -> R.drawable.mm_ais_sar;
            case AIS_ATON -> R.drawable.mm_ais_aton;
            case AIS_ATON_VIRTUAL -> R.drawable.mm_ais_aton_virt;
        };
    }

    public static int selectColor(@NonNull AisObjType type) {
	    return switch (type) {
		    case AIS_VESSEL -> Color.GREEN;
		    case AIS_VESSEL_SPORT -> Color.YELLOW;
		    case AIS_VESSEL_FAST -> Color.BLUE;
		    case AIS_VESSEL_PASSENGER -> Color.CYAN;
		    case AIS_VESSEL_FREIGHT -> Color.GRAY;
		    case AIS_VESSEL_COMMERCIAL -> Color.LTGRAY;
		    case AIS_VESSEL_AUTHORITIES ->
				    Color.argb(0xff, 0x55, 0x6b, 0x2f); // 0x556b2f: darkolivegreen
		    case AIS_VESSEL_SAR -> Color.argb(0xff, 0xfa, 0x80, 0x72); // 0xfa8072: salmon
		    case AIS_VESSEL_OTHER -> Color.argb(0xff, 0x00, 0xbf, 0xff); // 0x00bfff: deepskyblue
		    default -> 0; // default icon
	    };
    }

    private void setBitmap(@NonNull AisTrackerLayer mapLayer) {
        invalidateBitmap();
        vesselAtRest = isVesselAtRest();
        if (isLost(vesselLostTimeoutInMinutes) && !vesselAtRest) {
            if (isMovable()) {
                this.bitmap = mapLayer.getBitmap(R.drawable.mm_ais_vessel_cross);
                this.bitmapValid = true;
            }
        } else {
            int bitmapId = selectBitmap(this.objectClass);
            if (bitmapId >= 0) {
                this.bitmap = mapLayer.getBitmap(bitmapId);
                this.bitmapValid = true;
            }
        }
        this.setColor();
    }

    private void setColor() {
        if (isLost(vesselLostTimeoutInMinutes) && !vesselAtRest) {
            if (isMovable()) {
                this.bitmapColor = 0; // default icon
            }
        } else {
            this.bitmapColor = selectColor(this.objectClass);
        }
    }

    private void updateBitmap(@NonNull AisTrackerLayer mapLayer, @NonNull Paint paint) {
        if (isLost(vesselLostTimeoutInMinutes)) {
            setBitmap(mapLayer);
        } else {
            if (!this.bitmapValid) {
                setBitmap(mapLayer);
            }
            if (checkCpaWarning()) {
                activateCpaWarning();
            } else {
                deactivateCpaWarning();
            }
        }
        if (this.bitmapColor != 0) {
            paint.setColorFilter(new LightingColorFilter(this.bitmapColor, 0));
        } else {
            paint.setColorFilter(null);
        }
    }

    private void drawCircle(float locationX, float locationY,
                            @NonNull Paint paint, @NonNull Canvas canvas) {
        Paint localPaint = new Paint(paint);
        localPaint.setColorFilter(null);
        localPaint.setColor(Color.DKGRAY);
        canvas.drawCircle(locationX, locationY, 22.0f, localPaint);
        localPaint.setColor(this.bitmapColor);
        canvas.drawCircle(locationX, locationY, 18.0f, localPaint);
    }

    public void draw(@NonNull AisTrackerLayer mapLayer, @NonNull Paint paint,
                     @NonNull Canvas canvas, @NonNull RotatedTileBox tileBox) {
        updateBitmap(mapLayer, paint);
        if (this.bitmap != null) {
            canvas.save();
            canvas.rotate(tileBox.getRotate(), (float)tileBox.getCenterPixelX(), (float)tileBox.getCenterPixelY());
            float speedFactor = getMovement();
            int locationX = tileBox.getPixXFromLonNoRot(this.ais_position.getLongitude());
            int locationY = tileBox.getPixYFromLatNoRot(this.ais_position.getLatitude());
            float fx =  locationX - this.bitmap.getWidth() / 2.0f;
            float fy =  locationY - this.bitmap.getHeight() / 2.0f;
            if (!vesselAtRest && this.needRotation()) {
                float rotation = getVesselRotation();
                canvas.rotate(rotation, locationX, locationY);
            }
            if (vesselAtRest) {
                drawCircle(locationX, locationY, paint, canvas);
            } else {
                canvas.drawBitmap(this.bitmap, Math.round(fx), Math.round(fy), paint);
            }
            if ((speedFactor > 0) && (!isLost(vesselLostTimeoutInMinutes)) && !vesselAtRest) {
	            float lineLength = (float)this.bitmap.getHeight() * speedFactor;
                float lineStartY = locationY - this.bitmap.getHeight() / 4.0f;
                float lineEndY = lineStartY - lineLength;
                canvas.drawLine((float) locationX, lineStartY, (float) locationX, lineEndY, paint);
            }
            canvas.restore();
        }
    }

    public boolean isMovable() {
	    return switch (objectClass) {
		    case AIS_VESSEL, AIS_VESSEL_SPORT, AIS_VESSEL_FAST, AIS_VESSEL_PASSENGER,
		         AIS_VESSEL_FREIGHT, AIS_VESSEL_COMMERCIAL, AIS_VESSEL_AUTHORITIES, AIS_VESSEL_SAR,
		         AIS_VESSEL_OTHER, AIS_AIRPLANE -> true;
		    case AIS_INVALID -> (ais_sog != INVALID_SOG) && (ais_sog > 0.0d);
		    default -> false;
	    };
    }
    /*
    for AIS objects that are moving, return a value that is taken as multiple of bitmap
    height to draw a line to indicate the speed,
    otherwise return 0 (no movement)
    */
    private float getMovement() {
        if (this.ais_sog > 0.0d) {
            if (isMovable()) {
                if (this.ais_sog <  2.0d) { return 0.0f; }
                if (this.ais_sog <  5.0d) { return 1.0f; }
                if (this.ais_sog < 10.0d) { return 3.0f; }
                if (this.ais_sog < 25.0d) { return 6.0f; }
                return 8.0f;
            }
        }
        return 0.0f;
    }
    private boolean needRotation() {
        if (((this.ais_cog != INVALID_COG) && (this.ais_cog != 0)) ||
                ((this.ais_heading != INVALID_HEADING) && (this.ais_heading != 0)))
        {
            return isMovable();
        }
        return false;
    }
    /* return true if a vessel is moored etc. and needs to be drawn as a circle */
    private boolean isVesselAtRest() {
        switch (this.objectClass) {
            case AIS_VESSEL:
            case AIS_VESSEL_SPORT:
            case AIS_VESSEL_FAST:
            case AIS_VESSEL_PASSENGER:
            case AIS_VESSEL_FREIGHT:
            case AIS_VESSEL_COMMERCIAL:
            case AIS_VESSEL_AUTHORITIES:
            case AIS_VESSEL_SAR:
            case AIS_VESSEL_OTHER:
                switch (this.ais_navStatus) {
                    case 5: // moored
                        /* sometimes the ais_navStatus is wrong and contradicts other data... */
                        return (ais_cog == INVALID_COG) || (ais_sog < 0.2d);
                    default:
                        if (msgTypes.contains(18) || msgTypes.contains(24)
                        ||  msgTypes.contains(1)  || msgTypes.contains(3)) {
                            if ((ais_cog == INVALID_COG /* maybe remove this condition */)
                                    && (ais_sog < 0.2d)) {
                                return true;
                            }
                        }
                        return false;
                }
            default:
                return false;
        }
    }

    /* return true if the vessel gets too close with the own position in the future
    * (danger of collusion);
    * this situation occurs if all of the following conditions hold:
    *  (1) the calculated TCPA is in the future (>0)
    *  (2) the calculated CPA is not bigger than the configured warning distance
    *  (3) the calculated TCPA is not bigger than the configured warning time
    *  (4) the time when the own course crosses the course of the other vessel
    *      is not in the past
    *  (5) the time when the course of the other vessel crosses the own course
    *      is not in the past  */
    private boolean checkCpaWarning() {
        if (isMovable() && (objectClass != AIS_AIRPLANE) && (cpaWarningTime > 0) && (ais_sog > 0.0d)) {
            if (checkForCpaTimeout() && (ownPosition != null)) {
                Location aisPosition = getCurrentLocation();
                if (aisPosition != null) {
                    getCpa(ownPosition, aisPosition, cpa);
                    lastCpaUpdate = System.currentTimeMillis();
                }
            }
            if (cpa.isValid()) {
                double tcpa = cpa.getTcpa();
                if (tcpa > 0.0f) {
                    return ((cpa.getCpaDist() <= cpaWarningDistance) &&
                            ((tcpa * 60.0d) <= cpaWarningTime) &&
                            (cpa.getCrossingTime1() >= 0.0d) &&
                            (cpa.getCrossingTime2() >= 0.0d));
                }
            }
        }
        return false;
    }

    private void activateCpaWarning() {
        bitmapColor = Color.RED;
    }
    private void deactivateCpaWarning() {
        if (bitmapColor == Color.RED) {
            setColor();
        }
    }
    private boolean isLost(int maxAgeInMin) {
        return ((System.currentTimeMillis() - this.lastUpdate) / 1000 / 60) > maxAgeInMin;
    }
    private boolean checkForCpaTimeout() {
        return ((System.currentTimeMillis() - this.lastCpaUpdate) / 1000) > CPA_UPDATE_TIMEOUT_IN_SECONDS;
    }
    public static void setMaxObjectAge(int timeInMinutes) { maxObjectAgeInMinutes = timeInMinutes; }
    public static void setVesselLostTimeout(int timeInMinutes) { vesselLostTimeoutInMinutes = timeInMinutes; }
    public static void setCpaWarningTime(int warningTime) { cpaWarningTime = warningTime; }
    public static void setCpaWarningDistance(float warningDistance) { cpaWarningDistance = warningDistance; }
    public static void setOwnPosition(Location position) { if (!ownPositionFaked) { ownPosition = position; }}
    public static void fakeOwnPosition(Location fakePosition) { // used for test purposes
        ownPosition = fakePosition;
        ownPositionFaked = fakePosition != null;
    }
    /*
    * this function checks the age of the object (check lastUpdate against its limit)
    * and returns true if the object is outdated and can be removed
    * */
    public boolean checkObjectAge() {
        return isLost(maxObjectAgeInMinutes);
    }
    public int getMsgType() { return this.ais_msgType; }
    public SortedSet<Integer> getMsgTypes() { return this.msgTypes; }
    public int getMmsi() { return this.ais_mmsi; }
    public int getTimestamp() { return this.ais_timeStamp; }
    public int getImo() { return this.ais_imo; }
    public int getHeading() { return this.ais_heading; }
    public int getNavStatus() { return this.ais_navStatus; }
    public int getManInd() { return this.ais_manInd; }
    public int getShipType() { return this.ais_shipType; }
    public int getDimensionToBow() { return this.ais_dimensionToBow; }
    public int getDimensionToStern() { return this.ais_dimensionToStern; }
    public int getDimensionToPort() { return this.ais_dimensionToPort; }
    public int getDimensionToStarboard() { return this.ais_dimensionToStarboard; }
    public int getEtaMon() { return this.ais_etaMon; }
    public int getEtaDay() { return this.ais_etaDay; }
    public int getEtaHour() { return this.ais_etaHour; }
    public int getEtaMin() { return this.ais_etaMin; }
    public int getAltitude() { return this.ais_altitude; }
    public int getAidType() { return this.ais_aidType; }
    public double getCog() { return this.ais_cog; }
    public double getSog() { return this.ais_sog; }
    public double getRot() { return this.ais_rot; }
    public double getDraught() { return this.ais_draught; }
    @Nullable
    public LatLon getPosition() {
        return this.ais_position;
    }
    @Nullable
    public Location getLocation() {
        if (this.ais_position != null) {
            Location loc = new Location(AisTrackerPlugin.AISTRACKER_ID,
                    ais_position.getLatitude(), ais_position.getLongitude());
            if (ais_cog != INVALID_COG) {
                loc.setBearing((float)ais_cog);
            }
            if (ais_sog != INVALID_SOG) {
                loc.setSpeed((float)(ais_sog * 1852 / 3600)); // in m/s
            }
            if (ais_altitude != INVALID_ALTITUDE) {
                loc.setAltitude((float)ais_altitude);
            }
            return loc;
        }
        return null;
    }
    /* in contrast to getLocation(), this method considers the timestamp of the creation
    *  of the AIS object and adjusts the received position using the time difference
    *  between now and the timestamp (assuming that course and speed is constant) */
    @Nullable
    public Location getCurrentLocation() {
        Location loc = getLocation();
        Location newLocation = null;
        if (loc != null) {
            double ageInHours = (System.currentTimeMillis() - this.lastUpdate) / 1000.0 / 3600.0;
            newLocation = getNewPosition(loc, ageInHours);
        }
        return newLocation;
    }
    @Nullable public String getCallSign() { return this.ais_callSign; }
    @Nullable public String getShipName() { return this.ais_shipName; }
    @Nullable public String getDestination() { return this.ais_destination; }
    @NonNull  public String getCountryCode() { return this.countryCode; }
    public AisObjType getObjectClass() { return this.objectClass; }
    public long getLastUpdate() { return this.lastUpdate; }
    public static long getLastMessageReceived() { return lastMessageReceived; }
    public static long getAndUpdateLastMessageReceived() {
        long timestamp = getLastMessageReceived();
        lastMessageReceived = System.currentTimeMillis();
        return timestamp;
    }
    @NonNull
    public String getShipTypeString() {
        switch (this.ais_shipType) {
            case INVALID_SHIP_TYPE: // not initialized
                return("unknown");
            case 20:
                return("Wing in ground (WIG)");
            case 21:
                return("WIG, Hazardous category A");
            case 22:
                return("WIG, Hazardous category B");
            case 23:
                return("WIG, Hazardous category C");
            case 24:
                return("WIG, Hazardous category D");
            case 30:
                return("Fishing");
            case 31:
                return("Towing");
            case 32:
                return("Towing");
            case 33:
                return("Dredging");
            case 34:
                return("Diving ops");
            case 35:
                return("Military ops");
            case 36:
                return("Sailing");
            case 37:
                return("Pleasure Craft");
            case 40:
                return("High Speed Craft (HSC)");
            case 41:
                return("HSC, Hazardous category A");
            case 42:
                return("HSC, Hazardous category B");
            case 43:
                return("HSC, Hazardous category C");
            case 44:
                return("HSC, Hazardous category D");
            case 49: // HSC, No additional information
                return("High Speed Craft (HSC)");
            case 50:
                return("Pilot Vessel");
            case 51:
                return("Search and Rescue vessel");
            case 52:
                return("Tug");
            case 53:
                return("Port Tender");
            case 54:
                return("Anti-pollution equipment");
            case 55:
                return("Law Enforcement");
            case 56:
                return("Spare - Local Vessel");
            case 57:
                return("Spare - Local Vessel");
            case 58:
                return("Medical Transport");
            case 59:
                return("Noncombatant ship according to RR Resolution No. 18");
            case 60:
                return("Passenger");
            case 61:
                return("Passenger, Hazardous category A");
            case 62:
                return("Passenger, Hazardous category B");
            case 63:
                return("Passenger, Hazardous category C");
            case 64:
                return("Passenger, Hazardous category D");
            case 69: // Passenger, No additional information
                return("Passenger/Cruise/Ferry");
            case 70: // Cargo, all ships of this type
                return("Cargo");
            case 71:
                return("Cargo, Hazardous category A");
            case 72:
                return("Cargo, Hazardous category B");
            case 73:
                return("Cargo, Hazardous category C");
            case 74:
                return("Cargo, Hazardous category D");
            case 79: // Cargo, No additional information
                return("Cargo");
            case 80: // Tanker, all ships of this type
                return("Tanker");
            case 81:
                return("Tanker, Hazardous category A");
            case 82:
                return("Tanker, Hazardous category B");
            case 83:
                return("Tanker, Hazardous category C");
            case 84:
                return("Tanker, Hazardous category D");
            case 89: // Tanker, No additional information
                return("Tanker");
            case 90: // Other Type, all ships of this type
                return("Other Type");
            case 91:
                return("Other Type, Hazardous category A");
            case 92:
                return("Other Type, Hazardous category B");
            case 93:
                return("Other Type, Hazardous category C");
            case 94:
                return("Other Type, Hazardous category D");
            case 99: // Other Type, no additional information
                return("Other Type");
            default:
                return Integer.toString(ais_shipType);
        }
    }
    @NonNull
    public String getNavStatusString() {
        switch (this.ais_navStatus) {
            // see https://gpsd.gitlab.io/gpsd/AIVDM.html#_types_1_2_and_3_position_report_class_a
            case 0:
                return("Under way using engine");
            case 1:
                return("At anchor");
            case 2:
                return("Not under command");
            case 3:
                return("Restricted manoeuverability");
            case 4:
                return("Constrained by her draught");
            case 5:
                return("Moored");
            case 6:
                return("Aground");
            case 8:
                return("Under way sailing");
            case 11:
                return("Power-driven vessel towing astern (regional use)");
            case 12:
                return("Power-driven vessel pushing ahead or towing alongside (regional use)");
            case 7:
                return("Engaged in Fishing");
            case 14:
                return("AIS-SART is active");
            case INVALID_NAV_STATUS: // no valid value
                return("unknown");
            default:
                return(Integer.toString(ais_navStatus));
        }
    }
    @NonNull
    public String getManIndString() {
        // see https://gpsd.gitlab.io/gpsd/AIVDM.html#_types_1_2_and_3_position_report_class_a
        switch (this.ais_manInd) {
            case 0:
                return("Not available");
            case 1:
                return("No special maneuver");
            case 2:
                return("Special maneuver");
            default:
                return(Integer.toString(ais_manInd));
        }
    }
    @NonNull
    public String getAidTypeString() {
        switch (this.ais_aidType) {
            // see https://gpsd.gitlab.io/gpsd/AIVDM.html#_type_21_aid_to_navigation_report
            case 0:
                return("not specified");
            case 1:
                return("Reference point");
            case 2:
                return("RACON (radar transponder marking a navigation hazard)");
            case 3:
                return("Fixed structure off shore");
            case 4:
                return("Spare, Reserved for future use");
            case 5:
                return("Light, without sectors");
            case 6:
                return("Light, with sectors");
            case 7:
                return("Leading Light Front");
            case 8:
                return("Leading Light Rear");
            case 9:
                return("Beacon, Cardinal N");
            case 10:
                return("Beacon, Cardinal E");
            case 11:
                return("Beacon, Cardinal S");
            case 12:
                return("Beacon, Cardinal W");
            case 13:
                return("Beacon, Port hand");
            case 14:
                return("Beacon, Starboard hand");
            case 15:
                return("Beacon, Preferred Channel port hand");
            case 16:
                return("Beacon, Preferred Channel starboard hand");
            case 17:
                return("Beacon, Isolated danger");
            case 18:
                return("Beacon, Safe wate");
            case 19:
                return("Beacon, Special mark");
            case 20:
                return("Cardinal Mark N");
            case 21:
                return("Cardinal Mark E");
            case 22:
                return("Cardinal Mark S");
            case 23:
                return("Cardinal Mark W");
            case 24:
                return("Port hand Mark");
            case 25:
                return("Starboard hand Mark");
            case 26:
                return("Preferred Channel Port hand");
            case 27:
                return("Preferred Channel Starboard hand");
            case 28:
                return("Isolated danger");
            case 29:
                return("Safe Water");
            case 30:
                return("Special Mark");
            case 31:
                return("Light Vessel / LANBY / Rigs");
            default:
                return(Integer.toString(ais_aidType));
        }
    }
    private float getDistanceOrBearing(boolean needBearing) {
        Location aisLocation = getLocation();
        if ((ownPosition != null) && (aisLocation != null)) {
            return needBearing ? ownPosition.bearingTo(aisLocation) : ownPosition.distanceTo(aisLocation);
        } else {
            Log.e("AisObject", "getDistanceOrBearing(): ownLocation -> " + ownPosition +
                   ", aisLocation -> " + aisLocation);
            return -500.0f; // invalid
        }
    }
    /* get bearing from own position to the position of the AIS object */
    public float getBearing() {
        float bearing = getDistanceOrBearing(true);
        if ((bearing < 0.0f) && (bearing > -200.0f)) {
            while (bearing < 0.0f) {
                bearing += 360.0f;
            }
        }
        return bearing;
    }
    /* get distance from own position to the position of the AIS object in meters */
    public float getDistanceInMeters() {
        return getDistanceOrBearing(false);
    }
    public float getDistanceInNauticalMiles() {
        float dist = getDistanceInMeters();
        if (dist >= 0.0f) {
            dist = dist / 1852;
        }
        return dist;
    }
    public boolean getSignalLostState() {
        return (isLost(vesselLostTimeoutInMinutes) && isMovable() && !vesselAtRest);
    }

    private float getVesselRotation()
    {
        float rotation = 0;
        if (this.ais_cog != INVALID_COG) { rotation = (float)this.ais_cog; }
        else if (this.ais_heading != INVALID_HEADING ) { rotation = this.ais_heading; }
        return rotation;
    }

    public void createAisRenderData(int baseOrder, @NonNull AisTrackerLayer layer,
		    @NonNull Paint paint,
		    @NonNull MapMarkersCollection markersCollection,
		    @NonNull VectorLinesCollection vectorLinesCollection,
		    @NonNull SingleSkImage restImage) {
	    updateBitmap(layer, paint);

        Bitmap lostBitmap = layer.getBitmap(R.drawable.mm_ais_vessel_cross);

        if (bitmap == null || lostBitmap == null)
        {
            return;
        }

        SingleSkImage activeImage = NativeUtilities.createSkImageFromBitmap(bitmap);
        SingleSkImage lostImage = NativeUtilities.createSkImageFromBitmap(lostBitmap);

        MapMarkerBuilder markerBuilder = new MapMarkerBuilder();
        markerBuilder.setBaseOrder(baseOrder);
        markerBuilder.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(1), activeImage);
        markerBuilder.setIsHidden(true);
        activeMarker = markerBuilder.buildAndAddToCollection(markersCollection);

        markerBuilder.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(1), restImage);
        restMarker = markerBuilder.buildAndAddToCollection(markersCollection);

        markerBuilder.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(1), lostImage);
        lostMarker = markerBuilder.buildAndAddToCollection(markersCollection);

        VectorLineBuilder lineBuilder = new VectorLineBuilder();
        lineBuilder.setLineId(getMmsi());
        // To simplify algorithm draw line from the center of icon and increase order to draw it behind the icon
        lineBuilder.setBaseOrder(baseOrder + 10);
        lineBuilder.setIsHidden(true);
        lineBuilder.setFillColor(NativeUtilities.createFColorARGB(0xFF000000));
        // Create line with non empty vector, otherwise render symbol is not created, TODO: FIX IN ENGINE
        lineBuilder.setPoints(new QVectorPointI(2));
        lineBuilder.setLineWidth(6);
        directionLine = lineBuilder.buildAndAddToCollection(vectorLinesCollection);
    }

    public void updateAisRenderData(OsmandMapTileView TileView,
                                    @NonNull AisTrackerLayer mapLayer, @NonNull Paint paint) {
        // Call updateBitmap to update marker color
        updateBitmap(mapLayer, paint);

        if (activeMarker == null || restMarker == null || lostMarker == null || directionLine == null)
        {
            return;
        }

        int currentZoom = TileView != null ? TileView.getZoom() : 0;
        if (currentZoom < AisTrackerLayer.START_ZOOM) {
            activeMarker.setIsHidden(true);
            restMarker.setIsHidden(true);
            lostMarker.setIsHidden(true);
            directionLine.setIsHidden(true);
            return;
        }

        float speedFactor = getMovement();
        boolean lostTimeout = isLost(vesselLostTimeoutInMinutes) && !vesselAtRest;
        boolean drawDirectionLine = (speedFactor > 0) && (!lostTimeout) && !vesselAtRest;

        activeMarker.setIsHidden(vesselAtRest || lostTimeout);
        restMarker.setIsHidden(!vesselAtRest);
        lostMarker.setIsHidden(!lostTimeout);
        directionLine.setIsHidden(drawDirectionLine);

        float rotation = (getVesselRotation() + 180f) % 360f;
        if (!vesselAtRest && needRotation()) {
            activeMarker.setOnMapSurfaceIconDirection(SwigUtilities.getOnSurfaceIconKey(1), rotation);
            lostMarker.setOnMapSurfaceIconDirection(SwigUtilities.getOnSurfaceIconKey(1), rotation);
        }

        ColorARGB iconColor = bitmapColor == 0 ? NativeUtilities.createColorARGB(0xFFFFFFFF)
                : NativeUtilities.createColorARGB(bitmapColor);

        activeMarker.setOnSurfaceIconModulationColor(iconColor);
        restMarker.setOnSurfaceIconModulationColor(iconColor);

        LatLon location = getPosition();
        if (location != null) {
            PointI markerLocation = new PointI(
                    MapUtils.get31TileNumberX(location.getLongitude()),
                    MapUtils.get31TileNumberY(location.getLatitude())
            );

            activeMarker.setPosition(markerLocation);
            restMarker.setPosition(markerLocation);
            lostMarker.setPosition(markerLocation);

            int inverseZoom = TileView != null ? TileView.getMaxZoom() - TileView.getZoom() : 0;
            float lineLength = speedFactor * (float)MapUtils.getPowZoom(inverseZoom) * bitmap.getHeight() * 0.75f;

            double theta = Math.toRadians(rotation);
            float dx = (float) (-Math.sin(theta) * lineLength);
            float dy = (float) (Math.cos(theta) * lineLength);

            PointI directionLineEnd = new PointI(
                    (int) (markerLocation.getX() + Math.ceil(dx)),
                    (int) (markerLocation.getY() + Math.ceil(dy))
            );

            QVectorPointI points = new QVectorPointI();
            points.add(markerLocation);
            points.add(directionLineEnd);

            directionLine.setPoints(points);
            directionLine.setIsHidden(!drawDirectionLine);
        }
    }

    public void clearAisRenderData(@NonNull MapMarkersCollection markersCollection,
                                   @NonNull VectorLinesCollection vectorLinesCollection) {
        markersCollection.removeMarker(activeMarker);
        markersCollection.removeMarker(restMarker);
        markersCollection.removeMarker(lostMarker);
        vectorLinesCollection.removeLine(directionLine);
    }
}
