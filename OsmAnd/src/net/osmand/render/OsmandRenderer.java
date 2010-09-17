package net.osmand.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.LogUtil;
import net.osmand.osm.MapRenderObject;
import net.osmand.osm.MapRenderingTypes;

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
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.text.TextPaint;
import android.util.FloatMath;

public class OsmandRenderer implements Comparator<MapRenderObject> {
	private static final Log log = LogUtil.getLog(OsmandRenderer.class);
	
	private TextPaint paintText;
	private Paint paint;
	
	private Paint paintShield;
	
	private Paint paintFillEmpty;
	private Paint paintIcon;
	private float[] hsv = new float[3];
	
	public static final int TILE_SIZE = 256; 
	
	/// Colors
	private int clFillScreen = Color.rgb(241, 238, 232);

	private Map<String, PathEffect> dashEffect = new LinkedHashMap<String, PathEffect>();
	private Map<Integer, Shader> shaders = new LinkedHashMap<Integer, Shader>();
	private Map<Integer, Bitmap> cachedIcons = new LinkedHashMap<Integer, Bitmap>();

	private final Context context;

	
	private static class TextDrawInfo {
		String text = null;
		Path drawOnPath = null;
		float vOffset = 0;
		float centerX = 0;
		float pathRotate = 0;
		float centerY = 0;
		float textSize = 0;
		int textColor = Color.BLACK;
		int textShadow = 0;
		int textWrap = 0;
		boolean shield = false;
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
		float cosRotate;
		float sinRotate;

		
		// polyline props
		boolean showText = true;
		
		RenderingPaintProperties main = new RenderingPaintProperties();
		RenderingPaintProperties second = new RenderingPaintProperties();
		RenderingPaintProperties third = new RenderingPaintProperties();
		RenderingPaintProperties[] adds = null; 
		
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
		paintText.setAntiAlias(true);
		
		paint = new Paint();
		paint.setAntiAlias(true);
		
		paintShield = new Paint();
		paintShield.setAntiAlias(true);
		
		
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
	
	@Override
	public int compare(MapRenderObject object1, MapRenderObject object2) {
		float o1 = object1.getMapOrder();
		float o2 = object2.getMapOrder();
		return o1 < o2 ? -1 : (o1 == o2 ? 0 : 1);
	}
	
	
	
	public Bitmap generateNewBitmap(int width, int height, float leftTileX, float topTileY, 
			List<MapRenderObject> objects, int zoom, float rotate) {
		long now = System.currentTimeMillis();
		
		Collections.sort(objects, this);
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
			rc.cosRotate = FloatMath.cos((float) Math.toRadians(rotate));
			rc.sinRotate = FloatMath.sin((float) Math.toRadians(rotate));
			bmp = Bitmap.createBitmap(width, height, Config.RGB_565);
			
			Canvas cv = new Canvas(bmp);
			cv.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), paintFillEmpty);
			for (MapRenderObject o : objects) {
				draw(o, cv, rc);
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
			drawTextOverCanvas(rc, cv);
			log.info(String.format("Rendering has been done in %s ms. (%s points, %s points inside)", System.currentTimeMillis() - now, //$NON-NLS-1$ 
					rc.pointCount,rc.pointInsideCount)); 
		}
		
