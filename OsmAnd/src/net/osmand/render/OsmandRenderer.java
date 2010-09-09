package net.osmand.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.LogUtil;
import net.osmand.R;
import net.osmand.osm.MapRenderObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;

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
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.text.TextPaint;
import android.util.FloatMath;

public class OsmandRenderer implements Comparator<MapRenderObject> {
	private static final Log log = LogUtil.getLog(OsmandRenderer.class);
	
	private Paint paintStroke;
	private TextPaint paintText;
	private Paint paintFill;
	private Paint paintFillEmpty;
	private Paint paintIcon;
	
	
	/// Colors
	private int clFillScreen = Color.rgb(241, 238, 232);
	private int clTrunkRoad = Color.rgb(168, 218, 168); 
	private int clMotorwayRoad = Color.rgb(128,155,192);
	private int clPrimaryRoad = Color.rgb(235, 152, 154); 
	private int clSecondaryRoad = Color.rgb(253, 214, 164);
	private int clTertiaryRoad = Color.rgb(254, 254, 179);
	private int clTrackRoad = Color.GRAY;
	private int clRoadColor = Color.WHITE;
	private int clCycleWayRoad = Color.rgb(20, 20, 250);
	private int clPedestrianRoad = Color.rgb(250, 128, 115);
	
	private PathEffect dashEffect2_2 = new DashPathEffect(new float[]{2,2}, 1);
	private PathEffect dashEffect3_2 = new DashPathEffect(new float[]{3,2}, 1);
	private PathEffect dashEffect4_2 = new DashPathEffect(new float[]{4,2}, 1);
	private PathEffect dashEffect4_3 = new DashPathEffect(new float[]{4,3}, 1);
	private PathEffect dashEffect6_2 = new DashPathEffect(new float[]{6,2}, 1);
	private PathEffect dashEffect5_2 = new DashPathEffect(new float[]{5,2}, 1);
	private PathEffect dashEffect6_3 = new DashPathEffect(new float[]{6,3}, 1);
	private PathEffect dashEffect7_7 = new DashPathEffect(new float[]{7,7}, 1);
	private PathEffect dashEffect6_3_2_3 = new DashPathEffect(new float[]{6,3,2,3,}, 1);
	private PathEffect dashEffect6_3_2_3_2_3 = new DashPathEffect(new float[]{6,3,2,3,2,3}, 1);
	
	private PathEffect arrowDashEffect1 = new DashPathEffect(new float[]{0,12,10,152},0);
	private PathEffect arrowDashEffect2 = new DashPathEffect(new float[]{0,12,9,153}, 1);
	private PathEffect arrowDashEffect3 = new DashPathEffect(new float[]{0,18,2,154}, 1);
	private PathEffect arrowDashEffect4 = new DashPathEffect(new float[]{0,18,1,155}, 1);

	private Map<Integer, Shader> shaders = new LinkedHashMap<Integer, Shader>();
	private Map<Integer, Bitmap> cachedIcons = new LinkedHashMap<Integer, Bitmap>();

	private final Context context;

	
	private static class TextDrawInfo {
		String text = null;
		Path drawOnPath = null;
		float vOffset = 0;
		float centerX = 0;
		float centerY = 0;
		float textSize = 0;
		int textColor = Color.BLACK;
		int textShadow = 0;
		int textWrap = 0;
	}
	
	private static class IconDrawInfo {
		float x = 0;
		float y = 0;
		int resId;
	}
	
	private static class RenderingContext {
		List<TextDrawInfo> textToDraw = new ArrayList<TextDrawInfo>();
		List<IconDrawInfo> iconsToDraw = new ArrayList<IconDrawInfo>();
		float leftX;
		float rightX;
		float bottomY;
		float topY;
		
		int zoom;
		float rotate;
		float cosRotate;
		float sinRotate;
		float tileDivisor;
		
		int pointCount = 0;
		int pointInsideCount = 0;
		
