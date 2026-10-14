package net.osmand.gpx.flyover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.data.LatLon;
import net.osmand.gpx.flyover.FlyoverCameraPath.Frame;
import net.osmand.gpx.flyover.FlyoverCameraPath.Phase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FlyoverCameraPathTest {

	private static final int FPS = 30;
	private static final double LAT = 50.0;
	private static final double LON = 10.0;
	private static final double OVERVIEW_ZOOM = 12.4f;

	/** Straight track heading east, {@code count} points {@code stepMeters} apart. */
	static FlyoverTrackGeometry eastwardTrack(int count, double stepMeters) {
		double lonPerMeter = 1 / (111_320d * Math.cos(Math.toRadians(LAT)));
		List<LatLon> points = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			points.add(new LatLon(LAT, LON + i * stepMeters * lonPerMeter));
		}
		return new FlyoverTrackGeometry(points);
	}

	private static FlyoverCameraPath path(FlyoverTrackGeometry geometry) {
		return new FlyoverCameraPath(geometry, FPS, geometry.getCenterLat(), geometry.getCenterLon(),
				(float) OVERVIEW_ZOOM, FlyoverCameraPath.DEFAULT_FLIGHT_TILT);
	}

	@Test
	public void geometryMeasuresLengthAndPositions() {
		FlyoverTrackGeometry geometry = eastwardTrack(31, 100);
		assertEquals(3000, geometry.getLength(), 1);
		LatLon middle = geometry.positionAt(1500);
		assertEquals(LAT, middle.getLatitude(), 1e-9);
		assertEquals(geometry.getCenterLon(), middle.getLongitude(), 1e-9);
		assertEquals(90, geometry.bearingAt(1000, 100), 0.01);
		assertEquals(90, geometry.bearingAt(2990, 100), 0.01);
	}

	@Test
	public void frameCountsFollowTrackLength() {
		FlyoverCameraPath path = path(eastwardTrack(31, 100));
		// 3 km: 8 + 3 * 1.2 = 11.6 s, raised to the 12 s minimum
		assertEquals(12, path.getFlightSeconds(), 1e-9);
		assertEquals(360, path.getFlightFrames());
		int expected = 60 + 360 + 60 + 105;
		assertEquals(expected, path.getTotalFrames());

		assertEquals(FlyoverCameraPath.MAX_FLIGHT_SECONDS, FlyoverCameraPath.flightSeconds(100_000), 1e-9);
		assertEquals(20, FlyoverCameraPath.flightSeconds(10_000), 1e-9);
		// a very long track still gives a video of at most 40 seconds
		FlyoverCameraPath longest = path(eastwardTrack(1001, 100));
		assertEquals(FlyoverCameraPath.MAX_VIDEO_SECONDS, longest.getTotalFrames() / (double) FPS, 1e-9);
	}

	@Test
	public void introStartsAtOverviewAndEndsAtFlightStart() {
		FlyoverTrackGeometry geometry = eastwardTrack(31, 100);
		FlyoverCameraPath path = path(geometry);
		Frame first = path.frameAt(0);
		assertEquals(Phase.INTRO, first.phase);
		assertEquals(geometry.getCenterLat(), first.lat, 1e-9);
		assertEquals(geometry.getCenterLon(), first.lon, 1e-9);
		assertEquals(OVERVIEW_ZOOM, first.zoom, 1e-6);
		assertEquals(FlyoverCameraPath.TOP_DOWN_TILT, first.tilt, 1e-6);
		assertEquals(0, first.rotation, 1e-6);
		assertEquals(0, first.flightProgress, 1e-9);

		Frame flightStart = path.frameAt(path.getIntroFrames());
		assertEquals(Phase.FLIGHT, flightStart.phase);
		assertEquals(LON, flightStart.lon, 1e-9);
		Frame lastIntro = path.frameAt(path.getIntroFrames() - 1);
		assertEquals(flightStart.lon, lastIntro.lon, 1e-4);
		assertEquals(flightStart.tilt, lastIntro.tilt, 1);
	}

	@Test
	public void flightFollowsTrackHeadingUp() {
		FlyoverTrackGeometry geometry = eastwardTrack(31, 100);
		FlyoverCameraPath path = path(geometry);
		int start = path.getIntroFrames();
		int end = start + path.getFlightFrames();
		double previousLon = Double.NEGATIVE_INFINITY;
		double previousProgress = -1;
		for (int i = start; i < end; i++) {
			Frame frame = path.frameAt(i);
			assertEquals(Phase.FLIGHT, frame.phase);
			assertEquals(LAT, frame.lat, 1e-9);
			assertTrue("longitude must grow eastwards", frame.lon > previousLon);
			assertTrue("progress must grow", frame.flightProgress > previousProgress);
			assertEquals(FlyoverCameraPath.DEFAULT_FLIGHT_TILT, frame.tilt, 1e-6);
			assertEquals(path.getFlightZoom(), frame.zoom, 1e-6);
			// heading east means the map is rotated by -90 so that east points up
			assertEquals(-90, frame.rotation, 0.5);
			previousLon = frame.lon;
			previousProgress = frame.flightProgress;
		}
		assertEquals(0, path.frameAt(start).flightProgress, 1e-9);
		assertEquals(1, path.frameAt(end - 1).flightProgress, 1e-9);
		assertEquals(geometry.getMaxLon(), path.frameAt(end - 1).lon, 1e-9);
		assertEquals(0, path.flightTimeSeconds(start), 1e-9);
		assertEquals(path.getFlightSeconds(), path.flightTimeSeconds(end), 1e-6);
	}

	/** Track heading east over {@code lengthMeters}, swinging {@code amplitudeMeters} north and south every 300 m. */
	static FlyoverTrackGeometry zigzagTrack(double lengthMeters, double amplitudeMeters) {
		double lonPerMeter = 1 / (111_320d * Math.cos(Math.toRadians(LAT)));
		double latPerMeter = 1 / 111_320d;
		List<LatLon> points = new ArrayList<>();
		for (int i = 0; i * 300 <= lengthMeters; i++) {
			double offset = i % 2 == 0 ? amplitudeMeters : -amplitudeMeters;
			points.add(new LatLon(LAT + offset * latPerMeter, LON + i * 300 * lonPerMeter));
		}
		return new FlyoverTrackGeometry(points);
	}

	@Test
	public void cameraGlidesAlongTheLineOfAZigzagTrack() {
		FlyoverTrackGeometry geometry = zigzagTrack(12_000, 150);
		FlyoverCameraPath path = path(geometry);
		int start = path.getIntroFrames();
		int end = start + path.getFlightFrames();
		double latPerMeter = 1 / 111_320d;
		double maxCameraOffset = 0;
		double maxPointOffset = 0;
		for (int i = start; i < end; i++) {
			Frame frame = path.frameAt(i);
			LatLon point = geometry.positionAt(geometry.getLength() * frame.flightProgress);
			if (frame.flightProgress > 0.2 && frame.flightProgress < 0.8) {
				maxCameraOffset = Math.max(maxCameraOffset, Math.abs(frame.lat - LAT) / latPerMeter);
			}
			maxPointOffset = Math.max(maxPointOffset, Math.abs(point.getLatitude() - LAT) / latPerMeter);
		}
		// the current point swings 150 m to either side; the camera keeps to the line in the middle
		assertEquals(150, maxPointOffset, 1);
		assertTrue("camera offset " + maxCameraOffset, maxCameraOffset < 40);
		// and still starts and ends exactly on the track
		Frame first = path.frameAt(start);
		Frame last = path.frameAt(end - 1);
		assertEquals(geometry.positionAt(0).getLatitude(), first.lat, 1e-9);
		assertEquals(geometry.positionAt(0).getLongitude(), first.lon, 1e-9);
		assertEquals(geometry.positionAt(geometry.getLength()).getLatitude(), last.lat, 1e-9);
		assertEquals(geometry.positionAt(geometry.getLength()).getLongitude(), last.lon, 1e-9);
	}

	/** Track heading east for {@code eastMeters}, then north for {@code northMeters}, 100 m between points. */
	static FlyoverTrackGeometry cornerTrack(double eastMeters, double northMeters) {
		double lonPerMeter = 1 / (111_320d * Math.cos(Math.toRadians(LAT)));
		double latPerMeter = 1 / 111_320d;
		List<LatLon> points = new ArrayList<>();
		for (int i = 0; i * 100 <= eastMeters; i++) {
			points.add(new LatLon(LAT, LON + i * 100 * lonPerMeter));
		}
		double cornerLon = LON + eastMeters * lonPerMeter;
		for (int i = 1; i * 100 <= northMeters; i++) {
			points.add(new LatLon(LAT + i * 100 * latPerMeter, cornerLon));
		}
		return new FlyoverTrackGeometry(points);
	}

	@Test
	public void headingHoldsAndTurnsOnceAtACorner() {
		FlyoverCameraPath path = path(cornerTrack(5000, 5000));
		int start = path.getIntroFrames();
		int end = start + path.getFlightFrames();
		int changes = 0;
		float previous = path.frameAt(start).rotation;
		int firstChange = -1;
		int lastChange = -1;
		for (int i = start + 1; i < end; i++) {
			float rotation = path.frameAt(i).rotation;
			if (Math.abs(FlyoverCameraPath.shortestDiff(previous, rotation)) > 1e-3) {
				changes++;
				if (firstChange < 0) {
					firstChange = i;
				}
				lastChange = i;
			}
			previous = rotation;
		}
		// east leg: east points up; north leg: north points up
		assertEquals(-90, path.frameAt(start).rotation, 0.5);
		assertEquals(0, path.frameAt(end - 1).rotation, 0.5);
		// the heading changes only inside one turn of HEADING_TURN_SECONDS
		int turnFrames = (int) Math.round(FlyoverCameraPath.HEADING_TURN_SECONDS * FPS);
		assertTrue("changes: " + changes, changes > 0 && changes <= turnFrames);
		assertTrue("turn spans " + (lastChange - firstChange + 1) + " frames", lastChange - firstChange + 1 <= turnFrames);
		// the turn starts right after the corner, once the new direction is confirmed
		double corner = 0.5;
		double confirm = FlyoverCameraPath.HEADING_CHANGE_CONFIRM_SECONDS / path.getFlightSeconds();
		double turnStart = path.frameAt(firstChange).flightProgress;
		assertTrue("turn starts at " + turnStart, turnStart >= corner && turnStart <= corner + confirm + 0.02);
	}

	@Test
	public void outroReturnsToOverviewByShortestRotation() {
		FlyoverTrackGeometry geometry = eastwardTrack(31, 100);
		FlyoverCameraPath path = path(geometry);
		int start = path.getIntroFrames() + path.getFlightFrames();
		for (int i = start; i < start + 60; i++) {
			Frame frame = path.frameAt(i);
			assertEquals(Phase.OUTRO, frame.phase);
			assertTrue("rotation must stay on the short arc", frame.rotation <= 0 && frame.rotation >= -90);
			assertTrue(frame.tilt >= FlyoverCameraPath.DEFAULT_FLIGHT_TILT && frame.tilt <= 90);
			assertEquals(1, frame.flightProgress, 1e-9);
		}
		Frame last = path.frameAt(path.getTotalFrames() - 1);
		assertEquals(Phase.SUMMARY, last.phase);
		assertEquals(geometry.getCenterLat(), last.lat, 1e-9);
		assertEquals(geometry.getCenterLon(), last.lon, 1e-9);
		assertEquals(OVERVIEW_ZOOM, last.zoom, 1e-6);
		assertEquals(FlyoverCameraPath.TOP_DOWN_TILT, last.tilt, 1e-6);
		assertEquals(0, last.rotation, 1e-6);
		assertTrue(last.phaseProgress > 0.9 && last.phaseProgress < 1);
		// indexes past the end stay on the last frame instead of failing
		assertEquals(Phase.SUMMARY, path.frameAt(path.getTotalFrames() + 10).phase);
	}

	@Test
	public void angleHelpers() {
		assertEquals(20, FlyoverCameraPath.shortestDiff(170, -170), 1e-6);
		assertEquals(-20, FlyoverCameraPath.shortestDiff(-170, 170), 1e-6);
		assertEquals(-10, FlyoverCameraPath.normalize180(350), 1e-6);
		assertEquals(-180, FlyoverCameraPath.normalize180(180), 1e-6);
		assertEquals(10, FlyoverCameraPath.rotationForHeading(350), 1e-6);
		assertEquals(0, FlyoverCameraPath.smooth(0), 1e-9);
		assertEquals(1, FlyoverCameraPath.smooth(1), 1e-9);
		assertEquals(0.5, FlyoverCameraPath.smooth(0.5), 1e-9);
	}

	@Test
	public void flightZoomStaysBetweenLimitsAndAboveOverview() {
		// a short track flies close, a very long one is limited by the lower bound
		assertEquals(15.5f, FlyoverCameraPath.flightZoom(0, 10f), 1e-6);
		assertEquals(FlyoverCameraPath.MIN_FLIGHT_ZOOM, FlyoverCameraPath.flightZoom(1_500_000, 4f), 1e-6);
		// long tracks are flown in steps of zoom by length
		assertEquals(10.4f, FlyoverCameraPath.flightZoom(50_000, 5f), 0.05);
		assertEquals(10f, FlyoverCameraPath.flightZoom(100_000, 5f), 1e-6);
		assertEquals(9f, FlyoverCameraPath.flightZoom(300_000, 5f), 1e-6);
		assertEquals(8f, FlyoverCameraPath.flightZoom(700_000, 4f), 1e-6);
		assertEquals(7f, FlyoverCameraPath.flightZoom(1_200_000, 4f), 1e-6);
		// a compact loop that fits the frame at zoom 13 is flown two levels closer, not panned
		assertEquals(15f, FlyoverCameraPath.flightZoom(17_000, 13f), 1e-6);
		// never beyond the maximum map zoom
		assertEquals(FlyoverCameraPath.MAX_FLIGHT_ZOOM, FlyoverCameraPath.flightZoom(100, 16f), 1e-6);
		float mid = FlyoverCameraPath.flightZoom(5_000, 10f);
		assertTrue(mid > FlyoverCameraPath.MIN_FLIGHT_ZOOM && mid < FlyoverCameraPath.MAX_FLIGHT_ZOOM);
	}

	@Test
	public void overviewZoomFitsTheBoxIntoTheFrame() {
		// half the world across 256 px is zoom 1; the whole world would be zoom 0, below the map's lowest zoom
		assertEquals(1, FlyoverCameraPath.overviewZoom(-10, 10, -90, 90, 256, 256, 256), 0.01);
		assertEquals(1, FlyoverCameraPath.overviewZoom(-85, 85, -180, 180, 256, 256, 256), 0.01);
		// a quarter of the world across 256 px is zoom 2
		assertEquals(2, FlyoverCameraPath.overviewZoom(-10, 10, -45, 45, 256, 256, 256), 0.01);
		// a 3 km eastward track at latitude 50: its width in pixels at the zoom found fits exactly
		FlyoverTrackGeometry track = eastwardTrack(31, 100);
		float zoom = FlyoverCameraPath.overviewZoom(track.getMinLat(), track.getMaxLat(), track.getMinLon(), track.getMaxLon(),
				1000, 600, 256);
		double widthPx = (track.getMaxLon() - track.getMinLon()) / 360 * 256 * Math.pow(2, zoom);
		assertEquals(1000, widthPx, 0.5);
		// a taller frame part does not change a zoom limited by the width
		assertEquals(zoom, FlyoverCameraPath.overviewZoom(track.getMinLat(), track.getMaxLat(), track.getMinLon(), track.getMaxLon(),
				1000, 6000, 256), 1e-6);
		// a narrower one does
		assertTrue(FlyoverCameraPath.overviewZoom(track.getMinLat(), track.getMaxLat(), track.getMinLon(), track.getMaxLon(),
				500, 600, 256) < zoom);
	}

	@Test
	public void singlePointTrackDoesNotFail() {
		List<LatLon> points = new ArrayList<>();
		points.add(new LatLon(LAT, LON));
		FlyoverTrackGeometry geometry = new FlyoverTrackGeometry(points);
		FlyoverCameraPath path = path(geometry);
		for (int i = 0; i < path.getTotalFrames(); i++) {
			Frame frame = path.frameAt(i);
			assertEquals(LAT, frame.lat, 1e-9);
			assertEquals(LON, frame.lon, 1e-9);
		}
	}
}
