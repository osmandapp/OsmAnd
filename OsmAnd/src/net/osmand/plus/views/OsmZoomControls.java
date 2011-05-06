package net.osmand.plus.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ZoomControls;

public class OsmZoomControls extends ZoomControls {

	public OsmZoomControls(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OsmZoomControls(Context context) {
		super(context);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Ugly code : force buttons be equal size.
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		LayoutParams layoutParams = (android.widget.LinearLayout.LayoutParams) getChildAt(0).getLayoutParams();
		layoutParams.width = getMeasuredWidth()/ 2 ;
		layoutParams = (android.widget.LinearLayout.LayoutParams) getChildAt(1).getLayoutParams();
		layoutParams.width = getMeasuredWidth()/ 2 ;
		super.onMeasure(getMeasuredWidth(), getMeasuredHeight());
		
	}
	
	

}
