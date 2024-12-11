package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.Pair;

import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.plus.measurementtool.RoadSegmentData;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import gnu.trove.list.array.TByteArrayList;

public class MultiProfileGeometryWay extends GeometryWay<MultiProfileGeometryWayContext, MultiProfileGeometryWayDrawer> {

	private static final String DEFAULT_PROFILE_KEY = ApplicationMode.DEFAULT.getStringKey();

	private Map<Pair<WptPt, WptPt>, RoadSegmentData> segmentData;
	private List<TrkSegment> beforeSegments;
	private List<TrkSegment> afterSegments;

	public MultiProfileGeometryWay(MultiProfileGeometryWayContext context) {
		super(context, new MultiProfileGeometryWayDrawer(context));
	}

	public void drawSegments(Canvas canvas, RotatedTileBox tileBox) {
		QuadRect bounds = tileBox.getLatLonBounds();
		drawSegments(tileBox, canvas, bounds.top, bounds.left, bounds.bottom, bounds.right, null, 0);
	}

	public boolean updateRoute(RotatedTileBox tileBox, Map<Pair<WptPt, WptPt>, RoadSegmentData> segmentData,
							List<TrkSegment> beforeSegments, List<TrkSegment> afterSegments) {
		boolean shouldUpdateRoute = tileBox.getMapDensity() != getMapDensity() || segmentDataChanged(segmentData)
				|| this.beforeSegments != beforeSegments || this.afterSegments != afterSegments || getLocationProvider() == null;
		if (shouldUpdateRoute) {
			this.segmentData = segmentData;
			this.beforeSegments = beforeSegments;
			this.afterSegments = afterSegments;

			List<Location> locations;
			Map<Integer, GeometryWayStyle<?>> styleMap;
			List<Way> ways = new ArrayList<>();
			List<GeometryWayStyle<?>> styles = new ArrayList<>();
			locations = new ArrayList<>();

			List<TrkSegment> allSegments = new ArrayList<>();
			allSegments.addAll(beforeSegments);
			allSegments.addAll(afterSegments);
			setStyles(allSegments, ways, styles);

			styleMap = new TreeMap<>();
			int i = 0;
			int k = 0;
			if (ways.size() > 0) {
				for (Way w : ways) {
					styleMap.put(k, styles.get(i++));
					for (Node n : w.getNodes()) {
						Location ln = new Location("");
						ln.setLatitude(n.getLatitude());
						ln.setLongitude(n.getLongitude());
						locations.add(ln);
						k++;
					}
				}
			}

			updateWay(locations, styleMap, tileBox);
		}
		return shouldUpdateRoute;
	}

	@Override
	public void clearWay() {
		super.clearWay();
		if (segmentData != null) {
			segmentData.clear();
		}
	}

	private void setStyles(List<TrkSegment> segments, List<Way> ways, List<GeometryWayStyle<?>> styles) {
		for (TrkSegment segment : segments) {
			List<WptPt> points = segment.getPoints();
			for (int i = 0; i < points.size() - 1; i++) {
				setStylesInternal(points, i, ways, styles);
			}
			styles.add(new GeometryMultiProfileWayStyle(getContext(), new ArrayList<>(), 0, 0, true));
			Way way = new Way(-1);
			WptPt last = points.get(points.size() - 1);
			way.addNode(new Node(last.getLat(), last.getLon(), -1));
			ways.add(way);
		}
	}

	private void setStylesInternal(List<WptPt> points, int idx, List<Way> ways, List<GeometryWayStyle<?>> styles) {
		WptPt startPt = points.get(idx);
		WptPt endPt = points.get(idx + 1);
		List<LatLon> routePoints = getRoutePoints(startPt, endPt, segmentData);
		boolean isSecondToLast = idx + 2 == points.size();

		Way way = new Way(-1);
		String currProfileKey = getProfileKey(startPt);
		Pair<Integer, Integer> profileData = getProfileData(currProfileKey);
		GeometryMultiProfileWayStyle style = new GeometryMultiProfileWayStyle(
				getContext(), routePoints, profileData.first, profileData.second);
		styles.add(style);
		ways.add(way);

		for (LatLon routePt : routePoints) {
			if (isSecondToLast || routePt.getLatitude() != endPt.getLatitude()
					&& routePt.getLongitude() != endPt.getLongitude()) {
				way.addNode(new Node(routePt.getLatitude(), routePt.getLongitude(), -1));
			}
		}
	}

