package net.osmand.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.OsmandSettings;
import net.osmand.PoiFilter;
import net.osmand.R;
import net.osmand.ResourceManager;
import net.osmand.activities.EditingPOIActivity;
import net.osmand.data.Amenity;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

public class POIMapLayer implements OsmandMapLayer, ContextMenuLayer.IContextMenuProvider {
	// it is very slow to use with 15 level
	private static final int startZoom = 10;
	public static final int LIMIT_POI = 200;
	
	
	private Paint pointAltUI;
	private OsmandMapTileView view;
	private List<Amenity> objects = new ArrayList<Amenity>();

	private ResourceManager resourceManager;
	private PoiFilter filter;
	private DisplayMetrics dm;
	
	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}
	
	public PoiFilter getFilter() {
		return filter;
	}
	
	public void setFilter(PoiFilter filter) {
		this.filter = filter;
	}
	
	public Amenity getAmenityFromPoint(PointF point){
		Amenity result = null;
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
						radius = Math.max(Math.abs(x - ex), Math.abs(y - ey));
						result = n;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
		return result;
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
	
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		pointAltUI = new Paint();
		pointAltUI.setColor(Color.rgb(255, 128, 0));
		pointAltUI.setAlpha(200);
		pointAltUI.setAntiAlias(true);
		resourceManager = view.getApplication().getResourceManager();
		pixRect.set(0, 0, view.getWidth(), view.getHeight());
	}
	
	public int getRadiusPoi(int zoom){
		int r = 0;
		if(zoom < startZoom){
			r = 0;
		} else if(zoom <= 15){
			r = 10;
		} else if(zoom == 16){
			r = 14;
		} else if(zoom == 17){
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * dm.density);
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

	@Override
	public OnClickListener getActionListener(List<String> actionsList, Object o) {
		final Amenity a = (Amenity) o;
		actionsList.add(this.view.getResources().getString(R.string.poi_context_menu_modify));
		actionsList.add(this.view.getResources().getString(R.string.poi_context_menu_delete));
		final EditingPOIActivity edit = new EditingPOIActivity(view.getContext(), view.getApplication(), view);
		return new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					edit.showEditDialog(a);
				} else {
					edit.showDeleteDialog(a);
				}
			}
		};
	}

	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof Amenity){
			return ((Amenity)o).getSimpleFormat(OsmandSettings.usingEnglishNames(view.getContext()));
		}
		return null;
	}

	@Override
	public Object getPointObject(PointF point) {
		return getAmenityFromPoint(point);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof Amenity){
			return ((Amenity)o).getLocation();
		}
		return null;
	}

}
