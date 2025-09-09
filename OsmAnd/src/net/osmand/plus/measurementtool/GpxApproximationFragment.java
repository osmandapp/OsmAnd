package net.osmand.plus.measurementtool;

import static net.osmand.plus.measurementtool.ProfileCard.ProfileCardListener;
import static net.osmand.plus.measurementtool.SliderCard.SliderCardListener;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.GpxApproximator;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.router.GpxRouteApproximation;

import org.apache.commons.logging.Log;

import java.util.List;

public class GpxApproximationFragment extends ContextMenuScrollFragment implements SliderCardListener, ProfileCardListener, GpxApproximationListener {

	private static final Log LOG = PlatformUtil.getLog(GpxApproximationFragment.class);

	public static final String TAG = GpxApproximationFragment.class.getName();

	public static final int REQUEST_CODE = 1100;

	@NonNull
	private final GpxApproximationHelper helper;
	private boolean applyApproximation;

	private ProgressBar pbProgress;
	private DialogButton btnCancel;
	private DialogButton btnApply;
	private SliderCard sliderCard;
	private int menuTitleHeight;

	public GpxApproximationFragment(@NonNull OsmandApplication app,
	                                @NonNull GpxApproximationParams params) {
		helper = new GpxApproximationHelper(app, params);
		helper.setListener(this);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				dismissImmediate();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View mainView = super.onCreateView(inflater, container, savedInstanceState);
		if (mainView == null || !helper.canApproximate()) {
			return null;
		}
		btnApply = mainView.findViewById(R.id.right_bottom_button);
		btnCancel = mainView.findViewById(R.id.dismiss_button);
		if (isPortrait()) {
			updateCardsLayout();
		}
		updateCards();
		updateButtons(mainView);

		pbProgress = mainView.findViewById(R.id.progress_bar);
		if (pbProgress != null) {
			requireMapActivity().setupRouteCalculationProgressBar(pbProgress);
			pbProgress.setIndeterminate(false);
		}

		if (!isPortrait()) {
			int widthNoShadow = getLandscapeNoShadowWidth();
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.BOTTOM | Gravity.START;
			mainView.findViewById(R.id.control_buttons).setLayoutParams(params);
		}
		runLayoutListener();
		helper.calculateGpxApproximationAsync();

		ScrollView profileView = (ScrollView) getBottomScrollView();
		profileView.postDelayed(() -> {
			View view = profileView.findViewWithTag(helper.getModeKey());
			if (view != null) {
				int headerHeight = getResources().getDimensionPixelSize(R.dimen.measurement_tool_button_height);
				profileView.scrollTo(0, view.getTop() + headerHeight);
			}
		}, 100);

		refreshControlsButtons();
		return mainView;
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		int sliderHeight = sliderCard != null ? sliderCard.getViewHeight() : 0;
		int controlButtonsHeight = view.findViewById(R.id.control_buttons).getHeight();
		int buttonsShadowHeight = view.findViewById(R.id.buttons_shadow).getHeight();
		menuTitleHeight = controlButtonsHeight - buttonsShadowHeight + sliderHeight;
		super.calculateLayout(view, initLayout);
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (bottomContainer == null) {
				return;
			}
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackground(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(),
						R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				int listBgColor = ColorUtilities.getListBgColorId(isNightMode());
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, listBgColor);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, listBgColor);
			}
		}
	}

	private void updateButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.bottom_buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.bg_color));
		btnApply.setButtonType(DialogButtonType.PRIMARY);
		btnApply.setTitleId(R.string.shared_string_apply);
		btnApply.setOnClickListener(v -> {
			Fragment fragment = getTargetFragment();
			if (fragment instanceof GpxApproximationFragmentListener) {
				((GpxApproximationFragmentListener) fragment).onApplyGpxApproximation();
			}
			applyApproximation = true;
			dismiss();
		});
		btnCancel.setButtonType(DialogButtonType.SECONDARY);
		btnCancel.setTitleId(R.string.shared_string_cancel);
		btnCancel.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		AndroidUiHelper.updateVisibility(btnApply, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ViewGroup cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();

			if (getTopView() != null) {
				sliderCard = new SliderCard(mapActivity, helper.getDistanceThreshold());
				sliderCard.setListener(this);
				getTopView().addView(sliderCard.build(mapActivity));
			}

			ProfileCard profileCard = new ProfileCard(mapActivity, helper.getAppMode());
			profileCard.setListener(this);
			cardsContainer.addView(profileCard.build(mapActivity));
		}
	}

	@Override
	public void onSliderChange(int sliderValue) {
		helper.setDistanceThreshold(sliderValue, true);
	}

	@Override
	public void onProfileSelect(ApplicationMode applicationMode) {
		helper.setAppMode(applicationMode, true);
	}

	@Override
	public void onNewApproximation() {
		startProgress();
	}

	@Override
	public void onApproximationStarted() {
		setApplyButtonEnabled(false);
	}

	@Override
	public void updateApproximationProgress(@NonNull GpxApproximator approximator, int progress) {
		if (isResumed() && helper.isSameApproximator(approximator)) {
			updateProgress(progress);
		}
	}

	@Override
	public void processApproximationResults(@NonNull List<GpxRouteApproximation> approximations,
	                                        @NonNull List<List<WptPt>> points) {
		finishProgress();
		Fragment fragment = getTargetFragment();
		if (fragment instanceof GpxApproximationFragmentListener) {
			((GpxApproximationFragmentListener) fragment)
					.onGpxApproximationDone(approximations, points, helper.getAppMode());
		}
		setApplyButtonEnabled(!approximations.isEmpty());
	}

	private void setApplyButtonEnabled(boolean enabled) {
		if (btnApply != null) {
			btnApply.setEnabled(enabled);
		}
	}

	public void startProgress() {
		if (pbProgress != null) {
			pbProgress.setProgress(0);
			pbProgress.setVisibility(View.VISIBLE);
		}
	}

	public void updateProgress(int progress) {
		if (pbProgress != null) {
			if (pbProgress.getVisibility() != View.VISIBLE) {
				pbProgress.setVisibility(View.VISIBLE);
			}
			pbProgress.setProgress(progress);
		}
	}

	public void finishProgress() {
		if (pbProgress != null) {
			pbProgress.setVisibility(View.GONE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!helper.canApproximate()) {
			dismiss();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		helper.cancelApproximationIfPossible();
		if (!applyApproximation) {
			Fragment fragment = getTargetFragment();
			if (fragment instanceof GpxApproximationFragmentListener) {
				((GpxApproximationFragmentListener) fragment).onCancelGpxApproximation();
			}
		}
		refreshControlsButtons();
	}

	private void refreshControlsButtons() {
		app.getOsmandMap().getMapLayers().getMapControlsLayer().refreshButtons();
	}

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

	@Override
	protected boolean isHideable() {
		return false;
	}

	@Override
	public boolean shouldShowMapControls(int menuState) {
		return (menuState & (MenuState.HEADER_ONLY | MenuState.HALF_SCREEN)) != 0;
	}

	@Override
	protected String getThemeInfoProviderTag() {
		return TAG;
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

	public static void showInstance(@NonNull OsmandApplication app,
	                                @NonNull FragmentManager fragmentManager,
	                                @Nullable Fragment targetFragment,
	                                @NonNull GpxApproximationParams params) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			GpxApproximationFragment fragment = new GpxApproximationFragment(app, params);
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(targetFragment, REQUEST_CODE);
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
