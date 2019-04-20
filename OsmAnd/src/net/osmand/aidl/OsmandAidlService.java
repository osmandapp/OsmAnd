package net.osmand.aidl;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi.GpxBitmapCreatedCallback;
import net.osmand.aidl.OsmandAidlApi.OsmandAppInitCallback;
import net.osmand.aidl.OsmandAidlApi.SearchCompleteCallback;
import net.osmand.aidl.calculateroute.CalculateRouteParams;
import net.osmand.aidl.contextmenu.ContextMenuButtonsParams;
import net.osmand.aidl.contextmenu.RemoveContextMenuButtonsParams;
import net.osmand.aidl.contextmenu.UpdateContextMenuButtonsParams;
import net.osmand.aidl.customization.OsmandSettingsParams;
import net.osmand.aidl.customization.SetWidgetsParams;
import net.osmand.aidl.favorite.AddFavoriteParams;
import net.osmand.aidl.favorite.RemoveFavoriteParams;
import net.osmand.aidl.favorite.UpdateFavoriteParams;
import net.osmand.aidl.favorite.group.AddFavoriteGroupParams;
import net.osmand.aidl.favorite.group.RemoveFavoriteGroupParams;
import net.osmand.aidl.favorite.group.UpdateFavoriteGroupParams;
import net.osmand.aidl.gpx.AGpxBitmap;
import net.osmand.aidl.gpx.AGpxFile;
import net.osmand.aidl.gpx.ASelectedGpxFile;
import net.osmand.aidl.gpx.CreateGpxBitmapParams;
import net.osmand.aidl.gpx.HideGpxParams;
import net.osmand.aidl.gpx.ImportGpxParams;
import net.osmand.aidl.gpx.RemoveGpxParams;
import net.osmand.aidl.gpx.ShowGpxParams;
import net.osmand.aidl.gpx.StartGpxRecordingParams;
import net.osmand.aidl.gpx.StopGpxRecordingParams;
import net.osmand.aidl.map.SetMapLocationParams;
import net.osmand.aidl.maplayer.AddMapLayerParams;
import net.osmand.aidl.maplayer.RemoveMapLayerParams;
import net.osmand.aidl.maplayer.UpdateMapLayerParams;
import net.osmand.aidl.maplayer.point.AddMapPointParams;
import net.osmand.aidl.maplayer.point.RemoveMapPointParams;
import net.osmand.aidl.maplayer.point.ShowMapPointParams;
import net.osmand.aidl.maplayer.point.UpdateMapPointParams;
import net.osmand.aidl.mapmarker.AddMapMarkerParams;
import net.osmand.aidl.mapmarker.RemoveMapMarkerParams;
import net.osmand.aidl.mapmarker.UpdateMapMarkerParams;
import net.osmand.aidl.mapwidget.AddMapWidgetParams;
import net.osmand.aidl.mapwidget.RemoveMapWidgetParams;
import net.osmand.aidl.mapwidget.UpdateMapWidgetParams;
import net.osmand.aidl.navdrawer.NavDrawerFooterParams;
import net.osmand.aidl.navdrawer.NavDrawerHeaderParams;
import net.osmand.aidl.navdrawer.SetNavDrawerItemsParams;
import net.osmand.aidl.navigation.ANavigationUpdateParams;
import net.osmand.aidl.navigation.MuteNavigationParams;
import net.osmand.aidl.navigation.NavigateGpxParams;
import net.osmand.aidl.navigation.NavigateParams;
import net.osmand.aidl.navigation.NavigateSearchParams;
import net.osmand.aidl.navigation.PauseNavigationParams;
import net.osmand.aidl.navigation.ResumeNavigationParams;
import net.osmand.aidl.navigation.StopNavigationParams;
import net.osmand.aidl.navigation.UnmuteNavigationParams;
import net.osmand.aidl.note.StartAudioRecordingParams;
import net.osmand.aidl.note.StartVideoRecordingParams;
import net.osmand.aidl.note.StopRecordingParams;
import net.osmand.aidl.note.TakePhotoNoteParams;
import net.osmand.aidl.plugins.PluginParams;
import net.osmand.aidl.search.SearchParams;
import net.osmand.aidl.search.SearchResult;
import net.osmand.aidl.tiles.ASqliteDbFile;
import net.osmand.aidl.copyfile.CopyFileParams;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static net.osmand.aidl.OsmandAidlConstants.CANNOT_ACCESS_API_ERROR;
import static net.osmand.aidl.OsmandAidlConstants.MIN_UPDATE_TIME_MS;
import static net.osmand.aidl.OsmandAidlConstants.MIN_UPDATE_TIME_MS_ERROR;
import static net.osmand.aidl.OsmandAidlConstants.UNKNOWN_API_ERROR;

