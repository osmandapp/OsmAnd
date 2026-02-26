package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.widgets.style.CustomClickableSpan;
import net.osmand.util.Algorithms;

public abstract class WarningCard extends MapBaseCard {

	protected int imageId;
	protected Drawable imageDrawable;
	protected String title;
	protected String linkText;
	protected int startLinkIndex = -1;
	protected int endLinkIndex;

	protected WarningCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	public int getImageId() {
		return imageId;
	}

	public Drawable getImageDrawable() {
		return imageDrawable;
	}

	public String getTitle() {
		return title;
	}

	public String getLinkText() {
		return linkText;
	}

	protected void onLinkClicked() {
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.warning_card;
	}

	@Override
	protected void updateContent() {
		ImageView icon = view.findViewById(R.id.warning_img);
		TextView warningTitle = view.findViewById(R.id.warning_title);
		TextView warningLink = view.findViewById(R.id.warning_link);

		if (imageDrawable != null) {
			icon.setImageDrawable(imageDrawable);
		} else if (imageId != 0) {
			icon.setImageDrawable(getContentIcon(imageId));
		}
		warningTitle.setText(title);
		warningLink.setVisibility(!Algorithms.isEmpty(title) ? View.VISIBLE : View.GONE);

		if (!Algorithms.isEmpty(linkText)) {
			String text = linkText;
			SpannableString spannable = new SpannableString(text);
			ClickableSpan clickableSpan = new CustomClickableSpan() {
				@Override
				public void onClick(@NonNull View widget) {
					if (getListener() != null) {
						notifyButtonPressed(0);
					} else {
						onLinkClicked();
					}
				}

				@Override
				public void updateDrawState(@NonNull TextPaint ds) {
					super.updateDrawState(ds);
					ds.setColor(getActiveColor());
				}
			};
			int startLinkIndex = this.startLinkIndex;
			int endLinkIndex = this.endLinkIndex;
			if (startLinkIndex < 0 || endLinkIndex == 0) {
				startLinkIndex = 0;
				endLinkIndex = text.length();
				warningLink.setTextSize(15);
			}
			spannable.setSpan(clickableSpan, startLinkIndex, endLinkIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			spannable.setSpan(new StyleSpan(Typeface.BOLD), startLinkIndex, endLinkIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			warningLink.setMovementMethod(LinkMovementMethod.getInstance());
			warningLink.setText(spannable);
			warningLink.setVisibility(View.VISIBLE);
		} else {
			warningLink.setVisibility(View.GONE);
		}
	}
}