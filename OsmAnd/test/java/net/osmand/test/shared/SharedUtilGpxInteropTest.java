package net.osmand.test.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SharedUtilGpxInteropTest {

	@Test
	public void testJGpxFileCopiesDetachedDataWithoutPreClone() {
		GpxFile source = new GpxFile((String) null);

		Track track = new Track();
		TrkSegment segment = new TrkSegment();
		WptPt firstPoint = new WptPt();
		firstPoint.setLat(10.0);
		firstPoint.setLon(20.0);
		firstPoint.setTime(1_000L);
		WptPt secondPoint = new WptPt();
		secondPoint.setLat(10.1);
		secondPoint.setLon(20.1);
		secondPoint.setTime(2_000L);
		segment.getPoints().add(firstPoint);
		segment.getPoints().add(secondPoint);
		track.getSegments().add(segment);
		source.getTracks().add(track);

		WptPt waypoint = new WptPt();
		waypoint.setLat(11.0);
		waypoint.setLon(21.0);
		waypoint.setName("waypoint");
		source.addPoint(waypoint);
		source.addGeneralTrack();

		GPXFile javaGpx = SharedUtil.jGpxFile(source);

		assertEquals(source.getTracks().size(), javaGpx.tracks.size());
		assertEquals(source.getPointsSize(), javaGpx.getPointsSize());
		assertEquals(
				source.getTracks().get(0).getSegments().get(0).getPoints().size(),
				javaGpx.tracks.get(0).segments.get(0).points.size()
		);

		source.getTracks().get(0).getSegments().get(0).getPoints().get(0).setLat(99.0);
		source.getPointsList().get(0).setName("changed");

		assertNotEquals(99.0, javaGpx.tracks.get(0).segments.get(0).points.get(0).lat, 0.0);
		assertEquals("waypoint", javaGpx.getPoints().get(0).name);
	}
}
