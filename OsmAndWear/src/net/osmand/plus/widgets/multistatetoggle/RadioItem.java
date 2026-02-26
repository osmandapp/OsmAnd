package net.osmand.plus.widgets.multistatetoggle;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RadioItem {

	private boolean enabled = true;
	private OnRadioItemClickListener listener;
	@ColorInt
	private Integer customColor;
	private String contentDescription;
	private Object tag;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@NonNull
	public RadioItem setOnClickListener(OnRadioItemClickListener listener) {
		this.listener = listener;
		return this;
	}

	@Nullable
	public Object getTag() {
		return tag;
	}

	@NonNull
	public Object requireTag() {
		return tag;
	}

	@NonNull
	public RadioItem setTag(@Nullable Object tag) {
		this.tag = tag;
		return this;
	}

	public OnRadioItemClickListener getListener() {
		return listener;
	}

	@ColorInt
	@Nullable
	public Integer getCustomColor() {
		return customColor;
	}

	@NonNull
	public RadioItem setCustomColor(@ColorInt @Nullable Integer customColor) {
		this.customColor = customColor;
		return this;
	}

	@Nullable
	public String getContentDescription() {
		return contentDescription;
	}

	@NonNull
	public RadioItem setContentDescription(@Nullable String contentDescription) {
		this.contentDescription = contentDescription;
		return this;
	}

	public interface OnRadioItemClickListener {
		boolean onRadioItemClick(RadioItem radioItem, View view);
	}
}
