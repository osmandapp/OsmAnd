package net.osmand.plus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapAlgorithms;

import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class RenderableSegment {

	protected RenderType renderType;
	private double renderPriority;
	private double renderDist;
	protected List<WptPt> points = null;
	protected List<WptPt> culled = null;
	double hash;
	double param1,param2;

	AsyncRamerDouglasPeucer culler = null;

	public List<WptPt> getPoints() { return points; }

	RenderableSegment(OsmandMapTileView view, RenderType type, List<WptPt> pt, double param1, double param2) {

		hash = 0;

		points = null;
		culled = null;
		renderType = type;
		this.param1 = param1;
		this.param2 = param2;

		points = pt;
	}

	public void setRDP(List<WptPt> cull) {
		culled = cull;
	}

	public double getPriority() {
		return renderPriority;
	}

	public RenderType getRenderType() {
		return renderType;
	}

	public int lighter(int colour, float amount) {
		int red = (int) ((Color.red(colour) * (1 - amount) / 255 + amount) * 255);
		int green = (int) ((Color.green(colour) * (1 - amount) / 255 + amount) * 255);
		int blue = (int) ((Color.blue(colour) * (1 - amount) / 255 + amount) * 255);
		return Color.argb(Color.alpha(colour), red, green, blue);
	}

	public void recalculateRenderScale(OsmandMapTileView view, double zoom) {

		// Here we create the 'shadow' resampled/culled points list, based on the asynchronous call.
		// The asynchronous callback will set the variable, and that is used for rendering

		double hashCode = points.hashCode() + zoom;
		if (culled==null || hash != hashCode) {
			if (culler != null)
				culler.cancel(true);				// stop any still-running cull
			hash = hashCode;
			culler = new AsyncRamerDouglasPeucer(renderType, view, this, param1, param2);
			culled = null;				// effectively use full-resolution until re-cull complete (see below)
			culler.execute("");
		}
	}


	public void drawSingleSegment(Paint p, Canvas canvas, RotatedTileBox tileBox) {
		List<WptPt> pts = culled == null? points: culled;			// use culled points preferentially
		drawSingleSegment2(pts, p, canvas, tileBox );
	}


	private void drawSingleSegment2(List<WptPt> pts, Paint p, Canvas canvas, RotatedTileBox tileBox) {

		final QuadRect latLonBounds = tileBox.getLatLonBounds();

		int startIndex = -1;
		int endIndex = -1;
		int prevCross = 0;
		double shift = 0;
		for (int i = 0; i < pts.size(); i++) {
			WptPt ls = pts.get(i);
			int cross = 0;
			cross |= (ls.lon < latLonBounds.left - shift ? 1 : 0);
			cross |= (ls.lon > latLonBounds.right + shift ? 2 : 0);
			cross |= (ls.lat > latLonBounds.top + shift ? 4 : 0);
			cross |= (ls.lat < latLonBounds.bottom - shift ? 8 : 0);
			if (i > 0) {
				if ((prevCross & cross) == 0) {
					if (endIndex == i - 1 && startIndex != -1) {
						// continue previous line
					} else {
						// start new segment
						if (startIndex >= 0) {
							drawSegment(pts, p, canvas, tileBox, startIndex, endIndex);
						}
						startIndex = i - 1;
					}
					endIndex = i;
				}
			}
			prevCross = cross;
		}
		if (startIndex != -1) {
			drawSegment(pts, p, canvas, tileBox, startIndex, endIndex);
		}
	}


	private void drawSegment(List<WptPt> pts, Paint paint, Canvas canvas, RotatedTileBox tb, int startIndex, int endIndex) {
		TIntArrayList tx = new TIntArrayList();
		TIntArrayList ty = new TIntArrayList();
		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		Path path = new Path();
		for (int i = startIndex; i <= endIndex; i++) {
			WptPt p = pts.get(i);
			tx.add((int)(tb.getPixXFromLatLon(p.lat, p.lon) + 0.5));
			ty.add((int)(tb.getPixYFromLatLon(p.lat, p.lon) + 0.5));
		}

//TODO: colour
		calculatePath(tb, tx, ty, path);
		canvas.drawPath(path, paint);
		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	public int calculatePath(RotatedTileBox tb, TIntArrayList xs, TIntArrayList ys, Path path) {
		boolean start = false;
		int px = xs.get(0);
		int py = ys.get(0);
		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int cnt = 0;
		boolean pin = isIn(px, py, w, h);
		for(int i = 1; i < xs.size(); i++) {
			int x = xs.get(i);
			int y = ys.get(i);
			boolean in = isIn(x, y, w, h);
			boolean draw = false;
			if(pin && in) {
				draw = true;
			} else {
				long intersection = MapAlgorithms.calculateIntersection(x, y,
						px, py, 0, w, h, 0);
				if (intersection != -1) {
					px = (int) (intersection >> 32);
					py = (int) (intersection & 0xffffffff);
					draw = true;
				}
			}
			if (draw) {
				if (!start) {
					cnt++;
					path.moveTo(px, py);
				}
				path.lineTo(x, y);
				start = true;
			} else{
				start = false;
			}
			pin = in;
			px = x;
			py = y;
		}
		return cnt;
	}


	protected boolean isIn(float x, float y, float rx, float by) {
		return x >= 0f && x <= rx && y >= 0f && y <= by;
	}
}
