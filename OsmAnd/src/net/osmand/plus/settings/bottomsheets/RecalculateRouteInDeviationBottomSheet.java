package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.settings.fragments.RouteParametersFragment.DEFAULT_MODE;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.DISABLE_MODE;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitmeListDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatterParams;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.settings.enums.MetricsConstants;

public class RecalculateRouteInDeviationBottomSheet extends BooleanPreferenceBottomSheet {

	public static final String TAG = RecalculateRouteInDeviationBottomSheet.class.getSimpleName();

	private static final String CURRENT_VALUE = "current_value";

	private CommonPreference<Float> preference;

	private Slider slider;
	private TextView tvSliderTitle;
	private TextView tvSliderSummary;

	private Float[] entryValues;
	private float currentValue;
	private boolean enabled;
	private boolean sliderPositionChanged;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		preference = settings.ROUTE_RECALCULATION_DISTANCE;
		getPreferenceStateAndValue();

		SwitchPreferenceEx switchPref = (SwitchPreferenceEx) getPreference();
		if (switchPref == null) {
			return;
		}

		if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_VALUE)) {
			currentValue = savedInstanceState.getFloat(CURRENT_VALUE);
		}

		int contentPaddingSmall = getDimensionPixelSize(R.dimen.content_padding_small);
		int contentPadding = getDimensionPixelSize(R.dimen.content_padding);

		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
			entryValues = new Float[]{10.f, 20.0f, 30.0f, 50.0f, 100.0f, 200.0f, 500.0f, 1000.0f, 1500.0f};
		} else {
			entryValues = new Float[]{9.1f, 18.3f, 30.5f, 45.7f, 91.5f, 183.0f, 482.0f, 965.0f, 1609.0f};
		}

		int appModeColor = appMode.getProfileColor(nightMode);
		int activeColor = ColorUtilities.getActiveColorId(nightMode);
		int disabledColor = ColorUtilities.getSecondaryTextColorId(nightMode);

		String title = getString(R.string.recalculate_route_in_deviation);
		items.add(new TitleItem(title));

		View sliderView = inflate(R.layout.bottom_sheet_item_slider_with_two_text);
		slider = sliderView.findViewById(R.id.slider);
		tvSliderTitle = sliderView.findViewById(android.R.id.title);
		tvSliderTitle.setText(getString(R.string.distance));
		tvSliderSummary = sliderView.findViewById(android.R.id.summary);
		slider.setValueFrom(0);
		slider.setValueTo(entryValues.length - 1);
		slider.setStepSize(1);
		updateSliderView();

		String on = getString(R.string.shared_string_enabled);
		String off = getString(R.string.shared_string_disabled);
		Context context = requireContext();
		BottomSheetItemWithCompoundButton[] preferenceBtn = new BottomSheetItemWithCompoundButton[1];
		preferenceBtn[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(enabled)
				.setCompoundButtonColor(appModeColor)
				.setTitle(enabled ? on : off)
				.setTitleColorId(enabled ? activeColor : disabledColor)
				.setCustomView(getCustomButtonView(context, getAppMode(), enabled, nightMode))
				.setOnClickListener(v -> {
					enabled = !enabled;
					sliderPositionChanged = false;
					switchPref.setChecked(enabled);
					preferenceBtn[0].setTitle(enabled ? on : off);
					preferenceBtn[0].setTitleColorId(enabled ? activeColor : disabledColor);
					preferenceBtn[0].setChecked(enabled);
					getDefaultValue();
					updateSliderView();
					updateCustomButtonView(context, getAppMode(), v, enabled, nightMode);
					Fragment target = getTargetFragment();
					float newValue = enabled ? DEFAULT_MODE : DISABLE_MODE;
					if (target instanceof OnConfirmPreferenceChange) {
						((OnConfirmPreferenceChange) target).onConfirmPreferenceChange(switchPref.getKey(), newValue, ApplyQueryType.NONE);
					}
				})
				.create();
		items.add(preferenceBtn[0]);
		items.add(new DividerSpaceItem(app, contentPaddingSmall));
		items.add(new LongDescriptionItem(getString(R.string.select_distance_route_will_recalc)));
		items.add(new DividerSpaceItem(app, contentPadding));

		slider.addOnChangeListener((slider, value, fromUser) -> {
			sliderPositionChanged = true;
			if (fromUser) {
				currentValue = entryValues[(int) slider.getValue()];
				tvSliderSummary.setText(getFormattedDistance(app, currentValue));
			}
		});
		UiUtilities.setupSlider(slider, nightMode, appModeColor, true);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(sliderView).create());
		items.add(new SubtitmeListDividerItem(getContext()));
		items.add(new DividerSpaceItem(app, contentPaddingSmall));
		items.add(new LongDescriptionItem(getString(R.string.recalculate_route_distance_promo)));
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (enabled && sliderPositionChanged) {
			Fragment target = getTargetFragment();
			if (target instanceof OnConfirmPreferenceChange) {
				((OnConfirmPreferenceChange) target).onConfirmPreferenceChange(
						preference.getId(), currentValue, ApplyQueryType.SNACK_BAR);
			}
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat(CURRENT_VALUE, currentValue);
	}

	private void updateSliderView() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int disabledColor = ColorUtilities.getSecondaryTextColor(app, nightMode);
		int textColorPrimary = ColorUtilities.getPrimaryTextColor(app, nightMode);
		tvSliderTitle.setTextColor(enabled ? textColorPrimary : disabledColor);
		tvSliderSummary.setTextColor(enabled ? activeColor : disabledColor);
		tvSliderSummary.setText(getFormattedDistance(app, currentValue));
		slider.setValue(findIndexOfValue(currentValue));
		slider.setEnabled(enabled);
	}

	private void getPreferenceStateAndValue() {
		float allowedValue = preference.getModeValue(appMode);
		if (allowedValue == DISABLE_MODE) {
			enabled = false;
			getDefaultValue();
		} else {
			enabled = true;
			if (allowedValue == DEFAULT_MODE) {
				getDefaultValue();
			} else {
				currentValue = allowedValue;
			}
		}
	}

	private void getDefaultValue() {
		currentValue = RoutingHelper.getDefaultAllowedDeviation(settings, appMode);
	}

	private int findIndexOfValue(float allowedValue) {
		for (int i = 0; i < entryValues.length; i++) {
			float value = entryValues[i];
			if (allowedValue == value) {
				return i;
			} else if (value > allowedValue) {
				return i > 0 ? --i : i;
			}
		}
		return 0;
	}

	@NonNull
	private static String getFormattedDistance(@NonNull OsmandApplication app, float value) {
		return OsmAndFormatter.getFormattedDistance(value, app, OsmAndFormatterParams.NO_TRAILING_ZEROS);
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target,
	                                   boolean usedOnMap, @Nullable ApplicationMode appMode) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			RecalculateRouteInDeviationBottomSheet fragment = new RecalculateRouteInDeviationBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
			return true;
		}
		return false;
	}
}