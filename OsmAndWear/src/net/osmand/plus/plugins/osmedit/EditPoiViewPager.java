package net.osmand.plus.plugins.osmedit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

public class EditPoiViewPager extends ViewPager {
	public EditPoiViewPager(Context context) {
		super(context);
	}

	public EditPoiViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int height = 0;
		View view = null;
		for (int i = 0; i < getChildCount(); i++) {
			view = getChildAt(i);
			view.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			int h = view.getMeasuredHeight();
			if (h > height) height = h;
		}

		if (height != 0) {
			heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(getMeasuredWidth(), measureHeight(heightMeasureSpec, view));
	}

	private int measureHeight(int measureSpec, View view) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			if (view != null) {
				result = view.getMeasuredHeight();
			}
			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}
		return result;
	}
}
