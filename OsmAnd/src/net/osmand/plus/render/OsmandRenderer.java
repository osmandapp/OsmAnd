package net.osmand.plus.render;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.WindowManager;

public class OsmandRenderer {
	private static final Log log = LogUtil.getLog(OsmandRenderer.class);

	private final int clFillScreen = Color.rgb(241, 238, 232);

	
	private TextPaint paintText;
	private Paint paint;

	private Paint paintFillEmpty;
	private Paint paintIcon;

	public static final int TILE_SIZE = 256; 

	private Map<String, PathEffect> dashEffect = new LinkedHashMap<String, PathEffect>();
	private Map<String, Shader> shaders = new LinkedHashMap<String, Shader>();

	private final Context context;

	private DisplayMetrics dm;

	private static class TextDrawInfo {

		public TextDrawInfo(String text){
			this.text = text;
		}

		String text = null;
		Path drawOnPath = null;
		float vOffset = 0;
		float centerX = 0;
		float pathRotate = 0;
		float centerY = 0;
		float textSize = 0;
		float minDistance = 0;
		int textColor = Color.BLACK;
		int textShadow = 0;
		int textWrap = 0;
		boolean bold = false;
		String shieldRes = null;
		int textOrder = 100;
		
		public void fillProperties(RenderingRuleSearchRequest render, float centerX, float centerY){
			this.centerX = centerX;
			this.centerY = centerY + render.getIntPropertyValue(render.ALL.R_TEXT_DY, 0);
			textColor = render.getIntPropertyValue(render.ALL.R_TEXT_COLOR);
			if(textColor == 0){
				textColor = Color.BLACK;
			}
			textSize = render.getIntPropertyValue(render.ALL.R_TEXT_SIZE);
			textShadow = render.getIntPropertyValue(render.ALL.R_TEXT_HALO_RADIUS, 0);
			textWrap = render.getIntPropertyValue(render.ALL.R_TEXT_WRAP_WIDTH, 0);
			bold = render.getIntPropertyValue(render.ALL.R_TEXT_BOLD, 0) > 0;
			minDistance = render.getIntPropertyValue(render.ALL.R_TEXT_MIN_DISTANCE,0);
			if(render.isSpecified(render.ALL.R_TEXT_SHIELD)) {
				shieldRes = render.getStringPropertyValue(render.ALL.R_TEXT_SHIELD);
			}
			textOrder = render.getIntPropertyValue(render.ALL.R_TEXT_ORDER, 100);
		}
	}

	private static class IconDrawInfo {
		float x = 0;
		float y = 0;
		String resId;
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
		
		// int shadowRenderingMode = 0; // no shadow (minumum CPU)
		// int shadowRenderingMode = 1; // classic shadow (the implementaton in master)
		// int shadowRenderingMode = 2; // blur shadow (most CPU, but still reasonable)
		// int shadowRenderingMode = 3; solid border (CPU use like classic version or even smaller)
		int shadowRenderingMode = 3;
		
		// not expect any shadow
		int shadowLevelMin = 256;
		int shadowLevelMax = 0;

		String renderingDebugInfo;
		
		boolean ended = false;
	}

