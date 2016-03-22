package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.plus.R;
import net.osmand.plus.resources.TransportIndexRepository;

import java.util.ArrayList;
import java.util.List;

public class TransportStopsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	private static final int startZoom = 12;
	
	private OsmandMapTileView view;
	private List<TransportStop> objects = new ArrayList<>();

	private Paint paintIcon;
	private Bitmap stopBus;
	private Bitmap stopTram;
	private Bitmap stopSmall;


	@SuppressWarnings("deprecation")
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		paintIcon = new Paint();
		stopBus = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_transport_stop_bus);
		stopTram = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_transport_stop_tram);
		stopSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_transport_stop_small);
	}
	
	public void getFromPoint(RotatedTileBox tb,PointF point, List<? super TransportStop> res) {
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			final int rp = getRadiusPoi(tb);
			int radius = rp * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					TransportStop n = objects.get(i);
					if (n.getLocation() == null){
						continue;
					}
					int x = (int) tb.getPixXFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					int y = (int) tb.getPixYFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						radius = rp;
						res.add(n);
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
	}
	



	private String getStopDescription(TransportStop n, boolean useName) {
		StringBuilder text = new StringBuilder(250);
		text.append(view.getContext().getString(R.string.transport_Stop))
				.append(" : ").append(n.getName(view.getSettings().MAP_PREFERRED_LOCALE.get())); //$NON-NLS-1$
		text.append("\n").append(view.getContext().getString(R.string.transport_Routes)).append(" : "); //$NON-NLS-1$ //$NON-NLS-2$
		List<TransportIndexRepository> reps = view.getApplication().getResourceManager().searchTransportRepositories(
				n.getLocation().getLatitude(), n.getLocation().getLongitude());

		for (TransportIndexRepository t : reps) {
			if (t.acceptTransportStop(n)) {
				List<String> l;
				if (!useName) {
					l = t.getRouteDescriptionsForStop(n, "{1} {0}"); //$NON-NLS-1$
				} else if (view.getSettings().usingEnglishNames()) {
					l = t.getRouteDescriptionsForStop(n, "{1} {0} - {3}"); //$NON-NLS-1$
				} else {
					l = t.getRouteDescriptionsForStop(n, "{1} {0} - {2}"); //$NON-NLS-1$
				}
				if (l != null) {
					for (String s : l) {
						text.append("\n").append(s); //$NON-NLS-1$
					}
				}
			}
		}
		return text.toString();
	}
	
	public int getRadiusPoi(RotatedTileBox tb){
		final double zoom = tb.getZoom();
		int r;
		if(zoom < startZoom){
			r = 0;
		} else if(zoom <= 15){
			r = 8;
		} else if(zoom <= 16){
			r = 10;
		} else if(zoom <= 17){
			r = 14;
		} else {
			r = 18;
		}
		return (int) (r * tb.getDensity());
	}

	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox,
			DrawSettings settings) {
		if (tileBox.getZoom() >= startZoom) {
			objects.clear();

			float iconSize = stopBus.getWidth() * 3 / 2.5f;
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

			final QuadRect latLonBounds = tileBox.getLatLonBounds();
			view.getApplication().getResourceManager().searchTransportAsync(latLonBounds.top, latLonBounds.left,
					latLonBounds.bottom, latLonBounds.right, tileBox.getZoom(), objects);
			List<TransportStop> fullObjects = new ArrayList<>();
			for (TransportStop o : objects) {
				float x = tileBox.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				float y = tileBox.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());

				if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
					canvas.drawBitmap(stopSmall, x - stopSmall.getWidth() / 2, y - stopSmall.getHeight() / 2, paintIcon);
				} else {
					fullObjects.add(o);
				}
			}
			for (TransportStop o : fullObjects) {
				float x = tileBox.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				float y = tileBox.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				Bitmap b = stopBus;
				canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, paintIcon);
			}
		}
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof TransportStop){
			return getStopDescription((TransportStop) o, false);
		}
		return null;
	}
	
	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof TransportStop){
			return new PointDescription(PointDescription.POINT_TYPE_POI, view.getContext().getString(R.string.transport_Stop),
					((TransportStop)o).getName()); 
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res) {
		if (tileBox.getZoom() >= startZoom) {
			getFromPoint(tileBox, point, res);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof TransportStop){
			return ((TransportStop)o).getLocation();
		}
		return null;
	}
}
