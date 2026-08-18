package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportSidePanel;

public class TextInfoWidget extends MapWidget implements ISupportSidePanel {

	protected static final String NO_VALUE = "—";

	protected String contentTitle;

	protected ImageView imageView;
	protected TextView textView;
	protected TextView textViewShadow;
	protected TextView smallTextView;
	protected TextView smallTextViewShadow;
	protected View container;
	protected View emptyBanner;
	protected View bottomDivider;

	@DrawableRes
	private int dayIconId;
	@DrawableRes
	private int nightIconId;

	private Integer cachedMetricSystem;
	private Integer cachedAngularUnits;


	public TextInfoWidget(@NonNull MapActivity mapActivity, @Nullable WidgetType widgetType) {
		super(mapActivity, widgetType);
		container = view.findViewById(R.id.container);
		emptyBanner = view.findViewById(R.id.empty_banner);
		imageView = view.findViewById(R.id.widget_icon);
		textView = view.findViewById(R.id.widget_text);
		textViewShadow = view.findViewById(R.id.widget_text_shadow);
		smallTextViewShadow = view.findViewById(R.id.widget_text_small_shadow);
		smallTextView = view.findViewById(R.id.widget_text_small);
		bottomDivider = view.findViewById(R.id.bottom_divider);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.map_hud_widget;
	}

	public void setImageDrawable(Drawable imageDrawable) {
		setImageDrawable(imageDrawable, false);
	}

	public void setImageDrawable(int res) {
		setImageDrawable(iconsCache.getIcon(res, 0), false);
	}

	public void setImageDrawable(Drawable imageDrawable, boolean gone) {
		if (imageDrawable != null) {
			imageView.setImageDrawable(imageDrawable);
			Object anim = imageView.getDrawable();
			if (anim instanceof AnimationDrawable) {
				((AnimationDrawable) anim).start();
			}
			imageView.setVisibility(View.VISIBLE);
		} else {
			imageView.setVisibility(gone ? View.GONE : View.INVISIBLE);
		}
		imageView.invalidate();
	}

	public boolean setIcons(@NonNull WidgetType widgetType) {
		return setIcons(widgetType.dayIconId, widgetType.nightIconId);
	}

	public boolean setIcons(@DrawableRes int widgetDayIcon, @DrawableRes int widgetNightIcon) {
		if (dayIconId != widgetDayIcon || nightIconId != widgetNightIcon) {
			dayIconId = widgetDayIcon;
			nightIconId = widgetNightIcon;
			setImageDrawable(getIconId());
			return true;
		} else {
			return false;
		}
	}

	protected CharSequence combine(CharSequence text, CharSequence subtext) {
		if (TextUtils.isEmpty(text)) {
			return subtext;
		} else if (TextUtils.isEmpty(subtext)) {
			return text;
		}
		return text + " " + subtext;
	}

	public void setContentDescription(CharSequence text) {
		view.setContentDescription(combine(contentTitle, text));
	}

	public void setContentTitle(int messageId) {
		setContentTitle(getString(messageId));
	}

	public void setContentTitle(String text) {
		contentTitle = text;
		setContentDescription(combine(textView.getText(), smallTextView.getText()));
	}

	public void setText(String text, String subtext) {
		setTextNoUpdateVisibility(text, subtext);
		updateVisibility(text != null);
	}

	protected void setTextNoUpdateVisibility(String text, String subtext) {
		setContentDescription(combine(text, subtext));
		if (text == null) {
			setText("");
		} else {
			setText(text);
		}
		if (subtext == null) {
			setSmallText("");
		} else {
			setSmallText(subtext);
		}
	}

	private void setText(String text) {
		textView.setText(text);
		if (textViewShadow != null) {
			textViewShadow.setText(text);
		}
	}

	private void setSmallText(String text) {
		smallTextView.setText(text);
		if (smallTextViewShadow != null) {
			smallTextViewShadow.setText(text);
		}
	}

	public boolean isUpdateNeeded() {
		boolean updateNeeded = false;
		if (isMetricSystemDepended()) {
			int metricSystem = app.getSettings().METRIC_SYSTEM.get().ordinal();
			updateNeeded = cachedMetricSystem == null || cachedMetricSystem != metricSystem;
			cachedMetricSystem = metricSystem;
		}
		if (isAngularUnitsDepended()) {
			int angularUnits = app.getSettings().ANGULAR_UNITS.get().ordinal();
			updateNeeded |= cachedAngularUnits == null || cachedAngularUnits != angularUnits;
			cachedAngularUnits = angularUnits;
		}
		return updateNeeded;
	}

	public boolean isMetricSystemDepended() {
		return false;
	}

	public boolean isAngularUnitsDepended() {
		return false;
	}

	public void setOnClickListener(@Nullable OnClickListener onClickListener) {
		view.setOnClickListener(onClickListener);
	}

	@Override
	public void updateColors(@NonNull TextState textState) {
		super.updateColors(textState);
		updateTextColor(smallTextView, smallTextViewShadow, textState.textColor, textState.textShadowColor,
				textState.textBold, textState.textShadowRadius);
		updateTextColor(textView, textViewShadow, textState.textColor, textState.textShadowColor,
				textState.textBold, textState.textShadowRadius);
		int iconId = getIconId();
		if (iconId != 0) {
			setImageDrawable(iconId);
		}

		view.setBackgroundResource(getBackgroundResource(textState));
		if (bottomDivider != null) {
			bottomDivider.setBackgroundResource(textState.widgetDividerColorId);
		}
	}

	@DrawableRes
	protected int getBackgroundResource(@NonNull TextState textState) {
		return textState.widgetBackgroundId;
	}

	@Override
	public boolean isViewVisible() {
		return getContentView().getVisibility() == View.VISIBLE;
	}

	public boolean updateVisibility(boolean visible) {
		boolean updatedVisibility = AndroidUiHelper.updateVisibility(getContentView(), visible);
		if (updatedVisibility && app.accessibilityEnabled()) {
			getContentView().setFocusable(visible);
		}
		return updatedVisibility;
	}

	protected View getContentView() {
		return container;
	}

	public boolean updateBannerVisibility(boolean visible) {
		return AndroidUiHelper.updateVisibility(emptyBanner, visible);
	}

	@DrawableRes
	protected int getIconId() {
		return getIconId(isNightMode());
	}

	protected void setTimeText(long time) {
		if (DateFormat.is24HourFormat(app)) {
			setText(DateFormat.format("k:mm", time).toString(), null);
		} else {
			setText(DateFormat.format("h:mm", time).toString(),
					DateFormat.format("aa", time).toString());
		}
	}

	@DrawableRes
	public int getIconId(boolean nightMode) {
		return nightMode ? nightIconId : dayIconId;
	}
}