package net.osmand;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.GpxUtilities.Author;
import net.osmand.shared.gpx.GpxUtilities.Bounds;
import net.osmand.shared.gpx.GpxUtilities.Copyright;
import net.osmand.shared.gpx.GpxUtilities.Metadata;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.shared.gpx.GpxUtilities.Route;
import net.osmand.shared.gpx.GpxUtilities.RouteSegment;
import net.osmand.shared.gpx.GpxUtilities.RouteType;
import net.osmand.shared.gpx.GpxUtilities.Track;
import net.osmand.shared.gpx.GpxUtilities.TrkSegment;
import net.osmand.shared.gpx.GpxUtilities.WptPt;
import net.osmand.shared.io.KFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SharedUtil {

	public static KLatLon kLatLon(@NonNull LatLon latLon) {
		return new KLatLon(latLon.getLatitude(), latLon.getLongitude());
	}

	public static LatLon jLatLon(@NonNull KLatLon latLon) {
		return new LatLon(latLon.getLatitude(), latLon.getLongitude());
	}

	public static KQuadRect kQuadRect(@NonNull QuadRect rect) {
		return new KQuadRect(rect.left, rect.top, rect.right, rect.bottom);
	}

	public static QuadRect jQuadRect(@NonNull KQuadRect rect) {
		return new QuadRect(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
	}

	public static KFile kFile(@NonNull File file) {
		return new KFile(file.getAbsolutePath());
	}

	public static File jFile(@NonNull KFile file) {
		return new File(file.absolutePath());
	}

	public static GpxFile kGpxFile(@NonNull GPXFile gpxFile) {
		GpxFile kGpxFile = new GpxFile(gpxFile.author);
		if (gpxFile.metadata != null) {
			kGpxFile.setMetadata(kMetadata(gpxFile.metadata));
		}

		List<Track> kTracks = kGpxFile.getTracks();
		for (GPXUtilities.Track track : gpxFile.tracks) {
			kTracks.add(kTrack(track));
		}

		List<Route> kRoutes = kGpxFile.getRoutes();
		for (GPXUtilities.Route route : gpxFile.routes) {
			kRoutes.add(kRoute(route));
		}

		List<WptPt> kPoints = new ArrayList<>();
		for (GPXUtilities.WptPt point : gpxFile.getPoints()) {
			kPoints.add(kWptPt(point));
		}
		kGpxFile.setPointsList(kPoints);

		Map<String, PointsGroup> kPointsGroups = kGpxFile.getPointsGroups();
		for (Entry<String, GPXUtilities.PointsGroup> entry : gpxFile.getPointsGroups().entrySet()) {
			kPointsGroups.put(entry.getKey(), kPointsGroup(entry.getValue()));
		}

		kGpxFile.setPointsGroups(kPointsGroups);

		kGpxFile.addRouteKeyTags(gpxFile.getRouteKeyTags());

		return kGpxFile;
	}

	@NonNull
	private static Track kTrack(@NonNull GPXUtilities.Track track) {
		Track kTrack = new Track();
		kTrack.setName(track.name);
		kTrack.setDesc(track.desc);
		kTrack.setGeneralTrack(track.generalTrack);
		List<TrkSegment> kTrkSegments = new ArrayList<>();
		for (GPXUtilities.TrkSegment segment : track.segments) {
			kTrkSegments.add(kTrkSegment(segment));
		}
		kTrack.setSegments(kTrkSegments);
		copyExtensions(track, kTrack);
		return kTrack;
	}

	@NonNull
	private static Route kRoute(@NonNull GPXUtilities.Route route) {
		Route kRoute = new Route();
		kRoute.setName(route.name);
		kRoute.setDesc(route.desc);
		List<WptPt> kPoints = new ArrayList<>();
		for (GPXUtilities.WptPt point : route.points) {
			kPoints.add(kWptPt(point));
		}
		kRoute.setPoints(kPoints);
		copyExtensions(route, kRoute);
		return kRoute;
	}

	@NonNull
	private static PointsGroup kPointsGroup(@NonNull GPXUtilities.PointsGroup group) {
		PointsGroup kGroup = new PointsGroup(group.name, group.iconName, group.backgroundType, group.color);
		kGroup.setHidden(group.hidden);
		List<WptPt> kPoints = new ArrayList<>();
		for (GPXUtilities.WptPt point : group.points) {
			kPoints.add(kWptPt(point));
		}
		kGroup.setPoints(kPoints);
		return kGroup;
	}

	@NonNull
	private static TrkSegment kTrkSegment(@NonNull GPXUtilities.TrkSegment segment) {
		TrkSegment kSegment = new TrkSegment();
		kSegment.setName(segment.name);
		kSegment.setGeneralSegment(segment.generalSegment);
		List<WptPt> kPoints = new ArrayList<>();
		for (GPXUtilities.WptPt point : segment.points) {
			kPoints.add(kWptPt(point));
		}
		kSegment.setPoints(kPoints);
		List<RouteSegment> kRouteSegments = new ArrayList<>();
		for (GPXUtilities.RouteSegment rs : segment.routeSegments) {
			kRouteSegments.add(kRouteSegment(rs));
		}
		kSegment.setRouteSegments(kRouteSegments);
		List<RouteType> kRouteTypes = new ArrayList<>();
		for (GPXUtilities.RouteType rt : segment.routeTypes) {
			kRouteTypes.add(kRouteType(rt));
		}
		kSegment.setRouteTypes(kRouteTypes);
		copyExtensions(segment, kSegment);
		return kSegment;
	}

	private static WptPt kWptPt(GPXUtilities.WptPt point) {
		WptPt kPoint = new WptPt();
		kPoint.setFirstPoint(point.firstPoint);
		kPoint.setLastPoint(point.lastPoint);
		kPoint.setLat(point.lat);
		kPoint.setLon(point.lon);
		kPoint.setName(point.name);
		kPoint.setLink(point.link);
		kPoint.setCategory(point.category);
		kPoint.setDesc(point.desc);
		kPoint.setComment(point.comment);
		kPoint.setTime(point.time);
		kPoint.setEle(point.ele);
		kPoint.setSpeed(point.speed);
		kPoint.setHdop(point.hdop);
		kPoint.setHeading(point.heading);
		kPoint.setBearing(point.bearing);
		kPoint.setDeleted(point.deleted);
		kPoint.setSpeedColor(point.speedColor);
		kPoint.setAltitudeColor(point.altitudeColor);
		kPoint.setSlopeColor(point.slopeColor);
		kPoint.setColourARGB(point.colourARGB);
		kPoint.setDistance(point.distance);
		copyExtensions(point, kPoint);
		return kPoint;
	}

	@NonNull
	private static RouteSegment kRouteSegment(@NonNull GPXUtilities.RouteSegment rs) {
		RouteSegment kRs = new RouteSegment();
		kRs.setId(rs.id);
		kRs.setLength(rs.length);
		kRs.setStartTrackPointIndex(rs.startTrackPointIndex);
		kRs.setSegmentTime(rs.segmentTime);
		kRs.setSpeed(rs.speed);
		kRs.setTurnType(rs.turnType);
		kRs.setTurnLanes(rs.turnLanes);
		kRs.setTurnAngle(rs.turnAngle);
		kRs.setSkipTurn(rs.skipTurn);
		kRs.setTypes(rs.types);
		kRs.setPointTypes(rs.pointTypes);
		kRs.setNames(rs.names);
		return kRs;
	}

	@NonNull
	private static RouteType kRouteType(@NonNull GPXUtilities.RouteType rt) {
		RouteType kRt = new RouteType();
		kRt.setTag(rt.tag);
		kRt.setValue(rt.value);
		return kRt;
	}

	@NonNull
	private static Metadata kMetadata(@NonNull GPXUtilities.Metadata metadata) {
		Metadata kMetadata = new Metadata();
		kMetadata.setName(metadata.name);
		kMetadata.setLink(metadata.link);
		kMetadata.setKeywords(metadata.keywords);
		kMetadata.setTime(metadata.time);
		if (metadata.author != null) {
			kMetadata.setAuthor(kAuthor(metadata.author));
		}
		if (metadata.copyright != null) {
			kMetadata.setCopyright(kCopyright(metadata.copyright));
		}
		if (metadata.bounds != null) {
			kMetadata.setBounds(kBounds(metadata.bounds));
		}
		copyExtensions(metadata, kMetadata);
		return kMetadata;
	}

	@NonNull
	private static Author kAuthor(@NonNull GPXUtilities.Author author) {
		Author kAuthor = new Author();
		kAuthor.setName(author.name);
		kAuthor.setLink(author.link);
		kAuthor.setEmail(author.email);
		copyExtensions(author, kAuthor);
		return kAuthor;
	}

	@NonNull
	private static Copyright kCopyright(@NonNull GPXUtilities.Copyright copyright) {
		Copyright kCopyright = new Copyright();
		kCopyright.setAuthor(copyright.author);
		kCopyright.setYear(copyright.year);
		kCopyright.setLicense(copyright.license);
		copyExtensions(copyright, kCopyright);
		return kCopyright;
	}

	@NonNull
	private static Bounds kBounds(@NonNull GPXUtilities.Bounds bounds) {
		Bounds kBounds = new Bounds();
		kBounds.setMinlat(bounds.minlat);
		kBounds.setMinlon(bounds.minlon);
		kBounds.setMaxlat(bounds.maxlat);
		kBounds.setMaxlon(bounds.maxlon);
		copyExtensions(bounds, kBounds);
		return kBounds;
	}

	private static void copyExtensions(@NonNull GPXUtilities.GPXExtensions source,
	                                   @NonNull GpxUtilities.GpxExtensions destination) {
		Map<String, String> extensionsToRead = source.getExtensionsToRead();
		if (!extensionsToRead.isEmpty()) {
			destination.getExtensionsToWrite().putAll(extensionsToRead);
		}
	}

}
