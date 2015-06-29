package net.osmand.plus.views;

import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.R;
import net.osmand.plus.activities.TransportRouteHelper;
import net.osmand.plus.resources.TransportIndexRepository.RouteInfoLocation;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.widget.Toast;

public class TransportInfoLayer extends OsmandMapLayer {
	
	private final TransportRouteHelper routeHelper;
	private OsmandMapTileView view;
	private Paint paintInt;
	private Paint paintEnd;
	private boolean visible = true;

	public TransportInfoLayer(TransportRouteHelper routeHelper){
		this.routeHelper = routeHelper;
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		paintInt = new Paint();
		paintInt.setColor(view.getResources().getColor(R.color.transport_int));
		paintInt.setAntiAlias(true);
		paintEnd = new Paint();
		paintEnd.setColor(view.getResources().getColor(R.color.transport_end));
		paintEnd.setAntiAlias(true);
	}
	
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public int getRadius(RotatedTileBox tb){
		final double zoom = tb.getZoom();
		if(zoom <= 16) {
			return (int) (tb.getDensity() * 8);
		}
		return (int) (tb.getDensity() * 10);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {}
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (routeHelper.routeIsCalculated() && visible) {
			List<RouteInfoLocation> list = routeHelper.getRoute();
			for (RouteInfoLocation l : list) {
				if (l == null) {
					// once l is null in list
					continue;
				}
				TransportRoute route = l.getRoute();
				boolean start = false;
				boolean end = false;
				List<TransportStop> stops = l.getDirection() ? route.getForwardStops() : route.getBackwardStops();
				for (int i = 0; i < stops.size() && !end; i++) {
					Paint toShow = paintInt;
					TransportStop st = stops.get(i);
					if (!start) {
						if (st == l.getStart()) {
							start = true;
							toShow = paintEnd;
						}
					} else {
						if (st == l.getStop()) {
							end = true;
							toShow = paintEnd;
						}
					}
					if (start) {
						LatLon location = st.getLocation();
						if (tileBox.containsLatLon(location.getLatitude(), location.getLongitude())) {
							int x = (int) tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
							int y = (int) tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
							canvas.drawRect(x - getRadius(tileBox), y - getRadius(tileBox), x + getRadius(tileBox), y
									+ getRadius(tileBox), toShow);
						}
					}
				}

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
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		if (visible && !routeHelper.getRoute().isEmpty()) {
			for (RouteInfoLocation l : routeHelper.getRoute()) {
				if(l == null){
					// once l is null in list
					continue;
				}
				TransportRoute route = l.getRoute();
				boolean start = false;
				boolean end = false;
				List<TransportStop> stops = l.getDirection() ? route.getForwardStops() : route.getBackwardStops();
				for (int i = 0; i < stops.size() && !end; i++) {
					TransportStop st = stops.get(i);
					if (!start) {
						if (st == l.getStart()) {
							start = true;
						}
					} else {
						if (st == l.getStop()) {
							end = true;
						}
					}
					if (start) {
						LatLon location = st.getLocation();
						int x = (int) tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
						int y = (int) tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
						if (Math.abs(x - ex) < getRadius(tileBox) * 3 /2 && Math.abs(y - ey) < getRadius(tileBox) * 3 /2) {
							AccessibleToast.makeText(view.getContext(), st.getName(view.getSettings().MAP_PREFERRED_LOCALE.get()) + " : " + //$NON-NLS-1$
									route.getType() + " " + route.getRef() //$NON-NLS-1$
							, Toast.LENGTH_LONG).show();
							return true;
						}
					}
				}

			}
		}
		return false;
	}


	
	
}
