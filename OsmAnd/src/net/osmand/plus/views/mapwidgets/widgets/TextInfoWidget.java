package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceApplier;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportSidePanel;

public abstract class TextInfoWidget extends MapWidget implements ISupportSidePanel {

	protected static final String NO_VALUE = "—";

	protected String contentTitle;

	protected ImageView imageView;
	protected OutlinedTextContainer textView;
	protected OutlinedTextContainer smallTextView;
	protected TextView smallTextViewShadow;
	protected View container;
	protected View emptyBanner;
	protected View bottomDivider;

	@DrawableRes
	private int dayIconId;
	@DrawableRes
	private int nightIconId;

	private Integer cachedMetricSystem;
	private Integer cachedAltitudeMetric;
	private Integer cachedAngularUnits;

	protected String cachedText, cachedSmallText;
	protected Rect cachedTextBounds = new Rect();
	protected Rect cachedSmallTextBounds = new Rect();
	protected Rect cachedIconBounds = new Rect();
	protected TextPaint textPaint = new TextPaint();
	protected TextPaint smallTextPaint = new TextPaint();
	protected TextPaint workingPaint = new TextPaint();
	protected boolean shouldDrawAndroidAutoIcon;
	protected StaticLayout cachedTextLayout, cachedSmallTextLayout;


