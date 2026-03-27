package net.osmand.shared

import net.osmand.shared.util.KGeoPointParserUtil
import net.osmand.shared.util.KGeoParsedPoint
import net.osmand.shared.util.UrlEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.math.round
import kotlin.math.pow

class KGeoPointParserUtilTest {

    @Test
    fun testGeoPointUrlDecode() {
        // bug in get scheme getSchemeSpecificPart()
        // equal results for : URI.create("geo:0,0?q=86HJV99P+29") && URI.create("geo:0,0?q=86HJV99P%2B29");
        val test = KGeoPointParserUtil.parse("geo:0,0?q=86HJV99P%2B29")
        assertEquals("86HJV99P+29", test!!.getQuery())
    }

    @Test
    fun testGoogleMaps() {
        // https://www.google.com/maps?daddr=Bahnhofplatz+3,+7000+Chur@46.853582,9.529903
        var actual = KGeoPointParserUtil.parse(
            "https://www.google.com/maps?daddr=Bahnhofplatz+3,+7000+Chur"
        )
        assertGeoPoint(actual!!, KGeoParsedPoint("Bahnhofplatz 3, 7000 Chur"))

        actual = KGeoPointParserUtil.parse(
            "https://www.google.com/maps?daddr=Bahnhofplatz+3,+7000+Chur@46.853582,9.529903"
        )
        println(actual)
        assertGeoPoint(actual!!, KGeoParsedPoint(46.853582, 9.529903))
    }

    @Test
    fun testGoogleMapsData() {
        // https://www.google.com/maps?daddr=Bahnhofplatz+3,+7000+Chur@46.853582,9.529903
        var actual = KGeoPointParserUtil.parse(
            "https://www.google.co.in/maps/place/10%C2%B007'16.8%22N+76%C2%B020'54.2%22E/@10.1213253,76.3478427,247m/data=!3m2!1e3!4b1!4m6!3m5!1s0x0:0x0!7e2!8m2!3d10.1213237!4d76.348392?shorturl=1"
        )
        assertGeoPoint(actual!!, KGeoParsedPoint(10.1213237, 76.348392))

        actual = KGeoPointParserUtil.parse(
            "https://www.google.co.in/maps/place/data=!3m2!1e3!4b1!4m6!3m5!1s0x0:0x0!7e2!8m2!3d10.1213237!4d76.348392?shorturl=1"
        )
        assertGeoPoint(actual!!, KGeoParsedPoint(10.1213237, 76.348392))

        actual = KGeoPointParserUtil.parse(
            "https://www.google.com/maps/place/Kamzik/@48.1821032,17.0941412,17z/data=!4m6!3m5!1s0x476c8c15dee00531:0x9fe526fd2f5bdb5b!8m2!3d48.1826322!4d17.0949707!16zL20vMDVqZGI0"
        )
        assertGeoPoint(actual!!, KGeoParsedPoint(48.1826322, 17.0949707)) // @imprecise + 3d,4d precise = precise

        actual = KGeoPointParserUtil.parse(
            "https://www.google.com/maps/place/F79P%2BJ43+Madi+Cottage+And+Party+Palace,+Baruwa,+Madi+Road,+Madi+44200/@27.4695437,84.2860334,17z/data=!4m6!3m5!1s0x3994f56ccd448a8b:0xd6641aa08823442a!8m2!16s%2Fg%2F11fz9wkqn2"
        )
        assertGeoPoint(actual!!, KGeoParsedPoint(27.4695437, 84.2860334, 17)) // @imprecise + ftid(!1s) = imprecise

        actual = KGeoPointParserUtil.parse(
            "https://www.google.com/maps/place/Madi/data=!4m2!3m1!1s0x3994f56ccd448a8b:0xd6641aa08823442a?utm_source=mstt_1&entry=gps&lucs=47062702"
        )
        assertGeoPoint(actual!!, KGeoParsedPoint(27.46432660356932, 84.29101872350722)) // ftid (1s)

        actual = KGeoPointParserUtil.parse(
            "https://maps.google.com/maps?hl=en-US&gl=de&um=1&ie=UTF-8&fb=1&sa=X&ftid=0x479e7415349b0571:0xb7e03dcf1f6347f6"
        )
        assertGeoPoint(actual!!, KGeoParsedPoint(48.19432002567145, 11.598369987369695)) // ftid (query string)

        actual = KGeoPointParserUtil.parse(
            "http://maps.google.com/?q=query&ftid=0x3f8dfd04d309f925:0x2867166b05b0bfe6&hl=en&gl=us&shorturl=1"
        )
        assertGeoPoint(actual!!, KGeoParsedPoint(35.74387999018563, 51.31251448121059)) // ftid (old-style with http)
    }

