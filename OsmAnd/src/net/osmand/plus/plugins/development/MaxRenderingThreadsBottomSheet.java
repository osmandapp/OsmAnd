package net.osmand.plus.plugins.development;

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

import net.osmand.core.android.MapRendererView;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class MaxRenderingThreadsBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = MaxRenderingThreadsBottomSheet.class.getSimpleName();

	private Integer[] range;
	private int minValue;
	private int maxValue;
	private int initialValue;
	private int currentValue;
	private int defaultValue;

	private View sliderContainer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		initialValue = settings.MAX_RENDERING_THREADS.get();
		currentValue = initialValue;
		defaultValue = settings.MAX_RENDERING_THREADS.getDefaultValue();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);

		String title = getString(R.string.threads_allocated_for_rendering);
		items.add(new TitleItem(title));

		String description = getString(R.string.threads_allocated_for_rendering_desc);
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

		title.setText(getString(R.string.shared_string_threads));
		summary.setText(getFormattedThreads(currentValue));
		from.setText(getFormattedThreads(minValue));
		to.setText(getFormattedThreads(maxValue));

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
				summary.setText(getFormattedThreads(currentValue));
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
		summary.setText(getFormattedThreads(currentValue));

		Slider slider = container.findViewById(R.id.slider);
		slider.setValue(getRangeIndex(currentValue));
	}

	private String getFormattedThreads(int value) {
		if (value == 0) {
			value = getMaxLimit() / 2;
		}
		return String.valueOf(value);
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (isChanged()) {
			Fragment target = getTargetFragment();
			if (target instanceof BaseSettingsFragment fragment) {
				MapRendererView mapRenderer = getMapRenderer();
				if (mapRenderer != null) {
					if (currentValue > 0) {
						mapRenderer.setResourceWorkerThreadsLimit(currentValue);
					} else {
						mapRenderer.setResourceWorkerThreadsLimit(getMapRenderer().getDefaultWorkerThreadsLimit() / 2);
					}
				}
				fragment.onApplyPreferenceChange(settings.MAX_RENDERING_THREADS.getId(), false, currentValue);
			}
		}
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	private List<Integer> getAvailableRange() {
		List<Integer> range = new ArrayList<>();
		int maxLimit = getMaxLimit();
		for (int i = 1; i <= maxLimit; i++) {
			range.add(i);
		}
		return range;
	}

	private int getMaxLimit() {
		MapRendererView mapRenderer = getMapRenderer();
		return mapRenderer != null ? mapRenderer.getDefaultWorkerThreadsLimit() : 16;
	}

	@Nullable
	private MapRendererView getMapRenderer() {
		return app.getOsmandMap().getMapView().getMapRenderer();
	}

	private int getRangeIndex(float value) {
		for (int i = 0; i < range.length; i++) {
			if (value == range[i]) {
				return i;
			}
		}
		return range.length / 2 - 1;
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
			MaxRenderingThreadsBottomSheet fragment = new MaxRenderingThreadsBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(false);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}