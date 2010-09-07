package net.osmand.render;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.R;
import net.osmand.osm.MapRenderObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.util.FloatMath;

public class OsmandRenderer implements Comparator<MapRenderObject> {
	private static final Log log = LogUtil.getLog(OsmandRenderer.class);
	
	private Paint paintStroke;
	private Paint paintText;
	private Paint paintFill;
	private Paint paintFillWhite;
	
	
	/// Colors
	private int clFillScreen = Color.rgb(241, 238, 232);
	private int clTrunkRoad = Color.rgb(128,155,192); 
	private int clMotorwayRoad = Color.rgb(168, 218, 168);
	private int clPrimaryRoad = Color.rgb(235, 152, 154); 
	private int clSecondaryRoad = Color.rgb(253, 214, 164);
	private int clTertiaryRoad = Color.rgb(254, 254, 179);
	private int clTrackRoad = Color.GRAY;
	private int clRoadColor = Color.WHITE;
	private int clCycleWayRoad = Color.rgb(20, 20, 250);
	private int clPedestrianRoad = Color.rgb(250, 128, 115);
	
	private PathEffect pedestrianPathEffect = new DashPathEffect(new float[]{2,2}, 1);
	private PathEffect trackPathEffect = new DashPathEffect(new float[]{6,2}, 1);
	private PathEffect subwayPathEffect = new DashPathEffect(new float[]{6,3}, 1);
	private PathEffect railwayPathEffect = new DashPathEffect(new float[]{7,7}, 1);

	private final Context context;
	
