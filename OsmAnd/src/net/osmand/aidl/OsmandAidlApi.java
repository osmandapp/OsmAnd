package net.osmand.aidl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.aidl.gpx.AGpxFile;
import net.osmand.aidl.gpx.AGpxFileDetails;
import net.osmand.aidl.gpx.ASelectedGpxFile;
import net.osmand.aidl.navigation.ADirectionInfo;
import net.osmand.aidl.navigation.OnVoiceNavigationParams;
import net.osmand.aidl.tiles.ASqliteDbFile;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.SettingsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.helpers.ExternalApiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.IContextMenuButtonListener;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.myplaces.TrackBitmapDrawer;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.routing.IRoutingDataUpdateListener;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.plus.views.AidlMapLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static net.osmand.aidl.ConnectedApp.AIDL_OBJECT_ID;
import static net.osmand.aidl.ConnectedApp.AIDL_PACKAGE_NAME;
import static net.osmand.aidl.ConnectedApp.AIDL_ADD_MAP_LAYER;
import static net.osmand.aidl.ConnectedApp.AIDL_ADD_MAP_WIDGET;
import static net.osmand.aidl.ConnectedApp.AIDL_REMOVE_MAP_LAYER;
import static net.osmand.aidl.ConnectedApp.AIDL_REMOVE_MAP_WIDGET;

import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_IO_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_MAX_LOCK_TIME_MS;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_PARAMS_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_PART_SIZE_LIMIT;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_PART_SIZE_LIMIT_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_UNSUPPORTED_FILE_TYPE_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_WRITE_LOCK_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.OK_RESPONSE;

public class OsmandAidlApi {

	AidlCallbackListener aidlCallbackListener = null;
	AidlCallbackListenerV2 aidlCallbackListenerV2 = null;

	public static final int KEY_ON_UPDATE = 1;
	public static final int KEY_ON_NAV_DATA_UPDATE = 2;
	public static final int KEY_ON_CONTEXT_MENU_BUTTONS_CLICK = 4;
	public static final int KEY_ON_VOICE_MESSAGE = 5;

	private static final Log LOG = PlatformUtil.getLog(OsmandAidlApi.class);

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
	private static final String AIDL_SEARCH_QUERY = "aidl_search_query";
	private static final String AIDL_SEARCH_LAT = "aidl_search_lat";
	private static final String AIDL_SEARCH_LON = "aidl_search_lon";

	private static final String AIDL_ADD_CONTEXT_MENU_BUTTONS = "aidl_add_context_menu_buttons";
	private static final String AIDL_REMOVE_CONTEXT_MENU_BUTTONS = "aidl_remove_context_menu_buttons";

	private static final String AIDL_TAKE_PHOTO_NOTE = "aidl_take_photo_note";
	private static final String AIDL_START_VIDEO_RECORDING = "aidl_start_video_recording";
	private static final String AIDL_START_AUDIO_RECORDING = "aidl_start_audio_recording";
	private static final String AIDL_STOP_RECORDING = "aidl_stop_recording";

	private static final String AIDL_NAVIGATE = "aidl_navigate";
	private static final String AIDL_NAVIGATE_GPX = "aidl_navigate_gpx";
	private static final String AIDL_NAVIGATE_SEARCH = "aidl_navigate_search";
	private static final String AIDL_PAUSE_NAVIGATION = "pause_navigation";
	private static final String AIDL_RESUME_NAVIGATION = "resume_navigation";
	private static final String AIDL_STOP_NAVIGATION = "stop_navigation";
	private static final String AIDL_MUTE_NAVIGATION = "mute_navigation";
	private static final String AIDL_UNMUTE_NAVIGATION = "unmute_navigation";

	private static final String AIDL_SHOW_SQLITEDB_FILE = "aidl_show_sqlitedb_file";
	private static final String AIDL_HIDE_SQLITEDB_FILE = "aidl_hide_sqlitedb_file";
	private static final String AIDL_FILE_NAME = "aidl_file_name";


	private static final ApplicationMode DEFAULT_PROFILE = ApplicationMode.CAR;

	private static final ApplicationMode[] VALID_PROFILES = new ApplicationMode[] {
			ApplicationMode.CAR,
			ApplicationMode.BICYCLE,
			ApplicationMode.PEDESTRIAN
	};

	private static final int DEFAULT_ZOOM = 15;

	private OsmandApplication app;
	private Map<String, BroadcastReceiver> receivers = new TreeMap<>();
	private Map<String, ConnectedApp> connectedApps = new ConcurrentHashMap<>();
	private Map<String, AidlContextMenuButtonsWrapper> contextMenuButtonsParams = new ConcurrentHashMap<>();
	private Map<Long, VoiceRouter.VoiceMessageListener> voiceRouterMessageCallbacks = new ConcurrentHashMap<>();

	private AMapPointUpdateListener aMapPointUpdateListener;

	private boolean mapActivityActive = false;

	public OsmandAidlApi(OsmandApplication app) {
		this.app = app;
		loadConnectedApps();
	}

	public void onCreateMapActivity(MapActivity mapActivity) {
		mapActivityActive = true;
		registerRefreshMapReceiver(mapActivity);
		registerSetMapLocationReceiver(mapActivity);
		registerAddMapWidgetReceiver(mapActivity);
		registerAddContextMenuButtonsReceiver(mapActivity);
		registerRemoveMapWidgetReceiver(mapActivity);
		registerAddMapLayerReceiver(mapActivity);
		registerRemoveMapLayerReceiver(mapActivity);
		registerTakePhotoNoteReceiver(mapActivity);
		registerStartVideoRecordingReceiver(mapActivity);
		registerStartAudioRecordingReceiver(mapActivity);
		registerStopRecordingReceiver(mapActivity);
		registerNavigateReceiver(mapActivity);
		registerNavigateGpxReceiver(mapActivity);
		registerNavigateSearchReceiver(mapActivity);
		registerPauseNavigationReceiver(mapActivity);
		registerResumeNavigationReceiver(mapActivity);
		registerStopNavigationReceiver(mapActivity);
		registerMuteNavigationReceiver(mapActivity);
		registerUnmuteNavigationReceiver(mapActivity);
		registerShowSqliteDbFileReceiver(mapActivity);
		registerHideSqliteDbFileReceiver(mapActivity);
		initOsmandTelegram();
		app.getAppCustomization().addListener(mapActivity);
		aMapPointUpdateListener = mapActivity;
	}

