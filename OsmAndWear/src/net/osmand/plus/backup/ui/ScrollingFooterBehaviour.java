package net.osmand.plus.backup.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.utils.AndroidUtils;

public class ScrollingFooterBehaviour extends AppBarLayout.ScrollingViewBehavior {

	private AppBarLayout appBar;

	public ScrollingFooterBehaviour() {
	}

	public ScrollingFooterBehaviour(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent,
	                                      @NonNull View child,
	                                      @NonNull View dependency) {
		if (appBar == null) {
			appBar = ((AppBarLayout) dependency);
		}

		boolean viewChanged = super.onDependentViewChanged(parent, child, dependency);
		int bottomPadding = appBar.getTop() + appBar.getTotalScrollRange()
				- AndroidUtils.getStatusBarHeight(parent.getContext());
		boolean paddingChanged = bottomPadding != child.getPaddingBottom();
		if (paddingChanged) {
			child.setPadding(child.getPaddingLeft(), child.getPaddingTop(), child.getPaddingRight(),
					bottomPadding);
			child.requestLayout();
		}

		return paddingChanged || viewChanged;
	}
}
