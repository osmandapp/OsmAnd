package net.osmand.shared.util

import net.osmand.shared.util.KGeoPointParserUtil

class KGeoParsedPoint {

    companion object {
        val NO_ZOOM: Int = -1

        private fun parseLon(lonString: String?): Double {
            if (lonString != null) {
                if (lonString.startsWith("E")) {
                    return lonString.substring(1).toDouble()
                } else if (lonString.startsWith("W")) {
                    return -lonString.substring(1).toDouble()
                }
                return lonString.toDouble()
            }
            return 0.0
        }

        private fun parseLat(latString: String?): Double {
            if (latString != null) {
                if (latString.startsWith("S")) {
                    return -latString.substring(1).toDouble()
                } else if (latString.startsWith("N")) {
                    return latString.substring(1).toDouble()
                }
                return latString.toDouble()
            }
            return 0.0
        }
    }

    private var lat = 0.0
    private var lon = 0.0
    private var zoom = NO_ZOOM
    private var label: String? = null
    private var query: String? = null
    private var geoPoint = false
    private var geoAddress = false
    private var impreciseCoordinates = false

    constructor(lat: Double, lon: Double) {
        this.lat = lat
        this.lon = lon
        this.geoPoint = true
    }

    constructor(lat: Double, lon: Double, imprecise: Boolean) : this(lat, lon) {
        this.impreciseCoordinates = imprecise
    }

    constructor(lat: Double, lon: Double, label: String?) : this(lat, lon) {
        if (label != null) {
            this.label = label.replace("\\+", " ")
        }
    }

    constructor(lat: Double, lon: Double, zoom: Int) : this(lat, lon) {
        this.zoom = zoom
    }

    constructor(lat: Double, lon: Double, zoom: Int, label: String?) : this(lat, lon, label) {
        this.zoom = zoom
    }

    constructor(latString: String?, lonString: String?) : this(parseLat(latString), parseLon(lonString)) {
        this.zoom = NO_ZOOM
    }

    constructor(latString: String?, lonString: String?, zoomString: String?) : this(parseLat(latString), parseLon(lonString)) {
        this.zoom = KGeoPointParserUtil.parseZoom(zoomString)
    }

    constructor(latString: String?, lonString: String?, zoomString: String?, label: String?) : this(latString, lonString, zoomString) {
        this.label = label
    }

    /**
     * Accepts a plain {@code String}, not URL-encoded
     */
    constructor(query: String?) {
        this.query = query
    }

    fun getLatitude(): Double {
        return lat
    }

    fun getLongitude(): Double {
        return lon
    }

    fun getZoom(): Int {
        return zoom
    }

    fun getLabel(): String? {
        return label
    }

    fun getQuery(): String? {
        return query
    }

    fun isGeoPoint(): Boolean {
        return geoPoint
    }

    fun isGeoAddress(): Boolean {
        return geoAddress
    }

    fun hasImpreciseCoordinates(): Boolean {
        return impreciseCoordinates
    }
}