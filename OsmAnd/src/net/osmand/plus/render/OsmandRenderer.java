package net.osmand.plus.render;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TFloatObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MultyPolygon;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
	private Map<Integer, Shader> shaders = new LinkedHashMap<Integer, Shader>();
	private Map<Integer, Bitmap> cachedIcons = new LinkedHashMap<Integer, Bitmap>();

	private final Context context;

	private DisplayMetrics dm;

	private int[] shadowarray;
	private int shadownum;

	private static class TextDrawInfo {

		public TextDrawInfo(String text){
			this.text = text;
		}

		public void fillProperties(RenderingContext rc, float centerX, float centerY){
			this.centerX = centerX + rc.textDx;
			this.centerY = centerY + rc.textDy;
			textColor = rc.textColor;
			textSize = rc.textSize;
			textShadow = (int) rc.textHaloRadius;
			textWrap = rc.textWrapWidth;
			bold = rc.textBold;
			minDistance = rc.textMinDistance;
			shieldRes = rc.textShield;
			if(rc.textOrder >= 0){
				textOrder = rc.textOrder;
			}
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
		int shieldRes = 0;
		int textOrder = 20;
		
	}

	private static class IconDrawInfo {
		float x = 0;
		float y = 0;
		int resId;
	}

	/*package*/ static class RenderingContext {
		public boolean interrupted = false;
		public boolean nightMode = false;
		public boolean highResMode = false;
		public float mapTextSize = 1;

		List<TextDrawInfo> textToDraw = new ArrayList<TextDrawInfo>();
		List<IconDrawInfo> iconsToDraw = new ArrayList<IconDrawInfo>();

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

		// use to calculate points
		PointF tempPoint = new PointF();
		float cosRotateTileSize;
		float sinRotateTileSize;

		// These properties are used for rendering one object  
		// polyline props
		boolean showTextOnPath = false;
		String showAnotherText = null;
		float textSize = 0;
		int textColor = 0;
		int textMinDistance = 0;
		int textWrapWidth = 0;
		float textDx = 0;
		float textDy = 0;
		float textHaloRadius = 0;
		boolean textBold;
		int textShield = 0;
		int textOrder = -1;
		
		String renderingDebugInfo;

		RenderingPaintProperties main = new RenderingPaintProperties();
		RenderingPaintProperties second = new RenderingPaintProperties();
		RenderingPaintProperties third = new RenderingPaintProperties();
		RenderingPaintProperties[] adds = null;



		public void clearText() {
			showAnotherText = null;
			showTextOnPath = false;
			textSize = 0;
			textColor = 0;
			textOrder = -1;
			textMinDistance = 0;
			textWrapWidth = 0;
			textDx = 0;
			textDy = 0;
			textHaloRadius = 0;
			textBold = false;
			textShield = 0;
		}


	}

	/* package*/ static class RenderingPaintProperties {
		int color;
		float strokeWidth;
		int shadowRadius;
		int shadowColor;
		boolean fillArea;
		PathEffect pathEffect; 
		Shader shader;
		Cap cap;

		public void emptyLine(){
			color = 0;
			strokeWidth = 0;
			cap = Cap.BUTT;
			pathEffect = null;
			fillArea = false;
			shader = null;
			shadowColor = 0;
			shadowRadius = 0;
		}

		public void updatePaint(Paint p){
			p.setStyle(fillArea ? Style.FILL_AND_STROKE : Style.STROKE);
			p.setColor(color);
			p.setShader(shader);
			if(shadowColor == 0){
				shadowRadius = 0;
			}
			p.setShadowLayer(shadowRadius, 0, 0, shadowColor);
			p.setStrokeWidth(strokeWidth);
			p.setStrokeCap(cap);
			if (!fillArea) {
				p.setPathEffect(pathEffect);
			}
		}

		public void emptyArea(){
			color = 0;
			strokeWidth = 0;
			cap = Cap.BUTT;
			fillArea = false;
			shader = null;
			pathEffect = null;
			shadowColor = 0;
			shadowRadius = 0;
		}
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

	public Shader getShader(int resId){
		if(shaders.get(resId) == null){
			Shader sh = new BitmapShader(
					BitmapFactory.decodeResource(context.getResources(), resId), TileMode.REPEAT, TileMode.REPEAT);
			shaders.put(resId, sh);
		}	
		return shaders.get(resId);
	}

	private void put(TFloatObjectHashMap<TIntArrayList> map, Float k, int v, int init){
		if(!map.containsKey(k)){
			map.put(k, new TIntArrayList());
		}
		map.get(k).add(v);
	}


	public Bitmap generateNewBitmap(RenderingContext rc, List<BinaryMapDataObject> objects, Bitmap bmp, boolean useEnglishNames,
			BaseOsmandRender renderer, List<IMapDownloaderCallback> notifyList) {
		long now = System.currentTimeMillis();

		// fill area
		Canvas cv = new Canvas(bmp);
		if(renderer != null){
			int dc = renderer.getDefaultColor(rc.nightMode);
			if(dc != 0){
				paintFillEmpty.setColor(dc);
			}
		}
		cv.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), paintFillEmpty);

		// put in order map
		int sz = objects.size();
		int init = sz / 4;
		TFloatObjectHashMap<TIntArrayList> orderMap = new TFloatObjectHashMap<TIntArrayList>();
		if (renderer != null) {
			for (int i = 0; i < sz; i++) {
				BinaryMapDataObject o = objects.get(i);
				int sh = i << 8;

				if (o instanceof MultyPolygon) {
					int mask = MapRenderingTypes.MULTY_POLYGON_TYPE;
					int layer = ((MultyPolygon) o).getLayer();
					put(orderMap, renderer.getObjectOrder(((MultyPolygon) o).getTag(), ((MultyPolygon) o).getValue(), 
							mask, layer), sh, init);
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
							put(orderMap, renderer.getObjectOrder(pair.tag, pair.value, mask, layer), sh + j, init);
						}

					}
				}

				if (rc.interrupted) {
					return null;
				}
			}
		}

		if (objects != null && !objects.isEmpty() && rc.width > 0 && rc.height > 0) {
			// init rendering context
			rc.tileDivisor = (int) (1 << (31 - rc.zoom));
			rc.cosRotateTileSize = FloatMath.cos((float) Math.toRadians(rc.rotate)) * TILE_SIZE;
			rc.sinRotateTileSize = FloatMath.sin((float) Math.toRadians(rc.rotate)) * TILE_SIZE;

			//int shadow = 0; // no shadow (minumum CPU)
			//int shadow = 1; // classic shadow (the implementaton in master)
			//int shadow = 2; // blur shadow (most CPU, but still reasonable)
			int shadow = 3; // solid border (CPU use like classic version or even smaller)
			boolean repeat = false;

			float[] keys = orderMap.keys();
			Arrays.sort(keys);
			int objCount = 0;

			shadowarray = new int[keys.length];
			shadownum = 0;

			for (int k = 0; k < keys.length; k++) {

				if(repeat == true && shadowarray[shadownum] != k && shadowarray[shadownum] != -1  && keys[k] < 58){
					continue;
				}
				
				if(repeat == true && shadowarray[shadownum] == k){
					shadownum++;
				}

				TIntArrayList list = orderMap.get(keys[k]);

				for (int j = 0; j < list.size(); j++) {
					int i = list.get(j);
					int ind = i >> 8;
				int l = i & 0xff;
				BinaryMapDataObject obj = objects.get(ind);

				// show text only for main type
				drawObj(obj, renderer, cv, rc, l, l == 0, shadow, k);

				objCount++;
				}
				if(objCount > 25){
					notifyListeners(notifyList);
					objCount = 0;
				}
				if(rc.interrupted){
					return null;
				}

				// order = 57 should be set as limit for shadows 
				if(keys[k] > 57 && repeat == false && shadow > 1){
					shadow = 0;
					shadownum = 0;
					k = shadowarray[0]-1;
					repeat = true;
				}
			}
			notifyListeners(notifyList);
			long beforeIconTextTime = System.currentTimeMillis() - now;

			int skewConstant = (int) getDensityValue(rc, 16);

			int iconsW = rc.width / skewConstant ;
			int iconsH = rc.height / skewConstant;
			int[] alreadyDrawnIcons = new int[iconsW * iconsH / 32];
			for(IconDrawInfo icon : rc.iconsToDraw){
				if (icon.resId != 0) {
					if (cachedIcons.get(icon.resId) == null) {
						cachedIcons.put(icon.resId, UnscaledBitmapLoader.loadFromResource(context.getResources(), icon.resId, null, dm));
					}
					Bitmap ico = cachedIcons.get(icon.resId);
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
				if(rc.interrupted){
					return null;
				}
			}
			notifyListeners(notifyList);
			
			drawTextOverCanvas(rc, cv, useEnglishNames);
			long time = System.currentTimeMillis() - now;
			rc.renderingDebugInfo = String.format("Rendering done in %s (%s text) ms\n" +
					"(%s points, %s points inside, %s objects visile from %s)",//$NON-NLS-1$
					time, time - beforeIconTextTime,rc.pointCount, rc.pointInsideCount, rc.visible, rc.allObjects);
			log.info(rc.renderingDebugInfo);

		}

		return bmp;
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
		if (rc.highResMode && dm.density > 1) {
			return val * dm.density * rc.mapTextSize;
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
					if (text.shieldRes != 0) {
						if (cachedIcons.get(text.shieldRes) == null) {
							cachedIcons.put(text.shieldRes, BitmapFactory.decodeResource(context.getResources(), text.shieldRes));
						}
						Bitmap ico = cachedIcons.get(text.shieldRes);
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


	protected void drawObj(BinaryMapDataObject obj, BaseOsmandRender render, Canvas canvas, RenderingContext rc, int l, boolean renderText
			, int shadow, int index) {
		rc.allObjects++;
		if (obj instanceof MultyPolygon) {
			drawMultiPolygon(obj, render,canvas, rc);
		} else {
			int mainType = obj.getTypes()[l];
			int t = mainType & 3;
			int type = MapRenderingTypes.getMainObjectType(mainType);
			int subtype = MapRenderingTypes.getObjectSubType(mainType);
			TagValuePair pair = obj.getMapIndex().decodeType(type, subtype);
			if (t == MapRenderingTypes.POINT_TYPE) {
				drawPoint(obj, render, canvas, rc, pair, renderText);
			} else if (t == MapRenderingTypes.POLYLINE_TYPE) {
				int layer = MapRenderingTypes.getNegativeWayLayer(mainType);
				drawPolyline(obj, render, canvas, rc, pair, layer, shadow, index);
			} else if (t == MapRenderingTypes.POLYGON_TYPE) {
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
		cachedIcons.clear();
		shaders.clear();
	}

	private void drawMultiPolygon(BinaryMapDataObject obj, BaseOsmandRender render, Canvas canvas, RenderingContext rc) {
		String tag = ((MultyPolygon)obj).getTag();
		String value = ((MultyPolygon)obj).getValue();
		if(render == null || tag == null){
			return;
		}
		rc.main.emptyArea();
		rc.second.emptyLine();
		rc.main.color = Color.rgb(245, 245, 245);

		boolean rendered = render.renderPolygon(tag, value, rc.zoom, rc, this, rc.nightMode);
		if(!rendered){
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
		rc.main.updatePaint(paint);
		canvas.drawPath(path, paint);
		// for test purpose
		//	      rc.second.strokeWidth = 1.5f;
		//	      rc.second.color = Color.BLACK;

		if (rc.second.strokeWidth != 0) {
			rc.second.updatePaint(paint);
			canvas.drawPath(path, paint);
		}
	}


	private void drawPolygon(BinaryMapDataObject obj, BaseOsmandRender render, Canvas canvas, RenderingContext rc, TagValuePair pair) {

		if(render == null || pair == null){
			return;
		}
		float xText = 0;
		float yText = 0;
		int zoom = rc.zoom;
		Path path = null;
		rc.main.emptyArea();
		rc.second.emptyLine();
		// rc.main.color = Color.rgb(245, 245, 245);

		boolean rendered = render.renderPolygon(pair.tag, pair.value, zoom, rc, this, rc.nightMode);
		if(!rendered){
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

			rc.main.updatePaint(paint);
			canvas.drawPath(path, paint);
			if (rc.second.strokeWidth != 0) {
				rc.second.updatePaint(paint);
				canvas.drawPath(path, paint);
			}
			String name = obj.getName();
			if(name != null){
				drawPointText(render, rc, pair, xText / len, yText / len, name);
			}
		}
		return;
	}

	private void drawPointText(BaseOsmandRender render, RenderingContext rc, TagValuePair pair, float xText, float yText, String name) {
		rc.clearText();
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
			rc.clearText();
			ref = render.renderObjectText(ref, pair.tag, pair.value, rc, true, rc.nightMode);
			TextDrawInfo text = new TextDrawInfo(ref);
			text.fillProperties(rc, xText, yText);
			rc.textToDraw.add(text);

		}
		name = render.renderObjectText(name, pair.tag, pair.value, rc, false, rc.nightMode);
		if (rc.textSize > 0 && name != null) {
			TextDrawInfo info = new TextDrawInfo(name);
			info.fillProperties(rc, xText, yText);
			rc.textToDraw.add(info);
		}
	}


	private void drawPoint(BinaryMapDataObject obj, BaseOsmandRender render, Canvas canvas, RenderingContext rc, TagValuePair pair, boolean renderText) {
		if(render == null || pair == null){
			return;
		}

		Integer resId = render.getPointIcon(pair.tag, pair.value, rc.zoom, rc.nightMode);
		String name = null;
		if (renderText) {
			name = obj.getName();
		}
		if((resId == null || resId == 0) && name == null){
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

		if(resId != null && resId != 0){
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

	private void drawPolylineWithShadow(Canvas canvas, Path path, int shadow, int shadowRadius, int index){
		//if(paint.getStrokeCap() == Paint.Cap.ROUND)paint.setStrokeCap(Paint.Cap.SQUARE);
		//if(paint.getStrokeCap() == Paint.Cap.ROUND)paint.setStrokeCap(Paint.Cap.BUTT);

		if(paint.getPathEffect() != null){
			paint.setStrokeCap(Paint.Cap.BUTT);			
		}

		// no shadow
		if(shadow == 0){
			paint.setShadowLayer(0, 0, 0, 0);
			canvas.drawPath(path, paint);
		}

		//classic ugly shadows
		if(shadow == 1){
			canvas.drawPath(path, paint);
		}

		// blurred shadows
		if(shadow == 2){
			if(paint.getPathEffect() == null) paint.setColor(0xffffffff);
			canvas.drawPath(path, paint);
		}

		// option shadow = 3 with solid border
		if(shadow == 3 && shadowRadius > 0){
			paint.setShadowLayer(0, 0, 0, 0);
			paint.setStrokeWidth(paint.getStrokeWidth() + 2);
			if(paint.getPathEffect() == null) paint.setColor(0xffbababa);
			canvas.drawPath(path, paint);
		}
		else if(shadow == 3) canvas.drawPath(path, paint);

		//check for shadow and save index in array
		if(shadowRadius > 0 && shadow > 1){
			if(shadownum == 0){
				shadowarray[shadownum] = index;
				shadownum++;
			}
			if (shadowarray[shadownum-1] != index){
				shadowarray[shadownum] = index;
				shadownum++;
			}
		}
	}

	private void drawPolyline(BinaryMapDataObject obj, BaseOsmandRender render, Canvas canvas, RenderingContext rc, TagValuePair pair, int layer,
			int shadow, int index) {
		if(render == null || pair == null){
			return;
		}
		int length = obj.getPointsLength();
		if(length < 2){
			return;
		}
		rc.main.emptyLine();
		rc.second.emptyLine();
		rc.third.emptyLine();
		rc.adds = null;
		boolean res = render.renderPolyline(pair.tag, pair.value, rc.zoom, rc, this, layer, rc.nightMode);
		if(rc.main.strokeWidth == 0 || !res){
			return;
		}
		if(rc.zoom >= 16 && "highway".equals(pair.tag) && MapRenderingTypes.isOneWayWay(obj.getHighwayAttributes())){ //$NON-NLS-1$
			rc.adds = getOneWayProperties();
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
			rc.main.updatePaint(paint);
			drawPolylineWithShadow(canvas, path, shadow, rc.main.shadowRadius, index);
			if (rc.second.strokeWidth != 0) {
				rc.second.updatePaint(paint);
				drawPolylineWithShadow(canvas, path, shadow, rc.second.shadowRadius, index);
				if (rc.third.strokeWidth != 0) {
					rc.third.updatePaint(paint);
					drawPolylineWithShadow(canvas, path, shadow, rc.third.shadowRadius, index);
				}
			}
			if (rc.adds != null) {
				for (int i = 0; i < rc.adds.length; i++) {
					rc.adds[i].updatePaint(paint);
					drawPolylineWithShadow(canvas, path, shadow, rc.adds[i].shadowRadius, index);
				}
			}
			if (obj.getName() != null && obj.getName().length() > 0) {
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
					rc.clearText();
					ref = render.renderObjectText(ref, pair.tag, pair.value, rc, true, rc.nightMode);
					TextDrawInfo text = new TextDrawInfo(ref);
					text.fillProperties(rc, middlePoint.x, middlePoint.y);
					text.pathRotate = pathRotate;
					rc.textToDraw.add(text);

				}

				if(name != null && name.trim().length() > 0){
					rc.clearText();
					name = render.renderObjectText(name, pair.tag, pair.value, rc, false, rc.nightMode);
					if (rc.textSize > 0) {
						TextDrawInfo text = new TextDrawInfo(name);
						if (!rc.showTextOnPath) {
							text.fillProperties(rc, middlePoint.x, middlePoint.y);
							rc.textToDraw.add(text);
						} else {
							paintText.setTextSize(text.textSize);
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
								text.fillProperties(rc, xMid / 2, yMid / 2);
								text.pathRotate = pathRotate;
								text.drawOnPath = path;
								text.vOffset = rc.main.strokeWidth / 2 - 1;
								rc.textToDraw.add(text);
							}
						}
					}

				}
			}
		}
	}
	
	private static RenderingPaintProperties[] oneWay = null;
	public static RenderingPaintProperties[] getOneWayProperties(){
		if(oneWay == null){
			PathEffect arrowDashEffect1 = new DashPathEffect(new float[] { 0, 12, 10, 152 }, 0);
			PathEffect arrowDashEffect2 = new DashPathEffect(new float[] { 0, 12, 9, 153 }, 1);
			PathEffect arrowDashEffect3 = new DashPathEffect(new float[] { 0, 18, 2, 154 }, 1);
			PathEffect arrowDashEffect4 = new DashPathEffect(new float[] { 0, 18, 1, 155 }, 1);
			oneWay = new RenderingPaintProperties[4];
			oneWay[0] = new RenderingPaintProperties();
			oneWay[0].emptyLine();
			oneWay[0].color = 0xff6c70d5;
			oneWay[0].strokeWidth = 1;
			oneWay[0].pathEffect = arrowDashEffect1;

			oneWay[1] = new RenderingPaintProperties();
			oneWay[1].emptyLine();
			oneWay[1].color = 0xff6c70d5;
			oneWay[1].strokeWidth = 2;
			oneWay[1].pathEffect = arrowDashEffect2;

			oneWay[2] = new RenderingPaintProperties();
			oneWay[2].emptyLine();
			oneWay[2].color = 0xff6c70d5;
			oneWay[2].strokeWidth = 3;
			oneWay[2].pathEffect = arrowDashEffect3;

			oneWay[3] = new RenderingPaintProperties();
			oneWay[3].emptyLine();
			oneWay[3].color = 0xff6c70d5;
			oneWay[3].strokeWidth = 4;
			oneWay[3].pathEffect = arrowDashEffect4;

		}
		return oneWay;
	}


}
