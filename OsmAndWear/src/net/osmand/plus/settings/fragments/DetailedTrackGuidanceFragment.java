package net.osmand.plus.settings.fragments;

import static net.osmand.plus.utils.ColorUtilities.getCardAndListBackgroundColorId;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.TrackApproximationType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.ArrayList;
import java.util.List;

public class DetailedTrackGuidanceFragment extends BaseOsmAndFragment {

	private static final String TAG = DetailedTrackGuidanceFragment.class.getSimpleName();
	private static final String DETAILED_TRACK_GUIDANCE_KEY = "detailed_track_guidance_key";
	private static final String THRESHOLD_DISTANCE_KEY = "threshold_distance_key";
	private static final String SELECTED_APP_MODE_KEY = "selected_app_mode_key";

	private ApplicationMode selectedAppMode;

	private View sliderView;
	private TextView sliderTv;
	private List<View> radioButtons;
	private DialogButton applyButton;

	private TrackApproximationType changedTrackGuidance;
	private int changedThresholdDistance;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupAppMode(savedInstanceState);

		int distance = settings.GPX_APPROXIMATION_DISTANCE.getModeValue(selectedAppMode);
		TrackApproximationType type = settings.DETAILED_TRACK_GUIDANCE.getModeValue(selectedAppMode);
		if (savedInstanceState != null) {
			changedTrackGuidance = TrackApproximationType.values()[savedInstanceState.getInt(DETAILED_TRACK_GUIDANCE_KEY, type.ordinal())];
			changedThresholdDistance = savedInstanceState.getInt(THRESHOLD_DISTANCE_KEY, distance);
		} else {
			changedTrackGuidance = type;
			changedThresholdDistance = distance;
		}
	}

	private void setupAppMode(@Nullable Bundle savedInstanceState) {
		if (selectedAppMode == null && savedInstanceState != null) {
			selectedAppMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(SELECTED_APP_MODE_KEY), null);
		}
		if (selectedAppMode == null) {
			selectedAppMode = settings.getApplicationMode();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.detailed_track_guidance, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		AppBarLayout appBarLayout = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appBarLayout, 5.0f);

		setupToolbar(view);
		setupSlider(view);
		setupContent(view);
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(DETAILED_TRACK_GUIDANCE_KEY, changedTrackGuidance.ordinal());
		outState.putInt(THRESHOLD_DISTANCE_KEY, changedThresholdDistance);
		outState.putString(SELECTED_APP_MODE_KEY, selectedAppMode.getStringKey());
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.findViewById(R.id.toolbar_subtitle).setVisibility(View.GONE);
		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.detailed_track_guidance);

		ImageView navigationIcon = toolbar.findViewById(R.id.close_button);
		navigationIcon.setOnClickListener(iconView -> dismiss());
	}

	private void setupApplyButton(@NonNull View view) {
		AndroidUtils.setBackground(getContext(), view.findViewById(R.id.apply_button), getCardAndListBackgroundColorId(nightMode));
		view.findViewById(R.id.dismiss_button).setVisibility(View.GONE);
		view.findViewById(R.id.buttons_divider).setVisibility(View.GONE);
		applyButton = view.findViewById(R.id.right_bottom_button);
		applyButton.setVisibility(View.VISIBLE);
		applyButton.setButtonType(DialogButtonType.PRIMARY);
		applyButton.setTitleId(R.string.shared_string_apply);

		applyButton.setOnClickListener(v -> {
			if (isParametersChanged()) {
				settings.DETAILED_TRACK_GUIDANCE.setModeValue(selectedAppMode, changedTrackGuidance);
				settings.GPX_APPROXIMATION_DISTANCE.setModeValue(selectedAppMode, changedThresholdDistance);
				Fragment fragment = getTargetFragment();
				if (fragment instanceof NavigationFragment) {
					((NavigationFragment) fragment).showTrackGuidancePref();
				}
				dismiss();
			}
		});
	}

	private void setupContent(@NonNull View view) {
		ImageView imageView = view.findViewById(R.id.descriptionImage);
		imageView.setImageResource(nightMode ? R.drawable.img_detailed_track_guidance_dark : R.drawable.img_detailed_track_guidance);

		setupApplyButton(view);
		setupRadioButtons(view);
		updateContent();
	}

	private void setupRadioButtons(@NonNull View view) {
		radioButtons = new ArrayList<>();
		LinearLayout buttonsContainer = view.findViewById(R.id.buttons_container);

		for (int i = 0; i < TrackApproximationType.values().length; i++) {
			TrackApproximationType type = TrackApproximationType.values()[i];
			View button = themedInflater.inflate(R.layout.bottom_sheet_item_with_descr_and_left_radio_btn, buttonsContainer, false);
			boolean isSelected = changedTrackGuidance == type;
			boolean shouldShowDivider = i != TrackApproximationType.values().length - 1;
			setupRadioButton(button, type.getNameRes(), isSelected, shouldShowDivider, v -> {
				changedTrackGuidance = type;
				updateContent();
			});
			button.setTag(type);
			buttonsContainer.addView(button);
			radioButtons.add(button);
		}
	}

	private final Slider.OnChangeListener sliderChangeListener = new Slider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
			if (fromUser) {
				changedThresholdDistance = (int) value;
				updateSlider();
				updateApplyButton();
			}
		}
	};

	private void setupSlider(View view) {
		sliderView = view.findViewById(R.id.distance_slider);
		Slider slider = sliderView.findViewById(R.id.slider);
		sliderTv = view.findViewById(R.id.value);

		slider.addOnChangeListener(sliderChangeListener);
		slider.setValueTo(100);
		slider.setValueFrom(0);
		slider.setValue(changedThresholdDistance);
		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, profileColor);
		AndroidUiHelper.updateVisibility(sliderView.findViewById(R.id.divider), false);

		updateSlider();
	}

	private void updateSlider() {
		String text = app.getString(R.string.ltr_or_rtl_combine_via_space, String.valueOf(changedThresholdDistance), app.getString(R.string.m));
		sliderTv.setText(text);
	}

	private void updateContent() {
		for (View button : radioButtons) {
			TrackApproximationType approximation = (TrackApproximationType) button.getTag();
			RadioButton radioButton = button.findViewById(R.id.compound_button);
			radioButton.setChecked(approximation == changedTrackGuidance);
		}

		sliderView.setVisibility(changedTrackGuidance == TrackApproximationType.AUTOMATIC ? View.VISIBLE : View.INVISIBLE);
		updateApplyButton();
	}

	private void updateApplyButton() {
		applyButton.setEnabled(isParametersChanged());
	}

	private boolean isParametersChanged() {
		return settings.GPX_APPROXIMATION_DISTANCE.getModeValue(selectedAppMode) != changedThresholdDistance
				|| settings.DETAILED_TRACK_GUIDANCE.getModeValue(selectedAppMode) != changedTrackGuidance;
	}

	private void setupRadioButton(@NonNull View button, @StringRes int titleId, boolean selected, boolean showDivider, @NonNull View.OnClickListener listener) {
		TextView title = button.findViewById(R.id.title);
		title.setText(titleId);

		RadioButton radioButton = button.findViewById(R.id.compound_button);
		radioButton.setChecked(selected);

		AndroidUiHelper.updateVisibility(button.findViewById(R.id.description), false);
		AndroidUiHelper.updateVisibility(button.findViewById(R.id.divider_bottom), showDivider);
		button.setOnClickListener(listener);
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	@Override
	public void onResume() {
		super.onResume();
		getMapActivity().disableDrawer();
	}

	@Override
	public void onPause() {
		super.onPause();
		getMapActivity().enableDrawer();
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull ApplicationMode mode, NavigationFragment navigationFragment) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DetailedTrackGuidanceFragment fragment = new DetailedTrackGuidanceFragment();
			fragment.selectedAppMode = mode;
			fragment.setTargetFragment(navigationFragment, 0);
			manager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
