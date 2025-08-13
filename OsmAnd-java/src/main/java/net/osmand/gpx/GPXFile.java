package net.osmand.gpx;

import static net.osmand.gpx.GPXUtilities.PointsGroup.DEFAULT_WPT_GROUP_NAME;

import net.osmand.data.QuadRect;
import net.osmand.gpx.GPXTrackAnalysis.TrackPointsAnalyser;
import net.osmand.gpx.GPXUtilities.Route;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Deprecated
public class GPXFile extends GPXUtilities.GPXExtensions {

	public String author;
	public GPXUtilities.Metadata metadata = new GPXUtilities.Metadata();
	public List<GPXUtilities.Track> tracks = new ArrayList<>();
	public List<GPXUtilities.Route> routes = new ArrayList<>();

	final List<GPXUtilities.WptPt> points = new ArrayList<>();
	Map<String, GPXUtilities.PointsGroup> pointsGroups = new LinkedHashMap<>();
	final Map<String, String> networkRouteKeyTags = new LinkedHashMap<>();

	public Exception error = null;
	public String path = "";
	public boolean showCurrentTrack;
	public boolean hasAltitude;
	public long modifiedTime = 0;
	public long pointsModifiedTime = 0;

	private GPXUtilities.Track generalTrack;
	private GPXUtilities.TrkSegment generalSegment;

	public GPXFile(String author) {
		this.author = author;
	}

	public GPXFile(String title, String lang, String description) {
		if (description != null) {
			metadata.desc = description;
		}
		if (lang != null) {
			metadata.getExtensionsToWrite().put("article_lang", lang);
		}
		if (title != null) {
			metadata.getExtensionsToWrite().put("article_title", title);
		}
	}

	public boolean hasRoute() {
		return getNonEmptyTrkSegments(true).size() > 0;
	}

	public List<GPXUtilities.WptPt> getAllPoints() {
		List<GPXUtilities.WptPt> total = new ArrayList<>();
		total.addAll(getPoints());
		total.addAll(getAllSegmentsPoints());
		return total;
	}

	public List<GPXUtilities.WptPt> getPoints() {
		return Collections.unmodifiableList(points);
	}

	public List<GPXUtilities.WptPt> getAllSegmentsPoints() {
		List<GPXUtilities.WptPt> points = new ArrayList<>();
		for (GPXUtilities.Track track : tracks) {
			if (track.generalTrack) continue;
			for (GPXUtilities.TrkSegment segment : track.segments) {
				if (segment.generalSegment) continue;
				points.addAll(segment.points);
			}
		}
		return points;
	}

	public boolean isPointsEmpty() {
		return points.isEmpty();
	}

	public int getPointsSize() {
		return points.size();
	}

	public boolean containsPoint(GPXUtilities.WptPt point) {
		return points.contains(point);
	}

	public void clearPoints() {
		points.clear();
		pointsGroups.clear();
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;
	}

	public void addParsedPoint(GPXUtilities.WptPt point) {
		points.add(point);
	}

	public void addPoint(GPXUtilities.WptPt point) {
		points.add(point);
		addPointsToGroups(Collections.singleton(point));
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;
	}

	public void addPoint(int position, GPXUtilities.WptPt point) {
		points.add(position, point);
		addPointsToGroups(Collections.singleton(point));
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;
	}

	public void addPoints(Collection<? extends GPXUtilities.WptPt> collection) {
		points.addAll(collection);
		addPointsToGroups(collection);
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;
	}

	public void addPointsGroup(GPXUtilities.PointsGroup group) {
		points.addAll(group.points);
		pointsGroups.put(group.name, group);
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;
	}

	public void setPointsGroups(Map<String, GPXUtilities.PointsGroup> groups) {
		pointsGroups = groups;
	}

	private void addPointsToGroups(Collection<? extends GPXUtilities.WptPt> collection) {
		for (GPXUtilities.WptPt point : collection) {
			GPXUtilities.PointsGroup pointsGroup = getOrCreateGroup(point);
			pointsGroup.points.add(point);
		}
	}