		PointF tempPoint = new PointF();
	}
	
	public OsmandRenderer(Context context){
		this.context = context;
		paintStroke = new Paint();
		paintStroke.setStyle(Style.STROKE);
		paintStroke.setStrokeWidth(2);
		paintStroke.setColor(Color.BLACK);
		paintStroke.setAntiAlias(true);
		
		paintIcon = new Paint();
		paintIcon.setStyle(Style.STROKE);
		
		paintText = new TextPaint();
		paintText.setStyle(Style.FILL);
		paintText.setColor(Color.BLACK);
		paintText.setTextAlign(Align.CENTER);
		paintText.setAntiAlias(true);
		
		paintFill = new Paint();
		paintFill.setStyle(Style.FILL_AND_STROKE);
		paintFill.setStrokeWidth(2);
		paintFill.setColor(Color.LTGRAY);
		paintFill.setAntiAlias(true);
		
		paintFillEmpty = new Paint();
		paintFillEmpty.setStyle(Style.FILL);
		paintFillEmpty.setColor(clFillScreen);
		
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
		int o1 = object1.getMapOrder();
		int o2 = object2.getMapOrder();
		return o1 < o2 ? -1 : (o1 == o2 ? 0 : 1);
	}
	
	
	
	public Bitmap generateNewBitmap(RectF objectLoc, List<MapRenderObject> objects, int zoom, float rotate) {
		long now = System.currentTimeMillis();
		
		Collections.sort(objects, this);
		Bitmap bmp = null; 
		if (objects != null && !objects.isEmpty() && objectLoc.width() != 0f && objectLoc.height() != 0f) {
			// init rendering context
			RenderingContext rc = new RenderingContext();
			rc.leftX = (float) MapUtils.getTileNumberX(zoom, objectLoc.left);
			rc.rightX = (float) MapUtils.getTileNumberX(zoom, objectLoc.right);
			rc.topY = (float) MapUtils.getTileNumberY(zoom, objectLoc.top);
			rc.bottomY = (float) MapUtils.getTileNumberY(zoom, objectLoc.bottom);
			rc.zoom = zoom;
			rc.rotate = rotate;
			rc.cosRotate = FloatMath.cos((float) Math.toRadians(rotate));
			rc.sinRotate = FloatMath.sin((float) Math.toRadians(rotate));
			rc.tileDivisor = (int) (1 << (31 - zoom));
			
			
			bmp = Bitmap.createBitmap((int) ((rc.rightX - rc.leftX) * 256), (int) ((rc.bottomY - rc.topY) * 256), Config.RGB_565);
			Canvas cv = new Canvas(bmp);
			cv.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), paintFillEmpty);
			cv.rotate(-rotate);
			for (MapRenderObject w : objects) {
				draw(w, cv, rc);
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
			for(TextDrawInfo text : rc.textToDraw){
				if(text.text != null){
					paintText.setTextSize(text.textSize);
					paintText.setColor(text.textColor);
					if(text.drawOnPath != null){
						cv.drawTextOnPath(text.text, text.drawOnPath, 0, text.vOffset, paintText);
					} else {
						cv.drawText(text.text, text.centerX, text.centerY, paintText);
					}
				}
			}
			log.info(String.format("Rendering has been done in %s ms. (%s points, %s points inside)", System.currentTimeMillis() - now, //$NON-NLS-1$ 
					rc.pointCount,rc.pointInsideCount)); 
		}
		
		return bmp;
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
		if(tx >= rc.leftX && tx <= rc.rightX && ty >= rc.topY && ty <= rc.bottomY){
			rc.pointInsideCount++;
		}
		float dTileX =  tx - rc.leftX;
		float dTileY = ty - rc.topY;
		float x = (rc.cosRotate * dTileX - rc.sinRotate * dTileY) * 256f ;
		float y = (rc.sinRotate * dTileX + rc.cosRotate * dTileY) * 256f ;
		rc.tempPoint.set(x, y);
		
