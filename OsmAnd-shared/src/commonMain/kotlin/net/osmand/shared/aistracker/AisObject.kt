package net.osmand.shared.aistracker

import net.osmand.shared.aistracker.AisObjectConstants.COUNTRY_CODES
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_ALTITUDE
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_COG
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_DIMENSION
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_DRAUGHT
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_ETA
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_ETA_HOUR
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_ETA_MIN
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_HEADING
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_LAT
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_LON
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_MANEUVER_INDICATOR
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_NAV_STATUS
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_ROT
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_SHIP_TYPE
import net.osmand.shared.aistracker.AisObjectConstants.INVALID_SOG
import net.osmand.shared.aistracker.AisObjectConstants.UNSPECIFIED_AID_TYPE

class AisObject {
    var msgType: Int = 0
        private set
    var mmsi: Int = 0
        private set
    var timeStamp: Int = 0
        private set
    var imo: Int = 0
        private set
    var heading: Int = INVALID_HEADING
        private set
    var navStatus: Int = INVALID_NAV_STATUS
        private set
    var manInd: Int = INVALID_MANEUVER_INDICATOR
        private set
    var shipType: Int = INVALID_SHIP_TYPE
        private set
    var dimensionToBow: Int = INVALID_DIMENSION
        private set
    var dimensionToStern: Int = INVALID_DIMENSION
        private set
    var dimensionToPort: Int = INVALID_DIMENSION
        private set
    var dimensionToStarboard: Int = INVALID_DIMENSION
        private set
    var etaMon: Int = INVALID_ETA
        private set
    var etaDay: Int = INVALID_ETA
        private set
    var etaHour: Int = INVALID_ETA_HOUR
        private set
    var etaMin: Int = INVALID_ETA_MIN
        private set
    var altitude: Int = INVALID_ALTITUDE
        private set
    var aidType: Int = UNSPECIFIED_AID_TYPE
        private set
    var draught: Double = INVALID_DRAUGHT
        private set
    var cog: Double = INVALID_COG
        private set
    var sog: Double = INVALID_SOG
        private set
    var rot: Double = INVALID_ROT
        private set
    var position: AisLatLon? = null
        private set
    var callSign: String? = null
        private set
    var shipName: String? = null
        private set
    var destination: String? = null
        private set
    var countryCode: String = ""
        private set
    var msgTypes: MutableSet<Int> = mutableSetOf()
        private set
    var objectClass: AisObjType = AisObjType.AIS_INVALID
        private set
    var cpa: AisCpa = AisCpa()
        private set
    var lastUpdate: Long = 0
        private set

    constructor(mmsi: Int, msgType: Int, lat: Double, lon: Double) {
        initObj(mmsi, msgType)
        initLatLon(lat, lon)
        initObjectClass()
    }

    constructor(mmsi: Int, msgType: Int, timeStamp: Int, navStatus: Int, manInd: Int, heading: Int,
                cog: Double, sog: Double, lat: Double, lon: Double, rot: Double) {
        initObj(mmsi, msgType)
        initLatLon(lat, lon)
        this.timeStamp = timeStamp
        this.navStatus = navStatus
        this.manInd = manInd
        this.heading = heading
        this.cog = cog
        this.sog = sog
        this.rot = rot
        initObjectClass()
    }

    constructor(mmsi: Int, msgType: Int, timeStamp: Int, altitude: Int,
                cog: Double, sog: Double, lat: Double, lon: Double) {
        initObj(mmsi, msgType)
        initLatLon(lat, lon)
        this.timeStamp = timeStamp
        this.altitude = altitude
        this.cog = cog
        this.sog = sog
        initObjectClass()
    }

    constructor(mmsi: Int, msgType: Int, timeStamp: Int, heading: Int,
                cog: Double, sog: Double, lat: Double, lon: Double,
                shipType: Int, dimensionToBow: Int, dimensionToStern: Int,
                dimensionToPort: Int, dimensionToStarboard: Int) {
        initObj(mmsi, msgType)
        initLatLon(lat, lon)
        initDimensions(dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard)
        this.timeStamp = timeStamp
        this.heading = heading
        this.cog = cog
        this.sog = sog
        this.shipType = shipType
        initObjectClass()
    }

