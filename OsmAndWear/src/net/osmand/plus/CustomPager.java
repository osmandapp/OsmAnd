package net.osmand.plus;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

import net.osmand.plus.views.OsmAndMapSurfaceView;

public class CustomPager extends ViewPager {

	public CustomPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomPager(Context context) {
		super(context);
	}

	@Override
	protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
		if (v instanceof OsmAndMapSurfaceView) {
			return true;
		}
		return super.canScroll(v, checkV, dx, x, y);
	}

}
