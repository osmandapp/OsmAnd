package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.AsyncRamerDouglasPeucer;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXFile;
import net.osmand.plus.RenderType;
import net.osmand.plus.TrkSegment;
import net.osmand.plus.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;


public class GPXLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider,
			MapTextProvider<WptPt> {

	private OsmandMapTileView view;

	private Paint paint;
	private Paint paint2;
	private boolean isPaint2;
	private Paint shadowPaint;
	private boolean isShadowPaint;
	private Paint paint_1;
	private boolean isPaint_1;
	private int cachedHash;
	private int cachedColor;

	private Paint paintIcon;
	private Bitmap pointSmall;

	private static final int startZoom = 7;


	private GpxSelectionHelper selectedGpxHelper;
	private Paint paintBmp;
	private List<WptPt> cache = new ArrayList<WptPt>();
	private MapTextLayer textLayer;


	private Paint paintOuter;

	private Paint paintInnerCircle;

	private Paint paintTextIcon;

	private OsmandRenderer osmandRenderer;

	private List<TrkSegment> points;
	private GPXFile gpx;
	private HashMap<Object,AsyncRamerDouglasPeucer> asyncCuller = new HashMap();

//	private HashMap<Object,Integer> resamplerFingeprints = new HashMap();
//	private HashMap<String,AsyncRamerDouglasPeucer> asyncResampler = new HashMap();


	private boolean autoScale = false;                //TODO: INIT THIS TO TRUE ONLY FOR ANDREW'S TESTING

	public void setAutoScale(boolean scale) {
		autoScale = scale;
	}


	private void initUI() {
		paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setAntiAlias(true);
		paint2 = new Paint();
		paint2.setStyle(Style.STROKE);
		paint2.setAntiAlias(true);
		shadowPaint = new Paint();
		shadowPaint.setStyle(Style.STROKE);
		shadowPaint.setAntiAlias(true);
		paint_1 = new Paint();
		paint_1.setStyle(Style.STROKE);
		paint_1.setAntiAlias(true);


		paintBmp = new Paint();
		paintBmp.setAntiAlias(true);
		paintBmp.setFilterBitmap(true);
		paintBmp.setDither(true);

		paintTextIcon = new Paint();
		paintTextIcon.setTextSize(10 * view.getDensity());
		paintTextIcon.setTextAlign(Align.CENTER);
		paintTextIcon.setFakeBoldText(true);
		paintTextIcon.setColor(Color.BLACK);
		paintTextIcon.setAntiAlias(true);

		textLayer = view.getLayerByClass(MapTextLayer.class);

		paintOuter = new Paint();
		paintOuter.setColor(0x88555555);
		paintOuter.setAntiAlias(true);
		paintOuter.setStyle(Style.FILL_AND_STROKE);
		paintInnerCircle = new Paint();
		paintInnerCircle.setStyle(Style.FILL_AND_STROKE);
		paintInnerCircle.setColor(0xddFFFFFF);
		paintInnerCircle.setAntiAlias(true);

		paintIcon = new Paint();
		pointSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_white_shield_small);
	}


	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		selectedGpxHelper = view.getApplication().getSelectedGpxHelper();
		osmandRenderer = view.getApplication().getResourceManager().getRenderer().getRenderer();
		initUI();
	}


	public void updateLayerStyle() {
		cachedHash = -1;
	}

	private float getDefaultStrokeWidth() {
		return 7 * view.getDensity();
	}


	private int updatePaints(int color, boolean routePoints, boolean currentTrack, DrawSettings nightMode, RotatedTileBox tileBox) {
		RenderingRulesStorage rrs = view.getApplication().getRendererRegistry().getCurrentSelectedRenderer();
		final boolean isNight = nightMode != null && nightMode.isNightMode();
		int hsh = calculateHash(rrs, routePoints, isNight, tileBox.getMapDensity());
		if (hsh != cachedHash) {
			cachedHash = hsh;

			cachedColor = view.getResources().getColor(R.color.gpx_track);
			if (rrs != null) {
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, isNight);
				CommonPreference<String> p = view.getSettings().getCustomRenderProperty("currentTrackColor");
				if (p != null && p.isSet()) {
					RenderingRuleProperty ctColor = rrs.PROPS.get("currentTrackColor");
					if (ctColor != null) {
						req.setStringFilter(ctColor, p.get());
					}
				}
				CommonPreference<String> p2 = view.getSettings().getCustomRenderProperty("currentTrackWidth");
				if (p2 != null && p2.isSet()) {
					RenderingRuleProperty ctWidth = rrs.PROPS.get("currentTrackWidth");
					if (ctWidth != null) {
						req.setStringFilter(ctWidth, p2.get());
					}
				}
				String additional = "";
				if (routePoints) {
					additional = "routePoints=true";
				}
				if (currentTrack) {
					additional = (additional.length() == 0 ? "" : ";") + "currentTrack=true";
				}
				if (additional.length() > 0) {
					req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional);
				}
				if (req.searchRenderingAttribute("gpx")) {
					RenderingContext rc = new OsmandRenderer.RenderingContext(view.getContext());
					rc.setDensityValue((float) tileBox.getMapDensity());
					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_COLOR);
					osmandRenderer.updatePaint(req, paint, 0, false, rc);
					isPaint2 = osmandRenderer.updatePaint(req, paint2, 1, false, rc);
					isPaint_1 = osmandRenderer.updatePaint(req, paint_1, -1, false, rc);

					isShadowPaint = req.isSpecified(rrs.PROPS.R_SHADOW_RADIUS);
					if (isShadowPaint) {
						ColorFilter cf = new PorterDuffColorFilter(req.getIntPropertyValue(rrs.PROPS.R_SHADOW_COLOR),
								Mode.SRC_IN);
						shadowPaint.setColorFilter(cf);
						shadowPaint.setStrokeWidth(paint.getStrokeWidth() + 2
								* rc.getComplexValue(req, rrs.PROPS.R_SHADOW_RADIUS));
					}
				} else {
					System.err.println("Rendering attribute gpx is not found !");
					paint.setStrokeWidth(getDefaultStrokeWidth());
				}
			}
		}
		paint.setColor(color == 0 ? cachedColor : color);
		return cachedColor;
	}

	private int calculateHash(Object... o) {
		return Arrays.hashCode(o);
	}


	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (points != null) {
			updatePaints(0, false, false, settings, tileBox);
			drawSegments(canvas, tileBox, points);
		} else {
			List<SelectedGpxFile> selectedGPXFiles = selectedGpxHelper.getSelectedGPXFiles();
			cache.clear();
			if (!selectedGPXFiles.isEmpty()) {
				drawSelectedFilesSegments(canvas, tileBox, selectedGPXFiles, settings);
				canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
				drawSelectedFilesSplits(canvas, tileBox, selectedGPXFiles, settings);
				drawSelectedFilesPoints(canvas, tileBox, selectedGPXFiles);
			}
			if (textLayer != null && textLayer.isVisible()) {
				textLayer.putData(this, cache);
			}
		}
	}

	private void drawSelectedFilesSplits(Canvas canvas, RotatedTileBox tileBox, List<SelectedGpxFile> selectedGPXFiles,
										 DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			// request to load
			for (SelectedGpxFile g : selectedGPXFiles) {
				List<GpxDisplayGroup> groups = g.getDisplayGroups();
				if (groups != null) {
					for (GpxDisplayGroup group : groups) {
						List<GpxDisplayItem> items = group.getModifiableList();
						drawSplitItems(canvas, tileBox, items, settings);
					}
				}
			}
		}
	}

	private void drawSplitItems(Canvas canvas, RotatedTileBox tileBox, List<GpxDisplayItem> items, DrawSettings settings) {
		final QuadRect latLonBounds = tileBox.getLatLonBounds();
		int r = (int) (12 * tileBox.getDensity());
		int dr = r * 3 / 2;
		int px = -1;
		int py = -1;
		for (int k = 0; k < items.size(); k++) {
			GpxDisplayItem i = items.get(k);
			WptPt o = i.locationEnd;
			if (o != null && o.lat >= latLonBounds.bottom && o.lat <= latLonBounds.top && o.lon >= latLonBounds.left
					&& o.lon <= latLonBounds.right) {
				int x = (int) tileBox.getPixXFromLatLon(o.lat, o.lon);
				int y = (int) tileBox.getPixYFromLatLon(o.lat, o.lon);
				if (px != -1 || py != -1) {
					if (Math.abs(x - px) <= dr && Math.abs(y - py) <= dr) {
						continue;
					}
				}
				px = x;
				py = y;
				String nm = i.splitName;
				if (nm != null) {
					int ind = nm.indexOf(' ');
					if (ind > 0) {
						nm = nm.substring(0, ind);
					}
					canvas.drawCircle(x, y, r + (float) Math.ceil(tileBox.getDensity()), paintOuter);
					canvas.drawCircle(x, y, r - (float) Math.ceil(tileBox.getDensity()), paintInnerCircle);
					paintTextIcon.setTextSize(r);
					canvas.drawText(nm, x, y + r / 2, paintTextIcon);
				}
			}
		}
	}

	private void drawSelectedFilesPoints(Canvas canvas, RotatedTileBox tileBox, List<SelectedGpxFile> selectedGPXFiles) {
		int defPointColor = view.getResources().getColor(R.color.gpx_color_point);
		int visitedColor = view.getContext().getResources().getColor(R.color.color_ok);
		if (tileBox.getZoom() >= startZoom) {
			float iconSize = FavoriteImageDrawable.getOrCreate(view.getContext(), 0,
					true).getIntrinsicWidth() * 3 / 2.5f;
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

			// request to load
			final QuadRect latLonBounds = tileBox.getLatLonBounds();
			for (SelectedGpxFile g : selectedGPXFiles) {
				List<WptPt> pts = getListStarPoints(g);
				List<WptPt> fullObjects = new ArrayList<>();
				int fcolor = g.getColor() == 0 ? defPointColor : g.getColor();
				for (WptPt o : pts) {
					if (o.lat >= latLonBounds.bottom && o.lat <= latLonBounds.top
							&& o.lon >= latLonBounds.left && o.lon <= latLonBounds.right) {
						cache.add(o);
						float x = tileBox.getPixXFromLatLon(o.lat, o.lon);
						float y = tileBox.getPixYFromLatLon(o.lat, o.lon);

						if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
							boolean visit = isPointVisited(o);
							int col = visit ? visitedColor : o.getColor(fcolor);
							paintIcon.setColorFilter(new PorterDuffColorFilter(col, PorterDuff.Mode.MULTIPLY));
							canvas.drawBitmap(pointSmall, x - pointSmall.getWidth() / 2, y - pointSmall.getHeight() / 2, paintIcon);
						} else {
							fullObjects.add(o);
						}
					}
				}
				for (WptPt o : fullObjects) {
					float x = tileBox.getPixXFromLatLon(o.lat, o.lon);
					float y = tileBox.getPixYFromLatLon(o.lat, o.lon);
					boolean visit = isPointVisited(o);
					int pointColor = visit ? visitedColor : o.getColor(fcolor);
					FavoriteImageDrawable fid = FavoriteImageDrawable.getOrCreate(view.getContext(), pointColor, true);
					fid.drawBitmapInCenter(canvas, x, y);
				}
			}
		}
	}


	private double getScale(int zoom) {

		final double handRolledMagnification[] = {

				// Calculation: 2^((18-zoom)/2) and then handcrafted to finesse the visuals
				// Trimmed to 0 when not to be displayed, and all zooms after last entry use same value as last
				// Adjust the constant multiplier on the return value to make things a bit bigger or smaller

				0,                    // 0
				0,                    // 1
				0,                    // 2
				0,                    // 3
				0,                    // 4
				0,                    // 5
				0,                    // 6
				0.0220970869121,    // 7		1st zoom level where GPS tracks visible
				0.03125,            // 8
				0.0441941738242,    // 9
				0.0625,            // 10
				0.0883883476483,    // 11
				0.125,            // 12
				0.176776695297,    // 13
				0.25,                // 14
				0.353553390593,    // 15
				0.5,                // 16		last zoom level where things get bigger
				// 17... 	same as above

				// add or remove extra zoom level entries - the code will work OK as it's self-configuring
		};

		int clamped = Math.max(0, Math.min(zoom, handRolledMagnification.length - 1));
		return 32. * handRolledMagnification[clamped];
	}


	private void drawSelectedFilesSegments(Canvas canvas, RotatedTileBox tileBox,
										   List<SelectedGpxFile> selectedGPXFiles, DrawSettings settings) {

		for (SelectedGpxFile g : selectedGPXFiles) {
			List<TrkSegment> segments = g.getPointsToDisplay();
			for (TrkSegment ts : segments) {

				if (ts.renders.size()==0) {
					ts.addRenderable(view, RenderType.ORIGINAL, 0, 0);
					ts.addRenderable(view, RenderType.CONVEYOR, 10, 100);        // m, ms
					ts.addRenderable(view, RenderType.RESAMPLE, 1000, 70);        // 1000m, dot size
					ts.addRenderable(view, RenderType.RESAMPLE, 250, 30);        // m, dot size
				}

				//---------------------------------------------------------------------------
				// Each track segment has a companion 'culled' segment, which is the original passed through a point-reduction
				// to minimise the number of points required. The culled segment is only generated when needed - a "fingerprint"
				// consisting of the view scale and # points in the track changes.

				int viewZoom = view.getZoom();

				//---------------------------------------------------------------------------
				// Each track segment also has an associated resampled track. The resampled track(s) give us the
				// effects that can be overlaid - for example, the conveyor belt effect, or the distance markers.
				// The resampling occurs whenever there's a zoom change, too!

				ts.recalculateRenderScales(view, viewZoom);			// rework all renderers as required









				// The path width is based on the view zoom and any scale modifier from the extensions block in the GPX
				// AND it is scaled by the "default" original track width per the code where NO GPX track was detected.

				// Logic:  IFF the GPX specifies a zoom (i.e., getGpxZoom != 1.0) then we always use that to modify the
				// existing track width (i.e., we multiply the original track width by the zoom). This will make tracks wider
				// by relatively the same amount at all zoom levels, regardless of which system originally set the width.
				// IFF, however, the GPX does not specify a zoom, then we totally leave the track width alone.
				// >>> Constrained by the toggle ON/OFF (autoScale) which lets us choose between behaviours
				// autoScale = false --> above behaviour
				// autoScale = true --> Andrew's debugging version which assumes a constant track width for all zooms has
				//  been set by updatePaints, and then this is modified according to the view scale.  So...
				// *** MAKE SURE autoScale IS SET to 'false' !!!

				double gpxTrackScale = ts.getGpxZoom(g.getGpxZoom());        // will be 1.0 if no scaling specified in segment
				if (autoScale || ts.hasCustomZoom()) {
					gpxTrackScale *= getScale(viewZoom) / getDefaultStrokeWidth();
				}

				if (gpxTrackScale > 0) { // && ts.culledPoints != null) {

					updatePaints(ts.getColor(cachedColor), g.isRoutePoints(), g.isShowCurrentTrack(), settings, tileBox);

					// Now we have calculated the scaling for the tracks, and updatePaints has potentially set its idea
					// of the track sizes, we can revisit the paint contexts and resize the brushes accordingly. This should
					// leave the existing brush calculations intact, and simply modify the resultant sizes according to scale...
					// Note: If autoScale is 'false' then gpxTrackScale IS 1.0 and we have no net change on sizing.           <<< IMPORTANT!
					fixupStrokeWidths(gpxTrackScale);

					ts.drawRenderers(paint, canvas, tileBox);				// any renderers now get to draw

				}
			}
		}
	}

	private void fixupStrokeWidths(double rescale) {

		// Go through ALL the paint contexts which are affected by the new stroke width calculations for GPX tracks and
		// rescale their original widths based on the new scaling value.

		osmandRenderer.fixupStrokeWidth(paint, rescale);
		osmandRenderer.fixupStrokeWidth(paint2, rescale);
		osmandRenderer.fixupStrokeWidth(paint_1, rescale);
		//TODO: shadowPaint??
	}

	private boolean isPointVisited(WptPt o) {
		boolean visit = false;
		String visited = o.getExtensionsToRead().get("VISITED_KEY");
		if (visited != null && !visited.equals("0")) {
			visit = true;
		}
		return visit;
	}

	private List<WptPt> getListStarPoints(SelectedGpxFile g) {
		List<WptPt> pts = g.getGpxFile().points;
		// don't display points here
//		if(pts.isEmpty() & !g.getGpxFile().routes.isEmpty()) {
//			pts = g.getGpxFile().routes.get(0).points;
//		}
		return pts;
	}

	private void drawSegments(Canvas canvas, RotatedTileBox tileBox, List<TrkSegment> points) {
		for (TrkSegment ts : points) {
			//drawSingleSegment(canvas, tileBox, ts);
			ts.drawRenderers(paint, canvas,tileBox);
		}
	}


