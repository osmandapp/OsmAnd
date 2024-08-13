package net.osmand.plus.widgets.style;

import static android.graphics.Typeface.DEFAULT_BOLD;

import android.text.TextPaint;
import android.text.style.URLSpan;

import androidx.annotation.NonNull;

public class CustomURLSpan extends URLSpan {

	protected boolean useBoldTypeface = false;

	public CustomURLSpan(@NonNull String url) {
		super(url);
	}

	public void setUseBoldTypeface(boolean useBoldTypeface) {
		this.useBoldTypeface = useBoldTypeface;
	}

	@Override
	public void updateDrawState(@NonNull TextPaint ds) {
		super.updateDrawState(ds);
		ds.setUnderlineText(false);

		if (useBoldTypeface) {
			ds.setTypeface(DEFAULT_BOLD);
		}
	}
}
