package net.osmand.aidl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;

import net.osmand.aidl.mapmarker.AMapMarker;
import net.osmand.aidl.mapwidget.AMapWidget;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OsmandAidlApi {

	private static final String AIDL_REFRESH_MAP = "aidl_refresh_map";
	private static final String AIDL_ADD_MAP_WIDGET = "aidl_add_map_widget";
	private static final String AIDL_REMOVE_MAP_WIDGET = "aidl_remove_map_widget";
	private static final String AIDL_MAP_WIDGET_ID = "aidl_map_widget_id";

	private OsmandApplication app;
	private Map<String, AMapWidget> widgets = new ConcurrentHashMap<>();
	private Map<String, TextInfoWidget> widgetControls = new ConcurrentHashMap<>();

	private BroadcastReceiver refreshMapReceiver;
	private BroadcastReceiver addMapWidgetReceiver;
	private BroadcastReceiver removeMapWidgetReceiver;

	public OsmandAidlApi(OsmandApplication app) {
		this.app = app;
	}

	public void onCreateMapActivity(final MapActivity mapActivity) {
		registerRefreshMapReceiver(mapActivity);
		registerAddMapWidgetReceiver(mapActivity);
		registerRemoveMapWidgetReceiver(mapActivity);
	}

	public void onDestroyMapActivity(final MapActivity mapActivity) {
		if (refreshMapReceiver != null) {
			mapActivity.unregisterReceiver(refreshMapReceiver);
		}
		if (addMapWidgetReceiver != null) {
			mapActivity.unregisterReceiver(addMapWidgetReceiver);
		}
		if (removeMapWidgetReceiver != null) {
			mapActivity.unregisterReceiver(removeMapWidgetReceiver);
		}
		widgetControls.clear();
	}

	private void registerRefreshMapReceiver(final MapActivity mapActivity) {
		refreshMapReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mapActivity.refreshMap();
			}
		};
		mapActivity.registerReceiver(refreshMapReceiver, new IntentFilter(AIDL_REFRESH_MAP));
	}

	private int getDrawableId(String id) {
		if (Algorithms.isEmpty(id)) {
			return 0;
		} else {
			return app.getResources().getIdentifier(id, "drawable", app.getPackageName());
		}
	}

	private void registerAddMapWidgetReceiver(final MapActivity mapActivity) {
		addMapWidgetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String widgetId = intent.getStringExtra(AIDL_MAP_WIDGET_ID);
				if (widgetId != null) {
					AMapWidget widget = widgets.get(widgetId);
					if (widget != null) {
						MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
						if (layer != null) {
							TextInfoWidget control = createWidgetControl(mapActivity, widgetId);
							widgetControls.put(widgetId, control);
							int menuIconId = getDrawableId(widget.getMenuIconName());
							MapWidgetRegInfo widgetInfo = layer.registerSideWidget(control,
									menuIconId, widget.getMenuTitle(), "aidl_widget_" + widgetId,
									false, widget.getOrder());
							if (!mapActivity.getMapLayers().getMapWidgetRegistry().isVisible(widgetInfo.key)) {
								mapActivity.getMapLayers().getMapWidgetRegistry().setVisibility(widgetInfo, true, false);
							}
							layer.recreateControls();
						}
					}
				}
			}
		};
		mapActivity.registerReceiver(addMapWidgetReceiver, new IntentFilter(AIDL_ADD_MAP_WIDGET));
	}

	private void registerRemoveMapWidgetReceiver(final MapActivity mapActivity) {
		removeMapWidgetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String widgetId = intent.getStringExtra(AIDL_MAP_WIDGET_ID);
				if (widgetId != null) {
					MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
					TextInfoWidget widgetControl = widgetControls.get(widgetId);
					if (layer != null && widgetControl != null) {
						layer.removeSideWidget(widgetControl);
						widgetControls.remove(widgetId);
						layer.recreateControls();
					}
				}
			}
		};
		mapActivity.registerReceiver(removeMapWidgetReceiver, new IntentFilter(AIDL_REMOVE_MAP_WIDGET));
	}

	public void registerWidgetControls(MapActivity mapActivity) {
		for (AMapWidget widget : widgets.values()) {
			MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
			if (layer != null) {
				TextInfoWidget control = createWidgetControl(mapActivity, widget.getId());
				widgetControls.put(widget.getId(), control);
				int menuIconId = getDrawableId(widget.getMenuIconName());
				MapWidgetRegInfo widgetInfo = layer.registerSideWidget(control,
						menuIconId, widget.getMenuTitle(), "aidl_widget_" + widget.getId(),
						false, widget.getOrder());
				if (!mapActivity.getMapLayers().getMapWidgetRegistry().isVisible(widgetInfo.key)) {
					mapActivity.getMapLayers().getMapWidgetRegistry().setVisibility(widgetInfo, true, false);
				}
			}
		}
	}

	private void refreshMap() {
		Intent intent = new Intent();
		intent.setAction(AIDL_REFRESH_MAP);
		app.sendBroadcast(intent);
	}

	public TextInfoWidget createWidgetControl(final MapActivity mapActivity, final String widgetId) {
		final TextInfoWidget control = new TextInfoWidget(mapActivity) {

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				AMapWidget widget = widgets.get(widgetId);
				if (widget != null) {
					String txt = widget.getText();
					String subtxt = widget.getDescription();
					boolean night = drawSettings != null && drawSettings.isNightMode();
					int icon = night ? getDrawableId(widget.getDarkIconName()) : getDrawableId(widget.getLightIconName());
					setText(txt, subtxt);
					if (icon != 0) {
						setImageDrawable(icon);
					} else {
						setImageDrawable(null);
					}
					return true;
				} else {
					return false;
				}
			}
		};
		control.updateInfo(null);

		control.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AMapWidget widget = widgets.get(widgetId);
				if (widget != null && widget.getIntentOnClick() != null) {
					app.startActivity(widget.getIntentOnClick());
				}
			}
		});
		return control;
	}

	boolean addMapMarker(AMapMarker marker) {
		if (marker != null) {
			PointDescription pd = new PointDescription(
					PointDescription.POINT_TYPE_MAP_MARKER, marker.getName() != null ? marker.getName() : "");
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			markersHelper.addMapMarker(new LatLon(marker.getLatLon().getLatitude(), marker.getLatLon().getLongitude()), pd);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeMapMarker(AMapMarker marker) {
		if (marker != null) {
			LatLon latLon = new LatLon(marker.getLatLon().getLatitude(), marker.getLatLon().getLongitude());
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			List<MapMarker> mapMarkers = markersHelper.getMapMarkers();
			for (MapMarker m : mapMarkers) {
				if (m.getOnlyName().equals(marker.getName()) && latLon.equals(new LatLon(m.getLatitude(), m.getLongitude()))) {
					markersHelper.removeMapMarker(m);
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean updateMapMarker(AMapMarker markerPrev, AMapMarker markerNew) {
		if (markerPrev != null && markerNew != null) {
			LatLon latLon = new LatLon(markerPrev.getLatLon().getLatitude(), markerPrev.getLatLon().getLongitude());
			LatLon latLonNew = new LatLon(markerNew.getLatLon().getLatitude(), markerNew.getLatLon().getLongitude());
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			List<MapMarker> mapMarkers = markersHelper.getMapMarkers();
			for (MapMarker m : mapMarkers) {
				if (m.getOnlyName().equals(markerPrev.getName()) && latLon.equals(new LatLon(m.getLatitude(), m.getLongitude()))) {
					PointDescription pd = new PointDescription(
							PointDescription.POINT_TYPE_MAP_MARKER, markerNew.getName() != null ? markerNew.getName() : "");
					MapMarker marker = new MapMarker(m.point, pd, m.colorIndex, m.selected, m.index);
					markersHelper.moveMapMarker(marker, latLonNew);
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean addMapWidget(AMapWidget widget) {
		if (widget != null) {
			if (widgets.containsKey(widget.getId())) {
				updateMapWidget(widget);
			} else {
				widgets.put(widget.getId(), widget);
				Intent intent = new Intent();
				intent.setAction(AIDL_ADD_MAP_WIDGET);
				intent.putExtra(AIDL_MAP_WIDGET_ID, widget.getId());
				app.sendBroadcast(intent);
			}
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeMapWidget(String widgetId) {
		if (!Algorithms.isEmpty(widgetId) && widgets.containsKey(widgetId)) {
			widgets.remove(widgetId);
			Intent intent = new Intent();
			intent.setAction(AIDL_REMOVE_MAP_WIDGET);
			intent.putExtra(AIDL_MAP_WIDGET_ID, widgetId);
			app.sendBroadcast(intent);
			return true;
		} else {
			return false;
		}
	}

	boolean updateMapWidget(AMapWidget widget) {
		if (widget != null && widgets.containsKey(widget.getId())) {
			widgets.put(widget.getId(), widget);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}
}
