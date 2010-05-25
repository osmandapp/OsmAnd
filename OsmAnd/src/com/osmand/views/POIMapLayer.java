package com.osmand.views;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.widget.Toast;

import com.osmand.OsmSQLLiteRepository;
import com.osmand.ResourceManager;
import com.osmand.data.Amenity;
import com.osmand.data.DataTileManager;
import com.osmand.osm.MapUtils;
import com.osmand.osm.io.OsmLuceneRepository;

public class POIMapLayer implements OsmandMapLayer {
	private static final int radiusClick = 2; // for 15 level zoom
	
	private DataTileManager<Amenity> nodeManager = null;
	private Paint pointUI;
	private Paint pointAltUI;
	private OsmandMapTileView view;
	private List<Amenity> objects;
	
	

	// TODO optimize all evaluations
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN && objects != null) {
			double tileNumberX = MapUtils.getTileNumberX(view.getZoom(), view.getLongitude());
			double tileNumberY = MapUtils.getTileNumberY(view.getZoom(), view.getLatitude());
			double xTileLeft = tileNumberX - view.getWidth() / (2d * getTileSize());
			double yTileUp = tileNumberY - view.getHeight() / (2d * getTileSize());
			int ex = (int) event.getX();
			int ey = (int) event.getY();
			int radius = getRadiusPoi(view.getZoom()) * 3 / 2;
			for(Amenity n : objects){
				double tileX = MapUtils.getTileNumberX(view.getZoom(), n.getLocation().getLongitude());
				int x = (int) ((tileX - xTileLeft) * getTileSize());
				double tileY = MapUtils.getTileNumberY(view.getZoom(), n.getLocation().getLatitude());
				int y = (int) ((tileY - yTileUp) * getTileSize());
				if(Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius){
					Toast.makeText(view.getContext(), n.getSimpleFormat(), Toast.LENGTH_SHORT).show();
					return true;
				}
			}
		} 
//		return super.onTouchEvent(event);
		return false;
	}
	
	public int getTileSize(){
		return view.getTileSize();
		
	}
	
	public void setNodeManager(DataTileManager<Amenity> nodeManager) {
		this.nodeManager = nodeManager;
		if(view != null){
			view.prepareImage();
		}
	}
	
	public DataTileManager<Amenity> getNodeManager() {
		return nodeManager;
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		pointUI = new Paint();
		pointUI.setColor(Color.BLUE);
		pointUI.setAlpha(150);
		pointUI.setAntiAlias(true);
		
		pointAltUI = new Paint();
		pointAltUI.setColor(Color.GREEN);
		pointAltUI.setAlpha(150);
		pointAltUI.setAntiAlias(true);
	}
	
	public int getRadiusPoi(int zoom){
		if(zoom < 15){
			return 0;
		} else {
			return radiusClick << (zoom - 15);
		} 
	}

	@Override
	public void onDraw(Canvas canvas) {
		
		if (view.getZoom() >= 15) {
			double tileNumberX = MapUtils.getTileNumberX(view.getZoom(), view.getLongitude());
			double tileNumberY = MapUtils.getTileNumberY(view.getZoom(), view.getLatitude());
			double xTileLeft = tileNumberX - view.getWidth() / (2d * getTileSize());
			double xTileRight = tileNumberX + view.getWidth() / (2d * getTileSize());
			double yTileUp = tileNumberY - view.getHeight() / (2d * getTileSize());
			double yTileDown = tileNumberY + view.getHeight() / (2d * getTileSize());
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), yTileUp);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), xTileLeft);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), yTileDown);
			double rightLongitude= MapUtils.getLongitudeFromTile(view.getZoom(), xTileRight);
			
			OsmLuceneRepository searcher = ResourceManager.getResourceManager().getAmenityIndexSearcher();
			if (searcher != null) {
				objects = searcher.searchAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
				for (Amenity o : objects) {
					double tileX = MapUtils.getTileNumberX(view.getZoom(), o.getLocation().getLongitude());
					int x = (int) ((tileX - xTileLeft) * getTileSize());
					double tileY = MapUtils.getTileNumberY(view.getZoom(), o.getLocation().getLatitude());
					int y = (int) ((tileY - yTileUp) * getTileSize());
					canvas.drawCircle(x, y, getRadiusPoi(view.getZoom()), pointAltUI);
				}
			} 
			
			OsmSQLLiteRepository sqlLite = ResourceManager.getResourceManager().getAmenityRepository();
			if (sqlLite != null) {
				objects = sqlLite.searchAmenities(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
				for (Amenity o : objects) {
					double tileX = MapUtils.getTileNumberX(view.getZoom(), o.getLocation().getLongitude());
					int x = (int) ((tileX - xTileLeft) * getTileSize());
					double tileY = MapUtils.getTileNumberY(view.getZoom(), o.getLocation().getLatitude());
					int y = (int) ((tileY - yTileUp) * getTileSize());
					canvas.drawCircle(x, y, getRadiusPoi(view.getZoom()), pointAltUI);
				}
			}
			if (nodeManager != null) {
				List<Amenity> objects = nodeManager.getObjects(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
				for (Amenity o : objects) {
					double tileX = MapUtils.getTileNumberX(view.getZoom(), o.getLocation().getLongitude());
					int x = (int) ((tileX - xTileLeft) * getTileSize());
					double tileY = MapUtils.getTileNumberY(view.getZoom(), o.getLocation().getLatitude());
					int y = (int) ((tileY - yTileUp) * getTileSize());
					canvas.drawCircle(x, y, getRadiusPoi(view.getZoom()), pointAltUI);
				}
			}
			
		}
	}

	@Override
	public void destroyLayer() {
		
	}

}
