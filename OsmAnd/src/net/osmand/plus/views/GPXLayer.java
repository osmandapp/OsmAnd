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

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
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
	
	private int updatePaints(int color, boolean routePoints, boolean currentTrack, DrawSettings nightMode, RotatedTileBox tileBox){
		RenderingRulesStorage rrs = view.getApplication().getRendererRegistry().getCurrentSelectedRenderer();
		final boolean isNight = nightMode != null && nightMode.isNightMode();
		int hsh = calculateHash(rrs, routePoints, isNight, tileBox.getMapDensity(), tileBox.getZoom());
		if (hsh != cachedHash) {
			cachedHash = hsh;
			cachedColor = view.getResources().getColor(R.color.gpx_track);
			if (rrs != null) {
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, isNight);
				CommonPreference<String> p = view.getSettings().getCustomRenderProperty("currentTrackColor");
				if(p != null && p.isSet()) {
					RenderingRuleProperty ctColor = rrs.PROPS.get("currentTrackColor");
					if(ctColor != null) {
						req.setStringFilter(ctColor, p.get());
					}
				}
				CommonPreference<String> p2 = view.getSettings().getCustomRenderProperty("currentTrackWidth");
				if(p2 != null && p2.isSet()) {
					RenderingRuleProperty ctWidth = rrs.PROPS.get("currentTrackWidth");
					if(ctWidth != null) {
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
				req.setIntFilter(rrs.PROPS.R_MINZOOM, tileBox.getZoom());
				req.setIntFilter(rrs.PROPS.R_MAXZOOM, tileBox.getZoom());
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
					paint.setStrokeWidth(7 * view.getDensity());
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
		if(points != null) {
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
		for(int k = 0; k < items.size(); k++) {
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

			List<LatLon> fullObjectsLatLon = new ArrayList<>();
			List<LatLon> smallObjectsLatLon = new ArrayList<>();
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
							smallObjectsLatLon.add(new LatLon(o.lat, o.lon));
						} else {
							fullObjects.add(o);
							fullObjectsLatLon.add(new LatLon(o.lat, o.lon));
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
			this.fullObjectsLatLon = fullObjectsLatLon;
			this.smallObjectsLatLon = smallObjectsLatLon;
		}
	}

	private void drawSelectedFilesSegments(Canvas canvas, RotatedTileBox tileBox,
			List<SelectedGpxFile> selectedGPXFiles, DrawSettings settings) {
		for (SelectedGpxFile g : selectedGPXFiles) {
			List<TrkSegment> points = g.getPointsToDisplay();
			boolean routePoints = g.isRoutePoints();
			updatePaints(g.getColor(), routePoints, g.isShowCurrentTrack(), settings, tileBox);
			drawSegments(canvas, tileBox, points);
		}
	}


	private boolean isPointVisited(WptPt o) {
		boolean visit = false;
		String visited = o.getExtensionsToRead().get("VISITED_KEY");
		if(visited != null && !visited.equals("0")) {
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
		final QuadRect latLonBounds = tileBox.getLatLonBounds();
		for (TrkSegment l : points) {
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
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}


	
	private void drawSegment(Canvas canvas, RotatedTileBox tb, TrkSegment l, int startIndex, int endIndex) {
		TIntArrayList tx = new TIntArrayList();
		TIntArrayList ty = new TIntArrayList();
		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		Path path = new Path();
		for (int i = startIndex; i <= endIndex; i++) {
			WptPt p = l.points.get(i);
			int x = (int) tb.getPixXFromLatLon(p.lat, p.lon);
			int y = (int) tb.getPixYFromLatLon(p.lat, p.lon);
//			int x = tb.getPixXFromLonNoRot(p.lon);
//			int y = tb.getPixYFromLatNoRot(p.lat);
			tx.add(x);
			ty.add(y);
		}
		calculatePath(tb, tx, ty, path);
		if(isPaint_1) {
			canvas.drawPath(path, paint_1);
		}
		if(isShadowPaint) {
			canvas.drawPath(path, shadowPaint);
		}
		int clr = paint.getColor();
		if(clr != l.getColor(clr) && l.getColor(clr) != 0) {
			paint.setColor(l.getColor(clr));
		}
		canvas.drawPath(path, paint);
		paint.setColor(clr);
		if(isPaint2) {
			canvas.drawPath(path, paint2);
		}
		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		
	}
	
	
	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return (Math.abs(objx - ex) <= radius * 2 && Math.abs(objy - ey) <= radius * 2) ;
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
		if(o instanceof WptPt){
			return view.getContext().getString(R.string.gpx_wpt) + " : " + ((WptPt)o).name; //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof WptPt){
			return new PointDescription(PointDescription.POINT_TYPE_WPT, ((WptPt)o).name); //$NON-NLS-1$
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
	public boolean isObjectClickable(Object o) {
		return o instanceof WptPt;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res) {
		if (tileBox.getZoom() >= startZoom) {
			getWptFromPoint(tileBox, point, res);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof WptPt){
			return new LatLon(((WptPt)o).lat, ((WptPt)o).lon);
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
		return new LatLon(((WptPt)o).lat, ((WptPt)o).lon);
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
		this.points = (gpx == null ? null :	gpx.proccessPoints());
	}




}
