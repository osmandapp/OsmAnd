package net.osmand.aidl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import net.osmand.aidl.calculateroute.CalculateRouteParams;
import net.osmand.aidl.maplayer.AddMapLayerParams;
import net.osmand.aidl.maplayer.RemoveMapLayerParams;
import net.osmand.aidl.maplayer.UpdateMapLayerParams;
import net.osmand.aidl.maplayer.point.AddMapPointParams;
import net.osmand.aidl.maplayer.point.RemoveMapPointParams;
import net.osmand.aidl.maplayer.point.UpdateMapPointParams;
import net.osmand.aidl.mapmarker.AddMapMarkerParams;
import net.osmand.aidl.mapmarker.RemoveMapMarkerParams;
import net.osmand.aidl.mapmarker.UpdateMapMarkerParams;
import net.osmand.aidl.mapwidget.AddMapWidgetParams;
import net.osmand.aidl.mapwidget.RemoveMapWidgetParams;
import net.osmand.aidl.mapwidget.UpdateMapWidgetParams;
import net.osmand.plus.OsmandApplication;

public class OsmandAidlService extends Service {

	OsmandApplication getApp() {
		return (OsmandApplication) getApplication();
	}

	OsmandAidlApi getApi() {
		return getApp().getAidlApi();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Return the interface
		return mBinder;
	}

	private final IOsmAndAidlInterface.Stub mBinder = new IOsmAndAidlInterface.Stub() {

		@Override
		public boolean addMapMarker(AddMapMarkerParams params) throws RemoteException {
			try {
				return params != null && getApi().addMapMarker(params.getMarker());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean removeMapMarker(RemoveMapMarkerParams params) throws RemoteException {
			try {
				return params != null && getApi().removeMapMarker(params.getMarker());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean updateMapMarker(UpdateMapMarkerParams params) throws RemoteException {
			try {
				return params != null && getApi().updateMapMarker(params.getMarkerPrev(), params.getMarkerNew());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean addMapWidget(AddMapWidgetParams params) throws RemoteException {
			try {
				return params != null && getApi().addMapWidget(params.getWidget());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean removeMapWidget(RemoveMapWidgetParams params) throws RemoteException {
			try {
				return params != null && getApi().removeMapWidget(params.getId());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean updateMapWidget(UpdateMapWidgetParams params) throws RemoteException {
			try {
				return params != null && getApi().updateMapWidget(params.getWidget());
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public boolean addMapPoint(AddMapPointParams params) throws RemoteException {
			return false;
		}

		@Override
		public boolean removeMapPoint(RemoveMapPointParams params) throws RemoteException {
			return false;
		}

		@Override
		public boolean updateMapPoint(UpdateMapPointParams params) throws RemoteException {
			return false;
		}

		@Override
		public boolean addMapLayer(AddMapLayerParams params) throws RemoteException {
			return false;
		}

		@Override
		public boolean removeMapLayer(RemoveMapLayerParams params) throws RemoteException {
			return false;
		}

		@Override
		public boolean updateMapLayer(UpdateMapLayerParams params) throws RemoteException {
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
	};
}
