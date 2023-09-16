package net.osmand.util;


import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class GeoPointParserUtilTest {

	@Test
	public void testGeoPointUrlDecode() {
		// bug in get scheme getSchemeSpecificPart()
		// equal results for : URI.create("geo:0,0?q=86HJV99P+29") && URI.create("geo:0,0?q=86HJV99P%2B29");
		GeoParsedPoint test = GeoPointParserUtil.parse("geo:0,0?q=86HJV99P%2B29");
		Assert.assertEquals(test.getQuery(), "86HJV99P+29");
	}

	@Test
	public void testGoogleMaps() {
		// https://www.google.com/maps?daddr=Bahnhofplatz+3,+7000+Chur@46.853582,9.529903
		GeoParsedPoint actual = GeoPointParserUtil.parse(
				"https://www.google.com/maps?daddr=Bahnhofplatz+3,+7000+Chur");
		assertGeoPoint(actual, new GeoParsedPoint("Bahnhofplatz 3, 7000 Chur"));

		actual = GeoPointParserUtil.parse(
				"https://www.google.com/maps?daddr=Bahnhofplatz+3,+7000+Chur@46.853582,9.529903");
		System.out.println(actual);
		assertGeoPoint(actual, new GeoParsedPoint(46.853582, 9.529903));
	}
	
	@Test
	public void testGoogleMapsData() {
		// https://www.google.com/maps?daddr=Bahnhofplatz+3,+7000+Chur@46.853582,9.529903
		GeoParsedPoint actual = GeoPointParserUtil.parse(
				"https://www.google.co.in/maps/place/10%C2%B007'16.8%22N+76%C2%B020'54.2%22E/@10.1213253,76.3478427,247m/data=!3m2!1e3!4b1!4m6!3m5!1s0x0:0x0!7e2!8m2!3d10.1213237!4d76.348392?shorturl=1");
		assertGeoPoint(actual, new GeoParsedPoint(10.1213237, 76.348392));
		actual = GeoPointParserUtil.parse(
				"https://www.google.co.in/maps/place/data=!3m2!1e3!4b1!4m6!3m5!1s0x0:0x0!7e2!8m2!3d10.1213237!4d76.348392?shorturl=1");
		assertGeoPoint(actual, new GeoParsedPoint(10.1213237, 76.348392));
	}
	
	@Test
	public void testMapsMeParser() {
		GeoParsedPoint actual = GeoPointParserUtil.parse(
				"http://ge0.me/44TvlEGXf-/Kyiv");
		assertGeoPoint(actual, new GeoParsedPoint(50.45003, 30.52414, 18, "Kyiv"));
	}

	@Test
	public void testGeoPoint() {
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
			Map<String, String> map = GeoPointParserUtil.getQueryParameters(uri);
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
			Map<String, String> map = GeoPointParserUtil.getQueryParameters(uri);
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
			Map<String, String> map = GeoPointParserUtil.getQueryParameters(uri);
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

		// geo:?q=Paris
		qstr = "Paris";
		url = "geo:?q=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));
		assertUrlEquals("geo:0,0?q=" + URLEncoder.encode(qstr), actual.getGeoUriString());

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

		url = "http://www.openstreetmap.org/search?query=" + qlat + "%2C" + qlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qlat, qlon));

		url = "http://www.openstreetmap.org/search?query=" + qlat + "%20" + qlon;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qlat, qlon));

		// http://download.osmand.net/go?lat=34.99393&lon=-106.61568&z=11
		url = "http://download.osmand.net/go?lat=" + dlat + "&lon=" + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// http://maps.google.com/maps?q=N34.939,W106
		url = "http://maps.google.com/maps?q=N" + dlat + ",W" + Math.abs(dlon);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon));

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

		// LEGACY this URL does not work, where is it used?
		// http://maps.google.com/maps/q=loc:34,-106&z=11
		url = "http://maps.google.com/maps/q=loc:" + ilat + "," + ilon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// LEGACY this URL does not work, where is it used?
		// http://maps.google.com/maps/q=loc:34.99393,-106.61568&z=11
		url = "http://maps.google.com/maps/q=loc:" + dlat + "," + dlon + "&z=" + z;
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(dlat, dlon, z));

		// LEGACY  this URL does not work, where is it used?
		// whatsapp
		// http://maps.google.com/maps/q=loc:34,-106 (You)
		z = GeoParsedPoint.NO_ZOOM;
		url = "http://maps.google.com/maps/q=loc:" + ilat + "," + ilon + " (You)";
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(ilat, ilon, z));

		// LEGACY this URL does not work, where is it used?
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

		// http://www.openstreetmap.org/search?query=Amsterdam
		qstr = "Amsterdam";
		url = "http://www.openstreetmap.org/search?query=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr));

		// http://www.openstreetmap.org/search?query=Bloemstraat+51A,+Amsterdam
		qstr = "Bloemstraat 51A, Amsterdam";
		url = "http://www.openstreetmap.org/search?query=" + URLEncoder.encode(qstr);
		System.out.println("url: " + url);
		actual = GeoPointParserUtil.parse(url);
		assertGeoPoint(actual, new GeoParsedPoint(qstr.replace(',',' ')));

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

	@Test
	public void testDirectionsPoints() {
		List<GeoParsedPoint> actual = GeoPointParserUtil.parsePoints(
				"https://www.openstreetmap.org/directions?from=37.2445%2C-121.9125&to=37.2888%2C-122.1899");
		assertGeoPoint(actual.get(0), new GeoParsedPoint(37.2445, -121.9125));
		assertGeoPoint(actual.get(1), new GeoParsedPoint(37.2888, -122.1899));

		actual = GeoPointParserUtil.parsePoints(
				"https://www.google.com/maps/dir/?api=1&origin=-39.7649077,175.8822549&destination=-41.156615,174.975586");
		assertGeoPoint(actual.get(0), new GeoParsedPoint(-39.7649077,175.8822549));
		assertGeoPoint(actual.get(1), new GeoParsedPoint(-41.156615, 174.975586));
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





}
