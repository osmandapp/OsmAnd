package net.osmand.plus.card.base.slider.moded.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class SliderMode {

	@DrawableRes
	public int iconId;
	public Object tag;

	public SliderMode(@DrawableRes int iconId, @NonNull Object tag) {
		this.iconId = iconId;
		this.tag = tag;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public Object getTag() {
		return tag;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SliderMode)) return false;

		SliderMode that = (SliderMode) o;
		return getTag().equals(that.getTag());
	}

	@Override
	public int hashCode() {
		return getTag().hashCode();
	}
}
