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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;

public class OsmandRenderer implements Comparator<MapRenderObject> {
	private static final Log log = LogUtil.getLog(OsmandRenderer.class);
	
	private Paint paintStroke;
	private Paint paintText;
	private Paint paintFill;
	private Paint paintFillWhite;
	
	private Resources resources = null;
	
	/// Colors
	private int clFillScreen = Color.rgb(241, 238, 232);
	private int clPoint = Color.rgb(200, 200, 200);
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
	private PathEffect trackPathEffect = new DashPathEffect(new float[]{5,2}, 1);
	
	public OsmandRenderer(){
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
		} else if(obj.isPolyLine() && MapRenderingTypes.isHighway(obj.getType())){
			drawHighway(obj, canvas, leftTileX, topTileY, zoom, rotate);
		} else {
			Paint paint;
			float xText = 0;
			float yText = 0;
			Path path = null;
			if(obj.isPolygon()){
				paint = paintFill;
				// for buildings !
				if(MapRenderingTypes.isPolygonBuilding(obj.getType())){
					paint.setColor(Color.rgb(188, 169, 169));
				} else {
					paint.setColor(Color.rgb(188, 169, 169));
//					paint.setColor(Color.GRAY);
				}
			} else {
				paint = paintStroke;
				paint.setStrokeWidth(2);
				paint.setColor(Color.DKGRAY);
			}
			
			for (int i = 0; i < obj.getPointsLength(); i++) {
				float x = (float) ((MapUtils.getTileNumberX(zoom, obj.getPointLongitude(i)) - leftTileX) * 256f);
				float y = (float) ((MapUtils.getTileNumberY(zoom, obj.getPointLatitude(i)) - topTileY) * 256f);
				xText += x;
				yText += y;
				if (path == null) {
					path = new Path();
					path.moveTo(x, y);
				} else {
					path.lineTo(x, y);
				}
			}
			
			if (path != null) {
				xText /= obj.getPointsLength();
				yText /= obj.getPointsLength();
				canvas.drawPath(path, paint);
				String name = obj.getName();
				if(name != null){
					
					boolean accept = true;
					if(zoom <= 15){
						accept = name.length() < 4; 
					} else if(zoom < 17){
						accept = name.length() < 6;
					} else if(zoom < 18){
						accept = name.length() < 8;
					}
					if(accept){
						canvas.drawText(name, xText, yText, paintText);
					}
				}
			}
		}
		
	}
	
	public void drawPoint(MapRenderObject obj, Canvas canvas, double leftTileX, double topTileY, int zoom, float rotate){
		if (zoom > 15) {
			float x = (float) ((MapUtils.getTileNumberX(zoom, obj.getPointLongitude(0)) - leftTileX) * 256f);
			float y = (float) ((MapUtils.getTileNumberY(zoom, obj.getPointLatitude(0)) - topTileY) * 256f);
			int subType = MapRenderingTypes.getPointSubType(obj.getType());
			int type = MapRenderingTypes.getObjectType(obj.getType());
			if(type == MapRenderingTypes.HIGHWAY && subType == 38){
				if (zoom > 16) {
					drawBitmap(canvas, x, y, R.drawable.h_traffic_light);
				}
			} else {
				paintFill.setColor(clPoint);
				canvas.drawCircle(x, y, 6, paintFill);
			}
		}

	}
	
	public Resources getResources() {
		
		return resources;
	}
	public void setResources(Resources resources) {
		this.resources = resources;
	}
	
	private void drawBitmap(Canvas canvas, float x, float y, int resId){
		if(resources != null){
			Bitmap bmp = BitmapFactory.decodeResource(resources, resId);
			if(bmp != null){
				canvas.drawBitmap(bmp, x - bmp.getWidth() / 2, y - bmp.getHeight() / 2, paintText);
			}
		}
	}
	
	public void drawHighway(MapRenderObject obj, Canvas canvas, double leftTileX, double topTileY, int zoom, float rotate) {
		if(obj.getPointsLength() == 0){
			return;
		}
		
		float xText = 0;
		float yText = 0;
		
		Path path = null;
		float pathRotate = 0;
		float xLength = 0;
		float yLength = 0;
		
		
		Paint paint = paintStroke;
		int hwType = MapRenderingTypes.getHighwayType(obj.getType());
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
		} else if (hwType == MapRenderingTypes.PL_HW_SERVICE || hwType == MapRenderingTypes.PL_HW_UNCLASSIFIED
				||  hwType == MapRenderingTypes.PL_HW_RESIDENTIAL) {
			paint.setColor(clRoadColor);
		} else {
			carRoad = false;
			paint.setStrokeWidth(2);
			paint.setPathEffect(pedestrianPathEffect);
			if(hwType == MapRenderingTypes.PL_HW_TRACK || hwType == MapRenderingTypes.PL_HW_PATH){
				paint.setColor(clTrackRoad);
				paint.setPathEffect(trackPathEffect);
			} else if(hwType == MapRenderingTypes.PL_HW_CYCLEWAY || hwType == MapRenderingTypes.PL_HW_BRIDLEWAY){
				paint.setColor(clCycleWayRoad);
			} else {
				paint.setColor(clPedestrianRoad);
				
			}
			
			
		}
		if (carRoad) {
			paint.setPathEffect(null);
			if (zoom < 16) {
				paint.setStrokeWidth(6);
			} else if (zoom == 16) {
				paint.setStrokeWidth(7);
			} else if (zoom == 17) {
				paint.setStrokeWidth(11);
			} else if (zoom >= 18) {
				paint.setStrokeWidth(16);
			}
			if(hwType == MapRenderingTypes.PL_HW_SERVICE){
				paint.setStrokeWidth(paint.getStrokeWidth() - 2);
			}
		}

		boolean inverse = false;
		float xPrev = 0;
		float yPrev = 0;
		int middle = obj.getPointsLength() / 2;
		for (int i = 0; i < obj.getPointsLength(); i++) {
			float x = (float) ((MapUtils.getTileNumberX(zoom, obj.getPointLongitude(i)) - leftTileX) * 256f);
			float y = (float) ((MapUtils.getTileNumberY(zoom, obj.getPointLatitude(i)) - topTileY) * 256f);
//			xText += x;
//			yText += y;
			if (path == null) {
				path = new Path();
				path.moveTo(x, y);
			} else {
				if (xPrev > 0) {
					xLength += x - xPrev; // not abs
					yLength += y - yPrev; // not abs
				}
				if(i == middle){
					double rot = -Math.atan2(x - xPrev, y - yPrev) * 180 / Math.PI + 90;
					if (rot < 0) {
						rot += 360;
					}
					if (rot < 270 && rot > 90) {
						rot += 180;
						inverse = true;
					}
					pathRotate = (float) rot;
					xText = (x + xPrev) / 2;
					yText = (y + yPrev) / 2;
				}
				if (pathRotate == 0) {
					
				}
				path.lineTo(x, y);
			}
			xPrev = x;
			yPrev = y;
		}
		if (path != null) {
//			xText /= obj.getPointsLength();
//			yText /= obj.getPointsLength();
			canvas.drawPath(path, paint);
			if (obj.getName() != null && carRoad) {
				
				if (paintText.measureText(obj.getName()) < Math.max(Math.abs(xLength), Math.abs(yLength))) {
					
//					paintText.setTextSize(paintText.getTextSize() + 2);
//					int sv = canvas.save();
//					canvas.rotate(pathRotate, xText, yText);
//					canvas.drawText(obj.getName(), xText, yText, paintText);
//					canvas.restoreToCount(sv);
					if (inverse) {
						path.rewind();
						boolean st = true;
						for (int i = obj.getPointsLength() - 1; i >= 0; i--) {
							float x = (float) ((MapUtils.getTileNumberX(zoom, obj.getPointLongitude(i)) - leftTileX) * 256f);
							float y = (float) ((MapUtils.getTileNumberY(zoom, obj.getPointLatitude(i)) - topTileY) * 256f);
							if (st) {
								st = false;
								path.moveTo(x, y);
							} else {
								path.lineTo(x, y);
							}
						}
					}
					
					canvas.drawTextOnPath(obj.getName(), path, 0, 0, paintText);
//					paintText.setTextSize(paintText.getTextSize() - 2);
				}
				
			}
		}
	}
	
	
	@Override
	public int compare(MapRenderObject object1, MapRenderObject object2) {
//		if(object1.isPolygon() != object2.isPolygon()){
//			return object1.isPolygon()? -1 : 1;
//		} else if(object1.isPoint() != object2.isPoint()){
//			return object1.isPoint()? 1 : -1;
//		}
//		int t1 = object1.getType();
//		int t2 = object2.getType();
//		if(MapRenderingTypes.isHighway(t1) && MapRenderingTypes.isHighway(t2)){
//			return new Integer(t1).compareTo(new Integer(t2));
			// TODO change if type of highway changed
//			return t1 > t2 ? -1 : (t1 == t2 ? 0 : 1);
//		}
//		return 0;
		int o1 = object1.getMapOrder();
		int o2 = object2.getMapOrder();
		return o1 < o2 ? -1 : (o1 == o2 ? 0 : 1);
	}
	

}
