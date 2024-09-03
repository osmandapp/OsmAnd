package net.osmand.plus.views.layers.geometry;

import android.graphics.Path;
import android.graphics.PointF;

import net.osmand.core.jni.QListFloat;
import net.osmand.data.RotatedTileBox;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import gnu.trove.list.array.TByteArrayList;

public class GeometryWayPathAlgorithms {
	public static void cullRamerDouglasPeucker(TByteArrayList survivor, GeometryWay.GeometryWayProvider locationProvider,
												int start, int end, double epsillon) {
		double dmax = Double.NEGATIVE_INFINITY;
		int index = -1;
		for (int i = start + 1; i < end; i++) {
			double d = MapUtils.getOrthogonalDistance(locationProvider.getLatitude(i), locationProvider.getLongitude(i),
					locationProvider.getLatitude(start), locationProvider.getLongitude(start),
					locationProvider.getLatitude(end), locationProvider.getLongitude(end));
			if (d > dmax) {
				dmax = d;
				index = i;
			}
		}
		if (dmax > epsillon) {
			cullRamerDouglasPeucker(survivor, locationProvider, start, index, epsillon);
			cullRamerDouglasPeucker(survivor, locationProvider, index, end, epsillon);
		} else {
			survivor.set(end, (byte) 1);
		}
	}



	public static boolean isIn(float x, float y, int lx, int ty, int rx, int by) {
		return x >= lx && x <= rx && y >= ty && y <= by;
	}

	public static List<GeometryWayDrawer.DrawPathData> calculatePath(@NonNull RotatedTileBox tb,
																	 @NonNull List<Float> xs, @NonNull List<Float> ys,
																	 @NonNull Path path) {
		List<GeometryWayDrawer.DrawPathData> pathsData = calculatePath(tb, null, xs, ys, (List<GeometryWayStyle<?>>) null);
		if (pathsData.size() > 0) {
			path.addPath(pathsData.get(0).path);
		}
		return pathsData;
	}

	public  static List<GeometryWayDrawer.DrawPathData> calculatePath(@NonNull RotatedTileBox tb,
																	  @Nullable List<GeometryWayPoint> points,
																	  @Nullable List<Float> xs, @Nullable List<Float> ys,
																	   @Nullable List<GeometryWayStyle<?>> styles) {
		List<GeometryWayDrawer.DrawPathData> pathsData = new ArrayList<>();
		boolean segmentStarted = false;
		GeometryWayPoint first = points == null ? null : points.get(0);
		float prevX = first != null ? first.tx : xs.get(0);
		float prevY = first != null ? first.ty : ys.get(0);
		int size = (points != null ? points.size() : xs.size());
		int height = tb.getPixHeight();
		int width = tb.getPixWidth();
		int cnt = 0;
		boolean hasStyles = (styles != null && styles.size() == xs.size());
		GeometryWayStyle<?> style = hasStyles ? styles.get(0) : (first == null ? null : first.style);
		Path path = new Path();
		float prevXorig = prevX;
		float prevYorig = prevY;
		float currXorig = Float.NaN;
		float currYorig = Float.NaN;
		boolean prevIn = isIn(prevX, prevY, 0, 0, width, height);
		for (int i = 1; i < size; i++) {
			GeometryWayPoint pnt = points == null ? null : points.get(i);
			float currX = pnt != null ? pnt.tx : xs.get(i);
			float currY = pnt != null ? pnt.ty : ys.get(i);
			currXorig = currX;
			currYorig = currY;
			boolean currIn = isIn(currX, currY, 0, 0, width, height);
			boolean draw = false;
			if (prevIn && currIn) {
				draw = true;
			} else {
				long intersection = MapAlgorithms.calculateIntersection((int) currX, (int) currY,
						(int) prevX, (int) prevY, 0, width, height, 0);
				if (intersection != -1) {
					if (prevIn && (i == 1)) {
						cnt++;
						path.moveTo(prevX, prevY);
						segmentStarted = true;
					}
					prevX = (int) (intersection >> 32);
					prevY = (int) (intersection & 0xffffffff);
					draw = true;
				}
				if (i == size - 1 && !currIn) {
					long inter = MapAlgorithms.calculateIntersection((int) prevX, (int) prevY,
							(int) currX, (int) currY, 0, width, height, 0);
					if (inter != -1) {
						currX = (int) (inter >> 32);
						currY = (int) (inter & 0xffffffff);
					}
				}
			}
			if (draw) {
				if (!segmentStarted) {
					cnt++;
					path.moveTo(prevX, prevY);
					segmentStarted = true;
				}
				path.lineTo(currX, currY);
			} else {
				segmentStarted = false;
			}
			prevIn = currIn;
			prevX = currX;
			prevY = currY;

			if (hasStyles || (first != null && first.style != null)) {
				GeometryWayStyle<?> newStyle = pnt != null ? pnt.style : styles.get(i);
				if (!style.equals(newStyle) || newStyle.isUnique()) {
					pathsData.add(new GeometryWayDrawer.DrawPathData(path, new PointF(prevXorig, prevYorig),
							new PointF(currXorig, currYorig), style));
					prevXorig = currXorig;
					prevYorig = currYorig;
					path = new Path();
					if (segmentStarted) {
						path.moveTo(currX, currY);
					}
					style = newStyle;
				}
			}
		}
		if (!path.isEmpty() && !Float.isNaN(currXorig)) {
			pathsData.add(new GeometryWayDrawer.DrawPathData(path, new PointF(prevXorig, prevYorig),
					new PointF(currXorig, currYorig), style));
		}
		return pathsData;
	}