/*	private void drawSingleSegment(Canvas canvas, RotatedTileBox tileBox, TrkSegment l) {

		final QuadRect latLonBounds = tileBox.getLatLonBounds();

		int startIndex = -1;
		int endIndex = -1;
		int prevCross = 0;
		double shift = 0;
		for (int i = 0; i < l.points.size(); i++) {
			WptPt ls = l.points.get(i);
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
							drawSegment(canvas, tileBox, l, startIndex, endIndex);
						}
						startIndex = i - 1;
					}
					endIndex = i;
				}
			}
			prevCross = cross;
		}
		if (startIndex != -1) {
			drawSegment(canvas, tileBox, l, startIndex, endIndex);
		}
	}
*/

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

/*
	private void drawSegment(Canvas canvas, RotatedTileBox tb, TrkSegment l, int startIndex, int endIndex) {
		TIntArrayList tx = new TIntArrayList();
		TIntArrayList ty = new TIntArrayList();
		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		Path path = new Path();
		for (int i = startIndex; i <= endIndex; i++) {
			WptPt p = l.points.get(i);
			int x = (int) (tb.getPixXFromLatLon(p.lat, p.lon) + 0.5);
			int y = (int) (tb.getPixYFromLatLon(p.lat, p.lon) + 0.5);
//			int x = tb.getPixXFromLonNoRot(p.lon);
//			int y = tb.getPixYFromLatNoRot(p.lat);
			tx.add(x);
			ty.add(y);
		}
		calculatePath(tb, tx, ty, path);
		if (isPaint_1) {
			canvas.drawPath(path, paint_1);
		}
		if (isShadowPaint) {
			canvas.drawPath(path, shadowPaint);
		}
		int clr = paint.getColor();
		if (clr != l.getColor(clr) && l.getColor(clr) != 0) {
			paint.setColor(l.getColor(clr));
		}
		canvas.drawPath(path, paint);
		paint.setColor(clr);
		if (isPaint2) {
			canvas.drawPath(path, paint2);
		}
		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());

	}
*/

	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return (Math.abs(objx - ex) <= radius * 2 && Math.abs(objy - ey) <= radius * 2);
