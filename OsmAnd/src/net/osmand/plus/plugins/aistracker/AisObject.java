package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_AIRPLANE;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_ATON;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_ATON_VIRTUAL;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_INVALID;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_LANDSTATION;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_SART;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_VESSEL;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_VESSEL_COMMERCIAL;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_VESSEL_FAST;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_VESSEL_FREIGHT;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_VESSEL_PASSENGER;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.AisObjType.AIS_VESSEL_SPORT;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.COUNTRY_CODES;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_ALTITUDE;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_COG;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_DIMENSION;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_DRAUGHT;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_ETA;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_ETA_HOUR;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_ETA_MIN;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_HEADING;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_LAT;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_LON;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_MANEUVER_INDICATOR;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_NAV_STATUS;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_ROT;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_SHIP_TYPE;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.INVALID_SOG;
import static net.osmand.plus.plugins.aistracker.AisObjectConstants.UNSPECIFIED_AID_TYPE;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;

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
    private long lastUpdate = 0;
    /* after this time the object is outdated and can be removed: */

    private AisObjType objectClass;
    private Bitmap bitmap = null;
    private int bitmapColor;

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
        this.ais_mmsi = mmsi;
        this.ais_msgType = msgType;
        this.countryCode = getCountryCode(this.ais_mmsi);
        this.msgTypes.add(ais_msgType);
        this.lastUpdate = System.currentTimeMillis();
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
            case 35: // Military ops
            case 50: // Pilot Vessel
            case 51: // Search and Rescue vessel
            case 52: // Tug
            case 53: // Port Tender
            case 54: // Anti-pollution equipment
            case 55: // Law Enforcement
            case 56: // Spare - Local Vessel
            case 57: // Spare - Local Vessel
            case 58: // Medical Transport
            case 59: // Noncombatant ship according to RR Resolution No. 18
                this.objectClass = AIS_VESSEL_COMMERCIAL;
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
                this.objectClass = AIS_VESSEL;
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

    public void set(@NonNull AisObject ais) {
        this.ais_mmsi = ais.getMmsi();
        this.ais_msgType = ais.getMsgType();
        if (ais.getTimestamp() != 0) { this.ais_timeStamp = ais.getTimestamp(); }
        if (ais.getImo() != 0 ) { this.ais_imo = ais.getImo(); }
        if (ais.getHeading() != INVALID_HEADING ) { this.ais_heading = ais.getHeading(); }
        if (ais.getNavStatus() != INVALID_NAV_STATUS ) { this.ais_navStatus = ais.getNavStatus(); }
        if (ais.getManInd() != INVALID_MANEUVER_INDICATOR ) { this.ais_manInd = ais.getManInd(); }
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
        if (ais.getCog() != INVALID_COG) { this.ais_cog = ais.getCog(); }
        if (ais.getSog() != INVALID_SOG) { this.ais_sog = ais.getSog(); }
        if (ais.getRot() != INVALID_ROT) { this.ais_rot = ais.getRot(); }
        if (ais.getPosition() != null) { this.ais_position = ais.getPosition(); }
        if (ais.getCallSign() != null) { this.ais_callSign = ais.getCallSign(); }
        if (ais.getShipName() != null) { this.ais_shipName = ais.getShipName(); }
        if (ais.getDestination() != null ) { this.ais_destination = ais.getDestination(); }

        this.countryCode = ais.getCountryCode();

        /* this method does not produce an exact copy of the given object, here are the differences: */
        this.lastUpdate = System.currentTimeMillis();
        if (this.msgTypes == null) {
            this.msgTypes = new TreeSet<>();
        }
        this.msgTypes.add(ais_msgType);
        this.initObjectClass();
        //this.objectClass = ais.getObjectClass(); // test only... remove later
        this.bitmap = null;
        this.bitmapColor = 0;
    }

    private void setBitmap(@NonNull AisTrackerLayer mapLayer, int maxAgeInMin) {
        if (isLost(maxAgeInMin)) {
            if (isMovable()) {
                this.bitmap = mapLayer.getBitmap(R.drawable.ais_vessel_cross);
            }
        } else {
            switch (this.objectClass) {
                case AIS_VESSEL:
                case AIS_VESSEL_SPORT:
                case AIS_VESSEL_FAST:
                case AIS_VESSEL_PASSENGER:
                case AIS_VESSEL_FREIGHT:
                case AIS_VESSEL_COMMERCIAL:
                case AIS_INVALID:
                    this.bitmap = mapLayer.getBitmap(R.drawable.ais_vessel);
                    break;
                case AIS_LANDSTATION:
                    this.bitmap = mapLayer.getBitmap(R.drawable.ais_land);
                    break;
                case AIS_AIRPLANE:
                    this.bitmap = mapLayer.getBitmap(R.drawable.ais_plane);
                    break;
                case AIS_SART:
                    this.bitmap = mapLayer.getBitmap(R.drawable.ais_sar);
                    break;
                case AIS_ATON:
                    this.bitmap = mapLayer.getBitmap(R.drawable.ais_aton);
                    break;
                case AIS_ATON_VIRTUAL:
                    this.bitmap = mapLayer.getBitmap(R.drawable.ais_aton_virt);
                    break;
            }
        }
        this.setColor(maxAgeInMin);
    }

    private void setColor(int maxAgeInMin) {
        if (isLost(maxAgeInMin)) {
            if (isMovable()) {
                this.bitmapColor = 0; // black
            }
        } else {
            switch (this.objectClass) {
                case AIS_VESSEL:
                    this.bitmapColor = Color.GREEN;
                    break;
                case AIS_VESSEL_SPORT:
                    this.bitmapColor = Color.YELLOW;
                    break;
                case AIS_VESSEL_FAST:
                    this.bitmapColor = Color.BLUE;
                    break;
                case AIS_VESSEL_PASSENGER:
                    this.bitmapColor = Color.CYAN;
                    break;
                case AIS_VESSEL_FREIGHT:
                    this.bitmapColor = Color.GRAY;
                    break;
                case AIS_VESSEL_COMMERCIAL:
                    this.bitmapColor = Color.LTGRAY;
                    break;
                default:
                    this.bitmapColor = 0; // black
            }
        }
    }

    public void draw(@NonNull AisTrackerLayer mapLayer, @NonNull Paint paint,
                     @NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
                     int maxAgeInMin) {
        if ((this.bitmap == null) || isLost(maxAgeInMin)) {
            this.setBitmap(mapLayer, maxAgeInMin);
        }
        if (this.bitmapColor != 0) {
            paint.setColorFilter(new LightingColorFilter(this.bitmapColor, 0));
        } else {
            paint.setColorFilter(null);
        }
        if (this.bitmap != null) {
            canvas.save();
            canvas.rotate(tileBox.getRotate(), (float)tileBox.getCenterPixelX(), (float)tileBox.getCenterPixelY());
            float speedFactor = getMovement();
            int locationX = tileBox.getPixXFromLonNoRot(this.ais_position.getLongitude());
            int locationY = tileBox.getPixYFromLatNoRot(this.ais_position.getLatitude());
            float fx =  locationX - this.bitmap.getWidth() / 2.0f;
            float fy =  locationY - this.bitmap.getHeight() / 2.0f;
            if (this.needRotation()) {
                float rotation = 0;
                if (this.ais_cog != INVALID_COG) { rotation = (float)this.ais_cog; }
                else if (this.ais_heading != INVALID_HEADING ) { rotation = this.ais_heading; }
                canvas.rotate(rotation, locationX, locationY);
            }
            canvas.drawBitmap(this.bitmap, Math.round(fx), Math.round(fy), paint);
            if ((speedFactor > 0) && (!isLost(maxAgeInMin))) {
                float lineStartX = locationX;
                float lineLength = (float)this.bitmap.getHeight() * speedFactor;
                float lineStartY = locationY - this.bitmap.getHeight() / 4.0f;
                float lineEndY = lineStartY - lineLength;
                canvas.drawLine(lineStartX, lineStartY, lineStartX, lineEndY, paint);
            }
            canvas.restore();
        }
    }

    public boolean isMovable() {
        switch (this.objectClass) {
            case AIS_VESSEL:
            case AIS_VESSEL_SPORT:
            case AIS_VESSEL_FAST:
            case AIS_VESSEL_PASSENGER:
            case AIS_VESSEL_FREIGHT:
            case AIS_VESSEL_COMMERCIAL:
            case AIS_AIRPLANE:
                return true;
            default:
                return false;
        }
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

    private boolean isLost(int maxAgeInMin) {
        return ((System.currentTimeMillis() - this.lastUpdate) / 1000 / 60) > maxAgeInMin;
    }

    /*
    * this function checks the age of the object (check lastUpdate against its limit)
    * and returns true if the object is outdated and can be removed
    * */
    public boolean checkObjectAge(int maxAgeInMinutes) {
        return isLost(maxAgeInMinutes);
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
    @Nullable
    public String getCallSign() {
        return this.ais_callSign;
    }
    @Nullable
    public String getShipName() {
        return this.ais_shipName;
    }
    @Nullable
    public String getDestination() {
        return this.ais_destination;
    }
    @NonNull
    public String getCountryCode() { return this.countryCode; }
    public AisObjType getObjectClass() { return this.objectClass; }
    public long getLastUpdate() { return this.lastUpdate; }
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
            case 36:
                return("Sailing");
            case 37:
                return("Pleasure Craft");
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
                return("Passenger");
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
    private float getDistanceOrBearing(@Nullable Location ownLocation, boolean needBearing) {
        Location aisLocation = getLocation();
        if ((ownLocation != null) && (aisLocation != null)) {
            return needBearing ? ownLocation.bearingTo(aisLocation) : ownLocation.distanceTo(aisLocation);
        } else {
            Log.e("AisObject", "getDistanceOrBearing(): ownLocation -> " + ownLocation +
                   ", aisLocation -> " + aisLocation);
            return -500.0f; // invalid
        }
    }
    /* get bearing from own position to the position of the AIS object */
    public float getBearing(@Nullable Location ownLocation) {
        float bearing = getDistanceOrBearing(ownLocation, true);
        if ((bearing < 0.0f) && (bearing > -200.0f)) {
            while (bearing < 0.0f) {
                bearing += 360.0f;
            }
        }
        return bearing;
    }
    /* get distance from own position to the position of the AIS object in meters */
    public float getDistanceInMeters(@Nullable Location ownLocation) {
        return getDistanceOrBearing(ownLocation, false);
    }
    public float getDistanceInNauticalMiles(@Nullable Location ownLocation) {
        float dist = getDistanceInMeters(ownLocation);
        if (dist >= 0.0f) {
            dist = dist / 1852;
        }
        return dist;
    }
}
