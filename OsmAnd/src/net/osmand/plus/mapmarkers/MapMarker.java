package net.osmand.plus.mapmarkers;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import static net.osmand.data.PointDescription.POINT_TYPE_MAP_MARKER;

public class MapMarker implements LocationPoint {

	private static int[] colors;

	public String id;
	public LatLon point;
	private PointDescription pointDescription;
	public int colorIndex;
	public boolean history;
	public long creationDate;
	public long visitedDate;
	public String groupKey;
	public String groupName;
	public WptPt wptPt;
	public FavouritePoint favouritePoint;
	public String mapObjectName;

	public int dist;
	public boolean selected;

	public MapMarker(@NonNull LatLon point, @NonNull PointDescription name, int colorIndex) {
		this.point = point;
		this.pointDescription = name;
		this.colorIndex = colorIndex;
	}

	public ItineraryType getType() {
		return favouritePoint == null ?
				(wptPt == null ? ItineraryType.MARKERS : ItineraryType.TRACK) :
				ItineraryType.FAVOURITES;
	}

	public PointDescription getPointDescription(@NonNull Context ctx) {
		return new PointDescription(POINT_TYPE_MAP_MARKER, ctx.getString(R.string.map_marker), getOnlyName());
	}

	public String getName(Context ctx) {
		String name;
		PointDescription pd = getPointDescription(ctx);
		if (Algorithms.isEmpty(pd.getName())) {
			name = pd.getTypeName();
		} else {
			name = pd.getName();
		}
		return name;
	}

	public PointDescription getOriginalPointDescription() {
		return pointDescription;
	}

	public void setOriginalPointDescription(PointDescription pointDescription) {
		this.pointDescription = pointDescription;
	}

	public String getOnlyName() {
		return pointDescription == null ? "" : pointDescription.getName();
	}

	public double getLatitude() {
		return point.getLatitude();
	}

	public double getLongitude() {
		return point.getLongitude();
	}

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MapMarker mapMarker = (MapMarker) o;

		return colorIndex == mapMarker.colorIndex && point.equals(mapMarker.point);
	}

	@Override
	public int hashCode() {
		int result = point.hashCode();
		result = 31 * result + colorIndex;
		return result;
	}

	public void copyParams(@NonNull MapMarker marker) {
		point = marker.point;
		colorIndex = marker.colorIndex;
		history = marker.history;
		selected = marker.selected;
		dist = marker.dist;
		creationDate = marker.creationDate;
		visitedDate = marker.visitedDate;
		groupKey = marker.groupKey;
		groupName = marker.groupName;
		wptPt = marker.wptPt;
		favouritePoint = marker.favouritePoint;
		mapObjectName = marker.mapObjectName;
		pointDescription = marker.pointDescription;
	}

	private static final int[] colorsIds = {
			R.color.marker_blue,
			R.color.marker_green,
			R.color.marker_orange,
			R.color.marker_red,
			R.color.marker_yellow,
			R.color.marker_teal,
			R.color.marker_purple
	};

	public static int[] getColors(Context context) {
		if (colors != null) {
			return colors;
		}
		colors = new int[colorsIds.length];
		for (int i = 0; i < colorsIds.length; i++) {
			colors[i] = ContextCompat.getColor(context, colorsIds[i]);
		}
		return colors;
	}

	public static int getColorId(int colorIndex) {
		return (colorIndex >= 0 && colorIndex < colorsIds.length) ? colorsIds[colorIndex] : colorsIds[0];
	}

	public static int getColorIndex(Context context, @ColorInt int color) {
		int[] colors = getColors(context);
		for (int i = 0; i < colors.length; i++) {
			if (color == colors[i]) {
				return i;
			}
		}
		return 0;
	}
}
