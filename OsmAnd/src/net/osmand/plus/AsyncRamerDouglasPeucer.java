package net.osmand.plus;

import android.graphics.Color;
import android.os.AsyncTask;

import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class AsyncRamerDouglasPeucer extends AsyncTask<String,Integer,String> {

	private OsmandMapTileView view;
	private RenderableSegment rs;
	private RenderType renderType;
	private double param1, param2;

	private List<WptPt> culled = null;


	public AsyncRamerDouglasPeucer(RenderType rt, OsmandMapTileView view, RenderableSegment rs, double param1, double param2) {
		this.view = view;
		this.rs = rs;
		this.renderType = rt;

		this.param1 = param1;
		this.param2 = param2;
	}

/*	private int getColor(double percent) {
		int r = (int)(255 * percent);
		int g = (int)(255 * (1.0 - percent));
		return 0xFF000000 + (r<<16) + (g<<8);
	}
*/

	public int getColor2(double percent) {
		double a = (1. - percent) * 5.;
		int X = (int)Math.floor(a);
		int Y = (int)(Math.floor(255 * (a - X)));
		int r = 0,g = 0,b = 0;
		switch (X) {
			case 0:
				r = 255;
				g = Y;
				b = 0;
				break;
			case 1:
				r = 255 - Y;
				g = 255;
				b = 0;
				break;
			case 2:
				r = 0;
				g = 255;
				b = Y;
				break;
			case 3:
				r = 0;
				g = 255 - Y;
				b = 255;
				break;
			case 4:
				r = Y;
				g = 0;
				b = 255;
				break;
			case 5:
				r = 255;
				g = 0;
				b = 255;
				break;
		}
		return 0xFF000000 + (r<<16) + (g<<8) + b;
	}

	public int getColor(double percent) {
		float hsv[] = { (float)percent, 1.0f, 0.5f };
		return Color.HSVToColor(hsv);
	}

	private List<WptPt> resampleAltitude(List<WptPt> pts, double dist) {

		assert dist > 0;
		assert pts != null;

		List<WptPt> track = resampleTrack(pts, dist);

		int halfC = getColor2(0.5);

		// Calculate the absolutes of the altitude variations
		Double max = Double.NEGATIVE_INFINITY;
		Double min = Double.POSITIVE_INFINITY;
		for (WptPt pt : track) {
			max = Math.max(max, pt.ele);
			min = Math.min(min, pt.ele);
			pt.colourARGB = halfC;
		}
		Double elevationRange = max-min;
		if (elevationRange > 0)
			for (WptPt pt : track)
				pt.colourARGB = getColor2((pt.ele - min)/elevationRange);

		return track;
	}


	private List<WptPt> resampleSpeed(List<WptPt> pts, double dist) {

		List<WptPt> track = resampleTrack(pts, dist);

		WptPt lastPt = track.get(0);
		lastPt.speed = 0;

		// calculate speeds
		for (int i=1; i<track.size(); i++) {
			WptPt pt = track.get(i);
			double delta = pt.time - lastPt.time;
			if (delta>0)
				pt.speed = MapUtils.getDistance(pt.getLatitude(),pt.getLongitude(),
						lastPt.getLatitude(),lastPt.getLongitude())/delta;
			else
				pt.speed = 0;		// GPX doesn't have time - this is OK, colour will be mid-range for whole track
			lastPt = pt;
		}

		// Calculate the absolutes of the altitude variations
		Double max = Double.NEGATIVE_INFINITY;
		Double min = Double.POSITIVE_INFINITY;
		for (WptPt pt : track) {
			max = Math.max(max, pt.speed);
			min = Math.min(min, pt.speed);
			pt.colourARGB = getColor2(0.5);
		}
		Double range = max-min;
		if (range > 0)
			for (WptPt pt : track)
				pt.colourARGB = getColor2((pt.speed - min) / range);



		return track;
	}



	// Resample a list of points into a new list of points.
	// The new list is evenly-spaced (dist) and contains the first and last point from the original list.
	// The purpose is to allow tracks to be displayed with colours/shades/animation with even spacing
	// This routine essentially 'walks' along the path, dropping sample points along the trail where necessary. It is
	// Typically, pass a point list to this, and set dist (in metres) to something that's relative to screen zoom
	// The new point list has resampled times, elevations, speed and hdop too!

	private List<WptPt> resampleTrack(List<WptPt> pts, double dist) {

		ArrayList<WptPt> newPts = new ArrayList<WptPt>();

		int ptCt = pts.size();
		if (pts != null && ptCt > 0) {

			WptPt lastPt = pts.get(0);
			double segSub = 0;
			double cumDist = 0;
			for (int i = 1; i < ptCt; i++) {
				WptPt pt = pts.get(i);
				double segLength = MapUtils.getDistance(pt.getLatitude(), pt.getLongitude(), lastPt.getLatitude(), lastPt.getLongitude());

				// March along the segment, calculating the interpolated point values as we go
				while (segSub < segLength) {
					double partial = segSub / segLength;
					WptPt newPoint = new WptPt(
							lastPt.getLatitude() + partial * (pt.getLatitude() - lastPt.getLatitude()),
							lastPt.getLongitude() + partial * (pt.getLongitude() - lastPt.getLongitude()),
							(long) (lastPt.time + partial * (pt.time - lastPt.time)),
							lastPt.ele + partial * (pt.ele - lastPt.ele),
							lastPt.speed + partial * (pt.speed - lastPt.speed),
							lastPt.hdop + partial * (pt.hdop - lastPt.hdop)
					);
					newPoint.setCumulativeDistance(cumDist+segLength*partial);
					newPts.add(newPts.size(), newPoint);
					segSub += dist;
				}
				segSub -= segLength;                // leftover
				cumDist += segLength;
				lastPt = pt;
			}

			// Add in the last point as a terminator (with total distance recorded)
			WptPt newPoint = new WptPt( lastPt.getLatitude(), lastPt.getLongitude(), lastPt.time, lastPt.ele, lastPt.speed, lastPt. hdop);
			newPoint.setCumulativeDistance(cumDist);
			newPts.add(newPts.size(), newPoint);
		}
		return newPts;
	}

	// Reduce the point-count of the GPX track. The concept is that at arbitrary scales, some points are superfluous.
	// This is handled using the well-known 'Ramer-Douglas-Peucker' algorithm. This code is modified from the similar code elsewhere
	// but optimised for this specific usage.

	private boolean[] cullRamerDouglasPeucer(List<WptPt> points, double epsilon) {

		int nsize = points.size();
		boolean[] survivor = new boolean[nsize];
		if (nsize > 0) {
			cullRamerDouglasPeucer(points, epsilon, survivor, 0, nsize - 1);
			survivor[0] = true;
		}
		return survivor;
	}

	private void cullRamerDouglasPeucer(List<WptPt> pt, double epsilon, boolean[] survivor, int start, int end) {

		double dmax = -1;
		int index = -1;
		for (int i = start + 1; i < end; i++) {

			if (isCancelled())
				return;

			double d = MapUtils.getOrthogonalDistance(
					pt.get(i).getLatitude(), pt.get(i).getLongitude(),
					pt.get(start).getLatitude(), pt.get(start).getLongitude(),
					pt.get(end).getLatitude(), pt.get(end).getLongitude());
			if (d > dmax) {
				dmax = d;
				index = i;
			}
		}
		if (dmax >= epsilon) {
			cullRamerDouglasPeucer(pt, epsilon, survivor, start, index);
			cullRamerDouglasPeucer(pt, epsilon, survivor, index, end);
		} else {
			survivor[end] = true;
		}
	}

	@Override
	protected String doInBackground(String... params) {

		String resp = "OK";
		try {
			List<WptPt> points = rs.getPoints();

			switch (renderType) {

				case FORCE_REDRAW:
					break;

				case ALTITUDE:
					culled = resampleAltitude(points, param1);
					break;

				case SPEED:
					culled = resampleSpeed(points, param1);
					break;

				case CONVEYOR:
				case DISTANCE:
					culled = resampleTrack(points, param1);
					break;

				case ORIGINAL:
					boolean[] survivor = cullRamerDouglasPeucer(points, param1);
					culled = new ArrayList<>();
					for (int i = 0; i < survivor.length; i++)
						if (survivor[i])
							culled.add(points.get(i));
					break;

				default:
					break;
			}


		} catch (Exception e) {
			e.printStackTrace();
			resp = e.getMessage();
		}


		return resp;
	}

	@Override
	protected void onPostExecute(String result) {
		// executes on the UI thread so it's OK to change its variables
		if (rs != null && result.equals("OK") && !isCancelled()) {
			rs.setRDP(culled);
			view.refreshMap();					// FORCE redraw to guarantee new culled track is shown
		}
	}
}
