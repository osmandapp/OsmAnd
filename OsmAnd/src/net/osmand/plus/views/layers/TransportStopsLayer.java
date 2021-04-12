package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import net.osmand.ResultMatcher;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.geometry.GeometryWay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class TransportStopsLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {

	public static final String TRANSPORT_STOPS_OVER_MAP = "transportStops";

	private static final int startZoom = 12;
	private static final int startZoomRoute = 10;

	private final MapActivity mapActivity;
	private OsmandMapTileView view;

	private RenderingLineAttributes attrs;

	private MapLayerData<List<TransportStop>> data;
	private TransportStopRoute stopRoute = null;

	private CommonPreference<Boolean> showTransportStops;

	private Path path;

	public TransportStopsLayer(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		showTransportStops = settings.getCustomRenderBooleanProperty(TRANSPORT_STOPS_OVER_MAP).cache();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		path = new Path();
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
				try {
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
				} catch (IOException e) {
					return new ArrayList<>();
				}
			}
		};
	}

	private void getFromPoint(RotatedTileBox tb, PointF point, List<? super TransportStop> res,
							  List<TransportStop> objects) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		final int rp = getScaledTouchRadius(mapActivity.getMyApplication(), getRadiusPoi(tb));
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

	private int getRadiusPoi(RotatedTileBox tb){
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
		boolean nightMode = settings.isNightMode();
		OsmandApplication app = mapActivity.getMyApplication();
		if (tb.getZoom() >= startZoomRoute) {
			if (stopRoute != null) {
				objects = stopRoute.route.getForwardStops();
				int color = stopRoute.getColor(app, nightMode);
				attrs.paint.setColor(color);
				attrs.updatePaints(view.getApplication(), settings, tb);
				try {
					path.reset();
					List<Way> ws = stopRoute.route.getForwardWays();
					if (ws != null) {
						for (Way w : ws) {
							List<Float> tx = new ArrayList<>();
							List<Float> ty = new ArrayList<>();
							for (int i = 0; i < w.getNodes().size(); i++) {
								Node o = w.getNodes().get(i);
								float x = tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
								float y = tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
								tx.add(x);
								ty.add(y);
							}
							GeometryWay.calculatePath(tb, tx, ty, path);
						}
					}
					attrs.drawPath(canvas, path);
				} catch (Exception e) {
					// ignore
				}
			}
		}

		if (showTransportStops.get() && tb.getZoom() >= startZoom && objects == null) {
			data.queryNewData(tb);
			objects = data.getResults();
		}

		if (objects != null) {
			float textScale = app.getSettings().TEXT_SCALE.get();
			float iconSize = getIconSize(app);
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tb);
			List<TransportStop> fullObjects = new ArrayList<>();
			for (TransportStop o : objects) {
				float x = tb.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				float y = tb.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());

				if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
					PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(mapActivity,
							ContextCompat.getColor(mapActivity, R.color.transport_stop_icon_background),
							true, false, 0, BackgroundType.SQUARE);
					pointImageDrawable.setAlpha(0.9f);
					pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
				} else {
					fullObjects.add(o);
				}
			}

			for (TransportStop o : fullObjects) {
				float x = tb.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				float y = tb.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				if (stopRoute != null) {
					TransportStopType type = TransportStopType.findType(stopRoute.route.getType());
					if (type != null) {
						drawPoint(canvas, textScale, x, y, RenderingIcons.getResId(type.getResName()));
					}
				} else {
					drawPoint(canvas, textScale, x, y, R.drawable.mx_highway_bus_stop);
				}
			}
		}
	}

	private void drawPoint(Canvas canvas, float textScale, float x, float y, @DrawableRes int iconId) {
		PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(mapActivity,
				ContextCompat.getColor(mapActivity, R.color.transport_stop_icon_background),
				true,false ,iconId, BackgroundType.SQUARE);
		pointImageDrawable.setAlpha(0.9f);
		pointImageDrawable.drawPoint(canvas, x, y, textScale, false);
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
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
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