	private GPXUtilities.PointsGroup getOrCreateGroup(GPXUtilities.WptPt point) {
		if (point.category == null && pointsGroups.containsKey(DEFAULT_WPT_GROUP_NAME)) {
			return pointsGroups.get(DEFAULT_WPT_GROUP_NAME);
		}
		if (pointsGroups.containsKey(point.category)) {
			return pointsGroups.get(point.category);
		}
		GPXUtilities.PointsGroup pointsGroup = new GPXUtilities.PointsGroup(point);

		pointsGroups.put(pointsGroup.name, pointsGroup);

		return pointsGroup;
	}

	public boolean deleteWptPt(GPXUtilities.WptPt point) {
		removePointFromGroup(point);
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;

		return points.remove(point);
	}

	public boolean deleteWptPt(String wptName, int index) {
		GPXUtilities.WptPt currentWpt = getWptPt(wptName, index);
		if (currentWpt != null) {
			return deleteWptPt(currentWpt);
		}
		return false;
	}

	private void removePointFromGroup(GPXUtilities.WptPt point) {
		removePointFromGroup(point, point.category);
	}

	private void removePointFromGroup(GPXUtilities.WptPt point, String groupName) {
		GPXUtilities.PointsGroup group = pointsGroups.get(groupName);
		if (group != null) {
			group.points.remove(point);
		}
	}

	public void updateWptPt(String wptName, int wptIndex, GPXUtilities.WptPt newWpt, boolean updateTimestamp) {
		GPXUtilities.WptPt currentWpt = getWptPt(wptName, wptIndex);
		if (currentWpt != null) {
			updateWptPt(currentWpt, newWpt, updateTimestamp);
		} else {
			addPoint(newWpt);
		}
	}

	private GPXUtilities.WptPt getWptPt(String wptName, int wptIndex) {
		GPXUtilities.WptPt currentWpt = null;
		if (wptIndex < points.size() && wptIndex >= 0) {
			currentWpt = points.get(wptIndex);
			if (!currentWpt.name.equals(wptName)) {
				currentWpt = null;
			}
		}
		return currentWpt;
	}

	public void updateWptPt(GPXUtilities.WptPt existingPoint, GPXUtilities.WptPt newWpt, boolean updateTimestamp) {
		int index = points.indexOf(existingPoint);
		if (index == -1) {
			return;
		}
		String prevGroupName = existingPoint.category == null ? DEFAULT_WPT_GROUP_NAME : existingPoint.category;
		long prevTime = existingPoint.time;
		existingPoint.updatePoint(newWpt);
		if (!updateTimestamp) {
			existingPoint.time = prevTime;
		}
		if (Algorithms.stringsEqual(newWpt.category, prevGroupName)
				|| Algorithms.isEmpty(newWpt.category) && Algorithms.isEmpty(prevGroupName)) {
			removePointFromGroup(existingPoint, prevGroupName);
			GPXUtilities.PointsGroup pointsGroup = getOrCreateGroup(existingPoint);
			pointsGroup.points.add(existingPoint);
		}
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;
	}

	public void updateWptPt(GPXUtilities.WptPt existingPoint, GPXUtilities.WptPt newWpt) {
		updateWptPt(existingPoint, newWpt, true);
	}

	public void updatePointsGroup(String prevGroupName, GPXUtilities.PointsGroup pointsGroup) {
		pointsGroups.remove(prevGroupName);
		pointsGroups.put(pointsGroup.name, pointsGroup);
		modifiedTime = System.currentTimeMillis();
	}

	public boolean isCloudmadeRouteFile() {
		return "cloudmade".equalsIgnoreCase(author);
	}

	public boolean hasGeneralTrack() {
		return generalTrack != null;
	}

	public void addGeneralTrack() {
		GPXUtilities.Track generalTrack = getGeneralTrack();
		if (generalTrack != null && !tracks.contains(generalTrack)) {
			tracks.add(0, generalTrack);
		}
	}

	public GPXUtilities.Track getGeneralTrack() {
		GPXUtilities.TrkSegment generalSegment = getGeneralSegment();
		if (generalTrack == null && generalSegment != null) {
			GPXUtilities.Track track = new GPXUtilities.Track();
			track.segments = new ArrayList<>();
			track.segments.add(generalSegment);
			generalTrack = track;
			track.generalTrack = true;
		}
		return generalTrack;
	}

