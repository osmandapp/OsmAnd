package net.osmand.util;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

		String noQueryParameters[] = {
				"geo:0,0",
				"geo:0,0?",
				"http://download.osmand.net/go",
				"http://download.osmand.net/go?",
		};
		for (String s : noQueryParameters) {
			URI uri = URI.create(s);
			Map<String, String> map = getQueryParameters(uri);
			System.out.print(s + " map: " + map.size() + "...");
			if (map.size() != 0) {
				System.out.println("");
				throw new RuntimeException("Map should be 0 but is " + map.size());
			}
			System.out.println(" Passed!");
		}

		String oneQueryParameter[] = {
				"geo:0,0?m",
				"geo:0,0?m=",
				"geo:0,0?m=foo",
				"geo:0,0?q=%D0%9D%D0",
				"http://download.osmand.net/go?lat",
				"http://download.osmand.net/go?lat=",
				"http://download.osmand.net/go?lat=34.99393",
		};
		for (String s : oneQueryParameter) {
			URI uri = URI.create(s);
			Map<String, String> map = getQueryParameters(uri);
			System.out.print(s + " map: " + map.size() + "...");
			if (map.size() != 1) {
				System.out.println("");
				throw new RuntimeException("Map should be 1 but is " + map.size());
			}
			System.out.println(" Passed!");
		}

		String twoQueryParameters[] = {
				"geo:0,0?z=11&q=Lots+Of+Stuff",
				"http://osmand.net/go?lat=34.99393&lon=-110.12345",
				"http://www.osmand.net/go.html?lat=34.99393&lon=-110.12345",
				"http://download.osmand.net/go?lat=34.99393&lon=-110.12345",
				"http://download.osmand.net/go?lat=34.99393&lon=-110.12345#this+should+be+ignored",
		};
		for (String s : twoQueryParameters) {
			URI uri = URI.create(s);
			Map<String, String> map = getQueryParameters(uri);
			System.out.print(s + " map: " + map.size() + "...");
			if (map.size() != 2) {
				System.out.println("");
				throw new RuntimeException("Map should be 2 but is " + map.size());
			}
			System.out.println(" Passed!");
		}

		
		
		// geo:34,-106
		url = "geo:" + ilat + "," + ilon;
		System.out.println("url: " + url);
		GeoParsedPoint actual = GeoPointParserUtil.parse(url);
		assertUrlEquals(url, actual.getGeoUriString());
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon));

		// google.navigation:q=34.99393,-106.61568
		url = "google.navigation:q=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon));
		
		// geo:34.99393,-106.61568
		url = "geo:" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertUrlEquals(url, actual.getGeoUriString());
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon));


		// geo:34.99393,-106.61568?z=11
		z = 11;
		url = "geo:" + dlat + "," + dlon + "?z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertUrlEquals(url, actual.getGeoUriString());
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

		// geo:34.99393,-106.61568?q=34.99393,-106.61568(Treasure+Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:" + dlat + "," + dlon + "?q=" + dlat + "," + dlon + "(" + URLEncoder.encode(name) + ")";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z, name));
		assertUrlEquals(url, actual.getGeoUriString());

		// 0,0?q=34,-106(Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:0,0?q=" + ilat + "," + ilon + "(" + name + ")";
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z, name));

		// 0,0?q=34.99393,-106.61568(Treasure Island)
		z = GeoParsedPoint.NO_ZOOM;
		url = "geo:0,0?q=" + dlat + "," + dlon + "(" + name + ")";
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
		url = "geo:0,0?q=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));
		assertUrlEquals(url, actual.getGeoUriString());

		// geo:0,0?q=760 West Genesee Street Syracuse NY 13204
		qstr = "760 West Genesee Street Syracuse NY 13204";
		url = "geo:0,0?q=" + qstr;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// geo:0,0?z=11&q=1600+Amphitheatre+Parkway,+CA
		qstr = "1600 Amphitheatre Parkway, CA";
		url = "geo:0,0?q=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));
		assertUrlEquals(url, actual.getGeoUriString());

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

		// geo:0,0?q=50.45%2C%2030.5233
		z = GeoParsedPoint.NO_ZOOM;
		qlat = 50.4513;
		qlon = 30.5699;

		url = "geo:0,0?q=" + qlat + "%2C%20" + qlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qlat, qlon, z, null));

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

		// http://maps.google.com/maps?q=N34.939,E-106
		url = "http://maps.google.com/maps?q=N" + dlat + ",E" + Math.abs(dlon);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, -Math.abs(dlon)));

		url = "http://maps.google.com/maps?f=d&saddr=" + dlat +"," +dlon +"&daddr=" +dlat +"," +dlon+"&hl=en"; 
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon));
		
		url = "http://maps.google.com/maps?f=d&saddr=My+Location&daddr=" +dlat +"," +dlon+"&hl=en"; 
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon));

		// http://www.osmand.net/go.html?lat=34&lon=-106&z=11
		url = "http://www.osmand.net/go.html?lat=" + ilat + "&lon=" + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://www.osmand.net/go.html?lat=34.99393&lon=-106.61568&z=11
		url = "http://www.osmand.net/go.html?lat=" + dlat + "&lon=" + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://osmand.net/go?lat=34&lon=-106&z=11
		url = "http://osmand.net/go?lat=" + ilat + "&lon=" + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://osmand.net/go?lat=34.99393&lon=-106.61568&z=11
		url = "http://osmand.net/go?lat=" + dlat + "&lon=" + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://openstreetmap.org/#map=11/34/-106
		z = 11;
		url = "https://openstreetmap.org/#map=" + z + "/" + ilat + "/" + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// https://openstreetmap.org/#map=11/34.99393/-106.61568
		url = "https://openstreetmap.org/#map=" + z + "/" + dlat + "/" + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://openstreetmap.org/#11/34.99393/-106.61568
		url = "https://openstreetmap.org/#" + z + "/" + dlat + "/" + dlon;
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

		// https://osm.org/go/TyFSutZ-?m=
		// https://www.openstreetmap.org/?mlat=34.993933029174805&mlon=-106.61568069458008#map=15/34.99393/-106.61568
		z = 15;
		url = "https://osm.org/go/TyFYuF6P--?m=";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertApproximateGeoPoint(actual, new GeoParsedPoint(longLat, longLon, z));

		// https://osm.org/go/TyFS--
		// https://www.openstreetmap.org/#map=3/34.99/-106.70
		z = 3;
		url = "https://osm.org/go/TyFS--";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertApproximateGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://osm.org/go/TyFYuF6P~~-?m // current shortlink format with "~"
		// https://www.openstreetmap.org/?mlat=34.993933029174805&mlon=-106.61568069458008#map=15/34.99393/-106.61568
		z = 20;
		url = "https://osm.org/go/TyFYuF6P~~-?m";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertApproximateGeoPoint(actual, new GeoParsedPoint(longLat, longLon, z));

		// https://osm.org/go/TyFYuF6P@@--?m= // old, deprecated shortlink format with "@"
		// https://www.openstreetmap.org/?mlat=34.993933029174805&mlon=-106.61568069458008#map=15/34.99393/-106.61568
		z = 21;
		url = "https://osm.org/go/TyFYuF6P@@--?m=";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertApproximateGeoPoint(actual, new GeoParsedPoint(longLat, longLon, z));

		// https://openstreetmap.de/zoom=11&lat=34&lon=-106
		z = 11;
		url = "https://openstreetmap.de/zoom=" + z + "&lat=" + ilat + "&lon=" + ilon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// https://openstreetmap.de/zoom=11&lat=34.99393&lon=-106.61568
		url = "https://openstreetmap.de/zoom=" + z + "&lat=" + dlat + "&lon=" + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://openstreetmap.de/lat=34.99393&lon=-106.61568&zoom=11
		url = "https://openstreetmap.de/lat=" + dlat + "&lon=" + dlon + "&zoom=" + z;
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
		
		// https://maps.google.com/maps?q=loc:-21.8835112,-47.7838932 (Name)
		url = "https://maps.google.com/maps?q=loc:" + dlat + "," + dlon + " (Name)" ;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon));

		// http://maps.google.com/maps/ll=34.99393,-106.61568,z=11
		url = "http://maps.google.com/maps/ll=" + dlat + "," + dlon + ",z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com/maps/?q=loc:34,-106&z=11
		url = "http://www.google.com/maps/?q=loc:" + ilat + "," + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://www.google.com/maps/?q=loc:34.99393,-106.61568&z=11
		url = "http://www.google.com/maps/?q=loc:" + dlat + "," + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://www.google.com/maps/preview#!q=paris&data=!4m15!2m14!1m13!1s0x47e66e1f06e2b70f%3A0x40b82c3688c9460!3m8!1m3!1d24383582!2d-95.677068!3d37.0625!3m2!1i1222!2i718!4f13.1!4m2!3d48.856614!4d2.3522219
		url = "https://www.google.com/maps/preview#!q=paris&data=!4m15!2m14!1m13!1s0x47e66e1f06e2b70f%3A0x40b82c3688c9460!3m8!1m3!1d24383582!2d-95.677068!3d37.0625!3m2!1i1222!2i718!4f13.1!4m2!3d48.856614!4d2.3522219";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint("paris"));

		// TODO this URL does not work, where is it used?
		// http://maps.google.com/maps/q=loc:34,-106&z=11
		url = "http://maps.google.com/maps/q=loc:" + ilat + "," + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// TODO this URL does not work, where is it used?
		// http://maps.google.com/maps/q=loc:34.99393,-106.61568&z=11
		url = "http://maps.google.com/maps/q=loc:" + dlat + "," + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// TODO this URL does not work, where is it used?
		// whatsapp
		// http://maps.google.com/maps/q=loc:34,-106 (You)
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps/q=loc:" + ilat + "," + ilon + " (You)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// TODO this URL does not work, where is it used?
		// whatsapp
		// http://maps.google.com/maps/q=loc:34.99393,-106.61568 (You)
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps/q=loc:" + dlat + "," + dlon + " (You)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// whatsapp
		// https://maps.google.com/maps?q=loc:34.99393,-106.61568 (You)
		z = GeoParsedPoint.NO_ZOOM;
		url = "https://maps.google.com/maps?q=loc:" + dlat + "," + dlon + " (You)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// whatsapp
		// https://maps.google.com/maps?q=loc:34.99393,-106.61568 (USER NAME)
		z = GeoParsedPoint.NO_ZOOM;
		url = "https://maps.google.com/maps?q=loc:" + dlat + "," + dlon + " (USER NAME)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// whatsapp
		// https://maps.google.com/maps?q=loc:34.99393,-106.61568 (USER NAME)
		z = GeoParsedPoint.NO_ZOOM;
		url = "https://maps.google.com/maps?q=loc:" + dlat + "," + dlon + " (+55 99 99999-9999)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// whatsapp
		// https://www.google.com/maps/search/34.99393,-106.61568/data=!4m4!2m3!3m1!2s-23.2776,-45.8443128!4b1
		url = "https://maps.google.com/maps?q=loc:" + dlat + "," + dlon + "/data=!4m4!2m3!3m1!2s-23.2776,-45.8443128!4b1";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com/maps/search/food/34,-106,14z
		url = "http://www.google.com/maps/search/food/" + ilat + "," + ilon + "," + z + "z";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// http://www.google.com/maps/search/food/34.99393,-106.61568,14z
		url = "http://www.google.com/maps/search/food/" + dlat + "," + dlon + "," + z + "z";
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

		// http://maps.google.co.uk/?q=34.99393,-106.61568
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.co.uk/?q=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com.tr/maps?q=34.99393,-106.61568
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://www.google.com.tr/maps?q=" + dlat + "," + dlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps?lci=com.google.latitudepublicupdates&ll=34.99393%2C-106.61568&q=34.99393%2C-106.61568
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps?lci=com.google.latitudepublicupdates&ll=" + dlat
				+ "%2C" + dlon + "&q=" + dlat + "%2C" + dlon + "((" + dlat + "%2C%20" + dlon + "))";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://www.google.com/maps/place/34%C2%B059'38.1%22N+106%C2%B036'56.5%22W/@34.99393,-106.61568,17z/data=!3m1!4b1!4m2!3m1!1s0x0:0x0
		z = 17;
		url = "https://www.google.com/maps/place/34%C2%B059'38.1%22N+106%C2%B036'56.5%22W/@" + dlat + "," + dlon + "," + z + "z/data=!3m1!4b1!4m2!3m1!1s0x0:0x0";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://www.google.com/maps/place/760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760 West Genesee Street Syracuse NY 13204";
		url = "http://www.google.com/maps/place/" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://maps.google.com/maps?q=760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760 West Genesee Street Syracuse NY 13204";
		url = "http://www.google.com/maps?q=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://maps.google.com/maps?daddr=760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760 West Genesee Street Syracuse NY 13204";
		url = "http://www.google.com/maps?daddr=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://www.google.com/maps/dir/Current+Location/760+West+Genesee+Street+Syracuse+NY+13204
		qstr = "760 West Genesee Street Syracuse NY 13204";
		url = "http://www.google.com/maps/dir/Current+Location/" + URLEncoder.encode(qstr);
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
		int latint = ((int) (dlat * 100000));
		int lonint = ((int) (dlon * 100000));
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

		// http://share.here.com/l/52.5134272,13.3778416,Hannah-Arendt-Stra%C3%9Fe?z=16.0&t=normal
		url = "http://share.here.com/l/" + dlat + "," + dlon + ",Hannah-Arendt-Stra%C3%9Fe?z=" + z + "&t=normal";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://www.here.com/location?map=52.5134272,13.3778416,16,normal&msg=Hannah-Arendt-Stra%C3%9Fe
		z = 16;
		url = "https://www.here.com/location?map=" + dlat + "," + dlon + "," + z + ",normal&msg=Hannah-Arendt-Stra%C3%9Fe";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// https://www.here.com/?map=48.23145,16.38454,15,normal
		z = 16;
		url = "https://www.here.com/?map=" + dlat + "," + dlon + "," + z + ",normal";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://map.wap.qq.com/loc/detail.jsp?sid=AU8f3ck87L6XDmytunBm4iWg&g_ut=2&city=%E5%8C%97%E4%BA%AC&key=NOBU%20Beijing&x=116.48177&y=39.91082&md=10461366113386140862
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://map.wap.qq.com/loc/detail.jsp?sid=AU8f3ck87L6XDmytunBm4iWg&g_ut=2&city=%E5%8C%97%E4%BA%AC&key=NOBU%20Beijing&x=" + dlon + "&y=" + dlat + "&md=10461366113386140862";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://map.qq.com/AppBox/print/?t=&c=%7B%22base%22%3A%7B%22l%22%3A11%2C%22lat%22%3A39.90403%2C%22lng%22%3A116.407526%7D%7D
		z = 11;
		url = "http://map.qq.com/AppBox/print/?t=&c=%7B%22base%22%3A%7B%22l%22%3A11%2C%22lat%22%3A" + dlat + "%2C%22lng%22%3A" + dlon + "%7D%7D";
		System.out.println("url: " + url);
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
		};

		for (String u : urls) {
			System.out.println("url: " + u);
			actual = GeoPointParserUtil.parse(u);
			if (actual == null)
				throw new RuntimeException(u + " not parsable!");
			System.out.println("Properly parsed as: " + actual.getGeoUriString());
		}

		// these URLs are not parsable, but should not crash or cause problems
		String[] unparsableUrls = {
				"http://maps.yandex.ru/-/CVCw6M9g",
				"http://maps.yandex.com/-/CVCXEKYW",
				"http://goo.gl/maps/Cji0V",
				"http://amap.com/0F0i02",
				"http://j.map.baidu.com/oXrVz",
				"http://l.map.qq.com/9741483212?m",
				"http://map.qq.com/?l=261496722",
				"http://her.is/vLCEXE",
		};

		for (String u : unparsableUrls) {
			System.out.println("url: " + u);
			actual = GeoPointParserUtil.parse(u);
			if (actual != null)
				throw new RuntimeException(u + " not parsable, but parse did not return null!");
			System.out.println("Handled URL");
		}


	}

	private static boolean areCloseEnough(double a, double b, long howClose) {
		long aRounded = (long) Math.round(a * Math.pow(10, howClose));
		long bRounded = (long) Math.round(b * Math.pow(10, howClose));
		return aRounded == bRounded;
	}

	private static void assertGeoPoint(GeoParsedPoint actual, GeoParsedPoint expected) {
		if (expected.getQuery() != null) {
			if (!expected.getQuery().equals(actual.getQuery()))
				throw new RuntimeException("Query param not equal:\n'" +
						actual.getQuery() + "' != '" + expected.getQuery());
		} else {
			double aLat = actual.getLatitude(), eLat = expected.getLatitude(), aLon = actual.getLongitude(), eLon = expected.getLongitude();
			int aZoom = actual.getZoom(), eZoom = expected.getZoom();
			String aLabel = actual.getLabel(), eLabel = expected.getLabel();
			if (eLabel != null) {
				if (!aLabel.equals(eLabel)) {
					throw new RuntimeException("Point label is not equal; actual="
							+ aLabel + ", expected=" + eLabel);
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
		System.out.println("Passed: " + actual);
	}

	private static void assertApproximateGeoPoint(GeoParsedPoint actual, GeoParsedPoint expected) {
		if (expected.getQuery() != null) {
			if (!expected.getQuery().equals(actual.getQuery()))
				throw new RuntimeException("Query param not equal");
		} else {
			double aLat = actual.getLatitude(), eLat = expected.getLatitude(), aLon = actual.getLongitude(), eLon = expected.getLongitude();
			int aZoom = actual.getZoom(), eZoom = expected.getZoom();
			String aLabel = actual.getLabel(), eLabel = expected.getLabel();
			if (eLabel != null) {
				if (!aLabel.equals(eLabel)) {
					throw new RuntimeException("Point label is not equal; actual="
							+ aLabel + ", expected=" + eLabel);
				}
			}
			if (((int) eLat) != ((int) aLat)) {
				throw new RuntimeException("Latitude is not equal; actual=" + aLat + ", expected=" + eLat);
			}
			if (((int) eLon) != ((int) aLon)) {
				throw new RuntimeException("Longitude is not equal; actual=" + aLon + ", expected=" + eLon);
			}
			if (eZoom != aZoom) {
				throw new RuntimeException("Zoom is not equal; actual=" + aZoom + ", expected=" + eZoom);
			}
		}
		System.out.println("Passed!");
	}

	private static void assertUrlEquals(String actual, String expected) {
		if (actual == null || !actual.equals(expected))
			throw new RuntimeException("URLs not equal; actual=" + actual + ", expected=" + expected);
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
	 * This parses out all of the parameters in the query string for both
	 * http: and geo: URIs.  This will only work on URIs with valid syntax, so
	 * it will not work on URIs that do odd things like have a query string in
	 * the fragment, like this one:
	 * http://www.amap.com/#!poi!!q=38.174596,114.995033|2|%E5%AE%BE%E9%A6%86&radius=1000
	 *
	 * @param uri
	 * @return {@link Map<String, String>} a Map of the query parameters
	 */
	private static Map<String, String> getQueryParameters(URI uri) {
		String query = null;
		if (uri.isOpaque()) {
			String schemeSpecificPart = uri.getSchemeSpecificPart();
			int pos = schemeSpecificPart.indexOf("?");
			if (pos == schemeSpecificPart.length()) {
				query = "";
			} else if (pos > -1) {
				query = schemeSpecificPart.substring(pos + 1);
			}
		} else {
			query = uri.getRawQuery();
		}
		return getQueryParameters(query);
	}

	private static Map<String, String> getQueryParameters(String query) {
		final LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		if (query != null && !query.equals("")) {
			String[] params = query.split("[&/]");
			for (String p : params) {
				String[] keyValue = p.split("=");
				if (keyValue.length == 1)
					map.put(keyValue[0], "");
				else if (keyValue.length > 1)
					map.put(keyValue[0], URLDecoder.decode(keyValue[1]));
			}
		}
		return map;
	}

	/**
	 * Parses geo and map intents:
	 *
	 * @param uriString The URI as a String
	 * @return {@link GeoParsedPoint}
	 */
	public static GeoParsedPoint parse(final String uriString) {
		URI uri;
		try {
			// amap.com uses | in their URLs, which is an illegal character for a URL
			uri = URI.create(uriString.replaceAll("\\s+", "+")
					.replaceAll("%20", "+")
					.replaceAll("%2C", ",")
					.replaceAll("\\|", ";")
					.replaceAll("\\(\\(\\S+\\)\\)", ""));
		} catch (IllegalArgumentException e) {
			return null;
		}

		String scheme = uri.getScheme();
		if (scheme == null)
			return null;
		else
			scheme = scheme.toLowerCase(Locale.US);

		if ("http".equals(scheme) || "https".equals(scheme) || "google.navigation".equals(scheme)) {
			String host = uri.getHost();
			Map<String, String> params = getQueryParameters(uri);
			if("google.navigation".equals(scheme)) {
				host = scheme;
				if(uri.getSchemeSpecificPart() == null) {
					return null;
				} else if(!uri.getSchemeSpecificPart().contains("=")) {
					params = getQueryParameters("q="+uri.getSchemeSpecificPart());	
				} else {
					params = getQueryParameters(uri.getSchemeSpecificPart());
				}
			} else if (host == null) {
				return null;
			} else {
				host = host.toLowerCase(Locale.US);
			}
			String path = uri.getPath();
			if (path == null) {
				path = "";
			}
			String fragment = uri.getFragment();

			// lat-double, lon - double, zoom or z - int
			Set<String> simpleDomains = new HashSet<String>();
			simpleDomains.add("osmand.net");
			simpleDomains.add("www.osmand.net");
			simpleDomains.add("download.osmand.net");
			simpleDomains.add("openstreetmap.de");
			simpleDomains.add("www.openstreetmap.de");


			final Pattern commaSeparatedPairPattern = Pattern.compile("(?:loc:)?([N|S]?[+-]?\\d+(?:\\.\\d+)?),([E|W]?[+-]?\\d+(?:\\.\\d+)?)");

			try {
				if (host.equals("osm.org") || host.endsWith("openstreetmap.org")) {
					Pattern p;
					Matcher matcher;
					if (path.startsWith("/go/")) { // short URL form
						p = Pattern.compile("^/go/([A-Za-z0-9_@~]+-*)(?:.*)");
						matcher = p.matcher(path);
						if (matcher.matches()) {
							return MapUtils.decodeShortLinkString(matcher.group(1));
						}
					} else { // data in the query and/or feature strings
						double lat = 0;
						double lon = 0;
						int zoom = GeoParsedPoint.NO_ZOOM;
						if (fragment != null) {
							if (fragment.startsWith("map=")) {
								fragment = fragment.substring("map=".length());
							}
							String[] vls = fragment.split("/|&"); //"&" to split off trailing extra parameters
							if (vls.length >= 3) {
								zoom = parseZoom(vls[0]);
								lat = parseSilentDouble(vls[1]);
								lon = parseSilentDouble(vls[2]);
							}
						}
						// the query string sometimes has higher resolution values
						String mlat = getQueryParameter("mlat", uri);
						if (mlat != null) {
							lat = parseSilentDouble(mlat);
						}
						String mlon = getQueryParameter("mlon", uri);
						if (mlon != null) {
							lon = parseSilentDouble(mlon);
						}
						return new GeoParsedPoint(lat, lon, zoom);
					}
				} else if (host.startsWith("map.baidu.")) { // .com and .cn both work
					/* Baidu Map uses a custom format for lat/lon., it is basically standard lat/lon
                     * multiplied by 100,000, then rounded to an integer */
					String zm = params.get("l");
					String[] vls = silentSplit(params.get("c"), ",");
					if (vls != null && vls.length >= 2) {
						double lat = parseSilentInt(vls[0]) / 100000.;
						double lon = parseSilentInt(vls[1]) / 100000.;
						int zoom = parseZoom(zm);
						return new GeoParsedPoint(lat, lon, zoom);
					}
				} else if (simpleDomains.contains(host)) {
					if (uri.getQuery() == null && params.size() == 0) {
						// DOUBLE check this may be wrong test of openstreetmap.de (looks very weird url and server doesn't respond)
						params = getQueryParameters(path.substring(1));
					}
					if (params.containsKey("lat") && params.containsKey("lon")) {
						final double lat = parseSilentDouble(params.get("lat"));
						final double lon = parseSilentDouble(params.get("lon"));
						int zoom = GeoParsedPoint.NO_ZOOM;
						if (params.containsKey("z")) {
							zoom = parseZoom(params.get("z"));
						} else if (params.containsKey("zoom")) {
							zoom = parseZoom(params.get("zoom"));
						}
						return new GeoParsedPoint(lat, lon, zoom);
					}
				} else if (host.matches("(?:www\\.)?(?:maps\\.)?yandex\\.[a-z]+")) {
					String ll = params.get("ll");
					if (ll != null) {
						Matcher matcher = commaSeparatedPairPattern.matcher(ll);
						if (matcher.matches()) {
							String z = String.valueOf(parseZoom(params.get("z")));
							return new GeoParsedPoint(matcher.group(1), matcher.group(2), z, params.get("text"));
						}
					}
				} else if (host.matches("(?:www\\.)?(?:maps\\.)?google\\.[a-z.]+")) {
					String latString = null;
					String lonString = null;
					String z = String.valueOf(GeoParsedPoint.NO_ZOOM);
					
					if (params.containsKey("q")) {
						System.out.println("q=" + params.get("q"));
						Matcher matcher = commaSeparatedPairPattern.matcher(params.get("q"));
						if (matcher.matches()) {
							latString = matcher.group(1);
							lonString = matcher.group(2);
						}
					} else if (params.containsKey("ll")) {
						Matcher matcher = commaSeparatedPairPattern.matcher(params.get("ll"));
						if (matcher.matches()) {
							latString = matcher.group(1);
							lonString = matcher.group(2);
						}
					}
					if (latString != null && lonString != null) {
						if (params.containsKey("z")) {
							z = params.get("z");
						}
						return new GeoParsedPoint(latString, lonString, z);
					}
					if (params.containsKey("daddr")) {
						return parseGoogleMapsPath(params.get("daddr"), params);
					} else if (params.containsKey("saddr")) {
						return parseGoogleMapsPath(params.get("saddr"), params);
					} else if (params.containsKey("q")) {
						String opath = params.get("q");
						final String pref = "loc:";
						if(opath.contains(pref)) {
							opath = opath.substring(opath.lastIndexOf(pref) + pref.length());
						}
						final String postf = "\\s\\((\\p{L}|\\p{M}|\\p{Z}|\\p{S}|\\p{N}|\\p{P}|\\p{C})*\\)$";
						opath = opath.replaceAll(postf, "");
						System.out.println("opath=" + opath);
						return parseGoogleMapsPath(opath, params);
					}
					if (fragment != null) {
						Pattern p = Pattern.compile(".*[!&]q=([^&!]+).*");
						Matcher m = p.matcher(fragment);
						if (m.matches()) {
							return new GeoParsedPoint(m.group(1));
						}
					}

					String[] pathPrefixes = new String[]{"/@", "/ll=",
							"loc:", "/"};
					for (String pref : pathPrefixes) {
						if (path.contains(pref)) {
							path = path.substring(path.lastIndexOf(pref) + pref.length());
							return parseGoogleMapsPath(path, params);
						}
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
							".*p=(?:[A-Z0-9]+),([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?).*",};
					for (int i = 0; i < patterns.length; i++) {
						p = Pattern.compile(patterns[i]);
						matcher = p.matcher(subString);
						if (matcher.matches()) {
							if (matcher.groupCount() == 3) {
								// amap uses radius in meters, so do rough conversion into zoom level
								float radius = Float.valueOf(matcher.group(3));
								long zoom = Math.round(23. - Math.log(radius) / Math.log(2.0));
								return new GeoParsedPoint(matcher.group(1), matcher.group(2), String.valueOf(zoom));
							} else if (matcher.groupCount() == 2) {
								return new GeoParsedPoint(matcher.group(1), matcher.group(2));
							}
						}
					}
				} else if (host.equals("here.com") || host.endsWith(".here.com")) { // www.here.com, share.here.com, here.com 
					String z = String.valueOf(GeoParsedPoint.NO_ZOOM);
					String label = null;
					if (params.containsKey("msg")) {
						label = params.get("msg");
					}
					if (params.containsKey("z")) {
						z = params.get("z");
					}
					if (params.containsKey("map")) {
						String[] mapArray = params.get("map").split(",");
						if (mapArray.length > 2) {
							return new GeoParsedPoint(mapArray[0], mapArray[1], mapArray[2], label);
						} else if (mapArray.length > 1) {
							return new GeoParsedPoint(mapArray[0], mapArray[1], z, label);
						}
					}
					if (path.startsWith("/l/")) {
						Pattern p = Pattern.compile("^/l/([+-]?\\d+(?:\\.\\d+)),([+-]?\\d+(?:\\.\\d+)),(.*)");
						Matcher matcher = p.matcher(path);
						if (matcher.matches()) {
							return new GeoParsedPoint(matcher.group(1), matcher.group(2), z, matcher.group(3));
						}
					}
				} else if (host.endsWith(".qq.com")) {
					String x = null;
					String y = null;
					String z = String.valueOf(GeoParsedPoint.NO_ZOOM);
					String label = null;
					if (params.containsKey("city")) {
						label = params.get("city");
					} else if (params.containsKey("key")) {
						label = params.get("key");
					} else if (params.containsKey("a")) {
						label = params.get("a");
					} else if (params.containsKey("n")) {
						label = params.get("n");
					}
					String m = params.get("m");
					if (m != null) {
						Matcher matcher = commaSeparatedPairPattern.matcher(m);
						if (matcher.matches()) {
							x = matcher.group(2);
							y = matcher.group(1);
						}
					}
					String c = params.get("c");
					if (c != null) {
						// there are two different patterns of data that can be in ?c=
						Matcher matcher = commaSeparatedPairPattern.matcher(c);
						if (matcher.matches()) {
							x = matcher.group(2);
							y = matcher.group(1);
						} else {
							x = c.replaceAll(".*\"lng\":\\s*([+\\-]?[0-9.]+).*", "$1");
							if (x == null) // try 'lon' for the second time
								x = c.replaceAll(".*\"lon\":\\s*([+\\-]?[0-9.]+).*", "$1");
							y = c.replaceAll(".*\"lat\":\\s*([+\\-]?[0-9.]+).*", "$1");
							z = c.replaceAll(".*\"l\":\\s*([+-]?[0-9.]+).*", "$1");
							return new GeoParsedPoint(y, x, z, label);
						}
					}
					for (String key : new String[]{"centerX", "x", "x1", "x2"}) {
						if (params.containsKey(key)) {
							x = params.get(key);
							break;
						}
					}
					for (String key : new String[]{"centerY", "y", "y1", "y2"}) {
						if (params.containsKey(key)) {
							y = params.get(key);
							break;
						}
					}
					if (x != null && y != null)
						return new GeoParsedPoint(y, x, z, label);
				} else if (host.equals("maps.apple.com")) {
					// https://developer.apple.com/library/iad/featuredarticles/iPhoneURLScheme_Reference/MapLinks/MapLinks.html
					String z = String.valueOf(GeoParsedPoint.NO_ZOOM);
					String label = null;
					if (params.containsKey("q")) {
						label = params.get("q");
					}
					if (params.containsKey("near")) {
						label = params.get("near");
					}
					if (params.containsKey("z")) {
						z = params.get("z");
					}
					String ll = params.get("ll");
					if (ll != null) {
						Matcher matcher = commaSeparatedPairPattern.matcher(ll);
						if (matcher.matches()) {
							return new GeoParsedPoint(matcher.group(1), matcher.group(2), z, label);
						}
					}
					String sll = params.get("sll");
					if (sll != null) {
						Matcher matcher = commaSeparatedPairPattern.matcher(sll);
						if (matcher.matches()) {
							return new GeoParsedPoint(matcher.group(1), matcher.group(2), z, label);
						}
					}
					// if no ll= or sll=, then just use the q string
					if (params.containsKey("q")) {
						return new GeoParsedPoint(params.get("q"));
					}
					// if no q=, then just use the destination address
					if (params.containsKey("daddr")) {
						return new GeoParsedPoint(params.get("daddr"));
					}
					// if no daddr=, then just use the source address
					if (params.containsKey("saddr")) {
						return new GeoParsedPoint(params.get("saddr"));
					}
				}

			} catch (RuntimeException e) {
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
					"([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)");
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

	private static GeoParsedPoint parseGoogleMapsPath(String opath, Map<String, String> params) {
		String zmPart = "";
		String descr = "";
		String path = opath;
		if (path.contains("&")) {
			String[] vls = path.split("&");
			path = vls[0];
			for (int i = 1; i < vls.length; i++) {
				int ik = vls[i].indexOf('=');
				if (ik > 0) {
					params.put(vls[i].substring(0, ik), vls[i].substring(ik + 1));
				}
			}
		}
		if (path.contains("+")) {
			path = path.substring(0, path.indexOf("+"));
			descr = path.substring(path.indexOf("+") + 1);
			if (descr.contains(")")) {
				descr = descr.substring(0, descr.indexOf(")"));
			}

		}
		if (params.containsKey("z")) {
			zmPart = params.get("z");
		}
		String[] vls = silentSplit(path, ",");

		if (vls.length >= 2) {
			double lat = parseSilentDouble(vls[0]);
			double lon = parseSilentDouble(vls[1]);
			int zoom = GeoParsedPoint.NO_ZOOM;
			if (vls.length >= 3 || zmPart.length() > 0) {
				if (zmPart.length() == 0) {
					zmPart = vls[2];
				}
				if (zmPart.startsWith("z=")) {
					zmPart = zmPart.substring(2);
				} else if (zmPart.contains("z")) {
					zmPart = zmPart.substring(0, zmPart.indexOf('z'));
				}
				zoom = parseZoom(zmPart);
			}
			return new GeoParsedPoint(lat, lon, zoom);
		}
		return new GeoParsedPoint(URLDecoder.decode(opath));
	}

	private static String[] silentSplit(String vl, String split) {
		if (vl == null) {
			return null;
		}
		return vl.split(split);
	}

	private static int parseZoom(String zoom) {
		try {
			if (zoom != null) {
				return Integer.valueOf(zoom);
			}
		} catch (NumberFormatException e) {
		}
		return GeoParsedPoint.NO_ZOOM;
	}

	private static double parseSilentDouble(String zoom) {
		try {
			if (zoom != null) {
				return Double.valueOf(zoom);
			}
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	private static int parseSilentInt(String zoom) {
		try {
			if (zoom != null) {
				return Integer.valueOf(zoom);
			}
		} catch (NumberFormatException e) {
		}
		return 0;
	}

	public static class GeoParsedPoint {
		public static final int NO_ZOOM = -1;

		private double lat = 0;
		private double lon = 0;
		private int zoom = NO_ZOOM;
		private String label;
		private String query;
		private boolean geoPoint;
		private boolean geoAddress;

		public GeoParsedPoint(double lat, double lon) {
			super();
			this.lat = lat;
			this.lon = lon;
			this.geoPoint = true;
		}

		public GeoParsedPoint(double lat, double lon, String label) {
			this(lat, lon);
			if (label != null)
				this.label = label.replaceAll("\\+", " ");
		}

		public GeoParsedPoint(double lat, double lon, int zoom) {
			this(lat, lon);
			this.zoom = zoom;
		}

		public GeoParsedPoint(double lat, double lon, int zoom, String label) {
			this(lat, lon, label);
			this.zoom = zoom;
		}

		public GeoParsedPoint(String latString, String lonString, String zoomString, String label) throws NumberFormatException {
			this(latString, lonString, zoomString);
			this.label = label;
		}

		public GeoParsedPoint(String latString, String lonString, String zoomString) throws NumberFormatException {
			this(parseLat(latString), parseLon(lonString));
			this.zoom = parseZoom(zoomString);
		}

		private static double parseLon(String lonString) {
			if (lonString.startsWith("E")) {
				return -Double.valueOf(lonString.substring(1));
			} else if (lonString.startsWith("W")) {
				return Double.valueOf(lonString.substring(1));
			}
			return Double.valueOf(lonString);
		}

		private static double parseLat(String latString) {
			if (latString.startsWith("S")) {
				return -Double.valueOf(latString.substring(1));
			} else if (latString.startsWith("N")) {
				return Double.valueOf(latString.substring(1));
			}
			return Double.valueOf(latString);
		}

		public GeoParsedPoint(String latString, String lonString) throws NumberFormatException {
			this(parseLat(latString), parseLon(lonString));
			this.zoom = NO_ZOOM;
		}

		/**
		 * Accepts a plain {@code String}, not URL-encoded
		 */
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

		public String getLabel() {
			return label;
		}

		public String getQuery() {
			return query;
		}

		public boolean isGeoPoint() {
			return geoPoint;
		}

		private String formatDouble(double d) {
			if (d == (long) d)
				return String.format(Locale.ENGLISH, "%d", (long) d);
			else
				return String.format("%s", d);
		}

		public boolean isGeoAddress() {
			return geoAddress;
		}

		/**
		 * Generates a URI string according to https://tools.ietf.org/html/rfc5870 and
		 * https://developer.android.com/guide/components/intents-common.html#Maps
		 */
		public String getGeoUriString() {
			String uriString;
			if (isGeoPoint()) {
				String latlon = formatDouble(lat) + "," + formatDouble(lon);
				uriString = "geo:" + latlon;
				LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
				if (zoom != NO_ZOOM)
					map.put("z", String.valueOf(zoom));
				if (query != null)
					map.put("q", URLEncoder.encode(query));
				if (label != null)
					if (query == null)
						map.put("q", latlon + "(" + URLEncoder.encode(label) + ")");
				if (map.size() > 0)
					uriString += "?";
				int i = 0;
				for (String key : map.keySet()) {
					if (i > 0)
						uriString += "&";
					uriString += key + "=" + map.get(key);
					i++;
				}
				return uriString;
			}
			if (isGeoAddress()) {
				uriString = "geo:0,0";
				if (query != null) {
					uriString += "?";
					if (zoom != NO_ZOOM)
						uriString += "z=" + zoom + "&";
					uriString += "q=" + URLEncoder.encode(query);
				}
				return uriString;
			}
			return null;
		}

		@Override
		public String toString() {
			return isGeoPoint() ? "GeoParsedPoint [lat=" + lat + ", lon=" + lon + ", zoom=" + zoom
					+ ", label=" + label + "]" : "GeoParsedPoint [query=" + query;
		}
	}
}