    @Test
    fun testMapsMeParser() {
        val actual = KGeoPointParserUtil.parse(
            "http://ge0.me/44TvlEGXf-/Kyiv"
        )
        assertGeoPoint(actual!!, KGeoParsedPoint(50.45003, 30.52414, 18, "Kyiv"))
    }

    @Test
    fun testGeoPoint() {
        val ilat = 34
        val ilon = -106
        val dlat = 34.99393
        val dlon = -106.61568
        val longLat = 34.993933029174805
        val longLon = -106.615680694580078
        val name = "Treasure Island"
        var z = KGeoParsedPoint.NO_ZOOM
        var url: String

        val noQueryParameters = arrayOf(
            "geo:0,0",
            "geo:0,0?",
            "http://download.osmand.net/go",
            "http://download.osmand.net/go?",
        )
        for (s in noQueryParameters) {
            val uri = KGeoPointParserUtil.createUri(s) ?: throw RuntimeException("Uri is null: $s")
            val map = KGeoPointParserUtil.getQueryParameters(uri)
            print("$s map: ${map.size}...")
            if (map.size != 0) {
                println("")
                throw RuntimeException("Map should be 0 but is ${map.size}")
            }
            println(" Passed!")
        }

        val oneQueryParameter = arrayOf(
            "geo:0,0?m",
            "geo:0,0?m=",
            "geo:0,0?m=foo",
            "geo:0,0?q=%D0%9D%D0",
            "http://download.osmand.net/go?lat",
            "http://download.osmand.net/go?lat=",
            "http://download.osmand.net/go?lat=34.99393",
        )
        for (s in oneQueryParameter) {
            val uri = KGeoPointParserUtil.createUri(s) ?: throw RuntimeException("Uri is null: $s")
            val map = KGeoPointParserUtil.getQueryParameters(uri)
            print("$s map: ${map.size}...")
            if (map.size != 1) {
                println("")
                throw RuntimeException("Map should be 1 but is ${map.size}")
            }
            println(" Passed!")
        }

        val twoQueryParameters = arrayOf(
            "geo:0,0?z=11&q=Lots+Of+Stuff",
            "http://osmand.net/go?lat=34.99393&lon=-110.12345",
            "http://www.osmand.net/go.html?lat=34.99393&lon=-110.12345",
            "http://download.osmand.net/go?lat=34.99393&lon=-110.12345",
            "http://download.osmand.net/go?lat=34.99393&lon=-110.12345#this+should+be+ignored",
        )
        for (s in twoQueryParameters) {
            val uri = KGeoPointParserUtil.createUri(s) ?: throw RuntimeException("Uri is null: $s")
            val map = KGeoPointParserUtil.getQueryParameters(uri)
            print("$s map: ${map.size}...")
            if (map.size != 2) {
                println("")
                throw RuntimeException("Map should be 2 but is ${map.size}")
            }
            println(" Passed!")
        }

        // geo:34,-106
        url = "geo:$ilat,$ilon"
        println("url: $url")
        var actual = KGeoPointParserUtil.parse(url)

        assertUrlEquals(actual?.getGeoUriString(), url)
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble()))

        // google.navigation:q=34.99393,-106.61568
        url = "google.navigation:q=$dlat,$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon))

        // geo:34.99393,-106.61568
        url = "geo:$dlat,$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertUrlEquals(url, actual.getGeoUriString())
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon))

        // geo:34.99393,-106.61568?z=11
        z = 11
        url = "geo:$dlat,$dlon?z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertUrlEquals(url, actual.getGeoUriString())
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // geo:34.99393,-106.61568 (Treasure Island)
        url = "geo:$dlat,$dlon ($name)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, name))

        // geo:34.99393,-106.61568?z=11 (Treasure Island)
        z = 11
        url = "geo:$dlat,$dlon?z=$z ($name)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z, name))

        // geo:34.99393,-106.61568?q=34.99393%2C-106.61568 (Treasure Island)
        z = KGeoParsedPoint.NO_ZOOM
        url = "geo:$dlat,$dlon?q=$dlat%2C$dlon ($name)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z, name))

        // geo:34.99393,-106.61568?q=34.99393,-106.61568(Treasure+Island)
        z = KGeoParsedPoint.NO_ZOOM
        url = "geo:$dlat,$dlon?q=$dlat,$dlon(${UrlEncoder.encode(name)})"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z, name))
        assertUrlEquals(url, actual.getGeoUriString())

        // 0,0?q=34,-106(Treasure Island)
        z = KGeoParsedPoint.NO_ZOOM
        url = "geo:0,0?q=$ilat,$ilon($name)"
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z, name))

        // 0,0?q=34.99393,-106.61568(Treasure Island)
        z = KGeoParsedPoint.NO_ZOOM
        url = "geo:0,0?q=$dlat,$dlon($name)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z, name))

        // geo:0,0?z=11&q=34.99393,-106.61568(Treasure Island)
        z = 11
        url = "geo:0,0?z=$z&q=$dlat,$dlon ($name)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z, name))

        // geo:0,0?z=11&q=34.99393,-106.61568
        z = 11
        url = "geo:0,0?z=$z&q=$dlat,$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // google calendar
        // geo:0,0?q=760 West Genesee Street Syracuse NY 13204
        var qstr = "760 West Genesee Street Syracuse NY 13204"
        url = "geo:0,0?q=${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))
        assertUrlEquals(actual.getGeoUriString(), url)

        // geo:?q=Paris
        qstr = "Paris"
        url = "geo:?q=${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))
        assertUrlEquals("geo:0,0?q=${UrlEncoder.encode(qstr)}", actual.getGeoUriString())


        // geo:0,0?q=760 West Genesee Street Syracuse NY 13204
        qstr = "760 West Genesee Street Syracuse NY 13204"
        url = "geo:0,0?q=$qstr"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))

        // geo:0,0?z=11&q=1600+Amphitheatre+Parkway,+CA
        qstr = "1600 Amphitheatre Parkway, CA"
        url = "geo:0,0?q=${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))
        assertUrlEquals(url, actual.getGeoUriString())

        // geo:0,0?z=11&q=1600+Amphitheatre+Parkway,+CA
        qstr = "1600 Amphitheatre Parkway, CA"
        url = "geo:0,0?z=11&q=${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))

        // geo:50.451300,30.569900?z=15&q=50.451300,30.569900 (Kiev)
        z = 15
        val qname = "Kiev"
        var qlat = 50.4513
        var qlon = 30.5699

        url = "geo:50.451300,30.569900?z=15&q=50.451300,30.569900 (Kiev)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qlat, qlon, z, qname))

        // geo:0,0?q=50.45%2C%2030.5233
        z = KGeoParsedPoint.NO_ZOOM
        qlat = 50.4513
        qlon = 30.5699

        url = "geo:0,0?q=$qlat%2C%20$qlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qlat, qlon, z, null))

        // http://download.osmand.net/go?lat=34&lon=-106&z=11
        url = "http://download.osmand.net/go?lat=$ilat&lon=$ilon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        url = "http://www.openstreetmap.org/search?query=$qlat%2C$qlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qlat, qlon))

        url = "http://www.openstreetmap.org/search?query=$qlat%20$qlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qlat, qlon))

        // http://download.osmand.net/go?lat=34.99393&lon=-106.61568&z=11
        url = "http://download.osmand.net/go?lat=$dlat&lon=$dlon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://maps.google.com/maps?q=N34.939,W106
        url = "http://maps.google.com/maps?q=N$dlat,W${kotlin.math.abs(dlon)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon))

        url = "http://maps.google.com/maps?f=d&saddr=$dlat,$dlon&daddr=$dlat,$dlon&hl=en"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon))

        url = "http://maps.google.com/maps?f=d&saddr=My+Location&daddr=$dlat,$dlon&hl=en"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon))

        // http://www.osmand.net/go.html?lat=34&lon=-106&z=11
        url = "http://www.osmand.net/go.html?lat=$ilat&lon=$ilon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // http://www.osmand.net/go.html?lat=34.99393&lon=-106.61568&z=11
        url = "http://www.osmand.net/go.html?lat=$dlat&lon=$dlon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://osmand.net/go?lat=34&lon=-106&z=11
        url = "http://osmand.net/go?lat=$ilat&lon=$ilon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // http://osmand.net/go?lat=34.99393&lon=-106.61568&z=11
        url = "http://osmand.net/go?lat=$dlat&lon=$dlon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://openstreetmap.org/#map=11/34/-106
        z = 11
        url = "https://openstreetmap.org/#map=$z/$ilat/$ilon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // https://openstreetmap.org/#map=11/34.99393/-106.61568
        url = "https://openstreetmap.org/#map=$z/$dlat/$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://openstreetmap.org/#11/34.99393/-106.61568
        url = "https://openstreetmap.org/#$z/$dlat/$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://www.openstreetmap.org/#map=11/49.563/17.291
        url = "https://www.openstreetmap.org/#map=$z/$dlat/$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://www.openstreetmap.org/?mlat=34.993933029174805&mlon=-106.61568069458008#map=11/34.99393/-106.61568
        url = "https://www.openstreetmap.org/?mlat=$longLat&mlon=$longLon#map=$z/$dlat/$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://wiki.openstreetmap.org/wiki/Shortlink

        // https://osm.org/go/TyFSutZ-?m=
        // https://www.openstreetmap.org/?mlat=34.993933029174805&mlon=-106.61568069458008#map=15/34.99393/-106.61568
        z = 15
        url = "https://osm.org/go/TyFYuF6P--?m="
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertApproximateGeoPoint(actual, KGeoParsedPoint(longLat, longLon, z))

        // https://osm.org/go/TyFS--
        // https://www.openstreetmap.org/#map=3/34.99/-106.70
        z = 3
        url = "https://osm.org/go/TyFS--"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertApproximateGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://osm.org/go/TyFYuF6P~~-?m // current shortlink format with "~"
        // https://www.openstreetmap.org/?mlat=34.993933029174805&mlon=-106.61568069458008#map=15/34.99393/-106.61568
        z = 20
        url = "https://osm.org/go/TyFYuF6P~~-?m"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertApproximateGeoPoint(actual, KGeoParsedPoint(longLat, longLon, z))

        // https://osm.org/go/TyFYuF6P@@--?m= // old, deprecated shortlink format with "@"
        // https://www.openstreetmap.org/?mlat=34.993933029174805&mlon=-106.61568069458008#map=15/34.99393/-106.61568
        z = 21
        url = "https://osm.org/go/TyFYuF6P@@--?m="
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertApproximateGeoPoint(actual, KGeoParsedPoint(longLat, longLon, z))

        // https://openstreetmap.de/zoom=11&lat=34&lon=-106
        z = 11
        url = "https://openstreetmap.de/zoom=$z&lat=$ilat&lon=$ilon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // https://openstreetmap.de/zoom=11&lat=34.99393&lon=-106.61568
        url = "https://openstreetmap.de/zoom=$z&lat=$dlat&lon=$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://openstreetmap.de/lat=34.99393&lon=-106.61568&zoom=11
        url = "https://openstreetmap.de/lat=$dlat&lon=$dlon&zoom=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://maps.google.com/maps/@34,-106,11z
        url = "http://maps.google.com/maps/@$ilat,$ilon,${z}z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // http://maps.google.com/maps/@34.99393,-106.61568,11z
        url = "http://maps.google.com/maps/@$dlat,$dlon,${z}z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://maps.google.com/maps/ll=34,-106,z=11
        url = "http://maps.google.com/maps/ll=$ilat,$ilon,z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // https://maps.google.com/maps?q=loc:-21.8835112,-47.7838932 (Name)
        url = "https://maps.google.com/maps?q=loc:$dlat,$dlon (Name)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon))

        // http://maps.google.com/maps/ll=34.99393,-106.61568,z=11
        url = "http://maps.google.com/maps/ll=$dlat,$dlon,z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://www.google.com/maps/?q=loc:34,-106&z=11
        url = "http://www.google.com/maps/?q=loc:$ilat,$ilon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // http://www.google.com/maps/?q=loc:34.99393,-106.61568&z=11
        url = "http://www.google.com/maps/?q=loc:$dlat,$dlon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://www.google.com/maps/preview#!q=paris&data=!4m15!2m14!1m13!1s0x47e66e1f06e2b70f%3A0x40b82c3688c9460!3m8!1m3!1d24383582!2d-95.677068!3d37.0625!3m2!1i1222!2i718!4f13.1!4m2!3d48.856614!4d2.3522219
        url = "https://www.google.com/maps/preview#!q=paris&data=!4m15!2m14!1m13!1s0x47e66e1f06e2b70f%3A0x40b82c3688c9460!3m8!1m3!1d24383582!2d-95.677068!3d37.0625!3m2!1i1222!2i718!4f13.1!4m2!3d48.856614!4d2.3522219"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint("paris"))

        // LEGACY this URL does not work, where is it used?
        // http://maps.google.com/maps/q=loc:34,-106&z=11
        url = "http://maps.google.com/maps/q=loc:$ilat,$ilon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // LEGACY this URL does not work, where is it used?
        // http://maps.google.com/maps/q=loc:34.99393,-106.61568&z=11
        url = "http://maps.google.com/maps/q=loc:$dlat,$dlon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // LEGACY  this URL does not work, where is it used?
        // whatsapp
        // http://maps.google.com/maps/q=loc:34,-106 (You)
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://maps.google.com/maps/q=loc:$ilat,$ilon (You)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // LEGACY this URL does not work, where is it used?
        // whatsapp
        // http://maps.google.com/maps/q=loc:34.99393,-106.61568 (You)
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://maps.google.com/maps/q=loc:$dlat,$dlon (You)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // whatsapp
        // https://maps.google.com/maps?q=loc:34.99393,-106.61568 (You)
        z = KGeoParsedPoint.NO_ZOOM
        url = "https://maps.google.com/maps?q=loc:$dlat,$dlon (You)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // whatsapp
        // https://maps.google.com/maps?q=loc:34.99393,-106.61568 (USER NAME)
        z = KGeoParsedPoint.NO_ZOOM
        url = "https://maps.google.com/maps?q=loc:$dlat,$dlon (USER NAME)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // whatsapp
        // https://maps.google.com/maps?q=loc:34.99393,-106.61568 (USER NAME)
        z = KGeoParsedPoint.NO_ZOOM
        url = "https://maps.google.com/maps?q=loc:$dlat,$dlon (+55 99 99999-9999)"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // whatsapp
        // https://www.google.com/maps/search/34.99393,-106.61568/data=!4m4!2m3!3m1!2s-23.2776,-45.8443128!4b1
        url = "https://maps.google.com/maps?q=loc:$dlat,$dlon/data=!4m4!2m3!3m1!2s-23.2776,-45.8443128!4b1"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://www.google.com/maps/search/food/34,-106,14z
        url = "http://www.google.com/maps/search/food/$ilat,$ilon,${z}z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // http://www.google.com/maps/search/food/34.99393,-106.61568,14z
        url = "http://www.google.com/maps/search/food/$dlat,$dlon,${z}z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://maps.google.com?saddr=Current+Location&daddr=34,-106
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://maps.google.com?saddr=Current+Location&daddr=$ilat,$ilon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // http://maps.google.com?saddr=Current+Location&daddr=34.99393,-106.61568
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://maps.google.com?saddr=Current+Location&daddr=$dlat,$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://www.google.com/maps/dir/Current+Location/34,-106
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://www.google.com/maps/dir/Current+Location/$ilat,$ilon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // http://www.google.com/maps/dir/Current+Location/34.99393,-106.61568
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://www.google.com/maps/dir/Current+Location/$dlat,$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://maps.google.com/maps?q=34,-106
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://maps.google.com/maps?q=$ilat,$ilon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // http://maps.google.com/maps?q=34.99393,-106.61568
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://maps.google.com/maps?q=$dlat,$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://maps.google.co.uk/?q=34.99393,-106.61568
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://maps.google.co.uk/?q=$dlat,$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://www.google.com.tr/maps?q=34.99393,-106.61568
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://www.google.com.tr/maps?q=$dlat,$dlon"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://maps.google.com/maps?lci=com.google.latitudepublicupdates&ll=34.99393%2C-106.61568&q=34.99393%2C-106.61568
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://maps.google.com/maps?lci=com.google.latitudepublicupdates&ll=$dlat%2C$dlon&q=$dlat%2C$dlon(($dlat%2C%20$dlon))"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://www.google.com/maps/place/34%C2%B059'38.1%22N+106%C2%B036'56.5%22W/@34.99393,-106.61568,17z/data=!3m1!4b1!4m2!3m1!1s0x0:0x0
        z = 17
        url = "https://www.google.com/maps/place/34%C2%B059'38.1%22N+106%C2%B036'56.5%22W/@$dlat,$dlon,${z}z/data=!3m1!4b1!4m2!3m1!1s0x0:0x0"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://www.google.com/maps/place/760+West+Genesee+Street+Syracuse+NY+13204
        qstr = "760 West Genesee Street Syracuse NY 13204"
        url = "http://www.google.com/maps/place/${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))

        // http://maps.google.com/maps?q=760+West+Genesee+Street+Syracuse+NY+13204
        qstr = "760 West Genesee Street Syracuse NY 13204"
        url = "http://www.google.com/maps?q=${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))

        // http://www.openstreetmap.org/search?query=Amsterdam
        qstr = "Amsterdam"
        url = "http://www.openstreetmap.org/search?query=${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))

        // http://www.openstreetmap.org/search?query=Bloemstraat+51A,+Amsterdam
        qstr = "Bloemstraat 51A, Amsterdam"
        url = "http://www.openstreetmap.org/search?query=${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr.replace(',', ' ')))

        // http://maps.google.com/maps?daddr=760+West+Genesee+Street+Syracuse+NY+13204
        qstr = "760 West Genesee Street Syracuse NY 13204"
        url = "http://www.google.com/maps?daddr=${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))

        // http://www.google.com/maps/dir/Current+Location/760+West+Genesee+Street+Syracuse+NY+13204
        qstr = "760 West Genesee Street Syracuse NY 13204"
        url = "http://www.google.com/maps/dir/Current+Location/${UrlEncoder.encode(qstr)}"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(qstr))

        // http://maps.yandex.ru/?ll=34,-106&z=11
        z = 11
        url = "http://maps.yandex.ru/?ll=$ilat,$ilon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(ilat.toDouble(), ilon.toDouble(), z))

        // http://maps.yandex.ru/?ll=34.99393,-106.61568&z=11
        z = 11
        url = "http://maps.yandex.ru/?ll=$dlat,$dlon&z=$z"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://map.baidu.com/?l=13&tn=B_NORMAL_MAP&c=13748138,4889173&s=gibberish
        z = 7
        val latint = (dlat * 100000).toInt()
        val lonint = (dlon * 100000).toInt()
        url = "http://map.baidu.com/?l=$z&tn=B_NORMAL_MAP&c=$latint,$lonint&s=gibberish"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://www.amap.com/#!poi!!q=38.174596,114.995033|2|%E5%AE%BE%E9%A6%86&radius=1000
        z = 13 // amap uses radius, so 1000m is roughly zoom level 13
        url = "http://www.amap.com/#!poi!!q=$dlat,$dlon|2|%E5%AE%BE%E9%A6%86&radius=1000"
        println("\nurl: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        z = KGeoParsedPoint.NO_ZOOM
        url = "http://www.amap.com/?q=$dlat,$dlon,%E4%B8%8A%E6%B5v%B7%E5%B8%82%E6%B5%A6%E4%B8%9C%E6%96%B0%E5%8C%BA%E4%BA%91%E5%8F%B0%E8%B7%AF8086"
        println("\nurl: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://share.here.com/l/52.5134272,13.3778416,Hannah-Arendt-Stra%C3%9Fe?z=16.0&t=normal
        url = "http://share.here.com/l/$dlat,$dlon,Hannah-Arendt-Stra%C3%9Fe?z=$z&t=normal"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://www.here.com/location?map=52.5134272,13.3778416,16,normal&msg=Hannah-Arendt-Stra%C3%9Fe
        z = 16
        url = "https://www.here.com/location?map=$dlat,$dlon,$z,normal&msg=Hannah-Arendt-Stra%C3%9Fe"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://www.here.com/?map=48.23145,16.38454,15,normal
        z = 16
        url = "https://www.here.com/?map=$dlat,$dlon,$z,normal"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://map.wap.qq.com/loc/detail.jsp?sid=AU8f3ck87L6XDmytunBm4iWg&g_ut=2&city=%E5%8C%97%E4%BA%AC&key=NOBU%20Beijing&x=116.48177&y=39.91082&md=10461366113386140862
        z = KGeoParsedPoint.NO_ZOOM
        url = "http://map.wap.qq.com/loc/detail.jsp?sid=AU8f3ck87L6XDmytunBm4iWg&g_ut=2&city=%E5%8C%97%E4%BA%AC&key=NOBU%20Beijing&x=$dlon&y=$dlat&md=10461366113386140862"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // http://map.qq.com/AppBox/print/?t=&c=%7B%22base%22%3A%7B%22l%22%3A11%2C%22lat%22%3A39.90403%2C%22lng%22%3A116.407526%7D%7D
        z = 11
        url = "http://map.qq.com/AppBox/print/?t=&c=%7B%22base%22%3A%7B%22l%22%3A11%2C%22lat%22%3A$dlat%2C%22lng%22%3A$dlon%7D%7D"
        println("url: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        // https://developer.apple.com/library/ios/featuredarticles/iPhoneURLScheme_Reference/MapLinks/MapLinks.html

        // http://maps.apple.com/?ll=
        z = 11
        url = "http://maps.apple.com/?ll=$dlat,$dlon&z=$z"
        println("\nurl: $url")
        actual = KGeoPointParserUtil.parse(url)!!
        assertGeoPoint(actual, KGeoParsedPoint(dlat, dlon, z))

        /* URLs straight from various services, instead of generated here */

        val urls = arrayOf(
            "https://openstreetmap.org/go/0LQ127-?m",
            "https://osm.org/go/0LQ127-?m",
            "https://osm.org/go/0EEQjE==",
            "https://osm.org/go/0EEQjEEb",
            "https://osm.org/go/0EE~jEEb",
            "https://osm.org/go/0EE@jEEb",
            "https://osm.org/go/~~~~",
            "https://osm.org/go/@@@@",
            "https://www.openstreetmap.org/#map=0/0/0",
            "https://www.openstreetmap.org/#map=0/180/180",
            "https://www.openstreetmap.org/#map=0/-180/-180",
            "https://www.openstreetmap.org/#map=0/180.0/180.0",
            "https://www.openstreetmap.org/#map=6/33.907/34.662",
            "https://www.openstreetmap.org/?mlat=49.56275939941406&mlon=17.291107177734375#map=8/49.563/17.291",
            "https://www.google.at/maps/place/Bargou,+Tunesien/@36.0922506,9.5676327,15z/data=!3m1!4b1!4m2!3m1!1s0x12fc5d0b4dc5e66f:0xbd3618c6193d14cd",
            "http://www.amap.com/#!poi!!q=38.174596,114.995033,%E6%B2%B3%E5%8C%97%E7%9C%81%E7%9F%B3%E5%AE%B6%E5%BA%84%E5%B8%82%E6%97%A0%E6%9E%81%E5%8E%BF",
            "http://wb.amap.com/?p=B013706PJN,38.179456,114.98577,%E6%96%B0%E4%B8%9C%E6%96%B9%E5%A4%A7%E9%85%92%E5%BA%97(%E4%BF%9D%E9%99%A9%E8%8A%B1...,%E5%BB%BA%E8%AE%BE%E8%B7%AF67%E5%8F%B7",
            "http://www.amap.com/#!poi!!q=38.179456,114.98577|3|B013706PJN",
            "http://www.amap.com/#!poi!!q=38.174596,114.995033|2|%E5%AE%BE%E9%A6%86&radius=1000",
            "http://www.amap.com/?p=B013704EJT,38.17914,114.976337,%E6%97%A0%E6%9E%81%E5%8E%BF%E4%BA%BA%E6%B0%91%E6%94%BF%E5%BA%9C,%E5%BB%BA%E8%AE%BE%E4%B8%9C%E8%B7%AF12%E5%8F%B7",
            "http://share.here.com/l/52.5134272,13.3778416,Hannah-Arendt-Stra%C3%9Fe?z=16.0&t=normal",
            "https://www.here.com/location?map=52.5134272,13.3778416,16,normal&msg=Hannah-Arendt-Stra%C3%9Fe",
            "https://www.here.com/?map=48.23145,16.38454,15,normal",
            "http://map.wap.qq.com/loc/detail.jsp?sid=AU8f3ck87L6XDmytunBm4iWg&g_ut=2&city=%E5%8C%97%E4%BA%AC&key=NOBU%20Beijing&x=116.48177&y=39.91082&md=10461366113386140862",
            "http://map.wap.qq.com/loc/d.jsp?c=113.275020,39.188380&m=113.275020,39.188380&n=%E9%BC%93%E6%A5%BC&a=%E5%B1%B1%E8%A5%BF%E7%9C%81%E5%BF%BB%E5%B7%9E%E5%B8%82%E7%B9%81%E5%B3%99%E5%8E%BF+&p=+&i=16959367104973338386&z=0",
            "http://map.wap.qq.com/loc/d.jsp?c=113.275020,39.188380&m=113.275020,39.188380&n=%E9%BC%93%E6%A5%BC&a=%E5%B1%B1%E8%A5%BF%E7%9C%81%E5%BF%BB%E5%B7%9E%E5%B8%82%E7%B9%81%E5%B3%99%E5%8E%BF+&p=+&i=16959367104973338386&z=0&m",
            "http://map.qq.com/AppBox/print/?t=&c=%7B%22base%22%3A%7B%22l%22%3A11%2C%22lat%22%3A39.90403%2C%22lng%22%3A116.407526%7D%7D",
            "http://maps.yandex.com/?text=Australia%2C%20Victoria%2C%20Christmas%20Hills&sll=145.319026%2C-37.650344&ll=145.319026%2C-37.650344&spn=0.352249%2C0.151501&z=12&l=map",
            "http://maps.apple.com/?q=Bargou,+Tunisien",
            "http://maps.apple.com/?daddr=Bargou,+Tunisien",
            "http://maps.apple.com/?lsp=7618&q=40.738065,-73.988898&sll=40.738065,-73.988898",
            "http://maps.apple.com/?lsp=9902&auid=13787349062281695774&sll=40.694576,-73.982992&q=Garden%20Nail%20%26%20Spa&hnear=325%20Gold%20St%2C%20Brooklyn%2C%20NY%20%2011201-3054%2C%20United%20States",
            "https://www.google.com/maps/place/Wild+Herb+Market/@33.32787,-105.66291,14z/data=!4m5!1m2!2m1!1sfood!3m1!1s0x86e1ce2079e1f94b:0x1d7460465dcaf3ed",
            "http://www.google.com/maps/search/food/@34,-106,14z",
            "http://www.google.com/maps/search/food/@34.99393,-106.61568,14z",
        )

        for (u in urls) {
            println("url: $u")
            actual = KGeoPointParserUtil.parse(u)
                ?: throw RuntimeException("$u not parsable!")
            println("Properly parsed as: ${actual.getGeoUriString()}")
        }

        // these URLs are not parsable, but should not crash or cause problems
        val unparsableUrls = arrayOf(
            "http://maps.yandex.ru/-/CVCw6M9g",
            "http://maps.yandex.com/-/CVCXEKYW",
            "http://goo.gl/maps/Cji0V",
            "http://amap.com/0F0i02",
            "http://j.map.baidu.com/oXrVz",
            "http://l.map.qq.com/9741483212?m",
            "http://map.qq.com/?l=261496722",
            "http://her.is/vLCEXE",
        )

        for (u in unparsableUrls) {
            println("url: $u")
            actual = KGeoPointParserUtil.parse(u)
            if (actual != null) {
                throw RuntimeException("$u not parsable, but parse did not return null!")
            }
            println("Handled URL")
        }
    }

    @Test
    fun testDirectionsPoints() {
        var actual = KGeoPointParserUtil.parsePoints(
            "https://www.openstreetmap.org/directions?from=37.2445%2C-121.9125&to=37.2888%2C-122.1899"
        )!!
        assertGeoPoint(actual[0], KGeoParsedPoint(37.2445, -121.9125))
        assertGeoPoint(actual[1], KGeoParsedPoint(37.2888, -122.1899))

        actual = KGeoPointParserUtil.parsePoints(
            "https://www.google.com/maps/dir/?api=1&origin=-39.7649077,175.8822549&destination=-41.156615,174.975586"
        )!!
        assertGeoPoint(actual[0], KGeoParsedPoint(-39.7649077, 175.8822549))
        assertGeoPoint(actual[1], KGeoParsedPoint(-41.156615, 174.975586))
    }

    private fun areCloseEnough(a: Double, b: Double, howClose: Long): Boolean {

        val aRounded = round(a * 10.0.pow(howClose.toDouble())).toLong()
        val bRounded = round(b * 10.0.pow(howClose.toDouble())).toLong()
        return aRounded == bRounded
    }

    private fun assertGeoPoint(actual: KGeoParsedPoint?, expected: KGeoParsedPoint) {
        if (actual == null) {
            throw RuntimeException("Actual point is null. Expected: ${expected!!.getQuery()}")
        }

        if (expected.getQuery() != null) {
            if (expected.getQuery() != actual.getQuery()) {
                throw RuntimeException(
                    "Query param not equal:\n'${actual.getQuery()}' != '${expected.getQuery()}"
                )
            }
        } else {
            val aLat = actual.getLatitude()
            val eLat = expected.getLatitude()
            val aLon = actual.getLongitude()
            val eLon = expected.getLongitude()
            val aZoom = actual.getZoom()
            val eZoom = expected.getZoom()
            val aLabel = actual.getLabel()
            val eLabel = expected.getLabel()

            if (eLabel != null) {
                if (aLabel != eLabel) {
                    throw RuntimeException("Point label is not equal; actual=$aLabel, expected=$eLabel")
                }
            }
            if (!areCloseEnough(eLat, aLat, 5)) {
                throw RuntimeException("Latitude is not equal; actual=$aLat, expected=$eLat")
            }
            if (!areCloseEnough(eLon, aLon, 5)) {
                throw RuntimeException("Longitude is not equal; actual=$aLon, expected=$eLon")
            }
            if (eZoom != aZoom) {
                throw RuntimeException("Zoom is not equal; actual=$aZoom, expected=$eZoom")
            }
        }
        println("Passed: $actual")
    }

    private fun assertApproximateGeoPoint(actual: KGeoParsedPoint, expected: KGeoParsedPoint) {
        if (expected.getQuery() != null) {
            if (expected.getQuery() != actual.getQuery()) {
                throw RuntimeException("Query param not equal")
            }
        } else {
            val aLat = actual.getLatitude()
            val eLat = expected.getLatitude()
            val aLon = actual.getLongitude()
            val eLon = expected.getLongitude()
            val aZoom = actual.getZoom()
            val eZoom = expected.getZoom()
            val aLabel = actual.getLabel()
            val eLabel = expected.getLabel()

            if (eLabel != null) {
                if (aLabel != eLabel) {
                    throw RuntimeException("Point label is not equal; actual=$aLabel, expected=$eLabel")
                }
            }
            if (eLat.toInt() != aLat.toInt()) {
                throw RuntimeException("Latitude is not equal; actual=$aLat, expected=$eLat")
            }
            if (eLon.toInt() != aLon.toInt()) {
                throw RuntimeException("Longitude is not equal; actual=$aLon, expected=$eLon")
            }
            if (eZoom != aZoom) {
                throw RuntimeException("Zoom is not equal; actual=$aZoom, expected=$eZoom")
            }
        }
        println("Passed!")
    }

    private fun assertUrlEquals(actual: String?, expected: String?) {
        if (actual == null || actual != expected) {
            throw RuntimeException("URLs not equal; actual=$actual, expected=$expected")
        }
    }
}