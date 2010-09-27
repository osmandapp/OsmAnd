package net.osmand.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.LogUtil;
import net.osmand.R;
import net.osmand.osm.MapRenderObject;
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
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.text.TextPaint;
import android.util.FloatMath;

public class OsmandRenderer {
	private static final Log log = LogUtil.getLog(OsmandRenderer.class);
	
	private TextPaint paintText;
	private Paint paint;
	
	private Paint paintFillEmpty;
	private Paint paintIcon;
	
	public static final int TILE_SIZE = 256; 
	
	/// Colors
	private int clFillScreen = Color.rgb(241, 238, 232);

	private Map<String, PathEffect> dashEffect = new LinkedHashMap<String, PathEffect>();
	private Map<Integer, Shader> shaders = new LinkedHashMap<Integer, Shader>();
	private Map<Integer, Bitmap> cachedIcons = new LinkedHashMap<Integer, Bitmap>();

	private final Context context;

	
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
	}
	
	private static class IconDrawInfo {
		float x = 0;
		float y = 0;
		int resId;
	}
	
	/*package*/ static class RenderingContext {
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
		
		
		RenderingPaintProperties main = new RenderingPaintProperties();
		RenderingPaintProperties second = new RenderingPaintProperties();
		RenderingPaintProperties third = new RenderingPaintProperties();
		RenderingPaintProperties[] adds = null;
		
		public void clearText() {
			showAnotherText = null;
			showTextOnPath = false;
			textSize = 0;
			textColor = 0;
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
		int shadowLayer;
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
			shadowLayer = 0;
		}
		
		public void updatePaint(Paint p){
			p.setStyle(fillArea ? Style.FILL_AND_STROKE : Style.STROKE);
			p.setColor(color);
			p.setShader(shader);
			p.setShadowLayer(shadowLayer, 0, 0, shadowColor);
			if (!fillArea) {
				p.setStrokeCap(cap);
				p.setPathEffect(pathEffect);
				p.setStrokeWidth(strokeWidth);
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
			shadowLayer = 0;
		}
	}
	
	public OsmandRenderer(Context context){
		this.context = context;
		
		paintIcon = new Paint();
		paintIcon.setStyle(Style.STROKE);
		
		paintText = new TextPaint();
		paintText.setStyle(Style.FILL);
		paintText.setColor(Color.BLACK);
		paintText.setTextAlign(Align.CENTER);
		paintText.setTypeface(Typeface.create("Droid Serif", Typeface.NORMAL)); //$NON-NLS-1$
		paintText.setAntiAlias(true);
		
		paint = new Paint();
		paint.setAntiAlias(true);
		
		paintFillEmpty = new Paint();
		paintFillEmpty.setStyle(Style.FILL);
		paintFillEmpty.setColor(clFillScreen);
		
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
	
	private <K,T>void put(Map<K, List<T>> map, K k, T v, int init){
		if(!map.containsKey(k)){
			map.put(k, new ArrayList<T>(init));
		}
		map.get(k).add(v);
	}
	
	
	public Bitmap generateNewBitmap(int width, int height, float leftTileX, float topTileY, 
			List<MapRenderObject> objects, int zoom, float rotate, boolean useEnglishNames) {
		long now = System.currentTimeMillis();
		// put in order map
		int sz = objects.size();
		int init = sz / 4;
		TreeMap<Float, List<Integer>> orderMap = new TreeMap<Float, List<Integer>>();
		for (int i = 0; i < sz; i++) {
			MapRenderObject o = objects.get(i);
			int mt = o.getMainType();
			int sh = i << 8;
			put(orderMap, MapRenderObject.getOrder(mt), sh + 1, init);
			int s = o.getSecondType();
			if (s != 0) {
				put(orderMap, MapRenderObject.getOrder(s), sh + 2, init);
			}
			byte multiTypes = o.getMultiTypes();
			for (int j = 0; j < multiTypes; j++) {
				put(orderMap, MapRenderObject.getOrder(o.getAdditionalType(j)), sh + (j + 3), init);
			}
		}
		
		Bitmap bmp = null; 
		if (objects != null && !objects.isEmpty() && width > 0 && height > 0) {
			// init rendering context
			RenderingContext rc = new RenderingContext();
			rc.leftX = leftTileX;
			rc.topY = topTileY;
			rc.zoom = zoom;
			rc.rotate = rotate;
			rc.width = width;
			rc.height = height;
			rc.tileDivisor = (int) (1 << (31 - zoom));
			rc.cosRotateTileSize = FloatMath.cos((float) Math.toRadians(rotate)) * TILE_SIZE;
			rc.sinRotateTileSize = FloatMath.sin((float) Math.toRadians(rotate)) * TILE_SIZE;
			bmp = Bitmap.createBitmap(width, height, Config.RGB_565);
			
			Canvas cv = new Canvas(bmp);
			cv.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), paintFillEmpty);
			for (List<Integer> list : orderMap.values()) {
				for (Integer i : list) {
					int ind = i >> 8;
					int l = i & 0xff;
					MapRenderObject obj = objects.get(ind);
					if (l == 1) {
						// show text only for main type
						drawObj(obj, cv, rc, obj.getMainType(), true);
					} else if (l == 2) {
						drawObj(obj, cv, rc, obj.getSecondType(), false);
					} else {
						drawObj(obj, cv, rc, obj.getAdditionalType(l - 3), false);
					}
				}
			}
			for(IconDrawInfo icon : rc.iconsToDraw){
				if(icon.resId != 0){
					if(cachedIcons.get(icon.resId) == null){
						cachedIcons.put(icon.resId, BitmapFactory.decodeResource(context.getResources(), icon.resId));
					}
					Bitmap ico = cachedIcons.get(icon.resId);
					if (ico  != null) {
						cv.drawBitmap(ico, icon.x - ico.getWidth() / 2, icon.y - ico.getHeight() / 2, paintIcon);
					}
				}
			}
			drawTextOverCanvas(rc, cv, useEnglishNames);
			log.info(String.format("Rendering has been done in %s ms. (%s points, %s points inside)", System.currentTimeMillis() - now, //$NON-NLS-1$ 
					rc.pointCount,rc.pointInsideCount)); 
		}
		
		return bmp;
	}
	private final static boolean findAllTextIntersections = true;

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
		next: for (int i = 0; i < size; i++) {
			TextDrawInfo text  = rc.textToDraw.get(i);
			if(text.text != null){
				if(useEnglishNames){
					text.text = Junidecode.unidecode(text.text);
				}
				RectF bounds = new RectF();
				paintText.setTextSize(text.textSize);
				paintText.setFakeBoldText(text.bold);
				float mes = paintText.measureText(text.text);
				if(text.drawOnPath == null || 
						(text.pathRotate > 45 && text.pathRotate < 135) || (text.pathRotate > 225 && text.pathRotate < 315)){
					bounds.set(text.centerX - mes / 2, text.centerY - 3 * text.textSize / 2,
							text.centerX + mes / 2, text.centerY + 3 * text.textSize / 2);
				} else {
					bounds.set(text.centerX - 3 * text.textSize / 2, text.centerY - mes/2, 
										text.centerX + 3 * text.textSize / 2, text.centerY + mes/2);
				}
				if(text.minDistance > 0){
					bounds.set(bounds.left - text.minDistance / 2, bounds.top - text.minDistance / 2
							, bounds.right + text.minDistance / 2, bounds.bottom + text.minDistance / 2);
				}
				List<RectF> boundsIntersect = text.drawOnPath == null || findAllTextIntersections? 
						boundsNotPathIntersect : boundsPathIntersect;
				if(boundsIntersect.isEmpty()){
					boundsIntersect.add(bounds);
				} else {
					final int diff = (int) 3 ;
					final int diff2 =  (int) 15;
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
					for (int j = st; j < e; j++) {
						RectF b = boundsIntersect.get(j);
						float x = Math.min(bounds.right, b.right) - Math.max(b.left, bounds.left);
						float y = Math.min(bounds.bottom, b.bottom) - Math.max(b.top, bounds.top);
						if ((x > diff && y > diff2) || (x > diff2 && y > diff)) {
							continue next;
						}
					}
					// store in list sorted by left boundary
					if(text.minDistance > 0){
						bounds.set(bounds.left + text.minDistance / 2, bounds.top + text.minDistance / 2,
								bounds.right - text.minDistance / 2, bounds.bottom - text.minDistance / 2);
					}
					boundsIntersect.add(index, bounds);
				}
				
				
				// Shadow layer
				// paintText.setShadowLayer(text.textShadow, 0, 0, Color.WHITE);
//				if(text.textShadow > 0){
//					paintText.setColor(Color.WHITE);
//					paintText.setTextSize(text.textSize + text.textShadow * 2);
//					if(text.drawOnPath != null){
//						cv.drawTextOnPath(text.text, text.drawOnPath, 0, text.vOffset, paintText);
//					} else {
//						cv.drawText(text.text, text.centerX, text.centerY, paintText);
//					}
//					paintText.setTextSize(text.textSize);
//				}
				
				
				paintText.setColor(text.textColor);
				if(text.drawOnPath != null){
					cv.drawTextOnPath(text.text, text.drawOnPath, 0, text.vOffset, paintText);
				} else {
					if(text.textWrap == 0){
						// set maximum for all text
						text.textWrap = 40;
					}
					if(text.shieldRes != 0){
						if(cachedIcons.get(text.shieldRes) == null){
							cachedIcons.put(text.shieldRes, BitmapFactory.decodeResource(context.getResources(), text.shieldRes));
						}
						Bitmap ico = cachedIcons.get(text.shieldRes);
						if (ico  != null) {
							cv.drawBitmap(ico, text.centerX - ico.getWidth() / 2 - 0.5f, text.centerY - text.textSize - 2, paintIcon);
						}
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
								cv.drawText(text.text.substring(start, pos), 
										text.centerX, text.centerY + line * (text.textSize + 2), paintText);
								start = pos;
							} else {
								cv.drawText(text.text.substring(start, lastSpace), 
										text.centerX, text.centerY + line * (text.textSize + 2), paintText); 
								start = lastSpace + 1;
								limit += (start - pos) - 1;
							}
							line++;
							
						}
						
					} else {
						cv.drawText(text.text, text.centerX, text.centerY, paintText);
					}
				}
			}
		}
	}

	
	protected void drawObj(MapRenderObject obj, Canvas canvas, RenderingContext rc, int mainType, boolean renderText) {
		int t = mainType & 3;					
		int type = MapRenderingTypes.getMainObjectType(mainType);
		int subtype = MapRenderingTypes.getObjectSubType(mainType);
		if(t == MapRenderingTypes.POINT_TYPE){
			drawPoint(obj, canvas, rc, type, subtype, renderText);
		} else if(t == MapRenderingTypes.POLYLINE_TYPE){
			drawPolyline(obj, canvas, rc, type, subtype, mainType);
		} else {
			if(t == MapRenderingTypes.MULTY_POLYGON_TYPE &&  !(obj instanceof MultyPolygon)){
				return;
			}
			PointF center = drawPolygon(obj, canvas, rc, type, subtype, t == MapRenderingTypes.MULTY_POLYGON_TYPE);
			String name = obj.getName();
			if(name != null && center != null){
				rc.clearText();
				name = renderObjectText(name, subtype, type, rc.zoom, true, rc);
				if (rc.textSize > 0 && name != null) {
					TextDrawInfo info = new TextDrawInfo(name);
					info.fillProperties(rc, center.x, center.y);
					rc.textToDraw.add(info);
				}
			}
		}
		
	}
	
	
	private PointF calcPoint(MapRenderObject o, int ind, RenderingContext rc){
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

	private PointF drawPolygon(MapRenderObject obj, Canvas canvas, RenderingContext rc, int type, int subtype, boolean multipolygon) {
		float xText = 0;
		float yText = 0;
		int zoom = rc.zoom;
		Path path = null;
		rc.main.emptyArea();
		rc.second.emptyLine();
		rc.main.color = Color.rgb(245, 245, 245);
		
		
		PolygonRenderer.renderPolygon(rc, zoom, type, subtype, this);
		if(!rc.main.fillArea){
			return null;
		}
		int len = 0;
		if (!multipolygon) {
			len = obj.getPointsLength();
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
		} else	{
			len = 0;
			path = new Path();
			for (int i = 0; i < ((MultyPolygon)obj).getBoundsCount(); i++) {
				int cnt = ((MultyPolygon)obj).getBoundPointsCount(i);
				len += cnt;
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
			}
		}

		if (path != null && len > 0) {
			xText /= len;
			yText /= len;

			rc.main.updatePaint(paint);
			canvas.drawPath(path, paint);
			if (rc.second.strokeWidth != 0) {
				rc.second.updatePaint(paint);
				canvas.drawPath(path, paint);
			}
			return new PointF(xText, yText);
		}
		return null;
	}

	
	
	public void clearCachedResources(){
		Collection<Bitmap> values = new ArrayList<Bitmap>(cachedIcons.values());
		cachedIcons.clear();
		for(Bitmap b : values){
			if(b != null){
				b.recycle();
			}
		}
		shaders.clear();
	}
	
	private void drawPoint(MapRenderObject obj, Canvas canvas, RenderingContext rc, int type, int subtype, boolean renderText) {
		int len = obj.getPointsLength();
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
		int zoom = rc.zoom;
		int resId = PointRenderer.getPointBitmap(zoom, type, subtype);
		if(resId != 0){
			IconDrawInfo ico = new IconDrawInfo();
			ico.x = ps.x;
			ico.y = ps.y;
			ico.resId = resId;
			rc.iconsToDraw.add(ico);
		}
		if (renderText) {
			String n = obj.getName();
			if (n != null) {
				rc.clearText();
				n = renderObjectText(n, subtype, type, zoom, true, rc);
				if (rc.textSize > 0 && n != null) {
					TextDrawInfo info = new TextDrawInfo(n);
					info.fillProperties(rc, ps.x, ps.y);
					rc.textToDraw.add(info);
				}
			}
		}
			
	}


	
	private void drawPolyline(MapRenderObject obj, Canvas canvas, RenderingContext rc, int type, int subtype, int wholeType) {
		rc.main.emptyLine();
		rc.second.emptyLine();
		rc.third.emptyLine();
		rc.adds = null;
		
		PolylineRenderer.renderPolyline(type, subtype, wholeType, rc, this);
		
		
		if(rc.main.strokeWidth == 0){
			return;
		}
		int length = obj.getPointsLength();
		if(length < 2){
			return;
		}
		Path path = null;
		float pathRotate = 0;
		float xLength = 0;
		float yLength = 0;
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
				xLength += p.x - xPrev; // not abs
				yLength += p.y - yPrev; // not abs
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
			canvas.drawPath(path, paint);
			if (rc.second.strokeWidth != 0) {
				rc.second.updatePaint(paint);
				canvas.drawPath(path, paint);
				if (rc.third.strokeWidth != 0) {
					rc.third.updatePaint(paint);
					canvas.drawPath(path, paint);
				}
			}
			if (rc.adds != null) {
				for (int i = 0; i < rc.adds.length; i++) {
					rc.adds[i].updatePaint(paint);
					canvas.drawPath(path, paint);
				}
			}
			if (obj.getName() != null) {
				String name = obj.getName();
				rc.clearText();
				name = renderObjectText(name, subtype, type, rc.zoom, false, rc);
				if(rc.textSize == 0 && rc.showAnotherText != null){
					name = renderObjectText(rc.showAnotherText, subtype, type, rc.zoom, false, rc);
				}
				if (name != null && rc.textSize > 0) {
					if (!rc.showTextOnPath) {
						TextDrawInfo text = new TextDrawInfo(name);
						text.fillProperties(rc, middlePoint.x, middlePoint.y);
						rc.textToDraw.add(text);
					} 
					if(rc.showAnotherText != null){
						name = renderObjectText(rc.showAnotherText, subtype, type, rc.zoom, false, rc);
					}
					
					if (rc.showTextOnPath && paintText.measureText(obj.getName()) < Math.max(Math.abs(xLength), Math.abs(yLength))) {
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

						TextDrawInfo text = new TextDrawInfo(name);
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

	private static int[] trunkShields = new int[]{R.drawable.tru_shield1, R.drawable.tru_shield2, R.drawable.tru_shield3,
		R.drawable.tru_shield4, R.drawable.tru_shield5, R.drawable.tru_shield6, R.drawable.tru_shield7,};
	private static int[] motorShields = new int[]{R.drawable.mot_shield1, R.drawable.mot_shield2, R.drawable.mot_shield3,
		R.drawable.mot_shield4, R.drawable.mot_shield5, R.drawable.mot_shield6, R.drawable.mot_shield7,};
	private static int[] primaryShields = new int[]{R.drawable.pri_shield1, R.drawable.pri_shield2, R.drawable.pri_shield3,
		R.drawable.pri_shield4, R.drawable.pri_shield5, R.drawable.pri_shield6, R.drawable.pri_shield7,};
	private static int[] secondaryShields = new int[]{R.drawable.sec_shield1, R.drawable.sec_shield2, R.drawable.sec_shield3,
		R.drawable.sec_shield4, R.drawable.sec_shield5, R.drawable.sec_shield6, R.drawable.sec_shield7,};
	private static int[] tertiaryShields = new int[]{R.drawable.ter_shield1, R.drawable.ter_shield2, R.drawable.ter_shield3,
		R.drawable.ter_shield4, R.drawable.ter_shield5, R.drawable.ter_shield6, R.drawable.ter_shield7,};

	public static String renderObjectText(String name, int subType, int type, int zoom, boolean point, RenderingContext rc) {
		if(name == null || name.length() == 0){
			return null;
		}
		int textSize = 0;
		int textColor = 0;
		int wrapWidth = 0;
		int shadowRadius = 0;
		int textMinDistance = 0;
		int textShield = 0;
		int dy = 0;
		boolean bold = false;
		boolean showTextOnPath = false;
		
		switch (type) {
		case MapRenderingTypes.HIGHWAY : {
			if(name.charAt(0) == MapRenderingTypes.REF_CHAR){
				name = name.substring(1);
				for(int k = 0; k < name.length(); k++){
					if(name.charAt(k) == MapRenderingTypes.REF_CHAR){
						if(k < name.length() - 1 && zoom > 14){
							rc.showAnotherText = name.substring(k + 1);
						}
						name = name.substring(0, k);
						break;
					}
				}
				if(rc.showAnotherText != null && zoom >= 16){
					break;
				}
				if(name.length() > 6){
					name = name.substring(0, 6);
				}
				int len = name.length();
				if(len == 0){
					// skip it
				} else {
					textSize = 10;
					textColor = Color.WHITE;
					bold = true;
					textMinDistance = 70;
					// spacing = 750
					if (subType == MapRenderingTypes.PL_HW_TRUNK) {
						textShield = trunkShields[len - 1];
						if(zoom < 10){
							textSize = 0;
						}
					} else if (subType == MapRenderingTypes.PL_HW_MOTORWAY) {
						textShield = motorShields[len - 1];
						if(zoom < 10){
							textSize = 0;
						}
					} else if (subType == MapRenderingTypes.PL_HW_PRIMARY) {
						textShield = primaryShields[len - 1];
						if(zoom < 11){
							textSize = 0;
						}
					} else if (subType == MapRenderingTypes.PL_HW_SECONDARY) {
						if(zoom < 14){
							textSize = 0;
						}
						textShield = secondaryShields[len - 1];
					} else if (subType == MapRenderingTypes.PL_HW_TERTIARY) {
						if(zoom < 15){
							textSize = 0;
						}
						textShield = tertiaryShields[len - 1];
					} else {
						if(zoom < 16){
							textSize = 0;
						} else {
							showTextOnPath = true;
							textColor = Color.BLACK;
							textSize = 10;
							textMinDistance = 40;
							shadowRadius = 1;
							// spacing = 750;
						}
					}
				}
			} else {
				if(subType == MapRenderingTypes.PL_HW_TRUNK || subType == MapRenderingTypes.PL_HW_PRIMARY 
						|| subType == MapRenderingTypes.PL_HW_SECONDARY){
					textColor = Color.BLACK;
					showTextOnPath = true;
					if(zoom == 13 && type != MapRenderingTypes.PL_HW_SECONDARY){
						textSize = 8;
					} else if(zoom == 14){
						textSize = 9;
					} else if(zoom > 14 && zoom < 17){
						textSize = 10;
					} else if(zoom > 16){
						textSize = 12;
					}
				} else if(subType == MapRenderingTypes.PL_HW_TERTIARY || subType == MapRenderingTypes.PL_HW_RESIDENTIAL
						|| subType == MapRenderingTypes.PL_HW_UNCLASSIFIED || subType == MapRenderingTypes.PL_HW_SERVICE){
					textColor = Color.BLACK;
					showTextOnPath = true;
					if(zoom < 15){
						textSize = 0;
					} else if(zoom < 17){
						textSize = 9;
					} else {
						textSize = 11;
					}
				} else if(subType < 32){
					// highway subtype
					if(zoom >= 16){
						textColor = Color.BLACK;
						showTextOnPath = true;
						textSize = 9;
					}
				} else if(subType == 40){
					// bus stop
					if(zoom >= 17){
						textMinDistance = 20;
						textColor = Color.BLACK;
						textSize = 9;
						wrapWidth = 25;
						dy = 11;
					}
				}
			}
		} break;
		case MapRenderingTypes.WATERWAY : {
			if (subType == 1) {
				if (zoom >= 15 /* && !tunnel */) {
					showTextOnPath = true;
					textSize = 8;
					shadowRadius = 1;
					textColor = 0xff6699cc;
				}
			} else if (subType == 2 || subType == 4) {
				if (zoom > 13 /* && !tunnel */) {
					textSize = 9;
					showTextOnPath = true;
					shadowRadius = 1;
					textColor = 0xff6699cc;
					textMinDistance = 70;
				}
			} else if (subType == 5 || subType == 6) {
				if (zoom >= 15 /* && !tunnel */) {
					textSize = 8;
					showTextOnPath = true;
					shadowRadius = 1;
					textColor = 0xff6699cc;
				}
			} else if (subType == 12) {
				if(zoom >= 15){
					textColor = Color.BLACK;
					textSize = 8;
					shadowRadius = 1;
				}
			} else if (subType == 8) {
				if (zoom >= 15) {
					shadowRadius = 1;
					textSize = 9;
					textColor = 0xff0066ff;
					wrapWidth = 70;
					dy = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.AEROWAY: {
			textColor = 0xff6692da;
			shadowRadius = 1;
			if(name.charAt(0) == MapRenderingTypes.REF_CHAR){
				name = name.substring(1);
			}
			if (subType == 7 || subType == 8) {
				if (zoom >= 15) {
					showTextOnPath = true;
					textSize = 10;
					textColor = 0xff333333;
					textMinDistance = 50;
					shadowRadius = 1;
					// spacing = 750;
				}
			} else if (subType == 10) {
				// airport
				if (zoom >= 10 && zoom <= 12) {
					textSize = 9;
					dy = -12;
					bold = true;

				}
			} else if (subType == 1) {
				// aerodrome
				if (zoom >= 10 && zoom <= 12) {
					textSize = 8;
					dy = -12;
				}
			} else if (subType == 12) {
				if (zoom >= 17) {
					textSize = 10;
					textColor = 0xffaa66cc;
					shadowRadius = 1;
					wrapWidth = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.AERIALWAY: {
			if (subType == 7) {
				if (zoom >= 14) {
					textColor = 0xff6666ff;
					shadowRadius = 1;
					if (zoom == 14) {
						dy = -7;
						textSize = 8;

					} else {
						dy = -10;
						textSize = 10;
					}
				}

			}
		}
			break;
		case MapRenderingTypes.RAILWAY: {
			if (zoom >= 14) {
				textColor = 0xff6666ff;
				shadowRadius = 1;
				if (subType == 13) {
					bold = true;
					if (zoom == 14) {
						dy = -8;
						textSize = 9;
					} else {
						dy = -10;
						textSize = 11;
					}
				} else if (subType == 22 || subType == 23) {
					if (zoom == 14) {
						dy = -7;
						textSize = 8;
					} else {
						dy = -10;
						textSize = 10;
					}
				}
			}
		}
			break;
		case MapRenderingTypes.EMERGENCY: {
			if (zoom >= 17) {
				if (subType == 10) {
					dy = 9;
					textColor = 0xff734a08;
					wrapWidth = 30;
					textSize = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.NATURAL: {
			if (subType == 23) {
				if (zoom >= 15) {
					shadowRadius = 2;
					textColor = 0xff00000;
					textSize = 10;
					wrapWidth = 10;
				}
			} else if (subType == 13) {
				if (zoom >= 14) {
					shadowRadius = 1;
					textColor = 0xff654321;
					textSize = 9;
					dy = 5;
				}
			} else if (subType == 3) {
				if (zoom >= 15) {
					shadowRadius = 1;
					textColor = 0xff654321;
					textSize = 10;
					dy = 9;
					wrapWidth = 20;
				}
			} else if (subType == 21) {
				if (zoom >= 15) {
					textSize = 10;
					shadowRadius = 1;
					wrapWidth = 20;
					textColor = 0xff6699cc;
				}
			} else if (subType == 2) {
				if (zoom >= 14) {
					textSize = 10;
					shadowRadius = 1;
					wrapWidth = 20;
					textColor = 0xff6699cc;
				}
			} else if (subType == 17) {
				if (zoom >= 16) {
					textSize = 8;
					shadowRadius = 1;
					dy = 10;
					wrapWidth = 20;
					textColor = 0xff6699cc;
				}
			}
		}
			break;
		case MapRenderingTypes.LANDUSE: {
			if (zoom >= 15) {
				if (subType == 22) {
					textSize = 10;
					shadowRadius = 1;
					wrapWidth = 20;
					textColor = 0xff6699cc;
				} else if (point) {
					textColor = 0xff000000;
					shadowRadius = 2;
					wrapWidth = 10;
					textSize = 9;
				}
			}
		}
			break;
		case MapRenderingTypes.TOURISM: {
			if (subType == 9) {
				if (zoom >= 16) {
					textColor = 0xff6699cc;
					shadowRadius = 1;
					dy = 15;
					textSize = 9;
				}
			} else if (subType == 12 || subType == 13 || subType == 14) {
				if (zoom >= 17) {
					textColor = 0xff0066ff;
					shadowRadius = 1;
					dy = 14;
					textSize = 10;
				}
			} else if (subType == 11) {
				if (zoom >= 17) {
					textColor = 0xff0066ff;
					shadowRadius = 1;
					dy = 13;
					textSize = 8;
				}
			} else if (subType == 4) {
				if (zoom >= 17) {
					shadowRadius = 1;
					textSize = 10;
					textColor = 0xff0066ff;
					wrapWidth = 70;
					dy = 15;
				}
			} else if (subType == 5) {
				if (zoom >= 17) {
					shadowRadius = 1;
					textSize = 10;
					textColor = 0xff0066ff;
					wrapWidth = 70;
					dy = 19;
				}
			} else if (subType == 7) {
				if (zoom >= 15) {
					textColor = 0xff734a08;
					textSize = 9;
					wrapWidth = 30;
					shadowRadius = 1;
				}
			} else if (subType == 15) {
				if (zoom >= 17) {
					textColor = 0xff734a08;
					textSize = 10;
					dy = 12;
					shadowRadius = 1;
				}
			}
		}
			break;
		case MapRenderingTypes.LEISURE: {
			if (subType == 8) {
				if (zoom >= 15) {
					textColor = Color.BLUE;
					textSize = 9;
					wrapWidth = 30;
					shadowRadius = 1;
				}
			} else if ((zoom >= 15 && point) || zoom >= 17) {
				textColor = 0xff000000;
				shadowRadius = 2;
				wrapWidth = 15;
				textSize = 9;
			}
		}
			break;
		case MapRenderingTypes.HISTORIC: {
			if (zoom >= 17) {
				if (subType == 6) {
					shadowRadius = 1;
					textColor = 0xff654321;
					textSize = 9;
					dy = 12;
					wrapWidth = 20;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_TRANSPORTATION: {
			if (zoom >= 17) {
				if (subType == 1) {
					dy = 9;
					textColor = 0xff0066ff;
					textSize = 9;
					wrapWidth = 34;
				} else if (subType == 4 || subType == 18) {
					textColor = 0xff0066ff;
					shadowRadius = 1;
					dy = 13;
					textSize = 9;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_EDUCATION: {
			if (subType == 4) {
				if (zoom >= 17) {
					dy = 12;
					textColor = 0xff734a08;
					bold = true;
					textSize = 10;
				}
			} else if (subType == 5) {
				if (zoom >= 15) {
					textColor = 0xff000033;
					bold = true;
					textSize = 9;
					wrapWidth = 16;
				}
			} else if (subType == 1 || subType == 2 || subType == 3) {
				if (zoom >= 16) {
					textColor = 0xff000033;
					if(subType != 1){
						dy = 11;
					}
					textSize = 9;
					wrapWidth = 16;
				}
			}
		}
			break;
		case MapRenderingTypes.MAN_MADE: {
			if (subType == 1 || subType == 5) {
				if(zoom >= 16){
					textColor = 0xff444444;
					textSize = 9;
					if(zoom >= 17){
						textSize = 11;
						if(zoom >= 18){
							textSize = 15;
						}
					} 
					wrapWidth = 16;
				}
			} else if (subType == 17) {
				if (zoom >= 15) {
					textColor = 0xff000033;
					textSize = 9;
					shadowRadius = 2;
					dy = 16;
					wrapWidth = 12;
				}
			} else if (subType == 27) {
				if (zoom >= 17) {
					textSize = 9;
					textColor = 0xff734a08;
					dy = 12;
					shadowRadius = 1;
					wrapWidth = 20;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_ENTERTAINMENT: {
			if (zoom >= 17) {
				textSize = 9;
				textColor = 0xff734a08;
				dy = 12;
				shadowRadius = 1;
				wrapWidth = 15;
			}
		} break;
		case MapRenderingTypes.AMENITY_FINANCE: {
			if (subType == 2) {
				if (zoom >= 17) {
					shadowRadius = 1;
					textSize = 9;
					textColor = Color.BLACK;
					dy = 14;
				}
			}
		}
			break;
		case MapRenderingTypes.MILITARY: {
			if (subType == 4) {
				if (zoom >= 12) {
					bold = true;
					textSize = 9;
					shadowRadius = 1;
					wrapWidth = 10;
					textColor = 0xffffc0cb;
				}
			}
		}
			break;
		case MapRenderingTypes.SHOP: {
			if (subType == 42 || subType == 13 || subType == 16 || subType == 19 || subType == 31 || subType == 48) {
				if (zoom >= 17) {
					textColor = 0xff993399;
					textSize = 8;
					dy = 13;
					shadowRadius = 1;
					wrapWidth = 14;
				}
			} else if (subType == 65 || subType == 17) {
				if (zoom >= 16) {
					textSize = 9;
					textColor = 0xff993399;
					dy = 13;
					shadowRadius = 1;
					wrapWidth = 20;
				}
			}

		}
			break;
		case MapRenderingTypes.AMENITY_HEALTHCARE: {
			if (subType == 2) {
				if (zoom >= 16) {
					textSize = 8;
					textColor = 0xffda0092;
					dy = 12;
					shadowRadius = 2;
					wrapWidth = 24;
				}
			} else if (subType == 1) {
				if (zoom >= 17) {
					textSize = 8;
					textColor = 0xffda0092;
					dy = 11;
					shadowRadius = 1;
					wrapWidth = 12;
				}
			}

		}
			break;
		case MapRenderingTypes.AMENITY_OTHER: {
			if (subType == 10) {
				if (zoom >= 17) {
					wrapWidth = 30;
					textSize = 10;
					textColor = 0xff734a08;
					dy = 10;
				}
			} else if (subType == 26) {
				if (zoom >= 17) {
					wrapWidth = 30;
					textSize = 11;
					textColor = 0x000033;
					dy = 10;
				}
			} else if (subType == 16) {
				if (zoom >= 16) {
					textColor = 0xff6699cc;
					shadowRadius = 1;
					dy = 15;
					textSize = 9;
				}
			} else if (subType == 7) {
				if (zoom >= 17) {
					textColor = 0xff0066ff;
					shadowRadius = 1;
					wrapWidth = 20;
					dy = 8;
					textSize = 9;
				}
			} else if (subType == 13) {
				if (zoom >= 17) {
					textColor = 0xff734a08;
					textSize = 10;
					shadowRadius = 1;
					wrapWidth = 20;
					dy = 16;
				}
			} else if (subType == 2) {
				if (zoom >= 16) {
					textColor = 0xff660033;
					textSize = 10;
					shadowRadius = 2;
					wrapWidth = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.AMENITY_SUSTENANCE: {
			if (zoom >= 17) {
				if (subType >= 1 && subType <= 4) {
					shadowRadius = 1;
					textColor = 0xff734a08;
					wrapWidth = 34;
					dy = 13;
					textSize = 10;
				} else if (subType >= 4 && subType <= 6) {
					shadowRadius = 1;
					textColor = 0xff734a08;
					wrapWidth = 34;
					dy = 13;
					textSize = 10;
				}
			}
		}
			break;
		case MapRenderingTypes.ADMINISTRATIVE: {
			shadowRadius = 1;
			switch (subType) {
			case 11: {
				if (zoom >= 14 && zoom < 16) {
					textColor = 0xFF000000;
					textSize = 8;
				} else if (zoom >= 16) {
					textColor = 0xFF777777;
					textSize = 11;
				}
			}
				break;
			case 8:
			case 9: {
				if (zoom >= 12 && zoom < 15) {
					textColor = 0xFF000000;
					textSize = 9;
				} else if (zoom >= 15) {
					textColor = 0xFF777777;
					textSize = 12;
				}
			}
				break;
			case 10: {
				if (zoom >= 12 && zoom < 14) {
					textColor = 0xFF000000;
					textSize = 10;
				} else if (zoom >= 14) {
					textColor = 0xFF777777;
					textSize = 13;
				}
			}
				break;
			case 7: {
				wrapWidth = 20;
				if (zoom >= 9 && zoom < 11) {
					textColor = 0xFF000000;
					textSize = 8;
				} else if (zoom >= 13 && zoom < 14) {
					textColor = 0xFF000000;
					textSize = 10;
				} else if (zoom >= 14) {
					textColor = 0xFF777777;
					textSize = 13;
				}
			}
				break;
			case 12: {
				if (zoom >= 10) {
					textColor = 0xFF000000;
					textSize = 9;
				}
			}
				break;
			case 6: {
				wrapWidth = 20;
				textColor = 0xFF000000;
				if (zoom >= 6 && zoom < 9) {
					textSize = 8;
				} else if (zoom >= 9 && zoom < 11) {
					textSize = 11;
				} else if (zoom >= 11 && zoom <= 14) {
					textSize = 14;
				}
			}
				break;
			case 42: {
				wrapWidth = 20;
				textColor = 0xff9d6c9d;
				if (zoom >= 2 && zoom < 4) {
					textSize = 8;
				} else if (zoom >= 4 && zoom < 7) {
					textSize = 10;
				}
			}
				break;
			case 43:
			case 44: {
				wrapWidth = 20;
				textColor = 0xff9d6c9d;
				if (zoom >= 4 && zoom < 8) {
					textSize = 9;
				} else if (zoom >= 7 && zoom < 9) {
					textSize = 11;
				}
			}
				break;
			case 33: {
				if (zoom >= 17) {
					textSize = 9;
					textColor = 0xff444444;
				}
			}
				break;
			}
		}
			break;
		}
		rc.textColor = textColor;
		rc.textSize = textSize;
		rc.textMinDistance = textMinDistance;
		rc.showTextOnPath = showTextOnPath;
		rc.textShield = textShield;
		rc.textWrapWidth = wrapWidth;
		rc.textHaloRadius = shadowRadius;
		rc.textBold = bold;
		rc.textDy = dy;
		return name;
	}

	
}
