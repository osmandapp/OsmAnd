package net.osmand.plus.auto;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.SurfaceRenderer.SurfaceRendererCallback;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.util.Algorithms;

import java.util.List;

/**
 * Simple demo of how to present a trip on the routing screen.
 */
public final class NavigationScreen extends Screen implements SurfaceRendererCallback {
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
		void updateNavigation(boolean navigating);
		void stopNavigation();
	}

	@NonNull
	private final Listener mListener;
	@NonNull
	private final Action mSettingsAction;
	@NonNull
	private final SurfaceRenderer mSurfaceRenderer;

	private boolean mIsNavigating;
	private boolean mIsRerouting;
	private boolean mHasArrived;

	@Nullable
	private List<Destination> mDestinations;

	@Nullable
	private List<Step> mSteps;

	@Nullable
	private Distance mStepRemainingDistance;

	@Nullable
	private TravelEstimate mDestinationTravelEstimate;
	private boolean mShouldShowNextStep;
	private boolean mShouldShowLanes;

	@Nullable
	CarIcon mJunctionImage;

	private final AlarmWidget alarmWidget;

	private boolean mIsInPanMode;

	public NavigationScreen(
			@NonNull CarContext carContext,
			@NonNull Action settingsAction,
			@NonNull Listener listener,
			@NonNull SurfaceRenderer surfaceRenderer) {
		super(carContext);
		mListener = listener;
		mSettingsAction = settingsAction;
		mSurfaceRenderer = surfaceRenderer;
		alarmWidget = new AlarmWidget((OsmandApplication) carContext.getApplicationContext(), null);
	}

	@NonNull
	public SurfaceRenderer getSurfaceRenderer() {
		return mSurfaceRenderer;
	}

	@Override
	public void onFrameRendered(@NonNull Canvas canvas, @NonNull Rect visibleArea, @NonNull Rect stableArea) {
		DrawSettings drawSettings = new DrawSettings(getCarContext().isDarkMode(), false, mSurfaceRenderer.getDensity());
		alarmWidget.updateInfo(drawSettings);
		Bitmap widgetBitmap = alarmWidget.getWidgetBitmap();
		if (widgetBitmap != null) {
			canvas.drawBitmap(widgetBitmap, visibleArea.right - widgetBitmap.getWidth() - 10, visibleArea.top + 10, new Paint());
		}
	}

	/**
	 * Updates the navigation screen with the next instruction.
	 */
	public void updateTrip(
			boolean isNavigating,
			boolean isRerouting,
			boolean hasArrived,
			@Nullable List<Destination> destinations,
			@Nullable List<Step> steps,
			@Nullable TravelEstimate nextDestinationTravelEstimate,
			@Nullable Distance nextStepRemainingDistance,
			boolean shouldShowNextStep,
			boolean shouldShowLanes,
			@Nullable CarIcon junctionImage) {
		mIsNavigating = isNavigating;
		mIsRerouting = isRerouting;
		mHasArrived = hasArrived;
		mDestinations = destinations;
		mSteps = steps;
		mStepRemainingDistance = nextStepRemainingDistance;
		mDestinationTravelEstimate = nextDestinationTravelEstimate;
		mShouldShowNextStep = shouldShowNextStep;
		mShouldShowLanes = shouldShowLanes;
		mJunctionImage = junctionImage;
		invalidate();
	}

	public void stopTrip() {
		mIsNavigating = false;
		mIsRerouting = false;
		mHasArrived = false;
		mDestinations = null;
		mSteps = null;
		mStepRemainingDistance = null;
		mDestinationTravelEstimate = null;
		mShouldShowNextStep = false;
		mShouldShowLanes = false;
		mJunctionImage = null;
		invalidate();
	}

	@NonNull
	@Override
	public Template onGetTemplate() {
		mSurfaceRenderer.updateMarkerVisibility(
				/* showMarkers=*/ false, /* numMarkers=*/ 0, /* activeMarker=*/ -1);

		NavigationTemplate.Builder builder = new NavigationTemplate.Builder();
		builder.setBackgroundColor(CarColor.SECONDARY);

		// Set the action strip.
		ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();
		actionStripBuilder.addAction(mSettingsAction);
		if (mIsNavigating) {
			actionStripBuilder.addAction(
					new Action.Builder()
							.setTitle("Stop")
							.setOnClickListener(this::stopNavigation)
							.build());
		} else {
			/*
			actionStripBuilder.addAction(
					new Action.Builder()
							.setIcon(
									new CarIcon.Builder(
											IconCompat.createWithResource(
													getCarContext(),
													R.drawable.ic_action_search_dark))
											.build())
							.setOnClickListener(this::openSearch)
							.build());
			 */
			actionStripBuilder.addAction(
					new Action.Builder()
							.setTitle("Favorites")
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
								.setOnClickListener(mSurfaceRenderer::handleRecenter)
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
										() -> mSurfaceRenderer.handleScale(INVALID_FOCAL_POINT_VAL,
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
										() -> mSurfaceRenderer.handleScale(INVALID_FOCAL_POINT_VAL,
												INVALID_FOCAL_POINT_VAL,
												ZOOM_OUT_BUTTON_SCALE_FACTOR))
								.build())

				.build());

		// When the user enters the pan mode, remind the user that they can exit the pan mode by
		// pressing the select button again.
		builder.setPanModeListener(isInPanMode -> {
			if (isInPanMode) {
				CarToast.makeText(getCarContext(),
						"Press Select to exit the pan mode",
						CarToast.LENGTH_LONG).show();
			}
			mIsInPanMode = isInPanMode;
			invalidate();
		});

		if (mIsNavigating) {
			if (mDestinationTravelEstimate != null) {
				builder.setDestinationTravelEstimate(mDestinationTravelEstimate);
			}
			if (isRerouting()) {
				builder.setNavigationInfo(new RoutingInfo.Builder().setLoading(true).build());
			} else if (mHasArrived) {
				MessageInfo messageInfo = new MessageInfo.Builder(
						getCarContext().getString(R.string.arrived_at_destination)).build();
				builder.setNavigationInfo(messageInfo);
			} else if (!Algorithms.isEmpty(mSteps)) {
				RoutingInfo.Builder info = new RoutingInfo.Builder();
				Step firstStep = mSteps.get(0);
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
				if (mShouldShowLanes) {
					for (Lane lane : firstStep.getLanes()) {
						currentStep.addLane(lane);
					}
					CarIcon lanesImage = firstStep.getLanesImage();
					if (lanesImage != null) {
						currentStep.setLanesImage(lanesImage);
					}
				}
				if (mStepRemainingDistance != null) {
					info.setCurrentStep(currentStep.build(), mStepRemainingDistance);
					if (mShouldShowNextStep && mSteps.size() > 1) {
						info.setNextStep(mSteps.get(1));
					}
				}
				if (mJunctionImage != null) {
					info.setJunctionImage(mJunctionImage);
				}
				builder.setNavigationInfo(info.build());
			}
		}
		mListener.updateNavigation(mIsNavigating);

		return builder.build();
	}

	private boolean isRerouting() {
		return mIsRerouting || mDestinations == null;
	}

	private void stopNavigation() {
		mListener.stopNavigation();
	}

	private void openFavorites() {
		getScreenManager()
				.pushForResult(
						new FavoritesScreen(getCarContext(), mSettingsAction, mSurfaceRenderer),
						(obj) -> {
							if (obj != null) {
                                /*
                                // Need to copy over each element to satisfy Java type safety.
                                List<?> results = (List<?>) obj;
                                List<Instruction> instructions = new ArrayList<Instruction>();
                                for (Object result : results) {
                                    instructions.add((Instruction) result);
                                }
                                mListener.executeScript(instructions);
                                 */
							}
						});
	}

	private void openSearch() {
		getScreenManager()
				.pushForResult(
						new SearchScreen(getCarContext(), mSettingsAction, mSurfaceRenderer),
						(obj) -> {
							if (obj != null) {
                                /*
                                // Need to copy over each element to satisfy Java type safety.
                                List<?> results = (List<?>) obj;
                                List<Instruction> instructions = new ArrayList<Instruction>();
                                for (Object result : results) {
                                    instructions.add((Instruction) result);
                                }
                                mListener.executeScript(instructions);
                                 */
							}
						});
	}
}