    constructor(mmsi: Int, msgType: Int, imo: Int, callSign: String?, shipName: String?,
                shipType: Int, dimensionToBow: Int, dimensionToStern: Int,
                dimensionToPort: Int, dimensionToStarboard: Int,
                draught: Double, destination: String?, etaMon: Int,
                etaDay: Int, etaHour: Int, etaMin: Int) {
        initObj(mmsi, msgType)
        initDimensions(dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard)
        this.shipType = shipType
        this.draught = draught
        this.callSign = callSign
        this.shipName = shipName
        if (destination != null) {
            if (!destination.matches(Regex("^@+$"))) { // string consisting of only "@" characters is invalid
                this.destination = destination
            }
        }
        this.etaMon = etaMon
        this.etaDay = etaDay
        this.etaHour = etaHour
        this.etaMin = etaMin
        this.imo = imo
        initObjectClass()
    }

    constructor(mmsi: Int, msgType: Int, lat: Double, lon: Double, aidType: Int,
                dimensionToBow: Int, dimensionToStern: Int,
                dimensionToPort: Int, dimensionToStarboard: Int) {
        initObj(mmsi, msgType)
        initLatLon(lat, lon)
        initDimensions(dimensionToBow, dimensionToStern, dimensionToPort, dimensionToStarboard)
        this.aidType = aidType
        initObjectClass()
    }

    constructor(ais: AisObject) {
        this.set(ais)
    }

    private fun getCountryCode(mmsi: Int): String {
        val mmsiString = mmsi.toString()
        if (mmsiString.length > 2) {
            val ccStr = mmsiString.substring(0, 3)
            val ccName = COUNTRY_CODES[ccStr.toIntOrNull() ?: 0]
            if (ccName != null) {
                return ccName
            }
        }
        return ""
    }

    private fun initObj(mmsi: Int, msgType: Int) {
        this.mmsi = mmsi
        this.msgType = msgType
        this.countryCode = getCountryCode(this.mmsi)
        this.msgTypes.add(msgType)
        this.lastUpdate = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }

    private fun initLatLon(lat: Double, lon: Double) {
        if (lat != INVALID_LAT && lon != INVALID_LON) {
            position = AisLatLon(lat, lon)
        }
    }

    private fun initDimensions(dimensionToBow: Int, dimensionToStern: Int,
                               dimensionToPort: Int, dimensionToStarboard: Int) {
        this.dimensionToBow = dimensionToBow
        this.dimensionToStern = dimensionToStern
        this.dimensionToPort = dimensionToPort
        this.dimensionToStarboard = dimensionToStarboard
    }