	public GPXUtilities.TrkSegment getGeneralSegment() {
		if (generalSegment == null && getNonEmptySegmentsCount() > 1) {
			buildGeneralSegment();
		}
		return generalSegment;
	}

	private void buildGeneralSegment() {
		TrkSegment segment = new TrkSegment();
		for (Track track : tracks) {
			for (TrkSegment trkSegment : track.segments) {
				if (trkSegment.points.size() > 0) {
					List<WptPt> waypoints = new ArrayList<>(trkSegment.points.size());
					for (WptPt wptPt : trkSegment.points) {
						waypoints.add(new WptPt(wptPt));
					}
					waypoints.get(0).firstPoint = true;
					waypoints.get(waypoints.size() - 1).lastPoint = true;
					segment.points.addAll(waypoints);
				}
			}
		}
		if (segment.points.size() > 0) {
			segment.generalSegment = true;
			generalSegment = segment;
		}
	}

	public GPXTrackAnalysis getAnalysis(long fileTimestamp) {
		return getAnalysis(fileTimestamp, null, null, null);
	}

	public GPXTrackAnalysis getAnalysis(long fileTimestamp, Double fromDistance, Double toDistance, TrackPointsAnalyser pointsAnalyzer) {
		GPXTrackAnalysis analysis = new GPXTrackAnalysis();
		analysis.name = path;
		analysis.setWptPoints(points.size());
		analysis.setWptCategoryNames(getWaypointCategories());

		List<SplitSegment> segments = getSplitSegments(analysis, fromDistance, toDistance);
		analysis.prepareInformation(fileTimestamp, pointsAnalyzer, segments.toArray(new SplitSegment[0]));
		return analysis;
	}

	private List<SplitSegment> getSplitSegments(GPXTrackAnalysis analysis, Double fromDistance, Double toDistance) {
		List<SplitSegment> splitSegments = new ArrayList<>();
		for (int i = 0; i < tracks.size(); i++) {
			GPXUtilities.Track subtrack = tracks.get(i);
			for (GPXUtilities.TrkSegment segment : subtrack.segments) {
				if (!segment.generalSegment) {
					int totalTracks = analysis.getTotalTracks();
					analysis.setTotalTracks(totalTracks + 1);
					if (segment.points.size() > 1) {
						splitSegments.add(createSplitSegment(segment, fromDistance, toDistance));
					}
				}
			}
		}
		return splitSegments;
	}

	private SplitSegment createSplitSegment(TrkSegment segment, Double fromDistance, Double toDistance) {
		if (fromDistance != null && toDistance != null) {
			int startInd = getPointIndexByDistance(segment.points, fromDistance);
			int endInd = getPointIndexByDistance(segment.points, toDistance);
			return new SplitSegment(startInd, endInd, segment);
		} else {
			return new SplitSegment(segment);
		}
	}

	public int getPointIndexByDistance(List<GPXUtilities.WptPt> points, double distance) {
		int index = 0;
		double minDistanceChange = Double.MAX_VALUE;
		for (int i = 0; i < points.size(); i++) {
			GPXUtilities.WptPt point = points.get(i);
			double currentDistanceChange = Math.abs(point.distance - distance);
			if (currentDistanceChange < minDistanceChange) {
				minDistanceChange = currentDistanceChange;
				index = i;
			}
		}
		return index;
	}

	public boolean containsRoutePoint(GPXUtilities.WptPt point) {
		return getRoutePoints().contains(point);
	}

	public List<GPXUtilities.WptPt> getRoutePoints() {
		List<GPXUtilities.WptPt> points = new ArrayList<>();
		for (int i = 0; i < routes.size(); i++) {
			GPXUtilities.Route rt = routes.get(i);
			points.addAll(rt.points);
		}
		return points;
	}

