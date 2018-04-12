package net.osmand.aidl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AlertDialog;
import android.view.View;

import net.osmand.IndexConstants;
import net.osmand.aidl.favorite.AFavorite;
import net.osmand.aidl.favorite.group.AFavoriteGroup;
import net.osmand.aidl.gpx.ASelectedGpxFile;
import net.osmand.aidl.gpx.StartGpxRecordingParams;
import net.osmand.aidl.gpx.StopGpxRecordingParams;
import net.osmand.aidl.maplayer.AMapLayer;
import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.aidl.mapmarker.AMapMarker;
import net.osmand.aidl.mapwidget.AMapWidget;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.AidlMapLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OsmandAidlApi {

	private static final String AIDL_REFRESH_MAP = "aidl_refresh_map";
	private static final String AIDL_SET_MAP_LOCATION = "aidl_set_map_location";
	private static final String AIDL_LATITUDE = "aidl_latitude";
	private static final String AIDL_LONGITUDE = "aidl_longitude";
	private static final String AIDL_ZOOM = "aidl_zoom";
	private static final String AIDL_ANIMATED = "aidl_animated";

	private static final String AIDL_START_NAME = "aidl_start_name";
	private static final String AIDL_START_LAT = "aidl_start_lat";
	private static final String AIDL_START_LON = "aidl_start_lon";
	private static final String AIDL_DEST_NAME = "aidl_dest_name";
	private static final String AIDL_DEST_LAT = "aidl_dest_lat";
	private static final String AIDL_DEST_LON = "aidl_dest_lon";
	private static final String AIDL_PROFILE = "aidl_profile";
	private static final String AIDL_DATA = "aidl_data";
	private static final String AIDL_URI = "aidl_uri";
	private static final String AIDL_FORCE = "aidl_force";

	private static final String AIDL_OBJECT_ID = "aidl_object_id";

	private static final String AIDL_ADD_MAP_WIDGET = "aidl_add_map_widget";
	private static final String AIDL_REMOVE_MAP_WIDGET = "aidl_remove_map_widget";

	private static final String AIDL_ADD_MAP_LAYER = "aidl_add_map_layer";
	private static final String AIDL_REMOVE_MAP_LAYER = "aidl_remove_map_layer";

	private static final String AIDL_TAKE_PHOTO_NOTE = "aidl_take_photo_note";
	private static final String AIDL_START_VIDEO_RECORDING = "aidl_start_video_recording";
	private static final String AIDL_START_AUDIO_RECORDING = "aidl_start_audio_recording";
	private static final String AIDL_STOP_RECORDING = "aidl_stop_recording";

	private static final String AIDL_NAVIGATE = "aidl_navigate";
	private static final String AIDL_NAVIGATE_GPX = "aidl_navigate_gpx";

	private static final ApplicationMode DEFAULT_PROFILE = ApplicationMode.CAR;

	private static final ApplicationMode[] VALID_PROFILES = new ApplicationMode[]{
			ApplicationMode.CAR,
			ApplicationMode.BICYCLE,
			ApplicationMode.PEDESTRIAN
	};

	private OsmandApplication app;
	private Map<String, AMapWidget> widgets = new ConcurrentHashMap<>();
	private Map<String, TextInfoWidget> widgetControls = new ConcurrentHashMap<>();
	private Map<String, AMapLayer> layers = new ConcurrentHashMap<>();
	private Map<String, OsmandMapLayer> mapLayers = new ConcurrentHashMap<>();

	private BroadcastReceiver refreshMapReceiver;
	private BroadcastReceiver setMapLocationReceiver;
	private BroadcastReceiver addMapWidgetReceiver;
	private BroadcastReceiver removeMapWidgetReceiver;
	private BroadcastReceiver addMapLayerReceiver;
	private BroadcastReceiver removeMapLayerReceiver;
	private BroadcastReceiver takePhotoNoteReceiver;
	private BroadcastReceiver startVideoRecordingReceiver;
	private BroadcastReceiver startAudioRecordingReceiver;
	private BroadcastReceiver stopRecordingReceiver;
	private BroadcastReceiver navigateReceiver;
	private BroadcastReceiver navigateGpxReceiver;

	public OsmandAidlApi(OsmandApplication app) {
		this.app = app;
	}

	public void onCreateMapActivity(final MapActivity mapActivity) {
		registerRefreshMapReceiver(mapActivity);
		registerSetMapLocationReceiver(mapActivity);
		registerAddMapWidgetReceiver(mapActivity);
		registerRemoveMapWidgetReceiver(mapActivity);
		registerAddMapLayerReceiver(mapActivity);
		registerRemoveMapLayerReceiver(mapActivity);
		registerTakePhotoNoteReceiver(mapActivity);
		registerStartVideoRecordingReceiver(mapActivity);
		registerStartAudioRecordingReceiver(mapActivity);
		registerStopRecordingReceiver(mapActivity);
		registerNavigateReceiver(mapActivity);
		registerNavigateGpxReceiver(mapActivity);
	}

	public void onDestroyMapActivity(final MapActivity mapActivity) {
		if (refreshMapReceiver != null) {
			try {
				mapActivity.unregisterReceiver(refreshMapReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			refreshMapReceiver = null;
		}
		if (setMapLocationReceiver != null) {
			try {
				mapActivity.unregisterReceiver(setMapLocationReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			setMapLocationReceiver = null;
		}

		if (addMapWidgetReceiver != null) {
			try {
				mapActivity.unregisterReceiver(addMapWidgetReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			addMapWidgetReceiver = null;
		}
		if (removeMapWidgetReceiver != null) {
			try {
				mapActivity.unregisterReceiver(removeMapWidgetReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			removeMapWidgetReceiver = null;
		}
		widgetControls.clear();

		if (addMapLayerReceiver != null) {
			try {
				mapActivity.unregisterReceiver(addMapLayerReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			addMapLayerReceiver = null;
		}
		if (removeMapLayerReceiver != null) {
			try {
				mapActivity.unregisterReceiver(removeMapLayerReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			removeMapLayerReceiver = null;
		}
		if (takePhotoNoteReceiver != null) {
			try {
				mapActivity.unregisterReceiver(takePhotoNoteReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			takePhotoNoteReceiver = null;
		}
		if (startVideoRecordingReceiver != null) {
			try {
				mapActivity.unregisterReceiver(startVideoRecordingReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			startVideoRecordingReceiver = null;
		}
		if (startAudioRecordingReceiver != null) {
			try {
				mapActivity.unregisterReceiver(startAudioRecordingReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			startAudioRecordingReceiver = null;
		}
		if (stopRecordingReceiver != null) {
			try {
				mapActivity.unregisterReceiver(stopRecordingReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			stopRecordingReceiver = null;
		}
		if (navigateReceiver != null) {
			try {
				mapActivity.unregisterReceiver(navigateReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			navigateReceiver = null;
		}
		if (navigateGpxReceiver != null) {
			try {
				mapActivity.unregisterReceiver(navigateGpxReceiver);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			navigateGpxReceiver = null;
		}
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

	private void registerSetMapLocationReceiver(final MapActivity mapActivity) {
		setMapLocationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
				double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
				int zoom = intent.getIntExtra(AIDL_ZOOM, 0);
				boolean animated = intent.getBooleanExtra(AIDL_ANIMATED, false);
				if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
					OsmandMapTileView mapView = mapActivity.getMapView();
					if (zoom == 0) {
						zoom = mapView.getZoom();
					} else {
						zoom = zoom > mapView.getMaxZoom() ? mapView.getMaxZoom() : zoom;
						zoom = zoom < mapView.getMinZoom() ? mapView.getMinZoom() : zoom;
					}
					if (animated) {
						mapView.getAnimatedDraggingThread().startMoving(lat, lon, zoom, true);
					} else {
						mapView.setLatLon(lat, lon);
						mapView.setIntZoom(zoom);
					}
				}
				mapActivity.refreshMap();
			}
		};
		mapActivity.registerReceiver(setMapLocationReceiver, new IntentFilter(AIDL_SET_MAP_LOCATION));
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
				String widgetId = intent.getStringExtra(AIDL_OBJECT_ID);
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
				String widgetId = intent.getStringExtra(AIDL_OBJECT_ID);
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

	private void registerAddMapLayerReceiver(final MapActivity mapActivity) {
		addMapLayerReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String layerId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (layerId != null) {
					AMapLayer layer = layers.get(layerId);
					if (layer != null) {
						OsmandMapLayer mapLayer = mapLayers.get(layerId);
						if (mapLayer != null) {
							mapActivity.getMapView().removeLayer(mapLayer);
						}
						mapLayer = new AidlMapLayer(mapActivity, layer);
						mapActivity.getMapView().addLayer(mapLayer, layer.getZOrder());
						mapLayers.put(layerId, mapLayer);
					}
				}
			}
		};
		mapActivity.registerReceiver(addMapLayerReceiver, new IntentFilter(AIDL_ADD_MAP_LAYER));
	}

	private void registerRemoveMapLayerReceiver(final MapActivity mapActivity) {
		removeMapLayerReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String layerId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (layerId != null) {
					OsmandMapLayer mapLayer = mapLayers.remove(layerId);
					if (mapLayer != null) {
						mapActivity.getMapView().removeLayer(mapLayer);
						mapActivity.refreshMap();
					}
				}
			}
		};
		mapActivity.registerReceiver(removeMapLayerReceiver, new IntentFilter(AIDL_REMOVE_MAP_LAYER));
	}

	private void registerTakePhotoNoteReceiver(final MapActivity mapActivity) {
		takePhotoNoteReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.takePhoto(lat, lon, mapActivity, false, true);
				}
			}
		};
		mapActivity.registerReceiver(takePhotoNoteReceiver, new IntentFilter(AIDL_TAKE_PHOTO_NOTE));
	}

	private void registerStartVideoRecordingReceiver(final MapActivity mapActivity) {
		startVideoRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.recordVideo(lat, lon, mapActivity, true);
				}
			}
		};
		mapActivity.registerReceiver(startVideoRecordingReceiver, new IntentFilter(AIDL_START_VIDEO_RECORDING));
	}

	private void registerStartAudioRecordingReceiver(final MapActivity mapActivity) {
		startVideoRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.recordAudio(lat, lon, mapActivity);
				}
			}
		};
		mapActivity.registerReceiver(startVideoRecordingReceiver, new IntentFilter(AIDL_START_AUDIO_RECORDING));
	}

	private void registerStopRecordingReceiver(final MapActivity mapActivity) {
		stopRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (plugin != null) {
					plugin.stopRecording(mapActivity, false);
				}
			}
		};
		mapActivity.registerReceiver(stopRecordingReceiver, new IntentFilter(AIDL_STOP_RECORDING));
	}

	private void registerNavigateReceiver(final MapActivity mapActivity) {
		navigateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String profileStr = intent.getStringExtra(AIDL_PROFILE);
				final ApplicationMode profile = ApplicationMode.valueOfStringKey(profileStr, DEFAULT_PROFILE);
				boolean validProfile = false;
				for (ApplicationMode mode : VALID_PROFILES) {
					if (mode == profile) {
						validProfile = true;
						break;
					}
				}
				if (validProfile) {
					String startName = intent.getStringExtra(AIDL_START_NAME);
					if (Algorithms.isEmpty(startName)) {
						startName = "";
					}
					String destName = intent.getStringExtra(AIDL_DEST_NAME);
					if (Algorithms.isEmpty(destName)) {
						destName = "";
					}

					final LatLon start;
					final PointDescription startDesc;
					double startLat = intent.getDoubleExtra(AIDL_START_LAT, 0);
					double startLon = intent.getDoubleExtra(AIDL_START_LON, 0);
					if (startLat != 0 && startLon != 0) {
						start = new LatLon(startLat, startLon);
						startDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, startName);
					} else {
						start = null;
						startDesc = null;
					}

					double destLat = intent.getDoubleExtra(AIDL_DEST_LAT, 0);
					double destLon = intent.getDoubleExtra(AIDL_DEST_LON, 0);
					final LatLon dest = new LatLon(destLat, destLon);
					final PointDescription destDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, destName);

					final RoutingHelper routingHelper = app.getRoutingHelper();
					boolean force = intent.getBooleanExtra(AIDL_FORCE, true);
					if (routingHelper.isFollowingMode() && !force) {
						AlertDialog dlg = mapActivity.getMapActions().stopNavigationActionConfirm();
						dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

							@Override
							public void onDismiss(DialogInterface dialog) {
								if (!routingHelper.isFollowingMode()) {
									startNavigation(mapActivity, null, start, startDesc, dest, destDesc, profile);
								}
							}
						});
					} else {
						startNavigation(mapActivity, null, start, startDesc, dest, destDesc, profile);
					}
				}
			}
		};
		mapActivity.registerReceiver(navigateReceiver, new IntentFilter(AIDL_NAVIGATE));
	}

	private void registerNavigateGpxReceiver(final MapActivity mapActivity) {
		navigateGpxReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				boolean force = intent.getBooleanExtra(AIDL_FORCE, false);

				GPXFile gpx = null;
				if (intent.getStringExtra(AIDL_DATA) != null) {
					String gpxStr = intent.getStringExtra(AIDL_DATA);
					if (!Algorithms.isEmpty(gpxStr)) {
						gpx = GPXUtilities.loadGPXFile(mapActivity, new ByteArrayInputStream(gpxStr.getBytes()));
					}
				} else if (intent.getParcelableExtra(AIDL_URI) != null) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						Uri gpxUri = intent.getParcelableExtra(AIDL_URI);

						ParcelFileDescriptor gpxParcelDescriptor = null;
						try {
							gpxParcelDescriptor = mapActivity.getContentResolver().openFileDescriptor(gpxUri, "r");
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
						if (gpxParcelDescriptor != null) {
							FileDescriptor fileDescriptor = gpxParcelDescriptor.getFileDescriptor();
							gpx = GPXUtilities.loadGPXFile(mapActivity, new FileInputStream(fileDescriptor));
						}
					}
				}

				if (gpx != null) {
					final RoutingHelper routingHelper = app.getRoutingHelper();
					if (routingHelper.isFollowingMode() && !force) {
						final GPXFile gpxFile = gpx;
						AlertDialog dlg = mapActivity.getMapActions().stopNavigationActionConfirm();
						dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

							@Override
							public void onDismiss(DialogInterface dialog) {
								if (!routingHelper.isFollowingMode()) {
									startNavigation(mapActivity, gpxFile, null, null, null, null, null);
								}
							}
						});
					} else {
						startNavigation(mapActivity, gpx, null, null, null, null, null);
					}
				}
			}
		};
		mapActivity.registerReceiver(navigateGpxReceiver, new IntentFilter(AIDL_NAVIGATE_GPX));
	}

	private void startNavigation(MapActivity mapActivity,
								 GPXFile gpx,
								 LatLon from, PointDescription fromDesc,
								 LatLon to, PointDescription toDesc,
								 ApplicationMode mode) {
		OsmandApplication app = mapActivity.getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (gpx == null) {
			app.getSettings().APPLICATION_MODE.set(mode);
			final TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
			targets.removeAllWayPoints(false, true);
			targets.navigateToPoint(to, true, -1, toDesc);
		}
		mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(gpx, from, fromDesc, true, false);
		if (!app.getTargetPointsHelper().checkPointToNavigateShort()) {
			mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu().show();
		} else {
			if (app.getSettings().APPLICATION_MODE.get() != routingHelper.getAppMode()) {
				app.getSettings().APPLICATION_MODE.set(routingHelper.getAppMode());
			}
			mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
			app.getSettings().FOLLOW_THE_ROUTE.set(true);
			routingHelper.setFollowingMode(true);
			routingHelper.setRoutePlanningMode(false);
			mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
			app.getRoutingHelper().notifyIfRouteIsCalculated();
			routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
		}
	}

	public void registerMapLayers(MapActivity mapActivity) {
		for (AMapLayer layer : layers.values()) {
			OsmandMapLayer mapLayer = mapLayers.get(layer.getId());
			if (mapLayer != null) {
				mapActivity.getMapView().removeLayer(mapLayer);
			}
			mapLayer = new AidlMapLayer(mapActivity, layer);
			mapActivity.getMapView().addLayer(mapLayer, layer.getZOrder());
			mapLayers.put(layer.getId(), mapLayer);
		}
	}

	private void refreshMap() {
		Intent intent = new Intent();
		intent.setAction(AIDL_REFRESH_MAP);
		app.sendBroadcast(intent);
	}

	private TextInfoWidget createWidgetControl(final MapActivity mapActivity, final String widgetId) {
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

	boolean reloadMap() {
		refreshMap();
		return true;
	}

	boolean addFavoriteGroup(AFavoriteGroup favoriteGroup) {
		if (favoriteGroup != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritesDbHelper.FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
			for (FavouritesDbHelper.FavoriteGroup g : groups) {
				if (g.name.equals(favoriteGroup.getName())) {
					return false;
				}
			}
			int color = 0;
			if (!Algorithms.isEmpty(favoriteGroup.getColor())) {
				color = ColorDialogs.getColorByTag(favoriteGroup.getColor());
			}
			favoritesHelper.addEmptyCategory(favoriteGroup.getName(), color, favoriteGroup.isVisible());
			return true;
		} else {
			return false;
		}
	}

	boolean removeFavoriteGroup(AFavoriteGroup favoriteGroup) {
		if (favoriteGroup != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritesDbHelper.FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
			for (FavouritesDbHelper.FavoriteGroup g : groups) {
				if (g.name.equals(favoriteGroup.getName())) {
					favoritesHelper.deleteGroup(g);
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean updateFavoriteGroup(AFavoriteGroup gPrev, AFavoriteGroup gNew) {
		if (gPrev != null && gNew != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritesDbHelper.FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
			for (FavouritesDbHelper.FavoriteGroup g : groups) {
				if (g.name.equals(gPrev.getName())) {
					int color = 0;
					if (!Algorithms.isEmpty(gNew.getColor())) {
						color = ColorDialogs.getColorByTag(gNew.getColor());
					}
					favoritesHelper.editFavouriteGroup(g, gNew.getName(), color, gNew.isVisible());
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean addFavorite(AFavorite favorite) {
		if (favorite != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			FavouritePoint point = new FavouritePoint(favorite.getLat(), favorite.getLon(), favorite.getName(), favorite.getCategory());
			point.setDescription(favorite.getDescription());
			int color = 0;
			if (!Algorithms.isEmpty(favorite.getColor())) {
				color = ColorDialogs.getColorByTag(favorite.getColor());
			}
			point.setColor(color);
			point.setVisible(favorite.isVisible());
			favoritesHelper.addFavourite(point);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeFavorite(AFavorite favorite) {
		if (favorite != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritePoint> favorites = favoritesHelper.getFavouritePoints();
			for (FavouritePoint f : favorites) {
				if (f.getName().equals(favorite.getName()) && f.getCategory().equals(favorite.getCategory()) &&
						f.getLatitude() == favorite.getLat() && f.getLongitude() == favorite.getLon()) {
					favoritesHelper.deleteFavourite(f);
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean updateFavorite(AFavorite fPrev, AFavorite fNew) {
		if (fPrev != null && fNew != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritePoint> favorites = favoritesHelper.getFavouritePoints();
			for (FavouritePoint f : favorites) {
				if (f.getName().equals(fPrev.getName()) && f.getCategory().equals(fPrev.getCategory()) &&
						f.getLatitude() == fPrev.getLat() && f.getLongitude() == fPrev.getLon()) {
					if (fNew.getLat() != f.getLatitude() || fNew.getLon() != f.getLongitude()) {
						favoritesHelper.editFavourite(f, fNew.getLat(), fNew.getLon());
					}
					if (!fNew.getName().equals(f.getName()) || !fNew.getDescription().equals(f.getDescription()) ||
							!fNew.getCategory().equals(f.getCategory())) {
						favoritesHelper.editFavouriteName(f, fNew.getName(), fNew.getCategory(), fNew.getDescription());
					}
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
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
					markersHelper.moveMapMarkerToHistory(m);
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
					marker.id = m.id;
					marker.creationDate = m.creationDate;
					marker.visitedDate = m.visitedDate;
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
				intent.putExtra(AIDL_OBJECT_ID, widget.getId());
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
			intent.putExtra(AIDL_OBJECT_ID, widgetId);
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

	boolean addMapLayer(AMapLayer layer) {
		if (layer != null) {
			if (layers.containsKey(layer.getId())) {
				updateMapLayer(layer);
			} else {
				layers.put(layer.getId(), layer);
				Intent intent = new Intent();
				intent.setAction(AIDL_ADD_MAP_LAYER);
				intent.putExtra(AIDL_OBJECT_ID, layer.getId());
				app.sendBroadcast(intent);
			}
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeMapLayer(String layerId) {
		if (!Algorithms.isEmpty(layerId) && layers.containsKey(layerId)) {
			layers.remove(layerId);
			Intent intent = new Intent();
			intent.setAction(AIDL_REMOVE_MAP_LAYER);
			intent.putExtra(AIDL_OBJECT_ID, layerId);
			app.sendBroadcast(intent);
			return true;
		} else {
			return false;
		}
	}

	boolean updateMapLayer(AMapLayer layer) {
		if (layer != null && layers.containsKey(layer.getId())) {
			layers.put(layer.getId(), layer);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean putMapPoint(String layerId, AMapPoint point) {
		if (point != null) {
			AMapLayer layer = layers.get(layerId);
			if (layer != null) {
				layer.putPoint(point);
				refreshMap();
				return true;
			}
		}
		return false;
	}

	boolean removeMapPoint(String layerId, String pointId) {
		if (pointId != null) {
			AMapLayer layer = layers.get(layerId);
			if (layer != null) {
				layer.removePoint(pointId);
				refreshMap();
				return true;
			}
		}
		return false;
	}

	private void finishGpxImport(boolean destinationExists, File destination, String color, boolean show) {
		int col = ConfigureMapMenu.GpxAppearanceAdapter.parseTrackColor(
					app.getRendererRegistry().getCurrentSelectedRenderer(), color);
		if (!destinationExists) {
			GpxDataItem gpxDataItem = new GpxDataItem(destination, col);
			gpxDataItem.setApiImported(true);
			app.getGpxDatabase().add(gpxDataItem);
		} else {
			GpxDataItem item = app.getGpxDatabase().getItem(destination);
			if (item != null) {
				app.getGpxDatabase().updateColor(item, col);
			}
		}
		final GpxSelectionHelper helper = app.getSelectedGpxHelper();
		final SelectedGpxFile selectedGpx = helper.getSelectedFileByName(destination.getName());
		if (selectedGpx != null) {
			if (show) {
				new AsyncTask<File, Void, GPXFile>() {

					@Override
					protected GPXFile doInBackground(File... files) {
						return GPXUtilities.loadGPXFile(app, files[0]);
					}

					@Override
					protected void onPostExecute(GPXFile gpx) {
						if (gpx.warning == null) {
							selectedGpx.setGpxFile(gpx);
							refreshMap();
						}
					}

				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, destination);
			} else {
				helper.selectGpxFile(selectedGpx.getGpxFile(), false, false);
				refreshMap();
			}
		} else if (show) {
			new AsyncTask<File, Void, GPXFile>() {

				@Override
				protected GPXFile doInBackground(File... files) {
					return GPXUtilities.loadGPXFile(app, files[0]);
				}

				@Override
				protected void onPostExecute(GPXFile gpx) {
					if (gpx.warning == null) {
						helper.selectGpxFile(gpx, true, false);
						refreshMap();
					}
				}

			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, destination);
		}
	}

	boolean importGpxFromFile(File source, String destinationPath, String color, boolean show) {
		if (source != null && !Algorithms.isEmpty(destinationPath)) {
			if (source.exists() && source.canRead()) {
				File destination = app.getAppPath(IndexConstants.GPX_INDEX_DIR + destinationPath);
				if (destination.getParentFile().canWrite()) {
					boolean destinationExists = destination.exists();
					try {
						Algorithms.fileCopy(source, destination);
						finishGpxImport(destinationExists, destination, color, show);
						return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

	boolean importGpxFromUri(Uri gpxUri, String destinationPath, String color, boolean show) {
		if (gpxUri != null && !Algorithms.isEmpty(destinationPath)) {
			File destination = app.getAppPath(IndexConstants.GPX_INDEX_DIR + destinationPath);
			ParcelFileDescriptor gpxParcelDescriptor;
			try {
				gpxParcelDescriptor = app.getContentResolver().openFileDescriptor(gpxUri, "r");
				if (gpxParcelDescriptor != null) {
					boolean destinationExists = destination.exists();
					FileDescriptor fileDescriptor = gpxParcelDescriptor.getFileDescriptor();
					InputStream is = new FileInputStream(fileDescriptor);
					FileOutputStream fout = new FileOutputStream(destination);
					try {
						Algorithms.streamCopy(is, fout);
						finishGpxImport(destinationExists, destination, color, show);
					} finally {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						try {
							fout.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	boolean importGpxFromData(String sourceRawData, String destinationPath, String color, boolean show) {
		if (!Algorithms.isEmpty(sourceRawData) && !Algorithms.isEmpty(destinationPath)) {
			File destination = app.getAppPath(IndexConstants.GPX_INDEX_DIR + destinationPath);
			try {
				InputStream is = new ByteArrayInputStream(sourceRawData.getBytes());
				FileOutputStream fout = new FileOutputStream(destination);
				boolean destinationExists = destination.exists();
				try {
					Algorithms.streamCopy(is, fout);
					finishGpxImport(destinationExists, destination, color, show);
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						fout.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	boolean showGpx(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			File f = app.getAppPath(IndexConstants.GPX_INDEX_DIR + fileName);
			if (f.exists()) {
				new AsyncTask<File, Void, GPXFile>() {

					@Override
					protected GPXFile doInBackground(File... files) {
						return GPXUtilities.loadGPXFile(app, files[0]);
					}

					@Override
					protected void onPostExecute(GPXFile gpx) {
						if (gpx.warning == null) {
							app.getSelectedGpxHelper().selectGpxFile(gpx, true, false);
							refreshMap();
						}
					}

				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, f);

				return true;
			}
		}
		return false;
	}

	boolean hideGpx(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByName(fileName);
			if (selectedGpxFile != null) {
				app.getSelectedGpxHelper().selectGpxFile(selectedGpxFile.getGpxFile(), false, false);
				refreshMap();
				return true;
			}
		}
		return false;
	}

	boolean getActiveGpx(List<ASelectedGpxFile> files) {
		if (files != null) {
			List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
			String gpxPath = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath();
			for (SelectedGpxFile selectedGpxFile : selectedGpxFiles) {
				String path = selectedGpxFile.getGpxFile().path;
				if (!Algorithms.isEmpty(path)) {
					if (path.startsWith(gpxPath)) {
						path = path.substring(gpxPath.length() + 1);
					}
					files.add(new ASelectedGpxFile(path));
				}
			}
			return true;
		}
		return false;
	}

	boolean removeGpx(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			final File f = app.getAppPath(IndexConstants.GPX_INDEX_DIR + fileName);
			if (f.exists()) {
				GpxDataItem item = app.getGpxDatabase().getItem(f);
				if (item != null && item.isApiImported()) {
					Algorithms.removeAllFiles(f);
					app.getGpxDatabase().remove(f);
					return true;
				}
			}
		}
		return false;
	}

	boolean setMapLocation(double latitude, double longitude, int zoom, boolean animated) {
		Intent intent = new Intent();
		intent.setAction(AIDL_SET_MAP_LOCATION);
		intent.putExtra(AIDL_LATITUDE, latitude);
		intent.putExtra(AIDL_LONGITUDE, longitude);
		intent.putExtra(AIDL_ZOOM, zoom);
		intent.putExtra(AIDL_ANIMATED, animated);
		app.sendBroadcast(intent);
		return true;
	}

	boolean startGpxRecording(StartGpxRecordingParams params) {
		final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			plugin.startGPXMonitoring(null);
			plugin.updateControl();
			return true;
		}
		return false;
	}

	boolean stopGpxRecording(StopGpxRecordingParams params) {
		final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			plugin.stopRecording();
			plugin.updateControl();
			return true;
		}
		return false;
	}

	boolean takePhotoNote(double latitude, double longitude) {
		Intent intent = new Intent();
		intent.setAction(AIDL_TAKE_PHOTO_NOTE);
		intent.putExtra(AIDL_LATITUDE, latitude);
		intent.putExtra(AIDL_LONGITUDE, longitude);
		app.sendBroadcast(intent);
		return true;
	}

	boolean startVideoRecording(double latitude, double longitude) {
		Intent intent = new Intent();
		intent.setAction(AIDL_START_VIDEO_RECORDING);
		intent.putExtra(AIDL_LATITUDE, latitude);
		intent.putExtra(AIDL_LONGITUDE, longitude);
		app.sendBroadcast(intent);
		return true;
	}

	boolean startAudioRecording(double latitude, double longitude) {
		Intent intent = new Intent();
		intent.setAction(AIDL_START_AUDIO_RECORDING);
		intent.putExtra(AIDL_LATITUDE, latitude);
		intent.putExtra(AIDL_LONGITUDE, longitude);
		app.sendBroadcast(intent);
		return true;
	}

	boolean stopRecording() {
		Intent intent = new Intent();
		intent.setAction(AIDL_STOP_RECORDING);
		app.sendBroadcast(intent);
		return true;
	}

	boolean navigate(String startName, double startLat, double startLon, String destName, double destLat, double destLon, String profile, boolean force) {
		Intent intent = new Intent();
		intent.setAction(AIDL_NAVIGATE);
		intent.putExtra(AIDL_START_NAME, startName);
		intent.putExtra(AIDL_START_LAT, startLat);
		intent.putExtra(AIDL_START_LON, startLon);
		intent.putExtra(AIDL_DEST_NAME, destName);
		intent.putExtra(AIDL_DEST_LAT, destLat);
		intent.putExtra(AIDL_DEST_LON, destLon);
		intent.putExtra(AIDL_PROFILE, profile);
		intent.putExtra(AIDL_FORCE, force);
		app.sendBroadcast(intent);
		return true;
	}

	boolean navigateGpx(String data, Uri uri, boolean force) {
		Intent intent = new Intent();
		intent.setAction(AIDL_NAVIGATE_GPX);
		intent.putExtra(AIDL_DATA, data);
		intent.putExtra(AIDL_URI, uri);
		intent.putExtra(AIDL_FORCE, force);
		app.sendBroadcast(intent);
		return true;
	}
}
