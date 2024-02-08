package net.osmand.aidl;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.aidl.ConnectedApp.AIDL_ADD_MAP_LAYER;
import static net.osmand.aidl.ConnectedApp.AIDL_ADD_MAP_WIDGET;
import static net.osmand.aidl.ConnectedApp.AIDL_OBJECT_ID;
import static net.osmand.aidl.ConnectedApp.AIDL_PACKAGE_NAME;
import static net.osmand.aidl.ConnectedApp.AIDL_REMOVE_MAP_LAYER;
import static net.osmand.aidl.ConnectedApp.AIDL_REMOVE_MAP_WIDGET;
import static net.osmand.aidlapi.OsmandAidlConstants.CANNOT_ACCESS_API_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_IO_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_MAX_LOCK_TIME_MS;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_PARAMS_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_PART_SIZE_LIMIT;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_PART_SIZE_LIMIT_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_UNSUPPORTED_FILE_TYPE_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.COPY_FILE_WRITE_LOCK_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.OK_RESPONSE;
import static net.osmand.plus.myplaces.favorites.FavouritesFileHelper.LEGACY_FAV_FILE_PREFIX;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.REPLACE_KEY;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.SILENT_IMPORT_KEY;
import static net.osmand.gpx.GpxParameter.API_IMPORTED;
import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.FILE_LAST_MODIFIED_TIME;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.CallbackWithObject;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.aidl.gpx.AGpxFile;
import net.osmand.aidl.gpx.AGpxFileDetails;
import net.osmand.aidl.gpx.ASelectedGpxFile;
import net.osmand.aidl.navigation.ADirectionInfo;
import net.osmand.aidl.navigation.OnVoiceNavigationParams;
import net.osmand.aidl.quickaction.QuickActionInfoParams;
import net.osmand.aidl.tiles.ASqliteDbFile;
import net.osmand.aidlapi.customization.AProfile;
import net.osmand.aidlapi.customization.PreferenceParams;
import net.osmand.aidlapi.exit.ExitAppParams;
import net.osmand.aidlapi.info.AppInfoParams;
import net.osmand.aidlapi.info.GetTextParams;
import net.osmand.aidlapi.logcat.OnLogcatMessageParams;
import net.osmand.aidlapi.map.ALatLon;
import net.osmand.aidlapi.map.ALocation;
import net.osmand.aidlapi.navigation.ABlockedRoad;
import net.osmand.aidlapi.navigation.NavigateGpxParams;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.helpers.ExternalApiHelper;
import net.osmand.plus.helpers.LockHelper;
import net.osmand.plus.helpers.NavigateGpxHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.IContextMenuButtonListener;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.tracks.TrackBitmapDrawer;
import net.osmand.plus.myplaces.tracks.TrackBitmapDrawer.TrackBitmapDrawerListener;
import net.osmand.plus.myplaces.tracks.TrackBitmapDrawer.TracksDrawParams;
import net.osmand.plus.plugins.custom.CustomOsmandPlugin;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.development.LogcatAsyncTask;
import net.osmand.plus.plugins.development.LogcatMessageListener;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.routing.IRoutingDataUpdateListener;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.plus.routing.VoiceRouter.VoiceMessageListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ApplicationModeBean;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.AidlMapLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.SideWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class OsmandAidlApi {

	AidlCallbackListener aidlCallbackListener;
	AidlCallbackListenerV2 aidlCallbackListenerV2;

	public static final int KEY_ON_UPDATE = 1;
	public static final int KEY_ON_NAV_DATA_UPDATE = 2;
	public static final int KEY_ON_CONTEXT_MENU_BUTTONS_CLICK = 4;
	public static final int KEY_ON_VOICE_MESSAGE = 8;
	public static final int KEY_ON_KEY_EVENT = 16;
	public static final int KEY_ON_LOGCAT_MESSAGE = 32;

	public static final String WIDGET_ID_PREFIX = "aidl_widget_";

	private static final Log LOG = PlatformUtil.getLog(OsmandAidlApi.class);

	private static final String AIDL_REFRESH_MAP = "aidl_refresh_map";
	private static final String AIDL_SET_MAP_LOCATION = "aidl_set_map_location";
	private static final String AIDL_SET_LOCATION = "aidl_set_location";
	private static final String AIDL_LATITUDE = "aidl_latitude";
	private static final String AIDL_LONGITUDE = "aidl_longitude";
	private static final String AIDL_ZOOM = "aidl_zoom";
	private static final String AIDL_ROTATION = "aidl_rotation";
	private static final String AIDL_ANIMATED = "aidl_animated";
	private static final String AIDL_LOCATION = "aidl_location";
	private static final String AIDL_TIME_TO_NOT_USE_OTHER_GPS = "aidl_time_to_not_use_other_gps";

	private static final String AIDL_START_NAME = "aidl_start_name";
	private static final String AIDL_START_LAT = "aidl_start_lat";
	private static final String AIDL_START_LON = "aidl_start_lon";
	private static final String AIDL_DEST_NAME = "aidl_dest_name";
	private static final String AIDL_DEST_LAT = "aidl_dest_lat";
	private static final String AIDL_DEST_LON = "aidl_dest_lon";
	private static final String AIDL_PROFILE = "aidl_profile";
	private static final String AIDL_FORCE = "aidl_force";
	private static final String AIDL_LOCATION_PERMISSION = "aidl_location_permission";
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
	private static final String AIDL_NAVIGATE_SEARCH = "aidl_navigate_search";
	private static final String AIDL_PAUSE_NAVIGATION = "pause_navigation";
	private static final String AIDL_RESUME_NAVIGATION = "resume_navigation";
	private static final String AIDL_STOP_NAVIGATION = "stop_navigation";
	private static final String AIDL_MUTE_NAVIGATION = "mute_navigation";
	private static final String AIDL_UNMUTE_NAVIGATION = "unmute_navigation";

	private static final String AIDL_SHOW_SQLITEDB_FILE = "aidl_show_sqlitedb_file";
	private static final String AIDL_HIDE_SQLITEDB_FILE = "aidl_hide_sqlitedb_file";
	private static final String AIDL_FILE_NAME = "aidl_file_name";

	private static final String AIDL_EXECUTE_QUICK_ACTION = "aidl_execute_quick_action";
	private static final String AIDL_QUICK_ACTION_NUMBER = "aidl_quick_action_number";
	private static final String AIDL_LOCK_STATE = "lock_state";
	private static final String AIDL_EXIT_APP = "exit_app";
	private static final String AIDL_EXIT_APP_RESTART = "exit_app_restart";

	private static final ApplicationMode DEFAULT_PROFILE = ApplicationMode.CAR;

	private static final ApplicationMode[] VALID_PROFILES = {
			ApplicationMode.CAR,
			ApplicationMode.BICYCLE,
			ApplicationMode.PEDESTRIAN
	};

	private static final int DEFAULT_ZOOM = 15;

	private final OsmandApplication app;
	private Map<String, BroadcastReceiver> receivers = new TreeMap<>();
	private final Map<String, ConnectedApp> connectedApps = new ConcurrentHashMap<>();
	private final Map<Long, IRoutingDataUpdateListener> navUpdateCallbacks = new ConcurrentHashMap<>();
	private final Map<String, AidlContextMenuButtonsWrapper> contextMenuButtonsParams = new ConcurrentHashMap<>();
	private final Map<Long, VoiceRouter.VoiceMessageListener> voiceRouterMessageCallbacks = new ConcurrentHashMap<>();
	private final Map<Long, Set<Integer>> keyEventCallbacks = new ConcurrentHashMap<>();
	private final Map<Long, LogcatAsyncTask> logcatAsyncTasks = new ConcurrentHashMap<>();

	private MapActivity mapActivity;

	private boolean mapActivityActive;

	public OsmandAidlApi(@NonNull OsmandApplication app) {
		this.app = app;
		loadConnectedApps();
	}

	public void onCreateMapActivity(@NonNull MapActivity mapActivity) {
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
		registerNavigateSearchReceiver(mapActivity);
		registerPauseNavigationReceiver(mapActivity);
		registerResumeNavigationReceiver(mapActivity);
		registerStopNavigationReceiver(mapActivity);
		registerMuteNavigationReceiver(mapActivity);
		registerUnmuteNavigationReceiver(mapActivity);
		registerShowSqliteDbFileReceiver(mapActivity);
		registerHideSqliteDbFileReceiver(mapActivity);
		registerExecuteQuickActionReceiver(mapActivity);
		registerLockStateReceiver(mapActivity);
		registerSetLocationReceiver(mapActivity);
		registerExitAppReceiver(mapActivity);
		initOsmandTelegram();
		app.getAppCustomization().addListener(mapActivity);
		this.mapActivity = mapActivity;
	}

	public void onDestroyMapActivity(MapActivity mapActivity) {
		app.getAppCustomization().removeListener(mapActivity);
		this.mapActivity = null;
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
		return mapActivity;
	}

	private void initOsmandTelegram() {
		String[] packages = {"net.osmand.telegram", "net.osmand.telegram.debug"};
		Intent intent = new Intent("net.osmand.telegram.InitApp");
		for (String pack : packages) {
			intent.setComponent(new ComponentName(pack, "net.osmand.telegram.InitAppBroadcastReceiver"));
			app.sendBroadcast(intent);
		}
	}

	private void registerRefreshMapReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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

	private void registerSetMapLocationReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver setMapLocationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					int zoom = intent.getIntExtra(AIDL_ZOOM, 0);
					float zoomFloatPart;
					boolean animated = intent.getBooleanExtra(AIDL_ANIMATED, false);
					float rotation = intent.getFloatExtra(AIDL_ROTATION, Float.NaN);
					if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
						OsmandMapTileView mapView = mapActivity.getMapView();
						if (zoom == 0) {
							zoom = mapView.getZoom();
							zoomFloatPart = mapView.getZoomFloatPart();
						} else {
							zoom = Math.min(zoom, mapView.getMaxZoom());
							zoom = Math.max(zoom, mapView.getMinZoom());
							zoomFloatPart = 0;
						}
						if (!Float.isNaN(rotation)) {
							mapView.setRotate(rotation, false);
						}
						if (animated) {
							mapView.getAnimatedDraggingThread().startMoving(lat, lon, zoom, zoomFloatPart);
						} else {
							mapView.setLatLon(lat, lon);
							mapView.setZoomWithFloatPart(zoom, zoomFloatPart);
						}
					}
					mapActivity.refreshMap();
				}
			}
		};
		registerReceiver(setMapLocationReceiver, mapActivity, AIDL_SET_MAP_LOCATION);
	}

	private void registerAddMapWidgetReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver addMapWidgetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String widgetId = intent.getStringExtra(AIDL_OBJECT_ID);
				String packName = intent.getStringExtra(AIDL_PACKAGE_NAME);
				if (mapActivity != null && widgetId != null && packName != null) {
					ConnectedApp connectedApp = connectedApps.get(packName);
					if (connectedApp != null) {
						AidlMapWidgetWrapper widgetData = connectedApp.getWidgets().get(widgetId);
						MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
						if (widgetData != null && layer != null) {
							WidgetsAvailabilityHelper.regWidgetVisibility(widgetData.getId(), (ApplicationMode[]) null);
							TextInfoWidget widget = connectedApp.createWidgetControl(mapActivity, widgetId);
							connectedApp.getWidgetControls().put(widgetId, widget);

							int iconId = AndroidUtils.getDrawableId(app, widgetData.getMenuIconName());
							int menuIconId = iconId != 0 ? iconId : ContextMenuItem.INVALID_ID;
							String widgetKey = WIDGET_ID_PREFIX + widgetId;
							WidgetsPanel defaultPanel = widgetData.isRightPanelByDefault() ? WidgetsPanel.RIGHT : WidgetsPanel.LEFT;
							ApplicationMode appMode = app.getSettings().getApplicationMode();

							WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);
							MapWidgetInfo widgetInfo = creator.createExternalWidget(widgetKey, widget, menuIconId,
									widgetData.getMenuTitle(), defaultPanel, widgetData.getOrder());
							MapWidgetRegistry registry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
							registry.registerWidget(widgetInfo);

							((SideWidgetInfo) widgetInfo).setExternalProviderPackage(connectedApp.getPack());
							layer.recreateControls();
						}
					}
				}
			}
		};
		registerReceiver(addMapWidgetReceiver, mapActivity, AIDL_ADD_MAP_WIDGET);
	}

	boolean setMapMargins(int left, int top, int right, int bottom, @Nullable List<String> appModeKeys) {
		app.getAppCustomization().setMapMargins(left, top, right, bottom, appModeKeys);
		app.getAppCustomization().updateMapMargins(mapActivity);
		return true;
	}

	boolean setZoomLimits(int minZoom, int maxZoom) {
		if (minZoom > 0 && maxZoom > 0 && minZoom < maxZoom) {
			app.getAppCustomization().setZoomLimits(minZoom, maxZoom);
			return true;
		}
		return false;
	}

	private void registerAddContextMenuButtonsReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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
		try {
			receivers.put(filter, rec);
			ma.registerReceiver(rec, new IntentFilter(filter));
		} catch (IllegalStateException e) {
			LOG.error(e);
		}
	}

	private void registerRemoveMapWidgetReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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

	public void createWidgetControls(@NonNull MapActivity mapActivity,
	                                 @NonNull List<MapWidgetInfo> widgetsInfos,
	                                 @NonNull ApplicationMode appMode) {
		for (ConnectedApp connectedApp : connectedApps.values()) {
			connectedApp.createWidgetControls(mapActivity, widgetsInfos, appMode);
		}
	}

	private void registerAddMapLayerReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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

	private void registerRemoveMapLayerReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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

	private void registerTakePhotoNoteReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver takePhotoNoteReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				AudioVideoNotesPlugin plugin = PluginsHelper.getActivePlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.takePhoto(lat, lon, mapActivity, false, true);
				}
			}
		};
		registerReceiver(takePhotoNoteReceiver, mapActivity, AIDL_TAKE_PHOTO_NOTE);
	}

	private void registerStartVideoRecordingReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver startVideoRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				AudioVideoNotesPlugin plugin = PluginsHelper.getActivePlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.recordVideo(lat, lon, mapActivity, true);
				}
			}
		};
		registerReceiver(startVideoRecordingReceiver, mapActivity, AIDL_START_VIDEO_RECORDING);
	}

	private void registerStartAudioRecordingReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver startAudioRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				AudioVideoNotesPlugin plugin = PluginsHelper.getActivePlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.recordAudio(lat, lon, mapActivity);
				}
			}
		};
		registerReceiver(startAudioRecordingReceiver, mapActivity, AIDL_START_AUDIO_RECORDING);
	}

	private void registerStopRecordingReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver stopRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				AudioVideoNotesPlugin plugin = PluginsHelper.getActivePlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					plugin.stopRecording(mapActivity, false);
				}
			}
		};
		registerReceiver(stopRecordingReceiver, mapActivity, AIDL_STOP_RECORDING);
	}

	private void registerNavigateReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver navigateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String profileStr = intent.getStringExtra(AIDL_PROFILE);
				ApplicationMode profile = ApplicationMode.valueOfStringKey(profileStr, DEFAULT_PROFILE);
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

					LatLon start;
					PointDescription startDesc;
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
					LatLon dest = new LatLon(destLat, destLon);
					PointDescription destDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, destName);

					RoutingHelper routingHelper = app.getRoutingHelper();
					boolean force = intent.getBooleanExtra(AIDL_FORCE, true);
					boolean locationPermission = intent.getBooleanExtra(AIDL_LOCATION_PERMISSION, false);
					if (routingHelper.isFollowingMode() && !force) {
						mapActivity.getMapActions().stopNavigationActionConfirm(dialog -> {
							MapActivity activity = mapActivityRef.get();
							if (activity != null && !routingHelper.isFollowingMode()) {
								NavigateGpxHelper.startNavigation(activity, profile, start, startDesc, dest, destDesc, locationPermission);
							}
						});
					} else {
						NavigateGpxHelper.startNavigation(mapActivity, profile, start, startDesc, dest, destDesc, locationPermission);
					}
				}
			}
		};
		registerReceiver(navigateReceiver, mapActivity, AIDL_NAVIGATE);
	}

	private void registerNavigateSearchReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver navigateSearchReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String profileStr = intent.getStringExtra(AIDL_PROFILE);
				ApplicationMode profile = ApplicationMode.valueOfStringKey(profileStr, DEFAULT_PROFILE);
				boolean validProfile = false;
				for (ApplicationMode mode : VALID_PROFILES) {
					if (mode == profile) {
						validProfile = true;
						break;
					}
				}
				MapActivity mapActivity = mapActivityRef.get();
				String searchQuery = intent.getStringExtra(AIDL_SEARCH_QUERY);
				if (mapActivity != null && validProfile && !Algorithms.isEmpty(searchQuery)) {
					String startName = intent.getStringExtra(AIDL_START_NAME);
					if (Algorithms.isEmpty(startName)) {
						startName = "";
					}

					LatLon start;
					PointDescription startDesc;
					double startLat = intent.getDoubleExtra(AIDL_START_LAT, 0);
					double startLon = intent.getDoubleExtra(AIDL_START_LON, 0);
					if (startLat != 0 && startLon != 0) {
						start = new LatLon(startLat, startLon);
						startDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, startName);
					} else {
						start = null;
						startDesc = null;
					}

					LatLon searchLocation;
					double searchLat = intent.getDoubleExtra(AIDL_SEARCH_LAT, 0);
					double searchLon = intent.getDoubleExtra(AIDL_SEARCH_LON, 0);
					if (searchLat != 0 && searchLon != 0) {
						searchLocation = new LatLon(searchLat, searchLon);
					} else {
						searchLocation = null;
					}

					if (searchLocation != null) {
						RoutingHelper routingHelper = app.getRoutingHelper();
						boolean force = intent.getBooleanExtra(AIDL_FORCE, true);
						boolean locationPermission = intent.getBooleanExtra(AIDL_LOCATION_PERMISSION, false);
						if (routingHelper.isFollowingMode() && !force) {
							mapActivity.getMapActions().stopNavigationActionConfirm(dialog -> {
								MapActivity mapActivity1 = mapActivityRef.get();
								if (mapActivity1 != null && !routingHelper.isFollowingMode()) {
									ExternalApiHelper.searchAndNavigate(mapActivity1, searchLocation, start,
											startDesc, profile, searchQuery, false, locationPermission);
								}
							});
						} else {
							ExternalApiHelper.searchAndNavigate(mapActivity, searchLocation, start,
									startDesc, profile, searchQuery, false, locationPermission);
						}
					}
				}
			}
		};
		registerReceiver(navigateSearchReceiver, mapActivity, AIDL_NAVIGATE_SEARCH);
	}

	private void registerPauseNavigationReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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

	private void registerResumeNavigationReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver resumeNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					RoutingHelper routingHelper = mapActivity.getRoutingHelper();
					if (routingHelper.isRouteCalculated() && routingHelper.isRoutePlanningMode()) {
						routingHelper.setRoutePlanningMode(false);
						routingHelper.setFollowingMode(true);
						AndroidUtils.requestNotificationPermissionIfNeeded(mapActivity);
					}
				}
			}
		};
		registerReceiver(resumeNavigationReceiver, mapActivity, AIDL_RESUME_NAVIGATION);
	}

	private void registerStopNavigationReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver stopNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					RoutingHelper routingHelper = mapActivity.getRoutingHelper();
					if (routingHelper.isPauseNavigation() || routingHelper.isFollowingMode()) {
						mapActivity.getMapLayers().getMapActionsHelper().stopNavigationWithoutConfirm();
					}
				}
			}
		};
		registerReceiver(stopNavigationReceiver, mapActivity, AIDL_STOP_NAVIGATION);
	}

	private void registerMuteNavigationReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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

	private void registerUnmuteNavigationReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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

	private void registerShowSqliteDbFileReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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
						OsmandRasterMapsPlugin plugin = PluginsHelper.getActivePlugin(OsmandRasterMapsPlugin.class);
						if (plugin != null) {
							plugin.updateMapLayers(mapActivity, mapActivity, settings.MAP_OVERLAY);
						}
					}
				}
			}
		};
		registerReceiver(showSqliteDbFileReceiver, mapActivity, AIDL_SHOW_SQLITEDB_FILE);
	}

	private void registerHideSqliteDbFileReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
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
						OsmandRasterMapsPlugin plugin = PluginsHelper.getActivePlugin(OsmandRasterMapsPlugin.class);
						if (plugin != null) {
							plugin.updateMapLayers(mapActivity, mapActivity, settings.MAP_OVERLAY);
						}
					}
				}
			}
		};
		registerReceiver(hideSqliteDbFileReceiver, mapActivity, AIDL_HIDE_SQLITEDB_FILE);
	}

	private void registerExecuteQuickActionReceiver(@NonNull MapActivity mapActivity) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver executeQuickActionReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int actionNumber = intent.getIntExtra(AIDL_QUICK_ACTION_NUMBER, -1);
				MapActivity mapActivity = mapActivityRef.get();
				if (actionNumber != -1 && mapActivity != null) {
					List<QuickAction> actionsList = app.getQuickActionRegistry().getQuickActions();
					if (actionNumber < actionsList.size()) {
						QuickActionRegistry.produceAction(actionsList.get(actionNumber)).execute(mapActivity);
					}
				}
			}
		};
		registerReceiver(executeQuickActionReceiver, mapActivity, AIDL_EXECUTE_QUICK_ACTION);
	}

	private void registerLockStateReceiver(@NonNull MapActivity mapActivity) {
		BroadcastReceiver lockStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LockHelper lockHelper = app.getLockHelper();
				boolean lock = intent.getBooleanExtra(AIDL_LOCK_STATE, false);
				if (lock) {
					lockHelper.lock();
				} else {
					lockHelper.unlock();
				}
			}
		};
		registerReceiver(lockStateReceiver, mapActivity, AIDL_LOCK_STATE);
	}

	private void registerSetLocationReceiver(@NonNull MapActivity mapActivity) {
		BroadcastReceiver setLocationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String packName = intent.getStringExtra(AIDL_PACKAGE_NAME);
				ALocation aLocation = intent.getParcelableExtra(AIDL_LOCATION);
				long timeToNotUseOtherGPS = intent.getLongExtra(AIDL_TIME_TO_NOT_USE_OTHER_GPS, 0);

				if (!Algorithms.isEmpty(packName) && aLocation != null
						&& !Double.isNaN(aLocation.getLatitude()) && !Double.isNaN(aLocation.getLongitude())) {
					Location location = new Location(packName);
					location.setLatitude(aLocation.getLatitude());
					location.setLongitude(aLocation.getLongitude());
					location.setTime(aLocation.getTime());
					if (aLocation.hasAltitude()) {
						location.setAltitude(aLocation.getAltitude());
					}
					if (aLocation.hasSpeed()) {
						location.setSpeed(aLocation.getSpeed());
					}
					if (aLocation.hasBearing()) {
						location.setBearing(aLocation.getBearing());
					}
					if (aLocation.hasAccuracy()) {
						location.setAccuracy(aLocation.getAccuracy());
					}
					if (aLocation.hasVerticalAccuracy()) {
						location.setVerticalAccuracy(aLocation.getVerticalAccuracy());
					}
					app.getLocationProvider().setCustomLocation(location, timeToNotUseOtherGPS);
				}
			}
		};
		registerReceiver(setLocationReceiver, mapActivity, AIDL_SET_LOCATION);
	}

	private void registerExitAppReceiver(@NonNull MapActivity mapActivity) {
		BroadcastReceiver exitAppReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				boolean restart = intent.getBooleanExtra(AIDL_EXIT_APP_RESTART, false);
				if (restart) {
					RestartActivity.doRestartSilent(app);
				} else {
					mapActivity.finishAndRemoveTask();
					RestartActivity.exitApp();
				}
			}
		};
		registerReceiver(exitAppReceiver, mapActivity, AIDL_EXIT_APP);
	}

	public void registerMapLayers(@NonNull Context context) {
		for (ConnectedApp connectedApp : connectedApps.values()) {
			connectedApp.registerMapLayers(context);
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
		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
		List<FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
		for (FavoriteGroup g : groups) {
			if (g.getName().equals(name)) {
				return false;
			}
		}
		int color = 0;
		if (!Algorithms.isEmpty(colorTag)) {
			color = ColorDialogs.getColorByTag(colorTag);
		}
		FavoriteGroup group = favoritesHelper.addFavoriteGroup(name, color);
		group.setVisible(visible);

		return true;
	}

	boolean removeFavoriteGroup(String name) {
		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
		for (FavoriteGroup group : favoritesHelper.getFavoriteGroups()) {
			if (group.getName().equals(name)) {
				favoritesHelper.deleteGroup(group, false);
				return true;
			}
		}
		favoritesHelper.saveCurrentPointsIntoFile(true);
		return false;
	}

	boolean updateFavoriteGroup(String prevGroupName, String newGroupName, String colorTag, boolean visible) {
		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
		FavoriteGroup group = favoritesHelper.getGroup(prevGroupName);
		if (group != null) {
			int color = Algorithms.isEmpty(colorTag) ? 0 : ColorDialogs.getColorByTag(colorTag);

			favoritesHelper.updateGroupColor(group, color, true, false);
			favoritesHelper.updateGroupVisibility(group, visible, false);
			favoritesHelper.updateGroupName(group, newGroupName, false);

			favoritesHelper.saveCurrentPointsIntoFile(true);
			return true;
		}
		return false;
	}

	boolean addFavorite(double latitude, double longitude, String name, String category, String description, String colorTag, boolean visible) {
		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
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
		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
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

	boolean updateFavorite(String prevName, String prevCategory, double prevLat, double prevLon, String newName, String newCategory, String newDescription, String newAddress, double newLat, double newLon) {
		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
		List<FavouritePoint> favorites = favoritesHelper.getFavouritePoints();
		for (FavouritePoint f : favorites) {
			if (f.getName().equals(prevName) && f.getCategory().equals(prevCategory) &&
					f.getLatitude() == prevLat && f.getLongitude() == prevLon) {
				if (newLat != f.getLatitude() || newLon != f.getLongitude()) {
					favoritesHelper.editFavourite(f, newLat, newLon);
				}
				if (!newName.equals(f.getName()) || !newDescription.equals(f.getDescription()) ||
						!newCategory.equals(f.getCategory()) || !newAddress.equals(f.getAddress())) {
					favoritesHelper.editFavouriteName(f, newName, newCategory, newDescription, newAddress);
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
		markersHelper.addMapMarker(new LatLon(latitude, longitude), pd, null);
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
					MapMarker marker = new MapMarker(m.point, pd, m.colorIndex);
					marker.id = m.id;
					marker.selected = m.selected;
					marker.visitedDate = m.visitedDate;
					marker.creationDate = m.creationDate;
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
		int col = GpxAppearanceAdapter.parseTrackColor(
				app.getRendererRegistry().getCurrentSelectedRenderer(), color);
		if (!destinationExists) {
			GpxDataItem item = new GpxDataItem(app, destination);
			item.setParameter(COLOR, col);
			item.setParameter(API_IMPORTED, true);
			app.getGpxDbHelper().add(item);
		} else {
			GpxDataItem item = app.getGpxDbHelper().getItem(destination);
			if (item != null) {
				item.setParameter(COLOR, col);
				item.setParameter(API_IMPORTED, true);
				app.getGpxDbHelper().updateDataItem(item);
			}
		}
		GpxSelectionHelper helper = app.getSelectedGpxHelper();
		SelectedGpxFile selectedGpx = helper.getSelectedFileByPath(destination.getAbsolutePath());
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
							if (col != -1) {
								gpx.setColor(col);
							}
							selectedGpx.setGpxFile(gpx, app);
							refreshMap();
						}
					}

				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, destination);
			} else {
				GpxSelectionParams params = GpxSelectionParams.newInstance()
						.hideFromMap().syncGroup().saveSelection();
				helper.selectGpxFile(selectedGpx.getGpxFile(), params);
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
						GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
						helper.selectGpxFile(gpx, params);
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
						GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
						app.getSelectedGpxHelper().selectGpxFile(gpx, params);
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

	boolean hideGpx(String filePath, String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			GpxSelectionHelper selectionHelper = app.getSelectedGpxHelper();
			SelectedGpxFile selectedGpxFile = filePath != null
					? selectionHelper.getSelectedFileByPath(filePath)
					: selectionHelper.getSelectedFileByName(fileName);
			if (selectedGpxFile != null) {
				GpxSelectionParams params = GpxSelectionParams.newInstance()
						.hideFromMap().syncGroup().saveSelection();
				app.getSelectedGpxHelper().selectGpxFile(selectedGpxFile.getGpxFile(), params);
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
				String absolutePath = file.getAbsolutePath();
				boolean active = app.getSelectedGpxHelper().getSelectedFileByPath(absolutePath) != null;
				long modifiedTime = dataItem.getParameter(FILE_LAST_MODIFIED_TIME);
				long fileSize = file.length();
				int color = dataItem.getParameter(COLOR);
				String colorName = "";
				if (color != 0) {
					colorName = GpxAppearanceAdapter.parseTrackColorName(app.getRendererRegistry().getCurrentSelectedRenderer(), color);
				}
				net.osmand.aidlapi.gpx.AGpxFileDetails details = null;
				GPXTrackAnalysis analysis = dataItem.getAnalysis();
				if (analysis != null) {
					details = createGpxFileDetailsV2(analysis);
				}
				net.osmand.aidlapi.gpx.AGpxFile gpxFile = new net.osmand.aidlapi.gpx.AGpxFile(fileName, modifiedTime, fileSize, active, colorName, details);
				gpxFile.setRelativePath(GpxUiHelper.getGpxFileRelativePath(app, absolutePath));

				files.add(gpxFile);
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
				long modifiedTime = dataItem.getParameter(FILE_LAST_MODIFIED_TIME);
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
					int color = dataItem.getParameter(COLOR);
					if (color != 0) {
						return GpxAppearanceAdapter.parseTrackColorName(app.getRendererRegistry().getCurrentSelectedRenderer(), color);
					}
				}
			}
		}
		return null;
	}

	boolean removeGpx(@Nullable String fileName, @Nullable String relativePath) {
		File file = null;
		if (!Algorithms.isEmpty(relativePath)) {
			file = app.getAppPath(IndexConstants.GPX_INDEX_DIR + relativePath);
		} else if (!Algorithms.isEmpty(fileName)) {
			file = app.getAppPath(IndexConstants.GPX_INDEX_DIR + fileName);
		}

		if (file != null && file.exists()) {
			GpxDataItem item = app.getGpxDbHelper().getItem(file);
			boolean apiImported = item != null ? item.getParameter(API_IMPORTED) : false;
			if (apiImported) {
				return FileUtils.removeGpxFile(app, file);
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

	boolean setMapLocation(double latitude, double longitude, int zoom, float rotation, boolean animated) {
		Intent intent = new Intent();
		intent.setAction(AIDL_SET_MAP_LOCATION);
		intent.putExtra(AIDL_LATITUDE, latitude);
		intent.putExtra(AIDL_LONGITUDE, longitude);
		intent.putExtra(AIDL_ZOOM, zoom);
		intent.putExtra(AIDL_ROTATION, rotation);
		intent.putExtra(AIDL_ANIMATED, animated);
		app.sendBroadcast(intent);
		return true;
	}

	boolean startGpxRecording() {
		OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			plugin.startGPXMonitoring(null);
			plugin.updateWidgets();
			return true;
		}
		return false;
	}

	boolean stopGpxRecording() {
		OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			plugin.stopRecording();
			plugin.updateWidgets();
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
	                 String profile, boolean force, boolean requestLocationPermission) {
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
		intent.putExtra(AIDL_LOCATION_PERMISSION, requestLocationPermission);
		app.sendBroadcast(intent);
		return true;
	}

	boolean navigateSearch(String startName, double startLat, double startLon,
	                       String searchQuery, double searchLat, double searchLon,
	                       String profile, boolean force, boolean requestLocationPermission) {
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
		intent.putExtra(AIDL_LOCATION_PERMISSION, requestLocationPermission);
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

	boolean navigateGpx(String data, Uri uri, boolean force, boolean requestLocationPermission) {
		return mapActivity != null && NavigateGpxHelper.saveAndNavigateGpx(mapActivity, data, uri, force, requestLocationPermission);
	}

	boolean navigateGpxV2(@NonNull NavigateGpxParams params) {
		return mapActivity != null && NavigateGpxHelper.saveAndNavigateGpx(mapActivity, params);
	}

	boolean setLockState(boolean lock) {
		Intent intent = new Intent();
		intent.setAction(AIDL_LOCK_STATE);
		intent.putExtra(AIDL_LOCK_STATE, lock);
		app.sendBroadcast(intent);
		return true;
	}

	AppInfoParams getAppInfo() {
		ALatLon lastKnownLocation = null;
		Location location = app.getLocationProvider().getLastKnownLocation();
		if (location != null) {
			lastKnownLocation = new ALatLon(location.getLatitude(), location.getLongitude());
		}

		boolean mapVisible = false;
		ALatLon mapLocation = null;
		if (mapActivity != null) {
			LatLon mapLoc = mapActivity.getMapLocation();
			if (mapLoc != null) {
				mapLocation = new ALatLon(mapLoc.getLatitude(), mapLoc.getLongitude());
			}
			mapVisible = mapActivity.isMapVisible();
		}

		int leftTime = 0;
		int leftDistance = 0;
		long arrivalTime = 0;
		Bundle turnInfo = null;
		ALatLon destinationLocation = null;

		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isRouteCalculated()) {
			LatLon latLon = routingHelper.getFinalLocation();
			destinationLocation = new ALatLon(latLon.getLatitude(), latLon.getLongitude());

			leftTime = routingHelper.getLeftTime();
			leftDistance = routingHelper.getLeftDistance();
			arrivalTime = leftTime + System.currentTimeMillis() / 1000;
			turnInfo = ExternalApiHelper.getRouteDirectionsInfo(app);
		}
		AppInfoParams params = new AppInfoParams(lastKnownLocation, mapLocation, turnInfo, leftTime, leftDistance, arrivalTime, mapVisible);
		params.setVersionsInfo(ExternalApiHelper.getPluginAndProfileVersions());
		params.setDestinationLocation(destinationLocation);
		params.setOsmAndVersion(Version.getFullVersion(app));
		String releaseDate = app.getString(R.string.app_edition);
		params.setReleaseDate(releaseDate.isEmpty() ? null : releaseDate);
		params.setRoutingData(app.getAnalyticsHelper().getRoutingRecordedData());

		return params;
	}

	boolean search(String searchQuery, int searchType, double latitude, double longitude,
	               int radiusLevel, int totalLimit, SearchCompleteCallback callback) {
		if (Algorithms.isEmpty(searchQuery) || latitude == 0 || longitude == 0 || callback == null) {
			return false;
		}
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onFinish(@NonNull AppInitializer init) {
					ExternalApiHelper.runSearch(app, searchQuery, searchType, latitude, longitude, radiusLevel, totalLimit, callback);
				}
			});
		} else {
			ExternalApiHelper.runSearch(app, searchQuery, searchType, latitude, longitude, radiusLevel, totalLimit, callback);
		}
		return true;
	}

	boolean registerForOsmandInitialization(OsmandAppInitCallback callback) {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {

				@Override
				public void onFinish(@NonNull AppInitializer init) {
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

	public void registerNavDrawerItems(Activity activity, ContextMenuAdapter adapter) {
		app.getAppCustomization().registerNavDrawerItems(activity, adapter);
	}

	@Nullable
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

	void registerForNavigationUpdates(long id) {
		NextDirectionInfo baseNdi = new NextDirectionInfo();
		IRoutingDataUpdateListener listener = () -> {
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
		};
		navUpdateCallbacks.put(id, listener);
		app.getRoutingHelper().addRouteDataListener(listener);
	}

	public void unregisterFromUpdates(long id) {
		IRoutingDataUpdateListener callback = navUpdateCallbacks.remove(id);
		if (callback != null) {
			app.getRoutingHelper().removeRouteDataListener(callback);
		}
	}

	public void registerForVoiceRouterMessages(long id) {
		VoiceRouter.VoiceMessageListener listener = (cmds, played) -> {
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
		};
		voiceRouterMessageCallbacks.put(id, listener);
		app.getRoutingHelper().getVoiceRouter().addVoiceMessageListener(listener);
	}

	public void unregisterFromVoiceRouterMessages(long id) {
		VoiceMessageListener callback = voiceRouterMessageCallbacks.remove(id);
		if (callback != null) {
			app.getRoutingHelper().getVoiceRouter().removeVoiceMessageListener(callback);
		}
	}

	public void registerLogcatListener(long id, String filterLevel) {
		LogcatMessageListener listener = (_filterLevel, logs) -> {
			if (aidlCallbackListenerV2 != null) {
				for (OsmandAidlServiceV2.AidlCallbackParams cb : aidlCallbackListenerV2.getAidlCallbacks().values()) {
					if (!aidlCallbackListenerV2.getAidlCallbacks().isEmpty() && (cb.getKey() & KEY_ON_LOGCAT_MESSAGE) > 0) {
						try {
							cb.getCallback().onLogcatMessage(new OnLogcatMessageParams(_filterLevel, logs));
						} catch (Exception e) {
							LOG.error(e.getMessage(), e);
						}
					}
				}
			}
		};
		stopLogcatTask(id);
		LogcatAsyncTask task = new LogcatAsyncTask(listener, filterLevel);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		logcatAsyncTasks.put(id, task);
	}

	public void stopLogcatTask(long id) {
		LogcatAsyncTask task = logcatAsyncTasks.remove(id);
		if (task != null) {
			task.stop();
		}
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
		IContextMenuButtonListener listener = (buttonId, pointId, layerId) -> {
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
		};
		buttonsParams.setCallbackId(callbackId);
		contextMenuButtonsCallbacks.put(callbackId, listener);
	}

	private final Map<Long, IContextMenuButtonListener> contextMenuButtonsCallbacks = new ConcurrentHashMap<>();

	public void contextMenuCallbackButtonClicked(long callbackId, int buttonId, String pointId, String layerId) {
		IContextMenuButtonListener contextMenuButtonListener = contextMenuButtonsCallbacks.get(callbackId);
		if (contextMenuButtonListener != null) {
			contextMenuButtonListener.onContextMenuButtonClicked(buttonId, pointId, layerId);
		}
	}

	boolean getBitmapForGpx(Uri gpxUri, float density, int widthPixels,
	                        int heightPixels, int color, GpxBitmapCreatedCallback callback) {
		if (gpxUri == null || callback == null) {
			return false;
		}
		TrackBitmapDrawerListener drawerListener = new TrackBitmapDrawerListener() {
			@Override
			public void onTrackBitmapDrawing() {
			}

			@Override
			public void onTrackBitmapDrawn(boolean success) {

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
			app.getAppInitializer().addListener(new AppInitializeListener() {

				@Override
				public void onFinish(@NonNull AppInitializer init) {
					createGpxBitmapFromUri(gpxUri, density, widthPixels, heightPixels, color, drawerListener);
				}
			});
		} else {
			createGpxBitmapFromUri(gpxUri, density, widthPixels, heightPixels, color, drawerListener);
		}
		return true;
	}

	private void createGpxBitmapFromUri(Uri gpxUri, float density, int widthPixels,
	                                    int heightPixels, int color, TrackBitmapDrawerListener drawerListener) {
		GpxAsyncLoaderTask gpxAsyncLoaderTask = new GpxAsyncLoaderTask(app, gpxUri, result -> {
			TracksDrawParams drawParams = new TracksDrawParams(density, widthPixels, heightPixels, color);
			TrackBitmapDrawer trackBitmapDrawer = new TrackBitmapDrawer(app, result, drawParams, null);
			trackBitmapDrawer.addListener(drawerListener);
			trackBitmapDrawer.setDrawEnabled(true);
			trackBitmapDrawer.initAndDraw();
			return false;
		});
		gpxAsyncLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private final Map<String, FileCopyInfo> copyFilesCache = new ConcurrentHashMap<>();

	public boolean importProfile(Uri profileUri, String latestChanges, int version) {
		if (profileUri != null) {
			Bundle bundle = new Bundle();
			bundle.putString(SettingsHelper.SETTINGS_LATEST_CHANGES_KEY, latestChanges);
			bundle.putInt(SettingsHelper.SETTINGS_VERSION_KEY, version);

			MapActivity.launchMapActivityMoveToTop(app, null, profileUri, bundle);
			return true;
		}
		return false;
	}

	public boolean importProfileV2(Uri profileUri, List<String> settingsTypeKeys, boolean replace,
	                               boolean silent, String latestChanges, int version) {
		if (profileUri != null) {
			Bundle bundle = new Bundle();
			bundle.putStringArrayList(SettingsHelper.EXPORT_TYPE_LIST_KEY, new ArrayList<>(settingsTypeKeys));
			bundle.putBoolean(REPLACE_KEY, replace);
			bundle.putBoolean(SILENT_IMPORT_KEY, silent);
			bundle.putString(SettingsHelper.SETTINGS_LATEST_CHANGES_KEY, latestChanges);
			bundle.putInt(SettingsHelper.SETTINGS_VERSION_KEY, version);

			MapActivity.launchMapActivityMoveToTop(app, null, profileUri, bundle);
			return true;
		}
		return false;
	}

	public void registerLayerContextMenu(ContextMenuAdapter adapter, MapActivity mapActivity) {
		for (ConnectedApp connectedApp : getConnectedApps()) {
			if (!connectedApp.getLayers().isEmpty()) {
				connectedApp.registerLayerContextMenu(adapter, mapActivity);
			}
		}
	}

	public boolean executeQuickAction(int actionNumber) {
		Intent intent = new Intent();
		intent.setAction(AIDL_EXECUTE_QUICK_ACTION);
		intent.putExtra(AIDL_QUICK_ACTION_NUMBER, actionNumber);
		app.sendBroadcast(intent);
		return true;
	}

	public boolean getQuickActionsInfo(List<QuickActionInfoParams> quickActions) {
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<String, String>>() {
		}.getType();

		List<QuickAction> actionsList = app.getQuickActionRegistry().getQuickActions();
		for (int i = 0; i < actionsList.size(); i++) {
			QuickAction action = actionsList.get(i);
			String name = action.getName(app);
			String actionType = action.getActionType().getStringId();
			String params = gson.toJson(action.getParams(), type);

			quickActions.add(new QuickActionInfoParams(i, name, actionType, params));
		}
		return true;
	}

	public boolean getQuickActionsInfoV2(List<net.osmand.aidlapi.quickaction.QuickActionInfoParams> quickActions) {
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<String, String>>() {
		}.getType();

		List<QuickAction> actionsList = app.getQuickActionRegistry().getQuickActions();
		for (int i = 0; i < actionsList.size(); i++) {
			QuickAction action = actionsList.get(i);
			String name = action.getName(app);
			String actionType = action.getActionType().getStringId();
			String params = gson.toJson(action.getParams(), type);

			quickActions.add(new net.osmand.aidlapi.quickaction.QuickActionInfoParams(i, name, actionType, params));
		}
		return true;
	}

	public boolean exportProfile(String appModeKey, List<String> acceptedExportTypeKeys) {
		ApplicationMode appMode = ApplicationMode.valueOfStringKey(appModeKey, null);
		if (app != null && appMode != null) {
			List<ExportType> acceptedExportTypes = ExportType.valuesOf(acceptedExportTypeKeys);
			acceptedExportTypes.remove(ExportType.PROFILE);
			List<SettingsItem> settingsItems = new ArrayList<>();
			settingsItems.add(new ProfileSettingsItem(app, appMode));
			File exportDir = app.getSettings().getExternalStorageDirectory();
			String fileName = appMode.toHumanString();
			FileSettingsHelper settingsHelper = app.getFileSettingsHelper();
			settingsItems.addAll(settingsHelper.getFilteredSettingsItems(acceptedExportTypes, true, false, true));
			settingsHelper.exportSettings(exportDir, fileName, null, settingsItems, true);
			return true;
		}
		return false;
	}

	public boolean isFragmentOpen() {
		return mapActivity.getFragmentsHelper().isFragmentVisible();
	}

	public boolean isMenuOpen() {
		return mapActivity.getContextMenu().isVisible();
	}

	public int getPluginVersion(String pluginName) {
		OsmandPlugin plugin = PluginsHelper.getPlugin(pluginName);
		if (plugin instanceof CustomOsmandPlugin) {
			CustomOsmandPlugin customPlugin = (CustomOsmandPlugin) plugin;
			return customPlugin.getVersion();
		}
		return CANNOT_ACCESS_API_ERROR;
	}

	public boolean selectProfile(String appModeKey) {
		ApplicationMode appMode = ApplicationMode.valueOfStringKey(appModeKey, null);
		if (appMode != null) {
			app.runInUIThread(() -> {
				if (!ApplicationMode.values(app).contains(appMode)) {
					ApplicationMode.changeProfileAvailability(appMode, true, app);
				}
				app.getSettings().setApplicationMode(appMode);
			});
			return true;
		}
		return false;
	}

	public boolean getProfiles(List<AProfile> profiles) {
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			ApplicationModeBean bean = mode.toModeBean();
			AProfile aProfile = new AProfile(bean.stringKey, bean.userProfileName, bean.parent, bean.iconName,
					bean.iconColor.name(), bean.routingProfile, bean.routeService.name(), bean.locIcon.name(),
					bean.navIcon.name(), bean.order);
			aProfile.setVersion(bean.version);

			profiles.add(aProfile);
		}
		return true;
	}

	public boolean getBlockedRoads(List<ABlockedRoad> blockedRoads) {
		Map<LatLon, AvoidRoadInfo> impassableRoads = app.getAvoidSpecificRoads().getImpassableRoads();
		for (AvoidRoadInfo info : impassableRoads.values()) {
			blockedRoads.add(new ABlockedRoad(info.id, info.latitude, info.longitude, info.direction, info.name, info.appModeKey));
		}
		return true;
	}

	public boolean addRoadBlock(ABlockedRoad road) {
		LatLon latLon = new LatLon(road.getLatitude(), road.getLongitude());
		app.getAvoidSpecificRoads().addImpassableRoad(null, latLon, false, false, road.getAppModeKey());
		return true;
	}

	public boolean removeRoadBlock(ABlockedRoad road) {
		app.getAvoidSpecificRoads().removeImpassableRoad(new LatLon(road.getLatitude(), road.getLongitude()));
		return true;
	}

	public boolean setLocation(String packName, ALocation location, long timeToNotUseOtherGPS) {
		Intent intent = new Intent();
		intent.setAction(AIDL_SET_LOCATION);
		intent.putExtra(AIDL_LOCATION, location);
		intent.putExtra(AIDL_PACKAGE_NAME, packName);
		intent.putExtra(AIDL_TIME_TO_NOT_USE_OTHER_GPS, timeToNotUseOtherGPS);
		app.sendBroadcast(intent);
		return true;
	}

	public boolean exitApp(ExitAppParams params) {
		Intent intent = new Intent();
		intent.setAction(AIDL_EXIT_APP);
		intent.putExtra(AIDL_EXIT_APP_RESTART, params.shouldRestart());
		app.sendBroadcast(intent);
		return true;
	}

	public boolean getText(GetTextParams params) {
		Context context = app.getLocaleHelper().getLocalizedContext(params.getLocale());
		params.setValue(AndroidUtils.getStringByProperty(context, params.getKey()));
		return true;
	}

	public boolean reloadIndexes() {
		app.getResourceManager().reloadIndexesAsync(null, null);
		return true;
	}

	public boolean setPreference(PreferenceParams params) {
		String prefId = params.getPrefId();
		OsmandSettings settings = app.getSettings();
		OsmandPreference<?> pref = settings.getPreference(prefId);
		if (pref != null && settings.isExportAvailableForPref(pref)) {
			String value = params.getValue();
			ApplicationMode appMode = ApplicationMode.valueOfStringKey(params.getAppModeKey(), null);

			boolean success = settings.setPreference(prefId, value, appMode);
			if (success && settings.isRenderProperty(prefId) && mapActivity != null) {
				mapActivity.refreshMapComplete();
			}
			return success;
		}
		return false;
	}

	public boolean getPreference(PreferenceParams params) {
		String prefId = params.getPrefId();
		OsmandSettings settings = app.getSettings();
		OsmandPreference<?> pref = settings.getPreference(prefId);
		if (pref != null && settings.isExportAvailableForPref(pref)) {
			ApplicationMode appMode = ApplicationMode.valueOfStringKey(params.getAppModeKey(), null);
			String value = appMode != null ? pref.asStringModeValue(appMode) : pref.asString();
			params.setValue(value);
			return true;
		}
		return false;
	}

	private static class FileCopyInfo {
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

	int copyFileV2(String destinationDir, String fileName, byte[] filePartData, long startTime, boolean done) {
		if (Algorithms.isEmpty(fileName) || filePartData == null) {
			return COPY_FILE_PARAMS_ERROR;
		}
		if (filePartData.length > COPY_FILE_PART_SIZE_LIMIT) {
			return COPY_FILE_PART_SIZE_LIMIT_ERROR;
		}
		int result = copyFileImpl(fileName, filePartData, startTime, done, destinationDir);
		if (done) {
			if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) && IndexConstants.MAPS_PATH.equals(destinationDir)) {
				app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<>());
				app.getDownloadThread().updateLoadedFiles();
			} else if (fileName.endsWith(IndexConstants.GPX_FILE_EXT)) {
				if (destinationDir.startsWith(IndexConstants.GPX_INDEX_DIR)
						&& !(LEGACY_FAV_FILE_PREFIX + GPX_FILE_EXT).equals(fileName)) {
					destinationDir = destinationDir.replaceFirst(IndexConstants.GPX_INDEX_DIR, "");
					showGpx(new File(destinationDir, fileName).getPath());
				} else if (destinationDir.isEmpty() && (LEGACY_FAV_FILE_PREFIX + GPX_FILE_EXT).equals(fileName)) {
					app.getFavoritesHelper().loadFavorites();
				}
			}
		}
		return result;
	}

	private int copyFileImpl(String fileName, byte[] filePartData, long startTime, boolean done, String destinationDir) {
		File tempDir = FileUtils.getTempDir(app);
		File file = new File(tempDir, fileName);
		File destFile = app.getAppPath(new File(destinationDir, fileName).getPath());
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

		GpxAsyncLoaderTask(@NonNull OsmandApplication app, @NonNull Uri gpxUri, CallbackWithObject<GPXFile> callback) {
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
				FileDescriptor fileDescriptor = gpxParcelDescriptor.getFileDescriptor();
				return GPXUtilities.loadGPXFile(new FileInputStream(fileDescriptor));
			}
			return null;
		}
	}

	private static AGpxFileDetails createGpxFileDetails(@NonNull GPXTrackAnalysis a) {
		return new AGpxFileDetails(a.getTotalDistance(), a.getTotalTracks(), a.getStartTime(), a.getEndTime(),
				a.getTimeSpan(), a.getTimeMoving(), a.getTotalDistanceMoving(), a.getDiffElevationUp(), a.getDiffElevationDown(),
				a.getAvgElevation(), a.getMinElevation(), a.getMaxElevation(), a.getMinSpeed(), a.getMaxSpeed(), a.getAvgSpeed(),
				a.getPoints(), a.getWptPoints(), a.getWptCategoryNamesSet());
	}

	private static net.osmand.aidlapi.gpx.AGpxFileDetails createGpxFileDetailsV2(@NonNull GPXTrackAnalysis a) {
		return new net.osmand.aidlapi.gpx.AGpxFileDetails(a.getTotalDistance(), a.getTotalTracks(), a.getStartTime(), a.getEndTime(),
				a.getTimeSpan(), a.getTimeMoving(), a.getTotalDistanceMoving(), a.getDiffElevationUp(), a.getDiffElevationDown(),
				a.getAvgElevation(), a.getMinElevation(), a.getMaxElevation(), a.getMinSpeed(), a.getMaxSpeed(), a.getAvgSpeed(),
				a.getPoints(), a.getWptPoints(), a.getWptCategoryNamesSet());
	}

	public boolean onKeyEvent(KeyEvent event) {
		if (aidlCallbackListenerV2 != null) {
			for (Map.Entry<Long, OsmandAidlServiceV2.AidlCallbackParams> entry : aidlCallbackListenerV2.getAidlCallbacks().entrySet()) {
				OsmandAidlServiceV2.AidlCallbackParams cb = entry.getValue();
				if ((cb.getKey() & KEY_ON_KEY_EVENT) > 0) {
					Set<Integer> keyEventsList = keyEventCallbacks.get(entry.getKey());
					// An empty list means all key are requested
					if (keyEventsList != null && (keyEventsList.isEmpty() || keyEventsList.contains(event.getKeyCode()))) {
						try {
							cb.getCallback().onKeyEvent(event);
							return true;
						} catch (Exception e) {
							LOG.error(e.getMessage(), e);
						}
					}
				}
			}
		}
		return false;
	}

	void registerForKeyEvents(long id, ArrayList<Integer> keyEventLst) {
		keyEventCallbacks.put(id, new HashSet<>(keyEventLst));
	}

	public void unregisterFromKeyEvents(long id) {
		keyEventCallbacks.remove(id);
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