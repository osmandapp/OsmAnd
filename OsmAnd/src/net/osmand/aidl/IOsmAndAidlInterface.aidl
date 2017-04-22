package net.osmand.aidl;

import net.osmand.aidl.ALatLon;
import net.osmand.aidl.mapmarker.AMapMarker;
import net.osmand.aidl.mapmarker.AddMapMarkerParams;
import net.osmand.aidl.mapmarker.RemoveMapMarkerParams;
import net.osmand.aidl.mapmarker.UpdateMapMarkerParams;
import net.osmand.aidl.calculateroute.CalculateRouteParams;

import net.osmand.aidl.mapwidget.AMapWidget;
import net.osmand.aidl.mapwidget.AddMapWidgetParams;
import net.osmand.aidl.mapwidget.RemoveMapWidgetParams;
import net.osmand.aidl.mapwidget.UpdateMapWidgetParams;

import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.aidl.maplayer.point.AddMapPointParams;
import net.osmand.aidl.maplayer.point.RemoveMapPointParams;
import net.osmand.aidl.maplayer.point.UpdateMapPointParams;
import net.osmand.aidl.maplayer.AMapLayer;
import net.osmand.aidl.maplayer.AddMapLayerParams;
import net.osmand.aidl.maplayer.RemoveMapLayerParams;
import net.osmand.aidl.maplayer.UpdateMapLayerParams;

interface IOsmAndAidlInterface {

    boolean addMapMarker(in AddMapMarkerParams params);
    boolean removeMapMarker(in RemoveMapMarkerParams params);
    boolean updateMapMarker(in UpdateMapMarkerParams params);

    boolean addMapWidget(in AddMapWidgetParams params);
    boolean removeMapWidget(in RemoveMapWidgetParams params);
    boolean updateMapWidget(in UpdateMapWidgetParams params);

    boolean addMapPoint(in AddMapPointParams params);
    boolean removeMapPoint(in RemoveMapPointParams params);
    boolean updateMapPoint(in UpdateMapPointParams params);

    boolean addMapLayer(in AddMapLayerParams params);
    boolean removeMapLayer(in RemoveMapLayerParams params);
    boolean updateMapLayer(in UpdateMapLayerParams params);

    boolean calculateRoute(in CalculateRouteParams params);
}
