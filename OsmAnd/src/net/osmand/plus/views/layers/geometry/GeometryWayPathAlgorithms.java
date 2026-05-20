package net.osmand.plus.views.layers.geometry;

import android.graphics.Path;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer.DrawPathData31;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	public static List<DrawPathData31> calculatePath(@NonNull List<GeometryWayPoint> points) {
		List<DrawPathData31> pathsData = new ArrayList<>();
		GeometryWayPoint firstPoint = points.get(0);
		GeometryWayStyle<?> style = firstPoint.style;

		int count = 1;
		int size = points.size();
		int[] ind = new int[size];
		int[] tx = new int[size];
		int[] ty = new int[size];
		float[] heights = new float[size];
		float[] distances = new float[size];

		ind[0] = firstPoint.index;
		tx[0] = firstPoint.tx31;
		ty[0] = firstPoint.ty31;
		heights[0] = firstPoint.height;
		distances[0] = 0f;

		for (int i = 1; i < size; i++) {
			GeometryWayPoint pnt = points.get(i);

			ind[count] = pnt.index;
			tx[count] = pnt.tx31;
			ty[count] = pnt.ty31;
			heights[count] = pnt.height;
			distances[count] = (float) pnt.distance;

			count++;
			if (style != null) {
				GeometryWayStyle<?> newStyle = pnt.style;
				if (!style.equals(newStyle) || newStyle.isUnique()) {
					addPathData(pathsData, ind, tx, ty, heights, distances, style, count);

					ind[0] = pnt.index;
					tx[0] = pnt.tx31;
					ty[0] = pnt.ty31;
					heights[0] = pnt.height;
					distances[0] = 0f;

					count = 1;
					style = newStyle;
				}
			}
		}
		if (count > 1) {
			addPathData(pathsData, ind, tx, ty, heights, distances, style, count);
		}
		return pathsData;
	}

	private static void addPathData(@NonNull List<DrawPathData31> pathsData, @NonNull int[] ind,
			@NonNull int[] tx, @NonNull int[] ty, @NonNull float[] heights, @NonNull float[] distances,
			@Nullable GeometryWayStyle<?> style, int count) {
		DrawPathData31 data = new DrawPathData31(Arrays.copyOf(ind, count), Arrays.copyOf(tx, count), Arrays.copyOf(ty, count), style);
		data.heights = Arrays.copyOf(heights, count);
		data.distances = Arrays.copyOf(distances, count);
		pathsData.add(data);
	}
}