	public void onDestroyMapActivity(MapActivity mapActivity) {
		app.getAppCustomization().removeListener(mapActivity);
		aMapPointUpdateListener = null;
		mapActivityActive = false;
		for (BroadcastReceiver b : receivers.values()) {
			if (b == null) {
				continue;
			}
			try {
				mapActivity.unregisterReceiver(b);
			} catch (IllegalArgumentException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		receivers = new TreeMap<>();
	}

	public boolean isUpdateAllowed() {
		return mapActivityActive;
	}

	AMapPointUpdateListener getAMapPointUpdateListener() {
		return aMapPointUpdateListener;
	}

	private void initOsmandTelegram() {
		String[] packages = new String[] {"net.osmand.telegram", "net.osmand.telegram.debug"};
		Intent intent = new Intent("net.osmand.telegram.InitApp");
		for (String pack : packages) {
			intent.setComponent(new ComponentName(pack, "net.osmand.telegram.InitAppBroadcastReceiver"));
			app.sendBroadcast(intent);
		}
	}

	private void registerRefreshMapReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver refreshMapReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					mapActivity.refreshMap();
				}
			}
		};
		registerReceiver(refreshMapReceiver, mapActivity, AIDL_REFRESH_MAP);
	}

	private void registerSetMapLocationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver setMapLocationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
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
			}
		};
		registerReceiver(setMapLocationReceiver, mapActivity, AIDL_SET_MAP_LOCATION);
	}

	private void registerAddMapWidgetReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver addMapWidgetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String widgetId = intent.getStringExtra(AIDL_OBJECT_ID);
				String packName = intent.getStringExtra(AIDL_PACKAGE_NAME);
				if (mapActivity != null && widgetId != null && packName != null) {
					ConnectedApp connectedApp = connectedApps.get(packName);
					if (connectedApp != null) {
						AidlMapWidgetWrapper widget = connectedApp.getWidgets().get(widgetId);
						MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
						if (widget != null && layer != null) {
							ApplicationMode.regWidgetVisibility(widget.getId(), (ApplicationMode[]) null);
							TextInfoWidget control = connectedApp.createWidgetControl(mapActivity, widgetId);
							connectedApp.getWidgetControls().put(widgetId, control);
							int menuIconId = AndroidUtils.getDrawableId(app, widget.getMenuIconName());
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
		registerReceiver(addMapWidgetReceiver, mapActivity, AIDL_ADD_MAP_WIDGET);
	}

	private void registerAddContextMenuButtonsReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver addContextMenuButtonsParamsReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String ContextMenuButtonsParamsId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (mapActivity != null && ContextMenuButtonsParamsId != null) {
					AidlContextMenuButtonsWrapper buttonsParams = contextMenuButtonsParams.get(ContextMenuButtonsParamsId);
					if (buttonsParams != null) {
						MapContextMenu mapContextMenu = mapActivity.getContextMenu();
						if (mapContextMenu.isVisible()) {
							mapContextMenu.updateData();
						}
					}
				}
			}
		};
		registerReceiver(addContextMenuButtonsParamsReceiver, mapActivity, AIDL_ADD_CONTEXT_MENU_BUTTONS);
	}

	private void registerReceiver(BroadcastReceiver rec, MapActivity ma, String filter) {
		receivers.put(filter, rec);
		ma.registerReceiver(rec, new IntentFilter(filter));
	}

	private void registerRemoveMapWidgetReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver removeMapWidgetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String widgetId = intent.getStringExtra(AIDL_OBJECT_ID);
				String packName = intent.getStringExtra(AIDL_PACKAGE_NAME);
				if (mapActivity != null && widgetId != null && packName != null) {
					ConnectedApp connectedApp = connectedApps.get(packName);
					if (connectedApp != null) {
						MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
						TextInfoWidget widgetControl = connectedApp.getWidgetControls().get(widgetId);
						if (layer != null && widgetControl != null) {
							layer.removeSideWidget(widgetControl);
							connectedApp.getWidgetControls().remove(widgetId);
							layer.recreateControls();
						}
					}
				}
			}
		};
		registerReceiver(removeMapWidgetReceiver, mapActivity, AIDL_REMOVE_MAP_WIDGET);
	}

	public void registerWidgetControls(MapActivity mapActivity) {
		for (ConnectedApp connectedApp : connectedApps.values()) {
			connectedApp.registerWidgetControls(mapActivity);
		}
	}

	private void registerAddMapLayerReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver addMapLayerReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String layerId = intent.getStringExtra(AIDL_OBJECT_ID);
				String packName = intent.getStringExtra(AIDL_PACKAGE_NAME);
				if (mapActivity != null && layerId != null && packName != null) {
					ConnectedApp connectedApp = connectedApps.get(packName);
					if (connectedApp != null) {
						AidlMapLayerWrapper layer = connectedApp.getLayers().get(layerId);
						if (layer != null) {
							OsmandMapLayer mapLayer = connectedApp.getMapLayers().get(layerId);
							if (mapLayer != null) {
								mapActivity.getMapView().removeLayer(mapLayer);
							}
							mapLayer = new AidlMapLayer(mapActivity, layer, connectedApp.getPack());
							mapActivity.getMapView().addLayer(mapLayer, layer.getZOrder());
							connectedApp.getMapLayers().put(layerId, mapLayer);
						}
					}
				}
			}
		};
		registerReceiver(addMapLayerReceiver, mapActivity, AIDL_ADD_MAP_LAYER);
	}

	private void registerRemoveMapLayerReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver removeMapLayerReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String layerId = intent.getStringExtra(AIDL_OBJECT_ID);
				String packName = intent.getStringExtra(AIDL_PACKAGE_NAME);
				if (mapActivity != null && layerId != null && packName != null) {
					ConnectedApp connectedApp = connectedApps.get(packName);
					if (connectedApp != null) {
						OsmandMapLayer mapLayer = connectedApp.getMapLayers().remove(layerId);
						if (mapLayer != null) {
							mapActivity.getMapView().removeLayer(mapLayer);
							mapActivity.refreshMap();
						}
					}
				}
			}
		};
		registerReceiver(removeMapLayerReceiver, mapActivity, AIDL_REMOVE_MAP_LAYER);
	}

	private void registerTakePhotoNoteReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver takePhotoNoteReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.takePhoto(lat, lon, mapActivity, false, true);
				}
			}
		};
		registerReceiver(takePhotoNoteReceiver, mapActivity, AIDL_TAKE_PHOTO_NOTE);
	}

	private void registerStartVideoRecordingReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver startVideoRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.recordVideo(lat, lon, mapActivity, true);
				}
			}
		};
		registerReceiver(startVideoRecordingReceiver, mapActivity, AIDL_START_VIDEO_RECORDING);
	}

	private void registerStartAudioRecordingReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver startAudioRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.recordAudio(lat, lon, mapActivity);
				}
			}
		};
		registerReceiver(startAudioRecordingReceiver, mapActivity, AIDL_START_AUDIO_RECORDING);
	}

	private void registerStopRecordingReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver stopRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					plugin.stopRecording(mapActivity, false);
				}
			}
		};
		registerReceiver(stopRecordingReceiver, mapActivity, AIDL_STOP_RECORDING);
	}

	private void registerNavigateReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver navigateReceiver = new BroadcastReceiver() {
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
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null && validProfile) {
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
								MapActivity mapActivity = mapActivityRef.get();
								if (mapActivity != null && !routingHelper.isFollowingMode()) {
									ExternalApiHelper.startNavigation(mapActivity, start, startDesc, dest, destDesc, profile);
								}
							}
						});
					} else {
						ExternalApiHelper.startNavigation(mapActivity, start, startDesc, dest, destDesc, profile);
					}
				}
			}
		};
		registerReceiver(navigateReceiver, mapActivity, AIDL_NAVIGATE);
	}

	private void registerNavigateSearchReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver navigateSearchReceiver = new BroadcastReceiver() {
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
				MapActivity mapActivity = mapActivityRef.get();
				final String searchQuery = intent.getStringExtra(AIDL_SEARCH_QUERY);
				if (mapActivity != null && validProfile && !Algorithms.isEmpty(searchQuery)) {
					String startName = intent.getStringExtra(AIDL_START_NAME);
					if (Algorithms.isEmpty(startName)) {
						startName = "";
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

					final LatLon searchLocation;
					double searchLat = intent.getDoubleExtra(AIDL_SEARCH_LAT, 0);
					double searchLon = intent.getDoubleExtra(AIDL_SEARCH_LON, 0);
					if (searchLat != 0 && searchLon != 0) {
						searchLocation = new LatLon(searchLat, searchLon);
					} else {
						searchLocation = null;
					}

					if (searchLocation != null) {
						final RoutingHelper routingHelper = app.getRoutingHelper();
						boolean force = intent.getBooleanExtra(AIDL_FORCE, true);
						if (routingHelper.isFollowingMode() && !force) {
							AlertDialog dlg = mapActivity.getMapActions().stopNavigationActionConfirm();
							dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

								@Override
								public void onDismiss(DialogInterface dialog) {
									MapActivity mapActivity = mapActivityRef.get();
									if (mapActivity != null && !routingHelper.isFollowingMode()) {
										ExternalApiHelper.searchAndNavigate(mapActivity, searchLocation, start, startDesc, profile, searchQuery, false);
									}
								}
							});
						} else {
							ExternalApiHelper.searchAndNavigate(mapActivity, searchLocation, start, startDesc, profile, searchQuery, false);
						}
					}
				}
			}
		};
		registerReceiver(navigateSearchReceiver, mapActivity, AIDL_NAVIGATE_SEARCH);
	}

	private void registerNavigateGpxReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver navigateGpxReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					boolean force = intent.getBooleanExtra(AIDL_FORCE, false);
					GPXFile gpx = null;
					if (intent.getStringExtra(AIDL_DATA) != null) {
						String gpxStr = intent.getStringExtra(AIDL_DATA);
						if (!Algorithms.isEmpty(gpxStr)) {
							gpx = GPXUtilities.loadGPXFile(new ByteArrayInputStream(gpxStr.getBytes()));
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
								gpx = GPXUtilities.loadGPXFile(new FileInputStream(fileDescriptor));
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
									MapActivity mapActivity = mapActivityRef.get();
									if (mapActivity != null && !routingHelper.isFollowingMode()) {
										ExternalApiHelper.startNavigation(mapActivity, gpxFile);
									}
								}
							});
						} else {
							ExternalApiHelper.startNavigation(mapActivity, gpx);
						}
					}
				}
			}
		};
		registerReceiver(navigateGpxReceiver, mapActivity, AIDL_NAVIGATE_GPX);
	}

	private void registerPauseNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver pauseNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					RoutingHelper routingHelper = mapActivity.getRoutingHelper();
					if (routingHelper.isRouteCalculated() && !routingHelper.isRoutePlanningMode()) {
						routingHelper.setRoutePlanningMode(true);
						routingHelper.setFollowingMode(false);
						routingHelper.setPauseNavigation(true);
					}
				}
			}
		};
		registerReceiver(pauseNavigationReceiver, mapActivity, AIDL_PAUSE_NAVIGATION);
	}

	private void registerResumeNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver resumeNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					RoutingHelper routingHelper = mapActivity.getRoutingHelper();
					if (routingHelper.isRouteCalculated() && routingHelper.isRoutePlanningMode()) {
						routingHelper.setRoutePlanningMode(false);
						routingHelper.setFollowingMode(true);
					}
				}
			}
		};
		registerReceiver(resumeNavigationReceiver, mapActivity, AIDL_RESUME_NAVIGATION);
	}

	private void registerStopNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver stopNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					RoutingHelper routingHelper = mapActivity.getRoutingHelper();
					if (routingHelper.isPauseNavigation() || routingHelper.isFollowingMode()) {
						mapActivity.getMapLayers().getMapControlsLayer().stopNavigationWithoutConfirm();
					}
				}
			}
		};
		registerReceiver(stopNavigationReceiver, mapActivity, AIDL_STOP_NAVIGATION);
	}

	private void registerMuteNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver muteNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					mapActivity.getRoutingHelper().getVoiceRouter().setMute(true);
				}
			}
		};
		registerReceiver(muteNavigationReceiver, mapActivity, AIDL_MUTE_NAVIGATION);
	}

	private void registerUnmuteNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver unmuteNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					mapActivity.getRoutingHelper().getVoiceRouter().setMute(false);
				}
			}
		};
		registerReceiver(unmuteNavigationReceiver, mapActivity, AIDL_UNMUTE_NAVIGATION);
	}

	private void registerShowSqliteDbFileReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver showSqliteDbFileReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				OsmandSettings settings = app.getSettings();
				String fileName = intent.getStringExtra(AIDL_FILE_NAME);
				if (!Algorithms.isEmpty(fileName)) {
					settings.MAP_OVERLAY.set(fileName);
					settings.MAP_OVERLAY_PREVIOUS.set(fileName);
					MapActivity mapActivity = mapActivityRef.get();
					if (mapActivity != null) {
						OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
						if (plugin != null) {
							plugin.updateMapLayers(mapActivity.getMapView(), settings.MAP_OVERLAY, mapActivity.getMapLayers());
						}
					}
				}
			}
		};
		registerReceiver(showSqliteDbFileReceiver, mapActivity, AIDL_SHOW_SQLITEDB_FILE);
	}

	private void registerHideSqliteDbFileReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver hideSqliteDbFileReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				OsmandSettings settings = app.getSettings();
				String fileName = intent.getStringExtra(AIDL_FILE_NAME);
				if (!Algorithms.isEmpty(fileName) && fileName.equals(settings.MAP_OVERLAY.get())) {
					settings.MAP_OVERLAY.set(null);
					settings.MAP_OVERLAY_PREVIOUS.set(null);
					MapActivity mapActivity = mapActivityRef.get();
					if (mapActivity != null) {
						OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
						if (plugin != null) {
							plugin.updateMapLayers(mapActivity.getMapView(), settings.MAP_OVERLAY, mapActivity.getMapLayers());
						}
					}
				}
			}
		};
		registerReceiver(hideSqliteDbFileReceiver, mapActivity, AIDL_HIDE_SQLITEDB_FILE);
	}

	public void registerMapLayers(@NonNull MapActivity mapActivity) {
		for (ConnectedApp connectedApp : connectedApps.values()) {
			connectedApp.registerMapLayers(mapActivity);
		}
	}

	private void refreshMap() {
		Intent intent = new Intent();
		intent.setAction(AIDL_REFRESH_MAP);
		app.sendBroadcast(intent);
	}

	boolean reloadMap() {
		refreshMap();
		return true;
	}

	boolean addFavoriteGroup(String name, String colorTag, boolean visible) {
		FavouritesDbHelper favoritesHelper = app.getFavorites();
		List<FavouritesDbHelper.FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
		for (FavouritesDbHelper.FavoriteGroup g : groups) {
			if (g.name.equals(name)) {
				return false;
			}
		}
		int color = 0;
		if (!Algorithms.isEmpty(colorTag)) {
			color = ColorDialogs.getColorByTag(colorTag);
		}
		favoritesHelper.addEmptyCategory(name, color, visible);
		return true;
	}

	boolean removeFavoriteGroup(String name) {
		FavouritesDbHelper favoritesHelper = app.getFavorites();
		List<FavouritesDbHelper.FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
		for (FavouritesDbHelper.FavoriteGroup g : groups) {
			if (g.name.equals(name)) {
				favoritesHelper.deleteGroup(g);
				return true;
			}
		}
		return false;
	}

	boolean updateFavoriteGroup(String prevGroupName, String newGroupName, String colorTag, boolean visible) {
		FavouritesDbHelper favoritesHelper = app.getFavorites();
		List<FavouritesDbHelper.FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
		for (FavouritesDbHelper.FavoriteGroup g : groups) {
			if (g.name.equals(prevGroupName)) {
				int color = 0;
				if (!Algorithms.isEmpty(colorTag)) {
					color = ColorDialogs.getColorByTag(colorTag);
				}
				favoritesHelper.editFavouriteGroup(g, newGroupName, color, visible);
				return true;
			}
		}
		return false;
	}

	boolean addFavorite(double latitude, double longitude, String name, String category, String description, String colorTag, boolean visible) {
		FavouritesDbHelper favoritesHelper = app.getFavorites();
		FavouritePoint point = new FavouritePoint(latitude, longitude, name, category);
		point.setDescription(description);
		int color = 0;
		if (!Algorithms.isEmpty(colorTag)) {
			color = ColorDialogs.getColorByTag(colorTag);
		}
		point.setColor(color);
		point.setVisible(visible);
		favoritesHelper.addFavourite(point);
		refreshMap();
		return true;
	}

	boolean removeFavorite(String name, String category, double latitude, double longitude) {
		FavouritesDbHelper favoritesHelper = app.getFavorites();
		List<FavouritePoint> favorites = favoritesHelper.getFavouritePoints();
		for (FavouritePoint f : favorites) {
			if (f.getName().equals(name) && f.getCategory().equals(category) &&
					f.getLatitude() == latitude && f.getLongitude() == longitude) {
				favoritesHelper.deleteFavourite(f);
				refreshMap();
				return true;
			}
		}
		return false;
	}

	boolean updateFavorite(String prevName, String prevCategory, double prevLat, double prevLon, String newName, String newCategory, String newDescription, double newLat, double newLon) {
		FavouritesDbHelper favoritesHelper = app.getFavorites();
		List<FavouritePoint> favorites = favoritesHelper.getFavouritePoints();
		for (FavouritePoint f : favorites) {
			if (f.getName().equals(prevName) && f.getCategory().equals(prevCategory) &&
					f.getLatitude() == prevLat && f.getLongitude() == prevLon) {
				if (newLat != f.getLatitude() || newLon != f.getLongitude()) {
					favoritesHelper.editFavourite(f, newLat, newLon);
				}
				if (!newName.equals(f.getName()) || !newDescription.equals(f.getDescription()) ||
						!newCategory.equals(f.getCategory())) {
					favoritesHelper.editFavouriteName(f, newName, newCategory, newDescription);
				}
				refreshMap();
				return true;
			}
		}
		return false;
	}

	boolean addMapMarker(String name, double latitude, double longitude) {
		PointDescription pd = new PointDescription(
				PointDescription.POINT_TYPE_MAP_MARKER, name != null ? name : "");
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		markersHelper.addMapMarker(new LatLon(latitude, longitude), pd);
		refreshMap();
		return true;
	}

	boolean removeMapMarker(String name, double latitude, double longitude, boolean ignoreCoordinates) {
		LatLon latLon = new LatLon(latitude, longitude);
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<MapMarker> mapMarkers = markersHelper.getMapMarkers();
		for (MapMarker m : mapMarkers) {
			if (m.getOnlyName().equals(name)) {
				if (ignoreCoordinates || latLon.equals(new LatLon(m.getLatitude(), m.getLongitude()))) {
					markersHelper.moveMapMarkerToHistory(m);
					refreshMap();
					return true;
				}
			}
		}
		return false;
	}

	boolean removeAllActiveMapMarkers() {
		boolean refreshNeeded = false;
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<MapMarker> mapMarkers = markersHelper.getMapMarkers();
		for (MapMarker m : mapMarkers) {
			markersHelper.moveMapMarkerToHistory(m);
			refreshNeeded = true;
		}
		if (refreshNeeded) {
			refreshMap();
		}
		return true;
	}


	boolean updateMapMarker(String prevName, LatLon prevLatLon, String newName, LatLon newLatLon, boolean ignoreCoordinates) {
		LatLon latLon = new LatLon(prevLatLon.getLatitude(), prevLatLon.getLongitude());
		LatLon latLonNew = new LatLon(newLatLon.getLatitude(), newLatLon.getLongitude());
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<MapMarker> mapMarkers = markersHelper.getMapMarkers();
		for (MapMarker m : mapMarkers) {
			if (m.getOnlyName().equals(prevName)) {
				if (ignoreCoordinates || latLon.equals(new LatLon(m.getLatitude(), m.getLongitude()))) {
					PointDescription pd = new PointDescription(
							PointDescription.POINT_TYPE_MAP_MARKER, newName != null ? newName : "");
					MapMarker marker = new MapMarker(m.point, pd, m.colorIndex, m.selected, m.index);
					marker.id = m.id;
					marker.creationDate = m.creationDate;
					marker.visitedDate = m.visitedDate;
					markersHelper.moveMapMarker(marker, latLonNew);
					refreshMap();
					return true;
				}
			}
		}
		return false;
	}

	boolean addMapWidget(String packName, AidlMapWidgetWrapper widget) {
		if (widget != null) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null) {
				return connectedApp.addMapWidget(widget);
			}
		}
		return false;
	}

	boolean removeMapWidget(String packName, String widgetId) {
		if (!Algorithms.isEmpty(widgetId)) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null) {
				return connectedApp.removeMapWidget(widgetId);
			}
		}
		return false;
	}

	boolean updateMapWidget(String packName, AidlMapWidgetWrapper widget) {
		if (widget != null) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null) {
				return connectedApp.updateMapWidget(widget);
			}
		}
		return false;
	}

	boolean addMapLayer(String packName, AidlMapLayerWrapper layer) {
		if (layer != null) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null) {
				return connectedApp.addMapLayer(layer);
			}
		}
		return false;
	}

	boolean removeMapLayer(String packName, String layerId) {
		if (!Algorithms.isEmpty(layerId)) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null) {
				return connectedApp.removeMapLayer(layerId);
			}
		}
		return false;
	}

	boolean updateMapLayer(String packName, AidlMapLayerWrapper layer) {
		if (layer != null) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null) {
				return connectedApp.updateMapLayer(layer);
			}
		}
		return false;
	}

	boolean showMapPoint(String packName, String layerId, AidlMapPointWrapper point) {
		if (point != null) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null && !Algorithms.isEmpty(layerId)) {
				AidlMapLayerWrapper layer = connectedApp.getLayers().get(layerId);
				if (layer != null) {
					AidlMapPointWrapper p = layer.getPoint(point.getId());
					if (p != null) {
						point = p;
					}
				}
			}
			app.getSettings().setMapLocationToShow(
					point.getLocation().getLatitude(),
					point.getLocation().getLongitude(),
					DEFAULT_ZOOM,
					new PointDescription(PointDescription.POINT_TYPE_MARKER, point.getFullName()),
					false,
					point
			);
			MapActivity.launchMapActivityMoveToTop(app);

			return true;
		}
		return false;
	}

	boolean putMapPoint(String packName, String layerId, AidlMapPointWrapper point) {
		if (point != null) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null) {
				return connectedApp.putMapPoint(layerId, point);
			}
		}
		return false;
	}

	boolean updateMapPoint(String packName, String layerId, AidlMapPointWrapper point, boolean updateOpenedMenuAndMap) {
		if (point != null) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null) {
				return connectedApp.updateMapPoint(layerId, point, updateOpenedMenuAndMap);
			}
		}
		return false;
	}

	boolean removeMapPoint(String packName, String layerId, String pointId) {
		if (layerId != null && pointId != null) {
			ConnectedApp connectedApp = connectedApps.get(packName);
			if (connectedApp != null) {
				return connectedApp.removeMapPoint(layerId, pointId);
			}
		}
		return false;
	}

	@SuppressLint("StaticFieldLeak")
	private void finishGpxImport(boolean destinationExists, File destination, String color, boolean show) {
		int col = ConfigureMapMenu.GpxAppearanceAdapter.parseTrackColor(
				app.getRendererRegistry().getCurrentSelectedRenderer(), color);
		if (!destinationExists) {
			GpxDataItem gpxDataItem = new GpxDataItem(destination, col);
			gpxDataItem.setApiImported(true);
			app.getGpxDbHelper().add(gpxDataItem);
		} else {
			GpxDataItem item = app.getGpxDbHelper().getItem(destination);
			if (item != null) {
				app.getGpxDbHelper().updateColor(item, col);
			}
		}
		final GpxSelectionHelper helper = app.getSelectedGpxHelper();
		final SelectedGpxFile selectedGpx = helper.getSelectedFileByName(destination.getName());
		if (selectedGpx != null) {
			if (show) {
				new AsyncTask<File, Void, GPXFile>() {

					@Override
					protected GPXFile doInBackground(File... files) {
						return GPXUtilities.loadGPXFile(files[0]);
					}

					@Override
					protected void onPostExecute(GPXFile gpx) {
						if (gpx.error == null) {
							selectedGpx.setGpxFile(gpx, app);
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
					return GPXUtilities.loadGPXFile(files[0]);
				}

				@Override
				protected void onPostExecute(GPXFile gpx) {
					if (gpx.error == null) {
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
					if (!destinationExists) {
						Algorithms.createParentDirsForFile(destination);
					}
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
					if (!destinationExists) {
						Algorithms.createParentDirsForFile(destination);
					}
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
				if (!destinationExists) {
					Algorithms.createParentDirsForFile(destination);
				}
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

	@SuppressLint("StaticFieldLeak")
	boolean showGpx(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			File f = app.getAppPath(IndexConstants.GPX_INDEX_DIR + fileName);
			File fi = app.getAppPath(IndexConstants.GPX_IMPORT_DIR + fileName);
			AsyncTask<File, Void, GPXFile> asyncTask = new AsyncTask<File, Void, GPXFile>() {

				@Override
				protected GPXFile doInBackground(File... files) {
					return GPXUtilities.loadGPXFile(files[0]);
				}

				@Override
				protected void onPostExecute(GPXFile gpx) {
					if (gpx.error == null) {
						app.getSelectedGpxHelper().selectGpxFile(gpx, true, false);
						refreshMap();
					}
				}
			};

			if (f.exists()) {
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, f);
				return true;
			} else if (fi.exists()) {
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fi);
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
		List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
		String gpxPath = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath();
		for (SelectedGpxFile selectedGpxFile : selectedGpxFiles) {
			GPXFile gpxFile = selectedGpxFile.getGpxFile();
			String path = gpxFile.path;
			if (!Algorithms.isEmpty(path)) {
				if (path.startsWith(gpxPath)) {
					path = path.substring(gpxPath.length() + 1);
				}
				long modifiedTime = gpxFile.modifiedTime;
				long fileSize = new File(gpxFile.path).length();
				files.add(new ASelectedGpxFile(path, modifiedTime, fileSize, createGpxFileDetails(selectedGpxFile.getTrackAnalysis(app))));
			}
		}
		return true;
	}

	boolean getActiveGpxV2(List<net.osmand.aidlapi.gpx.ASelectedGpxFile> files) {
		List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
		String gpxPath = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath();
		for (SelectedGpxFile selectedGpxFile : selectedGpxFiles) {
			GPXFile gpxFile = selectedGpxFile.getGpxFile();
			String path = gpxFile.path;
			if (!Algorithms.isEmpty(path)) {
				if (path.startsWith(gpxPath)) {
					path = path.substring(gpxPath.length() + 1);
				}
				long modifiedTime = gpxFile.modifiedTime;
				long fileSize = new File(gpxFile.path).length();
				files.add(new net.osmand.aidlapi.gpx.ASelectedGpxFile(path, modifiedTime, fileSize, createGpxFileDetailsV2(selectedGpxFile.getTrackAnalysis(app))));
			}
		}
		return true;
	}

	boolean getImportedGpxV2(List<net.osmand.aidlapi.gpx.AGpxFile> files) {
		List<GpxDataItem> gpxDataItems = app.getGpxDbHelper().getItems();
		for (GpxDataItem dataItem : gpxDataItems) {
			File file = dataItem.getFile();
			if (file.exists()) {
				String fileName = file.getName();
				boolean active = app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath()) != null;
				long modifiedTime = dataItem.getFileLastModifiedTime();
				long fileSize = file.length();
				int color = dataItem.getColor();
				String colorName = "";
				if (color != 0) {
					colorName = ConfigureMapMenu.GpxAppearanceAdapter.parseTrackColorName(app.getRendererRegistry().getCurrentSelectedRenderer(), color);
				}
				net.osmand.aidlapi.gpx.AGpxFileDetails details = null;
				GPXTrackAnalysis analysis = dataItem.getAnalysis();
				if (analysis != null) {
					details = createGpxFileDetailsV2(analysis);
				}
				files.add(new net.osmand.aidlapi.gpx.AGpxFile(fileName, modifiedTime, fileSize, active, colorName, details));
			}
		}
		return true;
	}

	boolean getImportedGpx(List<AGpxFile> files) {
		List<GpxDataItem> gpxDataItems = app.getGpxDbHelper().getItems();
		for (GpxDataItem dataItem : gpxDataItems) {
			File file = dataItem.getFile();
			if (file.exists()) {
				String fileName = file.getName();
				boolean active = app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath()) != null;
				long modifiedTime = dataItem.getFileLastModifiedTime();
				long fileSize = file.length();
				AGpxFileDetails details = null;
				GPXTrackAnalysis analysis = dataItem.getAnalysis();
				if (analysis != null) {
					details = createGpxFileDetails(analysis);
				}
				files.add(new AGpxFile(fileName, modifiedTime, fileSize, active, details));
			}
		}
		return true;
	}

	String getGpxColor(String gpxFileName) {
		List<GpxDataItem> gpxDataItems = app.getGpxDbHelper().getItems();
		for (GpxDataItem dataItem : gpxDataItems) {
			File file = dataItem.getFile();
			if (file.exists()) {
				if (file.getName().equals(gpxFileName)) {
					int color = dataItem.getColor();
					if (color != 0) {
						return ConfigureMapMenu.GpxAppearanceAdapter.parseTrackColorName(app.getRendererRegistry().getCurrentSelectedRenderer(), color);
					}
				}
			}
		}
		return null;
	}

	boolean removeGpx(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			final File f = app.getAppPath(IndexConstants.GPX_INDEX_DIR + fileName);
			if (f.exists()) {
				GpxDataItem item = app.getGpxDbHelper().getItem(f);
				if (item != null && item.isApiImported()) {
					Algorithms.removeAllFiles(f);
					app.getGpxDbHelper().remove(f);
					return true;
				}
			}
		}
		return false;
	}

	private boolean getSqliteDbFiles(List<ASqliteDbFile> fileNames, boolean activeOnly) {
		File tilesPath = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
		if (tilesPath.canRead()) {
			File[] files = tilesPath.listFiles();
			if (files != null) {
				String activeFile = app.getSettings().MAP_OVERLAY.get();
				for (File tileFile : files) {
					String fileName = tileFile.getName();
					String fileNameLC = fileName.toLowerCase();
					if (tileFile.isFile() && !fileNameLC.startsWith("hillshade") && fileNameLC.endsWith(SQLiteTileSource.EXT)) {
						boolean active = fileName.equals(activeFile);
						if (!activeOnly || active) {
							fileNames.add(new ASqliteDbFile(fileName, tileFile.lastModified(), tileFile.length(), active));
						}
					}
				}
			}
		}
		return true;
	}

	private boolean getSqliteDbFilesV2(List<net.osmand.aidlapi.tiles.ASqliteDbFile> fileNames, boolean activeOnly) {
		File tilesPath = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
		if (tilesPath.canRead()) {
			File[] files = tilesPath.listFiles();
			if (files != null) {
				String activeFile = app.getSettings().MAP_OVERLAY.get();
				for (File tileFile : files) {
					String fileName = tileFile.getName();
					String fileNameLC = fileName.toLowerCase();
					if (tileFile.isFile() && !fileNameLC.startsWith("hillshade") && fileNameLC.endsWith(SQLiteTileSource.EXT)) {
						boolean active = fileName.equals(activeFile);
						if (!activeOnly || active) {
							fileNames.add(new net.osmand.aidlapi.tiles.ASqliteDbFile(fileName, tileFile.lastModified(), tileFile.length(), active));
						}
					}
				}
			}
		}
		return true;
	}

	boolean getSqliteDbFiles(List<ASqliteDbFile> fileNames) {
		return getSqliteDbFiles(fileNames, false);
	}

	boolean getActiveSqliteDbFiles(List<ASqliteDbFile> fileNames) {
		return getSqliteDbFiles(fileNames, true);
	}

	boolean getSqliteDbFilesV2(List<net.osmand.aidlapi.tiles.ASqliteDbFile> fileNames) {
		return getSqliteDbFilesV2(fileNames, false);
	}

	boolean getActiveSqliteDbFilesV2(List<net.osmand.aidlapi.tiles.ASqliteDbFile> fileNames) {
		return getSqliteDbFilesV2(fileNames, true);
	}

	boolean showSqliteDbFile(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			File tileFile = new File(app.getAppPath(IndexConstants.TILES_INDEX_DIR), fileName);
			String fileNameLC = fileName.toLowerCase();
			if (tileFile.isFile() && !fileNameLC.startsWith("hillshade") && fileNameLC.endsWith(SQLiteTileSource.EXT)) {
				OsmandSettings settings = app.getSettings();
				settings.MAP_OVERLAY.set(fileName);
				settings.MAP_OVERLAY_PREVIOUS.set(fileName);

				Intent intent = new Intent();
				intent.setAction(AIDL_SHOW_SQLITEDB_FILE);
				intent.putExtra(AIDL_FILE_NAME, fileName);
				app.sendBroadcast(intent);
			}
			return true;
		}
		return false;
	}

	boolean hideSqliteDbFile(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			if (fileName.equals(app.getSettings().MAP_OVERLAY.get())) {
				OsmandSettings settings = app.getSettings();
				settings.MAP_OVERLAY.set(null);
				settings.MAP_OVERLAY_PREVIOUS.set(null);

				Intent intent = new Intent();
				intent.setAction(AIDL_HIDE_SQLITEDB_FILE);
				intent.putExtra(AIDL_FILE_NAME, fileName);
				app.sendBroadcast(intent);
				return true;
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

	boolean startGpxRecording() {
		final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			plugin.startGPXMonitoring(null);
			plugin.updateControl();
			return true;
		}
		return false;
	}

	boolean stopGpxRecording() {
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

	boolean navigate(String startName, double startLat, double startLon,
	                 String destName, double destLat, double destLon,
	                 String profile, boolean force) {
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

	boolean navigateSearch(String startName, double startLat, double startLon,
	                       String searchQuery, double searchLat, double searchLon,
	                       String profile, boolean force) {
		Intent intent = new Intent();
		intent.setAction(AIDL_NAVIGATE_SEARCH);
		intent.putExtra(AIDL_START_NAME, startName);
		intent.putExtra(AIDL_START_LAT, startLat);
		intent.putExtra(AIDL_START_LON, startLon);
		intent.putExtra(AIDL_SEARCH_QUERY, searchQuery);
		intent.putExtra(AIDL_SEARCH_LAT, searchLat);
		intent.putExtra(AIDL_SEARCH_LON, searchLon);
		intent.putExtra(AIDL_PROFILE, profile);
		intent.putExtra(AIDL_FORCE, force);
		app.sendBroadcast(intent);
		return true;
	}

	boolean pauseNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_PAUSE_NAVIGATION);
		app.sendBroadcast(intent);
		return true;
	}

	boolean resumeNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_RESUME_NAVIGATION);
		app.sendBroadcast(intent);
		return true;
	}

	boolean stopNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_STOP_NAVIGATION);
		app.sendBroadcast(intent);
		return true;
	}

	boolean muteNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_MUTE_NAVIGATION);
		app.sendBroadcast(intent);
		return true;
	}

	boolean unmuteNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_UNMUTE_NAVIGATION);
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

	boolean search(final String searchQuery, final int searchType, final double latitude, final double longitude,
	               final int radiusLevel, final int totalLimit, final SearchCompleteCallback callback) {
		if (Algorithms.isEmpty(searchQuery) || latitude == 0 || longitude == 0 || callback == null) {
			return false;
		}
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					ExternalApiHelper.runSearch(app, searchQuery, searchType, latitude, longitude, radiusLevel, totalLimit, callback);
				}
			});
		} else {
			ExternalApiHelper.runSearch(app, searchQuery, searchType, latitude, longitude, radiusLevel, totalLimit, callback);
		}
		return true;
	}

	boolean registerForOsmandInitialization(final OsmandAppInitCallback callback) {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					try {
						callback.onAppInitialized();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			callback.onAppInitialized();
		}
		return true;
	}

	boolean setNavDrawerItems(String appPackage, List<OsmAndAppCustomization.NavDrawerItem> items) {
		return app.getAppCustomization().setNavDrawerItems(appPackage, items);
	}

	public void registerNavDrawerItems(final Activity activity, ContextMenuAdapter adapter) {
		app.getAppCustomization().registerNavDrawerItems(activity, adapter);
	}

	public ConnectedApp getConnectedApp(@NonNull String pack) {
		List<ConnectedApp> connectedApps = getConnectedApps();
		for (ConnectedApp connectedApp : connectedApps) {
			if (connectedApp.getPack().equals(pack)) {
				return connectedApp;
			}
		}
		return null;
	}

	public List<ConnectedApp> getConnectedApps() {
		List<ConnectedApp> res = new ArrayList<>(connectedApps.size());
		PackageManager pm = app.getPackageManager();
		for (ConnectedApp app : connectedApps.values()) {
			if (app.updateApplicationInfo(pm)) {
				res.add(app);
			}
		}
		Collections.sort(res);
		return res;
	}

	public boolean switchEnabled(@NonNull ConnectedApp connectedApp) {
		connectedApp.switchEnabled();
		return saveConnectedApps();
	}

	public boolean isAppEnabled(@NonNull String pack) {
		ConnectedApp connectedApp = connectedApps.get(pack);
		if (connectedApp == null) {
			connectedApp = new ConnectedApp(app, pack, true);
			connectedApps.put(pack, connectedApp);
			saveConnectedApps();
		}
		return connectedApp.isEnabled();
	}

	private boolean saveConnectedApps() {
		try {
			JSONArray array = new JSONArray();
			for (ConnectedApp connectedApp : connectedApps.values()) {
				JSONObject obj = new JSONObject();
				obj.put(ConnectedApp.ENABLED_KEY, connectedApp.isEnabled());
				obj.put(ConnectedApp.PACK_KEY, connectedApp.getPack());
				array.put(obj);
			}
			return app.getSettings().API_CONNECTED_APPS_JSON.set(array.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void loadConnectedApps() {
		try {
			connectedApps.clear();
			JSONArray array = new JSONArray(app.getSettings().API_CONNECTED_APPS_JSON.get());
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				String pack = obj.optString(ConnectedApp.PACK_KEY, "");
				boolean enabled = obj.optBoolean(ConnectedApp.ENABLED_KEY, true);
				connectedApps.put(pack, new ConnectedApp(app, pack, enabled));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	boolean setNavDrawerLogo(@Nullable String uri) {
		return app.getAppCustomization().setNavDrawerLogo(uri, null, null);
	}

	boolean setEnabledIds(Collection<String> ids) {
		app.getAppCustomization().setFeaturesEnabledIds(ids);
		return true;
	}

	boolean setDisabledIds(Collection<String> ids) {
		app.getAppCustomization().setFeaturesDisabledIds(ids);
		return true;
	}

	boolean setEnabledPatterns(Collection<String> patterns) {
		app.getAppCustomization().setFeaturesEnabledPatterns(patterns);
		return true;
	}

	boolean setDisabledPatterns(Collection<String> patterns) {
		app.getAppCustomization().setFeaturesDisabledPatterns(patterns);
		return true;
	}

	boolean regWidgetVisibility(@NonNull String widgetId, @Nullable List<String> appModeKeys) {
		app.getAppCustomization().regWidgetVisibility(widgetId, appModeKeys);
		return true;
	}

	boolean regWidgetAvailability(@NonNull String widgetId, @Nullable List<String> appModeKeys) {
		app.getAppCustomization().regWidgetAvailability(widgetId, appModeKeys);
		return true;
	}

	boolean customizeOsmandSettings(@NonNull String sharedPreferencesName, @Nullable Bundle bundle) {
		app.getAppCustomization().customizeOsmandSettings(sharedPreferencesName, bundle);
		return true;
	}

	boolean setNavDrawerLogoWithParams(
			String uri, @Nullable String packageName, @Nullable String intent) {
		return app.getAppCustomization().setNavDrawerLogoWithParams(uri, packageName, intent);
	}

	boolean setNavDrawerFooterWithParams(String uri, @Nullable String packageName, @Nullable String intent) {
		return app.getAppCustomization().setNavDrawerFooterParams(uri, packageName, intent);
	}

	boolean restoreOsmand() {
		return app.getAppCustomization().restoreOsmand();
	}

	boolean changePluginState(String pluginId, int newState) {
		return app.getAppCustomization().changePluginStatus(pluginId, newState);
	}

	private Map<Long, IRoutingDataUpdateListener> navUpdateCallbacks = new ConcurrentHashMap<>();

	void registerForNavigationUpdates(long id) {
		final NextDirectionInfo baseNdi = new NextDirectionInfo();
		IRoutingDataUpdateListener listener = new IRoutingDataUpdateListener() {
			@Override
			public void onRoutingDataUpdate() {
				if (aidlCallbackListener != null) {
					ADirectionInfo directionInfo = new ADirectionInfo(-1, -1, false);
					RoutingHelper rh = app.getRoutingHelper();
					if (rh.isDeviatedFromRoute()) {
						directionInfo.setTurnType(TurnType.OFFR);
						directionInfo.setDistanceTo((int) rh.getRouteDeviation());
					} else {
						NextDirectionInfo ndi = rh.getNextRouteDirectionInfo(baseNdi, true);
						if (ndi != null && ndi.distanceTo > 0 && ndi.directionInfo != null) {
							directionInfo.setDistanceTo(ndi.distanceTo);
							directionInfo.setTurnType(ndi.directionInfo.getTurnType().getValue());
						}
					}
					for (OsmandAidlService.AidlCallbackParams cb : aidlCallbackListener.getAidlCallbacks().values()) {
						if (!aidlCallbackListener.getAidlCallbacks().isEmpty() && (cb.getKey() & KEY_ON_NAV_DATA_UPDATE) > 0) {
							try {
								cb.getCallback().updateNavigationInfo(directionInfo);
							} catch (Exception e) {
								LOG.error(e.getMessage(), e);
							}
						}
					}
				}
				if (aidlCallbackListenerV2 != null) {
					net.osmand.aidlapi.navigation.ADirectionInfo directionInfo = new net.osmand.aidlapi.navigation.ADirectionInfo(-1, -1, false);
					RoutingHelper rh = app.getRoutingHelper();
					if (rh.isDeviatedFromRoute()) {
						directionInfo.setTurnType(TurnType.OFFR);
						directionInfo.setDistanceTo((int) rh.getRouteDeviation());
					} else {
						NextDirectionInfo ndi = rh.getNextRouteDirectionInfo(baseNdi, true);
						if (ndi != null && ndi.distanceTo > 0 && ndi.directionInfo != null) {
							directionInfo.setDistanceTo(ndi.distanceTo);
							directionInfo.setTurnType(ndi.directionInfo.getTurnType().getValue());
						}
					}
					for (OsmandAidlServiceV2.AidlCallbackParams cb : aidlCallbackListenerV2.getAidlCallbacks().values()) {
						if (!aidlCallbackListenerV2.getAidlCallbacks().isEmpty() && (cb.getKey() & KEY_ON_NAV_DATA_UPDATE) > 0) {
							try {
								cb.getCallback().updateNavigationInfo(directionInfo);
							} catch (Exception e) {
								LOG.error(e.getMessage(), e);
							}
						}
					}
				}
			}
		};
		navUpdateCallbacks.put(id, listener);
		app.getRoutingHelper().addRouteDataListener(listener);
	}

	public void unregisterFromUpdates(long id) {
		app.getRoutingHelper().removeRouteDataListener(navUpdateCallbacks.get(id));
		navUpdateCallbacks.remove(id);
	}

	public void registerForVoiceRouterMessages(long id) {
		VoiceRouter.VoiceMessageListener listener = new VoiceRouter.VoiceMessageListener() {
			@Override
			public void onVoiceMessage(List<String> cmds, List<String> played) {
				if (aidlCallbackListener != null) {
					for (OsmandAidlService.AidlCallbackParams cb : aidlCallbackListener.getAidlCallbacks().values()) {
						if (!aidlCallbackListener.getAidlCallbacks().isEmpty() && (cb.getKey() & KEY_ON_VOICE_MESSAGE) > 0) {
							try {
								cb.getCallback().onVoiceRouterNotify(new OnVoiceNavigationParams(cmds, played));
							} catch (Exception e) {
								LOG.error(e.getMessage(), e);
							}
						}
					}
				}
				if (aidlCallbackListenerV2 != null) {
					for (OsmandAidlServiceV2.AidlCallbackParams cb : aidlCallbackListenerV2.getAidlCallbacks().values()) {
						if (!aidlCallbackListenerV2.getAidlCallbacks().isEmpty() && (cb.getKey() & KEY_ON_VOICE_MESSAGE) > 0) {
							try {
								cb.getCallback().onVoiceRouterNotify(new net.osmand.aidlapi.navigation.OnVoiceNavigationParams(cmds, played));
							} catch (Exception e) {
								LOG.error(e.getMessage(), e);
							}
						}
					}
				}
			}
		};
		voiceRouterMessageCallbacks.put(id, listener);
		app.getRoutingHelper().getVoiceRouter().addVoiceMessageListener(listener);
	}

	public void unregisterFromVoiceRouterMessages(long id) {
		app.getRoutingHelper().getVoiceRouter().removeVoiceMessageListener(voiceRouterMessageCallbacks.get(id));
		voiceRouterMessageCallbacks.remove(id);
	}

	public Map<String, AidlContextMenuButtonsWrapper> getContextMenuButtonsParams() {
		return contextMenuButtonsParams;
	}

	boolean addContextMenuButtons(AidlContextMenuButtonsWrapper buttonsParams, long callbackId) {
		if (buttonsParams != null) {
			if (contextMenuButtonsParams.containsKey(buttonsParams.getId())) {
				updateContextMenuButtons(buttonsParams, callbackId);
			} else {
				addContextMenuButtonListener(buttonsParams, callbackId);
				contextMenuButtonsParams.put(buttonsParams.getId(), buttonsParams);
				Intent intent = new Intent();
				intent.setAction(AIDL_ADD_CONTEXT_MENU_BUTTONS);
				intent.putExtra(AIDL_OBJECT_ID, buttonsParams.getId());
				app.sendBroadcast(intent);
			}
			return true;
		} else {
			return false;
		}
	}

	boolean removeContextMenuButtons(String buttonsParamsId, long callbackId) {
		if (!Algorithms.isEmpty(buttonsParamsId) && contextMenuButtonsParams.containsKey(buttonsParamsId)) {
			contextMenuButtonsParams.remove(buttonsParamsId);
			contextMenuButtonsCallbacks.remove(callbackId);
			Intent intent = new Intent();
			intent.setAction(AIDL_REMOVE_CONTEXT_MENU_BUTTONS);
			intent.putExtra(AIDL_OBJECT_ID, buttonsParamsId);
			app.sendBroadcast(intent);
			return true;
		} else {
			return false;
		}
	}

	boolean updateContextMenuButtons(AidlContextMenuButtonsWrapper buttonsParams, long callbackId) {
		if (buttonsParams != null && contextMenuButtonsParams.containsKey(buttonsParams.getId())) {
			contextMenuButtonsParams.put(buttonsParams.getId(), buttonsParams);
			addContextMenuButtonListener(buttonsParams, callbackId);
			return true;
		} else {
			return false;
		}
	}

	boolean areOsmandSettingsCustomized(String sharedPreferencesName) {
		return app.getAppCustomization().areSettingsCustomizedForPreference(sharedPreferencesName);
	}

	private void addContextMenuButtonListener(AidlContextMenuButtonsWrapper buttonsParams, long callbackId) {
		IContextMenuButtonListener listener = new IContextMenuButtonListener() {

			@Override
			public void onContextMenuButtonClicked(int buttonId, String pointId, String layerId) {
				if (aidlCallbackListener != null) {
					for (OsmandAidlService.AidlCallbackParams cb : aidlCallbackListener.getAidlCallbacks().values()) {
						if (!aidlCallbackListener.getAidlCallbacks().isEmpty() && (cb.getKey() & KEY_ON_CONTEXT_MENU_BUTTONS_CLICK) > 0) {
							try {
								cb.getCallback().onContextMenuButtonClicked(buttonId, pointId, layerId);
							} catch (Exception e) {
								LOG.error(e.getMessage(), e);
							}
						}
					}
				}
				if (aidlCallbackListenerV2 != null) {
					for (OsmandAidlServiceV2.AidlCallbackParams cb : aidlCallbackListenerV2.getAidlCallbacks().values()) {
						if (!aidlCallbackListenerV2.getAidlCallbacks().isEmpty() && (cb.getKey() & KEY_ON_CONTEXT_MENU_BUTTONS_CLICK) > 0) {
							try {
								cb.getCallback().onContextMenuButtonClicked(buttonId, pointId, layerId);
							} catch (Exception e) {
								LOG.error(e.getMessage(), e);
							}
						}
					}
				}
			}
		};
		buttonsParams.setCallbackId(callbackId);
		contextMenuButtonsCallbacks.put(callbackId, listener);
	}

	private Map<Long, IContextMenuButtonListener> contextMenuButtonsCallbacks = new ConcurrentHashMap<>();

	public void contextMenuCallbackButtonClicked(long callbackId, int buttonId, String pointId, String layerId) {
		IContextMenuButtonListener contextMenuButtonListener = contextMenuButtonsCallbacks.get(callbackId);
		if (contextMenuButtonListener != null) {
			contextMenuButtonListener.onContextMenuButtonClicked(buttonId, pointId, layerId);
		}
	}

	boolean getBitmapForGpx(final Uri gpxUri, final float density, final int widthPixels,
	                        final int heightPixels, final int color, final GpxBitmapCreatedCallback callback) {
		if (gpxUri == null || callback == null) {
			return false;
		}
		final TrackBitmapDrawer.TrackBitmapDrawerListener drawerListener = new TrackBitmapDrawer.TrackBitmapDrawerListener() {
			@Override
			public void onTrackBitmapDrawing() {
			}

			@Override
			public void onTrackBitmapDrawn() {
			}

			@Override
			public boolean isTrackBitmapSelectionSupported() {
				return false;
			}

			@Override
			public void drawTrackBitmap(Bitmap bitmap) {
				callback.onGpxBitmapCreatedComplete(bitmap);
			}
		};

		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					createGpxBitmapFromUri(gpxUri, density, widthPixels, heightPixels, color, drawerListener);
				}
			});
		} else {
			createGpxBitmapFromUri(gpxUri, density, widthPixels, heightPixels, color, drawerListener);
		}
		return true;
	}

	private void createGpxBitmapFromUri(final Uri gpxUri, final float density, final int widthPixels, final int heightPixels, final int color, final TrackBitmapDrawer.TrackBitmapDrawerListener drawerListener) {
		GpxAsyncLoaderTask gpxAsyncLoaderTask = new GpxAsyncLoaderTask(app, gpxUri, new CallbackWithObject<GPXFile>() {
			@Override
			public boolean processResult(GPXFile result) {
				TrackBitmapDrawer trackBitmapDrawer = new TrackBitmapDrawer(app, result, null, result.getRect(), density, widthPixels, heightPixels);
				trackBitmapDrawer.addListener(drawerListener);
				trackBitmapDrawer.setDrawEnabled(true);
				trackBitmapDrawer.setTrackColor(color);
				trackBitmapDrawer.initAndDraw();
				return false;
			}
		});
		gpxAsyncLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private Map<String, FileCopyInfo> copyFilesCache = new ConcurrentHashMap<>();

	public boolean importProfile(final Uri profileUri, String latestChanges, int version) {
		if (profileUri != null) {
			Bundle bundle = new Bundle();
			bundle.putString(SettingsHelper.SETTINGS_LATEST_CHANGES_KEY, latestChanges);
			bundle.putInt(SettingsHelper.SETTINGS_VERSION_KEY, version);

			MapActivity.launchMapActivityMoveToTop(app, null, profileUri, bundle);
			return true;
		}
		return false;
	}

	public void registerLayerContextMenu(ContextMenuAdapter adapter, MapActivity mapActivity) {
		for (ConnectedApp connectedApp : getConnectedApps()) {
			connectedApp.registerLayerContextMenu(adapter, mapActivity);
		}
	}

	private class FileCopyInfo {
		long startTime;
		long lastAccessTime;
		FileOutputStream fileOutputStream;

		FileCopyInfo(long startTime, long lastAccessTime, FileOutputStream fileOutputStream) {
			this.startTime = startTime;
			this.lastAccessTime = lastAccessTime;
			this.fileOutputStream = fileOutputStream;
		}
	}

	int copyFile(String fileName, byte[] filePartData, long startTime, boolean done) {
		if (Algorithms.isEmpty(fileName) || filePartData == null) {
			return COPY_FILE_PARAMS_ERROR;
		}
		if (filePartData.length > COPY_FILE_PART_SIZE_LIMIT) {
			return COPY_FILE_PART_SIZE_LIMIT_ERROR;
		}
		if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			return copyFileImpl(fileName, filePartData, startTime, done, IndexConstants.TILES_INDEX_DIR);
		} else {
			return COPY_FILE_UNSUPPORTED_FILE_TYPE_ERROR;
		}
	}

	private int copyFileImpl(String fileName, byte[] filePartData, long startTime, boolean done, String destinationDir) {
		File file = app.getAppPath(IndexConstants.TEMP_DIR + fileName);
		File tempDir = app.getAppPath(IndexConstants.TEMP_DIR);
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
		File destFile = app.getAppPath(destinationDir + fileName);
		long currentTime = System.currentTimeMillis();
		try {
			FileCopyInfo info = copyFilesCache.get(fileName);
			if (info == null) {
				FileOutputStream fos = new FileOutputStream(file, true);
				copyFilesCache.put(fileName,
						new FileCopyInfo(startTime, currentTime, fos));
				if (done) {
					if (!finishFileCopy(filePartData, file, fos, fileName, destFile)) {
						return COPY_FILE_IO_ERROR;
					}
				} else {
					fos.write(filePartData);
				}
			} else {
				if (info.startTime != startTime) {
					if (currentTime - info.lastAccessTime < COPY_FILE_MAX_LOCK_TIME_MS) {
						return COPY_FILE_WRITE_LOCK_ERROR;
					} else {
						file.delete();
						copyFilesCache.remove(fileName);
						return copyFileImpl(fileName, filePartData, startTime, done, destinationDir);
					}
				}
				FileOutputStream fos = info.fileOutputStream;
				info.lastAccessTime = currentTime;
				if (done) {
					if (!finishFileCopy(filePartData, file, fos, fileName, destFile)) {
						return COPY_FILE_IO_ERROR;
					}
				} else {
					fos.write(filePartData);
				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return COPY_FILE_IO_ERROR;
		}
		return OK_RESPONSE;
	}

	private boolean finishFileCopy(byte[] data, File file, FileOutputStream fos, String fileName, File destFile) throws IOException {
		boolean res = true;
		if (data.length > 0) {
			fos.write(data);
		}
		if (destFile.exists() && !destFile.delete()) {
			res = false;
		}
		if (res && !file.renameTo(destFile)) {
			file.delete();
			res = false;
		}
		copyFilesCache.remove(fileName);
		return res;
	}

	private static class GpxAsyncLoaderTask extends AsyncTask<Void, Void, GPXFile> {

		private final OsmandApplication app;
		private final CallbackWithObject<GPXFile> callback;
		private final Uri gpxUri;

		GpxAsyncLoaderTask(@NonNull OsmandApplication app, @NonNull Uri gpxUri, final CallbackWithObject<GPXFile> callback) {
			this.app = app;
			this.gpxUri = gpxUri;
			this.callback = callback;
		}

		@Override
		protected void onPostExecute(GPXFile gpxFile) {
			if (gpxFile.error == null && callback != null) {
				callback.processResult(gpxFile);
			}
		}

		@Override
		protected GPXFile doInBackground(Void... voids) {
			ParcelFileDescriptor gpxParcelDescriptor = null;
			try {
				gpxParcelDescriptor = app.getContentResolver().openFileDescriptor(gpxUri, "r");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			if (gpxParcelDescriptor != null) {
				final FileDescriptor fileDescriptor = gpxParcelDescriptor.getFileDescriptor();
				return GPXUtilities.loadGPXFile(new FileInputStream(fileDescriptor));
			}
			return null;
		}
	}

	private static AGpxFileDetails createGpxFileDetails(@NonNull GPXTrackAnalysis a) {
		return new AGpxFileDetails(a.totalDistance, a.totalTracks, a.startTime, a.endTime,
				a.timeSpan, a.timeMoving, a.totalDistanceMoving, a.diffElevationUp, a.diffElevationDown,
				a.avgElevation, a.minElevation, a.maxElevation, a.minSpeed, a.maxSpeed, a.avgSpeed,
				a.points, a.wptPoints, a.wptCategoryNames);
	}

	private static net.osmand.aidlapi.gpx.AGpxFileDetails createGpxFileDetailsV2(@NonNull GPXTrackAnalysis a) {
		return new net.osmand.aidlapi.gpx.AGpxFileDetails(a.totalDistance, a.totalTracks, a.startTime, a.endTime,
				a.timeSpan, a.timeMoving, a.totalDistanceMoving, a.diffElevationUp, a.diffElevationDown,
				a.avgElevation, a.minElevation, a.maxElevation, a.minSpeed, a.maxSpeed, a.avgSpeed,
				a.points, a.wptPoints, a.wptCategoryNames);
	}

	public interface SearchCompleteCallback {
		void onSearchComplete(List<AidlSearchResultWrapper> resultSet);
	}

	public interface GpxBitmapCreatedCallback {
		void onGpxBitmapCreatedComplete(Bitmap bitmap);
	}

	public interface OsmandAppInitCallback {
		void onAppInitialized();
	}

	public interface AMapPointUpdateListener {
		void onAMapPointUpdated(AidlMapPointWrapper point, String layerId);
	}
}