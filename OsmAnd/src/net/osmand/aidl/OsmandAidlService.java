package net.osmand.aidl;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;

import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi.SearchCompleteCallback;
import net.osmand.aidl.calculateroute.CalculateRouteParams;
import net.osmand.aidl.favorite.AddFavoriteParams;
import net.osmand.aidl.favorite.RemoveFavoriteParams;
import net.osmand.aidl.favorite.UpdateFavoriteParams;
import net.osmand.aidl.favorite.group.AddFavoriteGroupParams;
import net.osmand.aidl.favorite.group.RemoveFavoriteGroupParams;
import net.osmand.aidl.favorite.group.UpdateFavoriteGroupParams;
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
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OsmandAidlService extends Service {
	
	private static final Log LOG = PlatformUtil.getLog(OsmandAidlService.class);

	private static final int UPDATE_TIME_MIL = 5000;
	
	private static final int MSG_RUN_SEARCH = 53;
	private static final String DATA_KEY_RESULT_SET = "resultSet";

	private ArrayList<IOsmAndAidlCallback> mRemoteCallbacks;
	private Set<IOsmAndAidlCallback> callbacks;
	private ServiceHandler mHandler = null;
	HandlerThread mHandlerThread = new HandlerThread("OsmAndAidlServiceThread");

	private boolean updatesStarted = false;

	OsmandApplication getApp() {
		return (OsmandApplication) getApplication();
	}

	OsmandAidlApi getApi(String reason) {
		LOG.info("Request AIDL API for " + reason);
		return getApp().getAidlApi();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Handler Thread handling all call back methods
		mHandlerThread.start();
		mHandler = new ServiceHandler(mHandlerThread.getLooper());

		// Return the interface
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mRemoteCallbacks = new ArrayList<>();
		callbacks = new HashSet<>();
	}

	private final IOsmAndAidlInterface.Stub mBinder = new IOsmAndAidlInterface.Stub() {

		private void handleException(Exception e) {
			LOG.error("AIDL e.getMessage()", e);
		}

		@Override
		public boolean refreshMap() throws RemoteException {
			try {
				return getApi("refreshMap").reloadMap();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}


		@Override
		public boolean addFavoriteGroup(AddFavoriteGroupParams params) throws RemoteException {
			try {
				return params != null && getApi("addFavoriteGroup").addFavoriteGroup(params.getFavoriteGroup());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeFavoriteGroup(RemoveFavoriteGroupParams params) throws RemoteException {
			try {
				return params != null && getApi("removeFavoriteGroup").removeFavoriteGroup(params.getFavoriteGroup());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateFavoriteGroup(UpdateFavoriteGroupParams params) throws RemoteException {
			try {
				return params != null && getApi("updateFavoriteGroup").updateFavoriteGroup(params.getFavoriteGroupPrev(), params.getFavoriteGroupNew());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addFavorite(AddFavoriteParams params) throws RemoteException {
			try {
				return params != null && getApi("addFavorite").addFavorite(params.getFavorite());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeFavorite(RemoveFavoriteParams params) throws RemoteException {
			try {
				return params != null && getApi("removeFavorite").removeFavorite(params.getFavorite());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateFavorite(UpdateFavoriteParams params) throws RemoteException {
			try {
				return params != null && getApi("updateFavorite").updateFavorite(params.getFavoritePrev(), params.getFavoriteNew());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapMarker(AddMapMarkerParams params) throws RemoteException {
			try {
				return params != null && getApi("addMapMarker").addMapMarker(params.getMarker());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean removeMapMarker(RemoveMapMarkerParams params) throws RemoteException {
			try {
				return params != null && getApi("removeMapMarker").removeMapMarker(params.getMarker());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapMarker(UpdateMapMarkerParams params) throws RemoteException {
			try {
				return params != null && getApi("updateMapMarker").updateMapMarker(params.getMarkerPrev(), params.getMarkerNew());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapWidget(AddMapWidgetParams params) throws RemoteException {
			try {
				return params != null && getApi("addMapWidget").addMapWidget(params.getWidget());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapWidget(RemoveMapWidgetParams params) throws RemoteException {
			try {
				return params != null && getApi("removeMapWidget").removeMapWidget(params.getId());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean updateMapWidget(UpdateMapWidgetParams params) throws RemoteException {
			try {
				return params != null && getApi("updateMapWidget").updateMapWidget(params.getWidget());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean showMapPoint(ShowMapPointParams params) throws RemoteException {
			try {
				return params != null && getApi("showMapPoint").showMapPoint(params.getLayerId(), params.getPoint());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapPoint(AddMapPointParams params) throws RemoteException {
			try {
				return params != null && getApi("addMapPoint").putMapPoint(params.getLayerId(), params.getPoint());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapPoint(RemoveMapPointParams params) throws RemoteException {
			try {
				return params != null && getApi("removeMapPoint").removeMapPoint(params.getLayerId(), params.getPointId());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapPoint(UpdateMapPointParams params) throws RemoteException {
			try {
				return params != null && getApi("updateMapPoint").putMapPoint(params.getLayerId(), params.getPoint());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapLayer(AddMapLayerParams params) throws RemoteException {
			try {
				return params != null && getApi("addMapLayer").addMapLayer(params.getLayer());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapLayer(RemoveMapLayerParams params) throws RemoteException {
			try {
				return params != null && getApi("removeMapLayer").removeMapLayer(params.getId());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapLayer(UpdateMapLayerParams params) throws RemoteException {
			try {
				return params != null && getApi("updateMapLayer").updateMapLayer(params.getLayer());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean importGpx(ImportGpxParams params) throws RemoteException {
			if (params != null && !Algorithms.isEmpty(params.getDestinationPath())) {
				if (params.getGpxFile() != null) {
					return getApi("importGpx").importGpxFromFile(params.getGpxFile(), params.getDestinationPath(),
							params.getColor(), params.isShow());
				} else if (params.getGpxUri() != null) {
					return getApi("importGpx").importGpxFromUri(params.getGpxUri(), params.getDestinationPath(),
							params.getColor(), params.isShow());
				} else if (params.getSourceRawData() != null) {
					return getApi("importGpx").importGpxFromData(params.getSourceRawData(), params.getDestinationPath(),
							params.getColor(), params.isShow());
				}
			}
			return false;
		}

		@Override
		public boolean showGpx(ShowGpxParams params) throws RemoteException {
			if (params != null && params.getFileName() != null) {
				return getApi("showGpx").showGpx(params.getFileName());
			}
			return false;
		}

		@Override
		public boolean hideGpx(HideGpxParams params) throws RemoteException {
			if (params != null && params.getFileName() != null) {
				return getApi("hideGpx").hideGpx(params.getFileName());
			}
			return false;
		}

		@Override
		public boolean getActiveGpx(List<ASelectedGpxFile> files) throws RemoteException {
			return getApi("getActiveGpx").getActiveGpx(files);
		}

		@Override
		public boolean removeGpx(RemoveGpxParams params) throws RemoteException {
			if (params != null && params.getFileName() != null) {
				return getApi("removeGpx").removeGpx(params.getFileName());
			}
			return false;
		}

		@Override
		public boolean setMapLocation(SetMapLocationParams params) throws RemoteException {
			if (params != null) {
				return getApi("setMapLocation").setMapLocation(params.getLatitude(), params.getLongitude(),
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
				return getApi("startGpxRecording").startGpxRecording(params);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopGpxRecording(StopGpxRecordingParams params) throws RemoteException {
			try {
				return getApi("stopGpxRecording").stopGpxRecording(params);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean takePhotoNote(TakePhotoNoteParams params) throws RemoteException {
			try {
				return params != null && getApi("takePhotoNote").takePhotoNote(params.getLatitude(), params.getLongitude());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean startVideoRecording(StartVideoRecordingParams params) throws RemoteException {
			try {
				return params != null && getApi("startVideoRecording").startVideoRecording(params.getLatitude(), params.getLongitude());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean startAudioRecording(StartAudioRecordingParams params) throws RemoteException {
			try {
				return params != null && getApi("startAudioRecording").startAudioRecording(params.getLatitude(), params.getLongitude());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopRecording(StopRecordingParams params) throws RemoteException {
			try {
				return getApi("stopRecording").stopRecording();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean navigate(NavigateParams params) throws RemoteException {
			try {
				return params != null && getApi("navigate").navigate(params.getStartName(), params.getStartLat(), params.getStartLon(), params.getDestName(), params.getDestLat(), params.getDestLon(), params.getProfile(), params.isForce());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean navigateGpx(NavigateGpxParams params) throws RemoteException {
			try {
				return params != null && getApi("navigateGpx").navigateGpx(params.getData(), params.getUri(), params.isForce());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean pauseNavigation(PauseNavigationParams params) throws RemoteException {
			try {
				return getApi("pauseNavigation").pauseNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean resumeNavigation(ResumeNavigationParams params) throws RemoteException {
			try {
				return getApi("resumeNavigation").resumeNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopNavigation(StopNavigationParams params) throws RemoteException {
			try {
				return getApi("stopNavigation").stopNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean muteNavigation(MuteNavigationParams params) throws RemoteException {
			try {
				return getApi("muteNavigation").muteNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean unmuteNavigation(UnmuteNavigationParams params) throws RemoteException {
			try {
				return getApi("unmuteNavigation").unmuteNavigation();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setNavDrawerItems(SetNavDrawerItemsParams params) throws RemoteException {
			try {
				return params != null && getApi("setNavDrawerItems").setNavDrawerItems(params.getAppPackage(), params.getItems());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean search(SearchParams params, final IOsmAndAidlCallback callback) throws RemoteException {
			try {
				return params != null && getApi("search").search(params.getSearchQuery(), params.getSearchType(),
						params.getLatutude(), params.getLongitude(), params.getRadiusLevel(), params.getTotalLimit(), new SearchCompleteCallback() {
					@Override
					public void onSearchComplete(List<SearchResult> resultSet) {
						Bundle data = new Bundle();
						if (resultSet.size() > 0) {
							data.putParcelableArrayList(DATA_KEY_RESULT_SET, new ArrayList<>(resultSet));
						}
						sendMsgToHandler(callback, MSG_RUN_SEARCH, data);
					}
				});
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean registerCallback(IOsmAndAidlCallback callback) throws RemoteException {
			if (callback != null) {
				callbacks.add(callback);
				if (!updatesStarted) {
					startRemoteUpdates();
					updatesStarted = true;
				}
				return true;
			}
			return false;
		}
	};

	void startRemoteUpdates() {
		mHandler.postDelayed((new Runnable() {
			@Override
			public void run() {
				for (IOsmAndAidlCallback callback : callbacks) {
					try {
						callback.update();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				startRemoteUpdates();
			}
		}), UPDATE_TIME_MIL);
	}
	
	/**
	 * Create handler message to be sent
	 */
	void sendMsgToHandler(IOsmAndAidlCallback callback, int flag, Bundle data) {

		mRemoteCallbacks.add(callback);

		Message message = mHandler.obtainMessage();
		message.arg1 = mRemoteCallbacks.size() - 1;
		message.setData(data);

		message.what = flag;
		mHandler.sendMessage(message);
	}

	/**
	 * Handler class sending result in callback to respective
	 * application
	 */
	private class ServiceHandler extends Handler {
		int callbackIndex = 0;

		ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			callbackIndex = msg.arg1;

			switch (msg.what) {

				case MSG_RUN_SEARCH:

					Bundle data = msg.getData();
					List<SearchResult> resultSet;
					if (data.containsKey(DATA_KEY_RESULT_SET)) {
						resultSet = data.getParcelableArrayList(DATA_KEY_RESULT_SET);
					} else {
						resultSet = Collections.emptyList();
					}

					try {
						mRemoteCallbacks.get(callbackIndex).onSearchComplete(resultSet);
					} catch (RemoteException e) {
						LOG.error("AIDL e.getMessage()", e);
					}
					break;
			}
		}
	}
}
