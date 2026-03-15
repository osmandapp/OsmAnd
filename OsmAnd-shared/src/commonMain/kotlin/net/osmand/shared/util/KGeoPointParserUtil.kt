package net.osmand.shared.util

import net.osmand.shared.data.KLatLon

object KGeoPointParserUtil {

    private fun getQueryParameter(param: String, uri: KUri): String? {
        val query = uri.query
        var value: String? = null
        if (query != null && query.contains(param)) {
            val params = query.split("&")
            for (p in params) {
                if (p.contains(param)) {
                    value = p.substring(p.indexOf("=") + 1, p.length)
                    break
                }
            }
        }
        return value
    }

    /**
     * This parses out all of the parameters in the query string for both
     * http: and geo: URIs.  This will only work on URIs with valid syntax, so
     * it will not work on URIs that do odd things like have a query string in
     * the fragment, like this one:
     * http://www.amap.com/#!poi!!q=38.174596,114.995033|2|%E5%AE%BE%E9%A6%86&radius=1000
     *
     * @param uri
     * @return {@link Map<String, String>} a Map of the query parameters
     */

    internal fun getQueryParameters(uri: KUri): Map<String, String> {
        var query: String? = null
        if (uri.isOpaque && uri.schemeSpecificPart != null) {
            val schemeSpecificPart = uri.schemeSpecificPart
            val pos = schemeSpecificPart.indexOf("?")
            if (pos == schemeSpecificPart.length) {
                query = ""
            } else if (pos > 1) {
                query = schemeSpecificPart.substring(pos + 1)
            }
        } else {
            query = uri.rawQuery
        }
        return getQueryParameters(query)
    }

    private fun getQueryParameters(query: String?): Map<String, String> {
        val map = linkedMapOf<String, String>()
        if (query != null && query.isNotEmpty()) {
            val params = query.split(Regex("[&/]"))
            for (p in params) {
                val keyValue = p.split("=")
                if (keyValue.size == 1) {
                    map[keyValue[0]] = ""
                } else if (keyValue.size > 1) {
                    map[keyValue[0]] = KUri.urlDecode(keyValue[1])
                }
            }
        }
        return map
    }

	private var kMaxPointBytes = 10
	private var kMaxCoordBits = kMaxPointBytes * 3

    fun decodeMapsMeLatLonToInt(s: String): KLatLon? {
        // 44TvlEGXf-
        var lat = 0
        var lon = 0
        var shift = kMaxCoordBits - 3
        for (i in s.indices) {
            val a = KAlgorithms.base64IndexOf(s[i])
            if (a < 0) {
                return null
            }

            val lat1 = (((a shr 5) and 1) shl 2) or (((a shr 3) and 1) shl 1) or ((a shr 1) and 1)
            val lon1 = (((a shr 4) and 1) shl 2) or (((a shr 2) and 1) shl 1) or (a and 1)
            lat = lat or (lat1 shl shift)
            lon = lon or (lon1 shl shift)
            shift -= 3
        }

        val middleOfSquare = (1 shl (3 * (kMaxPointBytes - s.length) - 1)).toDouble()
        lat += middleOfSquare.toInt()
        lon += middleOfSquare.toInt()

        val dlat = lat.toDouble() / ((1 shl kMaxCoordBits) - 1) * 180 - 90
        val dlon = lon.toDouble() / ((1 shl kMaxCoordBits) - 1 + 1) * 360.0 - 180
        return KLatLon(dlat, dlon)
    }


    /**
     * Parses geo and map intents:
     *
     * @param uriString The URI as a String
     * @return {@link GeoParsedPoint}
     */
	fun parse(uriString: String): KGeoParsedPoint? {
		val points = parsePoints(uriString)
		if (points != null && !points.isEmpty()) {
			return points[0]
		}
		return null
	}

	fun parsePoints(uriString: String): List<KGeoParsedPoint>? {
		val uri = createUri(uriString)
		var scheme = uri?.scheme
		if (scheme != null) {
			scheme = scheme.lowercase()

			if ("http" == scheme || "https" == scheme || "google.navigation" == scheme) {
				return parseLinkUri(uri, scheme)
			} else if ("geo" == scheme || "osmand.geo" == scheme) {
				return parseGeoUri(uri)
			}
		}
		return null
	}

