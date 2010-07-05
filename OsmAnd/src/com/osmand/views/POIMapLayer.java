package com.osmand.views;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.Toast;

import com.osmand.OsmandSettings;
import com.osmand.PoiFilter;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.activities.EditingPOIActivity;
import com.osmand.data.Amenity;
import com.osmand.osm.MapUtils;

public class POIMapLayer implements OsmandMapLayer {
	// it is very slow to use with 15 level
	private static final int startZoom = 10;
	public static final int LIMIT_POI = 200;
	
	
	private Paint pointAltUI;
	private OsmandMapTileView view;
	private List<Amenity> objects = new ArrayList<Amenity>();

	private ResourceManager resourceManager;
	private PoiFilter filter;
	
	@Override
	public boolean onLongPressEvent(PointF point) {
		final Amenity n = getAmenityFromPoint(point);
		if(n != null){
			Context ctx = view.getContext();
			Builder builder = new AlertDialog.Builder(ctx);
			final EditingPOIActivity edit = new EditingPOIActivity(ctx, view);
			builder.setItems(new String[]{
					this.view.getResources().getString(R.string.poi_context_menu_modify),
					this.view.getResources().getString(R.string.poi_context_menu_delete)
					}, new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(which == 0){
						edit.showEditDialog(n);
					} else {
						edit.showDeleteDialog(n);
					}
					
				}
				
			});
			builder.show();
			return true;
		}
		return false;
	}
	
	public PoiFilter getFilter() {
		return filter;
	}
	
	public void setFilter(PoiFilter filter) {
		this.filter = filter;
	}
	
	public Amenity getAmenityFromPoint(PointF point){
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int radius = getRadiusPoi(view.getZoom()) * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					Amenity n = objects.get(i);
					int x = view.getRotatedMapXForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					int y = view.getRotatedMapYForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						return n;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
		return null;
	}
	

	@Override
	public boolean onTouchEvent(PointF point) {
		Amenity n = getAmenityFromPoint(point);
		if(n != null){
			String format = n.getSimpleFormat(OsmandSettings.usingEnglishNames(view.getContext()));
			if(n.getOpeningHours() != null){
				format += "\n" + view.getContext().getString(R.string.opening_hours) +" : "+ n.getOpeningHours(); //$NON-NLS-1$ //$NON-NLS-2$
			}
			Toast.makeText(view.getContext(), format, Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}
	
	public int getTileSize(){
		return view.getTileSize();
		
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		pointAltUI = new Paint();
		pointAltUI.setColor(Color.rgb(255, 128, 0));
		pointAltUI.setAlpha(200);
		pointAltUI.setAntiAlias(true);
		resourceManager = ResourceManager.getResourceManager();
		pixRect.set(0, 0, view.getWidth(), view.getHeight());
	}
	
	public int getRadiusPoi(int zoom){
		if(zoom < startZoom){
			return 0;
		} else if(zoom <= 15){
			return 7;
		} else if(zoom == 16){
			return 10;
		} else if(zoom == 17){
			return 14;
		} else {
			return 18;
		}
	}

	Rect pixRect = new Rect();
	RectF tileRect = new RectF();
	
	@Override
	public void onDraw(Canvas canvas) {
		if (view.getZoom() >= startZoom) {
			pixRect.set(0, 0, view.getWidth(), view.getHeight());
			view.calculateTileRectangle(pixRect, view.getCenterPointX(), 
					view.getCenterPointY(), view.getXTile(), view.getYTile(), tileRect);
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);

			objects.clear();
			resourceManager.searchAmenitiesAsync(topLatitude, leftLongitude, bottomLatitude, rightLongitude, view.getZoom(), filter, objects);
			for (Amenity o : objects) {
				int x = view.getMapXForPoint(o.getLocation().getLongitude());
				int y = view.getMapYForPoint(o.getLocation().getLatitude());
				canvas.drawCircle(x, y, getRadiusPoi(view.getZoom()), pointAltUI);
			}

		}
	}

	@Override
	public void destroyLayer() {
		
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

}
