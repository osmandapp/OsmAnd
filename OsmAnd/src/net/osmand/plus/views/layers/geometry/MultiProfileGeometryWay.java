package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.Log;
import android.util.Pair;

import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.plus.R;
import net.osmand.plus.measurementtool.RoadSegmentData;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

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

	@Override
	protected void drawRouteSegment(RotatedTileBox tb, Canvas canvas, List<Float> tx, List<Float> ty, List<Double> angles, List<Double> distances, double distToFinish, List<GeometryWayStyle<?>> styles) {
		if (tx.size() < 2) {
			return;
		}
		try {
			List<Pair<Path, GeometryWayStyle<?>>> pathStyles = new ArrayList<>();
			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			calculatePath(tb, tx, ty, getContext().circleSize, styles, pathStyles);

			for (int i = 0; i < pathStyles.size(); i++) {
				Pair<Path, GeometryWayStyle<?>> currPathStyle = pathStyles.get(i);
				if (!((GeometryMultiProfileWayStyle) currPathStyle.second).isGap) {
					getDrawer().drawPathBorder(canvas, currPathStyle.first, currPathStyle.second);
					getDrawer().drawPath(canvas, currPathStyle.first, currPathStyle.second);
				}
			}
			getDrawer().drawArrowsOverPath(canvas, tb, tx, ty, angles, distances, distToFinish, styles);
		} finally {
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		}
	}

	public void updateRoute(RotatedTileBox tileBox, Map<Pair<WptPt, WptPt>, RoadSegmentData> segmentData,
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
			setStyles(tileBox, allSegments, ways, styles);

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
	}

	@Override
	public void clearWay() {
		super.clearWay();
		if (segmentData != null) {
			segmentData.clear();
		}
	}

	private void setStyles(RotatedTileBox tileBox, List<TrkSegment> segments, List<Way> ways, List<GeometryWayStyle<?>> styles) {
		Path path = new Path();
		PathMeasure pathMeasure = new PathMeasure();

		for (TrkSegment segment : segments) {
			List<WptPt> points = segment.points;
			for (int i = 0; i < points.size() - 1; i++) {
				setStylesInternal(tileBox, points, i, ways, styles, path, pathMeasure);
			}
			styles.add(new GeometryMultiProfileWayStyle(getContext(), 0, 0, true));
			Way way = new Way(-1);
			WptPt last = points.get(points.size() - 1);
			way.addNode(new Node(last.lat, last.lon, -1));
			ways.add(way);
		}
	}

	private void setStylesInternal(RotatedTileBox tileBox, List<WptPt> points, int idx, List<Way> ways,
								   List<GeometryWayStyle<?>> styles, Path path, PathMeasure pathMeasure) {
		MultiProfileGeometryWayContext context = getContext();
		WptPt leftPt = points.get(idx);
		Pair<WptPt, WptPt> userLine = new Pair<>(leftPt, points.get(idx + 1));
		RoadSegmentData routeBetweenPoints = segmentData.get(userLine);
		boolean isSecondToLast = idx + 2 == points.size();

		Way way = new Way(-1);
		String currProfileKey = getProfileKey(leftPt);
		Pair<Integer, Integer> profileData = getProfileData(currProfileKey);
		GeometryMultiProfileWayStyle style = new GeometryMultiProfileWayStyle(
				getContext(), profileData.first, profileData.second);
		styles.add(style);
		ways.add(way);

		path.reset();
		if (routeBetweenPoints == null || Algorithms.isEmpty(routeBetweenPoints.getPoints())) {
			way.addNode(new Node(userLine.first.lat, userLine.first.lon, -1));
			if (isSecondToLast) {
				way.addNode(new Node(userLine.second.lat, userLine.second.lon, -1));
			}
			movePathToWpt(path, tileBox, userLine.first);
			pathLineToWpt(path, tileBox, userLine.second);
		} else {
			movePathToWpt(path, tileBox, routeBetweenPoints.getPoints().get(0));
			for (WptPt pt : routeBetweenPoints.getPoints()) {
				if (pt.lat != userLine.second.lat && pt.lon != userLine.second.lon || isSecondToLast) {
					way.addNode(new Node(pt.lat, pt.lon, -1));
				}
				pathLineToWpt(path, tileBox, pt);
			}
		}

		float[] xy = new float[2];
		pathMeasure.setPath(path, false);
		float routeLength = pathMeasure.getLength();
		if ((routeLength - context.circleSize) / 2 >= context.minIconMargin) {
			pathMeasure.getPosTan(pathMeasure.getLength() * 0.5f, xy, null);
			style.setIconLat(tileBox.getLatFromPixel(xy[0], xy[1]));
			style.setIconLon(tileBox.getLonFromPixel(xy[0], xy[1]));
		}
	}

	@Override
	protected boolean shouldAddLocation(RotatedTileBox tileBox, double leftLon, double rightLon,
										double bottomLat, double topLat, GeometryWayProvider provider,
										int currLocationIdx) {
		float currX = tileBox.getPixXFromLatLon(provider.getLatitude(currLocationIdx), provider.getLongitude(currLocationIdx));
		float currY = tileBox.getPixYFromLatLon(provider.getLatitude(currLocationIdx), provider.getLongitude(currLocationIdx));
		if (tileBox.containsPoint(currX, currY, getContext().circleSize)) {
			return true;
		} else if (currLocationIdx + 1 >= provider.getSize()) {
			return false;
		}
		float nextX = tileBox.getPixXFromLatLon(provider.getLatitude(currLocationIdx + 1), provider.getLongitude(currLocationIdx + 1));
		float nextY = tileBox.getPixXFromLatLon(provider.getLatitude(currLocationIdx + 1), provider.getLongitude(currLocationIdx + 1));
		return tileBox.containsPoint(nextX, nextY, getContext().circleSize);
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

	private void movePathToWpt(Path path, RotatedTileBox tileBox, WptPt pt) {
		path.moveTo(tileBox.getPixXFromLatLon(pt.lat, pt.lon), tileBox.getPixYFromLatLon(pt.lat, pt.lon));
	}

	private void pathLineToWpt(Path path, RotatedTileBox tileBox, WptPt pt) {
		path.lineTo(tileBox.getPixXFromLatLon(pt.lat, pt.lon), tileBox.getPixYFromLatLon(pt.lat, pt.lon));
	}

	@NonNull
	private String getProfileKey(WptPt pt) {
		String key = pt.getProfileType();
		return  key == null ? DEFAULT_PROFILE_KEY : key;
	}

	private Pair<Integer, Integer> getProfileData(String profileKey) {
		boolean night = getContext().isNightMode();
		ApplicationMode mode = ApplicationMode.valueOfStringKey(profileKey, ApplicationMode.DEFAULT);
		return ApplicationMode.DEFAULT.getStringKey().equals(mode.getStringKey()) ?
				new Pair<>(ContextCompat.getColor(getContext().getCtx(), ProfileIconColors.DARK_YELLOW.getColor(night)), R.drawable.ic_action_split_interval) :
				new Pair<>(mode.getProfileColor(night), mode.getIconRes());
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		return null;
	}

	public static class GeometryMultiProfileWayStyle extends GeometryWayStyle<MultiProfileGeometryWayContext> {

		@ColorInt
		private final int lineColor;
		@ColorInt
		private final int borderColor;
		@DrawableRes
		private final int profileIconRes;

		private final boolean isGap;

		private double iconLat = Double.NaN;
		private double iconLon = Double.NaN;

		public GeometryMultiProfileWayStyle(MultiProfileGeometryWayContext context,
											@ColorInt int profileColor, @DrawableRes int profileIconRes,
											boolean isGap) {
			super(context);
			this.lineColor = profileColor;
			this.borderColor = ColorUtils.blendARGB(profileColor, Color.BLACK, 0.2f);
			this.profileIconRes = profileIconRes;
			this.isGap = isGap;
		}


		public GeometryMultiProfileWayStyle(MultiProfileGeometryWayContext context,
											@ColorInt int profileColor, @DrawableRes int profileIconRes) {
			this(context, profileColor, profileIconRes, false);
		}

		@ColorInt
		public int getBorderColor() {
			return borderColor;
		}

		@ColorInt
		public int getLineColor() {
			return lineColor;
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getProfileIconBitmap(profileIconRes, borderColor);
		}

		public void setIconLat(double lat) {
			iconLat = lat;
		}

		public void setIconLon(double lon) {
			iconLon = lon;
		}

		public double getIconLat() {
			return iconLat;
		}

		public double getIconLon() {
			return iconLon;
		}

		@Override
		public boolean equals(Object other) {
			return this == other;
		}

		@Override
		public boolean hasPathLine() {
			return true;
		}
	}
}