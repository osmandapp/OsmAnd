package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_AIRPLANE;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_ATON;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_ATON_VIRTUAL;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_INVALID;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_LANDSTATION;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_SART;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_VESSEL;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_VESSEL_AUTHORITIES;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_VESSEL_COMMERCIAL;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_VESSEL_FAST;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_VESSEL_FREIGHT;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_VESSEL_OTHER;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_VESSEL_PASSENGER;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_VESSEL_SAR;
import static net.osmand.plus.plugins.aistracker.AisObjType.AIS_VESSEL_SPORT;
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
import static net.osmand.plus.plugins.aistracker.AisTrackerHelper.getNewPosition;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.plugins.aistracker.AisTrackerHelper.Cpa;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class AisObject {

    private AisTrackerPlugin plugin;

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
    private AisObjType objectClass;
    private Cpa cpa;
    public static final float SPEED_CONSIDERED_IN_REST = 0.4f; // in knots: vessels up to this speed are considered as "in rest"

    /* timestamp of last AIS message received for the current instance: */
    private long lastUpdate = 0;

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

    public AisObject(@NonNull AisTrackerPlugin plugin, @NonNull AisObject ais) {
        this.plugin = plugin;
        this.set(ais);
    }

    @NonNull
    public AisTrackerPlugin getPlugin() {
        return plugin;
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
        this.cpa = new Cpa();
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
            case 25: // WIG, Reserved for future use
            case 26: // WIG, Reserved for future use
            case 27: // WIG, Reserved for future use
            case 28: // WIG, Reserved for future use
            case 29: // WIG, Reserved for future use
            case 40: // High Speed Craft (HSC)
            case 41: // HSC, Hazardous category A
            case 42: // HSC, Hazardous category B
            case 43: // HSC, Hazardous category C
            case 44: // HSC, Hazardous category D
            case 45: // HSC, Reserved for future use
            case 46: // HSC, Reserved for future use
            case 47: // HSC, Reserved for future use
            case 48: // HSC, Reserved for future use
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
            case 65: // Passenger, Reserved for future use
            case 66: // Passenger, Reserved for future use
            case 67: // Passenger, Reserved for future use
            case 68: // Passenger, Reserved for future use
            case 69: // Passenger, No additional information
                this.objectClass = AIS_VESSEL_PASSENGER;
                break;

            case 70: // Cargo, all ships of this type
            case 71: // Cargo, Hazardous category A
            case 72: // Cargo, Hazardous category B
            case 73: // Cargo, Hazardous category C
            case 74: // Cargo, Hazardous category D
            case 75: // Cargo, Reserved for future use
            case 76: // Cargo, Reserved for future use
            case 77: // Cargo, Reserved for future use
            case 78: // Cargo, Reserved for future use
            case 79: // Cargo, No additional information
            case 80: // Tanker, all ships of this type
            case 81: // Tanker, Hazardous category A
            case 82: // Tanker, Hazardous category B
            case 83: // Tanker, Hazardous category C
            case 84: // Tanker, Hazardous category D
            case 85: // Tanker, Reserved for future use
            case 86: // Tanker, Reserved for future use
            case 87: // Tanker, Reserved for future use
            case 88: // Tanker, Reserved for future use
            case 89: // Tanker, No additional information
                this.objectClass = AIS_VESSEL_FREIGHT;
                break;

            case 90: // Other Type, all ships of this type
            case 91: // Other Type, Hazardous category A
            case 92: // Other Type, Hazardous category B
            case 93: // Other Type, Hazardous category C
            case 94: // Other Type, Hazardous category D
            case 95: // Other Type, Reserved for future use
            case 96: // Other Type, Reserved for future use
            case 97: // Other Type, Reserved for future use
            case 98: // Other Type, Reserved for future use
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
        if (this.msgTypes == null) {
            this.msgTypes = new TreeSet<>();
        }
        this.msgTypes.add(ais_msgType);
        if (this.cpa == null) {
            cpa = new Cpa();
        }
        this.initObjectClass();
    }

    boolean isLost(int maxAgeInMin) {
        return ((System.currentTimeMillis() - this.lastUpdate) / 1000 / 60) > maxAgeInMin;
    }

    /*
    * this function checks the age of the object (check lastUpdate against its limit)
    * and returns true if the object is outdated and can be removed
    * */
    public boolean checkObjectAge() {
        return isLost(plugin.getMaxObjectAgeInMinutes());
    }

    public int getMsgType() {
        return this.ais_msgType;
    }

    public SortedSet<Integer> getMsgTypes() {
        return this.msgTypes;
    }

    public int getMmsi() {
        return this.ais_mmsi;
    }

    public int getTimestamp() {
        return this.ais_timeStamp;
    }

    public int getImo() {
        return this.ais_imo;
    }

    public int getHeading() {
        return this.ais_heading;
    }

    public int getNavStatus() {
        return this.ais_navStatus;
    }

    public int getManInd() {
        return this.ais_manInd;
    }

    public int getShipType() {
        return this.ais_shipType;
    }

    public int getDimensionToBow() {
        return this.ais_dimensionToBow;
    }

    public int getDimensionToStern() {
        return this.ais_dimensionToStern;
    }

    public int getDimensionToPort() {
        return this.ais_dimensionToPort;
    }

    public int getDimensionToStarboard() {
        return this.ais_dimensionToStarboard;
    }

    public int getEtaMon() {
        return this.ais_etaMon;
    }

    public int getEtaDay() {
        return this.ais_etaDay;
    }

    public int getEtaHour() {
        return this.ais_etaHour;
    }

    public int getEtaMin() {
        return this.ais_etaMin;
    }

    public int getAltitude() {
        return this.ais_altitude;
    }

    public int getAidType() {
        return this.ais_aidType;
    }

    public double getCog() {
        return this.ais_cog;
    }

    public double getSog() {
        return this.ais_sog;
    }

    public double getRot() {
        return this.ais_rot;
    }

    public double getDraught() {
        return this.ais_draught;
    }

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
                loc.setBearing((float) ais_cog);
            }
            if (ais_sog != INVALID_SOG) {
                loc.setSpeed((float) (ais_sog * 1852 / 3600)); // in m/s
            }
            if (ais_altitude != INVALID_ALTITUDE) {
                loc.setAltitude((float) ais_altitude);
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
    public String getCountryCode() {
        return this.countryCode;
    }

    public AisObjType getObjectClass() {
        return this.objectClass;
    }

    public long getLastUpdate() {
        return this.lastUpdate;
    }

    public Cpa getCpa() {
        return cpa;
    }

    @NonNull
    public String getShipTypeString() {
		return switch (this.ais_shipType) {
			case INVALID_SHIP_TYPE -> "unknown"; // not initialized
            case 20 -> "Wing in ground (WIG)";
			case 21 -> "WIG, Hazardous category A";
			case 22 -> "WIG, Hazardous category B";
			case 23 -> "WIG, Hazardous category C";
			case 24 -> "WIG, Hazardous category D";
            case 25 -> "Wing in ground (WIG)";
            case 26 -> "Wing in ground (WIG)";
            case 27 -> "Wing in ground (WIG)";
            case 28 -> "Wing in ground (WIG)";
            case 29 -> "Wing in ground (WIG)";
			case 30 -> "Fishing";
			case 31 -> "Towing";
			case 32 -> "Towing";
			case 33 -> "Dredging";
			case 34 -> "Diving ops";
			case 35 -> "Military ops";
			case 36 -> "Sailing";
			case 37 -> "Pleasure Craft";
			case 40 -> "High Speed Craft (HSC)";
			case 41 -> "HSC, Hazardous category A";
			case 42 -> "HSC, Hazardous category B";
			case 43 -> "HSC, Hazardous category C";
			case 44 -> "HSC, Hazardous category D";
            case 45 -> "High Speed Craft (HSC)"; // HSC, Reserved for future use
            case 46 -> "High Speed Craft (HSC)"; // HSC, Reserved for future use
            case 47 -> "High Speed Craft (HSC)"; // HSC, Reserved for future use
            case 48 -> "High Speed Craft (HSC)"; // HSC, Reserved for future use
			case 49 -> "High Speed Craft (HSC)"; // HSC, Reserved for future use
			case 50 -> "Pilot Vessel";
			case 51 -> "Search and Rescue vessel";
			case 52 -> "Tug";
			case 53 -> "Port Tender";
			case 54 -> "Anti-pollution equipment";
			case 55 -> "Law Enforcement";
			case 56 -> "Spare - Local Vessel";
			case 57 -> "Spare - Local Vessel";
			case 58 -> "Medical Transport";
			case 59 -> "Noncombatant ship according to RR Resolution No. 18";
			case 60 -> "Passenger";
			case 61 -> "Passenger, Hazardous category A";
			case 62 -> "Passenger, Hazardous category B";
			case 63 -> "Passenger, Hazardous category C";
			case 64 -> "Passenger, Hazardous category D";
            case 65 -> "Passenger/Cruise/Ferry"; // Passenger, reserved for future use
            case 66 -> "Passenger/Cruise/Ferry"; // Passenger, reserved for future use
            case 67 -> "Passenger/Cruise/Ferry"; // Passenger, reserved for future use
            case 68 -> "Passenger/Cruise/Ferry"; // Passenger, reserved for future use
			case 69 -> "Passenger/Cruise/Ferry"; // Passenger, No additional information
			case 70 -> "Cargo"; // Cargo, all ships of this type
			case 71 -> "Cargo, Hazardous category A";
			case 72 -> "Cargo, Hazardous category B";
			case 73 -> "Cargo, Hazardous category C";
			case 74 -> "Cargo, Hazardous category D";
            case 75 -> "Cargo"; // Cargo, reserved for future use
            case 76 -> "Cargo"; // Cargo, reserved for future use
            case 77 -> "Cargo"; // Cargo, reserved for future use
            case 78 -> "Cargo"; // Cargo, reserved for future use
			case 79 -> "Cargo"; // Cargo, No additional information
			case 80 -> "Tanker"; // Tanker, all ships of this type
			case 81 -> "Tanker, Hazardous category A";
			case 82 -> "Tanker, Hazardous category B";
			case 83 -> "Tanker, Hazardous category C";
			case 84 -> "Tanker, Hazardous category D";
            case 85 -> "Tanker"; // Tanker, reserved for future use
            case 86 -> "Tanker"; // Tanker, reserved for future use
            case 87 -> "Tanker"; // Tanker, reserved for future use
            case 88 -> "Tanker"; // Tanker, reserved for future use
			case 89 -> "Tanker"; // Tanker, No additional information
			case 90 -> "Other Type"; // Other Type, all ships of this type
			case 91 -> "Other Type, Hazardous category A";
			case 92 -> "Other Type, Hazardous category B";
			case 93 -> "Other Type, Hazardous category C";
			case 94 -> "Other Type, Hazardous category D";
            case 95 -> "Other Type"; // Other Type, reserved for future use
            case 96 -> "Other Type"; // Other Type, reserved for future use
            case 97 -> "Other Type"; // Other Type, reserved for future use
            case 98 -> "Other Type"; // Other Type, reserved for future use
			case 99 -> "Other Type"; // Other Type, no additional information
			default -> Integer.toString(ais_shipType);
		};
    }
    @NonNull
    public String getNavStatusString() {
		return switch (this.ais_navStatus) {
			// see https://gpsd.gitlab.io/gpsd/AIVDM.html#_types_1_2_and_3_position_report_class_a
			case 0 -> "Under way using engine";
			case 1 -> "At anchor";
			case 2 -> "Not under command";
			case 3 -> "Restricted manoeuverability";
			case 4 -> "Constrained by her draught";
			case 5 -> "Moored";
			case 6 -> "Aground";
			case 8 -> "Under way sailing";
			case 11 -> "Power-driven vessel towing astern (regional use)";
			case 12 -> "Power-driven vessel pushing ahead or towing alongside (regional use)";
			case 7 -> "Engaged in Fishing";
			case 14 -> "AIS-SART is active";
			case INVALID_NAV_STATUS -> "unknown"; // no valid value
			default -> Integer.toString(ais_navStatus);
		};
    }
    @NonNull
    public String getManIndString() {
        // see https://gpsd.gitlab.io/gpsd/AIVDM.html#_types_1_2_and_3_position_report_class_a
		return switch (this.ais_manInd) {
			case 0 -> "Not available";
			case 1 -> "No special maneuver";
			case 2 -> "Special maneuver";
			default -> Integer.toString(ais_manInd);
		};
    }
    @NonNull
    public String getAidTypeString() {
		return switch (this.ais_aidType) {
			// see https://gpsd.gitlab.io/gpsd/AIVDM.html#_type_21_aid_to_navigation_report
			case 0 -> "not specified";
			case 1 -> "Reference point";
			case 2 -> "RACON (radar transponder marking a navigation hazard)";
			case 3 -> "Fixed structure off shore";
			case 4 -> "Spare, Reserved for future use";
			case 5 -> "Light, without sectors";
			case 6 -> "Light, with sectors";
			case 7 -> "Leading Light Front";
			case 8 -> "Leading Light Rear";
			case 9 -> "Beacon, Cardinal N";
			case 10 -> "Beacon, Cardinal E";
			case 11 -> "Beacon, Cardinal S";
			case 12 -> "Beacon, Cardinal W";
			case 13 -> "Beacon, Port hand";
			case 14 -> "Beacon, Starboard hand";
			case 15 -> "Beacon, Preferred Channel port hand";
			case 16 -> "Beacon, Preferred Channel starboard hand";
			case 17 -> "Beacon, Isolated danger";
			case 18 -> "Beacon, Safe wate";
			case 19 -> "Beacon, Special mark";
			case 20 -> "Cardinal Mark N";
			case 21 -> "Cardinal Mark E";
			case 22 -> "Cardinal Mark S";
			case 23 -> "Cardinal Mark W";
			case 24 -> "Port hand Mark";
			case 25 -> "Starboard hand Mark";
			case 26 -> "Preferred Channel Port hand";
			case 27 -> "Preferred Channel Starboard hand";
			case 28 -> "Isolated danger";
			case 29 -> "Safe Water";
			case 30 -> "Special Mark";
			case 31 -> "Light Vessel / LANBY / Rigs";
			default -> Integer.toString(ais_aidType);
		};
    }

    private float getDistanceOrBearing(boolean needBearing) {
        Location aisLocation = getLocation();
        Location ownPosition = plugin.getOwnPosition();
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

    public boolean isMovable() {
        return switch (objectClass) {
            case AIS_VESSEL, AIS_VESSEL_SPORT, AIS_VESSEL_FAST, AIS_VESSEL_PASSENGER,
                 AIS_VESSEL_FREIGHT, AIS_VESSEL_COMMERCIAL, AIS_VESSEL_AUTHORITIES, AIS_VESSEL_SAR,
                 AIS_VESSEL_OTHER, AIS_AIRPLANE -> true;
            case AIS_INVALID -> (ais_sog != INVALID_SOG) && (ais_sog > 0.0d);
            default -> false;
        };
    }

    /* return true if a vessel is moored etc. and needs to be drawn as a circle */
    public boolean isVesselAtRest() {
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
                    case 1: // at anchor
                    case 5: // moored
                        /* sometimes the ais_navStatus is wrong and contradicts other data... */
                        return (ais_cog == INVALID_COG) || (ais_sog < SPEED_CONSIDERED_IN_REST);
                    default:
                        if (msgTypes.contains(18) || msgTypes.contains(24)
                                ||  msgTypes.contains(1)  || msgTypes.contains(3)) {
                            return (ais_sog < SPEED_CONSIDERED_IN_REST);
                        }
                        return false;
                }
            default:
                return false;
        }
    }

    public boolean getSignalLostState() {
        return (isLost(plugin.getVesselLostTimeoutInMinutes()) && isMovable() && !isVesselAtRest());
    }

    float getVesselRotation() {
        float rotation = 0;
        if (this.ais_cog != INVALID_COG) {
            rotation = (float) this.ais_cog;
        } else if (this.ais_heading != INVALID_HEADING) {
            rotation = this.ais_heading;
        }
        return rotation;
    }
}
