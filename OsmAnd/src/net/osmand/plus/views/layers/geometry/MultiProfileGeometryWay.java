package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
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
			calculatePath(tb, tx, ty, styles, pathStyles);

			for (int i = 0; i < pathStyles.size(); i++) {
				Pair<Path, GeometryWayStyle<?>> currPathStyle = pathStyles.get(i);
				getDrawer().drawPathBorder(canvas, currPathStyle.first, currPathStyle.second);
				getDrawer().drawPath(canvas, currPathStyle.first, currPathStyle.second);
			}
//			drawer.drawArrowsOverPath(canvas, tb, tx, ty, angles, distances, distToFinish, styles);
		} finally {
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		}
	}

	public void updateRoute(RotatedTileBox tileBox, Map<Pair<WptPt, WptPt>, RoadSegmentData> segmentData,
							boolean before, List<TrkSegment> segments, int segmentIdx) {
		boolean shouldUpdateRoute = tileBox.getMapDensity() != getMapDensity() || segmentDataChanged(segmentData)
				|| getSegments(before) != segments || true;
		if (shouldUpdateRoute && segments.get(segmentIdx).points.size() >= 2) {
			this.segmentData = segmentData;
			setSegments(before, segments);
			List<WptPt> userPoints = segments.get(segmentIdx).points;
			List<Location> locations;
			Map<Integer, GeometryWayStyle<?>> styleMap;

			List<Way> ways = new ArrayList<>();
			List<GeometryWayStyle<?>> styles = new ArrayList<>();
			setStyles(userPoints, ways, styles);
			locations = new ArrayList<>();

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

	private void setStyles(List<WptPt> userPoints, List<Way> ways, List<GeometryWayStyle<?>> styles) {
		String prevProfileKey = "";
		Way way = new Way(-2);

		for (int i = 0; i < userPoints.size() - 1; i++) {
			WptPt leftPt = userPoints.get(i);
			Pair<WptPt, WptPt> userLine = new Pair<>(leftPt, userPoints.get(i + 1));
			RoadSegmentData routeBetweenPoints = segmentData.get(userLine);

			if (!prevProfileKey.equals(getProfileKey(leftPt)) && !leftPt.isGap()) {
				way = new Way(-2);
				String currProfileKey = getProfileKey(leftPt);
				Pair<Integer, Integer> profileData = getProfileData(currProfileKey);
				styles.add(new GeometryMultiProfileWayStyle(getContext(), profileData.first, profileData.second));
				ways.add(way);
				prevProfileKey = currProfileKey;
			}

			boolean isSecondToLast = i + 2 == userPoints.size();
			if (routeBetweenPoints == null || Algorithms.isEmpty(routeBetweenPoints.getPoints())) {
				way.addNode(new Node(userLine.first.lat, userLine.first.lon, -1));
				if (isSecondToLast) {
					way.addNode(new Node(userLine.second.lat, userLine.second.lon, -1));
				}
			} else {
				for (WptPt pt : routeBetweenPoints.getPoints()) {
					if (pt.lat != userLine.second.lat && pt.lon != userLine.second.lon || isSecondToLast) {
						way.addNode(new Node(pt.lat, pt.lon, -1));
					}
				}
			}
		}
	}

	@Override
	protected boolean shouldAddLocation(double leftLon, double rightLon, double bottomLat, double topLat, GeometryWayProvider provider, int currLocationIdx) {
		return super.shouldAddLocation(leftLon, rightLon, bottomLat, topLat, provider, currLocationIdx)
				|| currLocationIdx + 1 < provider.getSize()
				&& super.shouldAddLocation(leftLon, rightLon, bottomLat, topLat, provider, currLocationIdx + 1);
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

	private void setSegments(boolean before, List<TrkSegment> segments) {
		if (before) {
			this.beforeSegments = segments;
		} else {
			this.afterSegments = segments;
		}
	}

	private List<TrkSegment> getSegments(boolean before) {
		return before ? this.beforeSegments : this.afterSegments;
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

	public static class GeometryMultiProfileWayStyle extends  GeometryWayStyle<MultiProfileGeometryWayContext> {

		@ColorInt
		private final int lineColor;
		@ColorInt
		private final int borderColor;
		@DrawableRes
		private final int profileIconRes;

		public GeometryMultiProfileWayStyle(MultiProfileGeometryWayContext context,
											@ColorInt int profileColor, @DrawableRes int profileIconRes) {
			super(context);
			this.lineColor = profileColor;
			this.borderColor = ColorUtils.blendARGB(profileColor, Color.BLACK, 0.2f);
			this.profileIconRes = profileIconRes;
		}

		@ColorInt
		public int getBorderColor() {
			return borderColor;
		}

		@ColorInt
		public int getLineColor() {
			return lineColor;
		}

		@DrawableRes
		public int getProfileIconRes() {
			return profileIconRes;
		}

		@Override
		public Bitmap getPointBitmap() {
//			return getContext().getProfileIconBitmap();
			return null;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || getClass() != other.getClass()) {
				return false;
			}
			if (!super.equals(other)) {
				return false;
			}
			GeometryMultiProfileWayStyle that = (GeometryMultiProfileWayStyle) other;
			return lineColor == that.lineColor &&
					borderColor == that.borderColor &&
					profileIconRes == that.profileIconRes;
		}

		@Override
		public boolean hasPathLine() {
			return true;
		}
	}
}