package net.osmand.core.samples.android.sample1.customcontrols;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class OsmandButton extends Button {

	public OsmandButton(Context context) {
		super(context);
	}

	public OsmandButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		OsmandTextView.parseAttributes(this, attrs, 0, 0);
	}

	public OsmandButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		OsmandTextView.parseAttributes(this, attrs, defStyleAttr, 0);
	}

	@TargetApi(21)
	public OsmandButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		OsmandTextView.parseAttributes(this, attrs, defStyleAttr, defStyleRes);
	}
}
