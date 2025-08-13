package net.osmand.plus.widgets.chips;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class ChipItem {

	public final String id;
	public String title;
	public Drawable icon;

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

	@ColorInt
	public Integer strokeColor;
	@ColorInt
	public Integer strokeSelectedColor;
	@ColorInt
	public Integer strokeDisabledColor;

	public Integer strokeWidth;
	public Integer strokeSelectedWidth;
	public Integer strokeDisabledWidth;

	@ColorInt
	public Integer rippleColor;

	public Integer drawablePaddingPx;

	public String contentDescription;
	public View boundView;

	public Object tag;

	public OnAfterViewBoundCallback onAfterViewBoundCallback;

	public ChipItem(@NonNull String id) {
		this.id = id;
	}

	public interface OnAfterViewBoundCallback {
		void onAfterViewBound(@NonNull ChipItem chip, @NonNull ChipViewHolder holder);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChipItem)) return false;

		ChipItem item = (ChipItem) o;

		return id.equals(item.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
