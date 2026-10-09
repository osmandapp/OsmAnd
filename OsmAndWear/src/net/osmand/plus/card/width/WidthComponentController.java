package net.osmand.plus.card.width;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.ksoichiro.android.observablescrollview.ScrollUtils;

import net.osmand.plus.R;
import net.osmand.plus.card.base.slider.ISliderCard;
import net.osmand.plus.card.base.slider.moded.IModedSliderComponent;
import net.osmand.plus.card.base.slider.moded.IModedSliderController;
import net.osmand.plus.card.base.slider.moded.data.SliderMode;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WidthComponentController implements IModedSliderController {

	private final WidthComponentListener listener;

	private IModedSliderComponent cardInstance;
	private OnNeedScrollListener onNeedScrollListener;
	private WidthMode widthMode;
	private int customValue;

	public WidthComponentController(@NonNull WidthMode widthMode, int customValue,
	                                @NonNull WidthComponentListener listener) {
		this.listener = listener;
		this.widthMode = widthMode;
		this.customValue = customValue;
	}

	@Override
	public void bindComponent(@NonNull ISliderCard cardInstance) {
		this.cardInstance = (IModedSliderComponent) cardInstance;
	}

	public void setOnNeedScrollListener(@NonNull OnNeedScrollListener onNeedScrollListener) {
		this.onNeedScrollListener = onNeedScrollListener;
	}

	@Override
	public void onChangeSliderValue(float newValue) {
		customValue = (int) newValue;
		notifyWidthSelected();
	}

	@Override
	public boolean isSliderVisible() {
		return widthMode == WidthMode.CUSTOM;
	}

	@NonNull
	public String getSelectedCustomValue() {
		return String.valueOf(getSelectedSliderValue());
	}

	@Override
	public int getSelectedSliderValue() {
		return customValue;
	}

	@Override
	public @NonNull List<SliderMode> getSliderModes() {
		List<SliderMode> sliderModes = new ArrayList<>();
		for (WidthMode widthMode : WidthMode.values()) {
			sliderModes.add(new SliderMode(widthMode.getIconId(), widthMode));
		}
		return sliderModes;
	}

	public void askSelectWidthMode(@Nullable String width) {
		askSelectWidthMode(WidthMode.valueOfKey(width));
	}

	private void askSelectWidthMode(@NonNull WidthMode widthMode) {
		askSelectSliderMode(new SliderMode(widthMode.getIconId(), widthMode));
	}

	@Nullable
	@Override
	public SliderMode getSelectedSliderMode() {
		return new SliderMode(widthMode.getIconId(), widthMode);
	}

	public void askSelectSliderMode(@NonNull SliderMode sliderMode) {
		if (!isSelectedSliderMode(sliderMode)) {
			widthMode = (WidthMode) sliderMode.getTag();
			if (cardInstance != null) {
				cardInstance.updateSegmentedButtonSelection();
				cardInstance.updateSliderVisibility();
			}
			notifyWidthSelected();
		}
		requestVerticalScrollIfNeeded();
	}

	@Override
	public boolean isSelectedSliderMode(@NonNull SliderMode sliderMode) {
		return Objects.equals(sliderMode.getTag(), widthMode);
	}

	@NonNull
	public String getSummary(@NonNull Context context) {
		if (widthMode == WidthMode.CUSTOM) {
			String custom = context.getString(R.string.shared_string_custom);
			String value = getSelectedCustomValue();
			return context.getString(R.string.ltr_or_rtl_combine_via_comma, custom, value);
		}
		return context.getString(widthMode.getTitleId());
	}

	@NonNull
	public String getSelectedWidthValue() {
		return widthMode == WidthMode.CUSTOM ? String.valueOf(customValue) : widthMode.getKey();
	}

	private void notifyWidthSelected() {
		String width = getSelectedWidthValue();
		listener.onWidthSelected(width);
	}

	private void requestVerticalScrollIfNeeded() {
		if (widthMode == WidthMode.CUSTOM && cardInstance != null) {
			View sliderContainer = cardInstance.getSliderContainer();
			ScrollUtils.addOnGlobalLayoutListener(sliderContainer, () -> {
				if (sliderContainer.getVisibility() == View.VISIBLE && onNeedScrollListener != null) {
					int y = AndroidUtils.getViewOnScreenY(sliderContainer);
					int viewHeight = sliderContainer.getHeight();
					onNeedScrollListener.onVerticalScrollNeeded(y + viewHeight);
				}
			});
		}
	}
}