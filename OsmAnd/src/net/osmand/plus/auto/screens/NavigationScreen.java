package net.osmand.plus.auto.screens;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
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
import net.osmand.plus.auto.NavigationListener;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.SurfaceRenderer;
import net.osmand.plus.auto.SurfaceRenderer.SurfaceRendererCallback;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.plus.views.mapwidgets.widgets.SpeedometerWidget;
import net.osmand.util.Algorithms;

import java.util.List;

public final class NavigationScreen extends BaseAndroidAutoScreen implements SurfaceRendererCallback,
		IRouteInformationListener, DefaultLifecycleObserver {

	@NonNull
	private final NavigationListener listener;
	@NonNull
	private final Action settingsAction;

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
	private final SpeedometerWidget speedometerWidget;
	@DrawableRes
	private int compassResId = R.drawable.ic_compass_niu;

	private boolean panMode;

	public NavigationScreen(
			@NonNull CarContext carContext,
			@NonNull Action settingsAction,
			@NonNull NavigationListener listener) {
		super(carContext);
		this.listener = listener;
		this.settingsAction = settingsAction;

		OsmandApplication app = getApp();
		alarmWidget = new AlarmWidget(app, null);
		speedometerWidget = new SpeedometerWidget(app, null, null);

		getLifecycle().addObserver(this);
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
		SurfaceRenderer surfaceRenderer = getSurfaceRenderer();
		if (surfaceRenderer != null) {
			DrawSettings drawSettings = new DrawSettings(getCarContext().isDarkMode(), false, surfaceRenderer.getDensity());

			alarmWidget.updateInfo(drawSettings, true);
			speedometerWidget.updateInfo(drawSettings, true, drawSettings.isNightMode());

			Bitmap alarmBitmap = alarmWidget.getWidgetBitmap();
			Bitmap speedometerBitmap = speedometerWidget.getWidgetBitmap();

			if (speedometerBitmap != null) {
				canvas.drawBitmap(speedometerBitmap, visibleArea.right - speedometerBitmap.getWidth() - 10, visibleArea.top + 10, new Paint());
			}
			if (alarmBitmap != null) {
				int offset = speedometerBitmap != null ? speedometerBitmap.getWidth() : 0;
				canvas.drawBitmap(alarmBitmap, visibleArea.right - alarmBitmap.getWidth() - 10 - offset, visibleArea.top + 10, new Paint());
			}
		}
	}

	@Nullable
	private SurfaceRenderer getSurfaceRenderer() {
		NavigationSession session = getApp().getCarNavigationSession();
		return session != null ? session.getNavigationCarSurface() : null;
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
		finish();
	}

	private void updateNavigation() {
		adjustMapPosition(navigating);
	}

	private void adjustMapPosition(boolean shiftMapIfSessionRunning) {
		OsmandApplication app = getApp();
		NavigationSession session = app.getCarNavigationSession();
		boolean sessionStarted = session != null && session.hasStarted();
		boolean shiftMap = shiftMapIfSessionRunning && sessionStarted;
		app.getMapViewTrackingUtilities().getMapDisplayPositionManager().setMapPositionShiftedX(shiftMap);
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		NavigationTemplate.Builder builder = new NavigationTemplate.Builder();
		builder.setBackgroundColor(CarColor.SECONDARY);

		// Set the action strip.
		SurfaceRenderer surfaceRenderer = getSurfaceRenderer();
		ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();
		updateCompass();
		if (!navigating) {
			actionStripBuilder.addAction(
					new Action.Builder()
							.setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), R.drawable.ic_action_list_flat)).build())
							.setOnClickListener(this::goBack)
							.build());
		}
		actionStripBuilder.addAction(
				new Action.Builder()
						.setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), compassResId)).build())
						.setOnClickListener(this::compassClick)
						.build());
		if (getApp().useOpenGlRenderer()) {
			actionStripBuilder.addAction(
					new Action.Builder()
							.setIcon(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), R.drawable.ic_action_3d)).build())
							.setOnClickListener(() -> {
								if (surfaceRenderer != null) {
									surfaceRenderer.handleTilt();
								}
							})
							.build());
		}
		actionStripBuilder.addAction(settingsAction);
		if (navigating) {
			actionStripBuilder.addAction(
					new Action.Builder()
							.setTitle(getApp().getString(R.string.shared_string_control_stop))
							.setOnClickListener(this::stopNavigation)
							.build());
		}
		builder.setActionStrip(actionStripBuilder.build());

		// Set the map action strip with the pan and zoom buttons.
		//CarIcon.Builder panIconBuilder = new CarIcon.Builder(
		//		IconCompat.createWithResource(getCarContext(), R.drawable.ic_action_item_move));
		//if (mIsInPanMode) {
		//	panIconBuilder.setTint(CarColor.BLUE);
		//}

		ActionStrip.Builder mapActionStripBuilder = new ActionStrip.Builder();
		builder.setMapActionStrip(
				mapActionStripBuilder
						.addAction(new Action.Builder(Action.PAN)
								//.setIcon(panIconBuilder.build())
								.build())
						.addAction(new Action.Builder()
								.setIcon(
										new CarIcon.Builder(
												IconCompat.createWithResource(
														getCarContext(),
														R.drawable.ic_my_location))
												.build())
								.setOnClickListener(() -> {
									if (!listener.requestLocationNavigation()) {
										if (surfaceRenderer != null) {
											surfaceRenderer.handleRecenter();
										}
									}
								})
								.build())
						.addAction(new Action.Builder()
								.setIcon(
										new CarIcon.Builder(
												IconCompat.createWithResource(
														getCarContext(),
														R.drawable.ic_zoom_in))
												.build())
								.setOnClickListener(
										() -> {
											if (surfaceRenderer != null) {
												surfaceRenderer.handleScale(NavigationSession.INVALID_FOCAL_POINT_VAL,
														NavigationSession.INVALID_FOCAL_POINT_VAL,
														NavigationSession.ZOOM_IN_BUTTON_SCALE_FACTOR);
											}
										})
								.build())
						.addAction(new Action.Builder()
								.setIcon(
										new CarIcon.Builder(
												IconCompat.createWithResource(
														getCarContext(),
														R.drawable.ic_zoom_out))
												.build())
								.setOnClickListener(
										() -> {
											if (surfaceRenderer != null) {
												surfaceRenderer.handleScale(NavigationSession.INVALID_FOCAL_POINT_VAL,
														NavigationSession.INVALID_FOCAL_POINT_VAL,
														NavigationSession.ZOOM_OUT_BUTTON_SCALE_FACTOR);
											}
										})
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
		boolean nightMode = getCarContext().isDarkMode();
		CompassMode compassMode = settings.getCompassMode();
		compassResId = compassMode.getIconId(nightMode);
	}

	private boolean isRerouting() {
		return rerouting || destinations == null;
	}

	private void stopNavigation() {
		listener.stopNavigation();
	}

	private void compassClick() {
		getApp().getMapViewTrackingUtilities().requestSwitchCompassToNextMode();
		invalidate();
	}

	private void goBack() {
		finish();
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
