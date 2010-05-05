package com.osmand.views;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.osmand.DefaultLauncherConstants;
import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.data.DataTileManager;
import com.osmand.map.ITileSource;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;

public class POIMapLayer extends View {
	private DataTileManager<Node> nodeManager = null;
	private LatLon currentLocation = null;
	private int zoomLevel = DefaultLauncherConstants.MAP_startMapZoom;
	private Map<Node, Point> points = new LinkedHashMap<Node, Point>();
	private Paint pointUI;
	private static final int radiusClick = 16;
	private Toast previousShownToast =null;
	private final static Log log = LogUtil.getLog(POIMapLayer.class);
	

	public POIMapLayer(Context context, AttributeSet attrs) {
		super(context, attrs);
		initUI();
	}
	
	public POIMapLayer(Context context) {
		super(context);
		initUI();
	}

	private void initUI() {
		pointUI = new Paint();
		pointUI.setColor(Color.CYAN);
		pointUI.setAlpha(150);
		pointUI.setAntiAlias(true);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		for(Node n : points.keySet()){
			Point p = points.get(n);
			canvas.drawCircle(p.x, p.y, radiusClick/2, pointUI);
		}
	}
	
	public void preparePoints() {
		points.clear();
		if (nodeManager != null && currentLocation != null) {
			double tileNumberX = MapUtils.getTileNumberX(zoomLevel, currentLocation.getLongitude());
			double tileNumberY = MapUtils.getTileNumberY(zoomLevel, currentLocation.getLatitude());
			double xTileLeft = tileNumberX - getWidth() / (2d * getTileSize());
			double xTileRight = tileNumberX + getWidth() / (2d * getTileSize());
			double yTileUp = tileNumberY - getHeight() / (2d * getTileSize());
			double yTileDown = tileNumberY + getHeight() / (2d * getTileSize());

			List<Node> objects = nodeManager.getObjects(MapUtils.getLatitudeFromTile(zoomLevel, yTileUp), 
					MapUtils.getLongitudeFromTile(zoomLevel, xTileLeft), 
					MapUtils.getLatitudeFromTile(zoomLevel, yTileDown), 
					MapUtils.getLongitudeFromTile(zoomLevel, xTileRight));
			for (Node o : objects) {
				double tileX = MapUtils.getTileNumberX(zoomLevel, o.getLongitude());
				int x = (int) ((tileX - xTileLeft) * getTileSize());
				double tileY = MapUtils.getTileNumberY(zoomLevel, o.getLatitude());
				int y = (int) ((tileY - yTileUp) * getTileSize());
				points.put(o, new Point(x, y));
			}
		}
		invalidate();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN) {

			if(previousShownToast != null){
				previousShownToast.cancel();
				previousShownToast = null;
			}
			int x = (int) event.getX();
			int y = (int) event.getY();
			
			for(Node n : points.keySet()){
				Point p = points.get(n);
				if(Math.abs(p.x - x) <= radiusClick && Math.abs(p.y - y) <= radiusClick){
					StringBuilder b = new StringBuilder();
					b.append("This is an amenity : \n");
					b.append("type - ").append(n.getTag(OSMTagKey.AMENITY)).append("\n");
					if(n.getTag(OSMTagKey.NAME) != null){
						b.append("name - ").append(n.getTag(OSMTagKey.NAME)).append("\n");
					}
					b.append("id - ").append(n.getId());
					
					previousShownToast = Toast.makeText(getContext(), b.toString(), Toast.LENGTH_SHORT);
					previousShownToast.show();
					// TODO use precision
					log.debug("Precision is " + event.getXPrecision());
					return true;
				}
			}
		} 
		return super.onTouchEvent(event);
	}
	
	public void setCurrentLocationAndZoom(LatLon currentLocation, int zoom) {
		this.currentLocation = currentLocation;
		this.zoomLevel = zoom;
		preparePoints();
	}
	
	public int getTileSize(){
		ITileSource source = OsmandSettings.tileSource;
		return source == null ? 256 : source.getTileSize();
		
	}
	
	public void setNodeManager(DataTileManager<Node> nodeManager) {
		this.nodeManager = nodeManager;
		preparePoints();
	}
	
	public LatLon getCurrentLocation() {
		return currentLocation;
	}
	
	public DataTileManager<Node> getNodeManager() {
		return nodeManager;
	}
	
	
	public int getZoomLevel() {
		return zoomLevel;
	}

}
