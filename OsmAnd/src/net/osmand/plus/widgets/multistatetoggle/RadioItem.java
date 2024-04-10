package net.osmand.plus.widgets.multistatetoggle;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public class RadioItem {

	private boolean enabled = true;
	private OnRadioItemClickListener listener;
	@ColorInt
	private Integer customColor;
	private Object tag;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setOnClickListener(OnRadioItemClickListener listener) {
		this.listener = listener;
	}

	@Nullable
	public Object getTag() {
		return tag;
	}

	public void setTag(@Nullable Object tag) {
		this.tag = tag;
	}

	public OnRadioItemClickListener getListener() {
		return listener;
	}

	@ColorInt
	@Nullable
	public Integer getCustomColor() {
		return customColor;
	}

	public void setCustomColor(@ColorInt @Nullable Integer customColor) {
		this.customColor = customColor;
	}

	public interface OnRadioItemClickListener {
		boolean onRadioItemClick(RadioItem radioItem, View view);
	}
}
