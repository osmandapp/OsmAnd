package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.widget.Toast;

public class GPXLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider, 
			MapTextProvider<WptPt> {
	
	private OsmandMapTileView view;
	
	private Paint paint;

	private Path path;
	private static final int startZoom = 7;
	
	private RenderingRulesStorage cachedRrs;
	private boolean cachedNightMode;
	private int cachedColor;

	private GpxSelectionHelper selectedGpxHelper;

	private Paint paintBmp;
	private List<WptPt> cache = new ArrayList<WptPt>();
	private MapTextLayer textLayer;

	private DashPathEffect pathEffect;

	private Paint paintOuter;

	private Paint paintInnerCircle;

	private Paint paintTextIcon;

//	private Drawable favoriteIcon;
	
	
	private void initUI() {
		paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(14);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);

		path = new Path();
		
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
		//favoriteIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_favourite);
		
		paintOuter = new Paint();
		paintOuter.setColor(0x88555555);
		paintOuter.setAntiAlias(true);
		paintOuter.setStyle(Style.FILL_AND_STROKE);
		paintInnerCircle = new Paint();
		paintInnerCircle.setStyle(Style.FILL_AND_STROKE);
		paintInnerCircle.setColor(0xddFFFFFF);
		paintInnerCircle.setAntiAlias(true);
	}
	

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		selectedGpxHelper = view.getApplication().getSelectedGpxHelper();
		pathEffect = new DashPathEffect(new float[] { 5 * view.getDensity(),
				3 * view.getDensity()}, 3);
		initUI();
	}

	
	private int getColor(DrawSettings nightMode){
		RenderingRulesStorage rrs = view.getApplication().getRendererRegistry().getCurrentSelectedRenderer();
		boolean n = nightMode != null && nightMode.isNightMode();
		if (rrs != cachedRrs || cachedNightMode != n) {
			cachedRrs = rrs;
			cachedNightMode = n;
			cachedColor = view.getResources().getColor(R.color.gpx_track);
			if (cachedRrs != null) {
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, cachedNightMode);
				if (req.searchRenderingAttribute("gpxColor")) {
					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_ATTR_COLOR_VALUE);
				}
			}
		}
		return cachedColor;
	}
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		List<SelectedGpxFile> selectedGPXFiles = selectedGpxHelper.getSelectedGPXFiles();
		cache.clear();
		if (!selectedGPXFiles.isEmpty()) {
			drawSelectedFilesSegments(canvas, tileBox, selectedGPXFiles, settings);
			canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			drawSelectedFilesSplits(canvas, tileBox, selectedGPXFiles, settings);
			drawSelectedFilesPoints(canvas, tileBox, selectedGPXFiles);
		}
		if(textLayer.isVisible()) {
			textLayer.putData(this, cache);
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
			// request to load
			final QuadRect latLonBounds = tileBox.getLatLonBounds();
			for (SelectedGpxFile g : selectedGPXFiles) {
				List<WptPt> pts = getListStarPoints(g);
				int fcolor = g.getColor() == 0 ? defPointColor : g.getColor();
				
				for (WptPt o : pts) {
					boolean visit = isPointVisited(o);
					int pointColor = visit ? visitedColor : o.getColor(fcolor);
					FavoriteImageDrawable fid = FavoriteImageDrawable.getOrCreate(view.getContext(), pointColor);
					if (o.lat >= latLonBounds.bottom && o.lat <= latLonBounds.top
							&& o.lon >= latLonBounds.left && o.lon <= latLonBounds.right) {
						cache.add(o);
						int x = (int) tileBox.getPixXFromLatLon(o.lat, o.lon);
						int y = (int) tileBox.getPixYFromLatLon(o.lat, o.lon);
						fid.drawBitmapInCenter(canvas, x, y, tileBox.getDensity());
//							canvas.drawBitmap(favoriteIcon, x - favoriteIcon.getWidth() / 2,
//									y - favoriteIcon.getHeight(), paint);
					}
				}
			}
		}
	}

	private void drawSelectedFilesSegments(Canvas canvas, RotatedTileBox tileBox,
			List<SelectedGpxFile> selectedGPXFiles, DrawSettings settings) {
		int clr = getColor(settings);
		for (SelectedGpxFile g : selectedGPXFiles) {
			List<List<WptPt>> points = g.getPointsToDisplay();
			boolean routePoints = g.isRoutePoints();
			int fcolor = g.getColor() == 0 ? clr : g.getColor();
			paint.setColor(fcolor);
			drawSegments(canvas, tileBox, points, routePoints);
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
		if(pts.isEmpty() & !g.getGpxFile().routes.isEmpty()) {
			pts = g.getGpxFile().routes.get(0).points;
		}
		return pts;
	}

	private void drawSegments(Canvas canvas, RotatedTileBox tileBox, List<List<WptPt>> points, boolean routePoints) {
		final QuadRect latLonBounds = tileBox.getLatLonBounds();
		for (List<WptPt> l : points) {
			path.rewind();
			paint.setPathEffect(routePoints ? pathEffect : null);
			if (routePoints){
				paint.setStrokeCap(Cap.BUTT);
			} else {
				paint.setStrokeCap(Cap.ROUND);
			}
			int startIndex = -1;

			for (int i = 0; i < l.size(); i++) {
				WptPt ls = l.get(i);
				if (startIndex == -1) {
					if (ls.lat >= latLonBounds.bottom - 0.1 && ls.lat <= latLonBounds.top + 0.1  && ls.lon >= latLonBounds.left - 0.1
							&& ls.lon <= latLonBounds.right + 0.1 && !isPointVisited(ls)) {
						startIndex = i > 0 ? i - 1 : i;
					}
				} else if (!(latLonBounds.left <= ls.lon + 0.1 && ls.lon - 0.1 <= latLonBounds.right
						&& latLonBounds.bottom <= ls.lat + 0.1 && ls.lat - 0.1 <= latLonBounds.top)) {
					drawSegment(canvas, tileBox, l, startIndex, i);
					startIndex = -1;
				}
			}
			if (startIndex != -1) {
				drawSegment(canvas, tileBox, l, startIndex, l.size() - 1);
			}
		}
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}


	
	private void drawSegment(Canvas canvas, RotatedTileBox tb, List<WptPt> l, int startIndex, int endIndex) {
		int px = tb.getPixXFromLonNoRot(l.get(startIndex).lon);
		int py = tb.getPixYFromLatNoRot(l.get(startIndex).lat);
		path.moveTo(px, py);
		for (int i = startIndex + 1; i <= endIndex; i++) {
			WptPt p = l.get(i);
			if(isPointVisited(p)) {
				continue;
			}
			int x = tb.getPixXFromLonNoRot(p.lon);
			int y = tb.getPixYFromLatNoRot(p.lat);
			path.lineTo(x, y);
		}
		canvas.drawPath(path, paint);
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
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		List<WptPt> gpxPoints = new ArrayList<WptPt>();
		getWptFromPoint(tileBox, point, gpxPoints);
		if(!gpxPoints.isEmpty() && (tileBox.getZoom() > 14 || gpxPoints.size() < 6)){
			StringBuilder res = new StringBuilder();
			int i = 0;
			for(WptPt fav : gpxPoints) {
				if (i++ > 0) {
					res.append("\n\n");
				}
				res.append(view.getContext().getString(R.string.gpx_wpt) + " : " + fav.name);  //$NON-NLS-1$
			}
			AccessibleToast.makeText(view.getContext(), res.toString(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}


	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof WptPt){
			return view.getContext().getString(R.string.gpx_wpt) + " : " + ((WptPt)o).name; //$NON-NLS-1$
		}
		return null;
	}
	
	@Override
	public String getObjectName(Object o) {
		if(o instanceof WptPt){
			return ((WptPt)o).name; //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res) {
		getWptFromPoint(tileBox, point, res);
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




}