public class OsmandAidlService extends Service implements AidlCallbackListener {

	private static final Log LOG = PlatformUtil.getLog(OsmandAidlService.class);

	private static final String DATA_KEY_RESULT_SET = "resultSet";

	public static final int KEY_ON_UPDATE = 1;
	public static final int KEY_ON_NAV_DATA_UPDATE = 2;
	public static final int KEY_ON_CONTEXT_MENU_BUTTONS_CLICK = 4;

	private Map<Long, AidlCallbackParams> callbacks = new ConcurrentHashMap<>();
	private Handler mHandler = null;
	HandlerThread mHandlerThread = new HandlerThread("OsmAndAidlServiceThread");

	private final AtomicLong aidlCallbackId = new AtomicLong(0);

	private OsmandApplication getApp() {
		return (OsmandApplication) getApplication();
	}

	@Nullable
	private OsmandAidlApi getApi(String reason) {
		LOG.info("Request AIDL API for " + reason);
		OsmandAidlApi api = getApp().getAidlApi();
		String pack = getApp().getPackageManager().getNameForUid(Binder.getCallingUid());
		if (pack != null && !pack.equals(getApp().getPackageName()) && !api.isAppEnabled(pack)) {
			return null;
		}
		return api;
	}

	@Override
	public IBinder onBind(Intent intent) {
		mHandlerThread.start();
		mHandler = new Handler(mHandlerThread.getLooper());

		// Return the interface
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		OsmandAidlApi api = getApi("setting_listener");
		if(api != null) {
			api.aidlCallbackListener = this;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		callbacks.clear();
		OsmandAidlApi api = getApi("clear_listener");
		if(api != null) {
			api.aidlCallbackListener = null;
		}
		mHandlerThread.quit();
	}

	private long getCallbackId() {
		return aidlCallbackId.get();
	}

	private long getAndIncrementCallbackId() {
		return aidlCallbackId.getAndIncrement();
	}

	@Override
	public long addAidlCallback(IOsmAndAidlCallback callback, int key) {
		callbacks.put(getAndIncrementCallbackId(), new AidlCallbackParams(callback, key));
		return getCallbackId();
	}

	@Override
	public boolean removeAidlCallback(long id) {
		for (Long key : callbacks.keySet()) {
			if (key == id) {
				callbacks.remove(id);
				return true;
			}
		}
		return false;
	}

	@Override
	public Map<Long, AidlCallbackParams> getAidlCallbacks() {
		return callbacks;
	}

	private final IOsmAndAidlInterface.Stub mBinder = new IOsmAndAidlInterface.Stub() {

		private void handleException(Exception e) {
			LOG.error("AIDL e.getMessage()", e);
		}

		@Override
		public boolean refreshMap() {
			try {
				OsmandAidlApi api = getApi("refreshMap");
				return api != null && api.reloadMap();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}


		@Override
		public boolean addFavoriteGroup(AddFavoriteGroupParams params) {
			try {
				OsmandAidlApi api = getApi("addFavoriteGroup");
				return params != null && api != null && api.addFavoriteGroup(params.getFavoriteGroup());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeFavoriteGroup(RemoveFavoriteGroupParams params) {
			try {
				OsmandAidlApi api = getApi("removeFavoriteGroup");
				return params != null && api != null && api.removeFavoriteGroup(params.getFavoriteGroup());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateFavoriteGroup(UpdateFavoriteGroupParams params) {
			try {
				OsmandAidlApi api = getApi("updateFavoriteGroup");
				return params != null && api != null && api.updateFavoriteGroup(params.getFavoriteGroupPrev(), params.getFavoriteGroupNew());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addFavorite(AddFavoriteParams params) {
			try {
				OsmandAidlApi api = getApi("addFavorite");
				return params != null && api != null && api.addFavorite(params.getFavorite());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeFavorite(RemoveFavoriteParams params) {
			try {
				OsmandAidlApi api = getApi("removeFavorite");
				return params != null && api != null && api.removeFavorite(params.getFavorite());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateFavorite(UpdateFavoriteParams params) {
			try {
				OsmandAidlApi api = getApi("updateFavorite");
				return params != null && api != null && api.updateFavorite(params.getFavoritePrev(), params.getFavoriteNew());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapMarker(AddMapMarkerParams params) {
			try {
				OsmandAidlApi api = getApi("addMapMarker");
				return params != null && api != null && api.addMapMarker(params.getMarker());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean removeMapMarker(RemoveMapMarkerParams params) {
			try {
				OsmandAidlApi api = getApi("removeMapMarker");
				return params != null && api != null && api.removeMapMarker(params.getMarker());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapMarker(UpdateMapMarkerParams params) {
			try {
				OsmandAidlApi api = getApi("updateMapMarker");
				return params != null && api != null && api.updateMapMarker(params.getMarkerPrev(), params.getMarkerNew());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapWidget(AddMapWidgetParams params) {
			try {
				OsmandAidlApi api = getApi("addMapWidget");
				return params != null && api != null && api.addMapWidget(params.getWidget());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapWidget(RemoveMapWidgetParams params) {
			try {
				OsmandAidlApi api = getApi("removeMapWidget");
				return params != null && api != null && api.removeMapWidget(params.getId());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean updateMapWidget(UpdateMapWidgetParams params) {
			try {
				OsmandAidlApi api = getApi("updateMapWidget");
				return params != null && api != null && api.updateMapWidget(params.getWidget());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean showMapPoint(ShowMapPointParams params) {
			try {
				OsmandAidlApi api = getApi("showMapPoint");
				return params != null && api != null && api.showMapPoint(params.getLayerId(), params.getPoint());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapPoint(AddMapPointParams params) {
			try {
				OsmandAidlApi api = getApi("addMapPoint");
				return params != null && api != null && api.putMapPoint(params.getLayerId(), params.getPoint());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapPoint(RemoveMapPointParams params) {
			try {
				OsmandAidlApi api = getApi("removeMapPoint");
				return params != null && api != null && api.removeMapPoint(params.getLayerId(), params.getPointId());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapPoint(UpdateMapPointParams params) {
			try {
				OsmandAidlApi api = getApi("updateMapPoint");
				return params != null && api != null && api.updateMapPoint(params.getLayerId(), params.getPoint(), params.isUpdateOpenedMenuAndMap());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapLayer(AddMapLayerParams params) {
			try {
				OsmandAidlApi api = getApi("addMapLayer");
				return params != null && api != null && api.addMapLayer(params.getLayer());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapLayer(RemoveMapLayerParams params) {
			try {
				OsmandAidlApi api = getApi("removeMapLayer");
				return params != null && api != null && api.removeMapLayer(params.getId());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapLayer(UpdateMapLayerParams params) {
			try {
				OsmandAidlApi api = getApi("updateMapLayer");
				return params != null && api != null && api.updateMapLayer(params.getLayer());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean importGpx(ImportGpxParams params) {
			try {
				if (params != null && !Algorithms.isEmpty(params.getDestinationPath())) {
					OsmandAidlApi api = getApi("importGpx");
					if (api != null) {
						if (params.getGpxFile() != null) {
							return api.importGpxFromFile(params.getGpxFile(), params.getDestinationPath(),
									params.getColor(), params.isShow());
						} else if (params.getGpxUri() != null) {
							return api.importGpxFromUri(params.getGpxUri(), params.getDestinationPath(),
									params.getColor(), params.isShow());
						} else if (params.getSourceRawData() != null) {
							return api.importGpxFromData(params.getSourceRawData(), params.getDestinationPath(),
									params.getColor(), params.isShow());
						}
					}
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean showGpx(ShowGpxParams params) {
			try {
				if (params != null && params.getFileName() != null) {
					OsmandAidlApi api = getApi("showGpx");
					return api != null && api.showGpx(params.getFileName());
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean hideGpx(HideGpxParams params) {
			try {
				if (params != null && params.getFileName() != null) {
					OsmandAidlApi api = getApi("hideGpx");
					return api != null && api.hideGpx(params.getFileName());
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean getActiveGpx(List<ASelectedGpxFile> files) {
			try {
				OsmandAidlApi api = getApi("getActiveGpx");
				return api != null && api.getActiveGpx(files);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean getImportedGpx(List<AGpxFile> files) {
			try {
				OsmandAidlApi api = getApi("getImportedGpx");
				return api != null && api.getImportedGpx(files);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeGpx(RemoveGpxParams params) {
			try {
				if (params != null && params.getFileName() != null) {
					OsmandAidlApi api = getApi("removeGpx");
					return api != null && api.removeGpx(params.getFileName());
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setMapLocation(SetMapLocationParams params) {
			try {
				if (params != null) {
					OsmandAidlApi api = getApi("setMapLocation");
					return api != null && api.setMapLocation(params.getLatitude(), params.getLongitude(),
							params.getZoom(), params.isAnimated());
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean calculateRoute(CalculateRouteParams params) {
			try {
				if (params == null || params.getEndPoint() == null) {
					return false;
				} else {
				/*
				final TargetPointsHelper targets = app.getTargetPointsHelper();
				targets.removeAllWayPoints(false, true);

				List<ALatLon> intermediatePoints = params.getIntermediatePoints();
				List<String> intermediateNames = params.getIntermediateNames();
				ALatLon intermediatePoint;
				String intermediateName;
				for (int i = 0; i < intermediatePoints.size(); i++ ) {
					intermediatePoint = intermediatePoints.get(i);
					if (i < intermediateNames.size()) {
						intermediateName = intermediateNames.get(i);
					} else {
						intermediateName = "";
					}
					if (intermediateName == null) {
						intermediateName = "";
					}
					targets.navigateToPoint(
							new LatLon(intermediatePoint.getLatitude(), intermediatePoint.getLongitude()),
							false, -1, new PointDescription(PointDescription.POINT_TYPE_LOCATION, intermediateName));
				}

				PointDescription endPointDescription = null;
				if (params.getEndPointName() != null) {
					endPointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, params.getEndPointName());
				}
				targets.navigateToPoint(
						new LatLon(params.getEndPoint().getLatitude(), params.getEndPoint().getLongitude()),
						true, -1, endPointDescription);

				LatLon startPoint = null;
				if (params.getStartPoint() != null) {
					startPoint = new LatLon(params.getStartPoint().getLatitude(), params.getStartPoint().getLongitude());
				}
				PointDescription startPointDescription = null;
				if (params.getStartPointName() != null) {
					startPointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, params.getStartPointName());
				}

				//mapActivity.getMapActions().enterRoutePlanningModeGivenGpx(null, startPoint, startPointDescription, true, false);
				*/
					return true;
				}
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean startGpxRecording(StartGpxRecordingParams params) {
			try {
				OsmandAidlApi api = getApi("startGpxRecording");
				return api != null && api.startGpxRecording(params);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopGpxRecording(StopGpxRecordingParams params) {
			try {
				OsmandAidlApi api = getApi("stopGpxRecording");
				return api != null && api.stopGpxRecording(params);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean takePhotoNote(TakePhotoNoteParams params) {
			try {
				OsmandAidlApi api = getApi("takePhotoNote");
				return params != null && api != null && api.takePhotoNote(params.getLatitude(), params.getLongitude());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean startVideoRecording(StartVideoRecordingParams params) {
			try {
				OsmandAidlApi api = getApi("startVideoRecording");
				return params != null && api != null && api.startVideoRecording(params.getLatitude(), params.getLongitude());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean startAudioRecording(StartAudioRecordingParams params) {
			try {
				OsmandAidlApi api = getApi("startAudioRecording");
				return params != null && api != null && api.startAudioRecording(params.getLatitude(), params.getLongitude());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopRecording(StopRecordingParams params) {
			try {
				OsmandAidlApi api = getApi("stopRecording");
				return api != null && api.stopRecording();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean navigate(NavigateParams params) {
			try {
				OsmandAidlApi api = getApi("navigate");
				return params != null && api != null && api.navigate(
						params.getStartName(), params.getStartLat(), params.getStartLon(),
						params.getDestName(), params.getDestLat(), params.getDestLon(),
						params.getProfile(), params.isForce());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean navigateGpx(NavigateGpxParams params) {
			try {
				OsmandAidlApi api = getApi("navigateGpx");
				return params != null && api != null && api.navigateGpx(params.getData(), params.getUri(), params.isForce());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean pauseNavigation(PauseNavigationParams params) {
			try {
				OsmandAidlApi api = getApi("pauseNavigation");
				return api != null && api.pauseNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean resumeNavigation(ResumeNavigationParams params) {
			try {
				OsmandAidlApi api = getApi("resumeNavigation");
				return api != null && api.resumeNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopNavigation(StopNavigationParams params) {
			try {
				OsmandAidlApi api = getApi("stopNavigation");
				return api != null && api.stopNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean muteNavigation(MuteNavigationParams params) {
			try {
				OsmandAidlApi api = getApi("muteNavigation");
				return api != null && api.muteNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean unmuteNavigation(UnmuteNavigationParams params) {
			try {
				OsmandAidlApi api = getApi("unmuteNavigation");
				return api != null && api.unmuteNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setNavDrawerItems(SetNavDrawerItemsParams params) {
			try {
				OsmandAidlApi api = getApi("setNavDrawerItems");
				return params != null && api != null && api.setNavDrawerItems(params.getAppPackage(), params.getItems());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean search(SearchParams params, final IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("search");
				return params != null && api != null && api.search(params.getSearchQuery(), params.getSearchType(),
						params.getLatitude(), params.getLongitude(), params.getRadiusLevel(), params.getTotalLimit(), new SearchCompleteCallback() {
							@Override
							public void onSearchComplete(List<SearchResult> resultSet) {
								Bundle data = new Bundle();
								if (resultSet.size() > 0) {
									data.putParcelableArrayList(DATA_KEY_RESULT_SET, new ArrayList<>(resultSet));
								}
								try {
									callback.onSearchComplete(resultSet);
								} catch (RemoteException e) {
									handleException(e);
								}
							}
						});
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean navigateSearch(NavigateSearchParams params) {
			try {
				OsmandAidlApi api = getApi("navigateSearch");
				return params != null && api != null && api.navigateSearch(
						params.getStartName(), params.getStartLat(), params.getStartLon(),
						params.getSearchQuery(), params.getSearchLat(), params.getSearchLon(),
						params.getProfile(), params.isForce());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public long registerForUpdates(long updateTimeMS, IOsmAndAidlCallback callback) {
			try {
				if (updateTimeMS >= MIN_UPDATE_TIME_MS) {
					long id = addAidlCallback(callback, KEY_ON_UPDATE);
					startRemoteUpdates(updateTimeMS, id, callback);
					return getCallbackId();
				} else {
					return MIN_UPDATE_TIME_MS_ERROR;
				}
			} catch (Exception e) {
				handleException(e);
				return UNKNOWN_API_ERROR;
			}
		}

		@Override
		public boolean unregisterFromUpdates(long callbackId) {
			try {
				return removeAidlCallback(callbackId);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setNavDrawerLogo(String imageUri) {
			try {
				OsmandAidlApi api = getApi("setNavDrawerLogo");
				return api != null && api.setNavDrawerLogo(imageUri);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setEnabledIds(List<String> ids) {
			try {
				OsmandAidlApi api = getApi("setFeaturesEnabledIds");
				return api != null && api.setEnabledIds(ids);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setDisabledIds(List<String> ids) {
			try {
				OsmandAidlApi api = getApi("setFeaturesDisabledIds");
				return api != null && api.setDisabledIds(ids);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setEnabledPatterns(List<String> patterns) {
			try {
				OsmandAidlApi api = getApi("setFeaturesEnabledPatterns");
				return api != null && api.setEnabledPatterns(patterns);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setDisabledPatterns(List<String> patterns) {
			try {
				OsmandAidlApi api = getApi("setFeaturesDisabledPatterns");
				return api != null && api.setDisabledPatterns(patterns);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		void startRemoteUpdates(final long updateTimeMS, final long callbackId, final IOsmAndAidlCallback callback) {
			try {
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						try {
							if (callbacks.containsKey(callbackId)) {
								OsmandAidlApi api = getApi("isUpdateAllowed");
								if (api != null && api.isUpdateAllowed()) {
									callback.onUpdate();
								}
								startRemoteUpdates(updateTimeMS, callbackId, callback);
							}
						} catch (RemoteException e) {
							handleException(e);
						}
					}
				}, updateTimeMS);
			} catch (Exception e) {
				handleException(e);
			}
		}

		@Override
		public boolean regWidgetVisibility(SetWidgetsParams params) {
			try {
				OsmandAidlApi api = getApi("regWidgetVisibility");
				return api != null && api.regWidgetVisibility(params.getWidgetKey(), params.getAppModesKeys());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean regWidgetAvailability(SetWidgetsParams params) {
			try {
				OsmandAidlApi api = getApi("regWidgetVisibility");
				return api != null && api.regWidgetAvailability(params.getWidgetKey(), params.getAppModesKeys());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean customizeOsmandSettings(OsmandSettingsParams params) {
			try {
				OsmandAidlApi api = getApi("customizeOsmandSettings");
				return api != null && api.customizeOsmandSettings(params.getSharedPreferencesName(), params.getBundle());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean getSqliteDbFiles(List<ASqliteDbFile> files) {
			try {
				OsmandAidlApi api = getApi("getSqliteDbFiles");
				return api != null && api.getSqliteDbFiles(files);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean getActiveSqliteDbFiles(List<ASqliteDbFile> files) {
			try {
				OsmandAidlApi api = getApi("getActiveSqliteDbFiles");
				return api != null && api.getActiveSqliteDbFiles(files);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean showSqliteDbFile(String fileName) {
			try {
				OsmandAidlApi api = getApi("showSqliteDbFile");
				return api != null && api.showSqliteDbFile(fileName);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean hideSqliteDbFile(String fileName) {
			try {
				OsmandAidlApi api = getApi("hideSqliteDbFile");
				return api != null && api.hideSqliteDbFile(fileName);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setNavDrawerLogoWithParams(NavDrawerHeaderParams params) {
			try {
				OsmandAidlApi api = getApi("setNavDrawerLogoWithParams");
				return api != null && api.setNavDrawerLogoWithParams(
						params.getImageUri(), params.getPackageName(), params.getIntent());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setNavDrawerFooterWithParams(NavDrawerFooterParams params) {
			try {
				OsmandAidlApi api = getApi("setNavDrawerFooterParams");
				return api != null && api.setNavDrawerFooterWithParams(params);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean restoreOsmand() {
			try {
				OsmandAidlApi api = getApi("restoreOsmand");
				return api != null && api.restoreOsmand();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean changePluginState(PluginParams params) {
			try {
				OsmandAidlApi api = getApi("changePluginState");
				return api != null && api.changePluginState(params);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean registerForOsmandInitListener(final IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("registerForOsmandInitListener");
				return api != null && api.registerForOsmandInitialization(new OsmandAppInitCallback() {
					@Override
					public void onAppInitialized() {
						try {
							callback.onAppInitialized();
						} catch (Exception e) {
							handleException(e);
						}
					}
				});
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean getBitmapForGpx(CreateGpxBitmapParams params, final IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("getBitmapForGpx");
				return params != null && api != null && api.getBitmapForGpx(params.getGpxUri(), params.getDensity(), params.getWidthPixels(), params.getHeightPixels(), params.getColor(), new GpxBitmapCreatedCallback() {
					@Override
					public void onGpxBitmapCreatedComplete(AGpxBitmap aGpxBitmap) {
						try {
							callback.onGpxBitmapCreated(aGpxBitmap);
						} catch (RemoteException e) {
							handleException(e);
						}
					}
				});
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public int copyFile(CopyFileParams copyFileParams) {
			try {
				OsmandAidlApi api = getApi("copyFile");
				if (api == null) {
					return CANNOT_ACCESS_API_ERROR;
				}
				return api.copyFile(copyFileParams);
			} catch (Exception e) {
				handleException(e);
				return UNKNOWN_API_ERROR;
			}
		}

		@Override
		public long registerForNavigationUpdates(ANavigationUpdateParams params, final IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("registerForNavUpdates");
				if (api != null ) {
					if (!params.isSubscribeToUpdates() && params.getCallbackId() != -1) {
						api.unregisterFromUpdates(params.getCallbackId());
						removeAidlCallback(params.getCallbackId());
						return -1;
					} else {
						long id = addAidlCallback(callback, KEY_ON_NAV_DATA_UPDATE);
						api.registerForNavigationUpdates(id);
						return id;
					}
				} else {
					return -1;
				}
			} catch (Exception e) {
				handleException(e);
				return UNKNOWN_API_ERROR;
			}
		}

		@Override
		public long addContextMenuButtons(ContextMenuButtonsParams params, final IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("addContextMenuButtons");
				if (api != null && params != null) {
					long callbackId = params.getCallbackId();
					if (callbackId == -1 || !callbacks.containsKey(callbackId)) {
						callbackId = addAidlCallback(callback, KEY_ON_CONTEXT_MENU_BUTTONS_CLICK);
						params.setCallbackId(callbackId);
					}
					boolean buttonsAdded = api.addContextMenuButtons(params, callbackId);
					return buttonsAdded ? callbackId : -1;
				} else {
					return -1;
				}
			} catch (Exception e) {
				handleException(e);
				return UNKNOWN_API_ERROR;
			}
		}

		@Override
		public boolean removeContextMenuButtons(RemoveContextMenuButtonsParams params) {
			try {
				OsmandAidlApi api = getApi("removeContextMenuButtons");
				if (params != null && api != null) {
					long callbackId = params.getCallbackId();
					removeAidlCallback(callbackId);
					return api.removeContextMenuButtons(params.getParamsId(), callbackId);
				}
				return false;
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean updateContextMenuButtons(UpdateContextMenuButtonsParams params) {
			try {
				OsmandAidlApi api = getApi("updateContextMenuButtons");
				if (params != null && api != null) {
					ContextMenuButtonsParams buttonsParams = params.getContextMenuButtonsParams();
					return api.updateContextMenuButtons(buttonsParams, buttonsParams.getCallbackId());
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}
	};

	public static class AidlCallbackParams {
		private IOsmAndAidlCallback callback;
		private long key;

		AidlCallbackParams(IOsmAndAidlCallback callback, long key) {
			this.callback = callback;

			this.key = key;
		}

		public IOsmAndAidlCallback getCallback() {
			return callback;
		}

		public void setCallback(IOsmAndAidlCallback callback) {
			this.callback = callback;
		}

		public long getKey() {
			return key;
		}

		public void setKey(long key) {
			this.key = key;
		}
	}


}
