package net.osmand.plus;

import net.osmand.plus.views.OsmAndMapSurfaceView;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

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
