package net.osmand.shared.util

class KGeoParsedPoint {

    val NO_ZOOM: Int = -1

    private var lat = 0.0
    private var lon = 0.0
    private var zoom = NO_ZOOM
    private var label: String? = null
    private var query: String? = null
    private var geoPoint = false
    private var geoAddress = false

    constructor(lat: Double, lon: Double) {
        this.lat = lat
        this.lon = lon
        this.geoPoint = true
    }

    constructor(lat: Double, lon: Double, zoom: Int) : this(lat, lon) {
        this.zoom = zoom
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
}