	public OsmandRenderer(Context context){
		this.context = context;
		paintStroke = new Paint();
		paintStroke.setStyle(Style.STROKE);
		paintStroke.setStrokeWidth(2);
		paintStroke.setColor(Color.BLACK);
		paintStroke.setStrokeJoin(Join.ROUND);
		paintStroke.setAntiAlias(true);
		
		
		paintText = new Paint();
		paintText.setStyle(Style.STROKE);
		paintText.setColor(Color.BLACK);
		paintText.setTextAlign(Align.CENTER);
		paintText.setAntiAlias(true);
		
		paintFill = new Paint();
		paintFill.setStyle(Style.FILL_AND_STROKE);
		paintFill.setStrokeWidth(2);
		paintFill.setColor(Color.LTGRAY);
		paintFill.setAntiAlias(true);
		
		paintFillWhite = new Paint();
		paintFillWhite.setStyle(Style.FILL);
		paintFillWhite.setColor(clFillScreen);
		
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
			double leftX = MapUtils.getTileNumberX(zoom, objectLoc.left);
			double rightX = MapUtils.getTileNumberX(zoom, objectLoc.right);
			double topY = MapUtils.getTileNumberY(zoom, objectLoc.top);
			double bottomY = MapUtils.getTileNumberY(zoom, objectLoc.bottom);
			bmp = Bitmap.createBitmap((int) ((rightX - leftX) * 256), (int) ((bottomY - topY) * 256), Config.RGB_565);
			Canvas cv = new Canvas(bmp);
			cv.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), paintFillWhite);
			cv.rotate(-rotate);
			for (MapRenderObject w : objects) {
				draw(w, cv, leftX, topY, zoom, rotate);
			}
		}
		log.info(String.format("Rendering has been done in %s ms. ", System.currentTimeMillis() - now)); //$NON-NLS-1$
		return bmp;
	}

	
	protected void draw(MapRenderObject obj, Canvas canvas, double leftTileX, double topTileY, int zoom, float rotate) {
		if(obj.isPoint()){
			drawPoint(obj, canvas, leftTileX, topTileY, zoom, rotate);
		} else if(obj.isPolyLine()){
			drawPolyline(obj, canvas, leftTileX, topTileY, zoom, rotate);
		} else {
			PointF center = drawPolygon(obj, canvas, leftTileX, topTileY, zoom, rotate);
			if(center != null){
				int typeT = MapRenderingTypes.getPolygonPointType(obj.getType());
				int subT = MapRenderingTypes.getPolygonPointSubType(obj.getType());
				if(typeT > 0 && subT > 0){
					drawPointBitmap(canvas, center.x, center.y, typeT, subT, zoom);
				}
			}
		}
	}
	
	public float calcDiffPixelY(float dTileX, float dTileY, float rotate){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.sin(rad) * dTileX + FloatMath.cos(rad) * dTileY) * 256f ;
	}
	
	public float calcDiffPixelX(float dTileX, float dTileY, float rotate){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.cos(rad) * dTileX - FloatMath.sin(rad) * dTileY) * 256f ;
	}
	

	// suppose that render works in one thread! Otherwise should be done anyway
	private PointF TEMP_POINT = new PointF();
	private PointF calcPoint(double leftTileX, double topTileY, float latitude, float longitude, int zoom, float rotate){
		float dTileX = (float) (MapUtils.getTileNumberX(zoom, longitude) - leftTileX);
		float dTileY = (float) (MapUtils.getTileNumberY(zoom, latitude) - topTileY);
		TEMP_POINT.set(calcDiffPixelX(dTileX, dTileY, rotate), calcDiffPixelY(dTileX, dTileY, rotate));
		return TEMP_POINT;
	}

	

	private PointF drawPolygon(MapRenderObject obj, Canvas canvas, double leftTileX, double topTileY, int zoom, float rotate) {
		Paint paint = paintFill;
		float xText = 0;
		float yText = 0;
		Path path = null;
		int type = MapRenderingTypes.getObjectType(obj.getType());
		int subtype = MapRenderingTypes.getPolygonSubType(obj.getType());
		int color = Color.LTGRAY;
		if (type == MapRenderingTypes.MAN_MADE) {
			if (subtype == MapRenderingTypes.SUBTYPE_BUILDING) {
				color = Color.rgb(188, 169, 169);
			} else if (subtype == MapRenderingTypes.SUBTYPE_GARAGES) {
				color = Color.rgb(221, 221, 221);
			}
			
		} else if (type == MapRenderingTypes.WATERWAY) {
			if(subtype == 3){
				color = Color.rgb(181, 208, 208);
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
			} else {
				// draw as building education
				color = Color.rgb(188, 169, 169);
			}
		} else if (type == MapRenderingTypes.LEISURE) {
			switch (subtype) {
			case 4:
				color = Color.rgb(51, 204, 153);
				break;
			case 12:
				color = Color.rgb(206, 246, 202);
				break;
			}

		} else if (type == MapRenderingTypes.LANDUSE) {
			switch (subtype) {
			case 5:
				color = Color.rgb(239, 200, 200);
				break;
			case 12:
				color = Color.rgb(207, 236, 168);
				break;
			case 15:
				color = Color.rgb(223, 209, 214);
				break;

			}
		}
			
		paint.setColor(color);
		
		for (int i = 0; i < obj.getPointsLength(); i++) {
			float lon = obj.getPointLongitude(i);
			float lat = obj.getPointLatitude(i);
			PointF p = calcPoint(leftTileX, topTileY, lat, lon, zoom, rotate);
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
			canvas.drawPath(path, paint);
			String name = obj.getName();
			if(name != null){
				
				boolean accept = zoom > 17;
				if(zoom > 15){
					accept = name.length() < 4; 
				} else if(zoom > 16){
					accept = name.length() < 8;
				}
				if(accept){
					canvas.drawText(name, xText, yText, paintText);
				}
			}
			return new PointF(xText, yText);
		}
		return null;
	}
	
	private void drawPoint(MapRenderObject obj, Canvas canvas, double leftTileX, double topTileY, int zoom, float rotate){
		if (zoom > 15) {
			float lon = obj.getPointLongitude(0);
			float lat = obj.getPointLatitude(0);
			PointF p = calcPoint(leftTileX, topTileY, lat, lon, zoom, rotate);
			int subType = MapRenderingTypes.getPointSubType(obj.getType());
			int type = MapRenderingTypes.getObjectType(obj.getType());
			drawPointBitmap(canvas, p.x, p.y, type, subType, zoom);
		}
	}

	
	
	private void drawPointBitmap(Canvas canvas, float x, float y, int type, int subType, int zoom) {
		int resId = getPointBitmap(zoom, type, subType);
		if(resId == 0){
//			paintFill.setColor(clPoint);
//			canvas.drawCircle(x, y, 6, paintFill);
		} else {
			Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resId);
			if (bmp != null) {
				canvas.drawBitmap(bmp, x - bmp.getWidth() / 2, y - bmp.getHeight() / 2, paintText);
			}
		}
		
	}
	
	private void drawPolyline(MapRenderObject obj, Canvas canvas, double leftTileX, double topTileY, int zoom, float rotate) {
		if(obj.getPointsLength() == 0){
			return;
		}
		
		Path path = null;
		float pathRotate = 0;
		float xLength = 0;
		float yLength = 0;
		
		
		Paint paint = paintStroke;
		int type = MapRenderingTypes.getObjectType(obj.getType());
		int subtype = MapRenderingTypes.getPolylineSubType(obj.getType());
		
		boolean showText = true;
		boolean showLine = true;
		paint.setPathEffect(null);
		paint.setShadowLayer(0, 0, 0, 0);
		if (type == MapRenderingTypes.HIGHWAY) {
			int hwType = subtype;
			boolean carRoad = true;
			if (hwType == MapRenderingTypes.PL_HW_TRUNK) {
				paint.setColor(clTrunkRoad);
			} else if (hwType == MapRenderingTypes.PL_HW_MOTORWAY) {
				paint.setColor(clMotorwayRoad);
			} else if (hwType == MapRenderingTypes.PL_HW_PRIMARY) {
				paint.setColor(clPrimaryRoad);
			} else if (hwType == MapRenderingTypes.PL_HW_SECONDARY) {
				paint.setColor(clSecondaryRoad);
			} else if (hwType == MapRenderingTypes.PL_HW_TERTIARY) {
				paint.setColor(clTertiaryRoad);
				paint.setShadowLayer(2, 0, 0, Color.rgb(186, 186, 186));
			} else if (hwType == MapRenderingTypes.PL_HW_SERVICE || hwType == MapRenderingTypes.PL_HW_UNCLASSIFIED
					|| hwType == MapRenderingTypes.PL_HW_RESIDENTIAL) {
				paint.setShadowLayer(1, 0, 0, Color.rgb(194, 194, 194));
				paint.setColor(clRoadColor);
			} else if (hwType == MapRenderingTypes.PL_HW_PEDESTRIAN) {
				paint.setShadowLayer(1, 0, 0, Color.rgb(176, 176, 176));
				paint.setColor(Color.rgb(236, 236, 236));
			} else {
				carRoad = false;
				paint.setStrokeWidth(2);
				paint.setPathEffect(pedestrianPathEffect);
				if (hwType == MapRenderingTypes.PL_HW_TRACK || hwType == MapRenderingTypes.PL_HW_PATH) {
					paint.setColor(clTrackRoad);
					paint.setPathEffect(trackPathEffect);
				} else if (hwType == MapRenderingTypes.PL_HW_CYCLEWAY || hwType == MapRenderingTypes.PL_HW_BRIDLEWAY) {
					paint.setColor(clCycleWayRoad);
				
				} else {
					
					paint.setColor(clPedestrianRoad);

				}
			}
			if (carRoad) {
				if (zoom < 16) {
					paint.setStrokeWidth(6);
				} else if (zoom == 16) {
					paint.setStrokeWidth(7);
				} else if (zoom == 17) {
					paint.setStrokeWidth(11);
				} else if (zoom >= 18) {
					paint.setStrokeWidth(16);
				}
				if (hwType == MapRenderingTypes.PL_HW_SERVICE) {
					paint.setStrokeWidth(paint.getStrokeWidth() - 2);
				}
			}
			showText = carRoad || zoom > 16;
		} else if(type == MapRenderingTypes.BARRIER){
			showLine = zoom > 16;	
//			if(subtype == 2){
			paint.setColor(Color.rgb(137, 136, 132));
//			}
			paint.setStrokeWidth(1);
		} else if(type == MapRenderingTypes.RAILWAY){
			paint.setStrokeWidth(2);
			if(subtype == 6){
				paint.setColor(Color.rgb(153, 153, 153));
				if(zoom > 16){
					paint.setStrokeWidth(3);
				}
				paint.setPathEffect(subwayPathEffect);
			} else if(subtype == 2){
				paint.setColor(Color.rgb(62, 62, 62));
			} else if(subtype == 1){
				paint.setColor(Color.rgb(153, 153, 153));
				if(zoom >= 16){
					paint.setStrokeWidth(3);
				}
				paint.setPathEffect(railwayPathEffect);
			} else {
				paint.setColor(Color.rgb(153, 153, 153));
			}
		} else if(type == MapRenderingTypes.WATERWAY){
			if(subtype >= 1 && subtype <= 6){
				paint.setColor(Color.rgb(181, 208, 208));
			}
		} else {
			paint.setColor(Color.BLACK);
			paint.setStrokeWidth(1);
		}
		
		if(!showLine){
			return;
		}

		boolean inverse = false;
		float xPrev = 0;
		float yPrev = 0;
		int middle = obj.getPointsLength() / 2;
		for (int i = 0; i < obj.getPointsLength(); i++) {
			float lon = obj.getPointLongitude(i);
			float lat = obj.getPointLatitude(i);
			PointF p = calcPoint(leftTileX, topTileY, lat, lon, zoom, rotate);
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
				if (pathRotate == 0) {
					
				}
				path.lineTo(p.x, p.y);
			}
			xPrev = p.x;
			yPrev = p.y;
		}
		if (path != null) {
			canvas.drawPath(path, paint);
			if (obj.getName() != null && showText) {
				
				if (paintText.measureText(obj.getName()) < Math.max(Math.abs(xLength), Math.abs(yLength))) {
					if (inverse) {
						path.rewind();
						boolean st = true;
						for (int i = obj.getPointsLength() - 1; i >= 0; i--) {
							float lon = obj.getPointLongitude(i);
							float lat = obj.getPointLatitude(i);
							PointF p = calcPoint(leftTileX, topTileY, lat, lon, zoom, rotate);
							if (st) {
								st = false;
								path.moveTo(p.x, p.y);
							} else {
								path.lineTo(p.x, p.y);
							}
						}
					}
					
					canvas.drawTextOnPath(obj.getName(), path, 0, 0, paintText);
				}
				
			}
		}
	}
	
	
	public static int getPointBitmap(int zoom, int type, int subType) {
		int resId = 0;
		if(type == MapRenderingTypes.HIGHWAY){
			if (zoom > 16) {
				if(subType == 38){
					resId = R.drawable.h_traffic_light;
				} else if(subType == 40){
					resId = R.drawable.h_bus_stop;
				}
			}
			
		} else if(type == MapRenderingTypes.SHOP){
			if (zoom > 15) {
				switch (subType) {
				case 27:
				case 65:
				case 53:
					resId = R.drawable.h_shop_supermarket;
					break;
				case 13:
					resId = R.drawable.h_shop_clothes;
					break;
				case 31:
					resId = R.drawable.h_shop_hairdresser;
					break;
				}
			}
			if (zoom > 16) {
				switch (subType) {
				case 48:
					resId = R.drawable.h_shop_butcher;
					break;
				case 42:
					resId = R.drawable.h_shop_bakery;
					break;
				case 20:
					resId = R.drawable.h_shop_diy;
					break;
				case 16:
					resId = R.drawable.h_shop_convenience;
					break;
				
					
				}
			}
		} else if(type == MapRenderingTypes.TOURISM){
			if (zoom > 15) {
				switch (subType) {
				case 4:
					resId = R.drawable.h_camp_site;
					break;
				case 5:
					resId = R.drawable.h_caravan_park;
					break;
				case 6:
					resId = R.drawable.h_camp_site; // picnic
					break;
				case 9:
					resId = R.drawable.h_alpinehut;
					break;
				case 10:
				case 11:
					resId = R.drawable.h_guest_house;
					break;
				case 12:
				case 14:
					resId = R.drawable.h_hostel;
					break;
				case 13:
					resId = R.drawable.h_hotel;
					break;
				case 15:
					resId = R.drawable.h_museum;
					break;
				}
			}
		} else if(type == MapRenderingTypes.AMENITY_SUSTENANCE){
			if (zoom > 15) {
				switch (subType) {
				case 1:
					resId = R.drawable.h_restaurant;
					break;
				case 2:
					resId = R.drawable.h_cafe;
					break;
				case 4:
					resId = R.drawable.h_fast_food;
					break;
				case 5:
					resId = R.drawable.h_pub;
					break;
				case 7:
				case 6:
					resId = R.drawable.h_bar;
					break;
				case 8:
					resId = R.drawable.h_food_drinkingtap;
					break;
				}
			}
		} else if(type == MapRenderingTypes.AMENITY_EDUCATION){
			if (zoom > 15){
				if(subType == 2){
					resId = R.drawable.h_school;
				} else if(subType == 4){
					resId = R.drawable.h_library;
				}
			}
		} else if (type == MapRenderingTypes.AMENITY_TRANSPORTATION) {
			if (subType == 1 || subType == 2) {
				resId = R.drawable.h_parking;
			} else if (subType == 4) {
				resId = R.drawable.h_fuel;
			} else if (subType == 18) {
				resId = R.drawable.h_bus_station;
			}
		} else if (type == MapRenderingTypes.AMENITY_FINANCE) {
			if (subType == 1) {
				if (zoom > 16) {
					resId = R.drawable.h_atm;
				}
			} else if (subType == 2) {
				if (zoom > 15) {
					resId = R.drawable.h_bank;
				}
			}
		} else if (type == MapRenderingTypes.AMENITY_HEALTHCARE) {
			if (subType == 1) {
				if (zoom > 15) {
					resId = R.drawable.h_pharmacy;
				}
			} else if (subType == 2) {
				resId = R.drawable.h_hospital;
			}
		} else if (type == MapRenderingTypes.AMENITY_ENTERTAINMENT) {
			if (zoom >= 15) {
				if (subType == 3) {
					resId = R.drawable.h_cinema;
				} else if(subType == 9) {
					resId = R.drawable.h_theatre;
				}
			}
		} else if(type == MapRenderingTypes.AMENITY_OTHER){
			if (zoom > 16) {
				switch (subType) {
				case 10:
					resId = R.drawable.h_police;
					break;
				case 18:
					resId = R.drawable.h_toilets;
					break;
				case 15:
					resId = R.drawable.h_recycling;
					break;
				case 7:
					resId = R.drawable.h_embassy;
					break;
				case 8:
					resId = R.drawable.h_grave_yard;
					break;
				case 17:
					resId = R.drawable.h_telephone;
					break;
				case 11:
					resId = R.drawable.h_postbox;
					break;
				case 12:
					resId = R.drawable.h_postoffice;
					break;
				}
			}
		}
		return resId;
	}

}
