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

import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi.SearchCompleteCallback;
import net.osmand.aidl.calculateroute.CalculateRouteParams;
import net.osmand.aidl.customization.OsmandSettingsParams;
import net.osmand.aidl.customization.SetWidgetsParams;
import net.osmand.aidl.favorite.AddFavoriteParams;
import net.osmand.aidl.favorite.RemoveFavoriteParams;
import net.osmand.aidl.favorite.UpdateFavoriteParams;
import net.osmand.aidl.favorite.group.AddFavoriteGroupParams;
import net.osmand.aidl.favorite.group.RemoveFavoriteGroupParams;
import net.osmand.aidl.favorite.group.UpdateFavoriteGroupParams;
import net.osmand.aidl.gpx.AGpxFile;
import net.osmand.aidl.gpx.ASelectedGpxFile;
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
import net.osmand.aidl.navdrawer.SetNavDrawerItemsParams;
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
import net.osmand.aidl.search.SearchParams;
import net.osmand.aidl.search.SearchResult;
import net.osmand.aidl.tiles.ASqliteDbFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OsmandAidlService extends Service {

	private static final Log LOG = PlatformUtil.getLog(OsmandAidlService.class);

	private static final String DATA_KEY_RESULT_SET = "resultSet";

	private static final int MIN_UPDATE_TIME_MS = 1000;

	private static final int MIN_UPDATE_TIME_MS_ERROR = -1;

	private Map<Long, IOsmAndAidlCallback> callbacks;
	private Handler mHandler = null;
	HandlerThread mHandlerThread = new HandlerThread("OsmAndAidlServiceThread");

	private long updateCallbackId = 0;

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
		callbacks = new ConcurrentHashMap<>();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mHandlerThread.quit();
		callbacks.clear();
	}

	private final IOsmAndAidlInterface.Stub mBinder = new IOsmAndAidlInterface.Stub() {

		private void handleException(Exception e) {
			LOG.error("AIDL e.getMessage()", e);
		}

		@Override
		public boolean refreshMap() throws RemoteException {
			try {
				OsmandAidlApi api = getApi("refreshMap");
				return api != null && api.reloadMap();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}


		@Override
		public boolean addFavoriteGroup(AddFavoriteGroupParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("addFavoriteGroup");
				return params != null && api != null && api.addFavoriteGroup(params.getFavoriteGroup());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeFavoriteGroup(RemoveFavoriteGroupParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("removeFavoriteGroup");
				return params != null && api != null && api.removeFavoriteGroup(params.getFavoriteGroup());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateFavoriteGroup(UpdateFavoriteGroupParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("updateFavoriteGroup");
				return params != null && api != null && api.updateFavoriteGroup(params.getFavoriteGroupPrev(), params.getFavoriteGroupNew());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addFavorite(AddFavoriteParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("addFavorite");
				return params != null && api != null && api.addFavorite(params.getFavorite());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeFavorite(RemoveFavoriteParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("removeFavorite");
				return params != null && api != null && api.removeFavorite(params.getFavorite());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateFavorite(UpdateFavoriteParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("updateFavorite");
				return params != null && api != null && api.updateFavorite(params.getFavoritePrev(), params.getFavoriteNew());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapMarker(AddMapMarkerParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("addMapMarker");
				return params != null && api != null && api.addMapMarker(params.getMarker());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean removeMapMarker(RemoveMapMarkerParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("removeMapMarker");
				return params != null && api != null && api.removeMapMarker(params.getMarker());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapMarker(UpdateMapMarkerParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("updateMapMarker");
				return params != null && api != null && api.updateMapMarker(params.getMarkerPrev(), params.getMarkerNew());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapWidget(AddMapWidgetParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("addMapWidget");
				return params != null && api != null && api.addMapWidget(params.getWidget());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapWidget(RemoveMapWidgetParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("removeMapWidget");
				return params != null && api != null && api.removeMapWidget(params.getId());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean updateMapWidget(UpdateMapWidgetParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("updateMapWidget");
				return params != null && api != null && api.updateMapWidget(params.getWidget());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean showMapPoint(ShowMapPointParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("showMapPoint");
				return params != null && api != null && api.showMapPoint(params.getLayerId(), params.getPoint());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapPoint(AddMapPointParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("addMapPoint");
				return params != null && api != null && api.putMapPoint(params.getLayerId(), params.getPoint());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapPoint(RemoveMapPointParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("removeMapPoint");
				return params != null && api != null && api.removeMapPoint(params.getLayerId(), params.getPointId());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapPoint(UpdateMapPointParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("updateMapPoint");
				return params != null && api != null && api.putMapPoint(params.getLayerId(), params.getPoint());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapLayer(AddMapLayerParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("addMapLayer");
				return params != null && api != null && api.addMapLayer(params.getLayer());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapLayer(RemoveMapLayerParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("removeMapLayer");
				return params != null && api != null && api.removeMapLayer(params.getId());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapLayer(UpdateMapLayerParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("updateMapLayer");
				return params != null && api != null && api.updateMapLayer(params.getLayer());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean importGpx(ImportGpxParams params) throws RemoteException {
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
		}

		@Override
		public boolean showGpx(ShowGpxParams params) throws RemoteException {
			if (params != null && params.getFileName() != null) {
				OsmandAidlApi api = getApi("showGpx");
				return api != null && api.showGpx(params.getFileName());
			}
			return false;
		}

		@Override
		public boolean hideGpx(HideGpxParams params) throws RemoteException {
			if (params != null && params.getFileName() != null) {
				OsmandAidlApi api = getApi("hideGpx");
				return api != null && api.hideGpx(params.getFileName());
			}
			return false;
		}

		@Override
		public boolean getActiveGpx(List<ASelectedGpxFile> files) throws RemoteException {
			OsmandAidlApi api = getApi("getActiveGpx");
			return api != null && api.getActiveGpx(files);
		}

		@Override
		public boolean getImportedGpx(List<AGpxFile> files) throws RemoteException {
			OsmandAidlApi api = getApi("getImportedGpx");
			return api != null && api.getImportedGpx(files);
		}

		@Override
		public boolean removeGpx(RemoveGpxParams params) throws RemoteException {
			if (params != null && params.getFileName() != null) {
				OsmandAidlApi api = getApi("removeGpx");
				return api != null && api.removeGpx(params.getFileName());
			}
			return false;
		}

		@Override
		public boolean setMapLocation(SetMapLocationParams params) throws RemoteException {
			if (params != null) {
				OsmandAidlApi api = getApi("setMapLocation");
				return api != null && api.setMapLocation(params.getLatitude(), params.getLongitude(),
						params.getZoom(), params.isAnimated());
			}
			return false;
		}

		@Override
		public boolean calculateRoute(CalculateRouteParams params) throws RemoteException {
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
		}

		@Override
		public boolean startGpxRecording(StartGpxRecordingParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("startGpxRecording");
				return api != null && api.startGpxRecording(params);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopGpxRecording(StopGpxRecordingParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("stopGpxRecording");
				return api != null && api.stopGpxRecording(params);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean takePhotoNote(TakePhotoNoteParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("takePhotoNote");
				return params != null && api != null && api.takePhotoNote(params.getLatitude(), params.getLongitude());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean startVideoRecording(StartVideoRecordingParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("startVideoRecording");
				return params != null && api != null && api.startVideoRecording(params.getLatitude(), params.getLongitude());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean startAudioRecording(StartAudioRecordingParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("startAudioRecording");
				return params != null && api != null && api.startAudioRecording(params.getLatitude(), params.getLongitude());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopRecording(StopRecordingParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("stopRecording");
				return api != null && api.stopRecording();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean navigate(NavigateParams params) throws RemoteException {
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
		public boolean navigateGpx(NavigateGpxParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("navigateGpx");
				return params != null && api != null && api.navigateGpx(params.getData(), params.getUri(), params.isForce());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean pauseNavigation(PauseNavigationParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("pauseNavigation");
				return api != null && api.pauseNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean resumeNavigation(ResumeNavigationParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("resumeNavigation");
				return api != null && api.resumeNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopNavigation(StopNavigationParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("stopNavigation");
				return api != null && api.stopNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean muteNavigation(MuteNavigationParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("muteNavigation");
				return api != null && api.muteNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean unmuteNavigation(UnmuteNavigationParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("unmuteNavigation");
				return api != null && api.unmuteNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setNavDrawerItems(SetNavDrawerItemsParams params) throws RemoteException {
			try {
				OsmandAidlApi api = getApi("setNavDrawerItems");
				return params != null && api != null && api.setNavDrawerItems(params.getAppPackage(), params.getItems());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean search(SearchParams params, final IOsmAndAidlCallback callback) throws RemoteException {
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
		public boolean navigateSearch(NavigateSearchParams params) throws RemoteException {
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
		public long registerForUpdates(long updateTimeMS, IOsmAndAidlCallback callback) throws RemoteException {
			if (updateTimeMS >= MIN_UPDATE_TIME_MS) {
				updateCallbackId++;
				callbacks.put(updateCallbackId, callback);
				startRemoteUpdates(updateTimeMS, updateCallbackId, callback);
				return updateCallbackId;
			} else {
				return MIN_UPDATE_TIME_MS_ERROR;
			}
		}

		@Override
		public boolean unregisterFromUpdates(long callbackId) throws RemoteException {
			callbacks.remove(callbackId);
			return true;
		}

		@Override
		public boolean setNavDrawerLogo(String imageUri) throws RemoteException {
			OsmandAidlApi api = getApi("setNavDrawerLogo");
			return api != null && api.setNavDrawerLogo(imageUri);
		}

		@Override
		public boolean setEnabledIds(List<String> ids) throws RemoteException {
			OsmandAidlApi api = getApi("setFeaturesEnabledIds");
			return api != null && api.setEnabledIds(ids);
		}

		@Override
		public boolean setDisabledIds(List<String> ids) throws RemoteException {
			OsmandAidlApi api = getApi("setFeaturesDisabledIds");
			return api != null && api.setDisabledIds(ids);
		}

		@Override
		public boolean setEnabledPatterns(List<String> patterns) throws RemoteException {
			OsmandAidlApi api = getApi("setFeaturesEnabledPatterns");
			return api != null && api.setEnabledPatterns(patterns);
		}

		@Override
		public boolean setDisabledPatterns(List<String> patterns) throws RemoteException {
			OsmandAidlApi api = getApi("setFeaturesDisabledPatterns");
			return api != null && api.setDisabledPatterns(patterns);
		}

		void startRemoteUpdates(final long updateTimeMS, final long callbackId, final IOsmAndAidlCallback callback) {
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
		}

		@Override
		public boolean regWidgetVisibility(SetWidgetsParams params) throws RemoteException {
			OsmandAidlApi api = getApi("regWidgetVisibility");
			return api != null && api.regWidgetVisibility(params.getWidgetKey(), params.getAppModesKeys());
		}

		@Override
		public boolean regWidgetAvailability(SetWidgetsParams params) throws RemoteException {
			OsmandAidlApi api = getApi("regWidgetVisibility");
			return api != null && api.regWidgetAvailability(params.getWidgetKey(), params.getAppModesKeys());
		}

		@Override
		public boolean customizeOsmandSettings(OsmandSettingsParams params) throws RemoteException {
			OsmandAidlApi api = getApi("customizeOsmandSettings");
			return api != null && api.customizeOsmandSettings(params.getSharedPreferencesName(), params.getBundle());
		}

		@Override
		public boolean getSqliteDbFiles(List<ASqliteDbFile> files) throws RemoteException {
			OsmandAidlApi api = getApi("getSqliteDbFiles");
			return api != null && api.getSqliteDbFiles(files);
		}

		@Override
		public boolean getActiveSqliteDbFiles(List<ASqliteDbFile> files) throws RemoteException {
			OsmandAidlApi api = getApi("getActiveSqliteDbFiles");
			return api != null && api.getActiveSqliteDbFiles(files);
		}

		@Override
		public boolean showSqliteDbFile(String fileName) throws RemoteException {
			OsmandAidlApi api = getApi("showSqliteDbFile");
			return api != null && api.showSqliteDbFile(fileName);
		}

		@Override
		public boolean hideSqliteDbFile(String fileName) throws RemoteException {
			OsmandAidlApi api = getApi("hideSqliteDbFile");
			return api != null && api.hideSqliteDbFile(fileName);
		}

		@Override
		public boolean setNavDrawerLogoWithIntent(String imageUri, String packageName, String intent) throws RemoteException {
			OsmandAidlApi api = getApi("setNavDrawerLogoWithIntent");
			return api != null && api.setNavDrawerLogoWithIntent(imageUri, packageName, intent);
		}

		@Override
		public boolean setNavDrawerFooterAction(String packageName, String intent, String appName) throws RemoteException {
			OsmandAidlApi api = getApi ("setNavDrawerFooterAction");
			return api != null && api.setNavDrawerFooterAction(packageName, intent, appName);
		}

		@Override
		public boolean setPointMenuEnabledIds(List<String> ids) throws RemoteException {
			return true;
		}

		@Override
		public boolean setPointMenuDisabledIds(List<String> ids) throws RemoteException {
			return true;
		}

		@Override
		public boolean setPointMenuEnabledPatterns(List<String> patterns) throws RemoteException {
			return true;
		}

		@Override
		public boolean setPointMenuDisabledPatterns(List<String> patterns) throws RemoteException {
			return true;
		}
	};
}
