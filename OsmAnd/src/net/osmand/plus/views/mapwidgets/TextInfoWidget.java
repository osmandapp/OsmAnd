package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.app.Activity;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class TextInfoWidget  {

	private String contentTitle;
	private View view;
	private ImageView imageView;
	private TextView textView;
	private TextView textViewShadow;
	private TextView smallTextView;
	private TextView smallTextViewShadow;
	private ImageView topImageView;
	private TextView topTextView;
	private boolean explicitlyVisible;
	private OsmandApplication app;

	private int dayIcon;
	private int nightIcon;
	private boolean isNight;


	public TextInfoWidget(Activity activity) {
		app = (OsmandApplication) activity.getApplication();
		view = activity.getLayoutInflater().inflate(R.layout.map_hud_widget, null);
		topImageView = (ImageView) view.findViewById(R.id.widget_top_icon);
		topTextView = (TextView) view.findViewById(R.id.widget_top_icon_text);
		imageView = (ImageView) view.findViewById(R.id.widget_icon);
		textView = (TextView) view.findViewById(R.id.widget_text);
		textViewShadow = (TextView) view.findViewById(R.id.widget_text_shadow);
		smallTextViewShadow = (TextView) view.findViewById(R.id.widget_text_small_shadow);
		smallTextView = (TextView) view.findViewById(R.id.widget_text_small);
	}
	
	public View getView() {
		return view;
	}
	
	public void setImageDrawable(Drawable imageDrawable) {
		setImageDrawable(imageDrawable, false);
	}
	
	public void setImageDrawable(int res) {
		setImageDrawable(app.getIconsCache().getIcon(res, 0), false);
	}
	
	
	public void setImageDrawable(Drawable imageDrawable, boolean gone) {
		if(imageDrawable != null) {
			imageView.setImageDrawable(imageDrawable);
			imageView.setVisibility(View.VISIBLE);
		} else {
			imageView.setVisibility(gone ? View.GONE : View.INVISIBLE);
		}
		imageView.invalidate();
	}
	
	public void setTopImageDrawable(Drawable imageDrawable, String topText) {
		if(imageDrawable != null) {
			topImageView.setImageDrawable(imageDrawable);
			topImageView.setVisibility(View.VISIBLE);
			topTextView.setVisibility(View.VISIBLE);
			topTextView.setText(topText == null ? "" : topText);
		} else {
			topImageView.setVisibility(View.GONE );
			topTextView.setVisibility(View.GONE );
		}
		topTextView.invalidate();
		topImageView.invalidate();
	}
	
	public void setIcons(int widgetDayIcon, int widgetNightIcon) {
		dayIcon = widgetDayIcon;
		nightIcon = widgetNightIcon;
		setImageDrawable(!isNight ? dayIcon : nightIcon);
	}

	public boolean isNight() {
		return isNight;
	}

	public void setContentDescription(CharSequence text) {
		if (contentTitle != null) {
			view.setContentDescription(contentTitle + " " + text); //$NON-NLS-1$
		} else { 
			view.setContentDescription(text);
		}
	}
	
	public void setContentTitle(int messageId) {
		setContentTitle(view.getContext().getString(messageId));
	}

	public void setContentTitle(String text) {
		contentTitle = text;
		view.setContentDescription(text);
	}
	
	public void setText(String text, String subtext) {
		setTextNoUpdateVisibility(text, subtext);
		updateVisibility(text != null);
	}

	protected void setTextNoUpdateVisibility(String text, String subtext) {
		if (text != null) {
			if (subtext != null) {
				setContentDescription(text + " " + subtext); //$NON-NLS-1$
			} else {
				setContentDescription(text);
			}
		} else if(subtext != null){
			setContentDescription(subtext);
		}
//		if(this.text != null && this.text.length() > 7) {
//			this.text = this.text.substring(0, 6) +"..";
//		}
		if(text == null) {
			textView.setText("");
			textViewShadow.setText("");
		} else {
			textView.setText(text);
			textViewShadow.setText(text);
		}
		if(subtext == null) {
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
		if(dayIcon != 0) {
			setImageDrawable(!night? dayIcon : nightIcon);
		}
	}

	public void updateTextColor(int textColor, int textShadowColor, boolean bold, int rad) {
		updateTextColor(smallTextView, smallTextViewShadow, textColor, textShadowColor, bold, rad);
		updateTextColor(textView, textViewShadow, textColor, textShadowColor, bold, rad);
		updateTextColor(topTextView, null, textColor, textShadowColor, bold, rad);
	}
	
	public static void updateTextColor(TextView tv, TextView shadow, int textColor, int textShadowColor, boolean textBold, int rad) {
		if(shadow != null) {
			if(rad > 0) {
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
