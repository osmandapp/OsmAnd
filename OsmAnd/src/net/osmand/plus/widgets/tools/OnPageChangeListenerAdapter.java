package net.osmand.plus.widgets.tools;

import androidx.viewpager.widget.ViewPager;

/**
 * Base class for scenarios where user wants to implement only one method of OnPageChangeListener.
 */
public class OnPageChangeListenerAdapter implements ViewPager.OnPageChangeListener {
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
	@Override
	public void onPageSelected(int position) {}
	@Override
	public void onPageScrollStateChanged(int state) {}
}