	public static List<LatLon> getRoutePoints(WptPt start, WptPt end,
	                                          Map<Pair<WptPt, WptPt>, RoadSegmentData> segmentData) {
		Pair<WptPt, WptPt> userLine = new Pair<>(start, end);
		RoadSegmentData roadSegmentData = segmentData.get(userLine);
		List<LatLon> routePoints = new ArrayList<>();

		if (roadSegmentData == null || Algorithms.isEmpty(roadSegmentData.getPoints())) {
			routePoints.add(new LatLon(start.getLat(), start.getLon()));
			routePoints.add(new LatLon(end.getLat(), end.getLon()));
		} else {
			List<WptPt> points = roadSegmentData.getPoints();
			if (points.get(0).getLatitude() != start.getLatitude() && points.get(0).getLongitude() != start.getLongitude()) {
				routePoints.add(new LatLon(start.getLat(), start.getLon()));
			}
			for (WptPt routePt : roadSegmentData.getPoints()) {
				routePoints.add(new LatLon(routePt.getLat(), routePt.getLon()));
			}
			int lastIdx = routePoints.size() - 1;
			if (routePoints.get(lastIdx).getLatitude() != end.getLatitude()
					&& routePoints.get(lastIdx).getLongitude() != end.getLongitude()) {
				routePoints.add(new LatLon(end.getLat(), end.getLon()));
			}
		}
		return routePoints;
	}

	@Override
	protected boolean shouldAddLocation(@Nullable TByteArrayList simplification, double leftLon, double rightLon,
	                                    double bottomLat, double topLat, GeometryWayProvider provider,
	                                    int previousVisible, int currLocationIdx) {
		if (hasMapRenderer()) {
			return true;
		}
		double currLat = provider.getLatitude(currLocationIdx);
		double currLon = provider.getLongitude(currLocationIdx);

		int nextSurvivedIdx = currLocationIdx;
		if (simplification != null) {
			for (int i = nextSurvivedIdx + 1; i < simplification.size(); i++) {
				if (simplification.getQuick(i) == 1) {
					nextSurvivedIdx = i;
					break;
				}
			}
		} else if (provider.getSize() > nextSurvivedIdx + 1) {
			nextSurvivedIdx++;
		}

		double nextLat = provider.getLatitude(nextSurvivedIdx);
		double nextLon = provider.getLongitude(nextSurvivedIdx);
		return Math.min(currLon, nextLon) < rightLon && Math.max(currLon, nextLon) > leftLon
				&& Math.min(currLat, nextLat) < topLat && Math.max(currLat, nextLat) > bottomLat;
	}

