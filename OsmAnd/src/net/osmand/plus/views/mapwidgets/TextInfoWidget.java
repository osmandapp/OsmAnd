package net.osmand.plus.views.mapwidgets;

import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.app.Activity;
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
	private TextView smallTextView;
	private ImageView topImageView;

	private boolean explicitlyVisible;


	public TextInfoWidget(Activity activity) {
		view = activity.getLayoutInflater().inflate(R.layout.map_hud_widget, null);
		topImageView = (ImageView) view.findViewById(R.id.widget_top_icon);
		imageView = (ImageView) view.findViewById(R.id.widget_icon);
		textView = (TextView) view.findViewById(R.id.widget_text);
		smallTextView = (TextView) view.findViewById(R.id.widget_text_small);
	}
	
	public View getView() {
		return view;
	}
	
	public void setImageDrawable(Drawable imageDrawable) {
		if(imageDrawable != null) {
			imageView.setImageDrawable(imageDrawable);
			imageView.setVisibility(View.VISIBLE);
		} else {
			imageView.setVisibility(View.INVISIBLE);
		}
		imageView.invalidate();
	}
	
	public void setTopImageDrawable(Drawable imageDrawable) {
		if(imageDrawable != null) {
			topImageView.setImageDrawable(imageDrawable);
			topImageView.setVisibility(View.VISIBLE);
		} else {
			topImageView.setVisibility(View.INVISIBLE);
		}
		topImageView.invalidate();
	}
	
	protected void invalidateImageViews() {
		topImageView.invalidate();
		imageView.invalidate();
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
		} else {
			textView.setText(text);
		}
		if(subtext == null) {
			smallTextView.setText("");
		} else {
			smallTextView.setText(subtext);
		}
		updateVisibility(text != null);
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

	public void updateTextColor(int textColor, int textShadowColor, boolean bold) {
		updateTextColor(smallTextView, textColor, textShadowColor, bold);
		updateTextColor(textView, textColor, textShadowColor, bold);
	}
	
	private void updateTextColor(TextView tv, int textColor, int textShadowColor, boolean textBold) {
		tv.setTextColor(textColor);
		tv.setShadowLayer(textShadowColor == 0 ? 0 : 8, 0, 0, textShadowColor);
		tv.setTypeface(Typeface.DEFAULT, textBold ? Typeface.BOLD : Typeface.NORMAL);
	}

	
	
}