package net.osmand.plus.widgets.chips;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class ChipItem {

	public final String id;
	public String title;
	public Drawable icon;

	boolean isSelected = false;
	public boolean isEnabled = true;

	@ColorInt
	public Integer titleColor;
	@ColorInt
	public Integer titleSelectedColor;
	@ColorInt
	public Integer titleDisabledColor;

	@ColorInt
	public Integer iconColor;
	@ColorInt
	public Integer iconSelectedColor;
	@ColorInt
	public Integer iconDisabledColor;
	public boolean useNaturalIconColor;

	@ColorInt
	public Integer bgColor;
	@ColorInt
	public Integer bgSelectedColor;
	@ColorInt
	public Integer bgDisabledColor;

	public Integer drawablePaddingPx;

	public OnBeforeBindCallback onBeforeBindCallback;
	public OnAfterBindCallback onAfterBindCallback;
	public Object tag;

	public boolean isSelected() {
		return isSelected;
	}

	public ChipItem(@NonNull String id) {
		this.id = id;
	}

	public interface OnBeforeBindCallback {
		void onBeforeViewBound(@NonNull ChipItem chip);
	}

	public interface OnAfterBindCallback {
		void onAfterViewBound(@NonNull ChipItem chip, @NonNull ChipViewHolder holder);
	}

}
