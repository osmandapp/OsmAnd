package net.osmand.util;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoPointParserUtil {

	public static void main(String[] args) {
		final int ilat = 34, ilon = -106;
		final double dlat = 34.99393, dlon = -106.61568;
		final double longLat = 34.993933029174805, longLon = -106.615680694580078;
		final String name = "Treasure Island";
		int z = GeoParsedPoint.NO_ZOOM;
		String url;

		// geo:34,-106
		url = "geo:" + ilat + "," + ilon;
		System.out.println("url: " + url);
		GeoParsedPoint actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon));

		// geo:34.99393,-106.61568
		url = "geo:" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon));

		// geo:34.99393,-106.61568?z=11
		z = 11;
		url = "geo:" + dlat + "," + dlon + "?z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// geo:34.99393,-106.61568 (Treasure Island)
		url = "geo:" + dlat + "," + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, name));

		// geo:34.99393,-106.61568?z=11 (Treasure Island)
		z = 11;
		url = "geo:" + dlat + "," + dlon + "?z=" + z + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// geo:34.99393,-106.61568?q=34.99393%2C-106.61568 (Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:" + dlat + "," + dlon + "?q=" + dlat + "%2C" + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// 0,0?q=34,-106(Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:0,0?q=" + ilat + "," + ilon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z, name));

		// 0,0?q=34.99393,-106.61568(Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:0,0?q=" + dlat + "," + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// geo:0,0?z=11&q=34.99393,-106.61568(Treasure Island)
		z = 11;
		url = "geo:0,0?z=" + z + "&q=" + dlat + "," + dlon + " (" + name + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));

		// geo:0,0?z=11&q=34.99393,-106.61568
		z = 11;
		url = "geo:0,0?z=" + z + "&q=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// google calendar
		// geo:0,0?q=760 West Genesee Street Syracuse NY 13204
		String qstr = "760 West Genesee Street Syracuse NY 13204";
		url = "geo:0,0?q=" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// geo:0,0?z=11&q=1600+Amphitheatre+Parkway,+CA
		qstr = "1600 Amphitheatre Parkway, CA";
		url = "geo:0,0?z=11&q=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// geo:50.451300,30.569900?z=15&q=50.451300,30.569900 (Kiev)
		z = 15;
		String qname = "Kiev";
		double qlat = 50.4513;
		double qlon = 30.5699;

		url = "geo:50.451300,30.569900?z=15&q=50.451300,30.569900 (Kiev)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qlat, qlon, z, qname));

		// http://download.osmand.net/go?lat=34&lon=-106&z=11
		url = "http://download.osmand.net/go?lat=" + ilat + "&lon=" + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://download.osmand.net/go?lat=34.99393&lon=-106.61568&z=11
		url = "http://download.osmand.net/go?lat=" + dlat + "&lon=" + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://openstreetmap.org/#map=11/34/-106
		z = 11;
		url = "http://openstreetmap.org/#map=" + z + "/" + ilat + "/" + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://openstreetmap.org/#map=11/34.99393/-106.61568
		url = "http://openstreetmap.org/#map=" + z + "/" + dlat + "/" + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://openstreetmap.org/#11/34.99393/-106.61568
		url = "http://openstreetmap.org/#" + z + "/" + dlat + "/" + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

        // https://www.openstreetmap.org/#map=11/49.563/17.291
		url = "https://www.openstreetmap.org/#map=" + z + "/" + dlat + "/" + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://www.openstreetmap.org/?mlat=34.993933029174805&mlon=-106.61568069458008#map=11/34.99393/-106.61568
		url = "https://www.openstreetmap.org/?mlat=" + longLat + "&mlon=" + longLon
            + "#map=" + z + "/" + dlat + "/" + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://wiki.openstreetmap.org/wiki/Shortlink

		// http://osm.org/go/TyFSutZ-?m=
		// https://www.openstreetmap.org/?mlat=34.993933029174805&mlon=-106.61568069458008#map=15/34.99393/-106.61568
		z = 15;
		url = "http://osm.org/go/TyFYuF6P--?m=";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertApproximateGeoPoint(actual, new GeoParsedPoint(longLat, longLon, z));

		// http://osm.org/go/TyFS--
		// http://www.openstreetmap.org/#map=3/34.99/-106.70
		z = 3;
		url = "http://osm.org/go/TyFS--";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertApproximateGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://openstreetmap.de/zoom=11&lat=34&lon=-106
		z = 11;
		url = "http://openstreetmap.de/zoom=" + z + "&lat=" + ilat + "&lon=" + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://openstreetmap.de/zoom=11&lat=34.99393&lon=-106.61568
		url = "http://openstreetmap.de/zoom=" + z + "&lat=" + dlat + "&lon=" + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://openstreetmap.de/lat=34.99393&lon=-106.61568&zoom=11
		url = "http://openstreetmap.de/lat=" + dlat + "&lon=" + dlon + "&zoom=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps/@34,-106,11z
		url = "http://maps.google.com/maps/@" + ilat + "," + ilon + "," + z + "z";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com/maps/@34.99393,-106.61568,11z
		url = "http://maps.google.com/maps/@" + dlat + "," + dlon + "," + z + "z";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps/ll=34,-106,z=11
		url = "http://maps.google.com/maps/ll=" + ilat + "," + ilon + ",z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com/maps/ll=34.99393,-106.61568,z=11
		url = "http://maps.google.com/maps/ll=" + dlat + "," + dlon + ",z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps/?q=loc:34,-106&z=11
		url = "http://maps.google.com/maps/q=loc:" + ilat + "," + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com/maps/?q=loc:34.99393,-106.61568&z=11
		url = "http://maps.google.com/maps/?q=loc:" + dlat + "," + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// whatsapp
		// http://maps.google.com/maps/?q=loc:34,-106 (You)
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps/?q=loc:" + ilat + "," + ilon + " (You)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// whatsapp
		// http://maps.google.com/maps/?q=loc:34.99393,-106.61568 (You)
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps/?q=loc:" + dlat + "," + dlon + " (You)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com/maps/search/food/@34,-106,14z
		url = "http://www.google.com/maps/search/food/@" + ilat + "," + ilon + "," + z + "z";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://www.google.com/maps/search/food/@34.99393,-106.61568,14z
		url = "http://www.google.com/maps/search/food/@" + dlat + "," + dlon + "," + z + "z";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com?saddr=Current+Location&daddr=34,-106
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com?saddr=Current+Location&daddr=" + ilat + "," + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com?saddr=Current+Location&daddr=34.99393,-106.61568
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com?saddr=Current+Location&daddr=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com/maps/dir/Current+Location/34,-106
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://www.google.com/maps/dir/Current+Location/" + ilat + "," + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://www.google.com/maps/dir/Current+Location/34.99393,-106.61568
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://www.google.com/maps/dir/Current+Location/" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps?q=34,-106
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps?q=" + ilat + "," + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.google.com/maps?q=34.99393,-106.61568
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps?q=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com/maps/place/760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760+West+Genesee+Street+Syracuse+NY+13204";
		url = "http://www.google.com/maps/place/" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://maps.google.com/maps?q=760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760+West+Genesee+Street+Syracuse+NY+13204";
		url = "http://www.google.com/maps?q=" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://maps.google.com/maps?daddr=760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760+West+Genesee+Street+Syracuse+NY+13204";
		url = "http://www.google.com/maps?daddr=" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://www.google.com/maps/dir/Current+Location/760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760+West+Genesee+Street+Syracuse+NY+13204";
		url = "http://www.google.com/maps/dir/Current+Location/" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://maps.yandex.ru/?ll=34,-106&z=11
		z = 11;
		url = "http://maps.yandex.ru/?ll=" + ilat + "," + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://maps.yandex.ru/?ll=34.99393,-106.61568&z=11
		z = 11;
		url = "http://maps.yandex.ru/?ll=" + dlat + "," + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://map.baidu.com/?l=13&tn=B_NORMAL_MAP&c=13748138,4889173&s=gibberish
		z = 7;
		int latint = ((int)(dlat * 100000));
		int lonint = ((int)(dlon * 100000));
		url = "http://map.baidu.com/?l=" + z + "&tn=B_NORMAL_MAP&c=" + latint + "," + lonint + "&s=gibberish";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

        // http://www.amap.com/#!poi!!q=38.174596,114.995033|2|%E5%AE%BE%E9%A6%86&radius=1000
		z = 13; // amap uses radius, so 1000m is roughly zoom level 13
		url = "http://www.amap.com/#!poi!!q=" + dlat + "," + dlon + "|2|%E5%AE%BE%E9%A6%86&radius=1000";
		System.out.println("\nurl: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		z = GeoParsedPoint.NO_ZOOM;
		url = "http://www.amap.com/?q=" + dlat + "," + dlon + ",%E4%B8%8A%E6%B5v%B7%E5%B8%82%E6%B5%A6%E4%B8%9C%E6%96%B0%E5%8C%BA%E4%BA%91%E5%8F%B0%E8%B7%AF8086";
		System.out.println("\nurl: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://developer.apple.com/library/ios/featuredarticles/iPhoneURLScheme_Reference/MapLinks/MapLinks.html

		// http://maps.apple.com/?ll=
		z = 11;
		url = "http://maps.apple.com/?ll=" + dlat + "," + dlon + "&z=" + z;
		System.out.println("\nurl: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

        /* URLs straight from various services, instead of generated here */
        
        String urls[] = {
			"https://openstreetmap.org/go/0LQ127-?m",
			"http://osm.org/go/0LQ127-?m",
			"http://osm.org/go/0EEQjE==",
			"http://osm.org/go/0EEQjEEb",
            "https://www.openstreetmap.org/#map=0/0/0",
            "https://www.openstreetmap.org/#map=0/180/180",
            "https://www.openstreetmap.org/#map=0/-180/-180",
            "https://www.openstreetmap.org/#map=0/180.0/180.0",
            "https://www.openstreetmap.org/#map=6/33.907/34.662",
            "https://www.openstreetmap.org/?mlat=49.56275939941406&mlon=17.291107177734375#map=8/49.563/17.291",
            "https://www.google.com/maps/place/Wild+Herb+Market/@33.32787,-105.66291,14z/data=!4m5!1m2!2m1!1sfood!3m1!1s0x86e1ce2079e1f94b:0x1d7460465dcaf3ed",
            "http://www.amap.com/#!poi!!q=38.174596,114.995033,%E6%B2%B3%E5%8C%97%E7%9C%81%E7%9F%B3%E5%AE%B6%E5%BA%84%E5%B8%82%E6%97%A0%E6%9E%81%E5%8E%BF",
            "http://wb.amap.com/?p=B013706PJN,38.179456,114.98577,%E6%96%B0%E4%B8%9C%E6%96%B9%E5%A4%A7%E9%85%92%E5%BA%97(%E4%BF%9D%E9%99%A9%E8%8A%B1...,%E5%BB%BA%E8%AE%BE%E8%B7%AF67%E5%8F%B7",
            "http://www.amap.com/#!poi!!q=38.179456,114.98577|3|B013706PJN",
            "http://www.amap.com/#!poi!!q=38.174596,114.995033|2|%E5%AE%BE%E9%A6%86&radius=1000",
            "http://www.amap.com/?p=B013704EJT,38.17914,114.976337,%E6%97%A0%E6%9E%81%E5%8E%BF%E4%BA%BA%E6%B0%91%E6%94%BF%E5%BA%9C,%E5%BB%BA%E8%AE%BE%E4%B8%9C%E8%B7%AF12%E5%8F%B7",
        };

        for (String u : urls) {
            System.out.println("url: " + u);
            actual = GeoPointParserUtil.parse(u);
            assert(actual != null);
            System.out.println("Properly parsed!");
        }
	}

	private static boolean areCloseEnough(int a, int b) {
		return a == b;
	}

	private static boolean areCloseEnough(double a, double b, long howClose) {
		long aRounded = (long) Math.round(a * Math.pow(10, howClose));
		long bRounded = (long) Math.round(b * Math.pow(10, howClose));
		System.out.println("areCloseEnough: " + aRounded + ", " + bRounded);
		return aRounded == bRounded;
	}

	private static void assertGeoPoint(GeoParsedPoint actual, GeoParsedPoint expected) {
		if (expected.getQuery() != null) {
			if (!expected.getQuery().equals(actual.getQuery()))
				throw new RuntimeException("Query param not equal");
		} else {
			double aLat = actual.getLatitude(), eLat = expected.getLatitude(), aLon = actual.getLongitude(), eLon = expected.getLongitude();
			int aZoom = actual.getZoom(), eZoom = expected.getZoom();
			String aName = actual.getName(), eName = expected.getName();
			if (eName != null) {
				if (!aName.equals(eName)) {
					throw new RuntimeException("Point name\\capture is not equal; actual=" + aName + ", expected="
							+ eName);
				}
			}
			if (!areCloseEnough(eLat, aLat, 5)) {
				throw new RuntimeException("Latitude is not equal; actual=" + aLat + ", expected=" + eLat);
			}
			if (!areCloseEnough(eLon, aLon, 5)) {
				throw new RuntimeException("Longitude is not equal; actual=" + aLon + ", expected=" + eLon);
			}
			if (eZoom != aZoom) {
				throw new RuntimeException("Zoom is not equal; actual=" + aZoom + ", expected=" + eZoom);
			}
		}
		System.out.println("Passed!");
	}

	private static void assertApproximateGeoPoint(GeoParsedPoint actual, GeoParsedPoint expected) {
		if (expected.getQuery() != null) {
			if (!expected.getQuery().equals(actual.getQuery()))
				throw new RuntimeException("Query param not equal");
		} else {
			double aLat = actual.getLatitude(), eLat = expected.getLatitude(), aLon = actual.getLongitude(), eLon = expected.getLongitude();
			int aZoom = actual.getZoom(), eZoom = expected.getZoom();
			String aName = actual.getName(), eName = expected.getName();
			if (eName != null) {
				if (!aName.equals(eName)) {
					throw new RuntimeException("Point name\\capture is not equal; actual=" + aName + ", expected="
							+ eName);
				}
			}
			if (((int)eLat) != ((int)aLat)) {
				throw new RuntimeException("Latitude is not equal; actual=" + aLat + ", expected=" + eLat);
			}
			if (((int)eLon) != ((int)aLon)) {
				throw new RuntimeException("Longitude is not equal; actual=" + aLon + ", expected=" + eLon);
			}
			if (eZoom != aZoom) {
				throw new RuntimeException("Zoom is not equal; actual=" + aZoom + ", expected=" + eZoom);
			}
		}
		System.out.println("Passed!");
	}

	private static String getQueryParameter(final String param, URI uri) {
		final String query = uri.getQuery();
		String value = null;
		if (query != null && query.contains(param)) {
			String[] params = query.split("&");
			for (String p : params) {
				if (p.contains(param)) {
					value = p.substring(p.indexOf("=") + 1, p.length());
					break;
				}
			}
		}
		return value;
	}

	/**
	 * Parses geo and map intents:
	 *
	 * @param uri
	 *            The URI object
	 * @return {@link GeoParsedPoint}
	 */
	public static GeoParsedPoint parse(final String uriString) {
        URI uri;
        try {
            // amap.com uses | in their URLs, which is an illegal character
            uri = URI.create(uriString.replaceAll("\\s+", "+")
                             .replaceAll("%20", "+")
                             .replaceAll("%2C", ",")
                             .replaceAll("\\|", ";"));
        } catch (IllegalArgumentException e) {
            return null;
        }

        String scheme = uri.getScheme();
        if (scheme == null)
            return null;
        else
            scheme = scheme.toLowerCase(Locale.US);

		if ("http".equals(scheme) || "https".equals(scheme)) {
            String host = uri.getHost();
            if (host == null)
                return null;
            else
                host = host.toLowerCase(Locale.US);

			final String schemeSpecific = uri.getSchemeSpecificPart();
			if (schemeSpecific == null)
				return null;

            try {
                if (host.equals("osm.org") || host.endsWith("openstreetmap.org")) {
                    Pattern p;
                    Matcher matcher;
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/go/")) { // short URL form
                        p = Pattern.compile("^/go/([A-Za-z0-9_@~]+-*)(?:.*)");
                        matcher = p.matcher(uri.getPath());
                        if (matcher.matches()) {
                            return MapUtils.decodeShortLinkString(matcher.group(1));
                        }
                    } else { // data in the query and/or feature strings
                        String lat = "0";
                        String lon = "0";
                        String zoom = String.valueOf(GeoParsedPoint.NO_ZOOM);
                        String fragment = uri.getFragment();
                        if (fragment != null) {
                            p = Pattern.compile("(?:map=)?(\\d{1,2})/([+-]?\\d+(?:\\.\\d+)?)/([+-]?\\d+(?:\\.\\d+)?)(?:.*)");
                            matcher = p.matcher(fragment);
                            if (matcher.matches()) {
                                zoom = matcher.group(1);
                                lat = matcher.group(2);
                                lon = matcher.group(3);
                            }
                        }
                        String query = uri.getQuery();
                        if (query != null) {
                            // the query string sometimes has higher resolution values
                            p = Pattern.compile("(?:.*)mlat=([+-]?\\d+(?:\\.\\d+)?)(?:.*)&mlon=([+-]?\\d+(?:\\.\\d+)?)(?:.*)?");
                            matcher = p.matcher(query);
                            if (matcher.matches()) {
                                lat = matcher.group(1);
                                lon = matcher.group(2);
                            }
                        }
                        return new GeoParsedPoint(lat, lon, zoom);
                    }
                } else if (host.matches("(maps|www)\\.?google.[a-z]+")) { // support www./maps. and .de/.com/.in/.eg/etc.
                    final String subString = schemeSpecific.substring(host.length() + 3); // +3 for the // and the /
                    Pattern p;
                    Matcher matcher;
                    final String[] patterns = {
                        "(?:.*)[@/]([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d{1,2})z(?:.*)",
                        "(?:.*)ll=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.+)z=([+-]?\\d{1,2})(?:.*)",
                        "(?:.*)q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)&?z=([+-]?\\d{1,2})",
                        "(?:.*)q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)",
                        "(?:.*)q=loc:([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)&?z=([+-]?\\d{1,2})(?:.*)",
                        // only includes lat/lon, not zoom
                        "(?:.*)q=loc:([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)",
                        "(?:.*)daddr=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)",
                        "(?:.*)/([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.*)",
                        // match as a single group
                        "(?:.*)daddr=(.*)",
                        ".*[/?&]?q=(.*)",
                        "(?:.*)/(.*)", };
                    for (int i = 0; i < patterns.length; i++) {
                        p = Pattern.compile(patterns[i]);
                        matcher = p.matcher(subString);
                        if (matcher.matches()) {
                            if (matcher.groupCount() == 3)
                                return new GeoParsedPoint(matcher.group(1), matcher.group(2), matcher.group(3));
                            else if (matcher.groupCount() == 2)
                                return new GeoParsedPoint(matcher.group(1), matcher.group(2));
                            else if (matcher.groupCount() == 1)
                                return new GeoParsedPoint(matcher.group(1));
                        }
                    }
                } else if (schemeSpecific.startsWith("//map.baidu.")) { // .com and .cn both work
                    /* Baidu Map uses a custom format for lat/lon., it is basically standard lat/lon
                     * multiplied by 100,000, then rounded to an integer */
                    Pattern p = Pattern.compile(".*[/?&]l=(\\d{1,2}).*&c=([+-]?\\d+),([+-]?\\d+).*");
                    Matcher matcher = p.matcher(schemeSpecific);
                    if (matcher.matches()) {
                        double lat = Integer.valueOf(matcher.group(2)) / 100000.;
                        double lon = Integer.valueOf(matcher.group(3)) / 100000.;
                        int zoom = parseZoom(matcher.group(1));
                        return new GeoParsedPoint(lat, lon, zoom);
                    }
				} else if (host.equals("maps.apple.com")) {
					Pattern p = Pattern.compile(".*ll=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.+z=(\\d{1,2}).*)");
					Matcher matcher = p.matcher(uri.getQuery());
					if (matcher.matches()) {
						String lat = matcher.group(1);
						String lon = matcher.group(2);
						String zoom = String.valueOf(GeoParsedPoint.NO_ZOOM);
						if (matcher.groupCount() == 3) {
							zoom = matcher.group(3);
						} else if (matcher.groupCount() == 2) {
							// see if z= precedes ll=
							p = Pattern.compile(".*z=(\\d{1,2}).*");
							Matcher zoomMatcher = p.matcher(uri.getQuery());
							if (zoomMatcher.matches()) {
								zoom = zoomMatcher.group(1);
							}
						}
						return new GeoParsedPoint(lat, lon, zoom);
					}
                } else if (host.startsWith("maps.yandex.")) {
                    Pattern p = Pattern.compile("(?:.*)ll=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)(?:.+)z=(\\d{1,2})(?:.*)");
                    Matcher matcher = p.matcher(uri.getQuery());
                    if (matcher.matches()) {
                        return new GeoParsedPoint(matcher.group(1), matcher.group(2), matcher.group(3));
                    }
                } else if (host.endsWith(".amap.com")) {
                    /* amap (mis)uses the Fragment, which is not included in the Scheme Specific Part,
                     * so instead we make a custom "everything but the Authority subString */
                    // +4 for the :// and the /
                    final String subString = uri.toString().substring(scheme.length() + host.length() + 4);
                    Pattern p;
                    Matcher matcher;
                    final String[] patterns = {
                        /* though this looks like Query String, it is also used as part of the Fragment */
                        ".*q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*&radius=(\\d+).*",
                        ".*q=([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*",
                        ".*p=(?:[A-Z0-9]+),([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*", };
                    for (int i = 0; i < patterns.length; i++) {
                        p = Pattern.compile(patterns[i]);
                        matcher = p.matcher(subString);
                        if (matcher.matches()) {
                            if (matcher.groupCount() == 3) {
                                // amap uses radius in meters, so do rough conversion into zoom level
                                float radius = Float.valueOf(matcher.group(3));
                                long zoom = Math.round(23. - Math.log(radius)/Math.log(2.0));
                                return new GeoParsedPoint(matcher.group(1), matcher.group(2), String.valueOf(zoom));
                            } else if (matcher.groupCount() == 2) {
                                return new GeoParsedPoint(matcher.group(1), matcher.group(2));
                            }
                        }
                    }
                } else if (host.endsWith("openstreetmap.de")) {
                    Pattern p = Pattern.compile("(?:.*)zoom=(\\d{1,2})&lat=([+-]?\\d+(?:\\.\\d+)?)&lon=([+-]?\\d+(?:\\.\\d+)?)(?:.*)");
                    Matcher matcher = p.matcher(schemeSpecific);
                    if (matcher.matches()) {
                        return new GeoParsedPoint(matcher.group(2), matcher.group(3), matcher.group(1));
                    }
                    // try the secondary pattern
                    p = Pattern.compile("(?:.*)lat=([+-]?\\d+(?:\\.\\d+)?)&lon=([+-]?\\d+(?:\\.\\d+)?)&?z(?:oom)?=(\\d{1,2})(?:.*)");
                    matcher = p.matcher(schemeSpecific);
                    if (matcher.matches()) {
                        return new GeoParsedPoint(matcher.group(1), matcher.group(2), matcher.group(3));
                    }
                } else if (host.equals("download.osmand.net")) {
                    Pattern p = Pattern.compile("lat=([+-]?\\d+(?:\\.\\d+)?)&lon=([+-]?\\d+(?:\\.\\d+)?)&?z=(\\d{1,2})");
                    Matcher matcher = p.matcher(uri.getQuery());
                    if (matcher.matches()) {
                        return new GeoParsedPoint(matcher.group(1), matcher.group(2), matcher.group(3));
                    }
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
			return null;
		} else if ("geo".equals(scheme) || "osmand.geo".equals(scheme)) {
			String schemeSpecific = uri.getSchemeSpecificPart();
			if (schemeSpecific == null) {
				return null;
			}

			String name = null;
			final Pattern namePattern = Pattern.compile("[\\+\\s]*\\((.*)\\)[\\+\\s]*$");
			final Matcher nameMatcher = namePattern.matcher(schemeSpecific);
			if (nameMatcher.find()) {
				name = URLDecoder.decode(nameMatcher.group(1));
				if (name != null) {
					schemeSpecific = schemeSpecific.substring(0, nameMatcher.start());
				}
			}

			String positionPart;
			String queryPart = "";
			int queryStartIndex = schemeSpecific.indexOf('?');
			if (queryStartIndex == -1) {
				positionPart = schemeSpecific;
			} else {
				positionPart = schemeSpecific.substring(0, queryStartIndex);
				if (queryStartIndex < schemeSpecific.length())
					queryPart = schemeSpecific.substring(queryStartIndex + 1);
			}

			final Pattern positionPattern = Pattern.compile(
					"([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)");
			final Matcher positionMatcher = positionPattern.matcher(positionPart);
			if (!positionMatcher.find()) {
				return null;
			}
			double lat = Double.valueOf(positionMatcher.group(1));
			double lon = Double.valueOf(positionMatcher.group(2));

			int zoom = GeoParsedPoint.NO_ZOOM;
			String searchRequest = null;
			for (String param : queryPart.split("&")) {
				String paramName;
				String paramValue = null;
				int nameValueDelimititerIndex = param.indexOf('=');
				if (nameValueDelimititerIndex == -1) {
					paramName = param;
				} else {
					paramName = param.substring(0, nameValueDelimititerIndex);
					if (nameValueDelimititerIndex < param.length())
						paramValue = param.substring(nameValueDelimititerIndex + 1);
				}

				if ("z".equals(paramName) && paramValue != null) {
					zoom = Integer.parseInt(paramValue);
				} else if ("q".equals(paramName) && paramValue != null) {
					searchRequest = URLDecoder.decode(paramValue);
				}
			}

			if (searchRequest != null) {
				final Matcher positionInSearchRequestMatcher =
						positionPattern.matcher(searchRequest);
				if (lat == 0.0 && lon == 0.0 && positionInSearchRequestMatcher.find()) {
					lat = Double.valueOf(positionInSearchRequestMatcher.group(1));
					lon = Double.valueOf(positionInSearchRequestMatcher.group(2));
				}
			}

			if (lat == 0.0 && lon == 0.0 && searchRequest != null) {
				return new GeoParsedPoint(searchRequest);
			}

			if (zoom != GeoParsedPoint.NO_ZOOM) {
				return new GeoParsedPoint(lat, lon, zoom, name);
			}
			return new GeoParsedPoint(lat, lon, name);
		}
		return null;
	}

    private static int parseZoom(String zoom) {
        try {
            return Integer.valueOf(zoom);
        } catch (NumberFormatException e) {
            return GeoParsedPoint.NO_ZOOM;
        }
    }

	public static class GeoParsedPoint {
		private static final int NO_ZOOM = -1;

		private double lat;
		private double lon;
		private int zoom = NO_ZOOM;
		private String name;
		private String query;
		private boolean geoPoint;
		private boolean geoAddress;

		public GeoParsedPoint(double lat, double lon) {
			super();
			this.lat = lat;
			this.lon = lon;
			this.geoPoint = true;
		}

		public GeoParsedPoint(double lat, double lon, String name) {
			this(lat, lon);
			if (name != null)
				this.name = name.replaceAll("\\+", " ");
		}

		public GeoParsedPoint(double lat, double lon, int zoom) {
			this(lat, lon);
			this.zoom = zoom;
		}

		public GeoParsedPoint(double lat, double lon, int zoom, String name) {
			this(lat, lon, name);
			this.zoom = zoom;
		}

		public GeoParsedPoint(String latString, String lonString, String zoomString) throws NumberFormatException {
			this(Double.valueOf(latString), Double.valueOf(lonString));
			this.zoom = parseZoom(zoomString);
		}

		public GeoParsedPoint(String latString, String lonString) throws NumberFormatException {
			this(Double.valueOf(latString), Double.valueOf(lonString));
			this.zoom = NO_ZOOM;
		}

		public GeoParsedPoint(String query) {
			super();
			this.query = query;
			this.geoAddress = true;
		}

		public double getLatitude() {
			return lat;
		}

		public double getLongitude() {
			return lon;
		}

		public int getZoom() {
			return zoom;
		}

		public String getName() {
			return name;
		}

		public String getQuery() {
			return query;
		}

		public boolean isGeoPoint() {
			return geoPoint;
		}

		public boolean isGeoAddress() {
			return geoAddress;
		}

		@Override
		public String toString() {
			return isGeoPoint() ? "GeoParsedPoint [lat=" + lat + ", lon=" + lon + ", zoom=" + zoom + ", name=" + name
					+ "]" : "GeoParsedPoint [query=" + query;
		}

	}
}
