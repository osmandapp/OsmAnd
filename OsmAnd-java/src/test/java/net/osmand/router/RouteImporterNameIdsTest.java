package net.osmand.router;

import net.osmand.binary.RouteDataObject;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RouteImporterNameIdsTest {

	// segment names reference type 2 although the GPX lists only types 0 and 1
	private static final String GPX = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n"
			+ "<gpx version=\"1.1\" creator=\"OsmAnd\" xmlns=\"http://www.topografix.com/GPX/1/1\""
			+ " xmlns:osmand=\"https://osmand.net/docs/technical/osmand-file-formats/osmand-gpx\">\n"
			+ "<trk><trkseg>\n"
			+ "<trkpt lat=\"39.4700\" lon=\"-0.3760\"/>\n"
			+ "<trkpt lat=\"39.4710\" lon=\"-0.3750\"/>\n"
			+ "<trkpt lat=\"39.4720\" lon=\"-0.3740\"/>\n"
			+ "<extensions>\n"
			+ "<osmand:route><segment id=\"1\" length=\"3\" startTrkptIdx=\"0\" segmentTime=\"10.0\" speed=\"10.0\""
			+ " types=\"0\" names=\"1,2\"/></osmand:route>\n"
			+ "<osmand:types><type t=\"highway\" v=\"primary\"/><type t=\"route_road_1_ref\" v=\"CV-35\"/></osmand:types>\n"
			+ "</extensions>\n"
			+ "</trkseg></trk></gpx>";

	@Test
	public void nameIdsBeyondRouteTypesAreDropped() {
		GPXFile gpxFile = GPXUtilities.loadGPXFile(new ByteArrayInputStream(GPX.getBytes(StandardCharsets.UTF_8)));
		List<RouteSegmentResult> route = new RouteImporter(gpxFile, false).importRoute();
		Assert.assertEquals(1, route.size());
		RouteDataObject rdo = route.get(0).getObject();
		Assert.assertArrayEquals(new int[] {1}, rdo.nameIds);
		for (int nameId : rdo.nameIds) {
			Assert.assertNotNull(rdo.region.quickGetEncodingRule(nameId));
		}
		Assert.assertEquals("CV-35", rdo.names.get(1));
	}
}