    private fun initObjectClass() {
        when (this.shipType) {
            INVALID_SHIP_TYPE -> {}
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49 -> this.objectClass = AisObjType.AIS_VESSEL_FAST
            30, 31, 32, 33, 34, 50, 52, 53, 54, 56, 57, 59 -> this.objectClass = AisObjType.AIS_VESSEL_COMMERCIAL
            35, 55 -> this.objectClass = AisObjType.AIS_VESSEL_AUTHORITIES
            51, 58 -> this.objectClass = AisObjType.AIS_VESSEL_SAR
            36, 37 -> this.objectClass = AisObjType.AIS_VESSEL_SPORT
            60, 61, 62, 63, 64, 65, 66, 67, 68, 69 -> this.objectClass = AisObjType.AIS_VESSEL_PASSENGER
            70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89 -> this.objectClass = AisObjType.AIS_VESSEL_FREIGHT
            90, 91, 92, 93, 94, 95, 96, 97, 98, 99 -> this.objectClass = AisObjType.AIS_VESSEL_OTHER
            else -> this.objectClass = AisObjType.AIS_VESSEL_OTHER
        }

        if (shipType == INVALID_SHIP_TYPE) {
            if (msgTypes.contains(9)) {
                this.objectClass = AisObjType.AIS_AIRPLANE
            } else if (msgTypes.contains(4)) {
                this.objectClass = AisObjType.AIS_LANDSTATION
            } else if (msgTypes.contains(21)) {
                when (aidType) {
                    29, 30 -> this.objectClass = AisObjType.AIS_ATON_VIRTUAL
                    else -> this.objectClass = AisObjType.AIS_ATON
                }
            } else if (msgTypes.contains(18)) {
                this.objectClass = AisObjType.AIS_VESSEL
            } else {
                when (navStatus) {
                    0, 1, 2, 3, 4, 5, 6, 8, 11, 12 -> this.objectClass = AisObjType.AIS_VESSEL
                    7 -> this.objectClass = AisObjType.AIS_VESSEL_COMMERCIAL
                    14 -> this.objectClass = AisObjType.AIS_SART
                    INVALID_NAV_STATUS -> this.objectClass = AisObjType.AIS_INVALID
                    else -> this.objectClass = AisObjType.AIS_INVALID
                }
            }
        }
    }

    fun set(ais: AisObject) {
        this.mmsi = ais.mmsi
        this.msgType = ais.msgType
        if (ais.timeStamp != 0) { this.timeStamp = ais.timeStamp }
        if (ais.imo != 0) { this.imo = ais.imo }
        if (ais.shipType != INVALID_SHIP_TYPE) { this.shipType = ais.shipType }
        if (ais.dimensionToBow != INVALID_DIMENSION) { this.dimensionToBow = ais.dimensionToBow }
        if (ais.dimensionToStern != INVALID_DIMENSION) { this.dimensionToStern = ais.dimensionToStern }
        if (ais.dimensionToPort != INVALID_DIMENSION) { this.dimensionToPort = ais.dimensionToPort }
        if (ais.dimensionToStarboard != INVALID_DIMENSION) { this.dimensionToStarboard = ais.dimensionToStarboard }
        if (ais.etaMon != INVALID_ETA) { this.etaMon = ais.etaMon }
        if (ais.etaDay != INVALID_ETA) { this.etaDay = ais.etaDay }
        if (ais.etaHour != INVALID_ETA_HOUR) { this.etaHour = ais.etaHour }
        if (ais.etaMin != INVALID_ETA_MIN) { this.etaMin = ais.etaMin }
        if (ais.altitude != INVALID_ALTITUDE) { this.altitude = ais.altitude }
        if (ais.aidType != UNSPECIFIED_AID_TYPE) { this.aidType = ais.aidType }
        if (ais.draught != INVALID_DRAUGHT) { this.draught = ais.draught }
        if (ais.position != null) { this.position = ais.position }
        if (ais.callSign != null) { this.callSign = ais.callSign }
        if (ais.shipName != null) { this.shipName = ais.shipName }
        if (ais.destination != null) { this.destination = ais.destination }

        val msgListHeading = listOf(1, 2, 3, 18, 19, 27)
        val msgListStatus = listOf(1, 2, 3, 27)
        val msgListCourse = listOf(1, 2, 3, 9, 18, 19, 27)

        if (msgListHeading.contains(msgType)) {
            this.heading = ais.heading
        }
        if (msgListStatus.contains(msgType)) {
            this.navStatus = ais.navStatus
            this.manInd = ais.manInd
            this.rot = ais.rot
        }
        if (msgListCourse.contains(msgType)) {
            this.cog = ais.cog
            this.sog = ais.sog
        }

        this.countryCode = ais.countryCode
        this.lastUpdate = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        this.msgTypes.add(msgType)
        this.initObjectClass()
    }

    fun isLost(maxAgeInMin: Int): Boolean {
        return ((kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - this.lastUpdate) / 1000 / 60) > maxAgeInMin
    }

