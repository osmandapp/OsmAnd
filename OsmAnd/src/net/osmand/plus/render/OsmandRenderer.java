package net.osmand.plus.render;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MultyPolygon;
import net.osmand.plus.render.NativeOsmandLibrary.NativeSearchResult;
import net.osmand.plus.render.TextRenderer.TextDrawInfo;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.Xfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.WindowManager;

public class OsmandRenderer {
	private static final Log log = LogUtil.getLog(OsmandRenderer.class);

	private Paint paint;

	private Paint paintIcon;

	public static final int TILE_SIZE = 256; 

	private Map<String, PathEffect> dashEffect = new LinkedHashMap<String, PathEffect>();
	private Map<String, Shader> shaders = new LinkedHashMap<String, Shader>();

	private final Context context;

	private DisplayMetrics dm;

	private TextRenderer textRenderer;


	private static class IconDrawInfo {
		float x = 0;
		float y = 0;
		String resId;
	}
	
	static enum ShadowRenderingMode {
		// int shadowRenderingMode = 0; // no shadow (minumum CPU)
		// int shadowRenderingMode = 1; // classic shadow (the implementaton in master)
		// int shadowRenderingMode = 2; // blur shadow (most CPU, but still reasonable)
		// int shadowRenderingMode = 3; solid border (CPU use like classic version or even smaller)
		NO_SHADOW(0),
		ONE_STEP(1),
		BLUR_SHADOW(2),
		SOLID_SHADOW(3);
		public final int value;
		ShadowRenderingMode(int v) {
			this.value = v;
		}
	}

