package net.osmand.plus.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;

import java.util.Locale;

public final class MaidenheadPoint {

	@NonNull
	public static String toMaidenhead(double lat, double lon) {
		lon += 180.0;
		lat += 90.0;
		// Clamp margin must exceed the worst rounding-epsilon effect below (2e-5 deg),
		// otherwise indices overflow their letter/digit range at lat=90 / lon=180.
		lon = Math.max(0, Math.min(lon, 360.0 - 1e-4));
		lat = Math.max(0, Math.min(lat, 180.0 - 1e-4));
		StringBuilder sb = new StringBuilder();
		int lonField = (int) (lon / 20 + 1e-6);
		int latField = (int) (lat / 10 + 1e-6);
		lon -= lonField * 20;
		lat -= latField * 10;
		sb.append((char) ('A' + lonField)).append((char) ('A' + latField));
		int lonSquare = (int) (lon / 2 + 1e-6);
		int latSquare = (int) (lat / 1 + 1e-6);
		lon -= lonSquare * 2;
		lat -= latSquare * 1;
		sb.append((char) ('0' + lonSquare)).append((char) ('0' + latSquare));
		int lonSub = (int) (lon * 12 + 1e-6);
		int latSub = (int) (lat * 24 + 1e-6);
		lon -= lonSub / 12.0;
		lat -= latSub / 24.0;
		sb.append((char) ('A' + lonSub)).append((char) ('A' + latSub));
		sb.append(' ');
		int lonExtSquare = (int) (lon * 120 + 1e-6);
		int latExtSquare = (int) (lat * 240 + 1e-6);
		lon -= lonExtSquare / 120.0;
		lat -= latExtSquare / 240.0;
		sb.append((char) ('0' + lonExtSquare)).append((char) ('0' + latExtSquare));
		int lonExtSub = (int) (lon * 2880 + 1e-6);
		int latExtSub = (int) (lat * 5760 + 1e-6);
		sb.append((char) ('A' + lonExtSub)).append((char) ('A' + latExtSub));
		return sb.toString();
	}

	@Nullable
	public static LatLon parse(@Nullable String maidenhead) {
		if (Algorithms.isEmpty(maidenhead)) {
			return null;
		}
		maidenhead = maidenhead.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.US);
		// Reject anything that is not a valid 2/4/6/8/10-char locator
		if (!maidenhead.matches("[A-R]{2}([0-9]{2}([A-X]{2}([0-9]{2}([A-X]{2})?)?)?)?")) {
			return null;
		}
		double lon = -180.0;
		double lat = -90.0;
		if (maidenhead.length() >= 2) {
			lon += (maidenhead.charAt(0) - 'A') * 20.0;
			lat += (maidenhead.charAt(1) - 'A') * 10.0;
		}
		if (maidenhead.length() >= 4) {
			lon += (maidenhead.charAt(2) - '0') * 2.0;
			lat += (maidenhead.charAt(3) - '0') * 1.0;
		} else {
			lon += 10.0;
			lat += 5.0;
			return new LatLon(lat, lon);
		}
		if (maidenhead.length() >= 6) {
			lon += (maidenhead.charAt(4) - 'A') * (5.0 / 60.0);
			lat += (maidenhead.charAt(5) - 'A') * (2.5 / 60.0);
		} else {
			lon += 1.0;
			lat += 0.5;
			return new LatLon(lat, lon);
		}
		if (maidenhead.length() >= 8) {
			lon += (maidenhead.charAt(6) - '0') * (5.0 / 600.0);
			lat += (maidenhead.charAt(7) - '0') * (2.5 / 600.0);
		} else {
			lon += 2.5 / 60.0;
			lat += 1.25 / 60.0;
			return new LatLon(lat, lon);
		}
		if (maidenhead.length() >= 10) {
			lon += (maidenhead.charAt(8) - 'A') * (5.0 / 14400.0);
			lat += (maidenhead.charAt(9) - 'A') * (2.5 / 14400.0);
			lon += 2.5 / 14400.0;
			lat += 1.25 / 14400.0;
		} else {
			lon += 2.5 / 600.0;
			lat += 1.25 / 600.0;
		}
		return new LatLon(lat, lon);
	}
}
