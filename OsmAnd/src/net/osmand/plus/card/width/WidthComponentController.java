package net.osmand.plus.card.width;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.card.base.slider.ISliderCard;
import net.osmand.plus.card.base.slider.moded.IModedSliderComponent;
import net.osmand.plus.card.base.slider.moded.IModedSliderController;
import net.osmand.plus.card.base.slider.moded.data.SliderMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WidthComponentController implements IModedSliderController {

	private final WidthComponentListener listener;

	private IModedSliderComponent cardInstance;
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

	@Override
	public void onChangeSliderValue(float newValue) {
		customValue = (int) newValue;
		notifyWidthSelected();
	}

	@Override
	public boolean isSliderVisible() {
		return widthMode == WidthMode.CUSTOM;
	}

	@Override
	public int getSelectedSliderValue() {
		return customValue;
	}

	@Override
	public List<SliderMode> getSliderModes() {
		List<SliderMode> sliderModes = new ArrayList<>();
		for (WidthMode widthMode : WidthMode.values()) {
			sliderModes.add(new SliderMode(widthMode.getIconId(), widthMode));
		}
		return sliderModes;
	}

	public void askSelectWidthMode(@Nullable String width) {
		askSelectWidthMode(WidthMode.valueOfKey(width));
	}

	public void askSelectWidthMode(@NonNull WidthMode widthMode) {
		askSelectSliderMode(new SliderMode(widthMode.getIconId(), widthMode));
	}

	public boolean askSelectSliderMode(@NonNull SliderMode sliderMode) {
		if (!isSelectedSliderMode(sliderMode)) {
			widthMode = (WidthMode) sliderMode.getTag();
			cardInstance.updateSliderVisibility();
			notifyWidthSelected();
			return true;
		}
		return false;
	}

	@Override
	public boolean isSelectedSliderMode(@NonNull SliderMode sliderMode) {
		return Objects.equals(sliderMode.getTag(), widthMode);
	}

	@NonNull
	public String getSummary(@NonNull Context context) {
		if (widthMode == WidthMode.CUSTOM) {
			String custom = context.getString(R.string.shared_string_custom);
			String value = String.valueOf(getSelectedSliderValue());
			return context.getString(R.string.ltr_or_rtl_combine_via_comma, custom, value);
		}
		return context.getString(widthMode.getTitleId());
	}

	public void updateControlsColor() {
		cardInstance.updateControlsColor();
	}

	private void notifyWidthSelected() {
		String width = widthMode == WidthMode.CUSTOM ? String.valueOf(customValue) : widthMode.getKey();
		listener.onWidthSelected(width);
	}
}