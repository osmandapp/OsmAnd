package net.osmand.core.samples.android.sample1.customcontrols;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.R;

public class OsmandTextView extends TextView {

	public OsmandTextView(Context context) {
		super(context);
	}

	public OsmandTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		parseAttributes(this, attrs, 0, 0);
	}

	public OsmandTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		parseAttributes(this, attrs, defStyleAttr, 0);
	}

	@TargetApi(21)
	public OsmandTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		parseAttributes(this, attrs, defStyleAttr, defStyleRes);
	}

	static void parseAttributes(TextView target, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		if (attrs == null) {
			return;
		}
		TypedArray resolvedAttrs = target.getContext().getTheme().obtainStyledAttributes(attrs,
				R.styleable.OsmandTextView, defStyleAttr, defStyleRes);
		applyAttributes(resolvedAttrs, target);
		resolvedAttrs.recycle();
	}

	private static void applyAttributes(TypedArray resolvedAttributes, TextView target) {
		applyAttribute_osmandText(resolvedAttributes, target);
	}

	static void applyAttribute_osmandText(TypedArray resolvedAttributes, TextView target) {
		if (!resolvedAttributes.hasValue(R.styleable.OsmandTextView_osmandText)
				|| target.isInEditMode()) {
			return;
		}

		String osmandText = resolvedAttributes.getString(R.styleable.OsmandTextView_osmandText);
		target.setText(OsmandResources.getString(osmandText));
	}
}
