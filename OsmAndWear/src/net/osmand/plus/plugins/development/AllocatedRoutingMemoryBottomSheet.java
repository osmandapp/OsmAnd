package net.osmand.plus.plugins.development;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

public class AllocatedRoutingMemoryBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = AllocatedRoutingMemoryBottomSheet.class.getSimpleName();

	private static final int BYTES_IN_MB = 1024 * 1024;

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
		preference = app.getSettings().MEMORY_ALLOCATED_FOR_ROUTING;
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

		String title = getString(R.string.memory_allocated_for_routing);
		items.add(new TitleItem(title));

		String description = getString(R.string.memory_allocated_for_routing_ds);
		items.add(new LongDescriptionItem(description));

		View view = inflater.inflate(R.layout.bottom_sheet_allocated_routing_memory, null);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());

		sliderContainer = view.findViewById(R.id.slider_container);
		setupSliderView(sliderContainer);

		View buttonView = view.findViewById(R.id.button_container);
		setupResetButton(buttonView);
	}

	private void setupSliderView(View container) {
		TextView title = container.findViewById(R.id.title);
		TextView summary = container.findViewById(R.id.summary);
		TextView from = container.findViewById(R.id.from_value);
		TextView to = container.findViewById(R.id.to_value);

		title.setText(getString(R.string.shared_string_memory));
		summary.setText(getFormattedMb(currentValue));
		from.setText(getFormattedMb(minValue));
		to.setText(getFormattedMb(maxValue));

		Slider slider = container.findViewById(R.id.slider);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		UiUtilities.setupSlider(slider, nightMode, activeColor, true);
		slider.setValueFrom(0);
		slider.setValueTo(range.length - 1);
		slider.setStepSize(1);
		slider.setValue(getRangeIndex(currentValue));

		slider.addOnChangeListener((slider1, value, fromUser) -> {
			if (fromUser) {
				currentValue = range[(int) slider1.getValue()];
				summary.setText(getFormattedMb(currentValue));
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

	private void updateSliderView(View container) {
		TextView summary = container.findViewById(R.id.summary);
		summary.setText(getFormattedMb(currentValue));

		Slider slider = container.findViewById(R.id.slider);
		slider.setValue(getRangeIndex(currentValue));
	}

	private String getFormattedMb(int value) {
		return getString(R.string.ltr_or_rtl_combine_via_space, String.valueOf(value), "MB");
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (isChanged()) {
			Fragment target = getTargetFragment();
			if (target instanceof BaseSettingsFragment) {
				BaseSettingsFragment fragment = (BaseSettingsFragment) target;
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
		int maxLimit = (int) getMaxLimit();
		// start from 2^6 = 64
		for (int i = 6; true; i++) {
			int res = (int) Math.pow(2, i);
			if (res < maxLimit) {
				powRange.add(res);
			} else {
				break;
			}
		}
		return powRange;
	}

	private float getMaxLimit() {
		Activity activity = requireActivity();
		MemoryInfo mi = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) activity.getSystemService(ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		double availableMb = (double) mi.totalMem / BYTES_IN_MB;
		return (float) availableMb / 2;
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

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull String key,
	                                @NonNull Fragment target,
	                                @Nullable ApplicationMode appMode) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);
			AllocatedRoutingMemoryBottomSheet fragment = new AllocatedRoutingMemoryBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(false);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

}
