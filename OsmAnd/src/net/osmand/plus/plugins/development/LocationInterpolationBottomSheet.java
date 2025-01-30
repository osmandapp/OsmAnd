package net.osmand.plus.plugins.development;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheetInitializer;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.search.SearchablePreferenceDialog;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LocationInterpolationBottomSheet extends BasePreferenceBottomSheet implements SearchablePreferenceDialog {

	public static final String TAG = LocationInterpolationBottomSheet.class.getSimpleName();

	private OsmandApplication app;

	private Integer[] range;
	private int minValue;
	private int maxValue;
	private int initialValue;
	private int currentValue;
	private int defaultValue;

	private CommonPreference<Integer> preference;

	private View sliderContainer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		preference = app.getSettings().LOCATION_INTERPOLATION_PERCENT;
		initData();
	}

	private void initData() {
		List<Integer> rangeList = getAvailableRange();
		range = new Integer[rangeList.size()];
		rangeList.toArray(range);
		if (range.length > 0) {
			minValue = range[0];
			maxValue = range[range.length - 1];
		}
		initialValue = preference.get();
		currentValue = initialValue;
		defaultValue = preference.getDefaultValue();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);

		items.add(new TitleItem(getTitle()));
		items.add(new LongDescriptionItem(getDescription()));

		View view = inflater.inflate(R.layout.bottom_sheet_allocated_routing_memory, null);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());

		sliderContainer = view.findViewById(R.id.slider_container);
		setupSliderView(sliderContainer);

		View buttonView = view.findViewById(R.id.button_container);
		setupResetButton(buttonView);
	}

	@NonNull
	private String getTitle() {
		return getString(R.string.location_interpolation_percent);
	}

	@NonNull
	private String getDescription() {
		return getString(R.string.location_interpolation_percent_desc);
	}

	@SuppressLint("SetTextI18n")
	private void setupSliderView(View container) {
		TextView title = container.findViewById(R.id.title);
		TextView summary = container.findViewById(R.id.summary);
		TextView from = container.findViewById(R.id.from_value);
		TextView to = container.findViewById(R.id.to_value);

		title.setText(getString(R.string.shared_string_interpolation));
		summary.setText("" + currentValue);
		from.setText("" + minValue);
		to.setText("" + maxValue);

		Slider slider = container.findViewById(R.id.slider);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		UiUtilities.setupSlider(slider, nightMode, activeColor, true);
		slider.setValueFrom(0);
		slider.setValueTo(range.length - 1);
		slider.setStepSize(1);
		slider.setValue(getRangeIndex(currentValue));

		slider.addOnChangeListener((sl, value, fromUser) -> {
			if (fromUser) {
				currentValue = range[(int) sl.getValue()];
				summary.setText("" + currentValue);
			}
		});
	}

	private void setupResetButton(View container) {
		ImageView icon = container.findViewById(R.id.button_icon);
		TextView text = container.findViewById(R.id.button_text);
		icon.setImageResource(R.drawable.ic_action_reset);
		text.setText(getString(R.string.reset_to_default));
		text.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(container, drawable);
		container.setOnClickListener(view -> onResetToDefault());
	}

	private void onResetToDefault() {
		currentValue = defaultValue;
		updateSliderView(sliderContainer);
	}

	@SuppressLint("SetTextI18n")
	private void updateSliderView(View container) {
		TextView summary = container.findViewById(R.id.summary);
		summary.setText("" + currentValue);

		Slider slider = container.findViewById(R.id.slider);
		slider.setValue(getRangeIndex(currentValue));
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (isChanged()) {
			Fragment target = getTargetFragment();
			if (target instanceof BaseSettingsFragment fragment) {
				fragment.onApplyPreferenceChange(preference.getId(), false, currentValue);
			}
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	private List<Integer> getAvailableRange() {
		List<Integer> powRange = new ArrayList<>();
		for (int i = 0; i <= 100; i += 10) {
			powRange.add(i);
		}
		return powRange;
	}

	private int getRangeIndex(float value) {
		for (int i = 0; i < range.length; i++) {
			if (value == range[i]) {
				return i;
			}
		}
		return range.length - 1;
	}

	private boolean isChanged() {
		return initialValue != currentValue;
	}

	public static LocationInterpolationBottomSheet createInstance(final Preference preference,
																  final Optional<Fragment> target,
																  final ApplicationMode appMode) {
		return BasePreferenceBottomSheetInitializer
				.initialize(new LocationInterpolationBottomSheet())
				.with(Optional.of(preference), appMode, false, target);
	}

	@Override
	public void show(final FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			show(fragmentManager, TAG);
		}
	}

	@Override
	public String getSearchableInfo() {
		return String.join(", ", getTitle(), getDescription());
	}
}