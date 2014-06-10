package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.widget.Toast;

public class GPXLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	
	private OsmandMapTileView view;
	
	private Paint paint;

	private Path path;
	private static final int startZoom = 7;
	
	private RenderingRulesStorage cachedRrs;
	private boolean cachedNightMode;
	private int cachedColor;

	private GpxSelectionHelper selectedGpxHelper;

	private Paint paintBmp;
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
		//favoriteIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.poi_favourite);
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		selectedGpxHelper = view.getApplication().getSelectedGpxHelper();
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
		int clr = getColor(settings);
		if (!selectedGPXFiles.isEmpty()) {
			for (SelectedGpxFile g : selectedGPXFiles) {
				List<List<WptPt>> points = g.getPointsToDisplay();
				int fcolor = g.getColor() == 0 ? clr : g.getColor();
				paint.setColor(fcolor);
				drawSegments(canvas, tileBox, points);
			}
			if (tileBox.getZoom() >= startZoom) {
				// request to load
				final QuadRect latLonBounds = tileBox.getLatLonBounds();
				for (SelectedGpxFile g : selectedGPXFiles) {
					List<WptPt> pts = g.getGpxFile().points;
					int fcolor = g.getColor() == 0 ? clr : g.getColor();
					FavoriteImageDrawable fid = FavoriteImageDrawable.getOrCreate(view.getContext(), fcolor);
					for (WptPt o : pts) {
						if (o.lat >= latLonBounds.bottom && o.lat <= latLonBounds.top
								&& o.lon >= latLonBounds.left && o.lon <= latLonBounds.right) {
							int x = (int) tileBox.getPixXFromLatLon(o.lat, o.lon);
							int y = (int) tileBox.getPixYFromLatLon(o.lat, o.lon);
							fid.drawBitmapInCenter(canvas, x, y);
//							canvas.drawBitmap(favoriteIcon, x - favoriteIcon.getWidth() / 2,
//									y - favoriteIcon.getHeight(), paint);
						}
					}
				}
			}
		}
	}

	private void drawSegments(Canvas canvas, RotatedTileBox tileBox, List<List<WptPt>> points) {
		final QuadRect latLonBounds = tileBox.getLatLonBounds();
		for (List<WptPt> l : points) {
			path.rewind();
			int startIndex = -1;

			for (int i = 0; i < l.size(); i++) {
				WptPt ls = l.get(i);
				if (startIndex == -1) {
					if (ls.lat >= latLonBounds.bottom - 0.1 && ls.lat <= latLonBounds.top + 0.1  && ls.lon >= latLonBounds.left - 0.1
							&& ls.lon <= latLonBounds.right + 0.1) {
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
			List<WptPt> pts = g.getGpxFile().points;
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
		List<WptPt> favs = new ArrayList<WptPt>();
		getWptFromPoint(tileBox, point, favs);
		if(!favs.isEmpty()){
			StringBuilder res = new StringBuilder();
			int i = 0;
			for(WptPt fav : favs) {
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
		if(o instanceof FavouritePoint){
			return new LatLon(((FavouritePoint)o).getLatitude(), ((FavouritePoint)o).getLongitude());
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




}
