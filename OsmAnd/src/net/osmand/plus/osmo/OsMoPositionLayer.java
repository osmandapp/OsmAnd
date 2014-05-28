package net.osmand.plus.osmo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.osmand.Location;
import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmo.OsMoGroups.OsMoGroupsUIListener;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoGroup;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Class represents a layer for osmo positions
 *
 */
public class OsMoPositionLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider, OsMoGroupsUIListener  {

	private DisplayMetrics dm;
	private final MapActivity map;
	private OsmandMapTileView view;
	private Paint pointAltUI;
	private Paint point;
	private OsMoPlugin plugin;
	private final static float startZoom = 7;
	private Handler uiHandler;

	public OsMoPositionLayer(MapActivity map, OsMoPlugin plugin) {
		this.map = map;
		this.plugin = plugin;
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		uiHandler = new Handler();
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		pointAltUI = new Paint();
		pointAltUI.setColor(view.getApplication().getResources().getColor(R.color.poi_background));
		pointAltUI.setStyle(Style.FILL);
		
		point = new Paint();
		point.setColor(Color.DKGRAY);
		point.setAntiAlias(true);
		point.setStyle(Style.FILL_AND_STROKE);
	}
	
	public Collection<OsMoDevice> getTrackingDevices() {
		return plugin.getTracker().getTrackingDevices();
	}
	
