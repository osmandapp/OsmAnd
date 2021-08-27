package net.osmand.plus.auto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Distance;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Lane;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.core.graphics.drawable.IconCompat;

import net.osmand.plus.R;

import java.util.List;

/**
 * Simple demo of how to present a trip on the routing screen.
 */
public final class NavigationScreen extends Screen {
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

		/**
		 * Stops navigation.
		 */
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
		CarIcon.Builder panIconBuilder = new CarIcon.Builder(
				IconCompat.createWithResource(
						getCarContext(),
						R.drawable.ic_action_item_move));
		if (mIsInPanMode) {
			panIconBuilder.setTint(CarColor.BLUE);
		}

		builder.setMapActionStrip(new ActionStrip.Builder()
				.addAction(new Action.Builder(Action.PAN)
						.setIcon(panIconBuilder.build())
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
			} else {
				RoutingInfo.Builder info = new RoutingInfo.Builder();
				Step tmp = mSteps.get(0);
				Step.Builder currentStep =
						new Step.Builder(tmp.getCue().toCharSequence())
								.setManeuver(tmp.getManeuver())
								.setRoad(tmp.getRoad().toCharSequence());
				if (mShouldShowLanes) {
					for (Lane lane : tmp.getLanes()) {
						currentStep.addLane(lane);
					}
					currentStep.setLanesImage(tmp.getLanesImage());
				}
				info.setCurrentStep(currentStep.build(), mStepRemainingDistance);
				if (mShouldShowNextStep && mSteps.size() > 1) {
					info.setNextStep(mSteps.get(1));
				}
				if (mJunctionImage != null) {
					info.setJunctionImage(mJunctionImage);
				}
				builder.setNavigationInfo(info.build());
			}
		}

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