	private boolean segmentDataChanged(Map<Pair<WptPt, WptPt>, RoadSegmentData> other) {
		if (other.size() != segmentData.size()) {
			return true;
		}
		for (Pair<WptPt, WptPt> data : other.keySet()) {
			if (other.get(data) != segmentData.get(data)) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	private String getProfileKey(WptPt pt) {
		String key = pt.getProfileType();
		return  key == null ? DEFAULT_PROFILE_KEY : key;
	}

	private Pair<Integer, Integer> getProfileData(String profileKey) {
		MultiProfileGeometryWayContext context = getContext();
		ApplicationMode mode = ApplicationMode.valueOfStringKey(profileKey, ApplicationMode.DEFAULT);
		return ApplicationMode.DEFAULT.getStringKey().equals(mode.getStringKey()) ?
				new Pair<>(context.getStraightLineColor(), context.getStraightLineIconRes()) :
				new Pair<>(mode.getProfileColor(context.isNightMode()), mode.getIconRes());
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		return new GeometryMultiProfileWayStyle(getContext(), Collections.emptyList(),
				getContext().getStraightLineColor(), getContext().getStraightLineIconRes());
	}

	@Nullable
	public static PointF getIconCenter(RotatedTileBox tileBox, List<LatLon> routePoints, Path path, PathMeasure pathMeasure) {
		if (Algorithms.isEmpty(routePoints)) {
			return null;
		}

		path.reset();
		PointF first = getPoint(tileBox, routePoints.get(0));
		path.moveTo(first.x, first.y);
		for (int i = 1; i < routePoints.size(); i++) {
			PointF pt = getPoint(tileBox, routePoints.get(i));
			path.lineTo(pt.x, pt.y);
		}

		pathMeasure.setPath(path, false);
		float routeLength = pathMeasure.getLength();
		float density = tileBox.getDensity();
		float profileIconSize = MultiProfileGeometryWayContext.getProfileIconSizePx(density);
		float minProfileIconMargin = MultiProfileGeometryWayContext.getMinProfileIconMarginPx(density);
		if ((routeLength - profileIconSize) / 2 < minProfileIconMargin) {
			return null;
		}

		float[] xy = new float[2];
		pathMeasure.getPosTan(routeLength * 0.5f, xy, null);
		return new PointF(xy[0], xy[1]);
	}

	private static PointF getPoint(RotatedTileBox tileBox, LatLon latLon) {
		return new PointF(tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude()),
				tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude()));
	}

	public static class GeometryMultiProfileWayStyle extends GeometryWayStyle<MultiProfileGeometryWayContext> {

		@ColorInt
		private final int pathColor;
		@ColorInt
		private final int pathBorderColor;
		@DrawableRes
		private final int profileIconRes;

		private final boolean isGap;

		private final List<LatLon> routePoints;

		public GeometryMultiProfileWayStyle(@NonNull MultiProfileGeometryWayContext context,
		                                    @NonNull List<LatLon> routePoints,
											@ColorInt int profileColor,
		                                    @DrawableRes int profileIconRes,
		                                    boolean isGap) {
			super(context);
			this.routePoints = routePoints;
			this.pathColor = profileColor;
			this.pathBorderColor = ColorUtils.blendARGB(profileColor, Color.BLACK, 0.2f);
			this.profileIconRes = profileIconRes;
			this.isGap = isGap;
		}

		public GeometryMultiProfileWayStyle(@NonNull MultiProfileGeometryWayContext context,
		                                    @NonNull List<LatLon> routePoints,
		                                    @ColorInt int profileColor,
		                                    @DrawableRes int profileIconRes) {
			this(context, routePoints, profileColor, profileIconRes, false);
		}

		@ColorInt
		public int getPathBorderColor() {
			return pathBorderColor;
		}

		@ColorInt
		public int getPathColor() {
			return pathColor;
		}

		@Nullable
		@Override
		public Integer getColor() {
			return pathColor;
		}

		@Override
		public Integer getColor(Integer def) {
			return pathColor;
		}

		@Nullable
		@Override
		public Float getWidth() {
			return getContext().getPathPaint().getStrokeWidth();
		}

		@Override
		public Float getWidth(Integer def) {
			return getContext().getPathPaint().getStrokeWidth();
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getProfileIconBitmap(profileIconRes, pathBorderColor);
		}

		@NonNull
		public List<LatLon> getRoutePoints() {
			return routePoints;
		}

		public boolean isGap() {
			return isGap;
		}

		@Override
		public boolean equals(Object other) {
			if (!getContext().hasMapRenderer()) {
				return this == other;
			} else if (super.equals(other)) {
				if (!(other instanceof GeometryMultiProfileWayStyle)) {
					return false;
				}
				GeometryMultiProfileWayStyle o = (GeometryMultiProfileWayStyle) other;
				return profileIconRes == o.profileIconRes
						&& pathBorderColor == o.pathBorderColor
						&& isGap == o.isGap;
			}
			return false;
		}

		@Override
		public boolean hasPathLine() {
			return !isGap;
		}
	}
}