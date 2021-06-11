package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.GPXUtilities.*;
import static net.osmand.data.Amenity.REF;
import static net.osmand.plus.helpers.GpxUiHelper.getGpxTitle;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;

public class TravelGpx extends TravelArticle {

	public static final String DISTANCE = "distance";
	public static final String DIFF_ELE_UP = "diff_ele_up";
	public static final String DIFF_ELE_DOWN = "diff_ele_down";
	public static final String USER = "user";
	public static final String ACTIVITY_TYPE = "route_activity_type";
	private static final String ROUTE_TRACK_POINT = "route_track_point";

	public String user;
	public String activityType;
	public String ref;
	public float totalDistance;
	public double diffElevationUp;
	public double diffElevationDown;
	public int routeRadius;

	@Nullable
	public GPXFile buildGpxFile(@NonNull List<BinaryMapIndexReader> readers) {
		final List<BinaryMapDataObject> segmentList = new ArrayList<>();
		final List<Amenity> wayPointList = new ArrayList<>();

		for (BinaryMapIndexReader reader : readers) {
			try {
				if (file != null && !file.equals(reader.getFile())) {
					continue;
				}
				BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader.buildSearchRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 15, null,
						new ResultMatcher<BinaryMapDataObject>() {
							@Override
							public boolean publish(BinaryMapDataObject object) {
								if (object.getPointsLength() > 1) {
									if (object.getTagValue(REF).equals(ref)
											&& createTitle(object.getName()).equals(title)) {
										segmentList.add(object);
									}
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						});
				reader.searchMapIndex(sr);
				BinaryMapIndexReader.SearchRequest<Amenity> pointRequest = BinaryMapIndexReader.buildSearchPoiRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 15, getSearchFilter(ROUTE_TRACK_POINT),
						new ResultMatcher<Amenity>() {
							@Override
							public boolean publish(Amenity object) {
								if (object.getRouteId().equals(getRouteId())) {
									wayPointList.add(object);
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						});
				reader.searchPoi(pointRequest);
				if (!Algorithms.isEmpty(segmentList)) {
					break;
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		GPXFile gpxFile = null;
		if (!segmentList.isEmpty()) {
			Track track = new Track();
			for (BinaryMapDataObject segment : segmentList) {
				TrkSegment trkSegment = new TrkSegment();
				for (int i = 0; i < segment.getPointsLength(); i++) {
					WptPt point = new WptPt();
					point.lat = MapUtils.get31LatitudeY(segment.getPoint31YTile(i));
					point.lon = MapUtils.get31LongitudeX(segment.getPoint31XTile(i));
					trkSegment.points.add(point);
				}
				track.segments.add(trkSegment);
			}
			gpxFile = new GPXFile(getTitle(), getLang(), "");
			gpxFile.tracks = new ArrayList<>();
			gpxFile.tracks.add(track);
			gpxFile.setRef(ref);
		}
		if (!wayPointList.isEmpty()) {
			if (gpxFile == null) {
				gpxFile = new GPXFile(getTitle(), getLang(), "");
			}
			for (Amenity wayPoint : wayPointList) {
				WptPt wptPt = new WptPt();
				wptPt.lat = wayPoint.getLocation().getLatitude();
				wptPt.lon = wayPoint.getLocation().getLongitude();
				wptPt.name = wayPoint.getName();
				gpxFile.addPoint(wptPt);
			}
		}
		this.gpxFile = gpxFile;
		return gpxFile;
	}

	@NonNull
	public String createTitle(@NonNull String name) {
		return capitalizeFirstLetter(getGpxTitle(name));
	}
}