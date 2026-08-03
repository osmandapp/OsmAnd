package net.osmand.plus.widgets.chips;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

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

	@ColorInt
	public int bgColor;
	@ColorInt
	public int bgSelectedColor;
	@ColorInt
	public int bgDisabledColor;
	@ColorInt
	public Integer rippleColor;
	public int bgRippleId;

	/**
	 * Drawable padding applies only when both of title and drawable allowed for chip item
	 */
	public int drawablePaddingPx;

	@ColorInt
	public int strokeColor;
	public int strokeWidth;

	private DefaultAttributes() {
	}

	public static DefaultAttributes createInstance(@NonNull Context context,
	                                               @Nullable AttributeSet attrs,
	                                               int defStyleAttr) {
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
				R.styleable.HorizontalChipsView,
				defStyleAttr,
				0
		);

		DefaultAttributes defAttrs = new DefaultAttributes();

		defAttrs.titleColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipTitleColor,
				getColorFromAttr(context, R.attr.chip_content_color));
		defAttrs.titleSelectedColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipTitleSelectedColor,
				getColor(context, R.color.card_and_list_background_light));
		defAttrs.titleDisabledColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipTitleDisabledColor,
				getColorFromAttr(context, R.attr.inactive_text_color));

		defAttrs.iconColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipIconColor,
				getColorFromAttr(context, R.attr.chip_content_color));
		defAttrs.iconSelectedColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipIconSelectedColor,
				getColor(context, R.color.card_and_list_background_light));
		defAttrs.iconDisabledColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipIconDisabledColor,
				getColorFromAttr(context, R.attr.inactive_text_color));

		defAttrs.bgColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipBgColor,
				getColor(context, R.color.color_transparent));
		defAttrs.bgSelectedColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipBgSelectedColor,
				getColorFromAttr(context, R.attr.active_color_basic));
		defAttrs.bgDisabledColor = a.getInteger(
				R.styleable.HorizontalChipsView_chipBgDisabledColor,
				getColor(context, R.color.color_transparent));
		defAttrs.rippleColor = resolveAttribute(context, R.attr.active_color_basic);
		defAttrs.bgRippleId = resolveAttribute(context, R.attr.chip_ripple);

		defAttrs.drawablePaddingPx = getDimension(context, R.dimen.content_padding_half);

		defAttrs.strokeColor = getColorFromAttr(context, R.attr.stroked_buttons_and_links_outline);
		defAttrs.strokeWidth = AndroidUtils.dpToPx(context, 1);

		return defAttrs;
	}

	@ColorInt
	private static int getColorFromAttr(@NonNull Context ctx, int attr) {
		return getColor(ctx, resolveAttribute(ctx, attr));
	}

	private static int resolveAttribute(@NonNull Context ctx, int attr) {
		return AndroidUtils.resolveAttribute(ctx, attr);
	}

	@ColorInt
	private static int getColor(@NonNull Context ctx, @ColorRes int colorId) {
		return ColorUtilities.getColor(ctx, colorId);
	}

	private static int getDimension(@NonNull Context ctx, @DimenRes int dimen) {
		return ctx.getResources().getDimensionPixelSize(dimen);
	}

}
