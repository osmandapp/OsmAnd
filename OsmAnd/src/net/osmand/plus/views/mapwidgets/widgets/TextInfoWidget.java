package net.osmand.plus.views.mapwidgets.widgets;

import android.app.Activity;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;

public class TextInfoWidget {

	private OsmandApplication app;

	private String contentTitle;
	private View view;
	private ImageView imageView;
	private TextView textView;
	private TextView textViewShadow;
	private TextView smallTextView;
	private TextView smallTextViewShadow;
	private ImageView topImageView;
	protected TextView topTextView;
	private boolean explicitlyVisible;

	private int dayIcon;
	private int nightIcon;
	private boolean isNight;
	private ViewGroup bottomLayout;

	private Integer cachedMetricSystem = null;
	private Integer cachedAngularUnits = null;

	public TextInfoWidget(Activity activity) {
		app = (OsmandApplication) activity.getApplication();
		view = UiUtilities.getInflater(activity, isNight).inflate(R.layout.map_hud_widget, null);
		bottomLayout = view.findViewById(R.id.widget_bottom_layout);
		topImageView = view.findViewById(R.id.widget_top_icon);
		topTextView = view.findViewById(R.id.widget_top_icon_text);
		imageView = view.findViewById(R.id.widget_icon);
		textView = view.findViewById(R.id.widget_text);
		textViewShadow = view.findViewById(R.id.widget_text_shadow);
		smallTextViewShadow = view.findViewById(R.id.widget_text_small_shadow);
		smallTextView = view.findViewById(R.id.widget_text_small);
	}

	public OsmandApplication getApplication() {
		return app;
	}

	public View getView() {
		return view;
	}

	public void setImageDrawable(Drawable imageDrawable) {
		setImageDrawable(imageDrawable, false);
	}

	public void setImageDrawable(int res) {
		setImageDrawable(app.getUIUtilities().getIcon(res, 0), false);
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

	public void setTopImageDrawable(Drawable imageDrawable, String topText) {
		if (imageDrawable != null) {
			topImageView.setImageDrawable(imageDrawable);
			topImageView.setVisibility(View.VISIBLE);
			LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
			lp.gravity = Gravity.CENTER_HORIZONTAL;
			bottomLayout.setLayoutParams(lp);
			bottomLayout.invalidate();
			topTextView.setVisibility(View.VISIBLE);
			topTextView.setText(topText == null ? "" : topText);
		} else {
			topImageView.setVisibility(View.GONE);
			topTextView.setVisibility(View.GONE);
			LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) bottomLayout.getLayoutParams();
			lp.gravity = Gravity.NO_GRAVITY;
			bottomLayout.setLayoutParams(lp);
		}

		topTextView.invalidate();
		topImageView.invalidate();
	}

	public boolean setIcons(int widgetDayIcon, int widgetNightIcon) {
		if (dayIcon != widgetDayIcon || nightIcon != widgetNightIcon) {
			dayIcon = widgetDayIcon;
			nightIcon = widgetNightIcon;
			setImageDrawable(!isNight ? dayIcon : nightIcon);
			return true;
		} else {
			return false;
		}
	}

	public boolean isNight() {
		return isNight;
	}

	private CharSequence combine(CharSequence text, CharSequence subtext) {
		if (TextUtils.isEmpty(text)) {
			return subtext;
		} else if (TextUtils.isEmpty(subtext)) {
			return text;
		}
		return text + " " + subtext; //$NON-NLS-1$
	}

	public void setContentDescription(CharSequence text) {
		view.setContentDescription(combine(contentTitle, text));
	}

	public void setContentTitle(int messageId) {
		setContentTitle(view.getContext().getString(messageId));
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
//		if(this.text != null && this.text.length() > 7) {
//			this.text = this.text.substring(0, 6) +"..";
//		}
		if (text == null) {
			textView.setText("");
			textViewShadow.setText("");
		} else {
			textView.setText(text);
			textViewShadow.setText(text);
		}
		if (subtext == null) {
			smallTextView.setText("");
			smallTextViewShadow.setText("");
		} else {
			smallTextView.setText(subtext);
			smallTextViewShadow.setText(subtext);
		}
	}

	protected boolean updateVisibility(boolean visible) {
		if (visible != (view.getVisibility() == View.VISIBLE)) {
			if (visible) {
				view.setVisibility(View.VISIBLE);
			} else {
				view.setVisibility(View.GONE);
			}
			view.invalidate();
			if (app.accessibilityEnabled())
				view.setFocusable(visible);
			return true;
		}
		return false;
	}

	public boolean isVisible() {
		return view.getVisibility() == View.VISIBLE && view.getParent() != null;
	}

	public boolean updateInfo(DrawSettings drawSettings) {
		return false;
	}

	public boolean isUpdateNeeded() {
		boolean res = false;
		if (isMetricSystemDepended()) {
			int metricSystem = app.getSettings().METRIC_SYSTEM.get().ordinal();
			res |= cachedMetricSystem == null || cachedMetricSystem != metricSystem;
			cachedMetricSystem = metricSystem;
		}
		if (isAngularUnitsDepended()) {
			int angularUnits = app.getSettings().ANGULAR_UNITS.get().ordinal();
			res |= cachedAngularUnits == null || cachedAngularUnits != angularUnits;
			cachedAngularUnits = angularUnits;
		}
		return res;
	}

	public boolean isMetricSystemDepended() {
		return false;
	}

	public boolean isAngularUnitsDepended() {
		return false;
	}

	public void setOnClickListener(OnClickListener onClickListener) {
		view.setOnClickListener(onClickListener);
	}

	public void setExplicitlyVisible(boolean explicitlyVisible) {
		this.explicitlyVisible = explicitlyVisible;
	}

	public boolean isExplicitlyVisible() {
		return explicitlyVisible;
	}

	public void updateIconMode(boolean night) {
		isNight = night;
		if (dayIcon != 0) {
			setImageDrawable(!night ? dayIcon : nightIcon);
		}
	}

	public void updateTextColor(int textColor, int textShadowColor, boolean bold, int rad) {
		updateTextColor(smallTextView, smallTextViewShadow, textColor, textShadowColor, bold, rad);
		updateTextColor(textView, textViewShadow, textColor, textShadowColor, bold, rad);
		updateTextColor(topTextView, null, textColor, textShadowColor, bold, rad);
	}

	public static void updateTextColor(TextView tv, TextView shadow, int textColor, int textShadowColor, boolean textBold, int rad) {
		if (shadow != null) {
			if (rad > 0) {
				shadow.setVisibility(View.VISIBLE);
				shadow.setTypeface(Typeface.DEFAULT, textBold ? Typeface.BOLD : Typeface.NORMAL);
				shadow.getPaint().setStrokeWidth(rad);
				shadow.getPaint().setStyle(Style.STROKE);
				shadow.setTextColor(textShadowColor);
//				tv.getPaint().setStyle(Style.FILL);
			} else {
//				tv.getPaint().setStyle(Style.FILL_AND_STROKE);
				shadow.setVisibility(View.GONE);
			}
		}
		tv.setTextColor(textColor);
		tv.setTypeface(Typeface.DEFAULT, textBold ? Typeface.BOLD : Typeface.NORMAL);
	}
}