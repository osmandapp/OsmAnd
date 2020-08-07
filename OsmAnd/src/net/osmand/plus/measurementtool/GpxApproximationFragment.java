package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.apache.commons.logging.Log;

import static net.osmand.plus.measurementtool.ProfileCard.*;
import static net.osmand.plus.measurementtool.SliderCard.*;

public class GpxApproximationFragment extends ContextMenuScrollFragment
		implements SliderCardListener, ProfileCardListener {

	private static final Log LOG = PlatformUtil.getLog(GpxApproximationFragment.class);
	public static final String TAG = GpxApproximationFragment.class.getSimpleName();
	public static final String DISTANCE_THRESHOLD_KEY = "distance_threshold";
	public static final String SNAP_TO_ROAD_APP_MODE_STRING_KEY = "snap_to_road_app_mode";

	private int menuTitleHeight;
	private ApplicationMode snapToRoadAppMode = ApplicationMode.CAR;
	private int distanceThreshold = 30;

	@Override
	public int getMainLayoutId() {
		return R.layout.fragment_gpx_approximation_bottom_sheet_dialog;
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
		return MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
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
		}

		if (isPortrait()) {
			updateCardsLayout();
		}
		updateCards();
		updateButtons(mainView);

		if (!isPortrait()) {
			int widthNoShadow = getLandscapeNoShadowWidth();
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.BOTTOM | Gravity.START;
			mainView.findViewById(R.id.control_buttons).setLayoutParams(params);
		}
		enterGpxApproximationMode();
		runLayoutListener();

		return mainView;
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = view.findViewById(R.id.control_buttons).getHeight()
				- view.findViewById(R.id.buttons_shadow).getHeight();
		super.calculateLayout(view, initLayout);
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
		exitGpxApproximationMode();
		Fragment fragment = getTargetFragment();
		if (fragment instanceof GpxApproximationFragmentListener) {
			((GpxApproximationFragmentListener) fragment).cancelButtonOnClick();
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
		View applyButton = view.findViewById(R.id.right_bottom_button);
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Fragment fragment = getTargetFragment();
				if (fragment instanceof GpxApproximationFragmentListener) {
					((GpxApproximationFragmentListener) fragment).applyButtonOnClick(snapToRoadAppMode, distanceThreshold);
				}
				dismiss();
			}
		});

		View cancelButton = view.findViewById(R.id.dismiss_button);
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

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();

			SliderCard sliderCard = new SliderCard(mapActivity, distanceThreshold);
			sliderCard.setListener(this);
			cardsContainer.addView(sliderCard.build(mapActivity));

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

	public static void showInstance(FragmentManager fm, Fragment targetFragment) {
		try {
			if (!fm.isStateSaved()) {
				GpxApproximationFragment fragment = new GpxApproximationFragment();
				fragment.setTargetFragment(targetFragment, 0);
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

	@Override
	public void onSliderChange(int sliderValue) {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof GpxApproximationFragmentListener) {
			((GpxApproximationFragmentListener) fragment).onParametersChanged(snapToRoadAppMode, sliderValue);
			distanceThreshold = sliderValue;
		}
	}

	@Override
	public void onProfileSelect(ApplicationMode applicationMode) {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof GpxApproximationFragmentListener) {
			((GpxApproximationFragmentListener) fragment).onParametersChanged(applicationMode, distanceThreshold);
			snapToRoadAppMode = applicationMode;
		}
	}

	public interface GpxApproximationFragmentListener {

		void onParametersChanged(ApplicationMode mode, int distanceThreshold);

		void applyButtonOnClick(ApplicationMode mode, int distanceThreshold);

		void cancelButtonOnClick();
	}
}