//		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
	}

	public void getWptFromPoint(RotatedTileBox tb, PointF point, List<? super WptPt> res) {
		int r = (int) (15 * tb.getDensity());
		int ex = (int) point.x;
		int ey = (int) point.y;
		for (SelectedGpxFile g : selectedGpxHelper.getSelectedGPXFiles()) {
			List<WptPt> pts = getListStarPoints(g);
			// int fcolor = g.getColor() == 0 ? clr : g.getColor();
			for (WptPt n : pts) {
				int x = (int) tb.getPixXFromLatLon(n.lat, n.lon);
				int y = (int) tb.getPixYFromLatLon(n.lat, n.lon);
				if (calculateBelongs(ex, ey, x, y, r)) {
					res.add(n);
				}
			}
		}
	}

	@Override
	public String getObjectDescription(Object o) {
		if (o instanceof WptPt) {
			return view.getContext().getString(R.string.gpx_wpt) + " : " + ((WptPt) o).name; //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof WptPt) {
			return new PointDescription(PointDescription.POINT_TYPE_WPT, ((WptPt) o).name); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res) {
		getWptFromPoint(tileBox, point, res);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof WptPt) {
			return new LatLon(((WptPt) o).lat, ((WptPt) o).lon);
		}
		return null;
	}


	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public LatLon getTextLocation(WptPt o) {
		return new LatLon(((WptPt) o).lat, ((WptPt) o).lon);
	}

	@Override
	public int getTextShift(WptPt o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity());
	}

	@Override
	public String getText(WptPt o) {
		return o.name;
	}


	public void setGivenGpx(GPXFile gpx) {
		this.gpx = gpx;
		this.points = (gpx == null ? null : gpx.proccessPoints());
	}


}