	public OsmandRenderer(Context context) {
		this.context = context;

		paintIcon = new Paint();
		paintIcon.setStyle(Style.STROKE);

		paintText = new TextPaint();
		paintText.setStyle(Style.FILL);
		paintText.setStrokeWidth(1);
		paintText.setColor(Color.BLACK);
		paintText.setTextAlign(Align.CENTER);
		paintText.setTypeface(Typeface.create("Droid Serif", Typeface.NORMAL)); //$NON-NLS-1$
		paintText.setAntiAlias(true);

		paint = new Paint();
		paint.setAntiAlias(true);

		paintFillEmpty = new Paint();
		paintFillEmpty.setStyle(Style.FILL);
		paintFillEmpty.setColor(clFillScreen);
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
	public void generateNewBitmapNative(RenderingContext rc, NativeSearchResult searchResultHandler, Bitmap bmp, boolean useEnglishNames,
			RenderingRuleSearchRequest render, final List<IMapDownloaderCallback> notifyList, int defaultColor) {
		long now = System.currentTimeMillis();
		if (rc.width > 0 && rc.height > 0 && searchResultHandler != null) {
			// init rendering context
			rc.tileDivisor = (int) (1 << (31 - rc.zoom));
			rc.cosRotateTileSize = FloatMath.cos((float) Math.toRadians(rc.rotate)) * TILE_SIZE;
			rc.sinRotateTileSize = FloatMath.sin((float) Math.toRadians(rc.rotate)) * TILE_SIZE;
			rc.density = dm.density;
			try {
				if(Looper.getMainLooper() != null){
					final Handler h = new Handler(Looper.getMainLooper());
					notifyListenersWithDelay(rc, notifyList, h);
				}
				String res = NativeOsmandLibrary.generateRendering(rc, searchResultHandler, bmp, useEnglishNames, render, defaultColor);
				rc.ended = true;
				notifyListeners(notifyList);
				long time = System.currentTimeMillis() - now;
				rc.renderingDebugInfo = String.format("Rendering done in %s (%s text) ms\n"
						+ "(%s points, %s points inside, %s objects visile from %s)\n" + res,//$NON-NLS-1$
						time, rc.textRenderingTime, rc.pointCount, rc.pointInsideCount, rc.visible, rc.allObjects);
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
				paintFillEmpty.setColor(defaultColor);
			}
			cv.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), paintFillEmpty);
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
			drawTextOverCanvas(rc, cv, useEnglishNames);

			long time = System.currentTimeMillis() - now;
			rc.renderingDebugInfo = String.format("Rendering done in %s (%s text) ms\n"
					+ "(%s points, %s points inside, %s objects visile from %s)",//$NON-NLS-1$
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
		}, 700);
	}

	private void drawIconsOverCanvas(RenderingContext rc, Canvas cv) {
		int skewConstant = (int) getDensityValue(rc, 16);
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
						int mask = wholeType & 3;
						int layer = 0;
						if (mask != 1) {
							layer = MapRenderingTypes.getNegativeWayLayer(wholeType);
						}

						TagValuePair pair = o.getMapIndex().decodeType(MapRenderingTypes.getMainObjectType(wholeType),
								MapRenderingTypes.getObjectSubType(wholeType));
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
	private final static boolean findAllTextIntersections = true;

	private float getDensityValue(RenderingContext rc, float val) {
		if (rc.highResMode && rc.density > 1) {
			return val * rc.density * rc.mapTextSize;
		} else {
			return val * rc.mapTextSize;
		}
	}

	public void drawTextOverCanvas(RenderingContext rc, Canvas cv, boolean useEnglishNames) {
		List<RectF> boundsNotPathIntersect = new ArrayList<RectF>();
		List<RectF> boundsPathIntersect = new ArrayList<RectF>();
		int size = rc.textToDraw.size();
		Comparator<RectF> c = new Comparator<RectF>(){
			@Override
			public int compare(RectF object1, RectF object2) {
				return Float.compare(object1.left, object2.left);
			}

		};
		
		// 1. Sort text using text order 
		Collections.sort(rc.textToDraw, new Comparator<TextDrawInfo>() {
			@Override
			public int compare(TextDrawInfo object1, TextDrawInfo object2) {
				return object1.textOrder - object2.textOrder;
			}
		});
		
		nextText: for (int i = 0; i < size; i++) {
			TextDrawInfo text  = rc.textToDraw.get(i);
			if(text.text != null){
				int d = text.text.indexOf(MapRenderingTypes.DELIM_CHAR);
				// not used now functionality 
				// possibly it will be used specifying english names after that character
				if(d > 0){
					text.text = text.text.substring(0, d);
				}
				if(useEnglishNames){
					text.text = Junidecode.unidecode(text.text);
				}


				// sest text size before finding intersection (it is used there)
				float textSize = getDensityValue(rc, text.textSize);
				paintText.setTextSize(textSize);
				paintText.setFakeBoldText(text.bold);
				paintText.setColor(text.textColor);
				// align center y
				text.centerY += (-paintText.ascent());

				// calculate if there is intersection
				boolean intersects = findTextIntersection(rc, boundsNotPathIntersect, boundsPathIntersect, c, text);
				if(intersects){
					continue nextText;
				}


				if(text.drawOnPath != null){
					if(text.textShadow > 0){
						paintText.setColor(Color.WHITE);
						paintText.setStyle(Style.STROKE);
						paintText.setStrokeWidth(2 + text.textShadow);
						cv.drawTextOnPath(text.text, text.drawOnPath, 0, text.vOffset, paintText);
						// reset
						paintText.setStyle(Style.FILL);
						paintText.setStrokeWidth(2);
						paintText.setColor(text.textColor);
					}
					cv.drawTextOnPath(text.text, text.drawOnPath, 0, text.vOffset, paintText);
				} else {
					if (text.shieldRes != null) {
						Bitmap ico = RenderingIcons.getIcon(context, text.shieldRes);
						if (ico != null) {
							cv.drawBitmap(ico, text.centerX - ico.getWidth() / 2 - 0.5f, text.centerY
									- ico.getHeight() / 2 - getDensityValue(rc, 4.5f) 
									, paintIcon);
						}
					}

					drawWrappedText(cv, text, textSize);
				}
			}
		}
	}

	private void drawWrappedText(Canvas cv, TextDrawInfo text, float textSize) {
		if(text.textWrap == 0){
			// set maximum for all text
			text.textWrap = 40;
		}

		if(text.text.length() > text.textWrap){
			int start = 0;
			int end = text.text.length();
			int lastSpace = -1;
			int line = 0;
			int pos = 0;
			int limit = 0;
			while(pos < end){
				lastSpace = -1;
				limit += text.textWrap;
				while(pos < limit && pos < end){
					if(!Character.isLetterOrDigit(text.text.charAt(pos))){
						lastSpace = pos;
					}
					pos++;
				}
				if(lastSpace == -1){
					drawTextOnCanvas(cv, text.text.substring(start, pos), 
							text.centerX, text.centerY + line * (textSize + 2), paintText, text.textShadow);
					start = pos;
				} else {
					drawTextOnCanvas(cv, text.text.substring(start, lastSpace), 
							text.centerX, text.centerY + line * (textSize + 2), paintText, text.textShadow); 
					start = lastSpace + 1;
					limit += (start - pos) - 1;
				}
				line++;

			}
		} else {
			drawTextOnCanvas(cv, text.text, text.centerX, text.centerY, paintText, text.textShadow);
		}
	}

	private void drawTextOnCanvas(Canvas cv, String text, float centerX, float centerY, Paint paint, float textShadow){
		if(textShadow > 0){
			int c = paintText.getColor();
			paintText.setStyle(Style.STROKE);
			paintText.setColor(Color.WHITE);
			paintText.setStrokeWidth(2 + textShadow);
			cv.drawText(text, centerX, centerY, paint);
			// reset
			paintText.setStrokeWidth(2);
			paintText.setStyle(Style.FILL);
			paintText.setColor(c);
		}
		cv.drawText(text, centerX, centerY, paint);
	}


	private boolean findTextIntersection(RenderingContext rc, List<RectF> boundsNotPathIntersect, List<RectF> boundsPathIntersect,
			Comparator<RectF> c, TextDrawInfo text) {
		boolean horizontalWayDisplay = (text.pathRotate > 45 && text.pathRotate < 135) || (text.pathRotate > 225 && text.pathRotate < 315);
//		text.minDistance = 0;
		float textWidth = paintText.measureText(text.text) + (!horizontalWayDisplay ? 0 : text.minDistance);
		// Paint.ascent is negative, so negate it.
		int ascent = (int) Math.ceil(-paintText.ascent());
		int descent = (int) Math.ceil(paintText.descent());
		float textHeight = ascent + descent + (horizontalWayDisplay ? 0 : text.minDistance) + getDensityValue(rc, 5);

		RectF bounds = new RectF();
		if(text.drawOnPath == null || horizontalWayDisplay){
			bounds.set(text.centerX - textWidth / 2, text.centerY - textHeight / 2 ,
					text.centerX + textWidth / 2 , text.centerY + textHeight / 2 );
		} else {
			bounds.set(text.centerX - textHeight / 2, text.centerY - textWidth / 2, 
					text.centerX + textHeight / 2 , text.centerY + textWidth / 2);
		}
		List<RectF> boundsIntersect = text.drawOnPath == null || findAllTextIntersections? 
				boundsNotPathIntersect : boundsPathIntersect;
		if(boundsIntersect.isEmpty()){
			boundsIntersect.add(bounds);
		} else {
			final int diff = (int) (getDensityValue(rc, 3));
			final int diff2 = (int) (getDensityValue(rc, 15));
			// implement binary search 
			int index = Collections.binarySearch(boundsIntersect, bounds, c);
			if (index < 0) {
				index = -(index + 1);
			}
			// find sublist that is appropriate
			int e = index;
			while (e < boundsIntersect.size()) {
				if (boundsIntersect.get(e).left < bounds.right ) {
					e++;
				} else {
					break;
				}
			}
			int st = index - 1;
			while (st >= 0) {
				// that's not exact algorithm that replace comparison rect with each other
				// because of that comparison that is not obvious 
				// (we store array sorted by left boundary, not by right) - that's euristic
				if (boundsIntersect.get(st).right > bounds.left ) {
					st--;
				} else {
					break;
				}
			}
			if (st < 0) {
				st = 0;
			}
			// test functionality
			//					 cv.drawRect(bounds, paint);
			//					 cv.drawText(text.text.substring(0, Math.min(5, text.text.length())), bounds.centerX(), bounds.centerY(), paint);

			for (int j = st; j < e; j++) {
				RectF b = boundsIntersect.get(j);
				float x = Math.min(bounds.right, b.right) - Math.max(b.left, bounds.left);
				float y = Math.min(bounds.bottom, b.bottom) - Math.max(b.top, bounds.top);
				if ((x > diff && y > diff2) || (x > diff2 && y > diff)) {
					return true;
				}
			}
			// store in list sorted by left boundary
			//					if(text.minDistance > 0){
			//						if (verticalText) {
			//							bounds.set(bounds.left + text.minDistance / 2, bounds.top, 
			//									bounds.right - text.minDistance / 2, bounds.bottom);
			//						} else {
			//							bounds.set(bounds.left, bounds.top + text.minDistance / 2, bounds.right, 
			//									bounds.bottom - text.minDistance / 2);
			//						}
			//					}
			boundsIntersect.add(index, bounds);
		}
		return false;
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
			int t = mainType & 3;
			int type = MapRenderingTypes.getMainObjectType(mainType);
			int subtype = MapRenderingTypes.getObjectSubType(mainType);
			TagValuePair pair = obj.getMapIndex().decodeType(type, subtype);
			if (t == MapRenderingTypes.POINT_TYPE && !drawOnlyShadow) {
				drawPoint(obj, render, canvas, rc, pair, renderText);
			} else if (t == MapRenderingTypes.POLYLINE_TYPE) {
				int layer = MapRenderingTypes.getNegativeWayLayer(mainType);
				drawPolyline(obj, render, canvas, rc, pair, layer, drawOnlyShadow);
			} else if (t == MapRenderingTypes.POLYGON_TYPE && !drawOnlyShadow) {
				drawPolygon(obj, render, canvas, rc, pair);
			} else {
				if (t == MapRenderingTypes.MULTY_POLYGON_TYPE && !(obj instanceof MultyPolygon)) {
					// log this situation
					return;
				}
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
				String name = ((MultyPolygon) obj).getName(i);
				if (name != null) {
					drawPointText(render, rc, new TagValuePair(tag, value), xText / cnt, yText / cnt, name);
				}
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
			String name = obj.getName();
			if(name != null){
				drawPointText(render, rc, pair, xText / len, yText / len, name);
			}
		}
	}
	
	private boolean updatePaint(RenderingRuleSearchRequest req, Paint p, int ind, boolean area, RenderingContext rc){
		RenderingRuleProperty rColor;
		RenderingRuleProperty rStrokeW;
		RenderingRuleProperty rCap;
		RenderingRuleProperty rPathEff;
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
				p.setColor(Color.BLACK);
				p.setShader(getShader(resId));
			} else {
				p.setShader(null);
			}
			
			// do not check shadow color here
			if(rc.shadowRenderingMode != 1) {
				paint.clearShadowLayer();
			} else {
				int shadowColor = req.getIntPropertyValue(req.ALL.R_SHADOW_COLOR);
				int shadowLayer = req.getIntPropertyValue(req.ALL.R_SHADOW_RADIUS);
				if (shadowColor == 0) {
					shadowLayer = 0;
				}
				p.setShadowLayer(shadowLayer, 0, 0, shadowColor);
			}
		} else {
			p.setShader(null);
			p.clearShadowLayer();
		}
		return true;
		
	}
	

	private void drawPointText(RenderingRuleSearchRequest render, RenderingContext rc, TagValuePair pair, float xText, float yText, String name) {
		String ref = null;
		if (name.charAt(0) == MapRenderingTypes.REF_CHAR) {
			ref = name.substring(1);
			name = ""; //$NON-NLS-1$
			for (int k = 0; k < ref.length(); k++) {
				if (ref.charAt(k) == MapRenderingTypes.REF_CHAR) {
					if (k < ref.length() - 1) {
						name = ref.substring(k + 1);
					}
					ref = ref.substring(0, k);
					break;
				}
			}
		}

		if (ref != null && ref.trim().length() > 0) {
			render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom);
			render.setIntFilter(render.ALL.R_TEXT_LENGTH, ref.length());
			render.setBooleanFilter(render.ALL.R_REF, true);
			if(render.search(RenderingRulesStorage.TEXT_RULES)){
				if(render.getIntPropertyValue(render.ALL.R_TEXT_SIZE) > 0){
					TextDrawInfo text = new TextDrawInfo(ref);
					text.fillProperties(render, xText, yText);
					rc.textToDraw.add(text);
				}
			}
		}
		
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom);
		render.setIntFilter(render.ALL.R_TEXT_LENGTH, name.length());
		render.setBooleanFilter(render.ALL.R_REF, false);
		if(render.search(RenderingRulesStorage.TEXT_RULES) ){
			if(render.getIntPropertyValue(render.ALL.R_TEXT_SIZE) > 0){
				TextDrawInfo info = new TextDrawInfo(name);
				info.fillProperties(render, xText, yText);
				rc.textToDraw.add(info);
			}
		}
	}
	
	private void drawPoint(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Canvas canvas, RenderingContext rc, TagValuePair pair, boolean renderText) {
		if(render == null || pair == null){
			return;
		}
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom);
		render.search(RenderingRulesStorage.POINT_RULES);
		
		String resId = render.getStringPropertyValue(render.ALL.R_ICON);
		String name = null;
		if (renderText) {
			name = obj.getName();
		}
		if(resId == null && name == null){
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
		if (name != null) {
			drawPointText(render, rc, pair, ps.x, ps.y, name);
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
			 paint.setColor(0xffbababa);
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
		if(rc.zoom >= 16 && "highway".equals(pair.tag) && MapRenderingTypes.isOneWayWay(obj.getHighwayAttributes())){ //$NON-NLS-1$
			oneway = true;
		}

		rc.visible++;

		Path path = null;
		float pathRotate = 0;
		float roadLength = 0;
		boolean inverse = false;
		float xPrev = 0;
		float yPrev = 0;
		float xMid = 0;
		float yMid = 0;
		PointF middlePoint = new PointF();
		int middle = obj.getPointsLength() / 2;

		for (int i = 0; i < length ; i++) {
			PointF p = calcPoint(obj, i, rc);
			if(i == 0 || i == length -1){
				xMid += p.x;
				yMid += p.y;
			}
			if (path == null) {
				path = new Path();
				path.moveTo(p.x, p.y);
			} else {
				roadLength += Math.sqrt((p.x - xPrev) * (p.x - xPrev) + (p.y - yPrev) * (p.y - yPrev)); 
				if(i == middle){
					middlePoint.set(p.x, p.y);
					double rot = - Math.atan2(p.x - xPrev, p.y - yPrev) * 180 / Math.PI;
					if (rot < 0) {
						rot += 360;
					}
					if (rot < 180) {
						rot += 180;
						inverse = true;
					}
					pathRotate = (float) rot;
				}
				path.lineTo(p.x, p.y);
			}
			xPrev = p.x;
			yPrev = p.y;
		}
		if (path != null) {
			if(drawOnlyShadow) {
				int shadowColor = render.getIntPropertyValue(render.ALL.R_SHADOW_COLOR);
				int shadowRadius = render.getIntPropertyValue(render.ALL.R_SHADOW_RADIUS);
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
			if (!drawOnlyShadow && obj.getName() != null && obj.getName().length() > 0) {
				calculatePolylineText(obj, render, rc, pair, path, pathRotate, roadLength, inverse, xMid, yMid, middlePoint);
			}
		}
	}

	private void calculatePolylineText(BinaryMapDataObject obj, RenderingRuleSearchRequest render, RenderingContext rc, TagValuePair pair,
			Path path, float pathRotate, float roadLength, boolean inverse, float xMid, float yMid, PointF middlePoint) {
		String name = obj.getName();
		String ref = null;
		if(name.charAt(0) == MapRenderingTypes.REF_CHAR){
			ref = name.substring(1);
			name = ""; //$NON-NLS-1$
			for(int k = 0; k < ref.length(); k++){
				if(ref.charAt(k) == MapRenderingTypes.REF_CHAR){
					if(k < ref.length() - 1){
						name = ref.substring(k + 1);
					}
					ref = ref.substring(0, k);
					break;
				}
			}
		}
		if(ref != null && ref.trim().length() > 0){
			render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom);
			render.setIntFilter(render.ALL.R_TEXT_LENGTH, ref.length());
			render.setBooleanFilter(render.ALL.R_REF, true);
			if(render.search(RenderingRulesStorage.TEXT_RULES)){
				if(render.getIntPropertyValue(render.ALL.R_TEXT_SIZE) > 0){
					TextDrawInfo text = new TextDrawInfo(ref);
					text.fillProperties(render, middlePoint.x, middlePoint.y);
					text.pathRotate = pathRotate;
					rc.textToDraw.add(text);
				}
			}
		}

		if(name != null && name.trim().length() > 0){
			render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom);
			render.setIntFilter(render.ALL.R_TEXT_LENGTH, name.length());
			render.setBooleanFilter(render.ALL.R_REF, false);
			if (render.search(RenderingRulesStorage.TEXT_RULES) && render.getIntPropertyValue(render.ALL.R_TEXT_SIZE) > 0) {
				TextDrawInfo text = new TextDrawInfo(name);
				if (render.getIntPropertyValue(render.ALL.R_TEXT_ON_PATH, 0) == 0) {
					text.fillProperties(render, middlePoint.x, middlePoint.y);
					rc.textToDraw.add(text);
				} else {
					paintText.setTextSize(render.getIntPropertyValue(render.ALL.R_TEXT_SIZE));
					if (paintText.measureText(obj.getName()) < roadLength ) {
						if (inverse) {
							path.rewind();
							boolean st = true;
							for (int i = obj.getPointsLength() - 1; i >= 0; i--) {
								PointF p = calcPoint(obj, i, rc);
								if (st) {
									st = false;
									path.moveTo(p.x, p.y);
								} else {
									path.lineTo(p.x, p.y);
								}
							}
						}
						text.fillProperties(render, xMid / 2, yMid / 2);
						text.pathRotate = pathRotate;
						text.drawOnPath = path;
						int strokeWidth = render.getIntPropertyValue(render.ALL.R_TEXT_SIZE);
						text.vOffset = strokeWidth / 2 - 1;
						rc.textToDraw.add(text);
					}
				}
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
