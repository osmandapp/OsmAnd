package net.osmand.plus;

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


	private List<WptPt> resampleAltitude(List<WptPt> pts, double dist) {

		assert dist > 0;
		assert pts != null;

		List<WptPt> track = resampleTrack(pts, dist);

		// Calculate the absolutes of the altitude variations
		Double max = Double.NEGATIVE_INFINITY;
		Double min = Double.POSITIVE_INFINITY;
		for (WptPt pt : track) {
			max = Math.max(max, pt.ele);
			min = Math.min(min, pt.ele);
			pt.fractionElevation = 0.5;
		}
		Double elevationRange = max-min;
		if (elevationRange > 0)
			for (WptPt pt : track)
				pt.fractionElevation = (pt.ele - min)/elevationRange;

		return track;
	}


	private List<WptPt> resampleSpeed(List<WptPt> pts, double dist) {

		//TODO: speed!!

		assert dist > 0;
		assert pts != null;

		List<WptPt> track = resampleTrack(pts, dist);

		// Calculate the absolutes of the altitude variations
		Double max = Double.NEGATIVE_INFINITY;
		Double min = Double.POSITIVE_INFINITY;
		for (WptPt pt : track) {
			max = Math.max(max, pt.ele);
			min = Math.min(min, pt.ele);
			pt.fractionElevation = 0.5;
		}
		Double elevationRange = max-min;
		if (elevationRange > 0)
			for (WptPt pt : track)
				pt.fractionElevation = (pt.ele - min)/elevationRange;

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
		if (pts != null && pts.size() > 0) {

			WptPt lastPt = pts.get(0);
			double segSub = 0;
			double cumDist = 0;
			for (int i = 1; i < pts.size(); i++) {
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
			// Add in the last point as a terminator
			newPts.add(newPts.size(), lastPt);
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
				case RESAMPLE:
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
