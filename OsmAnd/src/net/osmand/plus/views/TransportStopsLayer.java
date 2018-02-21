package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import net.osmand.ResultMatcher;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.plus.R;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.transport.TransportStopType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import gnu.trove.list.array.TIntArrayList;

public class TransportStopsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	private static final int startZoom = 12;
	private static final int startZoomRoute = 10;

	private final MapActivity mapActivity;
	private OsmandMapTileView view;

	private Paint paintIcon;
	private Bitmap stopBus;
	private Bitmap stopSmall;
	private RenderingLineAttributes attrs;

	private MapLayerData<List<TransportStop>> data;
	private TransportStopRoute stopRoute = null;

	private boolean showTransportStops;
	private Path path;

	public TransportStopsLayer(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		paintIcon = new Paint();
		path = new Path();
		stopBus = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_transport_stop_bus);
		stopSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_transport_stop_small);
		attrs = new RenderingLineAttributes("transport_route");
		attrs.defaultWidth = (int) (6 * view.getDensity());
		data = new OsmandMapLayer.MapLayerData<List<TransportStop>>() {
			{
				ZOOM_THRESHOLD = 0;
			}

			@Override
			public boolean isInterrupted() {
				return super.isInterrupted();
			}

			@Override
			public void layerOnPostExecute() {
				view.refreshMap();
			}

			@Override
			protected List<TransportStop> calculateResult(RotatedTileBox tileBox) {
				QuadRect latLonBounds = tileBox.getLatLonBounds();
				if (latLonBounds == null) {
					return new ArrayList<>();
				}
				List<TransportStop> res = view.getApplication().getResourceManager().searchTransportSync(latLonBounds.top, latLonBounds.left,
						latLonBounds.bottom, latLonBounds.right, new ResultMatcher<TransportStop>() {

							@Override
							public boolean publish(TransportStop object) {
								return true;
							}

							@Override
							public boolean isCancelled() {
								return isInterrupted();
							}
						});
				Collections.sort(res, new Comparator<TransportStop>() {
					@Override
					public int compare(TransportStop lhs, TransportStop rhs) {
						return lhs.getId() < rhs.getId() ? -1 : (lhs.getId().longValue() == rhs.getId().longValue() ? 0 : 1);
					}
				});

				return res;
			}
		};
	}
	
	public void getFromPoint(RotatedTileBox tb, PointF point, List<? super TransportStop> res,
			List<TransportStop> objects) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		final int rp = getRadiusPoi(tb);
		int radius = rp * 3 / 2;
		try {
			TreeSet<String> ms = new TreeSet<>();
			for (int i = 0; i < objects.size(); i++) {
				TransportStop n = objects.get(i);
				if (n.getLocation() == null) {
					continue;
				}
				int x = (int) tb.getPixXFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
				int y = (int) tb.getPixYFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
				if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
					if (!ms.add(n.getName())) {
						// only unique names
						continue;
					}
					radius = rp;
					res.add(n);
				}
			}
		} catch (IndexOutOfBoundsException e) {
			// that's really rare case, but is much efficient than introduce synchronized block
		}
	}
	
	public TransportStopRoute getRoute() {
		return stopRoute;
	}
	
	public void setRoute(TransportStopRoute route) {
		this.stopRoute = route;
	}

	public boolean isShowTransportStops() {
		return showTransportStops;
	}

	public void setShowTransportStops(boolean showTransportStops) {
		this.showTransportStops = showTransportStops;
	}

	public int getRadiusPoi(RotatedTileBox tb){
		final double zoom = tb.getZoom();
		int r;
		if(zoom < startZoomRoute){
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
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		List<TransportStop> objects = null;
		if (tb.getZoom() >= startZoomRoute) {
			if (stopRoute != null) {
				objects = stopRoute.route.getForwardStops();
				int color = stopRoute.getColor(mapActivity.getMyApplication(), settings.isNightMode());
				attrs.paint.setColor(color);
				attrs.updatePaints(view, settings, tb);
				try {
					path.reset();
					List<Way> ws = stopRoute.route.getForwardWays();
					if (ws != null) {
						for (Way w : ws) {
							TIntArrayList tx = new TIntArrayList();
							TIntArrayList ty = new TIntArrayList();
							for (int i = 0; i < w.getNodes().size(); i++) {
								Node o = w.getNodes().get(i);
								int x = (int) tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
								int y = (int) tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
								tx.add(x);
								ty.add(y);
							}
							calculatePath(tb, tx, ty, path);
						}
					}
					attrs.drawPath(canvas, path);
				} catch (Exception e) {
					// ignore
				}
			}
		}

		if (showTransportStops && tb.getZoom() >= startZoom && objects == null) {
			data.queryNewData(tb);
			objects = data.getResults();
		}
		
		if (objects != null) {
			float iconSize = stopBus.getWidth() * 3 / 2.5f;
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tb);
			List<TransportStop> fullObjects = new ArrayList<>();
			for (TransportStop o : objects) {
				float x = tb.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				float y = tb.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());

				if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
					canvas.drawBitmap(stopSmall, x - stopSmall.getWidth() / 2, y - stopSmall.getHeight() / 2, paintIcon);
				} else {
					fullObjects.add(o);
				}
			}
			for (TransportStop o : fullObjects) {
				float x = tb.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				float y = tb.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				Bitmap b = stopBus;
				if (stopRoute != null) {
					TransportStopType type = TransportStopType.findType(stopRoute.route.getType());
					if (type != null) {
						int iconId = getIconIdByTypeOfStop(type);
						if (iconId != -1) {
							Bitmap background = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_transport_stop_bg);
							Bitmap foreground = BitmapFactory.decodeResource(view.getResources(), iconId);
							Bitmap m = overlayBitmapToCenter(background, foreground);
							canvas.drawBitmap(m, x - m.getWidth() / 2, y - m.getHeight() / 2, paintIcon);
						}
					}
				} else {
					canvas.drawBitmap(b, x - b.getWidth() / 2, y - b.getHeight() / 2, paintIcon);
				}
			}
		}
	}

	private int getIconIdByTypeOfStop(TransportStopType type) {
		switch (type) {
			case BUS:
				return R.drawable.mm_route_bus_ref;
			case FERRY:
				return R.drawable.mm_route_ferry_ref;
			case FUNICULAR:
				return R.drawable.mm_route_funicular_ref;
			case LIGHT_RAIL:
				return R.drawable.mm_route_light_rail_ref;
			case MONORAIL:
				return R.drawable.mm_route_monorail_ref;
			case RAILWAY:
				return R.drawable.mm_route_railway_ref;
			case SHARE_TAXI:
				return R.drawable.mm_route_share_taxi_ref;
			case TRAIN:
				return R.drawable.mm_route_train_ref;
			case TRAM:
				return R.drawable.mm_route_tram_ref;
			case TROLLEYBUS:
				return R.drawable.mm_route_trolleybus_ref;
			case SUBWAY:
				return R.drawable.mm_subway_station;
			default:
				return -1;
		}
	}

	private Bitmap overlayBitmapToCenter(Bitmap background, Bitmap foreground) {
		int backgroundWidth = background.getWidth();
		int backgroundHeight = background.getHeight();
		int foregroundWidth = foreground.getWidth();
		int foregroundHeight = foreground.getHeight();

		float marginLeft = (float) (backgroundWidth * 0.5 - foregroundWidth * 0.5);
		float marginTop = (float) (backgroundHeight * 0.5 - foregroundHeight * 0.5);

		Bitmap overlayBitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, background.getConfig());
		Canvas canvas = new Canvas(overlayBitmap);
		Paint paint = new Paint();
		ColorFilter filter = new PorterDuffColorFilter(ContextCompat.getColor(view.getContext(), R.color.primary_text_dark), PorterDuff.Mode.SRC_IN);
		paint.setColorFilter(filter);
		canvas.drawBitmap(background, new Matrix(), null);
		canvas.drawBitmap(foreground, marginLeft, marginTop, paint);
		return overlayBitmap;
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
	public PointDescription getObjectName(Object o) {
		if(o instanceof TransportStop){
			return new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_STOP, mapActivity.getString(R.string.transport_Stop),
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
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res, boolean unknownLocation) {
		if(tileBox.getZoom() >= startZoomRoute  && stopRoute != null) {
			getFromPoint(tileBox, point, res, stopRoute.route.getForwardStops());
		} else if (tileBox.getZoom() >= startZoom && data.getResults() != null) {
			getFromPoint(tileBox, point, res, data.getResults());
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
