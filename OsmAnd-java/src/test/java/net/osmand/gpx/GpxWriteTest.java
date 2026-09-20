package net.osmand.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class GpxWriteTest {

	@Test
	public void testSpeedIsKeptInTheFieldOnly() {
		GPXFile gpxFile = loadGpx("<gpx version=\"1.1\" creator=\"test\"><trk><trkseg>"
				+ "<trkpt lat=\"10.0\" lon=\"20.0\">"
				+ "<extensions><speed>5.5</speed><bearing>42.0</bearing></extensions></trkpt>"
				+ "<trkpt lat=\"10.1\" lon=\"20.1\"><speed>7.5</speed></trkpt>"
				+ "</trkseg></trk></gpx>");

		WptPt first = gpxFile.tracks.get(0).segments.get(0).points.get(0);
		WptPt second = gpxFile.tracks.get(0).segments.get(0).points.get(1);
		assertEquals(5.5f, first.speed, 0.0001f);
		assertEquals(42.0f, first.bearing, 0.0001f);
		assertEquals(7.5f, second.speed, 0.0001f);
		assertFalse(first.getExtensionsToRead().containsKey(GPXUtilities.POINT_SPEED));
		assertEquals("42.0", first.getExtensionsToRead().get(GPXUtilities.POINT_BEARING));
		assertFalse(second.hasExtensions());

		GPXFile reloaded = loadGpx(writeGpx(gpxFile));
		assertEquals(5.5f, reloaded.tracks.get(0).segments.get(0).points.get(0).speed, 0.0001f);
		assertEquals(7.5f, reloaded.tracks.get(0).segments.get(0).points.get(1).speed, 0.0001f);
	}

	@Test
	public void testWritingDoesNotChangeThePoints() {
		GPXFile gpxFile = new GPXFile("test");
		WptPt point = new WptPt();
		point.lat = 10;
		point.lon = 20;
		point.speed = 5.5f;
		point.heading = 42f;
		point.getExtensionsToWrite().put(GPXUtilities.PROFILE_TYPE_EXTENSION, "car");
		point.getExtensionsToWrite().put(GPXUtilities.TRKPT_INDEX_EXTENSION, "0");
		Track track = new Track();
		TrkSegment segment = new TrkSegment();
		segment.points.add(point);
		track.segments.add(segment);
		gpxFile.tracks.add(track);

		String written = writeGpx(gpxFile);
		assertTrue(written.contains("<osmand:speed>5.5</osmand:speed>"));
		assertTrue(written.contains("<osmand:heading>42</osmand:heading>"));
		// the tags that are not written for a trkpt, and the values kept in the fields,
		// must still be on the point after a save
		assertEquals("car", point.getExtensionsToRead().get(GPXUtilities.PROFILE_TYPE_EXTENSION));
		assertEquals("0", point.getExtensionsToRead().get(GPXUtilities.TRKPT_INDEX_EXTENSION));
		assertEquals(2, point.getExtensionsToRead().size());
		assertNull(point.extensionsWriters);

		assertEquals(written, writeGpx(gpxFile));
	}

	private GPXFile loadGpx(String xml) {
		GPXFile gpxFile = GPXUtilities.loadGPXFile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		assertNull(gpxFile.error);
		return gpxFile;
	}

	private String writeGpx(GPXFile gpxFile) {
		StringWriter writer = new StringWriter();
		assertNull(GPXUtilities.writeGpx(writer, gpxFile, null));
		return writer.toString();
	}
}
