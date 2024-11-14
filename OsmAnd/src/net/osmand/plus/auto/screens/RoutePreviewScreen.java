package net.osmand.plus.auto.screens;

import static net.osmand.search.core.ObjectType.GPX_TRACK;

import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;


import net.osmand.PlatformUtil;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.TripUtils;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.search.core.SearchResult;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The route preview screen for the app.
 */
public final class RoutePreviewScreen extends BaseAndroidAutoScreen implements IRouteInformationListener,
		DefaultLifecycleObserver {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(RoutePreviewScreen.class);

	@NonNull
	private final Action settingsAction;
	@NonNull
	private final SearchResult searchResult;
	@NonNull
	private List<Row> routeRows = new ArrayList<>();

	@Nullable
	private GpxFile routeGpxFile;

	private CompassMode savedCompassMode = CompassMode.NORTH_IS_UP;
	private float prevElevationAngle = 90;
	private float prevRotationAngle = 0;
	private boolean prevMapLinkedToLocation = false;


	private final StateChangedListener<Void> stateChangedListener = new StateChangedListener<Void>() {
		@Override
		public void stateChanged(Void change) {
			if (routeGpxFile != null) {
				QuadRect mapRect = new QuadRect();
				Algorithms.extendRectToContainRect(mapRect, SharedUtil.jQuadRect(routeGpxFile.getRect()));

				adjustMapToRect(getApp().getMapViewTrackingUtilities().getDefaultLocation(), mapRect);
			}
		}
	};

	private boolean calculating;

	public RoutePreviewScreen(@NonNull CarContext carContext, @NonNull Action settingsAction,
	                          @NonNull SearchResult searchResult) {
		super(carContext);
		this.settingsAction = settingsAction;
		this.searchResult = searchResult;

		getLifecycle().addObserver(this);
		calculating = true;
	}

	private void prepareRoute() {
		if (searchResult.objectType == GPX_TRACK) {
			GPXInfo gpxInfo = ((GPXInfo) searchResult.relatedObject);
			File file = gpxInfo.getFile();
			SelectedGpxFile selectedGpxFile = getApp().getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath());
			if (selectedGpxFile == null) {
				GpxFileLoaderTask.loadGpxFile(file, null, gpxFile -> {
					buildRouteByGivenGpx(gpxFile);
					return true;
				});
			} else {
				buildRouteByGivenGpx(selectedGpxFile.getGpxFile());
			}
		} else {
			getApp().getOsmandMap().getMapLayers().getMapActionsHelper().replaceDestination(
					searchResult.location, QuickSearchListItem.getPointDescriptionObject(getApp(), searchResult).first);
			invalidate();
		}
	}

	private void buildRouteByGivenGpx(@NonNull GpxFile gpxFile) {
		routeGpxFile = gpxFile;
		getApp().getOsmandMap().getMapLayers().getMapActionsHelper().buildRouteByGivenGpx(gpxFile);
		invalidate();
	}

	private void updateRoute(boolean newRoute) {
		OsmandApplication app = getApp();
		RoutingHelper rh = app.getRoutingHelper();
		Distance distance = null;
		int leftTimeSec = 0;
		if (newRoute && rh.isRoutePlanningMode()) {
			distance = TripUtils.getDistance(app, rh.getLeftDistance());
			leftTimeSec = rh.getLeftTime();
		}
		if (distance != null && leftTimeSec > 0) {
			List<Row> routeRows = new ArrayList<>();
			SpannableString description = new SpannableString("  •  ");
			description.setSpan(DistanceSpan.create(distance), 0, 1, 0);
			description.setSpan(DurationSpan.create(leftTimeSec), 4, 5, 0);

			String name = QuickSearchListItem.getName(app, searchResult);
			String typeName = QuickSearchListItem.getTypeName(app, searchResult);
			String title = Algorithms.isEmpty(name) ? typeName : name;
			routeRows.add(new Row.Builder().setTitle(title).addText(description).build());
			this.routeRows = routeRows;
			calculating = false;
			invalidate();
		}
	}

	@Override
	public void onCreate(@NonNull LifecycleOwner owner) {
		getApp().getRoutingHelper().addListener(this);
		getApp().getTargetPointsHelper().addListener(stateChangedListener);
		prevMapLinkedToLocation = getApp().getMapViewTrackingUtilities().isMapLinkedToLocation();
		OsmandMapTileView mapView = getApp().getOsmandMap().getMapView();
		savedCompassMode = getApp().getSettings().getCompassMode();
		prevRotationAngle = mapView.getRotate();
		getApp().getSettings().setCompassMode(CompassMode.NORTH_IS_UP);
		prevElevationAngle = mapView.normalizeElevationAngle(mapView.getElevationAngle());
		NavigationSession navigationSession = getSession();
		if (getApp().getRoutingHelper().isRouteCalculated() && navigationSession != null && navigationSession.isCarNavigationActive()) {
			updateRoute(true);
		} else {
			prepareRoute();
		}
	}

	@Override
	public void onDestroy(@NonNull LifecycleOwner owner) {
		OsmandApplication app = getApp();
		RoutingHelper routingHelper = app.getRoutingHelper();
		routingHelper.removeListener(this);
		if (routingHelper.isRoutePlanningMode()) {
			app.stopNavigation();
		}
		getApp().getTargetPointsHelper().removeListener(stateChangedListener);
		getLifecycle().removeObserver(this);
	}

	@Override
	public void onStart(@NonNull LifecycleOwner owner) {
		recenterMap();
	}

	@Override
	public void onResume(@NonNull LifecycleOwner owner) {
		if (getApp().getRoutingHelper().isRouteCalculated()) {
			zoomMapToRoute();
		}
	}

	@Override
	public void onStop(@NonNull LifecycleOwner owner) {
		if(getApp().getSettings().getCompassMode() != savedCompassMode) {
			getApp().getSettings().setCompassMode(savedCompassMode);
		}
		OsmandMapTileView mapView = getApp().getOsmandMap().getMapView();
		if(mapView.getElevationAngle() != prevElevationAngle) {
			mapView.setElevationAngle(prevElevationAngle);
		}
		if(mapView.getRotate() != prevRotationAngle) {
			mapView.setRotate(prevRotationAngle, true);
		}
		if(prevMapLinkedToLocation != getApp().getMapViewTrackingUtilities().isMapLinkedToLocation()) {
			getApp().getMapViewTrackingUtilities().setMapLinkedToLocation(prevMapLinkedToLocation);
		}
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		ItemList.Builder listBuilder = new ItemList.Builder();
		listBuilder
				.setOnSelectedListener(this::onRouteSelected)
				.setOnItemsVisibilityChangedListener(this::onRoutesVisible);
		for (Row row : routeRows) {
			listBuilder.addItem(row);
		}
		RoutePreviewNavigationTemplate.Builder builder = new RoutePreviewNavigationTemplate.Builder();
		if (calculating) {
			builder.setLoading(true);
		} else {
			builder.setLoading(false);
			if (!Algorithms.isEmpty(routeRows)) {
				builder.setItemList(listBuilder.build());
			}
		}
		builder
				.setTitle(getCarContext().getString(R.string.current_route))
				.setActionStrip(new ActionStrip.Builder().addAction(settingsAction).build())
				.setHeaderAction(Action.BACK)
				.setNavigateAction(
						new Action.Builder()
								.setTitle(getApp().getString(R.string.shared_string_control_start))
								.setOnClickListener(this::onNavigate)
								.build());
		return builder.build();
	}

	private void onRouteSelected(int index) {
	}

	private void onRoutesVisible(int startIndex, int endIndex) {
	}

	private void onNavigate() {
		setResult(searchResult);
		finish();
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		zoomMapToRoute();
		updateRoute(newRoute);
	}

	private void zoomMapToRoute() {
		RoutingHelper rh = getApp().getRoutingHelper();
		QuadRect mapRect = RoutingHelperUtils.getRouteRect(getApp(), rh.getRoute());
		if (mapRect != null) {
			adjustMapToRect(getApp().getMapViewTrackingUtilities().getDefaultLocation(), mapRect);
		}
	}

	@Override
	public void routeWasCancelled() {
		if (!getApp().getRoutingHelper().isRoutePlanningMode()) {
			finish();
		}
	}

	@Override
	public void routeWasFinished() {
	}

	public void tryToStartNavigation() {
		if (!calculating) {
			onNavigate();
		}
	}

	@Override
	protected void adjustMapToRect(@NonNull LatLon location, @NonNull QuadRect mapRect) {
		OsmandMapTileView mapView = getApp().getOsmandMap().getMapView();
		mapView.setElevationAngle(90f);
		getApp().getMapViewTrackingUtilities().setMapLinkedToLocation(false);
		mapView.setRotate(0f, true);
		super.adjustMapToRect(location, mapRect);
	}
}
