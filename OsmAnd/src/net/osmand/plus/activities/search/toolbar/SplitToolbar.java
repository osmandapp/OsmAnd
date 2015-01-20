package net.osmand.plus.activities.search.toolbar;

import android.content.Context;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Denis
 * on 20.01.2015.
 */
public class SplitToolbar extends Toolbar {
	public SplitToolbar(Context context) {
		super(context);
	}

	public SplitToolbar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SplitToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void addView(View child, ViewGroup.LayoutParams params) {
		if (child instanceof ActionMenuView) {
			params.width = LayoutParams.MATCH_PARENT;
		}
		super.addView(child, params);
	}
}