    fun getShipTypeString(): String {
        return when (this.shipType) {
            INVALID_SHIP_TYPE -> "unknown"
            20, 25, 26, 27, 28, 29 -> "Wing in ground (WIG)"
            21 -> "WIG, Hazardous category A"
            22 -> "WIG, Hazardous category B"
            23 -> "WIG, Hazardous category C"
            24 -> "WIG, Hazardous category D"
            30 -> "Fishing"
            31 -> "Towing"
            32 -> "Towing"
            33 -> "Dredging"
            34 -> "Diving ops"
            35 -> "Military ops"
            36 -> "Sailing"
            37 -> "Pleasure Craft"
            40, 45, 46, 47, 48, 49 -> "High Speed Craft (HSC)"
            41 -> "HSC, Hazardous category A"
            42 -> "HSC, Hazardous category B"
            43 -> "HSC, Hazardous category C"
            44 -> "HSC, Hazardous category D"
            50 -> "Pilot Vessel"
            51 -> "Search and Rescue vessel"
            52 -> "Tug"
            53 -> "Port Tender"
            54 -> "Anti-pollution equipment"
            55 -> "Law Enforcement"
            56 -> "Spare - Local Vessel"
            57 -> "Spare - Local Vessel"
            58 -> "Medical Transport"
            59 -> "Noncombatant ship according to RR Resolution No. 18"
            60 -> "Passenger"
            61 -> "Passenger, Hazardous category A"
            62 -> "Passenger, Hazardous category B"
            63 -> "Passenger, Hazardous category C"
            64 -> "Passenger, Hazardous category D"
            65, 66, 67, 68, 69 -> "Passenger/Cruise/Ferry"
            70, 75, 76, 77, 78, 79 -> "Cargo"
            71 -> "Cargo, Hazardous category A"
            72 -> "Cargo, Hazardous category B"
            73 -> "Cargo, Hazardous category C"
            74 -> "Cargo, Hazardous category D"
            80, 85, 86, 87, 88, 89 -> "Tanker"
            81 -> "Tanker, Hazardous category A"
            82 -> "Tanker, Hazardous category B"
            83 -> "Tanker, Hazardous category C"
            84 -> "Tanker, Hazardous category D"
            90, 95, 96, 97, 98, 99 -> "Other Type"
            91 -> "Other Type, Hazardous category A"
            92 -> "Other Type, Hazardous category B"
            93 -> "Other Type, Hazardous category C"
            94 -> "Other Type, Hazardous category D"
            else -> this.shipType.toString()
        }
    }

    fun getNavStatusString(): String {
        return when (this.navStatus) {
            0 -> "Under way using engine"
            1 -> "At anchor"
            2 -> "Not under command"
            3 -> "Restricted manoeuverability"
            4 -> "Constrained by her draught"
            5 -> "Moored"
            6 -> "Aground"
            8 -> "Under way sailing"
            11 -> "Power-driven vessel towing astern (regional use)"
            12 -> "Power-driven vessel pushing ahead or towing alongside (regional use)"
            7 -> "Engaged in Fishing"
            14 -> "AIS-SART is active"
            INVALID_NAV_STATUS -> "unknown"
            else -> navStatus.toString()
        }
    }

    fun getManIndString(): String {
        return when (this.manInd) {
            0 -> "Not available"
            1 -> "No special maneuver"
            2 -> "Special maneuver"
            else -> manInd.toString()
        }
    }

