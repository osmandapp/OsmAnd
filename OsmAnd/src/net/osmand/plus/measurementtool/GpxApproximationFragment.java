package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.LocationsHolder;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.GpxApproximator;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;

import org.apache.commons.logging.Log;

import static net.osmand.plus.measurementtool.ProfileCard.ProfileCardListener;
import static net.osmand.plus.measurementtool.SliderCard.SliderCardListener;

public class GpxApproximationFragment extends ContextMenuScrollFragment
		implements SliderCardListener, ProfileCardListener {

	private static final Log LOG = PlatformUtil.getLog(GpxApproximationFragment.class);
	public static final String TAG = GpxApproximationFragment.class.getName();
	public static final String DISTANCE_THRESHOLD_KEY = "distance_threshold";
	public static final String SNAP_TO_ROAD_APP_MODE_STRING_KEY = "snap_to_road_app_mode";

	public static final int REQUEST_CODE = 1100;

	private int menuTitleHeight;
	private ApplicationMode snapToRoadAppMode = ApplicationMode.CAR;
	private int distanceThreshold = 50;
	private boolean applyApproximation;

	private LocationsHolder locationsHolder;
	@Nullable
	private GpxApproximator gpxApproximator;
	private ProgressBar progressBar;
	private View cancelButton;
	private View applyButton;

	private SliderCard sliderCard;

	@Override
	public int getMainLayoutId() {
		return R.layout.fragment_gpx_approximation_bottom_sheet_dialog;
	}

	@Override
	public int getTopViewId() {
		return R.id.gpx_approximation_top_shadow_all;
	}

	@Override
	public int getHeaderViewHeight() {
		return menuTitleHeight;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return true;
	}

	@Override
	public int getToolbarHeight() {
		return 0;
	}

	public float getMiddleStateKoef() {
		return 0.5f;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HALF_SCREEN;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View mainView = super.onCreateView(inflater, container, savedInstanceState);
		if (mainView == null) {
			return null;
		}
		if (savedInstanceState != null) {
			distanceThreshold = savedInstanceState.getInt(DISTANCE_THRESHOLD_KEY);
			snapToRoadAppMode = ApplicationMode.valueOfStringKey(
					savedInstanceState.getString(SNAP_TO_ROAD_APP_MODE_STRING_KEY), ApplicationMode.CAR);
			try {
				gpxApproximator = new GpxApproximator(requireMyApplication(), snapToRoadAppMode, distanceThreshold, locationsHolder);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		} else {
			try {
				gpxApproximator = new GpxApproximator(requireMyApplication(), locationsHolder);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}

		if (gpxApproximator != null) {
			gpxApproximator.setApproximationProgress(new GpxApproximator.GpxApproximationProgressCallback() {

				@Override
				public void start() {
					if (isResumed()) {
						startProgress();
					}
				}

				@Override
				public void updateProgress(int progress) {
					if (isResumed()) {
						GpxApproximationFragment.this.updateProgress(progress);
					}
				}

				@Override
				public void finish() {
					if (isResumed()) {
						finishProgress();
					}
				}
			});
		}

		applyButton = mainView.findViewById(R.id.right_bottom_button);
		cancelButton = mainView.findViewById(R.id.dismiss_button);
		if (isPortrait()) {
			updateCardsLayout();
		}
		updateCards();
		updateButtons(mainView);

		progressBar = mainView.findViewById(R.id.progress_bar);
		if (progressBar != null) {
			requireMapActivity().setupRouteCalculationProgressBar(progressBar);
			progressBar.setIndeterminate(false);
		}

		if (!isPortrait()) {
			int widthNoShadow = getLandscapeNoShadowWidth();
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.BOTTOM | Gravity.START;
			mainView.findViewById(R.id.control_buttons).setLayoutParams(params);
		}
		enterGpxApproximationMode();
		runLayoutListener();

		calculateGpxApproximation();

		return mainView;
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		int sliderHeight = sliderCard != null ? sliderCard.getViewHeight() : 0;
		menuTitleHeight = view.findViewById(R.id.control_buttons).getHeight()
				- view.findViewById(R.id.buttons_shadow).getHeight() + sliderHeight;
		super.calculateLayout(view, initLayout);
	}

	@Override
	protected boolean isHideable() {
		return false;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(DISTANCE_THRESHOLD_KEY, distanceThreshold);
		outState.putString(SNAP_TO_ROAD_APP_MODE_STRING_KEY, snapToRoadAppMode.getStringKey());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (gpxApproximator != null) {
			gpxApproximator.cancelApproximation();
		}
		exitGpxApproximationMode();
		if (!applyApproximation) {
			Fragment fragment = getTargetFragment();
			if (fragment instanceof GpxApproximationFragmentListener) {
				((GpxApproximationFragmentListener) fragment).onCancelGpxApproximation();
			}
		}
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackgroundDrawable(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(),
						R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, isNightMode(),
						R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(),
						R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
			}
		}
	}

	private void updateButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.route_info_bg));
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = getTargetFragment();
				if (fragment instanceof GpxApproximationFragmentListener) {
					((GpxApproximationFragmentListener) fragment).onApplyGpxApproximation();
				}
				applyApproximation = true;
				dismiss();
			}
		});
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});
		UiUtilities.setupDialogButton(isNightMode(), cancelButton, UiUtilities.DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		UiUtilities.setupDialogButton(isNightMode(), applyButton, UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_apply);
		AndroidUiHelper.updateVisibility(applyButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	private void setApplyButtonEnabled(boolean enabled) {
		if (applyButton != null) {
			applyButton.setEnabled(enabled);
		}
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();

			if (getTopView() != null) {
				sliderCard = new SliderCard(mapActivity, distanceThreshold);
				sliderCard.setListener(this);
				getTopView().addView(sliderCard.build(mapActivity));
			}

			ProfileCard profileCard = new ProfileCard(mapActivity, snapToRoadAppMode);
			profileCard.setListener(this);
			cardsContainer.addView(profileCard.build(mapActivity));
		}
	}

	private void enterGpxApproximationMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			AndroidUiHelper.setVisibility(mapActivity, portrait ? View.INVISIBLE : View.GONE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info);
		}
	}

	private void exitGpxApproximationMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info,
					R.id.map_search_button);
		}
	}

	@Override
	public boolean shouldShowMapControls(int menuState) {
		return (menuState & (MenuState.HEADER_ONLY | MenuState.HALF_SCREEN)) != 0;
	}

	public static void showInstance(@NonNull FragmentManager fm, @Nullable Fragment targetFragment, @NonNull LocationsHolder locationsHolder) {
		try {
			if (!fm.isStateSaved()) {
				GpxApproximationFragment fragment = new GpxApproximationFragment();
				fragment.setRetainInstance(true);
				fragment.setTargetFragment(targetFragment, REQUEST_CODE);
				fragment.setLocationsHolder(locationsHolder);
				fm.beginTransaction()
						.replace(R.id.fragmentContainer, fragment, TAG)
						.addToBackStack(TAG)
						.commitAllowingStateLoss();
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public void dismissImmediate() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			try {
				mapActivity.getSupportFragmentManager().popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				LOG.error(e);
			}
		}
	}

	public void calculateGpxApproximation() {
		if (gpxApproximator != null) {
			try {
				gpxApproximator.setMode(snapToRoadAppMode);
				gpxApproximator.setPointApproximation(distanceThreshold);
				approximateGpx();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void onSliderChange(int sliderValue) {
		if (distanceThreshold != sliderValue) {
			distanceThreshold = sliderValue;
			calculateGpxApproximation();
		}
	}

	@Override
	public void onProfileSelect(ApplicationMode applicationMode) {
		if (snapToRoadAppMode != applicationMode) {
			snapToRoadAppMode = applicationMode;
			calculateGpxApproximation();
		}
	}

	public LocationsHolder getLocationsHolder() {
		return locationsHolder;
	}

	public void setLocationsHolder(LocationsHolder locationsHolder) {
		this.locationsHolder = locationsHolder;
	}

	public void startProgress() {
		if (progressBar != null) {
			progressBar.setProgress(0);
			progressBar.setVisibility(View.VISIBLE);
		}
	}

	public void finishProgress() {
		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}
	}

	public void updateProgress(int progress) {
		if (progressBar != null) {
			if (progressBar.getVisibility() != View.VISIBLE) {
				progressBar.setVisibility(View.VISIBLE);
			}
			progressBar.setProgress(progress);
		}
	}

	private void approximateGpx() {
		if (gpxApproximator != null) {
			setApplyButtonEnabled(false);
			gpxApproximator.calculateGpxApproximation(new ResultMatcher<GpxRouteApproximation>() {
				@Override
				public boolean publish(final GpxRouteApproximation gpxApproximation) {
					OsmandApplication app = getMyApplication();
					if (app != null) {
						app.runInUIThread(new Runnable() {
							@Override
							public void run() {
								Fragment fragment = getTargetFragment();
								if (fragment instanceof GpxApproximationFragmentListener) {
									((GpxApproximationFragmentListener) fragment).onGpxApproximationDone(gpxApproximation, gpxApproximator.getMode());
								}
								setApplyButtonEnabled(gpxApproximation != null);
							}
						});
						return true;
					}
					return false;
				}

				@Override
				public boolean isCancelled() {
					return false;
				}
			});
		}
	}

	public interface GpxApproximationFragmentListener {

		void onGpxApproximationDone(GpxRouteApproximation gpxApproximation, ApplicationMode mode);

		void onApplyGpxApproximation();

		void onCancelGpxApproximation();
	}
}
