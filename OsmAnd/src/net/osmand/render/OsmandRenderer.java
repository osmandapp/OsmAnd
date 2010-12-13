package net.osmand.render;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TFloatObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
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

	private BaseOsmandRender render;
	private DisplayMetrics dm;
	
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
		public boolean interrupted = false;
		
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
			if(shadowColor == 0){
				shadowLayer = 0;
			}
			p.setShadowLayer(shadowLayer, 0, 0, shadowColor);
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
			shadowLayer = 0;
		}
	}
	
	public OsmandRenderer(Context context) {
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
		render = RendererRegistry.getRegistry().defaultRender();
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
	
	
	public Bitmap generateNewBitmap(RenderingContext rc, List<BinaryMapDataObject> objects, Bitmap bmp, boolean useEnglishNames, List<IMapDownloaderCallback> notifyList) {
		long now = System.currentTimeMillis();
		render = RendererRegistry.getRegistry().getCurrentSelectedRenderer();
		
		// fill area
		Canvas cv = new Canvas(bmp);
		if(render != null){
			int dc = render.getDefaultColor();
			if(dc != 0){
				paintFillEmpty.setColor(dc);
			}
		}
		cv.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), paintFillEmpty);
		
		// put in order map
		int sz = objects.size();
		int init = sz / 4;
		TFloatObjectHashMap<TIntArrayList> orderMap = new TFloatObjectHashMap<TIntArrayList>();
		if (render != null) {
			for (int i = 0; i < sz; i++) {
				BinaryMapDataObject o = objects.get(i);
				int sh = i << 8;

				for (int j = 0; j < o.getTypes().length; j++) {
					// put(orderMap, BinaryMapDataObject.getOrder(o.getTypes()[j]), sh + j, init);
					int wholeType = o.getTypes()[j];
					int mask = wholeType & 3;
					int layer = 0;
					if(mask != 1){
						layer = MapRenderingTypes.getNegativeWayLayer(wholeType);
					}
					if (o instanceof MultyPolygon) {
						put(orderMap, render.getObjectOrder(((MultyPolygon) o).getTag(), ((MultyPolygon) o).getValue(), 
								mask, layer), sh + j, init);
					} else {
						TagValuePair pair = o.getMapIndex().decodeType(MapRenderingTypes.getMainObjectType(wholeType),
								MapRenderingTypes.getObjectSubType(wholeType));
						if (pair != null) {
							put(orderMap, render.getObjectOrder(pair.tag, pair.value, mask, layer), sh + j, init);
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
			
			
			float[] keys = orderMap.keys();
			Arrays.sort(keys);
			int objCount = 0;
			for (int k = 0; k < keys.length; k++) {
				TIntArrayList list = orderMap.get(keys[k]);
				for (int j = 0; j < list.size(); j++) {
					int i = list.get(j);
					int ind = i >> 8;
					int l = i & 0xff;
					BinaryMapDataObject obj = objects.get(ind);

					// show text only for main type
					drawObj(obj, cv, rc, obj.getTypes()[l], l == 0);
					
					objCount++;
				}
				if(objCount > 25){
					notifyListeners(notifyList);
					objCount = 0;
				}
				if(rc.interrupted){
					return null;
				}
			}
			notifyListeners(notifyList);
			
			int skewConstant = (int) (16 * dm.density);
			
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
			log.info(String.format("Rendering has been done in %s ms. (%s points, %s points inside, %s visile from %s)",//$NON-NLS-1$
					time, rc.pointCount, rc.pointInsideCount, rc.visible, rc.allObjects));
			
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
				int d = text.text.indexOf(MapRenderingTypes.DELIM_CHAR);
				// not used now functionality 
				// possibly it will be used specifying english names after that character
				if(d > 0){
					text.text = text.text.substring(0, d);
				}
				if(useEnglishNames){
					text.text = Junidecode.unidecode(text.text);
				}
				RectF bounds = new RectF();
				paintText.setTextSize(text.textSize * dm.density);
				paintText.setFakeBoldText(text.bold);
				float mes = paintText.measureText(text.text);
				if(text.drawOnPath == null || 
						(text.pathRotate > 45 && text.pathRotate < 135) || (text.pathRotate > 225 && text.pathRotate < 315)){
					bounds.set(text.centerX - mes / 2, text.centerY - 3 * text.textSize / 2 ,
							text.centerX + mes / 2 , text.centerY + 3 * text.textSize / 2 );
				} else {
					bounds.set(text.centerX - 3 * text.textSize , text.centerY - mes, 
							text.centerX + 3 * text.textSize , text.centerY + mes );
				}
				if(text.minDistance > 0){
					bounds.set(bounds.left - text.minDistance / 2, bounds.top - text.minDistance / 2,
							bounds.right + text.minDistance / 2, bounds.bottom + text.minDistance / 2);
				}
				List<RectF> boundsIntersect = text.drawOnPath == null || findAllTextIntersections? 
						boundsNotPathIntersect : boundsPathIntersect;
				if(boundsIntersect.isEmpty()){
					boundsIntersect.add(bounds);
				} else {
					final int diff = (int) (3 * dm.density);
					final int diff2 = (int) (15 * dm.density);
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
							cv.drawBitmap(ico, text.centerX - ico.getWidth() / 2 - 0.5f * dm.density, text.centerY - text.textSize - 2 * dm.density, paintIcon);
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

	
	protected void drawObj(BinaryMapDataObject obj, Canvas canvas, RenderingContext rc, int mainType, boolean renderText) {
		int t = mainType & 3;					
		int type = MapRenderingTypes.getMainObjectType(mainType);
		int subtype = MapRenderingTypes.getObjectSubType(mainType);
		rc.allObjects ++;
		if(t == MapRenderingTypes.POINT_TYPE){
			drawPoint(obj, canvas, rc, type, subtype, renderText);
		} else if(t == MapRenderingTypes.POLYLINE_TYPE){
			drawPolyline(obj, canvas, rc, type, subtype, mainType);
		} else if(t == MapRenderingTypes.POLYGON_TYPE){
			drawPolygon(obj, canvas, rc, type, subtype);
		} else {
			if(t == MapRenderingTypes.MULTY_POLYGON_TYPE &&  !(obj instanceof MultyPolygon)){
				return;
			}
			drawMultiPolygon(obj, canvas, rc, type, subtype);
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
		Collection<Bitmap> values = new ArrayList<Bitmap>(cachedIcons.values());
		cachedIcons.clear();
		for(Bitmap b : values){
			if(b != null){
				b.recycle();
			}
		}
		shaders.clear();
	}
	
	private void drawMultiPolygon(BinaryMapDataObject obj, Canvas canvas, RenderingContext rc, int type, int subtype) {
		String tag = ((MultyPolygon)obj).getTag();
		String value = ((MultyPolygon)obj).getValue();
		if(render == null || tag == null){
			return;
		}
		rc.main.emptyArea();
		rc.second.emptyLine();
		rc.main.color = Color.rgb(245, 245, 245);
		
		boolean rendered = render.renderPolygon(tag, value, rc.zoom, rc, this);
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
					rc.clearText();
					name = render.renderObjectText(name, tag, value, rc, false);
					if (rc.textSize > 0 && name != null) {
						TextDrawInfo info = new TextDrawInfo(name);
						info.fillProperties(rc, xText / cnt, yText / cnt);
						rc.textToDraw.add(info);
					}
				}
			}
		}
		rc.main.updatePaint(paint);
		canvas.drawPath(path, paint);
		if (rc.second.strokeWidth != 0) {
		    //rc.second.strokeWidth = 1.5f;
		    //rc.second.color = Color.BLACK; 
		    
			rc.second.updatePaint(paint);
			canvas.drawPath(path, paint);
		}
	}
	
	
	private void drawPolygon(BinaryMapDataObject obj, Canvas canvas, RenderingContext rc, int type, int subtype) {
		TagValuePair pair = obj.getMapIndex().decodeType(type, subtype);
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
		
		boolean rendered = render.renderPolygon(pair.tag, pair.value, zoom, rc, this);
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
				rc.clearText();
				name = render.renderObjectText(name, pair.tag, pair.value, rc, false);
				if (rc.textSize > 0 && name != null) {
					xText /= len;
					yText /= len;
					TextDrawInfo info = new TextDrawInfo(name);
					info.fillProperties(rc, xText, yText);
					rc.textToDraw.add(info);
				}
			}
		}
		return;
	}
	
	private void drawPoint(BinaryMapDataObject obj, Canvas canvas, RenderingContext rc, int type, int subtype, boolean renderText) {
		TagValuePair pair = obj.getMapIndex().decodeType(type, subtype);
		if(render == null || pair == null){
			return;
		}
		
		Integer resId = render.getPointIcon(pair.tag, pair.value, rc.zoom);
		String name = null;
		if (renderText) {
			name = obj.getName();
			if (name != null) {
				rc.clearText();
				name = render.renderObjectText(name, pair.tag, pair.value, rc, false);
			}
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
		if (name != null && rc.textSize > 0) {
			TextDrawInfo info = new TextDrawInfo(name);
			info.fillProperties(rc, ps.x, ps.y);
			rc.textToDraw.add(info);
		}
			
	}
	

	
	private void drawPolyline(BinaryMapDataObject obj, Canvas canvas, RenderingContext rc, int type, int subtype, int wholeType) {
		TagValuePair pair = obj.getMapIndex().decodeType(type, subtype);
		if(render == null || pair == null){
			return;
		}
		int length = obj.getPointsLength();
		if(length < 2){
			return;
		}
		int layer = MapRenderingTypes.getNegativeWayLayer(wholeType);
		rc.main.emptyLine();
		rc.second.emptyLine();
		rc.third.emptyLine();
		rc.adds = null;
		boolean res = render.renderPolyline(pair.tag, pair.value, rc.zoom, rc, this, layer);
		if(rc.main.strokeWidth == 0 || !res){
			return;
		}
		if(rc.zoom >= 16 && "highway".equals(pair.tag) && MapRenderingTypes.isOneWayWay(obj.getHighwayAttributes())){ //$NON-NLS-1$
			rc.adds = getOneWayProperties();
		}
		
		
		rc.visible++;
		
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
					ref = render.renderObjectText(ref, pair.tag, pair.value, rc, true);
					TextDrawInfo text = new TextDrawInfo(ref);
					if(!rc.showTextOnPath){
						text.fillProperties(rc, middlePoint.x, middlePoint.y);
					} else {
						// TODO
					}
					rc.textToDraw.add(text);
					
				}
				
				if(name != null && name.trim().length() > 0){
					rc.clearText();
					name = render.renderObjectText(name, pair.tag, pair.value, rc, false);
					if (rc.textSize > 0) {
						TextDrawInfo text = new TextDrawInfo(name);
						if (!rc.showTextOnPath) {
							text.fillProperties(rc, middlePoint.x, middlePoint.y);
							rc.textToDraw.add(text);
						} else {
							if (paintText.measureText(obj.getName()) < Math.max(Math.abs(xLength), Math.abs(yLength))) {
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
