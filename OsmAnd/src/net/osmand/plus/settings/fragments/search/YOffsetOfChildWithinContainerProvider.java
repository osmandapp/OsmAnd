package net.osmand.plus.settings.fragments.search;

import android.view.View;

class YOffsetOfChildWithinContainerProvider {

	private final View container;

	public static int getYOffsetOfChildWithinContainer(final View child, final View container) {
		return new YOffsetOfChildWithinContainerProvider(container).getYOffset(child);
	}

	private YOffsetOfChildWithinContainerProvider(final View container) {
		this.container = container;
	}

	private int getYOffset(final View view) {
		return view == container ?
				0 :
				view.getTop() + getYOffset((View) view.getParent());
	}
}