	public int getRadiusPoi(RotatedTileBox tb){
		int r = 0;
		final float zoom = tb.getZoom() + tb.getZoomScale();
		if(zoom < startZoom){
			r = 0;
		} else if(zoom <= 15){
			r = 10;
		} else if(zoom <= 16){
			r = 14;
		} else if(zoom <= 17){
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {
		final int r = getRadiusPoi(tb) * 3 / 4;
		for (OsMoDevice t : getTrackingDevices()) {
			Location l = t.getLastLocation();
			if (l != null) {
				int x = (int) tb.getPixXFromLatLon(l.getLatitude(), l.getLongitude());
				int y = (int) tb.getPixYFromLatLon(l.getLatitude(), l.getLongitude());
				
				pointAltUI.setColor(t.getColor());
				canvas.drawCircle(x, y, r , point);
				canvas.drawCircle(x, y, r - 2, pointAltUI);
			}
		}
	}
	
	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		List<OsMoDevice> pos = new ArrayList<OsMoDevice>();
		getOsmoFromPoint(tileBox, point, pos);
		if (!pos.isEmpty()) {
			StringBuilder res = new StringBuilder();
			for (OsMoDevice d : pos) {
				res.append(getObjectDescription(d));
			}
			AccessibleToast.makeText(view.getContext(), res.toString(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		getOsmoFromPoint(tileBox, point, o);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
        if(o instanceof OsMoDevice) {
        	Location loc = ((OsMoDevice) o).getLastLocation();
        	if(loc != null) {
        		return new LatLon(loc.getLatitude(), loc.getLongitude());
        	}
        }
		return null;
	}
	
	@Override
	public String getObjectDescription(Object o) {
		if (o instanceof OsMoDevice) {
			String d = map.getString(R.string.osmo_user_name) + " " + ((OsMoDevice) o).getVisibleName();
			final Location l = ((OsMoDevice) o).getLastLocation();
			if(l != null && l.hasSpeed()) {
				d += "\n"+ OsmAndFormatter.getFormattedSpeed(l.getSpeed(), map.getMyApplication());
			}
			return d;
		}
		return null;
	}

	@Override
	public String getObjectName(Object o) {
		if(o instanceof OsMoDevice) {
			return map.getString(R.string.osmo_user_name) + " " + ((OsMoDevice) o).getVisibleName();
		}
		return null;
	}
	
	public void refresh() {
		if (view != null) {
			view.refreshMap();
		}
	}
	
	
	private void getOsmoFromPoint(RotatedTileBox tb, PointF point, List<? super OsMoDevice> points) {
		if (view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			final int rp = getRadiusPoi(tb);
			int compare = rp;
			int radius = rp * 3 / 2;
			for (OsMoDevice d : getTrackingDevices()) {
				Location position = d.getLastLocation();
				if (position != null) {
					int x = (int) tb.getPixXFromLatLon(position.getLatitude(), position.getLongitude());
					int y = (int) tb.getPixYFromLatLon(position.getLatitude(), position.getLongitude());
					// the width of an image is 40 px, the height is 60 px -> radius = 20,
					// the position of a parking point relatively to the icon is at the center of the bottom line of the
					// image
					if (Math.abs(x - ex) <= compare && Math.abs(y - ey) <= compare) {
						compare = radius;
						points.add(d);
					}
				}
			}
		}
	}

	@Override
	public void groupsListChange(String operation, OsMoGroup group) {
	}

	private volatile boolean schedule = false;
	// store between rotations
	private static String followTrackerId;
	private static LatLon followMapLocation;
	private static String followDestinationId;
	
	public static void setFollowTrackerId(OsMoDevice d) {
		if(d != null) {
			followTrackerId = d.trackerId;
			Location l = d.getLastLocation();
			if(l != null) {
				followMapLocation = new LatLon(l.getLatitude(), l.getLongitude());
			}
		} else {
			followTrackerId = null;
		}
		
	}
	
	public static void setFollowDestination(OsMoDevice followDestination) {
		followDestinationId = followDestination == null ? null : followDestination.trackerId;
	}
	
	public static String getFollowDestinationId() {
		return followDestinationId;
	}
	
	@Override
	public void deviceLocationChanged(final OsMoDevice device) {
		boolean sameId = Algorithms.objectEquals(followTrackerId, device.trackerId);
		boolean sameDestId = Algorithms.objectEquals(followDestinationId, device.trackerId);
		Location l = device.getLastLocation();
		if(sameDestId && l != null) {
			TargetPointsHelper targets = map.getMyApplication().getTargetPointsHelper();
			RoutingHelper rh = map.getMyApplication().getRoutingHelper();
			double dist = 0.1;
			if(rh.isRouteBeingCalculated()) {
				dist = 100;
			} else if(rh.isRouteCalculated()) {
				dist = 30;
			}
			LatLon lt = new LatLon(l.getLatitude(), l.getLongitude());
			if(targets.getPointToNavigate() == null || MapUtils.getDistance(targets.getPointToNavigate(), lt) > dist) {
				targets.navigateToPoint(lt, true, -1);
			}
		}
		if(sameId && !schedule  && l != null) {
			schedule = true;
			ContextMenuLayer cl = map.getMapLayers().getContextMenuLayer();
			final boolean sameObject = Algorithms.objectEquals(device, cl.getFirstSelectedObject()) && cl.isVisible();
			LatLon mapLoc = new LatLon(map.getMapView().getLatitude(), map.getMapView().getLongitude());
			final boolean centered = Algorithms.objectEquals(followMapLocation, mapLoc);
			if(sameObject || centered) {
				if(centered ) {
					followMapLocation = new LatLon(l.getLatitude(), l.getLongitude());
				} else if(!map.getMapView().getAnimatedDraggingThread().isAnimating()) {
					// disable tracking
					followMapLocation = null;
				}
				uiHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						schedule = false;
						if (sameObject) {
							ContextMenuLayer cl = map.getMapLayers().getContextMenuLayer();
							Location l = device.getLastLocation();
							cl.setLocation(new LatLon(l.getLatitude(), l.getLongitude()), getObjectDescription(device));
							cl.setSelectedObject(device);
						}
						if (centered) {
							map.getMapView().setLatLon(followMapLocation.getLatitude(),
									followMapLocation.getLongitude());
						}
						map.getMapView().refreshMap();
					}

				}, 150);
			} else {
				followTrackerId = null;
			}
		}		
	}
	
	
}
