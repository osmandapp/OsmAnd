package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.openlocationcode.OpenLocationCode;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.MGRSPoint;
import com.jwetherell.openmap.common.UTMPoint;
import com.jwetherell.openmap.common.ZonedUTMPoint;

import net.osmand.data.LatLon;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.util.KLocationParser;
import net.osmand.util.LocationParser;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * {@link KLocationParser} against {@link LocationParser} in OsmAnd-java, and through it the copies
 * of the Open Location Code library and of the UTM and MGRS parts of openmap.
 *
 * The phrases are the ones of {@code LocationSearchTest}, and places at random points of the world
 * written the ways people type them: decimal degrees with points and commas, degrees with minutes
 * and seconds, UTM with the hemisphere or the band, MGRS at every accuracy, full and short Open
 * Location Codes with a place name after them, and links. The java classes write the UTM, MGRS and
 * Open Location Code ones. Last come random strings of the characters these are made of, which
 * reach the branches that reject a phrase, and UTM zones out of 0 to 60.
 *
 * For each phrase both are asked for the place, for the Open Location Code and the place it
 * recovers near three points, and for the numbers and words it splits into. A place is compared
 * bit for bit; an exception by its kind, as the classes differ between the jvm and Kotlin/Native.
 *
 * {@link #javaDumpIsWritten} also writes java's answers to {@code build/location-parser-java.txt},
 * which {@code KLocationParserTest} in OsmAnd-shared holds the copy to on Kotlin/Native.
 */
public class LocationParserCompatTest {

	/** Read by {@code KLocationParserTest} in OsmAnd-shared, from {@code ../OsmAnd-java/build}. */
	private static final File JAVA_DUMP = new File("build/location-parser-java.txt");

	private static final int POINTS = 1500;
	private static final int RANDOM_PHRASES = 20000;
	private static final String FUZZ = "0123456789012345678901234567890123456789..,,;: --+°′″'\"NSEWnsewCUXPQMGRSTVJKLaz#()/+‎‬ \t٣٤";

	private static final String[] PHRASES = {
			// LocationSearchTest
			"geo:34.99393,-106.61568 (Treasure Island, other irrelevant info) ",
			"http://download.osmand.net/go?lat=34.99393&lon=-106.61568&z=11",
			"5.0,3.0", "(5.0,3.0)", "5.445,3.523", "5:1:1,3:1",
			"17N6734294749123", "17 N 673429 4749123", "36N 609752 5064037", "35U 332274 5421365",
			"5.0 3.0", "-5.0 -3.0", "-45.5 3.0S", "45.5S 3.0 W", "5.445 3.523", "5:1:1 3:1", "5:1#1 3#1",
			"5#1#1 3#1", "5'1'1 3'1", "Lat: 5.0 Lon: 3.0", "0 n, 78 w", "0 N, 78 W", "N 0 W 78", "n 0 w 78",
			"ftp://simpleurl?lat=34.23&lon=-53.2&z=15", "ftp://simpleurl?z=15&lat=34.23&lon=-53.2",
			"5 30 30 N 4 30 W", "5 30  -4 30", "S 5 30  4 30 W", "S5.4232  4.30W", "S5.4232  W4.30",
			"5.4232, W4.30", "5.4232N, 45 30.5W", "43°S 79°23′13.7″W", "43°38′33.24″N 79°23′13.7″W",
			"45° 30'30\"W 3.0", "43° 79°23′13.7″E", "43°38′ 79°23′13.7″E", "43°38′23\" 79°23′13.7″E",
			"(33,95060 °S, 151,14453° E)", "33,95060 °S, 151,14453° E", "33,95060, 151,14453",
			"33,95060 151,14453", "15,1235 S, 23,1244 W", "-15,1235, 23,1244",
			// olc.json of the search tests
			"8FVC9G8F+6X", "9G8F+6X", "9G8F+6X Zurich", "8FVC9G8F+6XQ", "8fvc9g8f+6x", "8FVC0000+", "8F000000+",
			// rejected
			"", " ", "  5.0 3.0  ", "‎5.0 3.0‏", "N 45.5 E 3.5 ‎", "a 1.5 b 2.5", "x 1.5 y 2.5 z",
			"61 N 673429 4749123", "-1 N 673429 4749123", "0 N 500000 0", "61N6734294749123", "17X6734294749123",
			"17N673429474912", "17N", "N", "-", "--", ".", "-0 5", "5 -0", "0 -0", "-0 -0", "1.2.3 4", "1e5 2",
			"٣٣.٥ ٤٤", "٣٣ ٤٤", "33UXP", "33UXP1", "33UXP12", "33UXP0450005500", "33 U XP 04500 05500",
			"1UXP12", "61UXP12", "0UXP12", "33ZXP12", "33UIP12", "33UXV12", "33UXZ12", "33UXP1E23", "33UXP12D3",
			"33UXP0X1P3", "4QFJ12345678", "4QFJ1234567", "18SUJ2337106519", "18sUJ2337106519", "18S UJ 23371 06519",
			"5c Hazelmere road, nw6 6", "100 bridge street", "31st road", "N50", "S50 E30", "50N", "50N 30E",
			"1,1234 2,1234", "1,123 2,123", "12,12345,12,12345", "+5.0 +3.0", "5.0\t3.0", "5.0 3.0",
			"geo:0,0", "https://maps.google.com/?q=48.8584,2.2945", "osmand.net/go?lat=1.5&lon=2.5",
	};

	private static final LatLon[] REFERENCES = {new LatLon(47.3769, 8.5417), new LatLon(-33.9, 151.2), new LatLon(89.9, -179.9)};

	private static List<String> phrases;

	@BeforeClass
	public static void collect() {
		Set<String> collected = new LinkedHashSet<>();
		Collections.addAll(collected, PHRASES);
		Random random = new Random(25092026L);
		for (int i = 0; i < POINTS; i++) {
			double lat = random.nextDouble() * 180 - 90;
			double lon = random.nextDouble() * 360 - 180;
			if (i % 50 == 0) {
				lat = Math.round(lat);
				lon = Math.round(lon);
			}
			addPlace(collected, random, lat, lon);
		}
		for (int i = 0; i < RANDOM_PHRASES; i++) {
			int length = 1 + random.nextInt(24);
			StringBuilder sb = new StringBuilder();
			for (int k = 0; k < length; k++) {
				sb.append(FUZZ.charAt(random.nextInt(FUZZ.length())));
			}
			collected.add(sb.toString());
		}
		phrases = new ArrayList<>(collected);
		assertTrue("phrases: " + phrases.size(), phrases.size() > RANDOM_PHRASES + POINTS * 10);
	}

	private static void addPlace(Set<String> out, Random random, double lat, double lon) {
		String la = String.format(Locale.US, "%." + (1 + random.nextInt(7)) + "f", lat);
		String lo = String.format(Locale.US, "%." + (1 + random.nextInt(7)) + "f", lon);
		out.add(la + " " + lo);
		out.add(la + ", " + lo);
		out.add(la + "," + lo);
		out.add("(" + la + "," + lo + ")");
		out.add(la.replace('.', ',') + " " + lo.replace('.', ','));
		out.add(la.replace('.', ',') + ", " + lo.replace('.', ','));
		String ns = lat < 0 ? "S" : "N";
		String ew = lon < 0 ? "W" : "E";
		String ala = la.replace("-", "");
		String alo = lo.replace("-", "");
		out.add(ala + ns + " " + alo + ew);
		out.add(ns + ala + " " + ew + alo);
		out.add(ala + " " + ns.toLowerCase() + ", " + alo + " " + ew.toLowerCase());
		out.add("Lat: " + la + " Lon: " + lo);
		out.add(dms(Math.abs(lat), random) + ns + " " + dms(Math.abs(lon), random) + ew);
		out.add(dm(Math.abs(lat)) + " " + ns + " " + dm(Math.abs(lon)) + " " + ew);
		out.add("geo:" + la + "," + lo + "?z=" + random.nextInt(20));
		out.add("https://osmand.net/map?pin=" + la + "," + lo + "#15/" + la + "/" + lo);
		out.add("http://download.osmand.net/go?lat=" + la + "&lon=" + lo + "&z=" + random.nextInt(20));

		LatLonPoint llp = new LatLonPoint(lat, lon);
		UTMPoint utm = UTMPoint.LLtoUTM(llp);
		if (utm != null) {
			out.add(utm.format());
			out.add(utm.zone_number + " " + utm.zone_letter + " " + (long) utm.easting + " " + (long) utm.northing);
			out.add(utm.zone_number + "" + utm.zone_letter + (long) utm.easting + (long) utm.northing);
			out.add(utm.format().toLowerCase());
			ZonedUTMPoint zoned = new ZonedUTMPoint(llp);
			out.add(zoned.zone_number + "" + zoned.zone_letter + " " + (long) zoned.easting + " " + (long) zoned.northing);
		}
		MGRSPoint mgrs = MGRSPoint.LLtoMGRS(llp);
		for (int accuracy = 1; accuracy <= 5; accuracy++) {
			mgrs.resolve(accuracy);
			if (mgrs.toString() != null) {
				out.add(mgrs.toString());
				out.add(mgrs.toString().toLowerCase());
				out.add(mgrs.toFlavoredString());
			}
		}
		mgrs.resolve(5);
		if (mgrs.toString() != null) {
			out.add(mgrs.toFlavoredString(1 + random.nextInt(5)));
		}

		int[] lengths = {4, 6, 8, 10, 11, 12, 13, 14, 15};
		int length = lengths[random.nextInt(lengths.length)];
		String code = OpenLocationCode.encode(lat, lon, length);
		out.add(code);
		out.add(code.toLowerCase());
		out.add(code + " Some Town");
		if (length >= 8) {
			OpenLocationCode olc = new OpenLocationCode(code);
			double refLat = lat + (random.nextDouble() - 0.5) * 0.1;
			double refLon = lon + (random.nextDouble() - 0.5) * 0.1;
			try {
				String shortCode = olc.shorten(refLat, refLon).getCode();
				out.add(shortCode);
				out.add(shortCode + " Zürich");
				out.add(shortCode + " " + shortCode);
			} catch (IllegalArgumentException | IllegalStateException e) {
				// too far from the reference, or padded
			}
		}
	}

	private static String dms(double v, Random random) {
		int d = (int) v;
		double m = (v - d) * 60;
		int mi = (int) m;
		double s = (m - mi) * 60;
		String sec = String.format(Locale.US, "%.2f", s);
		return random.nextBoolean() ? d + "°" + mi + "′" + sec + "″" : d + "°" + mi + "'" + sec + "\"";
	}

	private static String dm(double v) {
		int d = (int) v;
		return d + " " + String.format(Locale.US, "%.3f", (v - d) * 60);
	}

	@Test
	public void placesAreTheSame() {
		for (String phrase : phrases) {
			assertEquals(locationLine(phrase, false), locationLine(phrase, true));
		}
	}

	@Test
	public void openLocationCodesAreTheSame() {
		for (String phrase : phrases) {
			assertEquals(olcLine(phrase, false), olcLine(phrase, true));
		}
	}

	@Test
	public void phrasesSplitTheSameWay() {
		for (String phrase : phrases) {
			assertEquals(splitLine(phrase, false), splitLine(phrase, true));
		}
	}

	@Test
	public void javaDumpIsWritten() throws IOException {
		StringBuilder sb = new StringBuilder();
		int places = 0;
		for (String phrase : phrases) {
			String line = locationLine(phrase, false);
			places += line.endsWith("\tnull") || line.contains("\t!") ? 0 : 1;
			sb.append(line).append('\n');
			sb.append(olcLine(phrase, false)).append('\n');
			sb.append(splitLine(phrase, false)).append('\n');
		}
		JAVA_DUMP.getParentFile().mkdirs();
		Files.write(JAVA_DUMP.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
		System.out.println("LocationParserCompatTest: " + phrases.size() + " phrases, " + places + " places");
		assertTrue("places: " + places, places > POINTS * 10);
	}

	/** {@code L phrase place}, as {@code KLocationParserTest} builds it. */
	private static String locationLine(String phrase, boolean ofCopy) {
		return "L " + CommonWordsCompatTest.hex(phrase) + "\t" + outcome(() -> ofCopy
				? place(KLocationParser.INSTANCE.parseLocation(phrase))
				: place(LocationParser.parseLocation(phrase)));
	}

	/** {@code O phrase isValidOLC parsed recovered...}. */
	private static String olcLine(String phrase, boolean ofCopy) {
		StringBuilder sb = new StringBuilder("O ").append(CommonWordsCompatTest.hex(phrase));
		sb.append('\t').append(outcome(() -> String.valueOf(ofCopy
				? KLocationParser.INSTANCE.isValidOLC(phrase) : LocationParser.isValidOLC(phrase))));
		if (ofCopy) {
			KLocationParser.ParsedOpenLocationCode code = KLocationParser.INSTANCE.parseOpenLocationCode(phrase);
			sb.append('\t').append(code == null ? "null" : CommonWordsCompatTest.hex(code.getText()) + "," + hexOrNull(code.getCode())
					+ "," + code.isFull() + "," + hexOrNull(code.getPlaceName()) + "," + place(code.getLatLon()));
			if (code != null) {
				for (LatLon r : REFERENCES) {
					sb.append('\t').append(outcome(() -> place(code.recover(new KLatLon(r.getLatitude(), r.getLongitude())))));
				}
			}
		} else {
			LocationParser.ParsedOpenLocationCode code = LocationParser.parseOpenLocationCode(phrase);
			sb.append('\t').append(code == null ? "null" : CommonWordsCompatTest.hex(code.getText()) + "," + hexOrNull(code.getCode())
					+ "," + code.isFull() + "," + hexOrNull(code.getPlaceName()) + "," + place(code.getLatLon()));
			if (code != null) {
				for (LatLon r : REFERENCES) {
					sb.append('\t').append(outcome(() -> place(code.recover(r))));
				}
			}
		}
		return sb.toString();
	}

	/** {@code S phrase numbers objects texts partial coordinate}. */
	private static String splitLine(String phrase, boolean ofCopy) {
		List<Double> d = new ArrayList<>();
		List<Object> all = new ArrayList<>();
		List<String> strings = new ArrayList<>();
		boolean[] partial = {false};
		StringBuilder sb = new StringBuilder("S ").append(CommonWordsCompatTest.hex(phrase)).append('\t');
		String split = outcome(() -> {
			if (ofCopy) {
				KLocationParser.INSTANCE.splitObjects(phrase, d, all, strings, partial);
			} else {
				LocationParser.splitObjects(phrase, d, all, strings, partial);
			}
			return "";
		});
		sb.append(split);
		for (Double v : d) {
			sb.append(bits(v)).append(',');
		}
		sb.append('\t');
		for (Object o : all) {
			sb.append(o instanceof Double ? "d" + bits((Double) o) : "s" + CommonWordsCompatTest.hex((String) o)).append(',');
		}
		sb.append('\t');
		for (String s : strings) {
			sb.append(CommonWordsCompatTest.hex(s)).append(',');
		}
		sb.append('\t').append(partial[0]).append('\t');
		sb.append(outcome(() -> bits(ofCopy
				? KLocationParser.INSTANCE.parse1Coordinate(all, 0, all.size())
				: LocationParser.parse1Coordinate(all, 0, all.size()))));
		return sb.toString();
	}

	private static String place(LatLon l) {
		return l == null ? "null" : bits(l.getLatitude()) + "," + bits(l.getLongitude());
	}

	private static String place(KLatLon l) {
		return l == null ? "null" : bits(l.getLatitude()) + "," + bits(l.getLongitude());
	}

	private static String bits(double v) {
		return Long.toHexString(Double.doubleToRawLongBits(v));
	}

	private static String hexOrNull(String s) {
		return s == null ? "null" : CommonWordsCompatTest.hex(s);
	}

	/** What [call] returns, or {@code !} and the kind of what it throws. */
	private static String outcome(Supplier<String> call) {
		try {
			return call.get();
		} catch (NullPointerException e) {
			return "!NullPointer";
		} catch (NumberFormatException e) {
			return "!NumberFormat";
		} catch (IndexOutOfBoundsException e) {
			return "!IndexOutOfBounds";
		} catch (IllegalArgumentException e) {
			return "!IllegalArgument";
		} catch (IllegalStateException e) {
			return "!IllegalState";
		} catch (RuntimeException e) {
			return "!" + e.getClass().getSimpleName();
		}
	}
}
