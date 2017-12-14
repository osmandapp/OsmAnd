package net.osmand.plus.osmo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class represents a layer for osmo positions
 *
 */
public class OsMoPositionLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider, OsMoGroupsUIListener,
		ContextMenuLayer.IContextMenuProviderSelection{
 
	private static int POINT_OUTER_COLOR = 0x88555555;
	private static int PAINT_TEXT_ICON_COLOR = Color.BLACK;
	
	private DisplayMetrics dm;
	private final MapActivity map;
	private OsmandMapTileView view;
	private Paint pointInnerCircle;
	private Paint pointOuter;
	private OsMoPlugin plugin;
	private final static float startZoom = 7;
	private Handler uiHandler;
	private Paint paintPath;
	private Path pth;
	private Paint paintTextIcon;

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

		pointInnerCircle = new Paint();
		pointInnerCircle.setColor(view.getApplication().getResources().getColor(R.color.poi_background));
		pointInnerCircle.setStyle(Style.FILL);
		pointInnerCircle.setAntiAlias(true);
		
		paintPath = new Paint();
		paintPath.setStyle(Style.STROKE);
		paintPath.setStrokeWidth(14);
		paintPath.setAntiAlias(true);
		paintPath.setStrokeCap(Cap.ROUND);
		paintPath.setStrokeJoin(Join.ROUND);
		
		paintTextIcon = new Paint();
		paintTextIcon.setTextSize(10 * view.getDensity());
		paintTextIcon.setTextAlign(Align.CENTER);
		paintTextIcon.setFakeBoldText(true);
		paintTextIcon.setColor(PAINT_TEXT_ICON_COLOR);
		paintTextIcon.setAntiAlias(true);
		
		pth = new Path();

		pointOuter = new Paint();
		pointOuter.setColor(POINT_OUTER_COLOR);
		pointOuter.setAntiAlias(true);
		pointOuter.setStyle(Style.FILL_AND_STROKE);
	}
	
	public Collection<OsMoDevice> getTrackingDevices() {
		return plugin.getTracker().getTrackingDevices();
	}
	
	public int getRadiusPoi(RotatedTileBox tb){
		int r = 0;
		final double zoom = tb.getZoom();
		if(zoom < startZoom){
			r = 0;
		} else if(zoom <= 11){
			r = 10;
		} else if(zoom <= 14){
			r = 12;
		} else {
			r = 14;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		final int r = getRadiusPoi(tileBox);
		long treshold = System.currentTimeMillis() - 15000;
		for (OsMoDevice t : getTrackingDevices()) {
			Location l = t.getLastLocation();
			ConcurrentLinkedQueue<Location> plocations = t.getPreviousLocations(treshold);
			if (!plocations.isEmpty() && l != null) {
				int x = (int) tileBox.getPixXFromLonNoRot(l.getLongitude());
				int y = (int) tileBox.getPixYFromLatNoRot(l.getLatitude());
				pth.rewind();
				Iterator<Location> it = plocations.iterator();
				boolean f = true;
				while (it.hasNext()) {
					Location lo = it.next();
					int xt = (int) tileBox.getPixXFromLonNoRot(lo.getLongitude());
					int yt = (int) tileBox.getPixYFromLatNoRot(lo.getLatitude());
					if (f) {
						f = false;
						pth.moveTo(xt, yt);
					} else {
						pth.lineTo(xt, yt);
					}
				}
				pth.lineTo(x, y);
				paintPath.setColor(t.getColor());
				canvas.drawPath(pth, paintPath);
			}
		}
		canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		for (OsMoDevice t : getTrackingDevices()) {
			Location l = t.getLastLocation();
			if (l != null) {
				int x = (int) tileBox.getPixXFromLatLon(l.getLatitude(), l.getLongitude());
				int y = (int) tileBox.getPixYFromLatLon(l.getLatitude(), l.getLongitude());
				pointInnerCircle.setColor(t.getColor());
				pointOuter.setColor(POINT_OUTER_COLOR);
				paintTextIcon.setColor(PAINT_TEXT_ICON_COLOR);
				Location lastLocation = t.getLastLocation();
				if (lastLocation != null) {
					long now = System.currentTimeMillis();
					boolean recent = Math.abs( now - lastLocation.getTime() ) < OsMoGroupsActivity.RECENT_THRESHOLD;
					if (!recent) {
						int color = t.getNonActiveColor();
						pointInnerCircle.setColor(color);
						pointOuter.setColor(Color.argb(Color.alpha(color),
								Color.red(POINT_OUTER_COLOR), Color.green(POINT_OUTER_COLOR),
								Color.blue(POINT_OUTER_COLOR)));
						paintTextIcon.setColor(Color.argb(Color.alpha(color),
								Color.red(PAINT_TEXT_ICON_COLOR), Color.green(PAINT_TEXT_ICON_COLOR),
								Color.blue(PAINT_TEXT_ICON_COLOR)));
					}
				} else {
					int color = t.getNonActiveColor();
					pointInnerCircle.setColor(color);
					pointOuter.setColor(Color.argb(Color.alpha(color),
							Color.red(POINT_OUTER_COLOR), Color.green(POINT_OUTER_COLOR),
							Color.blue(POINT_OUTER_COLOR)));
					paintTextIcon.setColor(Color.argb(Color.alpha(color),
							Color.red(PAINT_TEXT_ICON_COLOR), Color.green(PAINT_TEXT_ICON_COLOR),
							Color.blue(PAINT_TEXT_ICON_COLOR)));
				}
				canvas.drawCircle(x, y, r + (float)Math.ceil(tileBox.getDensity()), pointOuter);
				canvas.drawCircle(x, y, r - (float)Math.ceil(tileBox.getDensity()), pointInnerCircle);
				paintTextIcon.setTextSize(r * 3 / 2);
				canvas.drawText(t.getVisibleName().substring(0, 1).toUpperCase(), x, y + r / 2, paintTextIcon);
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
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return o instanceof OsMoDevice;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
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
	public PointDescription getObjectName(Object o) {
		if(o instanceof OsMoDevice) {
			return new PointDescription(PointDescription.POINT_TYPE_MARKER, map.getString(R.string.osmo_user_name) + " " + ((OsMoDevice) o).getVisibleName());
		} else {
			return null;
		}
		//String desc = getObjectDescription(o);
		//return desc == null ? null : new PointDescription(PointDescription.POINT_TYPE_MARKER, desc);
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
	private static LatLon followTargetLocation;
	
	public static void setFollowTrackerId(OsMoDevice d, Location l) {
		if(d != null) {
			followTrackerId = d.trackerId;
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
		boolean sameDestId = Algorithms.objectEquals(followDestinationId, device.trackerId);
		Location l = device.getLastLocation();
		if(sameDestId && l != null) {
			TargetPointsHelper targets = map.getMyApplication().getTargetPointsHelper();
			final TargetPoint pn = targets.getPointToNavigate();
			LatLon lt = new LatLon(l.getLatitude(), l.getLongitude());
			boolean cancelDestinationId = false;
			if(followTargetLocation != null ) {
				if(pn == null || pn.point == null || MapUtils.getDistance(pn.point, followTargetLocation) > 10) {
					cancelDestinationId = true;
				}
			}
			if(cancelDestinationId) {
				followTargetLocation = null;
				followDestinationId = null;
			} else {
				RoutingHelper rh = map.getMyApplication().getRoutingHelper();
				double dist = 1;
				if (rh.isRouteBeingCalculated()) {
					dist = 100;
				} else if (rh.isRouteCalculated()) {
					dist = 30;
				}
				if (pn == null || MapUtils.getDistance(pn.point, lt) > dist) {
					followTargetLocation = lt;
					targets.navigateToPoint(lt, true, -1);
				}
			}
		}
		
		boolean sameId = Algorithms.objectEquals(followTrackerId, device.trackerId);
		if(sameId && !schedule && l != null) {
			ContextMenuLayer cl = map.getMapLayers().getContextMenuLayer();
			final boolean sameObject;
			if (map.getContextMenu().getObject() instanceof OsMoDevice && cl.isVisible()) {
				sameObject = Algorithms.objectEquals(device.trackerId,
						((OsMoDevice) map.getContextMenu().getObject()).trackerId);
			} else {
				sameObject = false;
			}
			LatLon mapLoc = new LatLon(map.getMapView().getLatitude(), map.getMapView().getLongitude());
			final boolean centered = followMapLocation != null && MapUtils.getDistance(mapLoc, followMapLocation) < 1;
			if(sameObject || centered) {
				final LatLon loc;
				if(centered ) {
					loc = new LatLon(l.getLatitude(), l.getLongitude());
				} else if(!map.getMapView().getAnimatedDraggingThread().isAnimating()) {
					// disable tracking
					loc = null;
				} else {
					loc = followMapLocation;
				}
				followMapLocation = loc;
				schedule = true;
				uiHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						schedule = false;
						if (sameObject) {
							Location l = device.getLastLocation();
							if (centered) {
								map.getContextMenu().updateMapCenter(loc);
							}
							map.getContextMenu().update(new LatLon(l.getLatitude(), l.getLongitude()),
									getObjectName(device), device);
						}
						if (centered) {
							map.getMapView().setLatLon(loc.getLatitude(), loc.getLongitude());
						}
						map.getMapView().refreshMap();
					}

				}, 150);
			} else {
				followTrackerId = null;
			}
		}		
		uiHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				map.getMapView().refreshMap();
			}

		}, 350);
	}

	@Override
	public int getOrder(Object o) {
		return 0;
	}

	@Override
	public void setSelectedObject(Object o) {
		if(o instanceof OsMoDevice) {
			followTrackerId = ((OsMoDevice) o).getTrackerId();
		}
	}

	@Override
	public void clearSelectedObject() {
		LatLon mapLoc = new LatLon(map.getMapView().getLatitude(), map.getMapView().getLongitude());
		final boolean centered = Algorithms.objectEquals(followMapLocation, mapLoc);
		if(!centered && followTrackerId != null) {
			followTrackerId = null;
		}
	}

	
}
