package net.osmand.plus.auto;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Distance;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Lane;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.data.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.SurfaceRenderer.SurfaceRendererCallback;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.util.Algorithms;

import java.util.List;

public final class NavigationScreen extends Screen implements SurfaceRendererCallback,
		IRouteInformationListener, DefaultLifecycleObserver {
	/**
	 * Invalid zoom focal point value, used for the zoom buttons.
	 */
	private static final float INVALID_FOCAL_POINT_VAL = -1f;

	/**
	 * Zoom-in scale factor, used for the zoom-in button.
	 */
	private static final float ZOOM_IN_BUTTON_SCALE_FACTOR = 1.1f;

	/**
	 * Zoom-out scale factor, used for the zoom-out button.
	 */
	private static final float ZOOM_OUT_BUTTON_SCALE_FACTOR = 0.9f;

	/**
	 * A listener for navigation start and stop signals.
	 */
	public interface Listener {
		boolean requestLocationNavigation();
		void updateNavigation(boolean navigating);
		void stopNavigation();
	}

	@NonNull
	private final Listener listener;
	@NonNull
	private final Action settingsAction;
	@NonNull
	private final SurfaceRenderer surfaceRenderer;

	private boolean navigating;
	private boolean rerouting;
	private boolean arrived;
	@Nullable
	private List<Destination> destinations;
	@Nullable
	private List<Step> steps;
	@Nullable
	private Distance stepRemainingDistance;
	@Nullable
	private TravelEstimate destinationTravelEstimate;
	private boolean shouldShowNextStep;
	private boolean shouldShowLanes;

	@Nullable
	CarIcon junctionImage;

	private final AlarmWidget alarmWidget;
	@DrawableRes
	private int compassResId = R.drawable.ic_compass_niu;

	private boolean panMode;

	public NavigationScreen(
			@NonNull CarContext carContext,
			@NonNull Action settingsAction,
			@NonNull Listener listener,
			@NonNull SurfaceRenderer surfaceRenderer) {
		super(carContext);
		this.listener = listener;
		this.settingsAction = settingsAction;
		this.surfaceRenderer = surfaceRenderer;
		alarmWidget = new AlarmWidget(getApp(), null);
		getLifecycle().addObserver(this);
	}

	@NonNull
	private OsmandApplication getApp() {
		return (OsmandApplication) getCarContext().getApplicationContext();
	}

	@NonNull
	public SurfaceRenderer getSurfaceRenderer() {
		return surfaceRenderer;
	}

	@Override
	public void onCreate(@NonNull LifecycleOwner owner) {
		getApp().getRoutingHelper().addListener(this);
	}

	@Override
	public void onDestroy(@NonNull LifecycleOwner owner) {
		adjustMapPosition(false);
		getApp().getRoutingHelper().removeListener(this);
		getLifecycle().removeObserver(this);
	}

	@Override
	public void onFrameRendered(@NonNull Canvas canvas, @NonNull Rect visibleArea, @NonNull Rect stableArea) {
		boolean nightMode = getApp().getDaynightHelper().isNightMode();
		DrawSettings drawSettings = new DrawSettings(nightMode, false, surfaceRenderer.getDensity());
		alarmWidget.updateInfo(drawSettings, true);
		Bitmap widgetBitmap = alarmWidget.getWidgetBitmap();
		if (widgetBitmap != null) {
			canvas.drawBitmap(widgetBitmap, visibleArea.right - widgetBitmap.getWidth() - 10, visibleArea.top + 10, new Paint());
		}
	}

	/**
	 * Updates the navigation screen with the next instruction.
	 */
	public void updateTrip(
			boolean navigating,
			boolean rerouting,
			boolean arrived,
			@Nullable List<Destination> destinations,
			@Nullable List<Step> steps,
			@Nullable TravelEstimate destinationTravelEstimate,
			@Nullable Distance stepRemainingDistance,
			boolean shouldShowNextStep,
			boolean shouldShowLanes,
			@Nullable CarIcon junctionImage) {
		this.navigating = navigating;
		this.rerouting = rerouting;
		this.arrived = arrived;
		this.destinations = destinations;
		this.steps = steps;
		this.stepRemainingDistance = stepRemainingDistance;
		this.destinationTravelEstimate = destinationTravelEstimate;
		this.shouldShowNextStep = shouldShowNextStep;
		this.shouldShowLanes = shouldShowLanes;
		this.junctionImage = junctionImage;

		updateNavigation();
		invalidate();
	}

	public void stopTrip() {
		navigating = false;
		rerouting = false;
		arrived = false;
		destinations = null;
		steps = null;
		stepRemainingDistance = null;
		destinationTravelEstimate = null;
		shouldShowNextStep = false;
		shouldShowLanes = false;
		junctionImage = null;

		updateNavigation();
		invalidate();
	}

	private void updateNavigation() {
		listener.updateNavigation(navigating);
		adjustMapPosition(navigating);
	}

	private void adjustMapPosition(boolean shiftMapIfSessionRunning) {
		OsmandApplication app = getApp();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		NavigationSession session = app.getCarNavigationSession();
		boolean sessionStarted = session != null && session.hasStarted();
		boolean shiftMap = shiftMapIfSessionRunning && sessionStarted;
		mapView.setMapPositionX(shiftMap ? 1 : 0);
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		NavigationTemplate.Builder builder = new NavigationTemplate.Builder();
		boolean nightMode = getApp().getDaynightHelper().isNightMode();
		builder.setBackgroundColor(nightMode ? CarColor.SECONDARY : CarColor.PRIMARY);

		// Set the action strip.
		ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();
		updateCompass();
		actionStripBuilder.addAction(
				new Action.Builder()
						.setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), compassResId)).build())
						.setOnClickListener(this::compassClick)
						.build());
		actionStripBuilder.addAction(settingsAction);
		if (navigating) {
			actionStripBuilder.addAction(
					new Action.Builder()
							.setTitle(getApp().getString(R.string.shared_string_control_stop))
							.setOnClickListener(this::stopNavigation)
							.build());
		} else {
			actionStripBuilder.addAction(
					new Action.Builder()
							.setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), R.drawable.ic_action_search_dark)).build())
							.setOnClickListener(this::openSearch)
							.build());
			actionStripBuilder.addAction(
					new Action.Builder()
							.setTitle(getApp().getString(R.string.shared_string_favorites))
							.setOnClickListener(this::openFavorites)
							.build());
		}
		builder.setActionStrip(actionStripBuilder.build());

		// Set the map action strip with the pan and zoom buttons.
		//CarIcon.Builder panIconBuilder = new CarIcon.Builder(
		//		IconCompat.createWithResource(getCarContext(), R.drawable.ic_action_item_move));
		//if (mIsInPanMode) {
		//	panIconBuilder.setTint(CarColor.BLUE);
		//}

		builder.setMapActionStrip(new ActionStrip.Builder()
				.addAction(new Action.Builder(Action.PAN)
						//.setIcon(panIconBuilder.build())
						.build())
				.addAction(
						new Action.Builder()
								.setIcon(
										new CarIcon.Builder(
												IconCompat.createWithResource(
														getCarContext(),
														R.drawable.ic_my_location))
												.build())
								.setOnClickListener(() -> {
									if (!listener.requestLocationNavigation()) {
										surfaceRenderer.handleRecenter();
									}
								})
								.build())
				.addAction(
						new Action.Builder()
								.setIcon(
										new CarIcon.Builder(
												IconCompat.createWithResource(
														getCarContext(),
														R.drawable.ic_zoom_in))
												.build())
								.setOnClickListener(
										() -> surfaceRenderer.handleScale(INVALID_FOCAL_POINT_VAL,
												INVALID_FOCAL_POINT_VAL,
												ZOOM_IN_BUTTON_SCALE_FACTOR))
								.build())
				.addAction(
						new Action.Builder()
								.setIcon(
										new CarIcon.Builder(
												IconCompat.createWithResource(
														getCarContext(),
														R.drawable.ic_zoom_out))
												.build())
								.setOnClickListener(
										() -> surfaceRenderer.handleScale(INVALID_FOCAL_POINT_VAL,
												INVALID_FOCAL_POINT_VAL,
												ZOOM_OUT_BUTTON_SCALE_FACTOR))
								.build())

				.build());

		// When the user enters the pan mode, remind the user that they can exit the pan mode by
		// pressing the select button again.
		builder.setPanModeListener(isInPanMode -> {
			if (isInPanMode) {
				CarToast.makeText(getCarContext(),
						R.string.exit_pan_mode_descr,
						CarToast.LENGTH_LONG).show();
			}
			panMode = isInPanMode;
			invalidate();
		});

		if (navigating) {
			if (destinationTravelEstimate != null) {
				builder.setDestinationTravelEstimate(destinationTravelEstimate);
			}
			if (isRerouting()) {
				builder.setNavigationInfo(new RoutingInfo.Builder().setLoading(true).build());
			} else if (arrived) {
				MessageInfo messageInfo = new MessageInfo.Builder(
						getCarContext().getString(R.string.arrived_at_destination)).build();
				builder.setNavigationInfo(messageInfo);
			} else if (!Algorithms.isEmpty(steps)) {
				RoutingInfo.Builder info = new RoutingInfo.Builder();
				Step firstStep = steps.get(0);
				Step.Builder currentStep = new Step.Builder();
				CarText cue = firstStep.getCue();
				if (cue != null) {
					currentStep.setCue(cue.toCharSequence());
				}
				Maneuver maneuver = firstStep.getManeuver();
				if (maneuver != null) {
					currentStep.setManeuver(maneuver);
				}
				CarText road = firstStep.getRoad();
				if (road != null) {
					currentStep.setRoad(road.toCharSequence());
				}
				if (shouldShowLanes) {
					for (Lane lane : firstStep.getLanes()) {
						currentStep.addLane(lane);
					}
					CarIcon lanesImage = firstStep.getLanesImage();
					if (lanesImage != null) {
						currentStep.setLanesImage(lanesImage);
					}
				}
				if (stepRemainingDistance != null) {
					info.setCurrentStep(currentStep.build(), stepRemainingDistance);
					if (shouldShowNextStep && steps.size() > 1) {
						info.setNextStep(steps.get(1));
					}
				}
				if (junctionImage != null) {
					info.setJunctionImage(junctionImage);
				}
				builder.setNavigationInfo(info.build());
			}
		}
		return builder.build();
	}

	private void updateCompass() {
		OsmandSettings settings = getApp().getSettings();
		boolean nightMode = getApp().getDaynightHelper().isNightMode();
		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_NONE || settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_MANUAL) {
			compassResId = !nightMode ? R.drawable.ic_compass_niu_white : R.drawable.ic_compass_niu;
		} else if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
			compassResId = !nightMode ? R.drawable.ic_compass_bearing_white : R.drawable.ic_compass_bearing;
		} else {
			compassResId = !nightMode ? R.drawable.ic_compass_white : R.drawable.ic_compass;
		}
	}

	private boolean isRerouting() {
		return rerouting || destinations == null;
	}

	private void stopNavigation() {
		listener.stopNavigation();
	}

	private void openFavorites() {
		getScreenManager().pushForResult(new FavoritesScreen(getCarContext(), settingsAction, surfaceRenderer), (obj) -> { });
	}

	private void compassClick() {
		getApp().getMapViewTrackingUtilities().switchRotateMapMode();
	}

	private void openSearch() {
		getScreenManager().pushForResult(new SearchScreen(getCarContext(), settingsAction, surfaceRenderer), (obj) -> { });
		// Test
		//getScreenManager().pushForResult(new SearchResultsScreen(getCarContext(), settingsAction, surfaceRenderer, "cafe"), (obj) -> { });
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		OsmandApplication app = getApp();
		OsmandMap map = app.getOsmandMap();
		RoutingHelper rh = app.getRoutingHelper();
		if (rh.isRoutePlanningMode()) {
			adjustMapPosition(true);
		}
		map.refreshMap();
		if (newRoute && rh.isRoutePlanningMode() && map.getMapView().isCarView()) {
			app.runInUIThread(() -> getApp().getOsmandMap().fitCurrentRouteToMap(false, 0), 300);
		}
	}

	@Override
	public void routeWasCancelled() {
	}

	@Override
	public void routeWasFinished() {
	}
}
