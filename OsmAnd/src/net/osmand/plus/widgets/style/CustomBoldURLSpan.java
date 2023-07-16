package net.osmand.plus.widgets.style;

import androidx.annotation.NonNull;

public class CustomBoldURLSpan extends CustomURLSpan {

	public CustomBoldURLSpan(@NonNull String url) {
		super(url);
		useBoldTypeface = true;
	}

}
