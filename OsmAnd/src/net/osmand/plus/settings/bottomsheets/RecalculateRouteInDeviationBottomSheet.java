package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitmeListDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class RecalculateRouteInDeviationBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = RecalculateRouteInDeviationBottomSheet.class.getSimpleName();

	private static final int INVALID_INDEX = -1;
	private static final int INVALID_VALUE = -1;

	private OsmandSettings.CommonPreference<Float> routeRecalculationDistanceValue;
	private OsmandSettings.CommonPreference<Boolean> routeRecalculationDistanceActivation;
	private Float[] entryValues;
	private float selectedValue = INVALID_VALUE;
	private boolean selectedValueChanged = false;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		int contentPadding = (int) getResources().getDimension(R.dimen.content_padding);
		int contentPaddingSmall = (int) getResources().getDimension(R.dimen.content_padding_small);
		int contentPaddingHalf = (int) getResources().getDimension(R.dimen.content_padding_half);

		final OsmandApplication app = requiredMyApplication();
		final OsmandSettings settings = app.getSettings();
		final ApplicationMode appMode = getAppMode();

		routeRecalculationDistanceValue = settings.ROUTE_RECALCULATION_DISTANCE_VALUE;
		routeRecalculationDistanceActivation = settings.ROUTE_RECALCULATION_DISTANCE_ACTIVATION;

		OsmandSettings.MetricsConstants mc = settings.METRIC_SYSTEM.get();
		if (mc == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			entryValues = new Float[]{10.f, 20.0f, 30.0f, 50.0f, 100.0f, 200.0f, 500.0f, 1000.0f, 1500.0f};
		} else {
			entryValues = new Float[]{9.1f, 18.3f, 30.5f, 45.7f, 91.5f, 183.0f, 482.0f, 965.0f, 1609.0f};
		}

		final SwitchPreferenceEx switchPreference = (SwitchPreferenceEx) getPreference();
		if (switchPreference == null) {
			return;
		}
		OsmandSettings.OsmandPreference preference = app.getSettings().getPreference(switchPreference.getKey());
		if (!(preference instanceof OsmandSettings.BooleanPreference)) {
			return;
		}

		String title = getString(R.string.recalculate_route_in_deviation);
		items.add(new TitleItem(title));

		boolean checked = routeRecalculationDistanceActivation.getModeValue(appMode);
		final OsmandSettings.BooleanPreference pref = (OsmandSettings.BooleanPreference) preference;
		final String on = getString(R.string.shared_string_enabled);
		final String off = getString(R.string.shared_string_disabled);
		final int appModeColor = appMode.getIconColorInfo().getColor(nightMode);
		final int activeColor = AndroidUtils.resolveAttribute(app, R.attr.active_color_basic);
		final int disabledColor = AndroidUtils.resolveAttribute(app, android.R.attr.textColorSecondary);
		final BottomSheetItemWithCompoundButton[] preferenceBtn = new BottomSheetItemWithCompoundButton[1];
		preferenceBtn[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(checked)
				.setCompoundButtonColorId(appModeColor)
				.setTitle(checked ? on : off)
				.setTitleColorId(checked ? activeColor : disabledColor)
				.setCustomView(getCustomCompoundButtonView(checked))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean newValue = !pref.getModeValue(appMode);
						if (switchPreference.callChangeListener(newValue)) {
							switchPreference.setChecked(newValue);
							preferenceBtn[0].setTitle(newValue ? on : off);
							preferenceBtn[0].setChecked(newValue);
							preferenceBtn[0].setTitleColorId(newValue ? activeColor : disabledColor);
							updateCustomButtonView(v, newValue);

							Fragment target = getTargetFragment();
							if (target instanceof OnPreferenceChanged) {
								((OnPreferenceChanged) target).onPreferenceChanged(switchPreference.getKey());
							}
						}
					}
				})
				.create();
		items.add(preferenceBtn[0]);
		items.add(createSpaceItem(app, contentPaddingSmall));
		items.add(new LongDescriptionItem(getString(R.string.select_distance_route_will_recalc)));
		items.add(createSpaceItem(app, contentPadding));

		View sliderView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.bottom_sheet_item_slider, null);
		TextView tvSliderTitle = sliderView.findViewById(android.R.id.title);
		final TextView tvSliderSummary = sliderView.findViewById(android.R.id.summary);
		final Slider slider = sliderView.findViewById(R.id.slider);
		tvSliderTitle.setText(getString(R.string.distance));
		slider.setValueFrom(0);
		slider.setValueTo(entryValues.length - 1);
		slider.setStepSize(1);
		float allowedDistance = routeRecalculationDistanceValue.getModeValue(appMode);
		float defaultDistance = RoutingHelper.getDefaultAllowedDeviation(
				settings, appMode, RoutingHelper.getPosTolerance(0));
		int selectedValueIndex;
		if (allowedDistance == 0) {
			selectedValueIndex = findIndexOfValue(defaultDistance);
		} else {
			selectedValueIndex = findIndexOfValue(allowedDistance);
			if (selectedValueIndex == INVALID_INDEX) {
				selectedValueIndex = findIndexOfValue(defaultDistance);
			}
		}
		if (selectedValueIndex == INVALID_INDEX) {
			selectedValueIndex = 0;
		}
		slider.setValue(selectedValueIndex);
		selectedValue = entryValues[selectedValueIndex];
		tvSliderSummary.setText(getFormattedDistance(app, selectedValue));
		slider.addOnChangeListener(new Slider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
				selectedValueChanged = true;
				selectedValue = entryValues[(int) slider.getValue()];
				tvSliderSummary.setText(getFormattedDistance(app, selectedValue));
			}
		});
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(sliderView)
				.create());
		items.add(new SubtitmeListDividerItem(getContext()));
		items.add(createSpaceItem(app, contentPaddingSmall));

		items.add(new LongDescriptionItem(getString(R.string.recalculate_route_distance_promo)));
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (selectedValueChanged) {
			routeRecalculationDistanceValue.setModeValue(getAppMode(), selectedValue);
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	private static BaseBottomSheetItem createSpaceItem(@NonNull Context ctx, int verticalSpaceDp) {
		View view = new View(ctx);
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, verticalSpaceDp);
		view.setLayoutParams(params);
		return new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
	}

	private View getCustomCompoundButtonView(boolean checked) {
		View customView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.bottom_sheet_item_preference_switch, null);
		updateCustomButtonView(customView, checked);

		return customView;
	}

	private void updateCustomButtonView(View customView, boolean checked) {
		OsmandApplication app = requiredMyApplication();
		ApplicationMode appMode = getAppMode();
		View buttonView = customView.findViewById(R.id.button_container);

		int colorRes = appMode.getIconColorInfo().getColor(nightMode);
		int color = checked ? getResolvedColor(colorRes) : AndroidUtils.getColorFromAttr(app, R.attr.divider_color_basic);
		int bgColor = UiUtilities.getColorWithAlpha(color, checked ? 0.1f : 0.5f);
		int selectedColor = UiUtilities.getColorWithAlpha(color, checked ? 0.3f : 0.5f);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			int bgResId = R.drawable.rectangle_rounded_right;
			int selectableResId = R.drawable.ripple_rectangle_rounded_right;

			Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
			Drawable selectable = app.getUIUtilities().getPaintedIcon(selectableResId, selectedColor);
			Drawable[] layers = {bgDrawable, selectable};
			AndroidUtils.setBackground(buttonView, new LayerDrawable(layers));
		} else {
			int bgResId = R.drawable.rectangle_rounded_right;
			Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
			AndroidUtils.setBackground(buttonView, bgDrawable);
		}
	}

	private int findIndexOfValue(Float value) {
		for (int i = 0; i < entryValues.length; i++) {
			if (value.equals(entryValues[i])) {
				return i;
			}
		}
		return -1;
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
