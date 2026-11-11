package net.osmand.router.gtfs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.osmand.data.LatLon;

/**
 * Builds small synthetic GTFS feeds in code. {@link #basic()} has the agency (America/New_York), four
 * stops A..D 850 m apart on a west-east line, two routes (R1, R2) and the services WD (Monday to
 * Friday), SA, SU for all of 2026 and 2027. Tests add trips and whatever else they need.
 */
final class GtfsFixture {

	static final ZoneId NY = ZoneId.of("America/New_York");
	static final LocalDate TUE = LocalDate.of(2026, 11, 17);
	static final LocalDate FRI = LocalDate.of(2026, 11, 20);
	static final LocalDate SAT = LocalDate.of(2026, 11, 21);
	static final LocalDate SUN = LocalDate.of(2026, 11, 22);

	private static final double LAT = 40.70;
	private static final double LON0 = -74.00;

	final Map<String, List<String>> files = new LinkedHashMap<>();

	static GtfsFixture basic() {
		GtfsFixture f = new GtfsFixture();
		f.file("agency.txt", "agency_id,agency_name,agency_url,agency_timezone",
				"a,Test Agency,http://example.com,America/New_York");
		f.file("stops.txt", "stop_id,stop_name,stop_lat,stop_lon");
		for (char c = 'A'; c <= 'D'; c++) {
			f.stop(String.valueOf(c), "Stop " + c, LAT, LON0 + 0.01 * (c - 'A'));
		}
		f.file("routes.txt", "route_id,route_short_name,route_long_name,route_type,route_color",
				"R1,L1,Line One,1,FF0000", "R2,L2,Line Two,3,");
		f.file("trips.txt", "route_id,service_id,trip_id,trip_headsign");
		f.file("stop_times.txt", "trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type");
		f.file("calendar.txt", "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date",
				"WD,1,1,1,1,1,0,0,20260101,20271231", "SA,0,0,0,0,0,1,0,20260101,20271231",
				"SU,0,0,0,0,0,0,1,20260101,20271231");
		return f;
	}

	GtfsFixture file(String name, String header, String... rows) {
		List<String> lines = new ArrayList<>();
		lines.add(header);
		for (String r : rows) {
			lines.add(r);
		}
		files.put(name, lines);
		return this;
	}

	GtfsFixture row(String name, String row) {
		files.get(name).add(row);
		return this;
	}

	GtfsFixture stop(String id, String name, double lat, double lon) {
		return row("stops.txt", id + "," + name + "," + lat + "," + lon);
	}

	/**
	 * Adds a trip. Each stop is "ID arrival [departure] [pickup_type=1] [drop_off_type=1]" with times
	 * as H:MM:SS or "-" for blank.
	 */
	GtfsFixture trip(String id, String route, String service, String... stops) {
		row("trips.txt", route + "," + service + "," + id + ",To " + id);
		int seq = 1;
		for (String s : stops) {
			String[] t = s.split(" ");
			String arr = t[1].equals("-") ? "" : t[1];
			String dep = arr;
			String pick = "", drop = "";
			for (int i = 2; i < t.length; i++) {
				if (t[i].startsWith("pickup_type=")) {
					pick = t[i].substring(12);
				} else if (t[i].startsWith("drop_off_type=")) {
					drop = t[i].substring(14);
				} else {
					dep = t[i].equals("-") ? "" : t[i];
				}
			}
			row("stop_times.txt", id + "," + arr + "," + dep + "," + t[0] + "," + seq++ + "," + pick + "," + drop);
		}
		return this;
	}

	/** Writes the feed as a zip; the files are written exactly as given (line ends are CRLF). */
	File write(File dir, String name) throws IOException {
		File zip = new File(dir, name);
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
			for (Map.Entry<String, List<String>> e : files.entrySet()) {
				out.putNextEntry(new ZipEntry(e.getKey()));
				out.write((String.join("\r\n", e.getValue()) + "\r\n").getBytes(StandardCharsets.UTF_8));
				out.closeEntry();
			}
		}
		return zip;
	}

	static File zipOf(File dir, String name, String... namesAndContents) throws IOException {
		File zip = new File(dir, name);
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
			for (int i = 0; i < namesAndContents.length; i += 2) {
				out.putNextEntry(new ZipEntry(namesAndContents[i]));
				out.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
				out.closeEntry();
			}
		}
		return zip;
	}

	static LatLon at(char stop) {
		return new LatLon(LAT, LON0 + 0.01 * (stop - 'A'));
	}

	/** Epoch second of a wall-clock time in the test zone. */
	static long epoch(LocalDate date, String hhmm) {
		return ZonedDateTime.of(date, LocalTime.parse(hhmm), NY).toEpochSecond();
	}

	static String hhmm(long epoch) {
		return java.time.Instant.ofEpochSecond(epoch).atZone(NY).toLocalTime().toString();
	}
}
