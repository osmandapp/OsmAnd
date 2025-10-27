package net.osmand.aidl;

import static net.osmand.aidl.OsmandAidlApi.KEY_ON_CONTEXT_MENU_BUTTONS_CLICK;
import static net.osmand.aidl.OsmandAidlApi.KEY_ON_NAV_DATA_UPDATE;
import static net.osmand.aidl.OsmandAidlApi.KEY_ON_UPDATE;
import static net.osmand.aidl.OsmandAidlApi.KEY_ON_VOICE_MESSAGE;
import static net.osmand.aidlapi.OsmandAidlConstants.CANNOT_ACCESS_API_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.MIN_UPDATE_TIME_MS;
import static net.osmand.aidlapi.OsmandAidlConstants.MIN_UPDATE_TIME_MS_ERROR;
import static net.osmand.aidlapi.OsmandAidlConstants.UNKNOWN_API_ERROR;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi.GpxBitmapCreatedCallback;
import net.osmand.aidl.OsmandAidlApi.OsmandAppInitCallback;
import net.osmand.aidl.OsmandAidlApi.SearchCompleteCallback;
import net.osmand.aidl.calculateroute.CalculateRouteParams;
import net.osmand.aidl.contextmenu.ContextMenuButtonsParams;
import net.osmand.aidl.contextmenu.RemoveContextMenuButtonsParams;
import net.osmand.aidl.contextmenu.UpdateContextMenuButtonsParams;
import net.osmand.aidl.copyfile.CopyFileParams;
import net.osmand.aidl.customization.CustomizationInfoParams;
import net.osmand.aidl.customization.OsmandSettingsInfoParams;
import net.osmand.aidl.customization.OsmandSettingsParams;
import net.osmand.aidl.customization.ProfileSettingsParams;
import net.osmand.aidl.customization.SetWidgetsParams;
import net.osmand.aidl.favorite.AFavorite;
import net.osmand.aidl.favorite.AddFavoriteParams;
import net.osmand.aidl.favorite.RemoveFavoriteParams;
import net.osmand.aidl.favorite.UpdateFavoriteParams;
import net.osmand.aidl.favorite.group.AFavoriteGroup;
import net.osmand.aidl.favorite.group.AddFavoriteGroupParams;
import net.osmand.aidl.favorite.group.RemoveFavoriteGroupParams;
import net.osmand.aidl.favorite.group.UpdateFavoriteGroupParams;
import net.osmand.aidl.gpx.AGpxBitmap;
import net.osmand.aidl.gpx.AGpxFile;
import net.osmand.aidl.gpx.ASelectedGpxFile;
import net.osmand.aidl.gpx.CreateGpxBitmapParams;
import net.osmand.aidl.gpx.GpxColorParams;
import net.osmand.aidl.gpx.HideGpxParams;
import net.osmand.aidl.gpx.ImportGpxParams;
import net.osmand.aidl.gpx.RemoveGpxParams;
import net.osmand.aidl.gpx.ShowGpxParams;
import net.osmand.aidl.gpx.StartGpxRecordingParams;
import net.osmand.aidl.gpx.StopGpxRecordingParams;
import net.osmand.aidl.lock.SetLockStateParams;
import net.osmand.aidl.map.ALatLon;
import net.osmand.aidl.map.SetMapLocationParams;
import net.osmand.aidl.maplayer.AddMapLayerParams;
import net.osmand.aidl.maplayer.RemoveMapLayerParams;
import net.osmand.aidl.maplayer.UpdateMapLayerParams;
import net.osmand.aidl.maplayer.point.AddMapPointParams;
import net.osmand.aidl.maplayer.point.RemoveMapPointParams;
import net.osmand.aidl.maplayer.point.ShowMapPointParams;
import net.osmand.aidl.maplayer.point.UpdateMapPointParams;
import net.osmand.aidl.mapmarker.AMapMarker;
import net.osmand.aidl.mapmarker.AddMapMarkerParams;
import net.osmand.aidl.mapmarker.RemoveMapMarkerParams;
import net.osmand.aidl.mapmarker.RemoveMapMarkersParams;
import net.osmand.aidl.mapmarker.UpdateMapMarkerParams;
import net.osmand.aidl.mapwidget.AddMapWidgetParams;
import net.osmand.aidl.mapwidget.RemoveMapWidgetParams;
import net.osmand.aidl.mapwidget.UpdateMapWidgetParams;
import net.osmand.aidl.navdrawer.NavDrawerFooterParams;
import net.osmand.aidl.navdrawer.NavDrawerHeaderParams;
import net.osmand.aidl.navdrawer.NavDrawerItem;
import net.osmand.aidl.navdrawer.SetNavDrawerItemsParams;
import net.osmand.aidl.navigation.ANavigationUpdateParams;
import net.osmand.aidl.navigation.ANavigationVoiceRouterMessageParams;
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
import net.osmand.aidl.quickaction.QuickActionInfoParams;
import net.osmand.aidl.quickaction.QuickActionParams;
import net.osmand.aidl.search.SearchParams;
import net.osmand.aidl.search.SearchResult;
import net.osmand.aidl.tiles.ASqliteDbFile;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OsmandAidlService extends Service implements AidlCallbackListener {

	private static final Log LOG = PlatformUtil.getLog(OsmandAidlService.class);

	private final Map<Long, AidlCallbackParams> callbacks = new ConcurrentHashMap<>();
	private Handler mHandler;
	HandlerThread mHandlerThread = new HandlerThread("OsmAndAidlServiceThread");

	private final AtomicLong aidlCallbackId = new AtomicLong(0);

	private OsmandApplication getApp() {
		return (OsmandApplication) getApplication();
	}

	@Nullable
	private OsmandAidlApi getApi(String reason) {
		LOG.info("Request AIDL API for " + reason);
		OsmandAidlApi api = getApp().getAidlApi();
		String packName = getCallingAppPackName();
		if (packName != null && !packName.equals(getApp().getPackageName()) && !api.isAppEnabled(packName)) {
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
		if (api != null) {
			api.aidlCallbackListener = this;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		callbacks.clear();
		OsmandAidlApi api = getApi("clear_listener");
		if (api != null) {
			api.aidlCallbackListener = null;
		}
		mHandlerThread.quit();
	}

	private String getCallingAppPackName() {
		return getApp().getPackageManager().getNameForUid(Binder.getCallingUid());
	}

	private long getCallbackId() {
		return aidlCallbackId.get();
	}

	private long getAndIncrementCallbackId() {
		return aidlCallbackId.getAndIncrement();
	}

	@Override
	public long addAidlCallback(IOsmAndAidlCallback callback, int key) {
		long id = getAndIncrementCallbackId();
		callbacks.put(id, new AidlCallbackParams(callback, key));
		return id;
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
				if (params != null && api != null) {
					AFavoriteGroup favoriteGroup = params.getFavoriteGroup();
					if (favoriteGroup != null) {
						return api.addFavoriteGroup(favoriteGroup.getName(), favoriteGroup.getColor(), favoriteGroup.isVisible());
					}
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeFavoriteGroup(RemoveFavoriteGroupParams params) {
			try {
				OsmandAidlApi api = getApi("removeFavoriteGroup");
				if (params != null && api != null) {
					AFavoriteGroup favoriteGroup = params.getFavoriteGroup();
					if (favoriteGroup != null) {
						return api.removeFavoriteGroup(favoriteGroup.getName());
					}
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateFavoriteGroup(UpdateFavoriteGroupParams params) {
			try {
				OsmandAidlApi api = getApi("updateFavoriteGroup");
				if (params != null && api != null) {
					AFavoriteGroup prevGroup = params.getFavoriteGroupPrev();
					AFavoriteGroup newGroup = params.getFavoriteGroupNew();
					if (prevGroup != null && newGroup != null) {
						return api.updateFavoriteGroup(prevGroup.getName(), newGroup.getName(), newGroup.getColor(), newGroup.isVisible());
					}
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addFavorite(AddFavoriteParams params) {
			try {
				OsmandAidlApi api = getApi("addFavorite");
				if (params != null && api != null) {
					AFavorite fav = params.getFavorite();
					if (fav != null) {
						return api.addFavorite(fav.getLat(), fav.getLon(), fav.getName(), fav.getCategory(), fav.getDescription(), fav.getColor(), fav.isVisible());
					}
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeFavorite(RemoveFavoriteParams params) {
			try {
				OsmandAidlApi api = getApi("removeFavorite");
				if (params != null && api != null) {
					AFavorite fav = params.getFavorite();
					if (fav != null) {
						return api.removeFavorite(fav.getName(), fav.getCategory(), fav.getLat(), fav.getLon());
					}
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateFavorite(UpdateFavoriteParams params) {
			try {
				OsmandAidlApi api = getApi("updateFavorite");
				if (params != null && api != null) {
					AFavorite prevFav = params.getFavoritePrev();
					AFavorite newFav = params.getFavoriteNew();
					if (prevFav != null && newFav != null) {
						return api.updateFavorite(prevFav.getName(), prevFav.getCategory(), prevFav.getLat(), prevFav.getLon(),
								newFav.getName(), newFav.getCategory(), newFav.getDescription(), newFav.getAddress(), newFav.getLat(), newFav.getLon());
					}
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapMarker(AddMapMarkerParams params) {
			try {
				OsmandAidlApi api = getApi("addMapMarker");
				if (params != null && api != null) {
					AMapMarker mapMarker = params.getMarker();
					if (mapMarker != null) {
						return api.addMapMarker(mapMarker.getName(), mapMarker.getLatLon().getLatitude(), mapMarker.getLatLon().getLongitude());
					}
				}
				return false;
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean removeMapMarker(RemoveMapMarkerParams params) {
			try {
				OsmandAidlApi api = getApi("removeMapMarker");
				if (params != null && api != null) {
					AMapMarker mapMarker = params.getMarker();
					if (mapMarker != null) {
						ALatLon aLatLon = mapMarker.getLatLon();
						return api.removeMapMarker(mapMarker.getName(), aLatLon.getLatitude(), aLatLon.getLongitude(), params.getIgnoreCoordinates());
					}
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapMarker(UpdateMapMarkerParams params) {
			try {
				OsmandAidlApi api = getApi("updateMapMarker");
				if (params != null && api != null) {
					AMapMarker markerPrev = params.getMarkerPrev();
					AMapMarker markerNew = params.getMarkerNew();
					if (markerPrev != null && markerNew != null) {
						ALatLon aLatLonPrev = markerPrev.getLatLon();
						ALatLon aLatLonNew = markerNew.getLatLon();
						LatLon prevLatLon = new LatLon(aLatLonPrev.getLatitude(), aLatLonPrev.getLongitude());
						LatLon newLatLon = new LatLon(aLatLonNew.getLatitude(), aLatLonNew.getLongitude());

						return api.updateMapMarker(markerPrev.getName(), prevLatLon, markerNew.getName(), newLatLon, params.getIgnoreCoordinates());
					}
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapWidget(AddMapWidgetParams params) {
			try {
				OsmandAidlApi api = getApi("addMapWidget");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.addMapWidget(packName, new AidlMapWidgetWrapper(params.getWidget()));
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapWidget(RemoveMapWidgetParams params) {
			try {
				OsmandAidlApi api = getApi("removeMapWidget");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.removeMapWidget(packName, params.getId());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean updateMapWidget(UpdateMapWidgetParams params) {
			try {
				OsmandAidlApi api = getApi("updateMapWidget");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.updateMapWidget(packName, new AidlMapWidgetWrapper(params.getWidget()));
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean showMapPoint(ShowMapPointParams params) {
			try {
				OsmandAidlApi api = getApi("showMapPoint");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.showMapPoint(packName, params.getLayerId(), new AidlMapPointWrapper(params.getPoint()));
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapPoint(AddMapPointParams params) {
			try {
				OsmandAidlApi api = getApi("addMapPoint");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.putMapPoint(packName, params.getLayerId(), new AidlMapPointWrapper(params.getPoint()));
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapPoint(RemoveMapPointParams params) {
			try {
				OsmandAidlApi api = getApi("removeMapPoint");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.removeMapPoint(packName, params.getLayerId(), params.getPointId());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapPoint(UpdateMapPointParams params) {
			try {
				OsmandAidlApi api = getApi("updateMapPoint");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.updateMapPoint(packName, params.getLayerId(), new AidlMapPointWrapper(params.getPoint()), params.isUpdateOpenedMenuAndMap());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean addMapLayer(AddMapLayerParams params) {
			try {
				OsmandAidlApi api = getApi("addMapLayer");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.addMapLayer(packName, new AidlMapLayerWrapper(params.getLayer()));
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean removeMapLayer(RemoveMapLayerParams params) {
			try {
				OsmandAidlApi api = getApi("removeMapLayer");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.removeMapLayer(packName, params.getId());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean updateMapLayer(UpdateMapLayerParams params) {
			try {
				OsmandAidlApi api = getApi("updateMapLayer");
				String packName = getCallingAppPackName();
				return params != null && api != null && api.updateMapLayer(packName, new AidlMapLayerWrapper(params.getLayer()));
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
					return api != null && api.hideGpx(null, params.getFileName());
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
				if (api != null && files != null) {
					return api.getActiveGpx(files);
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean getImportedGpx(List<AGpxFile> files) {
			try {
				OsmandAidlApi api = getApi("getImportedGpx");
				if (api != null && files != null) {
					return api.getImportedGpx(files);
				}
				return false;
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
					return api != null && api.removeGpx(params.getFileName(), null);
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
							params.getZoom(), params.getRotation(), params.isAnimated());
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
				return params != null && params.getEndPoint() != null;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean startGpxRecording(StartGpxRecordingParams params) {
			try {
				OsmandAidlApi api = getApi("startGpxRecording");
				return api != null && api.startGpxRecording();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean stopGpxRecording(StopGpxRecordingParams params) {
			try {
				OsmandAidlApi api = getApi("stopGpxRecording");
				return api != null && api.stopGpxRecording();
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
						params.getProfile(), params.isForce(), params.isNeedLocationPermission());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean navigateGpx(NavigateGpxParams params) {
			try {
				OsmandAidlApi api = getApi("navigateGpx");
				return params != null && api != null && api.navigateGpx(params.getData(), params.getUri(),
						params.isForce(), params.isNeedLocationPermission());
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
				if (api != null && params != null) {
					return api.setNavDrawerItems(params.getAppPackage(), convertNavDrawerItems(params.getItems()));
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean search(SearchParams params, IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("search");
				return params != null && api != null && api.search(params.getSearchQuery(), params.getSearchType(),
						params.getLatitude(), params.getLongitude(), params.getRadiusLevel(), params.getTotalLimit(), new SearchCompleteCallback() {
							@Override
							public void onSearchComplete(List<AidlSearchResultWrapper> resultSet) {
								try {
									List<SearchResult> searchResults = new ArrayList<>();
									for (AidlSearchResultWrapper item : resultSet) {
										SearchResult result = new SearchResult(item.getLatitude(), item.getLongitude(), item.getLocalName(),
												item.getLocalTypeName(), item.getAlternateName(), item.getOtherNames());
										searchResults.add(result);
									}
									callback.onSearchComplete(searchResults);
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
						params.getProfile(), params.isForce(), params.isNeedLocationPermission());
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
					return id;
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

		void startRemoteUpdates(long updateTimeMS, long callbackId, IOsmAndAidlCallback callback) {
			try {
				mHandler.postDelayed(() -> {
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
				return api != null && api.setNavDrawerFooterWithParams(
						params.getAppName(), params.getPackageName(), params.getIntent());
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
				return api != null && api.changePluginState(params.getPluginId(), params.getNewState());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean registerForOsmandInitListener(IOsmAndAidlCallback callback) {
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
		public boolean getBitmapForGpx(CreateGpxBitmapParams params, IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("getBitmapForGpx");
				return params != null && api != null && api.getBitmapForGpx(params.getGpxUri(), params.getDensity(), params.getWidthPixels(), params.getHeightPixels(), params.getColor(), new GpxBitmapCreatedCallback() {
					@Override
					public void onGpxBitmapCreatedComplete(Bitmap gpxBitmap) {
						try {
							callback.onGpxBitmapCreated(new AGpxBitmap(gpxBitmap));
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
		public int copyFile(CopyFileParams params) {
			try {
				OsmandAidlApi api = getApi("copyFile");
				if (api == null) {
					return CANNOT_ACCESS_API_ERROR;
				}
				return api.copyFile(params.getFileName(), params.getFilePartData(), params.getStartTime(), params.isDone());
			} catch (Exception e) {
				handleException(e);
				return UNKNOWN_API_ERROR;
			}
		}

		@Override
		public long registerForNavigationUpdates(ANavigationUpdateParams params, IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("registerForNavUpdates");
				if (api != null) {
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
		public long addContextMenuButtons(ContextMenuButtonsParams params, IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("addContextMenuButtons");
				if (api != null && params != null) {
					long callbackId = params.getCallbackId();
					if (callbackId == -1 || !callbacks.containsKey(callbackId)) {
						callbackId = addAidlCallback(callback, KEY_ON_CONTEXT_MENU_BUTTONS_CLICK);
						params.setCallbackId(callbackId);
					}
					boolean buttonsAdded = api.addContextMenuButtons(new AidlContextMenuButtonsWrapper(params), callbackId);
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
					return api.updateContextMenuButtons(new AidlContextMenuButtonsWrapper(buttonsParams), buttonsParams.getCallbackId());
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean areOsmandSettingsCustomized(OsmandSettingsInfoParams params) {
			try {
				OsmandAidlApi api = getApi("areOsmandSettingsCustomized");
				return api != null && api.areOsmandSettingsCustomized(params.getSharedPreferencesName());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setCustomization(CustomizationInfoParams params) {
			try {
				OsmandAidlApi api = getApi("setCustomization");
				if (api != null && params != null) {
					OsmandAidlService.this.setCustomization(api, params);
					return true;
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public long registerForVoiceRouterMessages(ANavigationVoiceRouterMessageParams params, IOsmAndAidlCallback callback) {
			try {
				OsmandAidlApi api = getApi("registerForVoiceRouterMessages");
				if (api != null) {
					if (!params.isSubscribeToUpdates() && params.getCallbackId() != -1) {
						api.unregisterFromVoiceRouterMessages(params.getCallbackId());
						removeAidlCallback(params.getCallbackId());
						return -1;
					} else {
						long id = addAidlCallback(callback, KEY_ON_VOICE_MESSAGE);
						api.registerForVoiceRouterMessages(id);
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
		public boolean removeAllActiveMapMarkers(RemoveMapMarkersParams params) {
			try {
				OsmandAidlApi api = getApi("removeAllActiveMapMarkers");
				return api != null && api.removeAllActiveMapMarkers();
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean getGpxColor(GpxColorParams params) {
			try {
				OsmandAidlApi api = getApi("getGpxColor");
				if (api != null && params != null) {
					String colorName = api.getGpxColor(params.getFileName());
					params.setGpxColor(colorName);
					return true;
				}
				return false;
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean importProfile(ProfileSettingsParams params) {
			try {
				OsmandAidlApi api = getApi("importProfile");
				return api != null && api.importProfile(params.getProfileSettingsUri(), params.getLatestChanges(),
						params.getVersion());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean executeQuickAction(QuickActionParams params) {
			try {
				OsmandAidlApi api = getApi("executeQuickAction");
				return api != null && api.executeQuickAction(params.getActionNumber());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean getQuickActionsInfo(List<QuickActionInfoParams> quickActions) {
			try {
				OsmandAidlApi api = getApi("getQuickActionsInfo");
				return api != null && api.getQuickActionsInfo(quickActions);
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}

		@Override
		public boolean setLockState(SetLockStateParams params) {
			try {
				OsmandAidlApi api = getApi("setLockState");
				return api != null && api.setLockState(params.getLockState());
			} catch (Exception e) {
				handleException(e);
				return false;
			}
		}
	};

	private void setCustomization(OsmandAidlApi api, CustomizationInfoParams params) {
		NavDrawerHeaderParams navDrawerHeaderParams = params.getNavDrawerHeaderParams();
		NavDrawerFooterParams navDrawerFooterParams = params.getNavDrawerFooterParams();
		SetNavDrawerItemsParams navDrawerItemsParams = params.getNavDrawerItemsParams();

		setNavDrawerParams(api, navDrawerHeaderParams, navDrawerFooterParams, navDrawerItemsParams);

		OsmandSettingsParams settingsParams = params.getSettingsParams();
		if (settingsParams != null) {
			api.customizeOsmandSettings(settingsParams.getSharedPreferencesName(), settingsParams.getBundle());
		}

		ArrayList<SetWidgetsParams> visibilityWidgetsParams = params.getVisibilityWidgetsParams();
		ArrayList<SetWidgetsParams> availabilityWidgetsParams = params.getAvailabilityWidgetsParams();

		regWidgetsVisibility(api, visibilityWidgetsParams);
		regWidgetsAvailability(api, availabilityWidgetsParams);

		ArrayList<PluginParams> pluginsParams = params.getPluginsParams();
		if (pluginsParams != null) {
			changePluginsStatus(api, pluginsParams);
		}

		List<String> enabledIds = params.getFeaturesEnabledIds();
		List<String> disabledIds = params.getFeaturesDisabledIds();

		api.setEnabledIds(enabledIds);
		api.setDisabledIds(disabledIds);

		List<String> enabledPatterns = params.getFeaturesEnabledPatterns();
		List<String> disabledPatterns = params.getFeaturesDisabledPatterns();

		api.setEnabledPatterns(enabledPatterns);
		api.setDisabledPatterns(disabledPatterns);
	}

	private void setNavDrawerParams(OsmandAidlApi api, NavDrawerHeaderParams navDrawerHeaderParams, NavDrawerFooterParams navDrawerFooterParams, SetNavDrawerItemsParams navDrawerItemsParams) {
		if (navDrawerHeaderParams != null) {
			api.setNavDrawerLogoWithParams(navDrawerHeaderParams.getImageUri(), navDrawerHeaderParams.getPackageName(), navDrawerHeaderParams.getIntent());
		}
		if (navDrawerFooterParams != null) {
			api.setNavDrawerFooterWithParams(navDrawerFooterParams.getAppName(), navDrawerFooterParams.getPackageName(), navDrawerFooterParams.getIntent());
		}
		if (navDrawerItemsParams != null) {
			api.setNavDrawerItems(navDrawerItemsParams.getAppPackage(), convertNavDrawerItems(navDrawerItemsParams.getItems()));
		}
	}

	private void regWidgetsVisibility(OsmandAidlApi api, ArrayList<SetWidgetsParams> visibilityWidgetsParams) {
		for (SetWidgetsParams setWidgetsParams : visibilityWidgetsParams) {
			api.regWidgetVisibility(setWidgetsParams.getWidgetKey(), setWidgetsParams.getAppModesKeys());
		}
	}

	private void regWidgetsAvailability(OsmandAidlApi api, ArrayList<SetWidgetsParams> availabilityWidgetsParams) {
		for (SetWidgetsParams setWidgetsParams : availabilityWidgetsParams) {
			api.regWidgetAvailability(setWidgetsParams.getWidgetKey(), setWidgetsParams.getAppModesKeys());
		}
	}

	public void changePluginsStatus(OsmandAidlApi api, List<PluginParams> pluginsParams) {
		for (PluginParams pluginParams : pluginsParams) {
			api.changePluginState(pluginParams.getPluginId(), pluginParams.getNewState());
		}
	}

	private List<OsmAndAppCustomization.NavDrawerItem> convertNavDrawerItems(List<NavDrawerItem> drawerItems) {
		List<OsmAndAppCustomization.NavDrawerItem> customizationItems = new ArrayList<>();
		for (NavDrawerItem item : drawerItems) {
			customizationItems.add(new OsmAndAppCustomization.NavDrawerItem(item.getName(), item.getUri(), item.getIconName(), item.getFlags()));
		}
		return customizationItems;
	}

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