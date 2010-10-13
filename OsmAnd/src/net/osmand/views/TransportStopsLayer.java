package net.osmand.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.OsmandSettings;
import net.osmand.R;
import net.osmand.TransportIndexRepository;
import net.osmand.data.TransportStop;
import net.osmand.osm.LatLon;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

public class TransportStopsLayer implements OsmandMapLayer, ContextMenuLayer.IContextMenuProvider {
	private static final int startZoom = 12;
	
	private Paint pointAltUI;
	private OsmandMapTileView view;
	private List<TransportStop> objects = new ArrayList<TransportStop>();
	private DisplayMetrics dm;
	
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		pointAltUI = new Paint();
		pointAltUI.setColor(Color.rgb(0, 0, 255));
		pointAltUI.setAlpha(150);
		pointAltUI.setAntiAlias(true);
	}
	
	public TransportStop getFromPoint(PointF point){
		TransportStop result = null;
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int radius = getRadiusPoi(view.getZoom()) * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					TransportStop n = objects.get(i);
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
		TransportStop n = getFromPoint(point);
		if(n != null){
			Toast.makeText(view.getContext(), getStopDescription(n, true), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	private String getStopDescription(TransportStop n, boolean useName) {
		StringBuilder text = new StringBuilder(250);
		text.append(view.getContext().getString(R.string.transport_Stop)).append(" : ").append(n.getName(OsmandSettings.usingEnglishNames(view.getSettings()))); //$NON-NLS-1$
		text.append("\n").append(view.getContext().getString(R.string.transport_Routes)).append(" : ");  //$NON-NLS-1$ //$NON-NLS-2$
		List<TransportIndexRepository> reps = view.getApplication().getResourceManager().searchTransportRepositories(n.getLocation().getLatitude(), n.getLocation().getLongitude());
		if(!reps.isEmpty()){
			List<String> l;
			if(!useName){
				l = reps.get(0).getRouteDescriptionsForStop(n, "{1} {0}"); //$NON-NLS-1$
			} else if(OsmandSettings.usingEnglishNames(view.getSettings())){
				 l = reps.get(0).getRouteDescriptionsForStop(n, "{1} {0} - {3}"); //$NON-NLS-1$
			} else {
				l = reps.get(0).getRouteDescriptionsForStop(n, "{1} {0} - {2}"); //$NON-NLS-1$
			}
			for(String s : l){
				text.append("\n").append(s); //$NON-NLS-1$
			}
		}
		return text.toString();
	}
	
	public int getRadiusPoi(int zoom){
		if(zoom < startZoom){
			return 0;
		} else if(zoom <= 15){
			return 8;
		} else if(zoom == 16){
			return 10;
		} else if(zoom == 17){
			return 14;
		} else {
			return 18;
		}
	}

	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds) {
		if (view.getZoom() >= startZoom) {
			objects.clear();
			view.getApplication().getResourceManager().searchTransportAsync(latLonBounds.top, latLonBounds.left, latLonBounds.bottom, latLonBounds.right, view.getZoom(), objects);
			int r = 3 * getRadiusPoi(view.getZoom()) / 4;
			for (TransportStop o : objects) {
				int x = view.getMapXForPoint(o.getLocation().getLongitude());
				int y = view.getMapYForPoint(o.getLocation().getLatitude());
				canvas.drawRect(x - r, y - r, x + r, y + r, pointAltUI);
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
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public OnClickListener getActionListener(List<String> actionsList, Object o) {
		return null;
	}


	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof TransportStop){
			return getStopDescription((TransportStop) o, false);
		}
		return null;
	}

	@Override
	public Object getPointObject(PointF point) {
		return getFromPoint(point);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof TransportStop){
			return ((TransportStop)o).getLocation();
		}
		return null;
	}



}
