package net.osmand.shared.util

import kotlin.math.abs
import kotlin.math.round

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
        this.geoAddress = true
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

    private fun formatDouble(d: Double): String {
        return formatDouble(d, -1)
    }

    private fun formatDouble(d: Double, precision: Int): String {
        if (d == d.toLong().toDouble()) return d.toLong().toString()
        if (precision < 0) return d.toString()

        var factor = 1L
        repeat(precision) { factor *= 10 }

        val scaled = round(abs(d) * factor).toLong()
        val sign = if (d < 0) "-" else ""

        return if (precision == 0) {
            "$sign$scaled"
        } else {
            "$sign${scaled / factor}.${(scaled % factor).toString().padStart(precision, '0')}"
        }
    }

    fun getGeoUriString(): String? {
        return buildGeoUri(formatDouble(lat), formatDouble(lon))
    }

    fun getGeoUriString(precision: Int): String? {
        return buildGeoUri(formatDouble(lat, precision), formatDouble(lon, precision))
    }

    /**
     * Generates a URI string according to https://tools.ietf.org/html/rfc5870 and
     * https://developer.android.com/guide/components/intents-common.html#Maps
     */
    private fun buildGeoUri(latStr: String, lonStr: String): String? {
        var uriString: String

        if (isGeoPoint()) {
            val latlon = "$latStr,$lonStr"
            uriString = "geo:$latlon"
            val map = linkedMapOf<String, String>()

            if (zoom != NO_ZOOM) {
                map["z"] = zoom.toString()
            }
            if (query != null) {
                map["q"] = UrlEncoder.encode(query!!)
            }
            if (label != null && query == null) {
                map["q"] = "$latlon(${UrlEncoder.encode(label!!)})"
            }

            if (map.isNotEmpty()) {
                uriString += "?"
            }

            var i = 0
            for ((key, value) in map) {
                if (i > 0) {
                    uriString += "&"
                }
                uriString += "$key=$value"
                i++
            }
            return uriString
        }

        if (isGeoAddress()) {
            uriString = "geo:0,0"
            if (query != null) {
                uriString += "?"
                if (zoom != NO_ZOOM) {
                    uriString += "z=$zoom&"
                }
                uriString += "q=${UrlEncoder.encode(query!!)}"
            }
            return uriString
        }

        return null
    }
}