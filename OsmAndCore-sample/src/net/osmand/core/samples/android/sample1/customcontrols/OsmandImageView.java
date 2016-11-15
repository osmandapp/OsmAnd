package net.osmand.core.samples.android.sample1.customcontrols;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.R;

public class OsmandImageView extends ImageView {
	public OsmandImageView(Context context) {
		super(context);
	}

	public OsmandImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		parseAttributes(this, attrs, 0, 0);
	}

	public OsmandImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		parseAttributes(this, attrs, defStyleAttr, 0);
	}

	@TargetApi(21)
	public OsmandImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		parseAttributes(this, attrs, defStyleAttr, defStyleRes);
	}

	static void parseAttributes(ImageView target, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		if (attrs == null) {
			return;
		}

		TypedArray resolvedAttrs = target.getContext().getTheme().obtainStyledAttributes(attrs,
				R.styleable.OsmandButton, defStyleAttr, defStyleRes);
		applyAttributes(resolvedAttrs, target);
		resolvedAttrs.recycle();
	}

	private static void applyAttributes(TypedArray resolvedAttributes, ImageView target) {
		applyAttribute_osmandSrc(resolvedAttributes, target);
	}

	static void applyAttribute_osmandSrc(TypedArray resolvedAttributes, ImageView target) {
		if (!resolvedAttributes.hasValue(R.styleable.OsmandImageButton_osmandSrc)
				|| target.isInEditMode()) {
			return;
		}

		String osmandSrc = resolvedAttributes.getString(R.styleable.OsmandImageButton_osmandSrc);
		target.setImageDrawable(OsmandResources.getDrawable(osmandSrc));
	}
}