	public TextInfoWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType,
	                      @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(mapActivity, widgetType, customId, panel);
	}

	public TextInfoWidget(@NonNull OsmandApplication app, @NonNull WidgetType widgetType,
	                      @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(app, widgetType, customId, panel);
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		container = view.findViewById(R.id.container);
		emptyBanner = view.findViewById(R.id.empty_banner);
		imageView = view.findViewById(R.id.widget_icon);
		textView = view.findViewById(R.id.widget_text);
		smallTextViewShadow = view.findViewById(R.id.widget_text_small_shadow);
		smallTextView = view.findViewById(R.id.widget_text_small);
		bottomDivider = view.findViewById(R.id.bottom_divider);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.map_hud_widget;
	}

	public void setImageDrawable(@Nullable Drawable imageDrawable) {
		setImageDrawable(imageDrawable, false);
	}

	public void setImageDrawable(@DrawableRes int iconId) {
		setImageDrawable(iconsCache.getIcon(iconId, 0), false);
	}

	public void setImageDrawable(@Nullable Drawable drawable, boolean gone) {
		if (imageView != null) {
			setImageDrawable(imageView, drawable, gone ? View.GONE : View.INVISIBLE);
		}
	}

	protected void setImageDrawable(@NonNull ImageView imageView, @Nullable Drawable drawable, int visibility) {
		if (drawable != null) {
			imageView.setImageDrawable(drawable);
			Object anim = imageView.getDrawable();
			if (anim instanceof AnimationDrawable) {
				((AnimationDrawable) anim).start();
			}
			imageView.setVisibility(View.VISIBLE);
		} else {
			imageView.setVisibility(visibility);
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

	public void setContentTitle(int messageId) {
		setContentTitle(getString(messageId));
	}

	public void setContentTitle(String text) {
		contentTitle = text;
		if (textView != null && smallTextView != null) {
			getView().setContentDescription(combine(textView.getText(), smallTextView.getText()));
		}
	}

	public void setText(String text, String subtext) {
		setTextNoUpdateVisibility(text, subtext);
		updateVisibility(text != null);
	}

	protected void setTextNoUpdateVisibility(String text, String subtext) {
		if (!isAndroidAuto()) {
			getView().setContentDescription(combine(text, subtext));
		}
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
		cachedText = text;
		if (textView != null) {
			textView.setText(text);
		}
		markAndroidAutoLayoutNeeded();
	}

	private void setSmallText(String text) {
		cachedSmallText = text;
		if (smallTextView != null) {
			smallTextView.setText(text);
			if (smallTextViewShadow != null) {
				smallTextViewShadow.setText(text);
			}
		}
		markAndroidAutoLayoutNeeded();
	}

	public boolean isUpdateNeeded() {
		boolean updateNeeded = false;
		if (isMetricSystemDepended()) {
			int metricSystem = app.getSettings().METRIC_SYSTEM.get().ordinal();
			updateNeeded = cachedMetricSystem == null || cachedMetricSystem != metricSystem;
			cachedMetricSystem = metricSystem;
		}
		if (isAltitudeMetricDepended()) {
			int altitudeMetric = app.getSettings().ALTITUDE_METRIC.get().ordinal();
			updateNeeded = cachedAltitudeMetric == null || cachedAltitudeMetric != altitudeMetric;
			cachedAltitudeMetric = altitudeMetric;
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

	public boolean isAltitudeMetricDepended() {
		return false;
	}

	public boolean isAngularUnitsDepended() {
		return false;
	}

	@Override
	protected void onPanelAppearanceChanged(@NonNull ResolvedPanelAppearance appearance) {
		super.onPanelAppearanceChanged(appearance);
		PanelAppearanceApplier.applyPrimaryText(textView, appearance);
		PanelAppearanceApplier.applySecondaryText(smallTextView, smallTextViewShadow, appearance);

		int iconId = getIconId();
		if (iconId != 0) {
			setImageDrawable(iconId);
		}

		PanelAppearanceApplier.applyBackground(getView(), appearance);
		PanelAppearanceApplier.applyDivider(bottomDivider, appearance);
	}

	@Override
	public void onAndroidAutoPanelAppearanceChanged(@NonNull ResolvedPanelAppearance appearance) {
		super.onAndroidAutoPanelAppearanceChanged(appearance);
		configureAAPaints(appearance);
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
		Pair<String, String> formattedTime = OsmAndFormatter.getFormattedTime(app, time);
		setText(formattedTime.first, formattedTime.second);
	}

	@DrawableRes
	public int getIconId(boolean nightMode) {
		return nightMode ? nightIconId : dayIconId;
	}

	@DrawableRes
	public int getMapIconId(boolean nightMode) {
		return getIconId(nightMode);
	}

	// region android auto
	protected StaticLayout updateCachedTextBounds(Rect bounds, TextPaint paint, String text) {
		return updateCachedTextBounds(bounds, paint, text, null);
	}

	protected StaticLayout updateCachedTextBounds(Rect bounds, TextPaint paint, String text, Float lineSpacingExtra) {
		StaticLayout staticLayout = null;
		bounds.setEmpty();
		if (text != null) {
			float desiredWidth = (float) Math.ceil(StaticLayout.getDesiredWidth(text, paint));
			staticLayout = buildTextLineStaticLayout(text, paint, desiredWidth, Gravity.START | Gravity.END, lineSpacingExtra, null);
			if (staticLayout != null) {
				bounds.set(0, 0, staticLayout.getWidth(), staticLayout.getHeight());
			}
		}
		return staticLayout;
	}

	protected void configureAAPaints(ResolvedPanelAppearance appearance) {
		applyPrimaryTextAppearance(textPaint, appearance);
		applySecondaryTextAppearance(smallTextPaint, appearance);
	}

	protected float getPrimaryTextSizeAA() {
		return app.getResources().getDimension(R.dimen.map_widget_text_size);
	}

	protected float getSecondaryTextSizeAA() {
		return app.getResources().getDimension(R.dimen.map_widget_text_size_small);
	}

	protected void applyPrimaryTextAppearance(Paint paint, ResolvedPanelAppearance appearance) {
		applyTextAppearance(paint, appearance.getPrimaryTextColor(), appearance);
		paint.setTextSize(getPrimaryTextSizeAA());
	}

	protected void applySecondaryTextAppearance(Paint paint, ResolvedPanelAppearance appearance) {
		applyTextAppearance(paint, appearance.getSecondaryTextColor(), appearance);
		paint.setTextSize(getSecondaryTextSizeAA());
	}

	protected void applyTextAppearance(
			Paint textPaint,
			@ColorInt int color,
			ResolvedPanelAppearance appearance
	) {
		int typefaceStyle = (appearance.getBoldText()) ? Typeface.BOLD : Typeface.NORMAL;
		textPaint.setColor(color);
		textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, typefaceStyle));
	}

	protected float findOptimalSingleLineTextSize(
			CharSequence text, TextPaint paint,
			int maxWidth, int maxHeight,
			float minSize, float maxSize, float step
	) {
		TextPaint testPaint = new TextPaint(paint);
		float low = minSize;
		float high = maxSize;
		float optimal = minSize;

		while (low <= high) {
			float mid = Math.round((low + high) / 2);
			testPaint.setTextSize(mid);

			StaticLayout.Builder layoutBuilder = StaticLayout.Builder
					.obtain(text, 0, text.length(), testPaint, Integer.MAX_VALUE)
					.setIncludePad(false);
			StaticLayout layout = layoutBuilder.build();

			if (layout.getLineWidth(0) <= maxWidth && layout.getHeight() <= maxHeight) {
				optimal = mid;
				low = mid + step;
			} else {
				high = mid - step;
			}
		}
		return optimal;
	}

	protected void drawTextLineInRect(
			Canvas canvas,
			Rect targetRect,
			StaticLayout staticLayout) {
		drawTextLineInRect(canvas,
				targetRect,
				staticLayout,
				Gravity.START | Gravity.BOTTOM
		);
	}

	protected StaticLayout measureText(Rect outRect,
	                                   float maxWidthPx, String text, Paint textPaint,
	                                   int gravity,
	                                   Float lineSpacingExtra,
	                                   TextUtils.TruncateAt ellipsizeAt,
	                                   Integer maxLines) {
		TextPaint workingPaint = new TextPaint(textPaint);
		Layout.Alignment layoutAlignment;
		int horizontalGravity = gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;

		if (horizontalGravity == Gravity.END) {
			layoutAlignment = Layout.Alignment.ALIGN_OPPOSITE;
		} else if (horizontalGravity == Gravity.CENTER_HORIZONTAL) {
			layoutAlignment = Layout.Alignment.ALIGN_CENTER;
		} else {
			layoutAlignment = Layout.Alignment.ALIGN_NORMAL;
		}

		StaticLayout.Builder layoutBuilder = StaticLayout.Builder
				.obtain(text, 0, text.length(), workingPaint, (int) maxWidthPx)
				.setAlignment(layoutAlignment)
				.setIncludePad(false);
		if (maxLines != null) {
			layoutBuilder.setMaxLines(maxLines);
		}
		if (ellipsizeAt != null) {
			layoutBuilder.setEllipsize(ellipsizeAt);
		}
		if (lineSpacingExtra != null) {
			layoutBuilder.setLineSpacing(lineSpacingExtra, 1f);
		}
		StaticLayout staticLayout = layoutBuilder.build();
		outRect.set(0, 0, staticLayout.getWidth(), staticLayout.getHeight());
		return staticLayout;
	}

	@Nullable
	protected StaticLayout buildTextLineStaticLayout(
			CharSequence text,
			TextPaint textPaint,
			float targetWidth,
			int gravity,
			Float lineSpacingExtra,
			TextUtils.TruncateAt ellipsizeAt
	) {
		if (text == null) {
			return null;
		}
		if (targetWidth <= 0) {
			return null;
		}
		workingPaint.set(textPaint);

		Layout.Alignment layoutAlignment;
		int horizontalGravity = gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;

		if (horizontalGravity == Gravity.END) {
			layoutAlignment = Layout.Alignment.ALIGN_OPPOSITE;
		} else if (horizontalGravity == Gravity.CENTER_HORIZONTAL) {
			layoutAlignment = Layout.Alignment.ALIGN_CENTER;
		} else {
			layoutAlignment = Layout.Alignment.ALIGN_NORMAL;
		}

		StaticLayout.Builder layoutBuilder = StaticLayout.Builder
				.obtain(text, 0, text.length(), workingPaint, (int) targetWidth)
				.setAlignment(layoutAlignment).setMaxLines(1)
				.setIncludePad(false);
		if (ellipsizeAt != null) {
			layoutBuilder.setEllipsize(ellipsizeAt);
		}
		if (lineSpacingExtra != null) {
			layoutBuilder.setLineSpacing(lineSpacingExtra, 1f);
		}

		return layoutBuilder.build();
	}

	protected void drawTextLineInRect(
			Canvas canvas,
			Rect targetRect,
			StaticLayout staticLayout,
			int gravity
	) {
		if (staticLayout == null) {
			return;
		}

		int rectWidth = targetRect.width();
		int rectHeight = targetRect.height();
		if (rectWidth <= 0 || rectHeight <= 0) {
			return;
		}

		int layoutWidth = staticLayout.getWidth();
		int layoutHeight = staticLayout.getHeight();

		int horizontalGravity = gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
		int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
		float xOffset = 0f;
		if (horizontalGravity == Gravity.END) {
			xOffset = (float) (rectWidth - layoutWidth);
		} else if (horizontalGravity == Gravity.CENTER_HORIZONTAL) {
			xOffset = (rectWidth - layoutWidth) / 2f;
		}

		float yOffset = 0f;
		if (verticalGravity == Gravity.BOTTOM) {
			yOffset = (float) (rectHeight - layoutHeight);
		} else if (verticalGravity == Gravity.CENTER_VERTICAL) {
			yOffset = (rectHeight - layoutHeight) / 2f;
		}

		float finalX = targetRect.left + xOffset;
		float finalY = targetRect.top + yOffset;

		canvas.save();
		canvas.clipRect(targetRect);
		canvas.translate(finalX, finalY);
		staticLayout.draw(canvas);
		canvas.restore();
	}
	// endregion
}