    fun getAidTypeString(): String {
        return when (this.aidType) {
            0 -> "not specified"
            1 -> "Reference point"
            2 -> "RACON (radar transponder marking a navigation hazard)"
            3 -> "Fixed structure off shore"
            4 -> "Spare, Reserved for future use"
            5 -> "Light, without sectors"
            6 -> "Light, with sectors"
            7 -> "Leading Light Front"
            8 -> "Leading Light Rear"
            9 -> "Beacon, Cardinal N"
            10 -> "Beacon, Cardinal E"
            11 -> "Beacon, Cardinal S"
            12 -> "Beacon, Cardinal W"
            13 -> "Beacon, Port hand"
            14 -> "Beacon, Starboard hand"
            15 -> "Beacon, Preferred Channel port hand"
            16 -> "Beacon, Preferred Channel starboard hand"
            17 -> "Beacon, Isolated danger"
            18 -> "Beacon, Safe wate"
            19 -> "Beacon, Special mark"
            20 -> "Cardinal Mark N"
            21 -> "Cardinal Mark E"
            22 -> "Cardinal Mark S"
            23 -> "Cardinal Mark W"
            24 -> "Port hand Mark"
            25 -> "Starboard hand Mark"
            26 -> "Preferred Channel Port hand"
            27 -> "Preferred Channel Starboard hand"
            28 -> "Isolated danger"
            29 -> "Safe Water"
            30 -> "Special Mark"
            31 -> "Light Vessel / LANBY / Rigs"
            else -> aidType.toString()
        }
    }

    fun isMovable(): Boolean {
        return when (objectClass) {
            AisObjType.AIS_VESSEL, AisObjType.AIS_VESSEL_SPORT, AisObjType.AIS_VESSEL_FAST, 
            AisObjType.AIS_VESSEL_PASSENGER, AisObjType.AIS_VESSEL_FREIGHT, AisObjType.AIS_VESSEL_COMMERCIAL, 
            AisObjType.AIS_VESSEL_AUTHORITIES, AisObjType.AIS_VESSEL_SAR, AisObjType.AIS_VESSEL_OTHER, 
            AisObjType.AIS_AIRPLANE -> true
            AisObjType.AIS_INVALID -> (sog != INVALID_SOG) && (sog > 0.0)
            else -> false
        }
    }

    fun isVesselAtRest(): Boolean {
        return when (objectClass) {
            AisObjType.AIS_VESSEL, AisObjType.AIS_VESSEL_SPORT, AisObjType.AIS_VESSEL_FAST,
            AisObjType.AIS_VESSEL_PASSENGER, AisObjType.AIS_VESSEL_FREIGHT, AisObjType.AIS_VESSEL_COMMERCIAL,
            AisObjType.AIS_VESSEL_AUTHORITIES, AisObjType.AIS_VESSEL_SAR, AisObjType.AIS_VESSEL_OTHER -> {
                when (navStatus) {
                    1, 5 -> (cog == INVALID_COG) || (sog < AisObjectConstants.SPEED_CONSIDERED_IN_REST)
                    else -> {
                        if (msgTypes.contains(18) || msgTypes.contains(24) || msgTypes.contains(1) || msgTypes.contains(3)) {
                            (sog < AisObjectConstants.SPEED_CONSIDERED_IN_REST)
                        } else {
                            false
                        }
                    }
                }
            }
            else -> false
        }
    }

    fun getVesselRotation(): Float {
        var rotation = 0f
        if (this.cog != INVALID_COG) {
            rotation = this.cog.toFloat()
        } else if (this.heading != INVALID_HEADING) {
            rotation = this.heading.toFloat()
        }
        return rotation
    }

    fun getAisLocation(): AisLocation? {
        val pos = position ?: return null
        return AisLocation(
            latitude = pos.latitude,
            longitude = pos.longitude,
            speed = if (sog != INVALID_SOG) (sog * 1852 / 3600).toFloat() else Float.NaN,
            bearing = if (cog != INVALID_COG) cog.toFloat() else Float.NaN,
            hasSpeed = sog != INVALID_SOG,
            hasBearing = cog != INVALID_COG
        )
    }

    fun getExtrapolatedLocation(now: Long): AisLocation? {
        val loc = getAisLocation() ?: return null
        val ageInHours = (now - lastUpdate) / 1000.0 / 3600.0
        val newPos = AisTrackerMath.getNewPosition(loc, ageInHours)
        if (newPos != null) {
            loc.latitude = newPos.latitude
            loc.longitude = newPos.longitude
        }
        return loc
    }
}
