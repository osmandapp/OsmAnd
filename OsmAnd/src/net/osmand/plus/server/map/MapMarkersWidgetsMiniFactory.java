package net.osmand.plus.server.map;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.*;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;
public class MapMarkersWidgetsMiniFactory {

    public static final int MIN_DIST_OK_VISIBLE = 40; // meters
    public static final int MIN_DIST_2ND_ROW_SHOW = 150; // meters
    private static OsmandApplication app;

    private MapMarkersHelper helper;
    private int screenOrientation;
    private boolean portraitMode;


    private LatLon loc;

    private boolean cachedTopBarVisibility;

    public MapMarkersWidgetsMiniFactory(final OsmandApplication application) {
        this.app = application;
        helper = app.getMapMarkersHelper();
        screenOrientation = app.getUIUtilities().getScreenOrientation();
        //portraitMode = AndroidUiHelper.isOrientationPortrait(map);

        updateVisibility(false);
    }

    private void removeMarker(int index) {
        if (helper.getMapMarkers().size() > index) {
            helper.moveMapMarkerToHistory(helper.getMapMarkers().get(index));
        }
    }

    private void showMarkerOnMap(int index) {
//        if (helper.getMapMarkers().size() > index) {
//            MapMarkersHelper.MapMarker marker = helper.getMapMarkers().get(index);
//            AnimateDraggingMapThread thread = app.getMapView().getAnimatedDraggingThread();
//            LatLon pointToNavigate = marker.point;
//            if (pointToNavigate != null) {
//                int fZoom = map.getMapView().getZoom() < 15 ? 15 : map.getMapView().getZoom();
//                thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
//            }
//            //MapMarkerDialogHelper.showMarkerOnMap(map, marker);
//        }
    }

    public boolean updateVisibility(boolean visible) {
        return visible;
    }

    public int getTopBarHeight() {
        return 0;
    }

    public boolean isTopBarVisible() {
        return false;
    }

    public void updateInfo(LatLon customLocation, int zoom) {
        if (customLocation != null) {
            loc = customLocation;
        } else {
            Location l = app.getLocationProvider().getLastStaleKnownLocation();
            if (l != null) {
                loc = new LatLon(l.getLatitude(), l.getLongitude());
            } else {
            }
        }

        List<MapMarkersHelper.MapMarker> markers = helper.getMapMarkers();
        if (zoom < 3 || markers.size() == 0
                || !app.getSettings().MARKERS_DISTANCE_INDICATION_ENABLED.get()
                || !app.getSettings().MAP_MARKERS_MODE.get().isToolbar()
                || app.getRoutingHelper().isFollowingMode()
                || app.getRoutingHelper().isRoutePlanningMode()) {
            updateVisibility(false);
            return;
        }

        Float heading = app.getMapViewTrackingUtilities().getHeading();
        MapMarkersHelper.MapMarker marker = markers.get(0);

        if (markers.size() > 1 && app.getSettings().DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 2) {
            marker = markers.get(1);
            if (loc != null && customLocation == null) {
                for (int i = 1; i < markers.size(); i++) {
                    MapMarkersHelper.MapMarker m = markers.get(i);
                    m.dist = (int) (MapUtils.getDistance(m.getLatitude(), m.getLongitude(),
                            loc.getLatitude(), loc.getLongitude()));
                    if (m.dist < MIN_DIST_2ND_ROW_SHOW && marker.dist > m.dist) {
                        marker = m;
                    }
                }
            }
        } else {
        }

        updateVisibility(true);
    }

    private void updateUI(LatLon loc, Float heading, MapMarkersHelper.MapMarker marker, ImageView arrowImg,
                          TextView distText, ImageButton okButton, TextView addressText,
                          boolean firstLine, boolean customLocation) {
        float[] mes = new float[2];
        if (loc != null && marker.point != null) {
            Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
        }

        if (customLocation) {
            heading = 0f;
        }

        boolean newImage = false;
        DirectionDrawable dd;
        if (!(arrowImg.getDrawable() instanceof DirectionDrawable)) {
            newImage = true;
            dd = new DirectionDrawable(app, arrowImg.getWidth(), arrowImg.getHeight());
        } else {
            dd = (DirectionDrawable) arrowImg.getDrawable();
        }
        dd.setImage(R.drawable.ic_arrow_marker_diretion, MapMarkersHelper.MapMarker.getColorId(marker.colorIndex));
        if (heading != null && loc != null) {
            dd.setAngle(mes[1] - heading + 180 + screenOrientation);
        }
        if (newImage) {
            arrowImg.setImageDrawable(dd);
        }
        arrowImg.invalidate();

        int dist = (int) mes[0];
        String txt;
        if (loc != null) {
            txt = OsmAndFormatter.getFormattedDistance(dist, app);
        } else {
            txt = "—";
        }
        if (txt != null) {
            distText.setText(txt);
        }
        AndroidUiHelper.updateVisibility(okButton, !customLocation && loc != null && dist < MIN_DIST_OK_VISIBLE);

        String descr;
        PointDescription pd = marker.getPointDescription(app);
        if (Algorithms.isEmpty(pd.getName())) {
            descr = pd.getTypeName();
        } else {
            descr = pd.getName();
        }
        if (!firstLine && !isLandscapeLayout()) {
            descr = "  •  " + descr;
        }

        addressText.setText(descr);
    }

