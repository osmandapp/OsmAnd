package net.osmand.core.samples.android.sample1.customcontrols;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageButton;

import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.R;


public class OsmandImageButton extends ImageButton {

	public OsmandImageButton(Context context) {
		super(context);
	}

	public OsmandImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		parseAttributes(this, attrs, 0, 0);
	}

	public OsmandImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		parseAttributes(this, attrs, defStyleAttr, 0);
	}

	@TargetApi(21)
	public OsmandImageButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		parseAttributes(this, attrs, defStyleAttr, defStyleRes);
	}

	static void parseAttributes(ImageButton target, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		if (attrs == null) {
			return;
		}

		TypedArray resolvedAttrs = target.getContext().getTheme().obtainStyledAttributes(attrs,
				R.styleable.OsmandButton, defStyleAttr, defStyleRes);
		applyAttributes(resolvedAttrs, target);
		resolvedAttrs.recycle();
	}

	private static void applyAttributes(TypedArray resolvedAttributes, ImageButton target) {
		applyAttribute_osmandSrc(resolvedAttributes, target);
	}

	static void applyAttribute_osmandSrc(TypedArray resolvedAttributes, ImageButton target) {
		if (!resolvedAttributes.hasValue(R.styleable.OsmandImageButton_osmandSrc)
				|| target.isInEditMode()) {
			return;
		}

		String osmandSrc = resolvedAttributes.getString(R.styleable.OsmandImageButton_osmandSrc);
		target.setImageDrawable(OsmandResources.getDrawable(osmandSrc));
	}
}