    fun createUri(uriString: String): KUri? {
        try {
            // amap.com uses | in their URLs, which is an illegal character for a URL
            val normalized = uriString.trim()
                .replace(Regex("\\s+"), "+")
                .replace("%20", "+")
                .replace("%2C", ",")
                .replace("|", ";")
                .replace(Regex("\\(\\(\\S+\\)\\)"), "")

            return KUri.create(normalized)
        } catch (_: Throwable) {
        }
        return null
    }

    private fun parseLinkUri(uri: KUri, scheme: String): List<KGeoParsedPoint>? {
        var host = uri.host
        var params = getQueryParameters(uri)
        if (scheme == "google.navigation") {
            host = scheme
            if (uri.schemeSpecificPart == null) {
                return null
            } else if (!uri.schemeSpecificPart.contains("=")) {
                params = getQueryParameters("q=" + uri.schemeSpecificPart)
            } else {
                params = getQueryParameters(uri.schemeSpecificPart)
            }
        } else if (host == null) {
            return null
        } else {
            host = host.lowercase()
        }

        var path = uri.path
        if (path == null) {
            path = ""
        }
        val fragment = uri.fragment

        // lat-double, lon - double, zoom or z - int
        val simpleDomains = linkedSetOf(
            "osmand.net",
            "www.osmand.net",
            "test.osmand.net",
            "download.osmand.net",
            "openstreetmap.de",
            "www.openstreetmap.de"
        )

        val commaSeparatedPairRegex =
            Regex("(?:loc:)?([N|S]?[+-]?\\d+(?:\\.\\d+)?),([E|W]?[+-]?\\d+(?:\\.\\d+)?)")

        try {
            if (host == "osm.org" || host.endsWith("openstreetmap.org")) {
                return parseOsmUri(uri, path, fragment)
            } else if (host.startsWith("map.baidu.")) { // .com and .cn both work
                return parseBaiduUri(params)
            } else if (host == "ge0.me") {
                return parseGe0meUri(path)
            } else if (simpleDomains.contains(host)) {
                return parseSimpleDomainsUri(uri, path, params, fragment)
            } else if (host.matches(Regex("(?:www\\.)?(?:maps\\.)?yandex\\.[a-z]+"))) {
                return parseYandexUri(params, commaSeparatedPairRegex)
            } else if (host.matches(Regex("(?:www\\.)?(?:maps\\.)?google\\.[a-z.]+"))) {
                return parseGoogleUri(uri, path, params, fragment, commaSeparatedPairRegex)
            } else if (host.endsWith(".amap.com")) {
                return parseAmapUri(uri, scheme, host)
            } else if (host == "here.com" || host.endsWith(".here.com")) { // www.here.com, share.here.com, here.com
                return parseHereUri(path, params)
            } else if (host.endsWith(".qq.com")) {
                return parseQqUri(params, commaSeparatedPairRegex)
            } else if (host == "maps.apple.com") {
                return parseAppleUri(params, commaSeparatedPairRegex)
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseOsmUri(uri: KUri, path: String, fragment: String?): List<KGeoParsedPoint>? {
        if (path.startsWith("/go/")) {  // short URL form
            val match = Regex("^/go/([A-Za-z0-9_@~]+-*)(?:.*)").matchEntire(path)
            if (match != null) {
                return listOf(KMapUtils.decodeShortLinkString(match.groupValues[1]))
            }
        } else { // data in the query and/or feature strings
            var lat = 0.0
            var lon = 0.0
            var zoom = KGeoParsedPoint.NO_ZOOM
            val queryMap = getQueryParameters(uri)

            if (queryMap.containsKey("route")) {
                val routeValue = queryMap["route"]
                val coordinatesMatch = Regex("^(\\d+[.]?\\d*),(\\d+[.]?\\d*);(\\d+[.]?\\d*),(\\d+[.]?\\d*)")
                    .matchEntire(routeValue ?: "")
                if (coordinatesMatch != null) {
                    val pointFrom = KGeoParsedPoint(
                        parseSilentDouble(coordinatesMatch.groupValues[1]),
                        parseSilentDouble(coordinatesMatch.groupValues[2])
                    )
                    val pointTo = KGeoParsedPoint(
                        parseSilentDouble(coordinatesMatch.groupValues[3]),
                        parseSilentDouble(coordinatesMatch.groupValues[4])
                    )
                    return listOf(pointFrom, pointTo)
                }
            } else if (queryMap.containsKey("from") || queryMap.containsKey("to")) {
                var pointFrom: KGeoParsedPoint? = null
                val from = queryMap["from"]
                if (from != null && !from.isEmpty()) {
                    val coordinates = from.split(",")
                    lat = parseSilentDouble(coordinates[0])
                    lon = parseSilentDouble(coordinates[1])
                    pointFrom = KGeoParsedPoint(lat, lon)
                }

                var pointTo: KGeoParsedPoint? = null
                val to = queryMap["to"]
                if (to != null && !to.isEmpty()) {
                    val coordinates = to.split(",")
                    lat = parseSilentDouble(coordinates[0])
                    lon = parseSilentDouble(coordinates[1])
                    pointTo = KGeoParsedPoint(lat, lon)
                }

                val parsedPoints = mutableListOf<KGeoParsedPoint>()
                if (pointFrom != null) {
                    parsedPoints.add(pointFrom)
                }
                if (pointTo != null) {
                    parsedPoints.add(pointTo)
                }
                return parsedPoints
            }

            if (fragment != null) {
                var mutableFragment = fragment
                if (mutableFragment.startsWith("map=")) {
                    mutableFragment = mutableFragment.substring("map=".length)
                }
                val vls = mutableFragment.split(Regex("/|&")) //"&" to split off trailing extra parameters
                if (vls.size >= 3) {
                    zoom = parseZoom(vls[0])
                    lat = parseSilentDouble(vls[1])
                    lon = parseSilentDouble(vls[2])
                }
            } else if (queryMap.isNotEmpty()) {
                var queryStr = queryMap["query"]
                if (queryStr != null) {
                    queryStr = queryStr.replace("+", " ").replace(",", " ")
                    val vls = queryStr.split(" ")
                    if (vls.size == 2) {
                        lat = parseSilentDouble(vls[0])
                        lon = parseSilentDouble(vls[1])
                    }
                    if (lat == 0.0 || lon == 0.0) {
                        return listOf(KGeoParsedPoint(queryStr))
                    }
                }
            }

            // the query string sometimes has higher resolution values
            val mlat = getQueryParameter("mlat", uri)
            if (mlat != null) {
                lat = parseSilentDouble(mlat)
            }
            val mlon = getQueryParameter("mlon", uri)
            if (mlon != null) {
                lon = parseSilentDouble(mlon)
            }

            return listOf(KGeoParsedPoint(lat, lon, zoom))
        }
        return null
    }

    private fun parseBaiduUri(params: Map<String, String>): List<KGeoParsedPoint>? {
        /* Baidu Map uses a custom format for lat/lon., it is basically standard lat/lon
		 * multiplied by 100,000, then rounded to an integer */
        val zm = params["l"]
        val vls = silentSplit(params["c"], ",")
        if (vls != null && vls.size >= 2) {
            val lat = parseSilentInt(vls[0]) / 100000.0
            val lon = parseSilentInt(vls[1]) / 100000.0
            val zoom = parseZoom(zm)
            return listOf(KGeoParsedPoint(lat, lon, zoom))
        }
        return null
    }

    private fun parseGe0meUri(path0: String): List<KGeoParsedPoint>? {
        // http:///44TvlEGXf-/Kyiv
        var path = path0
        if (path.startsWith("/")) {
            path = path.substring(1)
        }
        val pms = path.split("/")
        var label = ""
        if (pms.size > 1) {
            label = pms[1]
        }
        val qry = pms[0]
        if (qry.length < 10) {
            return null
        }
        val indZoom = KAlgorithms.base64IndexOf(qry[0])
        var zoom = 15
        if (indZoom >= 0) {
            zoom = indZoom / 4 + 4
        }
        val l = decodeMapsMeLatLonToInt(qry.substring(1).replace('-', '/')) ?: return null
        return listOf(KGeoParsedPoint(l.latitude, l.longitude, zoom, label))
    }

    private fun parseSimpleDomainsUri(
        uri: KUri,
        path: String,
        params: Map<String, String>,
        fragment: String?
    ): List<KGeoParsedPoint>? {
        var mutableParams = params
        if (uri.query == null && mutableParams.isEmpty()) {
            // DOUBLE check this may be wrong test of openstreetmap.de (looks very weird url and server doesn't respond)
            mutableParams = getQueryParameters(path.substring(1))
        }
        if (mutableParams.containsKey("lat") && mutableParams.containsKey("lon")) {
            val lat = parseSilentDouble(mutableParams["lat"])
            val lon = parseSilentDouble(mutableParams["lon"])
            var zoom = KGeoParsedPoint.NO_ZOOM
            if (mutableParams.containsKey("z")) {
                zoom = parseZoom(mutableParams["z"])
            } else if (mutableParams.containsKey("zoom")) {
                zoom = parseZoom(mutableParams["zoom"])
            }
            return listOf(KGeoParsedPoint(lat, lon, zoom))
        } else if (mutableParams.containsKey("pin")) {
            val coordinates = mutableParams["pin"]!!.split(",")
            val lat = parseSilentDouble(coordinates[0])
            val lon = parseSilentDouble(coordinates[1])
            var zoom = KGeoParsedPoint.NO_ZOOM
            if (fragment != null && !fragment.isEmpty()) {
                zoom = parseZoom(fragment.split("/")[0])
            }
            return listOf(KGeoParsedPoint(lat, lon, zoom))
        }
        return null
    }

    private fun parseYandexUri(
        params: Map<String, String>,
        commaSeparatedPairRegex: Regex
    ): List<KGeoParsedPoint>? {
        val ll = params["ll"]
        if (ll != null) {
            val match = commaSeparatedPairRegex.matchEntire(ll)
            if (match != null) {
                val z = parseZoom(params["z"]).toString()
                return listOf(
                    KGeoParsedPoint(
                        match.groupValues[1],
                        match.groupValues[2],
                        z,
                        params["text"]
                    )
                )
            }
        }
        return null
    }

    private fun parseGoogleUri(
        uri: KUri,
        path: String,
        params: Map<String, String>,
        fragment: String?,
        commaSeparatedPairRegex: Regex
    ): List<KGeoParsedPoint>? {
        var mutablePath = path
        var latString: String? = null
        var lonString: String? = null
        var ftidCellId: String? = null
        var z = KGeoParsedPoint.NO_ZOOM.toString()

        if (params.containsKey("q")) {
            val match = commaSeparatedPairRegex.matchEntire(params["q"] ?: "")
            if (match != null) {
                latString = match.groupValues[1]
                lonString = match.groupValues[2]
            }
        } else if (params.containsKey("ll")) {
            val match = commaSeparatedPairRegex.matchEntire(params["ll"] ?: "")
            if (match != null) {
                latString = match.groupValues[1]
                lonString = match.groupValues[2]
            }
        }

        if (latString != null && lonString != null) {
            if (params.containsKey("z")) {
                z = params["z"]!!
            }
            return listOf(KGeoParsedPoint(latString, lonString, z))
        }

        if (params.containsKey("daddr")) {
            return parseGoogleMapsPath(params["daddr"]!!, params)
        } else if (params.containsKey("saddr")) {
            return parseGoogleMapsPath(params["saddr"]!!, params)
        } else if (params.containsKey("ftid")) {
            ftidCellId = params["ftid"]
        } else if (params.containsKey("q")) {
            var opath = params["q"]!!
            val pref = "loc:"
            if (opath.contains(pref)) {
                opath = opath.substring(opath.lastIndexOf(pref) + pref.length)
            }
            val postf = Regex("\\s\\((\\p{L}|\\p{M}|\\p{Z}|\\p{S}|\\p{N}|\\p{P}|\\p{C})*\\)$")
            opath = opath.replace(postf, "")
            return parseGoogleMapsPath(opath, params)
        } else if (params.containsKey("destination") || params.containsKey("origin")) {
            val parsedPoints = mutableListOf<KGeoParsedPoint>()

            if (params.containsKey("origin")) {
                val coordinates = params["origin"]!!.split(",")
                val lat = parseSilentDouble(coordinates[0])
                val lon = parseSilentDouble(coordinates[1])
                parsedPoints.add(KGeoParsedPoint(lat, lon))
            }

            if (params.containsKey("destination")) {
                val coordinates = params["destination"]!!.split(",")
                val lat = parseSilentDouble(coordinates[0])
                val lon = parseSilentDouble(coordinates[1])
                parsedPoints.add(KGeoParsedPoint(lat, lon))
            }

            return parsedPoints
        }

        if (fragment != null) {
            val match = Regex(".*[!&]q=([^&!]+).*").matchEntire(fragment)
            if (match != null) {
                return listOf(KGeoParsedPoint(match.groupValues[1]))
            }
        }

        val dataPrefix = "/data="
        val pathPrefixes = arrayOf(dataPrefix, "/@", "/ll=", "loc:", "/")
        for (pref in pathPrefixes) {
            mutablePath = uri.path ?: ""
            if (mutablePath.contains(pref)) {
                mutablePath = mutablePath.substring(mutablePath.lastIndexOf(pref) + pref.length)
                if (mutablePath.contains("/")) {
                    mutablePath = mutablePath.substring(0, mutablePath.indexOf('/'))
                }
                if (pref == dataPrefix) {
                    val vls = mutablePath.split("!")
                    var lat: String? = null
                    var lon: String? = null
                    for (v in vls) {
                        when {
                            v.startsWith("3d") -> lat = v.substring(2)
                            v.startsWith("4d") -> lon = v.substring(2)
                            v.startsWith("1s") -> ftidCellId = v.substring(2)
                        }
                    }
                    if (lat != null && lon != null) {
                        return listOf(KGeoParsedPoint(lat.toDouble(), lon.toDouble()))
                    }
                } else {
                    if (pref == "/" && ftidCellId != null) {
                        // ftid (1s) processed after 3d/4d and /@
                        val ll = parseS2ftid(ftidCellId)
                        if (ll != null) {
                            return listOf(KGeoParsedPoint(ll.latitude, ll.longitude, true))
                        }
                    }
                    return parseGoogleMapsPath(mutablePath, params)
                }
            }
        }

        return null
    }

    private fun parseAmapUri(uri: KUri, scheme: String, host: String): List<KGeoParsedPoint>? {
        /* amap (mis)uses the Fragment, which is not included in the Scheme Specific Part,
		 * so instead we make a custom "everything but the Authority subString */
        // +4 for the :// and the /
        val subString = uri.original.substring(scheme.length + host.length + 4)
        val patterns = arrayOf(
            /* though this looks like Query String, it is also used as part of the Fragment */
            Regex(".*q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*&radius=(\\d+).*"),
            Regex(".*q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*"),
            Regex(".*p=(?:[A-Z0-9]+),([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*")
        )
        for (pattern in patterns) {
            val match = pattern.matchEntire(subString)
            if (match != null) {
                if (match.groupValues.size == 4) {
                    // amap uses radius in meters, so do rough conversion into zoom level
                    val radius = match.groupValues[3].toFloat()
                    val zoom = kotlin.math.round(23.0 - kotlin.math.ln(radius.toDouble()) / kotlin.math.ln(2.0)).toLong()
                    return listOf(KGeoParsedPoint(match.groupValues[1], match.groupValues[2], zoom.toString())
                    )
                } else if (match.groupValues.size == 3) {
                    return listOf(KGeoParsedPoint(match.groupValues[1], match.groupValues[2]))
                }
            }
        }
        return null
    }

    private fun parseHereUri(path: String, params: Map<String, String>): List<KGeoParsedPoint>? {
        var z = KGeoParsedPoint.NO_ZOOM.toString()
        var label: String? = null
        if (params.containsKey("msg")) {
            label = params["msg"]
        }
        if (params.containsKey("z")) {
            z = params["z"]!!
        }
        if (params.containsKey("map")) {
            val mapArray = params["map"]!!.split(",")
            if (mapArray.size > 2) {
                return listOf(KGeoParsedPoint(mapArray[0], mapArray[1], mapArray[2], label))
            } else if (mapArray.size > 1) {
                return listOf(KGeoParsedPoint(mapArray[0], mapArray[1], z, label))
            }
        }
        if (path.startsWith("/l/")) {
            val match = Regex("^/l/([+-]?\\d+(?:\\.\\d+)),([+-]?\\d+(?:\\.\\d+)),(.*)").matchEntire(path)
            if (match != null) {
                return listOf(
                    KGeoParsedPoint(
                        match.groupValues[1],
                        match.groupValues[2],
                        z,
                        match.groupValues[3]
                    )
                )
            }
        }
        return null
    }

    private fun parseQqUri(
        params: Map<String, String>,
        commaSeparatedPairRegex: Regex
    ): List<KGeoParsedPoint>? {
        var x: String? = null
        var y: String? = null
        var z = KGeoParsedPoint.NO_ZOOM.toString()
        var label: String? = null

        label = when {
            params.containsKey("city") -> params["city"]
            params.containsKey("key") -> params["key"]
            params.containsKey("a") -> params["a"]
            params.containsKey("n") -> params["n"]
            else -> null
        }

        val m = params["m"]
        if (m != null) {
            val match = commaSeparatedPairRegex.matchEntire(m)
            if (match != null) {
                x = match.groupValues[2]
                y = match.groupValues[1]
            }
        }

        val c = params["c"]
        if (c != null) {
            val match = commaSeparatedPairRegex.matchEntire(c)
            if (match != null) {
                // there are two different patterns of data that can be in ?c=
                x = match.groupValues[2]
                y = match.groupValues[1]
            } else {
                x = Regex(".*\"lng\":\\s*([+\\-]?[0-9.]+).*").replace(c, "$1")
                if (x == null) { // try 'lon' for the second time
                    x = Regex(".*\"lon\":\\s*([+\\-]?[0-9.]+).*").replace(c, "$1")
                }
                y = Regex(".*\"lat\":\\s*([+\\-]?[0-9.]+).*").replace(c, "$1")
                z = Regex(".*\"l\":\\s*([+-]?[0-9.]+).*").replace(c, "$1")
                return listOf(KGeoParsedPoint(y, x, z, label))
            }
        }

        for (key in arrayOf("centerX", "x", "x1", "x2")) {
            if (params.containsKey(key)) {
                x = params[key]
                break
            }
        }
        for (key in arrayOf("centerY", "y", "y1", "y2")) {
            if (params.containsKey(key)) {
                y = params[key]
                break
            }
        }
        if (x != null && y != null) {
            return listOf(KGeoParsedPoint(y, x, z, label))
        }
        return null
    }

    private fun parseAppleUri(
        params: Map<String, String>,
        commaSeparatedPairRegex: Regex
    ): List<KGeoParsedPoint>? {
        // https://developer.apple.com/library/iad/featuredarticles/iPhoneURLScheme_Reference/MapLinks/MapLinks.html
        var z = KGeoParsedPoint.NO_ZOOM.toString()
        var label: String? = null
        if (params.containsKey("q")) {
            label = params["q"]
        }
        if (params.containsKey("near")) {
            label = params["near"]
        }
        if (params.containsKey("z")) {
            z = params["z"]!!
        }

        val ll = params["ll"]
        if (ll != null) {
            val match = commaSeparatedPairRegex.matchEntire(ll)
            if (match != null) {
                return listOf(KGeoParsedPoint(match.groupValues[1], match.groupValues[2], z, label))
            }
        }

        val sll = params["sll"]
        if (sll != null) {
            val match = commaSeparatedPairRegex.matchEntire(sll)
            if (match != null) {
                return listOf(KGeoParsedPoint(match.groupValues[1], match.groupValues[2], z, label))
            }
        }
        // if no ll= or sll=, then just use the q string
        if (params.containsKey("q")) {
            return listOf(KGeoParsedPoint(params["q"]))
        }
        // if no q=, then just use the destination address
        if (params.containsKey("daddr")) {
            return listOf(KGeoParsedPoint(params["daddr"]))
        }
        // if no daddr=, then just use the source address
        if (params.containsKey("saddr")) {
            return listOf(KGeoParsedPoint(params["saddr"]))
        }
        return null
    }

    private fun parseGeoUri(uri: KUri): List<KGeoParsedPoint>? {
        var schemeSpecific = uri.schemeSpecificPart
        if (schemeSpecific == null) {
            return null
        }
        if (uri.rawSchemeSpecificPart.contains("%2B")) {
            schemeSpecific = schemeSpecific.replace("+", "%2B")
        }

        var name: String? = null
        val nameMatch = Regex("[\\+\\s]*\\((.*)\\)[\\+\\s]*$").find(schemeSpecific)
        if (nameMatch != null) {
            name = KUri.urlDecode(nameMatch.groupValues[1])
            schemeSpecific = schemeSpecific.substring(0, nameMatch.range.first)
        }

        val positionPart: String
        var queryPart = ""
        val queryStartIndex = schemeSpecific.indexOf('?')
        if (queryStartIndex == -1) {
            positionPart = schemeSpecific
        } else {
            positionPart = schemeSpecific.substring(0, queryStartIndex)
            if (queryStartIndex < schemeSpecific.length) {
                queryPart = schemeSpecific.substring(queryStartIndex + 1)
            }
        }

        val positionRegex = Regex("([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)")
        val positionMatch = positionRegex.find(positionPart)
        var lat = 0.0
        var lon = 0.0
        if (positionMatch != null) {
            lat = positionMatch.groupValues[1].toDouble()
            lon = positionMatch.groupValues[2].toDouble()
        }

        var zoom = KGeoParsedPoint.NO_ZOOM
        var searchRequest: String? = null

        for (param in queryPart.split("&")) {
            val paramName: String
            var paramValue: String? = null
            val nameValueDelimititerIndex = param.indexOf('=')
            if (nameValueDelimititerIndex == -1) {
                paramName = param
            } else {
                paramName = param.substring(0, nameValueDelimititerIndex)
                if (nameValueDelimititerIndex < param.length) {
                    paramValue = param.substring(nameValueDelimititerIndex + 1)
                }
            }

            if (paramName == "z" && paramValue != null) {
                zoom = paramValue.toFloat().toInt()
            } else if (paramName == "q" && paramValue != null) {
                searchRequest = KUri.urlDecode(paramValue)
            }
        }

        if (searchRequest != null) {
            val searchPattern = Regex("(?:\\.|,|\\s+|\\+|[+-]?\\d+(?:\\.\\d+)?)")
            val search = searchRequest.split(searchPattern)
            if (search.isNotEmpty()) {
                return listOf(KGeoParsedPoint(searchRequest))
            }
            val positionInSearchMatch = positionRegex.find(searchRequest)
            if (lat == 0.0 && lon == 0.0 && positionInSearchMatch != null) {
                val tempLat = positionInSearchMatch.groupValues[1].toDouble()
                val tempLon = positionInSearchMatch.groupValues[2].toDouble()
                if (tempLat in -90.0..90.0 && tempLon in -180.0..180.0) {
                    lat = tempLat
                    lon = tempLon
                }
            }
        }

        if (lat == 0.0 && lon == 0.0 && searchRequest != null) {
            return listOf(KGeoParsedPoint(searchRequest))
        }
        if (zoom != KGeoParsedPoint.NO_ZOOM) {
            return listOf(KGeoParsedPoint(lat, lon, zoom, name))
        }
        return listOf(KGeoParsedPoint(lat, lon, name))
    }

    private fun parseGoogleMapsPath(opath: String, params: Map<String, String>): List<KGeoParsedPoint>? {
        var zmPart = ""
        var descr = ""
        var path = opath
        val mutableParams = LinkedHashMap(params)

        if (path.contains("&")) {
            val vls = path.split("&")
            path = vls[0]
            for (i in 1 until vls.size) {
                val ik = vls[i].indexOf('=')
                if (ik > 0) {
                    mutableParams[vls[i].substring(0, ik)] = vls[i].substring(ik + 1)
                }
            }
        }

        if (path.contains("+")) {
            val plusIndex = path.indexOf("+")
            path = path.substring(0, plusIndex)   // TODO: Ask is this correct code? Maybe we need to run "descr" code line before "path" line. Because "path" overwrites itself and "descr" should be always empty.
            descr = path.substring(plusIndex + 1)
            if (descr.contains(")")) {
                descr = descr.substring(0, descr.indexOf(")"))
            }
        }

        if (mutableParams.containsKey("z")) {
            zmPart = mutableParams["z"]!!
        }

        var vls: Array<String>? = null
        if (path.contains("@")) {
            path = path.substring(path.indexOf("@") + 1)
            if (path.contains(",")) {
                vls = silentSplit(path, ",")
            }
        }
        if (vls == null) {
            vls = silentSplit(path, ",")
        }

        if (vls != null && vls.size >= 2) {
            val lat = parseSilentDouble(vls[0], Double.NaN)
            val lon = parseSilentDouble(vls[1], Double.NaN)
            var zoom = KGeoParsedPoint.NO_ZOOM
            if (vls.size >= 3 || zmPart.isNotEmpty()) {
                if (zmPart.isEmpty()) {
                    zmPart = vls[2]
                }
                if (zmPart.startsWith("z=")) {
                    zmPart = zmPart.substring(2)
                } else if (zmPart.contains("z")) {
                    zmPart = zmPart.substring(0, zmPart.indexOf('z'))
                }
                zoom = parseZoom(zmPart)
            }
            if (!lat.isNaN() && !lon.isNaN()) {
                return listOf(KGeoParsedPoint(lat, lon, zoom))
            }
        }

        return listOf(KGeoParsedPoint(KUri.urlDecode(opath)))
    }

    private fun silentSplit(vl: String?, split: String): Array<String>? {
        if (vl == null) {
            return null
        }
        return vl.split(split).toTypedArray()
    }

    fun parseZoom(zoom: String?): Int {
        return try {
            if (zoom != null) zoom.toFloat().toInt() else KGeoParsedPoint.NO_ZOOM
        } catch (_: NumberFormatException) {
            KGeoParsedPoint.NO_ZOOM
        }
    }

    private fun parseSilentDouble(zoom: String?): Double {
        return parseSilentDouble(zoom, 0.0)
    }

    private fun parseSilentDouble(zoom: String?, vl: Double): Double {
        return try {
            if (zoom != null) zoom.toDouble() else vl
        } catch (_: NumberFormatException) {
            vl
        }
    }

    private fun parseSilentInt(zoom: String?): Int {
        return try {
            if (zoom != null) zoom.toInt() else 0
        } catch (_: NumberFormatException) {
            0
        }
    }

    private fun parseS2ftid(ftid: String?): KLatLon? {
        if (ftid != null && !ftid.isEmpty()) {
            try {
                val id = KGeoPointParserSimpleS2.CellId.fromFtid(ftid!!)
                if (id.isValid()) {
                    val ll = id.toLatLon()
                    return KLatLon(ll[0], ll[1])
                }
            } catch (_: Exception) {
                return null
            }
        }
        return null
    }

    fun isGooGlUrl(url: String?): Boolean {
        if (url == null) {
            return false
        }
        val lowerUrl = url.lowercase()
        return lowerUrl.startsWith("http") && lowerUrl.contains("goo.gl/")
    }
}
