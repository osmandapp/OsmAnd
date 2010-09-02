package net.osmand.render;

import java.util.List;

import net.osmand.LogUtil;
import net.osmand.osm.MapRenderObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;

public class OsmandRenderer {
	private static final Log log = LogUtil.getLog(OsmandRenderer.class);
	
	private Paint paintStroke;
	private Paint paintText;
	private Paint paintFill;
	private Paint paintFillWhite;
	private Bitmap bmp;
	
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
		paintFillWhite.setColor(Color.rgb(241, 238, 232));
	}
	
	public Bitmap getBitmap(){
		return bmp;
	}
	
	public synchronized void clearBitmap(){
		if(bmp != null){
			bmp.recycle();
		}
		bmp = null;
	}
	
	public void generateNewBitmap(RectF objectLoc, List<MapRenderObject> objects, int zoom, float rotate) {
		long now = System.currentTimeMillis();
		// TODO sort objects first of all 
		if(bmp != null){
			bmp.recycle();
			bmp = null;
		}
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
				if(obj.getName() != null){
					canvas.drawText(obj.getName(), xText, yText, paintText);
				}
			}
		}
		
	}
	
	public void drawPoint(MapRenderObject obj, Canvas canvas, double leftTileX, double topTileY, int zoom, float rotate){
		if (zoom > 15) {
//			float x = (float) ((MapUtils.getTileNumberX(zoom, obj.getPointLongitude(0)) - leftTileX) * 256f);
//			float y = (float) ((MapUtils.getTileNumberY(zoom, obj.getPointLatitude(0)) - topTileY) * 256f);
//			canvas.drawCircle(x, y, 6, paintFill);
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
		if (hwType == MapRenderingTypes.PL_HW_MOTORWAY || hwType == MapRenderingTypes.PL_HW_TRUNK) {
			paint.setColor(Color.BLUE);
		} else if (hwType == MapRenderingTypes.PL_HW_PRIMARY) {
			paint.setColor(Color.rgb(235, 152, 154));
		} else if (hwType == MapRenderingTypes.PL_HW_SECONDARY) {
			paint.setColor(Color.rgb(253, 214, 164));
		} else if (hwType == MapRenderingTypes.PL_HW_SERVICE || hwType == MapRenderingTypes.PL_HW_UNCLASSIFIED
				|| hwType == MapRenderingTypes.PL_HW_TERTIARY || hwType == MapRenderingTypes.PL_HW_RESIDENTIAL) {
			paint.setColor(Color.WHITE);
		} else {
			// skip for now
			return;
		}
		if (zoom < 16) {
			paint.setStrokeWidth(6);
		} else if (zoom == 16) {
			paint.setStrokeWidth(7);
		} else if (zoom == 17) {
			paint.setStrokeWidth(11);
		} else if (zoom >= 18) {
			paint.setStrokeWidth(16);
		}

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
			if (obj.getName() != null) {
				if (paintText.measureText(obj.getName()) < Math.max(Math.abs(xLength), Math.abs(yLength))) {
					int sv = canvas.save();
					canvas.rotate(pathRotate, xText, yText);
					canvas.drawText(obj.getName(), xText, yText, paintText);
					canvas.restoreToCount(sv);
				}
			}
		}
	}
	

}