		return bmp;
	}

	public void drawTextOverCanvas(RenderingContext rc, Canvas cv) {
		List<RectF> boundsIntersect = new ArrayList<RectF>();
		int size = rc.textToDraw.size();
		next: for (int i = 0; i < size; i++) {
			TextDrawInfo text  = rc.textToDraw.get(i);
			if(text.text != null){
				paintText.setTextSize(text.textSize);
				paintText.setColor(text.textColor);
				RectF bounds = new RectF();
				float mes = paintText.measureText(text.text);
				if(text.drawOnPath == null || 
						(text.pathRotate > 45 && text.pathRotate < 135) || (text.pathRotate > 225 && text.pathRotate < 315)){
					bounds.set(text.centerX - mes / 2, text.centerY - 3 * text.textSize / 2,
							text.centerX + mes / 2, text.centerY + 3 * text.textSize / 2);
				} else {
					bounds.set(text.centerX - 3 * text.textSize / 2, text.centerY - mes/2, 
										text.centerX + 3 * text.textSize / 2, text.centerY + mes/2);
				}
				final int diff = 3;
				final int diff2 = 15;
				for(int j = 0; j < boundsIntersect.size() ; j++){
					RectF b = boundsIntersect.get(j);
					float x = Math.min(bounds.right, b.right) - Math.max(b.left, bounds.left);
					float y = Math.min(bounds.bottom, b.bottom) - Math.max(b.top, bounds.top);
					if((x > diff && y > diff2) || (x > diff2 && y > diff)){
						continue next;
					}
				}
				boundsIntersect.add(bounds);
				paintText.setFakeBoldText(false);
				if(text.drawOnPath != null){
					cv.drawTextOnPath(text.text, text.drawOnPath, 0, text.vOffset, paintText);
				} else if(text.shield){
					bounds.set(text.centerX - mes / 2 - 4, text.centerY - text.textSize,
							text.centerX + mes / 2 + 4, text.centerY + text.textSize / 2);
					paintShield.setStyle(Style.STROKE);
					paintShield.setColor(Color.WHITE);
					paintShield.setStrokeWidth(3);
					cv.drawOval(bounds, paintShield);
					
					paintShield.setStyle(Style.FILL);
					Color.colorToHSV(paintText.getColor(), hsv);
					hsv[2] *= 0.85;
					paintShield.setColor(Color.HSVToColor(hsv));
					cv.drawOval(bounds, paintShield);
					paintText.setFakeBoldText(true);
					paintText.setColor(Color.WHITE);
					cv.drawText(text.text, text.centerX, text.centerY + 3, paintText);
				} else {
					cv.drawText(text.text, text.centerX, text.centerY, paintText);
				}
			}
		}
	}

	
	protected void draw(MapRenderObject obj, Canvas canvas, RenderingContext rc) {
		if(obj.isPoint()){
			drawPoint(obj, canvas, rc);
		} else if(obj.isPolyLine()){
			drawPolyline(obj, canvas, rc);
		} else {
			PointF center = drawPolygon(obj, canvas, rc);
			if(center != null){
				int typeT = MapRenderingTypes.getPolygonPointType(obj.getType());
				int subT = MapRenderingTypes.getPolygonPointSubType(obj.getType());
				if(typeT != 0 && subT != 0){
					int resId = PointRenderer.getPointBitmap(rc.zoom, typeT, subT);
					if(resId != 0){
						IconDrawInfo ico = new IconDrawInfo();
						ico.x = center.x;
						ico.y = center.y;
						ico.resId = resId;
						rc.iconsToDraw.add(ico);
					}
				}
			}
		}
	}
	
	
	
	private PointF calcPoint(MapRenderObject o, int ind, RenderingContext rc){
		rc.pointCount ++;
		float tx = o.getPoint31XTile(ind) / rc.tileDivisor;
		float ty = o.getPoint31YTile(ind) / rc.tileDivisor;
		float dTileX =  tx - rc.leftX;
		float dTileY = ty - rc.topY;
		float x = (rc.cosRotate * dTileX - rc.sinRotate * dTileY) * TILE_SIZE ;
		float y = (rc.sinRotate * dTileX + rc.cosRotate * dTileY) * TILE_SIZE ;
		rc.tempPoint.set(x, y);
//		rc.tempPoint.set(dTileX * TILE_SIZE, dTileY * TILE_SIZE);
		if(rc.tempPoint.x >= 0 && rc.tempPoint.x < rc.width && 
				rc.tempPoint.y >= 0 && rc.tempPoint.y < rc.height){
			rc.pointInsideCount++;
		}
		
		
		
		return rc.tempPoint;
	}

	private PointF drawPolygon(MapRenderObject obj, Canvas canvas, RenderingContext rc) {
		float xText = 0;
		float yText = 0;
		int zoom = rc.zoom;
		Path path = null;
		int type = MapRenderingTypes.getObjectType(obj.getType());
		int subtype = MapRenderingTypes.getPolygonSubType(obj.getType());
		rc.main.emptyArea();
		rc.second.emptyLine();
		rc.main.color = Color.rgb(245, 245, 245);
		
		
		PolygonRenderer.renderPolygon(rc, zoom, type, subtype, this);
		if(!rc.main.fillArea){
			return null;
		}
			
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
		
		if (path != null) {
			xText /= obj.getPointsLength();
			yText /= obj.getPointsLength();
			rc.main.updatePaint(paint);
			canvas.drawPath(path, paint);
			if(rc.second.strokeWidth != 0){
				rc.second.updatePaint(paint);
				canvas.drawPath(path, paint);
			}
			String name = obj.getName();
			if(name != null){
				float textSize = 16;
				boolean accept = zoom > 17;
				if(zoom > 15 && zoom <= 16){
					accept = name.length() < 4;
					textSize = 10;
				} else if(zoom <= 17){
					accept = name.length() < 8;
					textSize = 12;
				}
				if(accept){
					TextDrawInfo text = new TextDrawInfo();
					text.textSize = textSize;
					text.textWrap = 20;
					text.centerX = xText;
					text.centerY = yText;
					text.text = name;
					rc.textToDraw.add(text);
				}
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
	
	private void drawPoint(MapRenderObject obj, Canvas canvas, RenderingContext rc) {
		PointF p = calcPoint(obj, 0, rc);
		int subType = MapRenderingTypes.getPointSubType(obj.getType());
		int type = MapRenderingTypes.getObjectType(obj.getType());
		int zoom = rc.zoom;
		int resId = PointRenderer.getPointBitmap(zoom, type, subType);
		if(resId != 0){
			IconDrawInfo ico = new IconDrawInfo();
			ico.x = p.x;
			ico.y = p.y;
			ico.resId = resId;
			rc.iconsToDraw.add(ico);
		}
		
		int textSize = 0;
		int textColor = 0;
		@SuppressWarnings("unused")
		int textWrap = 0;
		@SuppressWarnings("unused")
		int shadowRadius = 0;
		@SuppressWarnings("unused")
		int shadowColor = Color.WHITE;
		if(type == MapRenderingTypes.ADMINISTRATIVE){
			shadowRadius = 4;
			if(subType == 11){
				if(zoom >= 14 && zoom < 16){
					textColor = 0xFF000000;
					textSize = 8;
				} else if(zoom >= 16){
					textColor = 0xFF777777;
					textSize = 11;
				}
			} else 	if(subType == 8  || subType == 9){
				if(zoom >= 12 && zoom < 15){
					textColor = 0xFF000000;
					textSize = 9;
				} else if(zoom >= 15){
					textColor = 0xFF777777;
					textSize = 12;
				}
			} else 	if(subType == 10){
				if(zoom >= 12 && zoom < 14){
					textColor = 0xFF000000;
					textSize = 10;
				} else if(zoom >= 14){
					textColor = 0xFF777777;
					textSize = 13;
				}
			} else 	if(subType == 7){
				textWrap = 20;
				if(zoom >= 9 && zoom < 11){
					textColor = 0xFF000000;
					textSize = 8;
				} else if(zoom >= 13 && zoom < 14){
					textColor = 0xFF000000;
					textSize = 10;
				} else if(zoom >= 14){
					textColor = 0xFF777777;
					textSize = 13;
				}
			} else 	if(subType == 6){
				textWrap = 20;
				textColor = 0xFF000000;
				if(zoom >= 6 && zoom < 9){
					textSize = 8;
				} else if(zoom >= 9 && zoom < 11){
					textSize = 11;
				} else if(zoom >= 11 && zoom <= 14){
					textSize = 14;
				}
			} else 	if(subType == 42){
				textWrap = 20;
				textColor = 0xff9d6c9d;
				if(zoom >= 2 && zoom < 4){
					textSize = 8;
				} else if(zoom >= 4 && zoom < 7){
					textSize = 10;
				}
			} else 	if(subType == 43 || subType == 44){
				textWrap = 20;
				textColor = 0xff9d6c9d;
				if(zoom >= 4 && zoom < 8){
					textSize = 9;
				} else if(zoom >= 7 && zoom < 9){
					textSize = 11;
				}
			}
			textSize += 4; // for small screen
		}
		if(obj.getName() != null && textSize > 0){
			paintText.setTextSize(textSize);
			paintText.setColor(textColor);
			canvas.drawText(obj.getName(), p.x, p.y - textSize, paintText);
			
		}
			
	}

	
	private void drawPolyline(MapRenderObject obj, Canvas canvas, RenderingContext rc) {
		int type = MapRenderingTypes.getObjectType(obj.getType());
		int subtype = MapRenderingTypes.getPolylineSubType(obj.getType());
		rc.main.emptyLine();
		rc.second.emptyLine();
		rc.third.emptyLine();
		rc.adds = null;
		PolylineRenderer.renderPolyline(type, subtype, obj.getType(), rc, this);
		
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
		int middle = obj.getPointsLength() / 2;
		
		for (int i = 0; i < length ; i++) {
			PointF p = calcPoint(obj, i, rc);
			if(i == 0 || i == length -1){
				xMid+= p.x;
				yMid+= p.y;
			}
			if (path == null) {
				path = new Path();
				path.moveTo(p.x, p.y);
			} else {
				xLength += p.x - xPrev; // not abs
				yLength += p.y - yPrev; // not abs
				if(i == middle){
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
			if (obj.getName() != null && rc.showText) {
				float w = rc.main.strokeWidth + 3;
				if(w < 10){
					 w = 10;
				}
				paintText.setTextSize(w);
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
					
					TextDrawInfo text = new TextDrawInfo();
					text.text = obj.getName();
					text.centerX = xMid /2;
					text.centerY = yMid /2;
					text.pathRotate = pathRotate;
					text.drawOnPath = path;
					text.textColor = Color.BLACK;
					text.textSize = w;
					text.vOffset = rc.main.strokeWidth / 2 - 1;
					rc.textToDraw.add(text);
					if(text.text.startsWith(MapRenderingTypes.REF_CHAR)){
						if(text.text.length() > 5){
							text.text = text.text.substring(1, 5);
						} else {
							text.text = text.text.substring(1);
						}
						text.textColor = rc.second.strokeWidth != 0 ? rc.second.color : rc.main.color;
						text.shield = true;
						text.drawOnPath = null;
					}

				}
			}
		}
	}

	
	
}