	/*package*/ static class RenderingContext {
		// FIELDS OF THAT CLASS ARE USED IN C++
		public boolean interrupted = false;
		public boolean nightMode = false;
		public boolean highResMode = false;
		public float mapTextSize = 1;
		public float density = 1;
		public final Context ctx;

		List<TextDrawInfo> textToDraw = new ArrayList<TextDrawInfo>();
		List<IconDrawInfo> iconsToDraw = new ArrayList<IconDrawInfo>();
		
		public RenderingContext(Context ctx) {
			this.ctx = ctx;
		}

		float leftX;
		float topY;
		int width;
		int height;

		int zoom;
		float rotate;
		float tileDivisor;

		// debug purpose
		int pointCount = 0;
		int pointInsideCount = 0;
		int visible = 0;
		int allObjects = 0;
		int textRenderingTime = 0;
		int lastRenderedKey = 0;

		// use to calculate points
		PointF tempPoint = new PointF();
		float cosRotateTileSize;
		float sinRotateTileSize;
		
		// be aware field is using in C++
		int shadowRenderingMode = ShadowRenderingMode.BLUR_SHADOW.value;
		int shadowRenderingColor = 0xff969696;
		
		// not expect any shadow
		int shadowLevelMin = 256;
		int shadowLevelMax = 0;

		String renderingDebugInfo;
		
		boolean ended = false;
		
		float getDensityValue(float val) {
			if (highResMode && density > 1) {
				return val * density * mapTextSize;
			} else {
				return val * mapTextSize;
			}
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

	public PathEffect getDashEffect(String dashes){
		if(!dashEffect.containsKey(dashes)){
			String[] ds = dashes.split("_"); //$NON-NLS-1$
			float[] f = new float[ds.length];
			for(int i=0; i<ds.length; i++){
				f[i] = Float.parseFloat(ds[i]);
			}
			dashEffect.put(dashes, new DashPathEffect(f, 0));
		}
		return dashEffect.get(dashes);
	}

	public Shader getShader(String resId){
		
		if(shaders.get(resId) == null){
			Bitmap bmp = RenderingIcons.getIcon(context, resId);
			if(bmp != null){
				Shader sh = new BitmapShader(bmp, TileMode.REPEAT, TileMode.REPEAT);
				shaders.put(resId, sh);
			} else {
				shaders.put(resId, null);
			}
		}	
		return shaders.get(resId);
	}
	
	private void put(TIntObjectHashMap<TIntArrayList> map, int k, int v, int init){
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
			Bitmap bmp, boolean useEnglishNames,
			RenderingRuleSearchRequest render, final List<IMapDownloaderCallback> notifyList, int defaultColor) {
		long now = System.currentTimeMillis();
		if (rc.width > 0 && rc.height > 0 && searchResultHandler != null) {
			// init rendering context
			rc.tileDivisor = (int) (1 << (31 - rc.zoom));
			rc.cosRotateTileSize = FloatMath.cos((float) Math.toRadians(rc.rotate)) * TILE_SIZE;
			rc.sinRotateTileSize = FloatMath.sin((float) Math.toRadians(rc.rotate)) * TILE_SIZE;
			rc.density = dm.density;
			try {
				if(Looper.getMainLooper() != null && library.useDirectRendering()) {
					final Handler h = new Handler(Looper.getMainLooper());
					notifyListenersWithDelay(rc, notifyList, h);
				}
				
				// Native library will decide on it's own best way of rendering
				// If res.bitmapBuffer is null, it indicates that rendering was done directly to
				// memory of passed bitmap, but this is supported only on Android >= 2.2
				final NativeOsmandLibrary.RenderingGenerationResult res = library.generateRendering(
					rc, searchResultHandler,
					bmp, bmp.getWidth(), bmp.getHeight(), bmp.getRowBytes(), bmp.hasAlpha(),
					useEnglishNames, render, defaultColor);
				rc.ended = true;
				notifyListeners(notifyList);
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
	
	public void generateNewBitmap(RenderingContext rc, List<BinaryMapDataObject> objects, Bitmap bmp, boolean useEnglishNames,
			RenderingRuleSearchRequest render, final List<IMapDownloaderCallback> notifyList, int defaultColor) {
		long now = System.currentTimeMillis();

		if (objects != null && !objects.isEmpty() && rc.width > 0 && rc.height > 0) {
			// init rendering context
			rc.tileDivisor = (int) (1 << (31 - rc.zoom));
			rc.cosRotateTileSize = FloatMath.cos((float) Math.toRadians(rc.rotate)) * TILE_SIZE;
			rc.sinRotateTileSize = FloatMath.sin((float) Math.toRadians(rc.rotate)) * TILE_SIZE;
			rc.density = dm.density;

			// fill area
			Canvas cv = new Canvas(bmp);
			if (defaultColor != 0) {
				cv.drawColor(defaultColor);
			}
			// put in order map
			TIntObjectHashMap<TIntArrayList> orderMap = sortObjectsByProperOrder(rc, objects, render);

			int objCount = 0;

			int[] keys = orderMap.keys();
			Arrays.sort(keys);

			boolean shadowDrawn = false;

			for (int k = 0; k < keys.length; k++) {
				if (!shadowDrawn && keys[k] >= rc.shadowLevelMin && keys[k] <= rc.shadowLevelMax && rc.shadowRenderingMode > 1) {
					for (int ki = k; ki < keys.length; ki++) {
						if (keys[ki] > rc.shadowLevelMax || rc.interrupted) {
							break;
						}
						TIntArrayList list = orderMap.get(keys[ki]);
						for (int j = 0; j < list.size(); j++) {
							int i = list.get(j);
							int ind = i >> 8;
							int l = i & 0xff;
							BinaryMapDataObject obj = objects.get(ind);

							// show text only for main type
							drawObj(obj, render, cv, rc, l, l == 0, true);
							objCount++;
						}
					}
					shadowDrawn = true;
				}
				if (rc.interrupted) {
					return;
				}

				TIntArrayList list = orderMap.get(keys[k]);
				for (int j = 0; j < list.size(); j++) {
					int i = list.get(j);
					int ind = i >> 8;
					int l = i & 0xff;
					BinaryMapDataObject obj = objects.get(ind);

					// show text only for main type
					drawObj(obj, render, cv, rc, l, l == 0, false);
					objCount++;
				}
				rc.lastRenderedKey = keys[k];
				if (objCount > 25) {
					notifyListeners(notifyList);
					objCount = 0;
				}

			}

			long beforeIconTextTime = System.currentTimeMillis() - now;
			notifyListeners(notifyList);
			drawIconsOverCanvas(rc, cv);

			notifyListeners(notifyList);
			textRenderer.drawTextOverCanvas(rc, cv, useEnglishNames);

			long time = System.currentTimeMillis() - now;
			rc.renderingDebugInfo = String.format("Rendering: %s ms  (%s text)\n"
					+ "(%s points, %s points inside, %s of %s objects visible)",//$NON-NLS-1$
					time, time - beforeIconTextTime, rc.pointCount, rc.pointInsideCount, rc.visible, rc.allObjects);
			log.info(rc.renderingDebugInfo);

		}

		return;
	}

	private void notifyListenersWithDelay(final RenderingContext rc, final List<IMapDownloaderCallback> notifyList, final Handler h) {
		h.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(!rc.ended) {
					notifyListeners(notifyList);
					notifyListenersWithDelay(rc, notifyList, h);
				}
			}
		}, 800);
	}

	private void drawIconsOverCanvas(RenderingContext rc, Canvas cv) {
		int skewConstant = (int) rc.getDensityValue(16);
		int iconsW = rc.width / skewConstant;
		int iconsH = rc.height / skewConstant;
		int[] alreadyDrawnIcons = new int[iconsW * iconsH / 32];
		for (IconDrawInfo icon : rc.iconsToDraw) {
			if (icon.resId != null) {
				Bitmap ico = RenderingIcons.getIcon(context, icon.resId);
				if (ico != null) {
					if (icon.y >= 0 && icon.y < rc.height && icon.x >= 0 && icon.x < rc.width) {
						int z = (((int) icon.x / skewConstant) + ((int) icon.y / skewConstant) * iconsW);
						int i = z / 32;
						if (i >= alreadyDrawnIcons.length) {
							continue;
						}
						int ind = alreadyDrawnIcons[i];
						int b = z % 32;
						// check bit b if it is set
						if (((ind >> b) & 1) == 0) {
							alreadyDrawnIcons[i] = ind | (1 << b);
							cv.drawBitmap(ico, icon.x - ico.getWidth() / 2, icon.y - ico.getHeight() / 2, paintIcon);
						}
					}
				}
			}
			if (rc.interrupted) {
				return;
			}
		}
	}

	private TIntObjectHashMap<TIntArrayList> sortObjectsByProperOrder(RenderingContext rc, List<BinaryMapDataObject> objects,
			RenderingRuleSearchRequest render) {
		int sz = objects.size();
		int init = sz / 4;
		TIntObjectHashMap<TIntArrayList> orderMap = new TIntObjectHashMap<TIntArrayList>();
		if (render != null) {
			render.clearState();
			
			for (int i = 0; i < sz; i++) {
				BinaryMapDataObject o = objects.get(i);
				int sh = i << 8;
				if (o instanceof MultyPolygon) {
					int layer = ((MultyPolygon) o).getLayer();
					render.setTagValueZoomLayer(((MultyPolygon) o).getTag(), ((MultyPolygon) o).getValue(), rc.zoom, layer);
					render.setIntFilter(render.ALL.R_ORDER_TYPE, MapRenderingTypes.POLYGON_TYPE);
					if(render.search(RenderingRulesStorage.ORDER_RULES)) {
						int order = render.getIntPropertyValue(render.ALL.R_ORDER);
						put(orderMap, order, sh, init);
						if(render.isSpecified(render.ALL.R_SHADOW_LEVEL)){
							rc.shadowLevelMin = Math.min(rc.shadowLevelMin, order);
							rc.shadowLevelMax = Math.max(rc.shadowLevelMax, order);
							render.clearValue(render.ALL.R_SHADOW_LEVEL);
						}
					}
				} else {
					for (int j = 0; j < o.getTypes().length; j++) {
						// put(orderMap, BinaryMapDataObject.getOrder(o.getTypes()[j]), sh + j, init);
						int wholeType = o.getTypes()[j];
						
						int mask = MapRenderingTypes.POINT_TYPE;
						if(o.getPointsLength() > 1) {
							if(o.isArea()){
								mask = MapRenderingTypes.POLYGON_TYPE;
							} else {
								mask = MapRenderingTypes.POLYLINE_TYPE;
							}
						}
						int layer = 0;
						if (mask != MapRenderingTypes.POINT_TYPE) {
							// FIXME Layer
//							layer = MapRenderingTypes.getNegativeWayLayer(wholeType);
						}

						TagValuePair pair = o.getMapIndex().decodeType(wholeType);
						if (pair != null) {
							render.setTagValueZoomLayer(pair.tag, pair.value, rc.zoom, layer);
							render.setIntFilter(render.ALL.R_ORDER_TYPE, mask);
							if (render.search(RenderingRulesStorage.ORDER_RULES)) {
								int order = render.getIntPropertyValue(render.ALL.R_ORDER);
								put(orderMap, order, sh + j, init);
								if (render.isSpecified(render.ALL.R_SHADOW_LEVEL)) {
									rc.shadowLevelMin = Math.min(rc.shadowLevelMin, order);
									rc.shadowLevelMax = Math.max(rc.shadowLevelMax, order);
									render.clearValue(render.ALL.R_SHADOW_LEVEL);
								}
							}
						}

					}
				}

				if (rc.interrupted) {
					return orderMap;
				}
			}
		}
		return orderMap;
	}
	private void notifyListeners(List<IMapDownloaderCallback> notifyList) {
		if (notifyList != null) {
			for (IMapDownloaderCallback c : notifyList) {
				c.tileDownloaded(null);
			}
		}
	}

	protected void drawObj(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Canvas canvas, RenderingContext rc, int l,
			boolean renderText, boolean drawOnlyShadow) {
		rc.allObjects++;
		if (obj instanceof MultyPolygon) {
			if(!drawOnlyShadow){
				drawMultiPolygon(obj, render, canvas, rc);
			}
		} else {
			int mainType = obj.getTypes()[l];
			TagValuePair pair = obj.getMapIndex().decodeType(mainType);
			if (obj.getPointsLength() == 1 && !drawOnlyShadow) {
				drawPoint(obj, render, canvas, rc, pair, renderText);
			} else if (!obj.isArea()) {
				// FIXME Layer
				// int layer = MapRenderingTypes.getNegativeWayLayer(mainType);
				drawPolyline(obj, render, canvas, rc, pair, 0, drawOnlyShadow);
			} else if (obj.isArea() && !drawOnlyShadow) {
				drawPolygon(obj, render, canvas, rc, pair);
			}
		}
	}


	private PointF calcPoint(BinaryMapDataObject o, int ind, RenderingContext rc){
		rc.pointCount ++;
		float tx = o.getPoint31XTile(ind) / rc.tileDivisor;
		float ty = o.getPoint31YTile(ind) / rc.tileDivisor;
		float dTileX = tx - rc.leftX;
		float dTileY = ty - rc.topY;
		float x = rc.cosRotateTileSize * dTileX - rc.sinRotateTileSize * dTileY;
		float y = rc.sinRotateTileSize * dTileX + rc.cosRotateTileSize * dTileY;
		rc.tempPoint.set(x, y);
		if(rc.tempPoint.x >= 0 && rc.tempPoint.x < rc.width && 
				rc.tempPoint.y >= 0 && rc.tempPoint.y < rc.height){
			rc.pointInsideCount++;
		}
		return rc.tempPoint;
	}

	private PointF calcMultiPolygonPoint(MultyPolygon o, int i, int b, RenderingContext rc){
		rc.pointCount ++;
		float tx = o.getPoint31XTile(i, b)/ rc.tileDivisor;
		float ty = o.getPoint31YTile(i, b) / rc.tileDivisor;
		float dTileX = tx - rc.leftX;
		float dTileY = ty - rc.topY;
		float x = rc.cosRotateTileSize * dTileX - rc.sinRotateTileSize * dTileY;
		float y = rc.sinRotateTileSize * dTileX + rc.cosRotateTileSize * dTileY;
		rc.tempPoint.set(x, y);
		if(rc.tempPoint.x >= 0 && rc.tempPoint.x < rc.width && 
				rc.tempPoint.y >= 0 && rc.tempPoint.y < rc.height){
			rc.pointInsideCount++;
		}
		return rc.tempPoint;
	}

	public void clearCachedResources(){
		shaders.clear();
	}
	
	private void drawMultiPolygon(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Canvas canvas, RenderingContext rc) {
		String tag = ((MultyPolygon)obj).getTag();
		String value = ((MultyPolygon)obj).getValue();
		if(render == null || tag == null){
			return;
		}
		render.setInitialTagValueZoom(tag, value, rc.zoom);
		boolean rendered = render.search(RenderingRulesStorage.POLYGON_RULES);
		if(!rendered || !updatePaint(render, paint, 0, true, rc)){
			return;
		}
		rc.visible++;
		Path path = new Path();
		for (int i = 0; i < ((MultyPolygon) obj).getBoundsCount(); i++) {
			int cnt = ((MultyPolygon) obj).getBoundPointsCount(i);
			float xText = 0;
			float yText = 0;
			for (int j = 0; j < cnt; j++) {
				PointF p = calcMultiPolygonPoint((MultyPolygon) obj, j, i, rc);
				xText += p.x;
				yText += p.y;
				if (j == 0) {
					path.moveTo(p.x, p.y);
				} else {
					path.lineTo(p.x, p.y);
				}
			}
			if (cnt > 0) {
				textRenderer.renderText(obj, render, rc, new TagValuePair(tag, value, 0), xText / cnt, yText / cnt, null, null);
			}
		}
		canvas.drawPath(path, paint);
		// for test purpose 
//		paint.setStyle(Style.STROKE);
//		paint.setStrokeWidth(1.5f);
//		paint.setColor(Color.BLACK);
//		paint.setPathEffect(null);
//		canvas.drawPath(path, paint);
		
		if (updatePaint(render, paint, 1, false, rc)) {
			canvas.drawPath(path, paint);
		}
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
		render.setInitialTagValueZoom(pair.tag, pair.value, zoom);
		boolean rendered = render.search(RenderingRulesStorage.POLYGON_RULES);
		if(!rendered || !updatePaint(render, paint, 0, true, rc)){
			return;
		}
		rc.visible++;
		int len = obj.getPointsLength();
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

		if (path != null && len > 0) {
			canvas.drawPath(path, paint);
			if (updatePaint(render, paint, 1, false, rc)) {
				canvas.drawPath(path, paint);
			}
			textRenderer.renderText(obj, render, rc, pair, xText / len, yText / len, null, null);
		}
	}
	
	private boolean updatePaint(RenderingRuleSearchRequest req, Paint p, int ind, boolean area, RenderingContext rc){
		RenderingRuleProperty rColor;
		RenderingRuleProperty rStrokeW;
		RenderingRuleProperty rCap;
		RenderingRuleProperty rPathEff;
		
		p.setShader(null);
		p.setColorFilter(null);
		p.clearShadowLayer();
		if(ind == 0){
			rColor = req.ALL.R_COLOR;
			rStrokeW = req.ALL.R_STROKE_WIDTH;
			rCap = req.ALL.R_CAP;
			rPathEff = req.ALL.R_PATH_EFFECT;
		} else if(ind == 1){
			rColor = req.ALL.R_COLOR_2;
			rStrokeW = req.ALL.R_STROKE_WIDTH_2;
			rCap = req.ALL.R_CAP_2;
			rPathEff = req.ALL.R_PATH_EFFECT_2;
		} else {
			rColor = req.ALL.R_COLOR_3;
			rStrokeW = req.ALL.R_STROKE_WIDTH_3;
			rCap = req.ALL.R_CAP_3;
			rPathEff = req.ALL.R_PATH_EFFECT_3;
		}
		if(area){
			if(!req.isSpecified(rColor) && !req.isSpecified(req.ALL.R_SHADER)){
				return false;
			}
			p.setStyle(Style.FILL_AND_STROKE);
			p.setStrokeWidth(0);
		} else {
			if(!req.isSpecified(rStrokeW)){
				return false;
			}
			p.setStyle(Style.STROKE);
			p.setStrokeWidth(req.getFloatPropertyValue(rStrokeW));
			String cap = req.getStringPropertyValue(rCap);
			if(!Algoritms.isEmpty(cap)){
				p.setStrokeCap(Cap.valueOf(cap.toUpperCase()));
			} else {
				p.setStrokeCap(Cap.BUTT);
			}
			String pathEffect = req.getStringPropertyValue(rPathEff);
			if (!Algoritms.isEmpty(pathEffect)) {
				p.setPathEffect(getDashEffect(pathEffect));
			} else {
				p.setPathEffect(null);
			}
		}
		p.setColor(req.getIntPropertyValue(rColor));
		if(ind == 0){
			String resId = req.getStringPropertyValue(req.ALL.R_SHADER);
			if(resId != null){
				p.setShader(getShader(resId));
			}
			// do not check shadow color here
			if(rc.shadowRenderingMode == 1) {
				int shadowColor = req.getIntPropertyValue(req.ALL.R_SHADOW_COLOR);
				if(shadowColor == 0) {
					shadowColor = rc.shadowRenderingColor;
				}
				int shadowLayer = req.getIntPropertyValue(req.ALL.R_SHADOW_RADIUS);
				if (shadowColor == 0) {
					shadowLayer = 0;
				}
				p.setShadowLayer(shadowLayer, 0, 0, shadowColor);
			}
		}
		
		return true;
		
	}
	

	private void drawPoint(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Canvas canvas, RenderingContext rc, TagValuePair pair, boolean renderText) {
		if(render == null || pair == null){
			return;
		}
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom);
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
			ico.resId = resId;
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
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom);
		render.setIntFilter(render.ALL.R_LAYER, layer);
		boolean rendered = render.search(RenderingRulesStorage.LINE_RULES);
		if(!rendered || !updatePaint(render, paint, 0, false, rc)){
			return;
		}
		boolean oneway = false;
		//FIXME oneway
//		if(rc.zoom >= 16 && "highway".equals(pair.tag) && MapRenderingTypes.isOneWayWay(obj.getHighwayAttributes())){ //$NON-NLS-1$
//			oneway = true;
//		}

		rc.visible++;

		Path path = null;
		float xMid = 0;
		float yMid = 0;
		int middle = obj.getPointsLength() / 2;
		PointF[] textPoints = null;
		if (!drawOnlyShadow) {
			textPoints = new PointF[length];
		}

		for (int i = 0; i < length ; i++) {
			PointF p = calcPoint(obj, i, rc);
			if(textPoints != null) {
				textPoints[i] = new PointF(p.x, p.y);
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
		}
		if (path != null) {
			if(drawOnlyShadow) {
				int shadowColor = render.getIntPropertyValue(render.ALL.R_SHADOW_COLOR);
				int shadowRadius = render.getIntPropertyValue(render.ALL.R_SHADOW_RADIUS);
				if(shadowColor == 0) {
					shadowColor = rc.shadowRenderingColor;
				}
				drawPolylineShadow(canvas, rc, path, shadowColor, shadowRadius);
			} else {
				canvas.drawPath(path, paint);
				if (updatePaint(render, paint, 1, false, rc)) {
					canvas.drawPath(path, paint);
					if (updatePaint(render, paint, 2, false, rc)) {
						canvas.drawPath(path, paint);
					}
				}
			}
			
			if(oneway && !drawOnlyShadow){
				Paint[] paints = getOneWayPaints();
				for (int i = 0; i < paints.length; i++) {
					canvas.drawPath(path, paints[i]);
				}
			}
			if (textPoints != null) {
				textRenderer.renderText(obj, render, rc, pair, xMid, yMid, path, textPoints);
			}
		}
	}

		
	private static Paint[] oneWay = null;
	private static Paint oneWayPaint(){
		Paint oneWay = new Paint();
		oneWay.setStyle(Style.STROKE);
		oneWay.setColor(0xff6c70d5);
		oneWay.setAntiAlias(true);
		return oneWay; 
	}
	
	public static Paint[] getOneWayPaints(){
		if(oneWay == null){
			PathEffect arrowDashEffect1 = new DashPathEffect(new float[] { 0, 12, 10, 152 }, 0);
			PathEffect arrowDashEffect2 = new DashPathEffect(new float[] { 0, 12, 9, 153 }, 1);
			PathEffect arrowDashEffect3 = new DashPathEffect(new float[] { 0, 18, 2, 154 }, 1);
			PathEffect arrowDashEffect4 = new DashPathEffect(new float[] { 0, 18, 1, 155 }, 1);
			oneWay = new Paint[4];
			oneWay[0] = oneWayPaint();
			oneWay[0].setStrokeWidth(1);
			oneWay[0].setPathEffect(arrowDashEffect1);
			
			oneWay[1] = oneWayPaint();
			oneWay[1].setStrokeWidth(2);
			oneWay[1].setPathEffect(arrowDashEffect2);

			oneWay[2] = oneWayPaint();
			oneWay[2].setStrokeWidth(3);
			oneWay[2].setPathEffect(arrowDashEffect3);
			
			oneWay[3] = oneWayPaint();			
			oneWay[3].setStrokeWidth(4);
			oneWay[3].setPathEffect(arrowDashEffect4);
			
		}
		return oneWay;
	}
}
