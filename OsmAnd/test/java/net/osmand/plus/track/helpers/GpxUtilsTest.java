package net.osmand.plus.track.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GpxUtilsTest {
	private static final boolean TIME_WITH_GAPS = false;
	private static final boolean TIME_WITHOUT_GAPS = true;

	@Test
	public void testGetSegmentPointByTimeIncludesGapsBetweenSegments() {
		GpxFile gpxFile = new GpxFile((String) null);
		Track track = new Track();
		track.getSegments().add(createSegment(1_000L, 10.0));
		track.getSegments().add(createSegment(5_000L, 20.0));
		track.getSegments().add(createSegment(11_000L, 30.0));
		gpxFile.getTracks().add(track);

		TrkSegment generalSegment = gpxFile.getGeneralSegment();
		assertNotNull(generalSegment);

		WptPt secondSegmentStart = GpxUtils.getSegmentPointByTime(
				generalSegment, gpxFile, 4_000, false, TIME_WITH_GAPS);
		WptPt thirdSegmentStart = GpxUtils.getSegmentPointByTime(
				generalSegment, gpxFile, 10_000, false, TIME_WITH_GAPS);
		WptPt previousSegmentEnd = GpxUtils.getSegmentPointByTime(
				generalSegment, gpxFile, 2_000, false, TIME_WITH_GAPS);
		WptPt pointInSecondSegment = GpxUtils.getSegmentPointByTime(
				generalSegment, gpxFile, 4_500, true, TIME_WITH_GAPS);

		assertPointLatitude(20.0, secondSegmentStart);
		assertPointLatitude(30.0, thirdSegmentStart);
		assertPointLatitude(10.1, previousSegmentEnd);
		assertPointLatitude(20.05, pointInSecondSegment);
	}

	@Test
	public void testGetSegmentPointByTimeExcludesGapsWhenRequested() {
		GpxFile gpxFile = new GpxFile((String) null);
		Track track = new Track();
		track.getSegments().add(createSegment(1_000L, 10.0));
		track.getSegments().add(createSegment(5_000L, 20.0));
		gpxFile.getTracks().add(track);

		TrkSegment generalSegment = gpxFile.getGeneralSegment();
		assertNotNull(generalSegment);

		WptPt pointInSecondSegment = GpxUtils.getSegmentPointByTime(
				generalSegment, gpxFile, 1_500, true, TIME_WITHOUT_GAPS);

		assertPointLatitude(20.05, pointInSecondSegment);
	}

	@Test
	public void testGetSegmentPointByTimeUsesSameTimelineForSingleSegment() {
		GpxFile gpxFile = new GpxFile((String) null);
		Track track = new Track();
		TrkSegment segment = createSegment(1_000L, 10.0);
		track.getSegments().add(segment);
		gpxFile.getTracks().add(track);

		WptPt pointWithGaps = GpxUtils.getSegmentPointByTime(
				segment, gpxFile, 500, true, TIME_WITH_GAPS);
		WptPt pointWithoutGaps = GpxUtils.getSegmentPointByTime(
				segment, gpxFile, 500, true, TIME_WITHOUT_GAPS);

		assertPointLatitude(10.05, pointWithGaps);
		assertPointLatitude(10.05, pointWithoutGaps);
	}

	private TrkSegment createSegment(long startTime, double latitude) {
		TrkSegment segment = new TrkSegment();
		segment.getPoints().add(createPoint(startTime, latitude));
		segment.getPoints().add(createPoint(startTime + 1_000L, latitude + 0.1));
		return segment;
	}

	private WptPt createPoint(long time, double latitude) {
		WptPt point = new WptPt();
		point.setTime(time);
		point.setLat(latitude);
		point.setLon(0.0);
		return point;
	}

	private void assertPointLatitude(double expected, WptPt point) {
		assertNotNull(point);
		assertEquals(expected, point.getLat(), 0.000_001);
	}
}