	public List<GPXUtilities.WptPt> getRoutePoints(int routeIndex) {
		List<GPXUtilities.WptPt> points = new ArrayList<>();
		if (routes.size() > routeIndex) {
			GPXUtilities.Route rt = routes.get(routeIndex);
			points.addAll(rt.points);
		}
		return points;
	}

	public boolean isAttachedToRoads() {
		List<GPXUtilities.WptPt> points = getRoutePoints();
		if (!Algorithms.isEmpty(points)) {
			for (GPXUtilities.WptPt wptPt : points) {
				if (Algorithms.isEmpty(wptPt.getProfileType())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean hasRtePt() {
		for (GPXUtilities.Route r : routes) {
			if (r.points.size() > 0) {
				return true;
			}
		}
		return false;
	}

	public boolean hasWptPt() {
		return points.size() > 0;
	}

	public boolean hasTrkPt() {
		for (GPXUtilities.Track t : tracks) {
			for (GPXUtilities.TrkSegment ts : t.segments) {
				if (ts.points.size() > 0) {
					return true;
				}
			}
		}
		return false;
	}

	public List<GPXUtilities.TrkSegment> getNonEmptyTrkSegments(boolean routesOnly) {
		List<GPXUtilities.TrkSegment> segments = new ArrayList<>();
		for (GPXUtilities.Track t : tracks) {
			for (GPXUtilities.TrkSegment s : t.segments) {
				if (!s.generalSegment && s.points.size() > 0 && (!routesOnly || s.hasRoute())) {
					segments.add(s);
				}
			}
		}
		return segments;
	}

	public void addTrkSegment(List<GPXUtilities.WptPt> points) {
		removeGeneralTrackIfExists();

		GPXUtilities.TrkSegment segment = new GPXUtilities.TrkSegment();
		segment.points.addAll(points);

		if (tracks.size() == 0) {
			tracks.add(new GPXUtilities.Track());
		}
		GPXUtilities.Track lastTrack = tracks.get(tracks.size() - 1);
		lastTrack.segments.add(segment);

		modifiedTime = System.currentTimeMillis();
	}

	public boolean replaceSegment(GPXUtilities.TrkSegment oldSegment, GPXUtilities.TrkSegment newSegment) {
		removeGeneralTrackIfExists();

		for (int i = 0; i < tracks.size(); i++) {
			GPXUtilities.Track currentTrack = tracks.get(i);
			for (int j = 0; j < currentTrack.segments.size(); j++) {
				int segmentIndex = currentTrack.segments.indexOf(oldSegment);
				if (segmentIndex != -1) {
					currentTrack.segments.remove(segmentIndex);
					currentTrack.segments.add(segmentIndex, newSegment);
					addGeneralTrack();
					modifiedTime = System.currentTimeMillis();
					return true;
				}
			}
		}

		addGeneralTrack();
		return false;
	}

	public void addRoutePoints(List<GPXUtilities.WptPt> points, boolean addRoute) {
		if (routes.size() == 0 || addRoute) {
			GPXUtilities.Route route = new GPXUtilities.Route();
			routes.add(route);
		}

		GPXUtilities.Route lastRoute = routes.get(routes.size() - 1);
		lastRoute.points.addAll(points);
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;
	}

	public void replaceRoutePoints(List<GPXUtilities.WptPt> points) {
		routes.clear();
		routes.add(new GPXUtilities.Route());
		GPXUtilities.Route currentRoute = routes.get(routes.size() - 1);
		currentRoute.points.addAll(points);
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;
	}

	private void removeGeneralTrackIfExists() {
		if (generalTrack != null) {
			tracks.remove(generalTrack);
			this.generalTrack = null;
			this.generalSegment = null;
		}
	}

	public boolean removeTrkSegment(GPXUtilities.TrkSegment segment) {
		removeGeneralTrackIfExists();

		for (int i = 0; i < tracks.size(); i++) {
			GPXUtilities.Track currentTrack = tracks.get(i);
			for (int j = 0; j < currentTrack.segments.size(); j++) {
				if (currentTrack.segments.remove(segment)) {
					addGeneralTrack();
					modifiedTime = System.currentTimeMillis();
					return true;
				}
			}
		}
		addGeneralTrack();
		return false;
	}

	public boolean deleteRtePt(GPXUtilities.WptPt pt) {
		modifiedTime = System.currentTimeMillis();
		pointsModifiedTime = modifiedTime;
		for (GPXUtilities.Route route : routes) {
			if (route.points.remove(pt)) {
				return true;
			}
		}
		return false;
	}

	public List<GPXUtilities.TrkSegment> processRoutePoints() {
		List<GPXUtilities.TrkSegment> tpoints = new ArrayList<GPXUtilities.TrkSegment>();
		if (routes.size() > 0) {
			for (GPXUtilities.Route r : routes) {
				int routeColor = r.getColor(getColor(0));
				if (r.points.size() > 0) {
					GPXUtilities.TrkSegment sgmt = new GPXUtilities.TrkSegment();
					tpoints.add(sgmt);
					sgmt.points.addAll(r.points);
					sgmt.setColor(routeColor);
				}
			}
		}
		return tpoints;
	}

	public List<GPXUtilities.TrkSegment> proccessPoints() {
		List<GPXUtilities.TrkSegment> tpoints = new ArrayList<GPXUtilities.TrkSegment>();
		for (GPXUtilities.Track t : tracks) {
			int trackColor = t.getColor(getColor(0));
			for (GPXUtilities.TrkSegment ts : t.segments) {
				if (!ts.generalSegment && ts.points.size() > 0) {
					GPXUtilities.TrkSegment sgmt = new GPXUtilities.TrkSegment();
					tpoints.add(sgmt);
					sgmt.points.addAll(ts.points);
					sgmt.setColor(trackColor);
				}
			}
		}
		return tpoints;
	}

	public GPXUtilities.WptPt getLastPoint() {
		if (tracks.size() > 0) {
			GPXUtilities.Track tk = tracks.get(tracks.size() - 1);
			if (tk.segments.size() > 0) {
				GPXUtilities.TrkSegment ts = tk.segments.get(tk.segments.size() - 1);
				if (ts.points.size() > 0) {
					return ts.points.get(ts.points.size() - 1);
				}
			}
		}
		return null;
	}

	public WptPt findPointToShow() {
		for (Track track : tracks) {
			for (TrkSegment segment : track.segments) {
				if (segment.points.size() > 0) {
					return segment.points.get(0);
				}
			}
		}
		for (Route route : routes) {
			if (route.points.size() > 0) {
				return route.points.get(0);
			}
		}
		if (points.size() > 0) {
			return points.get(0);
		}
		return null;
	}

	public boolean isEmpty() {
		for (GPXUtilities.Track t : tracks) {
			if (t.segments != null) {
				for (GPXUtilities.TrkSegment s : t.segments) {
					boolean tracksEmpty = s.points.isEmpty();
					if (!tracksEmpty) {
						return false;
					}
				}
			}
		}
		return points.isEmpty() && routes.isEmpty();
	}

	public List<GPXUtilities.Track> getTracks(boolean includeGeneralTrack) {
		List<GPXUtilities.Track> tracks = new ArrayList<>();
		for (GPXUtilities.Track track : this.tracks) {
			if (includeGeneralTrack || !track.generalTrack) {
				tracks.add(track);
			}
		}
		return tracks;
	}

	public List<TrkSegment> getSegments(boolean includeGeneralTrack) {
		List<TrkSegment> segments = new ArrayList<>();
		for (Track track : tracks) {
			if (includeGeneralTrack || !track.generalTrack) {
				segments.addAll(track.segments);
			}
		}
		return segments;
	}

	public int getTracksCount() {
		int count = 0;
		for (GPXUtilities.Track track : tracks) {
			if (!track.generalTrack) {
				count++;
			}
		}
		return count;
	}

	public int getNonEmptyTracksCount() {
		int count = 0;
		for (GPXUtilities.Track track : tracks) {
			for (GPXUtilities.TrkSegment segment : track.segments) {
				if (segment.points.size() > 0) {
					count++;
					break;
				}
			}
		}
		return count;
	}

	public int getNonEmptySegmentsCount() {
		int count = 0;
		for (GPXUtilities.Track t : tracks) {
			for (GPXUtilities.TrkSegment s : t.segments) {
				if (s.points.size() > 0) {
					count++;
				}
			}
		}
		return count;
	}

	public Set<String> getWaypointCategories() {
		return new HashSet<>(pointsGroups.keySet());
	}

	public Map<String, GPXUtilities.PointsGroup> getPointsGroups() {
		return pointsGroups;
	}

	public List<Route> getRoutes() {
		return routes;
	}

	public Route getRouteByName(String name) {
		for (Route route : getRoutes()) {
			if (Algorithms.stringsEqual(route.name, name)) {
				return route;
			}
		}
		return null;
	}

	public QuadRect getRect() {
		return getBounds(0, 0);
	}

	public QuadRect getBounds(double defaultMissingLat, double defaultMissingLon) {
		QuadRect qr = new QuadRect(defaultMissingLon, defaultMissingLat, defaultMissingLon, defaultMissingLat);
		for (GPXUtilities.Track track : tracks) {
			for (GPXUtilities.TrkSegment segment : track.segments) {
				for (GPXUtilities.WptPt p : segment.points) {
					GPXUtilities.updateQR(qr, p, defaultMissingLat, defaultMissingLon);
				}
			}
		}
		for (GPXUtilities.WptPt p : points) {
			GPXUtilities.updateQR(qr, p, defaultMissingLat, defaultMissingLon);
		}
		for (GPXUtilities.Route route : routes) {
			for (GPXUtilities.WptPt p : route.points) {
				GPXUtilities.updateQR(qr, p, defaultMissingLat, defaultMissingLon);
			}
		}
		return qr;
	}

	public String getColoringType() {
		if (extensions != null) {
			return extensions.get("coloring_type");
		}
		return null;
	}

	public String getGradientScaleType() {
		if (extensions != null) {
			return extensions.get("gradient_scale_type");
		}
		return null;
	}

	public String getGradientColorPalette() {
		if (extensions != null) {
			return extensions.get("color_palette");
		}
		return null;
	}

	public void setGradientColorPalette(String gradientColorPaletteName) {
		getExtensionsToWrite().put("color_palette", gradientColorPaletteName);
	}

	public void setColoringType(String coloringType) {
		getExtensionsToWrite().put("coloring_type", coloringType);
	}

	public void removeGradientScaleType() {
		getExtensionsToWrite().remove("gradient_scale_type");
	}

	public String getSplitType() {
		if (extensions != null) {
			return extensions.get("split_type");
		}
		return null;
	}

	public void setSplitType(String gpxSplitType) {
		getExtensionsToWrite().put("split_type", gpxSplitType);
	}

	public double getSplitInterval() {
		if (extensions != null) {
			String splitIntervalStr = extensions.get("split_interval");
			if (!Algorithms.isEmpty(splitIntervalStr)) {
				try {
					return Double.parseDouble(splitIntervalStr);
				} catch (NumberFormatException e) {
					GPXUtilities.log.error("Error reading split_interval", e);
				}
			}
		}
		return 0;
	}

	public void setSplitInterval(double splitInterval) {
		getExtensionsToWrite().put("split_interval", String.valueOf(splitInterval));
	}

	public String getWidth(String defWidth) {
		String widthValue = null;
		if (extensions != null) {
			widthValue = extensions.get("width");
		}
		return widthValue != null ? widthValue : defWidth;
	}

	public void setWidth(String width) {
		getExtensionsToWrite().put("width", width);
	}

	public boolean isShowArrowsSet() {
		return extensions != null && extensions.containsKey("show_arrows");
	}

	public boolean isShowArrows() {
		String showArrows = null;
		if (extensions != null) {
			showArrows = extensions.get("show_arrows");
		}
		return Boolean.parseBoolean(showArrows);
	}

	public void setShowArrows(boolean showArrows) {
		getExtensionsToWrite().put("show_arrows", String.valueOf(showArrows));
	}

	public String get3DVisualizationType() {
		return extensions == null ? null : extensions.get("line_3d_visualization_by_type");
	}

	public void set3DVisualizationType(String visualizationType) {
		getExtensionsToWrite().put("line_3d_visualization_by_type", String.valueOf(visualizationType));
	}

	public String get3DWallColoringType() {
		return extensions == null ? null : extensions.get("line_3d_visualization_wall_color_type");
	}

	public void set3DWallColoringType(String trackWallColoringType) {
		getExtensionsToWrite().put("line_3d_visualization_wall_color_type", String.valueOf(trackWallColoringType));
	}

	public String get3DLinePositionType() {
		return extensions == null ? null : extensions.get("line_3d_visualization_position_type");
	}

	public void set3DLinePositionType(String trackLinePositionType) {
		getExtensionsToWrite().put("line_3d_visualization_position_type", String.valueOf(trackLinePositionType));
	}

	public void setAdditionalExaggeration(float additionalExaggeration) {
		getExtensionsToWrite().put("vertical_exaggeration_scale", String.valueOf(additionalExaggeration));
	}

	public float getAdditionalExaggeration() {
		String exaggeration = getExtensionsToRead().get("vertical_exaggeration_scale");
		return Algorithms.parseFloatSilently(exaggeration, 1f);
	}

	public void setElevationMeters(float elevation) {
		getExtensionsToWrite().put("elevation_meters", String.valueOf(elevation));
	}

	public float getElevationMeters() {
		String elevation = getExtensionsToRead().get("elevation_meters");
		return Algorithms.parseFloatSilently(elevation, 1000f);
	}

	public boolean isShowStartFinishSet() {
		return extensions != null && extensions.containsKey("show_start_finish");
	}

	public boolean isShowStartFinish() {
		if (extensions != null && extensions.containsKey("show_start_finish")) {
			return Boolean.parseBoolean(extensions.get("show_start_finish"));
		}
		return true;
	}

	public void setShowStartFinish(boolean showStartFinish) {
		getExtensionsToWrite().put("show_start_finish", String.valueOf(showStartFinish));
	}

	public void addRouteKeyTags(Map<String, String> routeKey) {
		networkRouteKeyTags.putAll(routeKey);
	}

	public Map<String, String> getRouteKeyTags() {
		return networkRouteKeyTags;
	}

	public void setRef(String ref) {
		getExtensionsToWrite().put("ref", ref);
	}

	public String getRef() {
		if (extensions != null) {
			return extensions.get("ref");
		}
		return null;
	}

	public String getOuterRadius() {
		QuadRect rect = getRect();
		int radius = (int) MapUtils.getDistance(rect.bottom, rect.left, rect.top, rect.right);
		return MapUtils.convertDistToChar(radius, GPXUtilities.TRAVEL_GPX_CONVERT_FIRST_LETTER, GPXUtilities.TRAVEL_GPX_CONVERT_FIRST_DIST,
				GPXUtilities.TRAVEL_GPX_CONVERT_MULT_1, GPXUtilities.TRAVEL_GPX_CONVERT_MULT_2);
	}

	public String getArticleTitle() {
		return metadata.getArticleTitle();
	}

	int getItemsToWriteSize() {
		int size = getPointsSize();
		for (GPXUtilities.Route route : routes) {
			size += route.points.size();
		}
		for (GPXUtilities.TrkSegment segment : getNonEmptyTrkSegments(false)) {
			size += segment.points.size();
		}

		// metadata
		size++;
		if (metadata.author != null) {
			size++;
		}
		if (metadata.copyright != null) {
			size++;
		}
		if (metadata.bounds != null) {
			size++;
		}
		size += getExtensionsToWrite().size();
		size += getExtensionsWriters().size();

		return size;
	}

	public long getLastPointTime() {
		long time = getLastPointTime(getAllSegmentsPoints());
		if (time == 0) {
			time = getLastPointTime(getRoutePoints());
		}
		if (time == 0) {
			time = getLastPointTime(getPoints());
		}
		return time;
	}

	private long getLastPointTime(List<WptPt> points) {
		for (int i = points.size() - 1; i >= 0; i--) {
			WptPt point = points.get(i);
			if (point.time > 0) {
				return point.time;
			}
		}
		return 0;
	}
}
