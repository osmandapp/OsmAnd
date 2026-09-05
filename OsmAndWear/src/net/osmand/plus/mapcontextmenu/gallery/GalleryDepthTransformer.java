package net.osmand.plus.mapcontextmenu.gallery;

import android.view.View;

import androidx.viewpager.widget.ViewPager;

public class GalleryDepthTransformer implements ViewPager.PageTransformer {
	private static final float MIN_SCALE = 0.5f;

	@Override
	public void transformPage(View view, float position) {
		int pageWidth = view.getWidth();
		if (position < -1) {
			view.setAlpha(0f);
		} else if (position <= 0) {
			view.setAlpha(1f);
			view.setTranslationX(0f);
			view.setScaleX(1f);
			view.setScaleY(1f);
		} else if (position <= 1) {
			view.setAlpha(1 - position);
			view.setTranslationX(pageWidth * -position);
			float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
			view.setScaleX(scaleFactor);
			view.setScaleY(scaleFactor);
		} else {
			view.setAlpha(0f);
		}
	}
}