	public static List<GeometryWayDrawer.DrawPathData31> calculatePath(@NonNull List<GeometryWayPoint> points) {
		List<GeometryWayDrawer.DrawPathData31> pathsData = new ArrayList<>();
		GeometryWayPoint firstPoint = points.get(0);
		GeometryWayStyle<?> style = points.get(0).style;
		List<Integer> ind = new ArrayList<>();
		List<Integer> tx = new ArrayList<>();
		List<Integer> ty = new ArrayList<>();
		List<Float> heights = new ArrayList<>();
		List<Float> distances = new ArrayList<>();
		ind.add(firstPoint.index);
		tx.add(firstPoint.tx31);
		ty.add(firstPoint.ty31);
		heights.add(firstPoint.height);
		distances.add(0f);
		for (int i = 1; i < points.size(); i++) {
			GeometryWayPoint pnt = points.get(i);
			ind.add(pnt.index);
			tx.add(pnt.tx31);
			ty.add(pnt.ty31);
			heights.add(pnt.height);
			distances.add((float) pnt.distance);
			if (style != null) {
				GeometryWayStyle<?> newStyle = pnt.style;
				if (!style.equals(newStyle) || newStyle.isUnique()) {
					GeometryWayDrawer.DrawPathData31 newPathData = new GeometryWayDrawer.DrawPathData31(ind, tx, ty, style);
					newPathData.heights = heights;
					newPathData.distances = distances;
					pathsData.add(newPathData);
					ind = new ArrayList<>();
					tx = new ArrayList<>();
					ty = new ArrayList<>();
					heights = new ArrayList<>();
					distances = new ArrayList<>();
					ind.add(pnt.index);
					tx.add(pnt.tx31);
					ty.add(pnt.ty31);
					heights.add(pnt.height);
					distances.add(0f);
					style = newStyle;
				}
			}
		}
		if (tx.size() > 1) {
			GeometryWayDrawer.DrawPathData31 newPathData = new GeometryWayDrawer.DrawPathData31(ind, tx, ty, style);
			pathsData.add(newPathData);
			newPathData.heights = heights;
			newPathData.distances = distances;
		}
		return pathsData;
	}
}
