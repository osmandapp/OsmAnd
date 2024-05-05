package net.osmand.plus.widgets.style;

import android.graphics.Typeface;
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
			ds.setTypeface(Typeface.DEFAULT_BOLD);
		}
	}
}
