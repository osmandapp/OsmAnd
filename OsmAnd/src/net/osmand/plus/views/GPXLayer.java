package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.TrackChartPoints;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GPXLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider,
		ContextMenuLayer.IMoveObjectProvider, MapTextProvider<WptPt> {

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
	private int currentTrackColor;

	private Bitmap selectedPoint;
	private TrackChartPoints trackChartPoints;

	private static final int startZoom = 7;


	private GpxSelectionHelper selectedGpxHelper;
	private Paint paintBmp;
	private List<WptPt> cache = new ArrayList<>();
	private Map<WptPt, SelectedGpxFile> pointFileMap = new HashMap<>();
	private MapTextLayer textLayer;

	private Paint paintOuter;

	private Paint paintInnerCircle;

	private Paint paintInnerRect;

	private Paint paintTextIcon;

	private Paint paintGridTextIcon;

	private OsmandRenderer osmandRenderer;

	private List<TrkSegment> points;
	private GPXFile gpx;

	private ContextMenuLayer contextMenuLayer;
	@ColorInt
	private int visitedColor;
	@ColorInt
	private int defPointColor;

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		selectedGpxHelper = view.getApplication().getSelectedGpxHelper();
		osmandRenderer = view.getApplication().getResourceManager().getRenderer().getRenderer();
		initUI();
	}

	public void setTrackChartPoints(TrackChartPoints trackChartPoints) {
		this.trackChartPoints = trackChartPoints;
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

		paintGridTextIcon = new Paint();
		paintGridTextIcon.setTextAlign(Align.CENTER);
		paintGridTextIcon.setFakeBoldText(true);
		paintGridTextIcon.setColor(Color.WHITE);
		paintGridTextIcon.setAntiAlias(true);

		textLayer = view.getLayerByClass(MapTextLayer.class);

		paintOuter = new Paint();
		paintOuter.setColor(0x88555555);
		paintOuter.setAntiAlias(true);
		paintOuter.setStyle(Style.FILL_AND_STROKE);
		paintInnerCircle = new Paint();
		paintInnerCircle.setStyle(Style.FILL_AND_STROKE);
		paintInnerCircle.setColor(0xddFFFFFF);
		paintInnerCircle.setAntiAlias(true);
		paintInnerRect = new Paint();
		paintInnerRect.setStyle(Style.FILL_AND_STROKE);
		paintInnerRect.setAntiAlias(true);

		paintIcon = new Paint();
		pointSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_white_shield_small);
		selectedPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_default_location);

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);

		visitedColor = ContextCompat.getColor(view.getApplication(), R.color.color_ok);
		defPointColor = ContextCompat.getColor(view.getApplication(), R.color.gpx_color_point);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (contextMenuLayer.getMoveableObject() instanceof WptPt) {
			WptPt objectInMotion = (WptPt) contextMenuLayer.getMoveableObject();
			PointF pf = contextMenuLayer.getMovableCenterPoint(tileBox);
			SelectedGpxFile gpxFile = pointFileMap.get(objectInMotion);
			if (gpxFile != null) {
				drawBigPoint(canvas, objectInMotion, getFileColor(gpxFile), pf.x, pf.y);
			}
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (points != null) {
			updatePaints(0, false, false, settings, tileBox);
			for (TrkSegment ts : points)
				ts.drawRenderers(view.getZoom(), paint, canvas, tileBox);
		} else {
			List<SelectedGpxFile> selectedGPXFiles = selectedGpxHelper.getSelectedGPXFiles();
			cache.clear();
			currentTrackColor = view.getSettings().CURRENT_TRACK_COLOR.get();
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

	private int updatePaints(int color, boolean routePoints, boolean currentTrack, DrawSettings nightMode, RotatedTileBox tileBox) {
		RenderingRulesStorage rrs = view.getApplication().getRendererRegistry().getCurrentSelectedRenderer();
		final boolean isNight = nightMode != null && nightMode.isNightMode();
		int hsh = calculateHash(rrs, routePoints, isNight, tileBox.getMapDensity(), tileBox.getZoom());
		if (hsh != cachedHash) {
			cachedHash = hsh;
			cachedColor = ContextCompat.getColor(view.getApplication(), R.color.gpx_track);
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
				@ColorInt
				int fileColor = getFileColor(g);
				for (WptPt o : pts) {
					if (o.lat >= latLonBounds.bottom && o.lat <= latLonBounds.top
							&& o.lon >= latLonBounds.left && o.lon <= latLonBounds.right
							&& o != contextMenuLayer.getMoveableObject()) {
						cache.add(o);
						float x = tileBox.getPixXFromLatLon(o.lat, o.lon);
						float y = tileBox.getPixYFromLatLon(o.lat, o.lon);

						if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
							@ColorInt
							int pointColor = getPointColor(o, fileColor);
							paintIcon.setColorFilter(new PorterDuffColorFilter(pointColor, PorterDuff.Mode.MULTIPLY));
							canvas.drawBitmap(pointSmall, x - pointSmall.getWidth() / 2, y - pointSmall.getHeight() / 2, paintIcon);
							smallObjectsLatLon.add(new LatLon(o.lat, o.lon));
						} else {
							fullObjects.add(o);
							fullObjectsLatLon.add(new LatLon(o.lat, o.lon));
						}
					}
					pointFileMap.put(o, g);
				}
				for (WptPt o : fullObjects) {
					float x = tileBox.getPixXFromLatLon(o.lat, o.lon);
					float y = tileBox.getPixYFromLatLon(o.lat, o.lon);
					drawBigPoint(canvas, o, fileColor, x, y);
				}
			}
			if (trackChartPoints != null) {
				drawXAxisPoints(canvas, tileBox);
				LatLon highlightedPoint = trackChartPoints.getHighlightedPoint();
				if (highlightedPoint.getLatitude() >= latLonBounds.bottom
						&& highlightedPoint.getLatitude() <= latLonBounds.top
						&& highlightedPoint.getLongitude() >= latLonBounds.left
						&& highlightedPoint.getLongitude() <= latLonBounds.right) {
					float x = tileBox.getPixXFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
					float y = tileBox.getPixYFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
					paintIcon.setColorFilter(null);
					canvas.drawBitmap(selectedPoint, x - selectedPoint.getWidth() / 2, y - selectedPoint.getHeight() / 2, paintIcon);
				}
			}
			this.fullObjectsLatLon = fullObjectsLatLon;
			this.smallObjectsLatLon = smallObjectsLatLon;
		}
	}

	private void drawXAxisPoints(Canvas canvas, RotatedTileBox tileBox) {
		int color = trackChartPoints.getSegmentColor();
		if (color == 0) {
			GpxDataItem gpxDataItem = null;
			if (!trackChartPoints.getGpx().showCurrentTrack) {
				gpxDataItem = view.getApplication().getGpxDatabase().getItem(new File(trackChartPoints.getGpx().path));
			}
			color = gpxDataItem != null ? gpxDataItem.getColor() : 0;
			if (trackChartPoints.getGpx().showCurrentTrack) {
				color = currentTrackColor;
			}
			if (color == 0) {
				color = cachedColor;
			}
			trackChartPoints.setSegmentColor(color);
		}
		paintInnerRect.setColor(color);
		QuadRect latLonBounds = tileBox.getLatLonBounds();
		List<Pair<String, WptPt>> xAxisPoints = trackChartPoints.getXAxisPoints();
		float r = 12 * tileBox.getDensity();
		paintGridTextIcon.setTextSize(r);
		for (int i = 0; i < xAxisPoints.size(); i++) {
			WptPt axisPoint = xAxisPoints.get(i).second;
			if (axisPoint.getLatitude() >= latLonBounds.bottom
					&& axisPoint.getLatitude() <= latLonBounds.top
					&& axisPoint.getLongitude() >= latLonBounds.left
					&& axisPoint.getLongitude() <= latLonBounds.right) {
				String textOnPoint = xAxisPoints.get(i).first;
				float textWidth = paintGridTextIcon.measureText(textOnPoint);
				float x = tileBox.getPixXFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				float y = tileBox.getPixYFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				canvas.drawRect(
						x - textWidth / 2 - 2 * (float) Math.ceil(tileBox.getDensity()),
						y - r / 2 - 2 * (float) Math.ceil(tileBox.getDensity()),
						x + textWidth / 2 + 2 * (float) Math.ceil(tileBox.getDensity()),
						y + r / 2 + 3 * (float) Math.ceil(tileBox.getDensity()),
						paintInnerRect);
				canvas.drawText(textOnPoint, x, y + r / 2, paintGridTextIcon);
			}
		}
	}

	private int getFileColor(@NonNull SelectedGpxFile g) {
		return g.getColor() == 0 ? defPointColor : g.getColor();
	}

	private void drawBigPoint(Canvas canvas, WptPt o, int fileColor, float x, float y) {
		int pointColor = getPointColor(o, fileColor);
		FavoriteImageDrawable fid = FavoriteImageDrawable.getOrCreate(view.getContext(), pointColor, true);
		fid.drawBitmapInCenter(canvas, x, y);
	}

	@ColorInt
	private int getPointColor(WptPt o, @ColorInt int fileColor) {
		boolean visit = isPointVisited(o);
		return visit ? visitedColor : o.getColor(fileColor);
	}

	private void drawSelectedFilesSegments(Canvas canvas, RotatedTileBox tileBox,
										   List<SelectedGpxFile> selectedGPXFiles, DrawSettings settings) {

		for (SelectedGpxFile g : selectedGPXFiles) {
			GpxDataItem gpxDataItem = null;
			if (!g.isShowCurrentTrack()) {
				gpxDataItem = view.getApplication().getGpxDatabase().getItem(new File(g.getGpxFile().path));
			}
			List<TrkSegment> segments = g.getPointsToDisplay();
			for (TrkSegment ts : segments) {
				int color = gpxDataItem != null ? gpxDataItem.getColor() : 0;
				if (g.isShowCurrentTrack()) {
					color = currentTrackColor;
				}
				if (color == 0) {
					color = ts.getColor(cachedColor);
				}
				if (ts.renders.isEmpty()                // only do once (CODE HERE NEEDS TO BE UI INSTEAD)
						&& !ts.points.isEmpty()) {        // hmmm. 0-point tracks happen, but.... how?
					if (g.isShowCurrentTrack()) {
						ts.renders.add(new Renderable.CurrentTrack(ts.points));
					} else {
						ts.renders.add(new Renderable.StandardTrack(ts.points, 17.2));
					}
				}
				updatePaints(color, g.isRoutePoints(), g.isShowCurrentTrack(), settings, tileBox);
				ts.drawRenderers(view.getZoom(), paint, canvas, tileBox);
			}
		}
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
		return g.getGpxFile().points;
	}

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
		return new LatLon(o.lat, o.lon);
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


	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof WptPt;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o,
									   @NonNull LatLon position,
									   @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback) {

		if (o instanceof WptPt) {
			WptPt objectInMotion = (WptPt) o;
			GPXFile gpxFile = pointFileMap.get(objectInMotion).getGpxFile();
			gpxFile.updateWptPt(objectInMotion, position.getLatitude(),
					position.getLongitude(), System.currentTimeMillis(), objectInMotion.desc,
					objectInMotion.name, objectInMotion.category, objectInMotion.getColor());
			new SaveGpxFileAsyncTask(view.getApplication(), callback, objectInMotion).execute(gpxFile);
		} else if (callback != null) {
			callback.onApplyMovedObject(false, o);
		}
	}

	static class SaveGpxFileAsyncTask extends AsyncTask<GPXFile, Void, String> {
		private final OsmandApplication app;
		@Nullable
		private final ContextMenuLayer.ApplyMovedObjectCallback callback;
		@Nullable
		private final WptPt point;

		SaveGpxFileAsyncTask(OsmandApplication app,
							 @Nullable ContextMenuLayer.ApplyMovedObjectCallback callback,
							 @Nullable WptPt point) {
			this.app = app;
			this.callback = callback;
			this.point = point;
		}

		@Override
		protected String doInBackground(GPXFile... params) {
			GPXFile gpxFile = params[0];
			return GPXUtilities.writeGpxFile(new File(gpxFile.path), gpxFile, app);
		}

		@Override
		protected void onPostExecute(String errorMessage) {
			if (callback != null) {
				callback.onApplyMovedObject(errorMessage == null, point);
			}
		}
	}
}