		return rc.tempPoint;
	}

	

	private PointF drawPolygon(MapRenderObject obj, Canvas canvas, RenderingContext rc) {
		Paint paint = paintFill;
		float xText = 0;
		float yText = 0;
		int zoom = rc.zoom;
		Path path = null;
		int type = MapRenderingTypes.getObjectType(obj.getType());
		int subtype = MapRenderingTypes.getPolygonSubType(obj.getType());
		int color = Color.rgb(245, 245, 245);
		int colorAround = 0;
		Shader shader = null;
		boolean showPolygon = true;
		if (type == MapRenderingTypes.MAN_MADE) {
			showPolygon = zoom > 15;
			if (subtype == MapRenderingTypes.SUBTYPE_BUILDING) {
				color = Color.rgb(188, 169, 169);
			} else if (subtype == MapRenderingTypes.SUBTYPE_GARAGES) {
				color = Color.rgb(221, 221, 221);
			}
			
		} else if (type == MapRenderingTypes.WATERWAY) {
			if(subtype == 3){
				color = Color.rgb(181, 208, 208);
			}
		} else if (type == MapRenderingTypes.POWER) {
			if(subtype == 5 || subtype == 6){
				color = Color.rgb(186, 186, 186);
			}
		} else if (type == MapRenderingTypes.HIGHWAY) {
			if (subtype == MapRenderingTypes.PL_HW_SERVICE || subtype == MapRenderingTypes.PL_HW_UNCLASSIFIED
				|| subtype == MapRenderingTypes.PL_HW_RESIDENTIAL) {
				colorAround = Color.rgb(194, 194, 194);
				color = clRoadColor;
			} else if(subtype == MapRenderingTypes.PL_HW_PEDESTRIAN || subtype == MapRenderingTypes.PL_HW_FOOTWAY){
				color = Color.rgb(236, 236, 236);
				colorAround = Color.rgb(176, 176, 176);
			}
		} else if (type == MapRenderingTypes.TOURISM) {
			showPolygon = zoom > 15;
			if (subtype == 2) {
				color = Color.rgb(204, 153, 153);
			} else if(subtype == 8){
				shader = getShader(R.drawable.h_zoo);
			}
			
		} else if (type == MapRenderingTypes.NATURAL) {
			if(subtype == 23){
				color = Color.rgb(174, 209, 160);
			} else if(subtype == 2){
				color = Color.rgb(238, 204, 85);
			} else if(subtype == 21 || subtype == 5){
				color = Color.rgb(181, 208, 208);
			}
			
		} else if (type == MapRenderingTypes.LANDUSE) {
			switch (subtype) {
			case 1:
				color = Color.rgb(189, 227, 203);
				break;
			case 2:
			case 22:
				color = Color.rgb(180, 213, 240);
				break;
			case 3:
				color = Color.rgb(235, 215, 254);
				break;
			case 4:
				shader = getShader(R.drawable.h_grave_yard);
				break;
			case 5:
				color = Color.rgb(239, 200, 200);
				break;
			case 6:
				color = Color.rgb(157, 157, 108);
				break;
			case 10:
				shader = getShader(R.drawable.h_forest);
				break;
			case 11 :
				color = Color.rgb(223, 209, 214);
				break;
			case 12:
				color = Color.rgb(207, 236, 168);
				break;
			case 15:
				color = Color.rgb(223, 209, 214);
				break;
			case 18:
				color = Color.rgb(252, 216, 219);
				break;
			case 23:
				color = Color.rgb(221, 221, 221);
				break;
			case 24:
				color = Color.rgb(254, 234, 234);
				colorAround = Color.rgb(245, 154, 152);
				break;
			case 27:
				shader = getShader(R.drawable.h_vineyard);
				break;
			}
		} else if (type == MapRenderingTypes.LEISURE) {
			colorAround = Color.rgb(147, 207, 170);
			switch (subtype) {
			case 2:
				color = Color.rgb(189, 227, 203);
				break;
			case 3:
			case 14:
			case 15:
				color = Color.rgb(199, 241, 163);
				break;
			case 6:
				color = Color.rgb(137, 210, 174);
			case 4:
				color = Color.rgb(51, 204, 153);
				break;
			case 5:
				color = Color.rgb(189, 207, 203);
				break;
			case 12:
				color = Color.rgb(206, 246, 202);
				break;
			case 13:
				color = Color.rgb(204, 255, 241);
				break;
			case 11:
				shader = getShader(R.drawable.h_nr);
				break;
			}
		} else if (type == MapRenderingTypes.AMENITY_HEALTHCARE) {
			if (subtype == 2) {
				color = Color.rgb(240, 240, 216);
				colorAround = Color.rgb(212, 168, 158);
			}
		} else if (type == MapRenderingTypes.AMENITY_TRANSPORTATION) {
			if (subtype == 1 || subtype == 2) {
				color = Color.rgb(246, 238, 183);
			}
		} else if (type == MapRenderingTypes.AMENITY_ENTERTAINMENT) {
			if (subtype == 3) {
				color = Color.rgb(204, 153, 153);
			}
		} else if (type == MapRenderingTypes.AMENITY_EDUCATION) {
			if(subtype == 2 || subtype == 3 || subtype == 5){
				color = Color.rgb(240, 240, 216);
				colorAround = Color.rgb(212, 168, 158);
			} else {
				// draw as building education
				color = Color.rgb(188, 169, 169);
			}
		}
		if(!showPolygon){
			return null;
		}
			
		paint.setColor(color);
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
			paint.setShader(shader);
			canvas.drawPath(path, paint);
			if(colorAround != 0){
				paintStroke.setColor(colorAround);
				paintStroke.setStrokeWidth(1);
				canvas.drawPath(path, paintStroke);
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
			if(subType == 9 || subType == 11){
				if(zoom >= 14 && zoom < 16){
					textColor = 0xFF000000;
					textSize = 8;
				} else if(zoom >= 16){
					textColor = 0xFF777777;
					textSize = 11;
				}
			} else 	if(subType == 8){
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
		if(obj.getPointsLength() == 0){
			return;
		}
		int type = MapRenderingTypes.getObjectType(obj.getType());
		int subtype = MapRenderingTypes.getPolylineSubType(obj.getType());
		int zoom = rc.zoom;
		
		boolean showText = true;
		PathEffect pathEffect = null;
		int color = Color.BLACK;
		int shadowLayer = 0;
		int shadowColor = 0;
		float strokeWidth = zoom >= 15 ? 1 : 0;
		
		if (type == MapRenderingTypes.HIGHWAY) {
			int hwType = subtype;
			boolean carRoad = true;
			if (hwType == MapRenderingTypes.PL_HW_TRUNK) {
				color = clTrunkRoad;
			} else if (hwType == MapRenderingTypes.PL_HW_MOTORWAY) {
				color = clMotorwayRoad;
			} else if (hwType == MapRenderingTypes.PL_HW_PRIMARY) {
				color = clPrimaryRoad;
			} else if (hwType == MapRenderingTypes.PL_HW_SECONDARY) {
				color = clSecondaryRoad;
			} else if (hwType == MapRenderingTypes.PL_HW_TERTIARY) {
				color = clTertiaryRoad;
				shadowLayer = 2;
				shadowColor = Color.rgb(186, 186, 186);
			} else if (hwType == MapRenderingTypes.PL_HW_SERVICE || hwType == MapRenderingTypes.PL_HW_UNCLASSIFIED
					|| hwType == MapRenderingTypes.PL_HW_RESIDENTIAL) {
				shadowLayer = 1;
				shadowColor = Color.rgb(194, 194, 194);
				color = clRoadColor;
			} else if (hwType == MapRenderingTypes.PL_HW_PEDESTRIAN) {
				shadowLayer = 1;
				shadowColor = Color.rgb(176, 176, 176);
				color = Color.rgb(236, 236, 236);
			} else {
				carRoad = false;
				strokeWidth = 2;
				pathEffect = dashEffect2_2;
				if (hwType == MapRenderingTypes.PL_HW_TRACK || hwType == MapRenderingTypes.PL_HW_PATH) {
					color = clTrackRoad;
					pathEffect = dashEffect6_2;
				} else if (hwType == MapRenderingTypes.PL_HW_CYCLEWAY || hwType == MapRenderingTypes.PL_HW_BRIDLEWAY) {
					color = clCycleWayRoad;
				} else {
					color = clPedestrianRoad;
				}
			}
			if (carRoad) {
				if (zoom < 16) {
					strokeWidth = 6;
				} else if (zoom == 16) {
					strokeWidth = 8;
				} else if (zoom == 17) {
					strokeWidth = 13;
				} else if (zoom >= 18) {
					strokeWidth = 16;
				} else if (zoom >= 19) {
					strokeWidth = 20;
				}
				if (hwType == MapRenderingTypes.PL_HW_SERVICE) {
					strokeWidth -= 2; 
				}
			}
			showText = carRoad || zoom > 16;
		} else if(type == MapRenderingTypes.BARRIER){
			if(subtype == 5){
				color = Color.GRAY;
				if(zoom == 14){
					strokeWidth = 4;
				} else if(zoom == 15){
					strokeWidth = 6;
				} else if(zoom > 15){
					strokeWidth = 9;
				} else {
					strokeWidth = 0;
				}
			} else {
				strokeWidth = zoom >= 16 ? 1 : 0;
				color = Color.BLACK;
			}
		} else if(type == MapRenderingTypes.POWER){
			if (zoom >= 14) {
				if (subtype == 3) {
					color = Color.rgb(186, 186, 186);
					strokeWidth = zoom == 14 ? 1 : 2;
				} else if (subtype == 4) {
					color = Color.rgb(186, 186, 186);
					strokeWidth = 1;
				}
			} else {
				strokeWidth = 0;
			}
		} else if(type == MapRenderingTypes.AERIALWAY){
			// TODO shader on path doesn't work
			if(zoom >= 12){
				if(subtype == 1 || subtype == 2){
					color = Color.rgb(186, 186, 186);
					strokeWidth = 2;
					//paint.setShader(getShader(R.drawable.h_cable_car));
				}  else if(subtype == 3 || subtype == 4 || subtype == 5){
					color = Color.rgb(186, 186, 186);
					strokeWidth = 2;
					//paint.setShader(getShader(R.drawable.h_chair_lift));
				}
			}
		} else if(type == MapRenderingTypes.ADMINISTRATIVE){
			color = 0xFF800080;
			if(subtype == 29 || subtype == 30){
				// admin level 9, 10
				if (zoom > 12) {
					pathEffect = dashEffect3_2;
					strokeWidth = 2;
					if (zoom > 16) {
						strokeWidth = 3;
					}
				} else {
					strokeWidth = 0;
				}
			} else if(subtype == 28 || subtype == 27){
				// admin level 7, 8
				if(zoom > 11){
					pathEffect = dashEffect5_2;
					strokeWidth = 2;
				} else {
					strokeWidth = 0;
				}
				strokeWidth = 2;
				pathEffect = dashEffect6_3;
			} else if(subtype == 25 || subtype == 26){
				// admin level 5, 6
				if(zoom > 10){
					pathEffect = subtype == 25 ? dashEffect6_3_2_3_2_3 : dashEffect6_3_2_3;
					strokeWidth = 2;
				} else {
					strokeWidth = 0;
				}
				
				
			} else if(subtype == 24){
				// admin level 4
				pathEffect = dashEffect4_3;
				if(zoom >= 4 && zoom <= 6){
					strokeWidth = 0.6f;
				} else if(zoom >= 7 && zoom <= 10){
					strokeWidth = 1;
				} else if(zoom > 10){
					strokeWidth = 3;
				} else {
					strokeWidth = 0;
				}
			} else if(subtype == 23 || subtype == 22){
				// admin level 2, 3
				if(zoom >= 4 && zoom <= 6){
					strokeWidth = 1;
				} else if(zoom >= 7 && zoom <= 9){
					strokeWidth = 2;
				} else if(zoom > 9){
					if(subtype == 22){
						strokeWidth = 6;
					} else {
						strokeWidth = 5;
						pathEffect = dashEffect4_2;
					}
				} else {
					strokeWidth = 0;
				}
				
			}
		} else if(type == MapRenderingTypes.RAILWAY){
			strokeWidth = 2;
			if(subtype == 6){
				color = Color.rgb(153, 153, 153);
				if(zoom > 16){
					strokeWidth = 3;
				}
				pathEffect = dashEffect6_3;
			} else if(subtype == 2){
				color = Color.rgb(62, 62, 62);
			} else if(subtype == 1){
				color = Color.rgb(153, 153, 153);
				if(zoom >= 16){
					strokeWidth = 3;
				}
				pathEffect = dashEffect7_7;
			} else {
				color = Color.rgb(153, 153, 153);
			}
		} else if(type == MapRenderingTypes.WATERWAY){
			if(subtype >= 1 && subtype <= 6){
				strokeWidth = 2;
				color = Color.rgb(181, 208, 208);
			}
		} 
		
		if(strokeWidth == 0){
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
		for (int i = 0; i < obj.getPointsLength(); i++) {
			PointF p = calcPoint(obj, i, rc);
			if (path == null) {
				path = new Path();
				path.moveTo(p.x, p.y);
			} else {
				xLength += p.x - xPrev; // not abs
				yLength += p.y - yPrev; // not abs
				if(i == middle){
					xMid = p.x;
					yMid = p.y;
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
				if (pathRotate == 0) {
					
				}
				path.lineTo(p.x, p.y);
			}
			xPrev = p.x;
			yPrev = p.y;
		}
		if (path != null) {
			paintStroke.setPathEffect(pathEffect);
			if(paintStroke.getShader() != null){
				paintStroke.setShader(null);
			}
			paintStroke.setShadowLayer(shadowLayer, 0, 0, shadowColor);
			paintStroke.setColor(color);
			paintStroke.setStrokeWidth(strokeWidth);
			canvas.drawPath(path, paintStroke);
//			if(type == MapRenderingTypes.HIGHWAY){
//				paintStroke.setColor(0xff6c70d5);
//				
//				paintStroke.setStrokeWidth(1);
//				paintStroke.setPathEffect(arrowDashEffect1);
//				canvas.drawPath(path, paintStroke);
//				
//				paintStroke.setStrokeWidth(2);
//				paintStroke.setPathEffect(arrowDashEffect2);
//				canvas.drawPath(path, paintStroke);
//				
//				paintStroke.setStrokeWidth(3);
//				paintStroke.setPathEffect(arrowDashEffect3);
//				canvas.drawPath(path, paintStroke);
//				
//				paintStroke.setStrokeWidth(4);
//				paintStroke.setPathEffect(arrowDashEffect4);
//				canvas.drawPath(path, paintStroke);
//			}
			if (obj.getName() != null && showText) {
				float w = strokeWidth + 3;
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
					text.centerX = xMid;
					text.centerY = yMid;
					text.drawOnPath = path;
					text.textColor = Color.BLACK;
					text.textSize = w;
					text.vOffset = strokeWidth / 2 - 1;
					rc.textToDraw.add(text);
				}
				
			}
		}
	}
	
}

