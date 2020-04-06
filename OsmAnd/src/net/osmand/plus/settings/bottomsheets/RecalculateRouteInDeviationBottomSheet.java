package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitmeListDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.ApplyQueryType;
import net.osmand.plus.settings.OnApplyPreference;
import net.osmand.plus.settings.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import static net.osmand.plus.settings.RouteParametersFragment.DEFAULT_MODE;
import static net.osmand.plus.settings.RouteParametersFragment.DISABLE_MODE;

public class RecalculateRouteInDeviationBottomSheet extends BooleanPreferenceBottomSheet {

	public static final String TAG = RecalculateRouteInDeviationBottomSheet.class.getSimpleName();

	private static final String CURRENT_VALUE = "current_value";

	private OsmandApplication app;
	private OsmandSettings settings;
	private ApplicationMode appMode;
	private OsmandSettings.CommonPreference<Float> preference;

	private Slider slider;
	private TextView tvSliderTitle;
	private TextView tvSliderSummary;

	private Float[] entryValues;
	private float currentValue;
	private boolean enabled = false;
	private boolean sliderPositionChanged = false;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		settings = app.getSettings();
		appMode = getAppMode();
		preference = settings.ROUTE_RECALCULATION_DISTANCE;
		getPreferenceStateAndValue();

		final SwitchPreferenceEx switchPref = (SwitchPreferenceEx) getPreference();
		if (switchPref == null) {
			return;
		}

		if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_VALUE)) {
			currentValue = savedInstanceState.getFloat(CURRENT_VALUE);
		}

		int contentPaddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		int dialogContentMargin = app.getResources().getDimensionPixelSize(R.dimen.dialog_content_margin);

		OsmandSettings.MetricsConstants mc = settings.METRIC_SYSTEM.get();
		if (mc == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			entryValues = new Float[]{10.f, 20.0f, 30.0f, 50.0f, 100.0f, 200.0f, 500.0f, 1000.0f, 1500.0f};
		} else {
			entryValues = new Float[]{9.1f, 18.3f, 30.5f, 45.7f, 91.5f, 183.0f, 482.0f, 965.0f, 1609.0f};
		}

		final int appModeColor = appMode.getIconColorInfo().getColor(nightMode);
		final int activeColor = AndroidUtils.resolveAttribute(app, R.attr.active_color_basic);
		final int disabledColor = AndroidUtils.resolveAttribute(app, android.R.attr.textColorSecondary);

		String title = getString(R.string.recalculate_route_in_deviation);
		items.add(new TitleItem(title));

		final View sliderView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.bottom_sheet_item_slider_with_two_text, null);
		slider = sliderView.findViewById(R.id.slider);
		tvSliderTitle = sliderView.findViewById(android.R.id.title);
		tvSliderTitle.setText(getString(R.string.distance));
		tvSliderSummary = sliderView.findViewById(android.R.id.summary);
		slider.setValueFrom(0);
		slider.setValueTo(entryValues.length - 1);
		slider.setStepSize(1);
		updateSliderView();

		final String on = getString(R.string.shared_string_enabled);
		final String off = getString(R.string.shared_string_disabled);
		final BottomSheetItemWithCompoundButton[] preferenceBtn = new BottomSheetItemWithCompoundButton[1];
		preferenceBtn[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(enabled)
				.setCompoundButtonColorId(appModeColor)
				.setTitle(enabled ? on : off)
				.setTitleColorId(enabled ? activeColor : disabledColor)
				.setCustomView(getCustomButtonView(enabled))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						enabled = !enabled;
						sliderPositionChanged = false;
						switchPref.setChecked(enabled);
						preferenceBtn[0].setTitle(enabled ? on : off);
						preferenceBtn[0].setTitleColorId(enabled ? activeColor : disabledColor);
						preferenceBtn[0].setChecked(enabled);
						getDefaultValue();
						updateSliderView();
						updateCustomButtonView(v, enabled);
						Fragment target = getTargetFragment();
						float newValue = enabled ? DEFAULT_MODE : DISABLE_MODE;
						if (target instanceof OnApplyPreference) {
							((OnApplyPreference) target).onApplyPreference(switchPref.getKey(), newValue, ApplyQueryType.NONE);
						}
						if (target instanceof OnPreferenceChanged) {
							((OnPreferenceChanged) target).onPreferenceChanged(switchPref.getKey());
						}
					}
				})
				.create();
		items.add(preferenceBtn[0]);
		items.add(new DividerSpaceItem(app, contentPaddingSmall));
		items.add(new LongDescriptionItem(getString(R.string.select_distance_route_will_recalc)));
		items.add(new DividerSpaceItem(app, dialogContentMargin));

		slider.addOnChangeListener(new Slider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
				sliderPositionChanged = true;
				if (fromUser) {
					currentValue = entryValues[(int) slider.getValue()];
					tvSliderSummary.setText(getFormattedDistance(app, currentValue));
				}
			}
		});
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(sliderView)
				.create());
		items.add(new SubtitmeListDividerItem(getContext()));
		items.add(new DividerSpaceItem(app, contentPaddingSmall));
		items.add(new LongDescriptionItem(getString(R.string.recalculate_route_distance_promo)));
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (enabled && sliderPositionChanged) {
			Fragment target = getTargetFragment();
			if (target instanceof OnApplyPreference) {
				((OnApplyPreference) target).onApplyPreference(preference.getId(), currentValue, ApplyQueryType.SNACK_BAR);
			}
			if (target instanceof OnPreferenceChanged) {
				((OnPreferenceChanged) target).onPreferenceChanged(preference.getId());
			}
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putFloat(CURRENT_VALUE, currentValue);
	}

	private void updateSliderView() {
		int activeColor = AndroidUtils.resolveAttribute(app, R.attr.active_color_basic);
		int disabledColor = AndroidUtils.resolveAttribute(app, android.R.attr.textColorSecondary);
		int textColorPrimary = AndroidUtils.resolveAttribute(app, android.R.attr.textColorPrimary);
		tvSliderTitle.setTextColor(ContextCompat.getColor(app, enabled ? textColorPrimary : disabledColor));
		tvSliderSummary.setTextColor(ContextCompat.getColor(app, enabled ? activeColor : disabledColor));
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
		currentValue = RoutingHelper.getDefaultAllowedDeviation(settings, appMode,
				RoutingHelper.getPosTolerance(0));
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

	private static String getFormattedDistance(@NonNull OsmandApplication app, float value) {
		return OsmAndFormatter.getFormattedDistance(value, app, false);
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target,
	                                   boolean usedOnMap, @Nullable ApplicationMode appMode) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			RecalculateRouteInDeviationBottomSheet fragment = new RecalculateRouteInDeviationBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}