package net.osmand.plus.widgets.chips;

import androidx.annotation.ColorInt;

class DefaultAttributes {

	@ColorInt
	public int titleColor;
	@ColorInt
	public int titleSelectedColor;
	@ColorInt
	public int titleDisabledColor;

	@ColorInt
	public int iconColor;
	@ColorInt
	public int iconSelectedColor;
	@ColorInt
	public int iconDisabledColor;
	public boolean useNaturalIconColor;

	@ColorInt
	public int bgColor;
	@ColorInt
	public int bgSelectedColor;
	@ColorInt
	public int bgDisabledColor;
	public int bgRippleId;

	/**
	 * Drawable padding applies only when both of title and drawable allowed for chip item
	 */
	public int drawablePaddingPx;

	@ColorInt
	public int strokeColor;
	public int strokeWidth;

}
