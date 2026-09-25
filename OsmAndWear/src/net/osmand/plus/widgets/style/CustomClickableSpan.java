package net.osmand.plus.widgets.style;

import android.text.TextPaint;
import android.text.style.ClickableSpan;

import androidx.annotation.NonNull;

public abstract class CustomClickableSpan extends ClickableSpan {

	@Override
	public void updateDrawState(@NonNull TextPaint ds) {
		super.updateDrawState(ds);
		ds.setUnderlineText(false);
	}
}