    public TextInfoWidget createMapMarkerControl(final MapActivity map, final boolean firstMarker) {
        return new net.osmand.plus.views.mapwidgets.MapMarkersWidgetsFactory.DistanceToMapMarkerControl(map, firstMarker) {
            @Override
            public LatLon getLatLon() {
                return loc;
            }

            @Override
            protected void click(OsmandMapTileView view) {
                showMarkerOnMap(firstMarker ? 0 : 1);
            }
        };
    }

    public boolean isLandscapeLayout() {
        return !portraitMode;
    }

    public abstract static class DistanceToMapMarkerControl extends TextInfoWidget {

        private boolean firstMarker;
        private final OsmandMapTileView view;
        private MapActivity map;
        private MapMarkersHelper helper;
        private float[] calculations = new float[1];
        private int cachedMeters;
        private int cachedMarkerColorIndex = -1;
        private Boolean cachedNightMode = null;

        public DistanceToMapMarkerControl(MapActivity map, boolean firstMarker) {
            super(map);
            this.map = map;
            this.firstMarker = firstMarker;
            this.view = map.getMapView();
            helper = app.getMapMarkersHelper();
            setText(null, null);
            setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    click(view);
                }
            });
        }

        protected abstract void click(OsmandMapTileView view);

        public abstract LatLon getLatLon();

        @Override
        public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
            MapMarkersHelper.MapMarker marker = getMarker();
            if (marker == null
                    || app.getRoutingHelper().isRoutePlanningMode()
                    || app.getRoutingHelper().isFollowingMode()) {
                cachedMeters = 0;
                setText(null, null);
                return false;
            }
            boolean res = false;
            int d = getDistance();
            if (isUpdateNeeded() || cachedMeters != d) {
                cachedMeters = d;
                String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, app);
                int ls = ds.lastIndexOf(' ');
                if (ls == -1) {
                    setText(ds, null);
                } else {
                    setText(ds.substring(0, ls), ds.substring(ls + 1));
                }
                res = true;
            }

            if (marker.colorIndex != -1) {
                if (marker.colorIndex != cachedMarkerColorIndex
                        || cachedNightMode == null || cachedNightMode != isNight()) {
                    setImageDrawable(app.getUIUtilities()
                            .getLayeredIcon(isNight() ? R.drawable.widget_marker_night : R.drawable.widget_marker_day,
                                    R.drawable.widget_marker_triangle, 0,
                                    MapMarkersHelper.MapMarker.getColorId(marker.colorIndex)));
                    cachedMarkerColorIndex = marker.colorIndex;
                    cachedNightMode = isNight();
                    res = true;
                }
            }
            return res;
        }

        @Override
        public boolean isMetricSystemDepended() {
            return true;
        }

        public LatLon getPointToNavigate() {
            MapMarkersHelper.MapMarker marker = getMarker();
            if (marker != null) {
                return marker.point;
            }
            return null;
        }

        private MapMarkersHelper.MapMarker getMarker() {
            List<MapMarkersHelper.MapMarker> markers = helper.getMapMarkers();
            if (firstMarker) {
                if (markers.size() > 0) {
                    return markers.get(0);
                }
            } else {
                if (markers.size() > 1) {
                    return markers.get(1);
                }
            }
            return null;
        }

        public int getDistance() {
            int d = 0;
            LatLon l = getPointToNavigate();
            if (l != null) {
                LatLon loc = getLatLon();
                if (loc == null) {
                    Location.distanceBetween(view.getLatitude(), view.getLongitude(), l.getLatitude(), l.getLongitude(), calculations);
                } else {
                    Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), l.getLatitude(), l.getLongitude(), calculations);
                }
                d = (int) calculations[0];
            }
            return d;
        }
    }
}

