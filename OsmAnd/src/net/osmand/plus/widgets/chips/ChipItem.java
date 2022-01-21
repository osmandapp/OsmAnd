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

	@ColorInt
	public Integer bgColor;
	@ColorInt
	public Integer bgSelectedColor;
	@ColorInt
	public Integer bgDisabledColor;

	public Integer drawablePaddingPx;

	public Object tag;

	public OnAfterViewBoundCallback onAfterViewBoundCallback;

	public ChipItem(@NonNull String id) {
		this.id = id;
	}

	public interface OnAfterViewBoundCallback {
		void onAfterViewBound(@NonNull ChipItem chip, @NonNull ChipViewHolder holder);
	}

}
