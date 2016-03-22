package net.osmand.plus.render;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.NativeLibrary;
import net.osmand.NativeLibrary.NativeSearchResult;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.map.MapTileDownloader;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.render.TextRenderer.TextDrawInfo;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class OsmandRenderer {
	private static final Log log = PlatformUtil.getLog(OsmandRenderer.class);

	private Paint paint;
	private Paint paintIcon;

	private float cachedStrokeWidth;
	private float defaultStrokeWidth;
	public float getStrokeWidth() {
		return cachedStrokeWidth;
	}


	public static final int TILE_SIZE = 256; 
	private static final int MAX_V = 75;

	private Map<float[], PathEffect> dashEffect = new LinkedHashMap<float[], PathEffect>();
	private Map<String, float[]> parsedDashEffects = new LinkedHashMap<String, float[]>();
	private Map<String, Shader> shaders = new LinkedHashMap<String, Shader>();

	private final Context context;

	private DisplayMetrics dm;

	private TextRenderer textRenderer;

	public class MapDataObjectPrimitive {
		BinaryMapDataObject obj;
		int typeInd;
		double order;
		int objectType;
	};

	private static class IconDrawInfo {
		float x = 0;
		float y = 0;
		String resId_1;
		String resId;
		String resId2;
		String resId3;
		String resId4;
		String resId5;
		String shieldId;
		int iconOrder;
		float iconSize;
	}
	
	
	

	/* package */
	public static class RenderingContext extends net.osmand.RenderingContext {
		List<TextDrawInfo> textToDraw = new ArrayList<TextDrawInfo>();
		List<IconDrawInfo> iconsToDraw = new ArrayList<IconDrawInfo>();
		Paint[] oneWay ;
		Paint[] reverseOneWay ;
		final Context ctx;

		public RenderingContext(Context ctx) {
			this.ctx = ctx;
		}

		// use to calculate points
		PointF tempPoint = new PointF();
		float cosRotateTileSize;
		float sinRotateTileSize;

		int shadowLevelMin = 256;
		int shadowLevelMax = 0;

		boolean ended = false;

		
		@Override
		protected byte[] getIconRawData(String data) {
			return RenderingIcons.getIconRawData(ctx, data);
		}
	}

	public OsmandRenderer(Context context) {
		this.context = context;

		paintIcon = new Paint();
		paintIcon.setStyle(Style.STROKE);


		textRenderer = new TextRenderer(context);
		paint = new Paint();
		paint.setAntiAlias(true);

		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
	}

	public PathEffect getDashEffect(RenderingContext rc, float[] cachedValues, float st){
		float[] dashes = new float[cachedValues.length / 2];
		for (int i = 0; i < dashes.length; i++) {
			dashes[i] = rc.getDensityValue(cachedValues[i * 2]) + cachedValues[i * 2 + 1];
		}
		if(!dashEffect.containsKey(dashes)){
			dashEffect.put(dashes, new DashPathEffect(dashes, st));
		}
		return dashEffect.get(dashes);
	}

	public Shader getShader(String resId){
		
		if(shaders.get(resId) == null){
			Bitmap bmp = RenderingIcons.getIcon(context, resId, true);
			if(bmp != null){
				Shader sh = new BitmapShader(bmp, TileMode.REPEAT, TileMode.REPEAT);
				shaders.put(resId, sh);
			} else {
				shaders.put(resId, null);
			}
		}	
		return shaders.get(resId);
	}
	
	private void put(TIntObjectHashMap<TIntArrayList> map, int k, int v){
		if(!map.containsKey(k)){
			map.put(k, new TIntArrayList());
		}
		map.get(k).add(v);
	}


	/**
	 * @return if map could be replaced
	 */
	public void generateNewBitmapNative(RenderingContext rc, NativeOsmandLibrary library, 
			NativeSearchResult searchResultHandler, 
			Bitmap bmp, RenderingRuleSearchRequest render, final MapTileDownloader mapTileDownloader) {
		long now = System.currentTimeMillis();
		if (rc.width > 0 && rc.height > 0 && searchResultHandler != null) {
			rc.cosRotateTileSize = (float) (Math.cos(Math.toRadians(rc.rotate)) * TILE_SIZE);
			rc.sinRotateTileSize = (float) (Math.sin(Math.toRadians(rc.rotate)) * TILE_SIZE);
			try {
				if(Looper.getMainLooper() != null && library.useDirectRendering()) {
					final Handler h = new Handler(Looper.getMainLooper());
					notifyListenersWithDelay(rc, mapTileDownloader, h);
				}
				
				// Native library will decide on it's own best way of rendering
				// If res.bitmapBuffer is null, it indicates that rendering was done directly to
				// memory of passed bitmap, but this is supported only on Android >= 2.2
				final NativeLibrary.RenderingGenerationResult res = library.generateRendering(
					rc, searchResultHandler, bmp, bmp.hasAlpha(), render);
				rc.ended = true;
				notifyListeners(mapTileDownloader);
				long time = System.currentTimeMillis() - now;
				rc.renderingDebugInfo = String.format("Rendering: %s ms  (%s text)\n"
						+ "(%s points, %s points inside, %s of %s objects visible)\n",//$NON-NLS-1$
						time, rc.textRenderingTime, rc.pointCount, rc.pointInsideCount, rc.visible, rc.allObjects);
				
				// See upper note
				if(res.bitmapBuffer != null) {
					bmp.copyPixelsFromBuffer(res.bitmapBuffer);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	void drawObject(RenderingContext rc,  Canvas cv, RenderingRuleSearchRequest req,
			List<MapDataObjectPrimitive> array, int objOrder) {
			//double polygonLimit = 100;
			//float orderToSwitch = 0;
			double minPolygonSize = 1. / rc.polygonMinSizeToDisplay;
			for (int i = 0; i < array.size(); i++) {
				rc.allObjects++;
				BinaryMapDataObject mObj = array.get(i).obj;
				TagValuePair pair = mObj.getMapIndex().decodeType(mObj.getTypes()[array.get(i).typeInd]);
				if (objOrder == 0) {
					if (array.get(i).order > minPolygonSize + ((int) array.get(i).order)) {
						continue;
					}
					// polygon
					drawPolygon(mObj, req, cv, rc, pair);
				} else if (objOrder == 1 || objOrder == 2) {
					drawPolyline(mObj, req, cv, rc, pair, mObj.getSimpleLayer(), objOrder == 1);
				} else if (objOrder == 3) {
					drawPoint(mObj, req, cv, rc, pair, array.get(i).typeInd == 0);
				}
				if (i % 25 == 0 && rc.interrupted) {
					return;
				}
			}
		}
	
	public void generateNewBitmap(RenderingContext rc, List<BinaryMapDataObject> objects, Bitmap bmp, 
				RenderingRuleSearchRequest render, final MapTileDownloader mapTileDownloader) {
		long now = System.currentTimeMillis();
		// fill area
		Canvas cv = new Canvas(bmp);
		if (rc.defaultColor != 0) {
			cv.drawColor(rc.defaultColor);
		}
		if (objects != null && !objects.isEmpty() && rc.width > 0 && rc.height > 0) {
			rc.cosRotateTileSize = (float) (Math.cos((float) Math.toRadians(rc.rotate)) * TILE_SIZE);
			rc.sinRotateTileSize = (float) (Math.sin((float) Math.toRadians(rc.rotate)) * TILE_SIZE);
			
			// put in order map
			List<MapDataObjectPrimitive>  pointsArray = new ArrayList<OsmandRenderer.MapDataObjectPrimitive>();
			List<MapDataObjectPrimitive> polygonsArray = new ArrayList<OsmandRenderer.MapDataObjectPrimitive>();
			List<MapDataObjectPrimitive>  linesArray = new ArrayList<OsmandRenderer.MapDataObjectPrimitive>();
			sortObjectsByProperOrder(rc, objects, render, pointsArray, polygonsArray, linesArray);

			rc.lastRenderedKey = 0;

			drawObject(rc, cv, render, polygonsArray, 0);
			rc.lastRenderedKey = 5;
			if (rc.shadowRenderingMode > 1) {
				drawObject(rc, cv, render, linesArray, 1);
			}
			rc.lastRenderedKey = 40;
			drawObject(rc, cv, render, linesArray, 2);
			rc.lastRenderedKey = 60;

			drawObject(rc, cv, render, pointsArray, 3);
			rc.lastRenderedKey = 125;


			long beforeIconTextTime = System.currentTimeMillis() - now;
			notifyListeners(mapTileDownloader);
			drawIconsOverCanvas(rc, cv);

			notifyListeners(mapTileDownloader);
			textRenderer.drawTextOverCanvas(rc, cv, rc.preferredLocale);

			long time = System.currentTimeMillis() - now;
			rc.renderingDebugInfo = String.format("Rendering: %s ms  (%s text)\n"
					+ "(%s points, %s points inside, %s of %s objects visible)",//$NON-NLS-1$
					time, time - beforeIconTextTime, rc.pointCount, rc.pointInsideCount, rc.visible, rc.allObjects);
			log.info(rc.renderingDebugInfo);

		}
	}

	private void notifyListenersWithDelay(final RenderingContext rc, final MapTileDownloader mapTileDownloader, final Handler h) {
		h.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(!rc.ended) {
					notifyListeners(mapTileDownloader);
					notifyListenersWithDelay(rc, mapTileDownloader, h);
				}
			}
		}, 800);
	}
	
	public float getDensity(){
		return dm.density;
	}

	private void drawIconsOverCanvas(RenderingContext rc, Canvas cv) {
		// 1. Sort text using text order
		Collections.sort(rc.iconsToDraw, new Comparator<IconDrawInfo>() {
			@Override
			public int compare(IconDrawInfo object1, IconDrawInfo object2) {
				return object1.iconOrder - object2.iconOrder;
			}
		});
		QuadRect bounds = new QuadRect(0, 0, rc.width, rc.height);
		bounds.inset(-bounds.width()/4, -bounds.height()/4);
		QuadTree<RectF> boundIntersections = new QuadTree<RectF>(bounds, 4, 0.6f);
		List<RectF> result = new ArrayList<RectF>();
		
		for (IconDrawInfo icon : rc.iconsToDraw) {
			if (icon.resId != null) {
				Bitmap ico = RenderingIcons.getIcon(context, icon.resId, true);
				if (ico != null) {
					if (icon.y >= 0 && icon.y < rc.height && icon.x >= 0 && icon.x < rc.width) {
						int visbleWidth = icon.iconSize >= 0 ? (int) icon.iconSize : ico.getWidth();
						int visbleHeight = icon.iconSize >= 0 ? (int) icon.iconSize : ico.getHeight();
						boolean intersects = false;
						float coeff = rc.getDensityValue(rc.screenDensityRatio * rc.textScale);
						RectF rf = calculateRect(rc, icon, ico.getWidth(), ico.getHeight());
						RectF visibleRect = null;
						if (visbleHeight > 0 && visbleWidth > 0) {
							visibleRect = calculateRect(rc, icon, visbleWidth, visbleHeight);
							boundIntersections.queryInBox(new QuadRect(visibleRect.left, visibleRect.top, visibleRect.right, visibleRect.bottom), result);
							for (RectF r : result) {
								if (r.intersects(visibleRect.left, visibleRect.top, visibleRect.right, visibleRect.bottom)) {
									intersects = true;
									break;
								}
							}
						}
						
						if (!intersects) {
							Bitmap shield = icon.shieldId == null ? null : RenderingIcons.getIcon(context, icon.shieldId, true);
							if(shield != null) {
								RectF shieldRf = calculateRect(rc, icon, shield.getWidth(), shield.getHeight());
								if (coeff != 1f) {
									Rect src = new Rect(0, 0, shield.getWidth(), shield.getHeight());
									drawBitmap(cv, shield, shieldRf, src);
								} else {
									drawBitmap(cv, shield, shieldRf);
								}	
							}
							if (coeff != 1f) {
								Rect src = new Rect(0, 0, ico.getWidth(), ico.getHeight());
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId_1, true), rf, src);
								drawBitmap(cv, ico, rf, src);
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId2, true), rf, src);
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId3, true), rf, src);
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId4, true), rf, src);
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId5, true), rf, src);
							} else {
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId_1, true), rf);
								drawBitmap(cv, ico, rf);
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId2, true), rf);
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId3, true), rf);
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId4, true), rf);
								drawBitmap(cv, RenderingIcons.getIcon(context, icon.resId5, true), rf);
							}
							if(visibleRect != null) {
								visibleRect.inset(-visibleRect.width() / 4, -visibleRect.height() / 4);
								boundIntersections.insert(visibleRect, 
										new QuadRect(visibleRect.left, visibleRect.top, visibleRect.right, visibleRect.bottom));
							}
						}
					}
				}
			}
			if (rc.interrupted) {
				return;
			}
		}
	}

	protected void drawBitmap(Canvas cv, Bitmap ico, RectF rf) {
		if(ico == null) {
			return;
		}
		cv.drawBitmap(ico, rf.left, rf.top, paintIcon);
	}

	protected void drawBitmap(Canvas cv, Bitmap ico, RectF rf, Rect src) {
		if(ico == null) {
			return;
		}
		cv.drawBitmap(ico, src, rf, paintIcon);
	}

	private RectF calculateRect(RenderingContext rc, IconDrawInfo icon, int visbleWidth, int visbleHeight) {
		RectF rf;
		float coeff = rc.getDensityValue(rc.screenDensityRatio * rc.textScale);
		float left = icon.x - visbleWidth / 2 * coeff;
		float top = icon.y - visbleHeight / 2 * coeff;
		float right = left + visbleWidth * coeff;
		float bottom = top + visbleHeight * coeff;
		rf = new RectF(left, top, right, bottom);
		return rf;
	}
	
	Comparator<MapDataObjectPrimitive> sortByOrder() {
		return new Comparator<MapDataObjectPrimitive>() {

			@Override
			public int compare(MapDataObjectPrimitive i, MapDataObjectPrimitive j) {
				if (i.order == j.order) {
					if (i.typeInd == j.typeInd) {
						if(i.obj.getPointsLength() == j.obj.getPointsLength()) {
							return 0;
						}
						return i.obj.getPointsLength() < j.obj.getPointsLength() ? -1 : 1;
					}
					return i.typeInd < j.typeInd ? -1 : 1;
				}
				return (i.order < j.order ? -1 : 1);
			}

		};
	}
	
	Comparator<MapDataObjectPrimitive> sortPolygonsOrder() {
		return new Comparator<MapDataObjectPrimitive>() {

			@Override
			public int compare(MapDataObjectPrimitive i, MapDataObjectPrimitive j) {
				if (i.order == j.order)
					return i.typeInd < j.typeInd ? -1 : 1;
				return (i.order > j.order) ? -1 : 1;
			}
		};
	}

	private void sortObjectsByProperOrder(RenderingContext rc, List<BinaryMapDataObject> objects,
			RenderingRuleSearchRequest render, 
			List<MapDataObjectPrimitive>  pointsArray, List<MapDataObjectPrimitive> polygonsArray,
			List<MapDataObjectPrimitive>  linesResArray) {
		int sz = objects.size();
		List<MapDataObjectPrimitive> linesArray = new ArrayList<OsmandRenderer.MapDataObjectPrimitive>();
		if (render != null) {
			render.clearState();

			float mult = (float) (1. / MapUtils.getPowZoom(Math.max(31 - (rc.zoom + 8), 0)));
			for (int i = 0; i < sz; i++) {
				BinaryMapDataObject o = objects.get(i);
				for (int j = 0; j < o.getTypes().length; j++) {
					int wholeType = o.getTypes()[j];
					int layer = 0;
					if (o.getPointsLength() > 1) {
						layer = o.getSimpleLayer();
					}

					TagValuePair pair = o.getMapIndex().decodeType(wholeType);
					if (pair != null) {
						render.setTagValueZoomLayer(pair.tag, pair.value, rc.zoom, layer, o);
						render.setBooleanFilter(render.ALL.R_AREA, o.isArea());
						render.setBooleanFilter(render.ALL.R_POINT, o.getPointsLength() == 1);
						render.setBooleanFilter(render.ALL.R_CYCLE, o.isCycle());
						if (render.search(RenderingRulesStorage.ORDER_RULES)) {
							int objectType = render.getIntPropertyValue(render.ALL.R_OBJECT_TYPE);
							int order = render.getIntPropertyValue(render.ALL.R_ORDER);
							MapDataObjectPrimitive mapObj = new MapDataObjectPrimitive();
							mapObj.objectType = objectType;
							mapObj.order = order;
							mapObj.typeInd = j;
							mapObj.obj = o;
							if(objectType == 3) {
								MapDataObjectPrimitive pointObj = mapObj;
								pointObj.objectType = 1;
								double area = polygonArea(mapObj, mult);
								if(area > MAX_V) { 
									mapObj.order = mapObj.order + (1. / area);
									polygonsArray.add(mapObj);
									pointsArray.add(pointObj); // TODO fix duplicate text? verify if it is needed for icon
								}
							} else if(objectType == 1) {
								pointsArray.add(mapObj);
							} else {
								linesArray.add(mapObj);
							}
							if (render.isSpecified(render.ALL.R_SHADOW_LEVEL)) {
								rc.shadowLevelMin = Math.min(rc.shadowLevelMin, order);
								rc.shadowLevelMax = Math.max(rc.shadowLevelMax, order);
								render.clearValue(render.ALL.R_SHADOW_LEVEL);
							}
						}

					}
				}

				if (rc.interrupted) {
					return;
				}
			}
		}
		Collections.sort(polygonsArray, sortByOrder());
		Collections.sort(pointsArray, sortByOrder());
		Collections.sort(linesArray, sortByOrder());
		filterLinesByDensity(rc, linesResArray, linesArray);
	}
	
	void filterLinesByDensity(RenderingContext rc, List<MapDataObjectPrimitive>  linesResArray,
			List<MapDataObjectPrimitive> linesArray) {
//		int roadsLimit = rc->roadsDensityLimitPerTile;
//		int densityZ = rc->roadDensityZoomTile;
//		if(densityZ == 0 || roadsLimit == 0) {
//			linesResArray = linesArray;
//			return;
//		}
//		linesResArray.reserve(linesArray.size());
//		UNORDERED(map)<int64_t, pair<int, int> > densityMap;
//		for (int i = linesArray.size() - 1; i >= 0; i--) {
//			bool accept = true;
//			int o = linesArray[i].order;
//			MapDataObject* line = linesArray[i].obj;
//			tag_value& ts = line->types[linesArray[i].typeInd];
//			if (ts.first == "highway") {
//				accept = false;
//				int64_t prev = 0;
//				for (uint k = 0; k < line->points.size(); k++) {
//					int dz = rc->getZoom() + densityZ;
//					int64_t x = (line->points[k].first) >> (31 - dz);
//					int64_t y = (line->points[k].second) >> (31 - dz);
//					int64_t tl = (x << dz) + y;
//					if (prev != tl) {
//						prev = tl;
//						pair<int, int>& p = densityMap[tl];
//						if (p.first < roadsLimit/* && p.second > o */) {
//							accept = true;
//							p.first++;
//							p.second = o;
//							densityMap[tl] = p;
//						}
//					}
//				}
//			}
//			if(accept) {
//				linesResArray.push_back(linesArray[i]);
//			}
//		}
//		reverse(linesResArray.begin(), linesResArray.end());
		// TODO
		linesResArray.addAll(linesArray);
	}

	private double polygonArea(MapDataObjectPrimitive mapObj, float mult) {
		double area = 0.;
		int j = mapObj.obj.getPointsLength() - 1;
		for (int i = 0; i < mapObj.obj.getPointsLength(); i++) {
			int px = mapObj.obj.getPoint31XTile(i);
			int py = mapObj.obj.getPoint31YTile(i);
			int sx = mapObj.obj.getPoint31XTile(j);
			int sy = mapObj.obj.getPoint31YTile(j);
			area += (sx + ((float) px)) * (sy - ((float) py));
			j = i;
		}
		return Math.abs(area) * mult * mult * .5;
	}

	private void notifyListeners(MapTileDownloader mapTileDownloader) {
		if (mapTileDownloader != null) {
			mapTileDownloader.fireLoadCallback(null);
		}
	}

	private PointF calcPoint(int xt, int yt, RenderingContext rc){
		rc.pointCount ++;
		double tx = xt / rc.tileDivisor;
		double ty = yt / rc.tileDivisor;
		double dTileX = (tx - rc.leftX);
		double dTileY = (ty - rc.topY);
		float x = (float) (rc.cosRotateTileSize * dTileX - rc.sinRotateTileSize * dTileY);
		float y = (float) (rc.sinRotateTileSize * dTileX + rc.cosRotateTileSize * dTileY);
		rc.tempPoint.set(x, y);
		if(rc.tempPoint.x >= 0 && rc.tempPoint.x < rc.width && 
				rc.tempPoint.y >= 0 && rc.tempPoint.y < rc.height){
			rc.pointInsideCount++;
		}
		return rc.tempPoint;
	}
	
	private PointF calcPoint(BinaryMapDataObject o, int ind, RenderingContext rc){
		return calcPoint(o.getPoint31XTile(ind), o.getPoint31YTile(ind), rc);
	}


	public void clearCachedResources(){
		shaders.clear();
	}
	
	private void drawPolygon(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Canvas canvas, RenderingContext rc, TagValuePair pair) {
		if(render == null || pair == null){
			return;
		}
		float xText = 0;
		float yText = 0;
		int zoom = rc.zoom;
		Path path = null;
		
		// rc.main.color = Color.rgb(245, 245, 245);
		render.setInitialTagValueZoom(pair.tag, pair.value, zoom, obj);
		boolean rendered = render.search(RenderingRulesStorage.POLYGON_RULES);
		if(!rendered || !updatePaint(render, paint, 0, true, rc)){
			return;
		}
		rc.visible++;
		int len = obj.getPointsLength();
//		if(len > 150) {
//			int[] ts = obj.getTypes();
//			System.err.println("Polygon " + len);
//			for(int i=0; i<ts.length; i++) {
//				System.err.println(obj.getMapIndex().decodeType(ts[i]));
//			}
//			return;
//		}
		for (int i = 0; i < obj.getPointsLength(); i++) {

			PointF p = calcPoint(obj, i, rc);
			xText += p.x;
			yText += p.y;
			if (path == null) {
				path = new Path();
				path.moveTo(p.x, p.y);
			} else {
				path.lineTo(p.x, p.y);
			}
		}
		int[][] polygonInnerCoordinates = obj.getPolygonInnerCoordinates();
		if (polygonInnerCoordinates != null && path != null) {
			path.setFillType(FillType.EVEN_ODD);
			for (int j = 0; j < polygonInnerCoordinates.length; j++) {
				for (int i = 0; i < polygonInnerCoordinates[j].length; i += 2) {
					PointF p = calcPoint(polygonInnerCoordinates[j][i], polygonInnerCoordinates[j][i + 1], rc);
					if (i == 0) {
						path.moveTo(p.x, p.y);
					} else {
						path.lineTo(p.x, p.y);
					}
				}
			}
		}

		if (path != null && len > 0) {
			canvas.drawPath(path, paint);
			if (updatePaint(render, paint, 1, false, rc)) {
				canvas.drawPath(path, paint);
			}
			textRenderer.renderText(obj, render, rc, pair, xText / len, yText / len, null, null);
		}
	}
	
	public boolean updatePaint(RenderingRuleSearchRequest req, Paint p, int ind, boolean area, RenderingContext rc){
		RenderingRuleProperty rColor;
		RenderingRuleProperty rStrokeW;
		RenderingRuleProperty rCap;
		RenderingRuleProperty rPathEff;
		
		if (ind == 0) {
			rColor = req.ALL.R_COLOR;
			rStrokeW = req.ALL.R_STROKE_WIDTH;
			rCap = req.ALL.R_CAP;
			rPathEff = req.ALL.R_PATH_EFFECT;
		} else if(ind == 1){
			rColor = req.ALL.R_COLOR_2;
			rStrokeW = req.ALL.R_STROKE_WIDTH_2;
			rCap = req.ALL.R_CAP_2;
			rPathEff = req.ALL.R_PATH_EFFECT_2;
		} else if(ind == -1){
			rColor = req.ALL.R_COLOR_0;
			rStrokeW = req.ALL.R_STROKE_WIDTH_0;
			rCap = req.ALL.R_CAP_0;
			rPathEff = req.ALL.R_PATH_EFFECT_0;
		} else if(ind == -2){
			rColor = req.ALL.R_COLOR__1;
			rStrokeW = req.ALL.R_STROKE_WIDTH__1;
			rCap = req.ALL.R_CAP__1;
			rPathEff = req.ALL.R_PATH_EFFECT__1;
		} else if(ind == 2){
			rColor = req.ALL.R_COLOR_3;
			rStrokeW = req.ALL.R_STROKE_WIDTH_3;
			rCap = req.ALL.R_CAP_3;
			rPathEff = req.ALL.R_PATH_EFFECT_3;
		} else if(ind == -3){
			rColor = req.ALL.R_COLOR__2;
			rStrokeW = req.ALL.R_STROKE_WIDTH__2;
			rCap = req.ALL.R_CAP__2;
			rPathEff = req.ALL.R_PATH_EFFECT__2;
		} else if(ind == 3){
			rColor = req.ALL.R_COLOR_4;
			rStrokeW = req.ALL.R_STROKE_WIDTH_4;
			rCap = req.ALL.R_CAP_4;
			rPathEff = req.ALL.R_PATH_EFFECT_4;
		} else {
			rColor = req.ALL.R_COLOR_5;
			rStrokeW = req.ALL.R_STROKE_WIDTH_5;
			rCap = req.ALL.R_CAP_5;
			rPathEff = req.ALL.R_PATH_EFFECT_5;
		}
		if(area){
			if(!req.isSpecified(rColor) && !req.isSpecified(req.ALL.R_SHADER)){
				return false;
			}
			p.setShader(null);
			p.setColorFilter(null);
			p.clearShadowLayer();
			p.setStyle(Style.FILL_AND_STROKE);
			p.setStrokeWidth(0);
		} else {
			if(!req.isSpecified(rStrokeW)){
				return false;
			}
			p.setShader(null);
			p.setColorFilter(null);
			p.clearShadowLayer();
			p.setStyle(Style.STROKE);
			p.setStrokeWidth(rc.getComplexValue(req, rStrokeW));
			cachedStrokeWidth = p.getStrokeWidth();

			String cap = req.getStringPropertyValue(rCap);
			if(!Algorithms.isEmpty(cap)){
				p.setStrokeCap(Cap.valueOf(cap.toUpperCase()));
			} else {
				p.setStrokeCap(Cap.BUTT);
			}
			String pathEffect = req.getStringPropertyValue(rPathEff);
			if (!Algorithms.isEmpty(pathEffect)) {
				if(!parsedDashEffects.containsKey(pathEffect)) {
					String[] vls = pathEffect.split("_");
					float[] vs = new float[vls.length * 2];
					for(int i = 0; i < vls.length; i++) {
						int s = vls[i].indexOf(':');
						String pre = vls[i];
						String post = "";
						if(s != -1) {
							pre = vls[i].substring(0, i);
							post = vls[i].substring(i + 1);
						}
						if(pre.length() > 0) {
							vs[i*2 ] = Float.parseFloat(pre);
						}
						if(post.length() > 0) {
							vs[i*2 +1] = Float.parseFloat(post);
						}
					}
					parsedDashEffects.put(pathEffect, vs);
				}
				float[] cachedValues = parsedDashEffects.get(pathEffect);
				
				p.setPathEffect(getDashEffect(rc, cachedValues, 0));
			} else {
				p.setPathEffect(null);
			}
		}
		p.setColor(req.getIntPropertyValue(rColor));
		if(ind == 0){
			String resId = req.getStringPropertyValue(req.ALL.R_SHADER);
			if(resId != null){
				if(req.getIntPropertyValue(rColor) == 0) {
					p.setColor(Color.WHITE); // set color required by skia
				}
				p.setShader(getShader(resId));
			}
			// do not check shadow color here
			if(rc.shadowRenderingMode == 1) {
				int shadowColor = req.getIntPropertyValue(req.ALL.R_SHADOW_COLOR);
				if(shadowColor == 0) {
					shadowColor = rc.shadowRenderingColor;
				}
				int shadowRadius = (int) rc.getComplexValue(req, req.ALL.R_SHADOW_RADIUS);
				if (shadowColor == 0) {
					shadowRadius = 0;
				}
				p.setShadowLayer(shadowRadius, 0, 0, shadowColor);
			}
		}
		
		return true;
		
	}
	

	private void drawPoint(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Canvas canvas, RenderingContext rc, TagValuePair pair, boolean renderText) {
		if(render == null || pair == null){
			return;
		}
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom, obj);
		render.search(RenderingRulesStorage.POINT_RULES);
		
		String resId = render.getStringPropertyValue(render.ALL.R_ICON);
		if(resId == null && !renderText){
			return;
		}
		int len = obj.getPointsLength();
		rc.visible++;
		PointF ps = new PointF(0, 0);
		for (int i = 0; i < len; i++) {
			PointF p = calcPoint(obj, i, rc);
			ps.x += p.x;
			ps.y += p.y;
		}
		if(len > 1){
			ps.x /= len;
			ps.y /= len;
		}

		if(resId != null){
			IconDrawInfo ico = new IconDrawInfo();
			ico.x = ps.x;
			ico.y = ps.y;
			ico.iconOrder = render.getIntPropertyValue(render.ALL.R_ICON_ORDER, 100);
			ico.iconSize = rc.getComplexValue(render, render.ALL.R_ICON_VISIBLE_SIZE, -1);
			ico.shieldId = render.getStringPropertyValue(render.ALL.R_SHIELD);
			ico.resId_1 = render.getStringPropertyValue(render.ALL.R_ICON__1);
			ico.resId = resId;
			ico.resId2 = render.getStringPropertyValue(render.ALL.R_ICON_2);
			ico.resId3 = render.getStringPropertyValue(render.ALL.R_ICON_3);
			ico.resId4 = render.getStringPropertyValue(render.ALL.R_ICON_4);
			ico.resId5 = render.getStringPropertyValue(render.ALL.R_ICON_5);
			rc.iconsToDraw.add(ico);
		}
		if (renderText) {
			textRenderer.renderText(obj, render, rc, pair, ps.x, ps.y, null, null);
		}

	}

	private void drawPolylineShadow(Canvas canvas, RenderingContext rc, Path path, int shadowColor, int shadowRadius) {
		// blurred shadows
		if (rc.shadowRenderingMode == 2 && shadowRadius > 0) {
			// simply draw shadow? difference from option 3 ?
			// paint.setColor(shadowRadius);
			// paint.setColor(0xffffffff);
			paint.setShadowLayer(shadowRadius, 0, 0, shadowColor);
			canvas.drawPath(path, paint);
		}

		// option shadow = 3 with solid border
		if (rc.shadowRenderingMode == 3 && shadowRadius > 0) {
			paint.clearShadowLayer();
			paint.setStrokeWidth(paint.getStrokeWidth() + shadowRadius * 2);
			cachedStrokeWidth = paint.getStrokeWidth();

			ColorFilter cf = new PorterDuffColorFilter(shadowColor, Mode.SRC_IN);
			paint.setColorFilter(cf);
//			 paint.setColor(0xffbababa);
//			paint.setColor(shadowColor);
			canvas.drawPath(path, paint);
		}
	}

	
	private void drawPolyline(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Canvas canvas, RenderingContext rc, TagValuePair pair, int layer,
			boolean drawOnlyShadow) {
		if(render == null || pair == null){
			return;
		}
		int length = obj.getPointsLength();
		if(length < 2){
			return;
		}
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom, obj);
		render.setIntFilter(render.ALL.R_LAYER, layer);
		boolean rendered = render.search(RenderingRulesStorage.LINE_RULES);
		if(!rendered || !updatePaint(render, paint, 0, false, rc)){
			return;
		}
		int oneway = 0;
		if(rc.zoom >= 16 && "highway".equals(pair.tag) ){ //$NON-NLS-1$
			if(obj.containsAdditionalType(obj.getMapIndex().onewayAttribute)) {
				oneway = 1;
			} else if(obj.containsAdditionalType(obj.getMapIndex().onewayReverseAttribute)){
				oneway = -1;
			}
		}

		rc.visible++;

		Path path = null;
		float xMid = 0;
		float yMid = 0;
		int middle = obj.getPointsLength() / 2;
		PointF[] textPoints = null;
		if (!drawOnlyShadow) {
			textPoints = new PointF[length];
		}

		boolean intersect = false;
		PointF prev = null;
		for (int i = 0; i < length ; i++) {
			PointF p = calcPoint(obj, i, rc);
			if(textPoints != null) {
				textPoints[i] = new PointF(p.x, p.y);
			}
			if (!intersect) {
				if (p.x >= 0 && p.y >= 0 && p.x < rc.width && p.y < rc.height) {
					intersect = true;
				}
				if (!intersect && prev != null) {
					if ((p.x < 0 && prev.x < 0) || (p.y < 0 && prev.y < 0) || (p.x > rc.width && prev.x > rc.width)
							|| (p.y > rc.height && prev.y > rc.height)) {
						intersect = false;
					} else {
						intersect = true;
					}

				}
			}
			if (path == null) {
				path = new Path();
				path.moveTo(p.x, p.y);
			} else {
				if(i == middle){
					xMid = p.x;
					yMid = p.y;
				}
				path.lineTo(p.x, p.y);
			}
			prev = p;
		}
		if (!intersect) {
//			System.err.println("Not intersect ");
//			int[] ts = obj.getTypes();
//			for(int i=0; i<ts.length; i++) {
//				System.err.println(obj.getMapIndex().decodeType(ts[i]));
//			}
			return;
		}
		if (path != null) {
			if(drawOnlyShadow) {
				int shadowColor = render.getIntPropertyValue(render.ALL.R_SHADOW_COLOR);
				int shadowRadius = (int) rc.getComplexValue(render, render.ALL.R_SHADOW_RADIUS);
				if(shadowColor == 0) {
					shadowColor = rc.shadowRenderingColor;
				}
				drawPolylineShadow(canvas, rc, path, shadowColor, shadowRadius);
			} else {
				boolean update = false;
				if (updatePaint(render, paint, -3, false, rc)) {
					update = true;
					canvas.drawPath(path, paint);
				}
				if (updatePaint(render, paint, -2, false, rc)) {
					update = true;
					canvas.drawPath(path, paint);
				}
				if (updatePaint(render, paint, -1, false, rc)) {
					update = true;
					canvas.drawPath(path, paint);
				}
				if(update) {
					updatePaint(render, paint, 0, false, rc);
				}
				canvas.drawPath(path, paint);
				if (updatePaint(render, paint, 1, false, rc)) {
					canvas.drawPath(path, paint);
				}
				if (updatePaint(render, paint, 2, false, rc)) {
					canvas.drawPath(path, paint);
				}
				if (updatePaint(render, paint, 3, false, rc)) {
					canvas.drawPath(path, paint);
				}
				if (updatePaint(render, paint, 4, false, rc)) {
					canvas.drawPath(path, paint);
				}
			}
			
			if(oneway != 0 && !drawOnlyShadow){
				Paint[] paints = oneway == -1? getReverseOneWayPaints(rc) :  getOneWayPaints(rc);
				for (int i = 0; i < paints.length; i++) {
					canvas.drawPath(path, paints[i]);
				}
			}
			if (textPoints != null) {
				textRenderer.renderText(obj, render, rc, pair, xMid, yMid, path, textPoints);
			}
		}
	}

		
	private static Paint oneWayPaint(){
		Paint oneWay = new Paint();
		oneWay.setStyle(Style.STROKE);
		oneWay.setColor(0xff6c70d5);
		oneWay.setAntiAlias(true);
		return oneWay; 
	}
	
	public Paint[] getReverseOneWayPaints(RenderingContext rc){
		if(rc.reverseOneWay == null){
			int rmin = (int)rc.getDensityValue(1);
			if(rmin > 2) {
				rmin = rmin / 2;
			}
			PathEffect arrowDashEffect1 = new DashPathEffect(new float[] { 0, 12, 10 * rmin, 152 }, 0);
			PathEffect arrowDashEffect2 = new DashPathEffect(new float[] { 0, 12 + rmin, 9 * rmin, 152 }, 1);
			PathEffect arrowDashEffect3 = new DashPathEffect(new float[] { 0, 12 + 2 * rmin, 2 * rmin, 152 + 6 * rmin }, 1);
			PathEffect arrowDashEffect4 = new DashPathEffect(new float[] { 0, 12 + 3 * rmin, 1 * rmin, 152 + 6 * rmin }, 1);
			rc.reverseOneWay = new Paint[4];
			rc.reverseOneWay[0] = oneWayPaint();
			rc.reverseOneWay[0].setStrokeWidth(rmin * 2);
			rc.reverseOneWay[0].setPathEffect(arrowDashEffect1);
			
			rc.reverseOneWay[1] = oneWayPaint();
			rc.reverseOneWay[1].setStrokeWidth(rmin);
			rc.reverseOneWay[1].setPathEffect(arrowDashEffect2);

			rc.reverseOneWay[2] = oneWayPaint();
			rc.reverseOneWay[2].setStrokeWidth(rmin * 3);
			rc.reverseOneWay[2].setPathEffect(arrowDashEffect3);
			
			rc.reverseOneWay[3] = oneWayPaint();			
			rc.reverseOneWay[3].setStrokeWidth(rmin * 4);
			rc.reverseOneWay[3].setPathEffect(arrowDashEffect4);
			
		}
		return rc.reverseOneWay;
	}
	
	public Paint[] getOneWayPaints(RenderingContext rc){
		if(rc.oneWay == null){
			float rmin = rc.getDensityValue(1);
			if(rmin > 1) {
				rmin = rmin * 2 / 3;
			}
			PathEffect arrowDashEffect1 = new DashPathEffect(new float[] { 0, 12, 10 * rmin, 152 }, 0);
			PathEffect arrowDashEffect2 = new DashPathEffect(new float[] { 0, 12, 9 * rmin, 152 + rmin }, 1);
			PathEffect arrowDashEffect3 = new DashPathEffect(new float[] { 0, 12 + 6 * rmin, 2 * rmin, 152 + 2 * rmin}, 1);
			PathEffect arrowDashEffect4 = new DashPathEffect(new float[] { 0, 12 + 6 * rmin, 1 * rmin, 152 + 3 * rmin}, 1);
			rc.oneWay = new Paint[4];
			rc.oneWay[0] = oneWayPaint();
			rc.oneWay[0].setStrokeWidth(rmin);
			rc.oneWay[0].setPathEffect(arrowDashEffect1);
			
			rc.oneWay[1] = oneWayPaint();
			rc.oneWay[1].setStrokeWidth(rmin * 2);
			rc.oneWay[1].setPathEffect(arrowDashEffect2);

			rc.oneWay[2] = oneWayPaint();
			rc.oneWay[2].setStrokeWidth(rmin * 3);
			rc.oneWay[2].setPathEffect(arrowDashEffect3);
			
			rc.oneWay[3] = oneWayPaint();			
			rc.oneWay[3].setStrokeWidth(rmin * 4);
			rc.oneWay[3].setPathEffect(arrowDashEffect4);
			
		}
		return rc.oneWay;
	}
}
