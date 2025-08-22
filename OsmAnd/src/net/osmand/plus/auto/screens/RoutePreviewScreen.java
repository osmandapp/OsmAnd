package net.osmand.plus.auto.screens;

import static net.osmand.search.core.ObjectType.GPX_TRACK;

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
import net.osmand.StateChangedListener;
import net.osmand.data.QuadRect;
import net.osmand.data.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.TripUtils;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.SelectedGpxFile;
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


	private boolean calculateRoute;
	private boolean calculating;

	private final StateChangedListener<Void> stateChangedListener = new StateChangedListener<>() {
		@Override
		public void stateChanged(Void change) {
			if (routeGpxFile != null) {
				QuadRect mapRect = new QuadRect();
				Algorithms.extendRectToContainRect(mapRect, SharedUtil.jQuadRect(routeGpxFile.getRect()));
				adjustMapToRect(getApp().getMapViewTrackingUtilities().getDefaultLocation(), mapRect);
			}
		}
	};

	public RoutePreviewScreen(@NonNull CarContext carContext, @NonNull Action settingsAction,
	                          @NonNull SearchResult searchResult, boolean calculateRoute) {
		super(carContext);
		this.settingsAction = settingsAction;
		this.searchResult = searchResult;
		this.calculateRoute = calculateRoute;
		getLifecycle().addObserver(this);
		calculating = calculateRoute;
		setMarker(RoutePreviewScreen.class.getSimpleName());
	}

	@Override
	protected boolean shouldRestoreMapState() {
		return true;
	}

	private void prepareRoute() {
		if (searchResult.objectType == GPX_TRACK) {
			GPXInfo gpxInfo = ((GPXInfo) searchResult.relatedObject);
			File file = gpxInfo.getFile();
			if (file == null) {
				return;
			}
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
			getApp().getOsmandMap().getMapActions().replaceDestination(
					searchResult.location, QuickSearchListItem.getPointDescriptionObject(getApp(), searchResult).first);
			invalidate();
		}
	}

	private void buildRouteByGivenGpx(@NonNull GpxFile gpxFile) {
		routeGpxFile = gpxFile;
		getApp().getOsmandMap().getMapActions().buildRouteByGivenGpx(gpxFile);
		invalidate();
	}

	private void updateRoute(boolean newRoute) {
		OsmandApplication app = getApp();
		RoutingHelper routingHelper = app.getRoutingHelper();
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();

		Distance distance = null;
		int leftTimeSec = 0;
		if (newRoute && routingHelper.isRoutePlanningMode()) {
			distance = TripUtils.getDistance(app, routingHelper.getLeftDistance());
			leftTimeSec = routingHelper.getLeftTime();
		}
		if (distance != null && leftTimeSec > 0) {
			List<Row> routeRows = new ArrayList<>();
			SpannableString description = new SpannableString("  •  ");
			description.setSpan(DistanceSpan.create(distance), 0, 1, 0);
			description.setSpan(DurationSpan.create(leftTimeSec), 4, 5, 0);

			TargetPoint finish = targetPointsHelper.getPointToNavigate();
			String title = finish != null ? finish.getRoutePointDescription(app, true) : null;

			if (Algorithms.isEmpty(title)) {
				String name = QuickSearchListItem.getName(app, searchResult);
				String typeName = QuickSearchListItem.getTypeName(app, searchResult);
				title = Algorithms.isEmpty(name) ? typeName : name;
			}
			routeRows.add(new Row.Builder().setTitle(title).addText(description).build());
			this.routeRows = routeRows;
			calculating = app.getRoutingHelper().isRouteBeingCalculated();
			invalidate();
		} else if (routingHelper.getRoute().hasMissingMaps()) {
			NavigationSession session = getSession();
			if (session != null) {
				session.showMissingMapsScreen();
			}
		}
	}

	@Override
	public void onCreate(@NonNull LifecycleOwner owner) {
		super.onCreate(owner);
		OsmandApplication app = getApp();
		app.getRoutingHelper().addListener(this);
		app.getTargetPointsHelper().addListener(stateChangedListener);
		if (calculateRoute) {
			prepareRoute();
		} else {
			updateRoute(true);
		}
	}

	@Override
	public void onDestroy(@NonNull LifecycleOwner owner) {
		super.onDestroy(owner);
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
	public void onResume(@NonNull LifecycleOwner owner) {
		if (getApp().getRoutingHelper().isRouteCalculated()) {
			zoomMapToRoute();
		}
	}

	@NonNull
	@Override
	public Template getTemplate() {
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
			if (!Algorithms.isEmpty(routeRows)) {
				builder.setLoading(false);
				builder.setItemList(listBuilder.build());
			} else {
				builder.setLoading(true);
				finish();
			}
		}
		builder.setTitle(getCarContext().getString(R.string.current_route))
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
}
