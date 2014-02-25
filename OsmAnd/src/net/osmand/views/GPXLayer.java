package net.osmand.views;

import java.util.List;

import net.osmand.OsmandSettings;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;

public class GPXLayer extends OsmandMapLayer {
	
	private OsmandMapTileView view;
	
	private Paint paint;

	private Path path;

	private OsmandSettings settings;
	
	private RenderingRulesStorage cachedRrs;
	private boolean cachedNightMode;
	private int cachedColor;
	
	
	private void initUI() {
		paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(14);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);

		path = new Path();
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		settings = view.getSettings();
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
		GPXFile gpxFile = view.getApplication().getGpxFileToDisplay();
		if(gpxFile == null){
			return;
		}
		List<List<WptPt>> points = gpxFile.processedPointsToDisplay;
		
		paint.setColor(getColor(settings));

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
				continue;
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
	

	public boolean isShowingCurrentTrack(){
		return settings.SHOW_CURRENT_GPX_TRACK.get();
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
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}




}
