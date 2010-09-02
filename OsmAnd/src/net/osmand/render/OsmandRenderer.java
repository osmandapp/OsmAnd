package net.osmand.render;

import java.util.List;

import org.apache.commons.logging.Log;

import net.osmand.LogUtil;
import net.osmand.osm.MapRenderObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
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
	
	public void generateNewBitmap(RectF objectLoc, List<MapRenderObject> objects, int zoom, float rotate) {
		long now = System.currentTimeMillis();
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

	
	protected void draw(MapRenderObject obj, Canvas canvas, double leftTileX, double topTileY, float zoom, float rotate) {
		float xText = -1;
		float yText = -1;
		Path path = null;
		if (obj.isPoint()) {
			float x = (float) ((MapUtils.getTileNumberX(zoom, obj.getPointLongitude(0)) - leftTileX) * 256f);
			float y = (float) ((MapUtils.getTileNumberY(zoom, obj.getPointLatitude(0)) - topTileY) * 256f);
//			xText = x;
//			yText = y;
			canvas.drawCircle(x, y, 6, paintFill);
		} else {
			
			Paint paint;
			if(obj.isPolygon()){
				paint = paintFill; 
				// for buildings !
				if(MapRenderingTypes.isPolygonBuilding(obj.getType())){
					paint.setColor(Color.rgb(188, 169, 169));
				} else {
					paint.setColor(Color.GRAY);
				}
			} else {
				paint = paintStroke;
				if(MapRenderingTypes.isHighway(obj.getType())){
					int hwType = MapRenderingTypes.getHighwayType(obj.getType());
					if(hwType == MapRenderingTypes.PL_HW_MOTORWAY || hwType == MapRenderingTypes.PL_HW_TRUNK){
						paint.setColor(Color.BLUE);
					} else if(hwType == MapRenderingTypes.PL_HW_PRIMARY){
						paint.setColor(Color.rgb(235, 152, 154));
					} else if(hwType == MapRenderingTypes.PL_HW_SECONDARY){
						paint.setColor(Color.rgb(253, 214, 164));
					} else {
						paint.setColor(Color.WHITE);
					}
					paint.setStrokeWidth(5);
					
				} else {
					paint.setStrokeWidth(2);
					paint.setColor(Color.DKGRAY);
				}
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
			}
		}
		if(obj.getName() != null && xText > 0 && yText > 0){
			if(obj.isPolyLine()){
				canvas.drawTextOnPath(obj.getName(), path, 0, 0, paintText);
			} else {
				canvas.drawText(obj.getName(), xText, yText, paintText);
			}
		}
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
}
