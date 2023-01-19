package net.osmand.plus.views.controls;

import static androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE;

import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import net.osmand.plus.OsmandApplication;

import java.lang.ref.WeakReference;

/**
 * Callback for {@link androidx.viewpager2.widget.ViewPager2} to wrap itself around
 * selected page
 */
public class WrapContentViewPager2Callback extends OnPageChangeCallback {

	private final OsmandApplication app;
	private final WeakReference<ViewPager2> viewPagerRef;

	public WrapContentViewPager2Callback(@NonNull ViewPager2 viewPager) {
		viewPagerRef = new WeakReference<>(viewPager);
		app = (OsmandApplication) viewPager.getContext().getApplicationContext();
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		boolean resizeAllowed = state == SCROLL_STATE_IDLE;

		app.runInUIThread(() -> {
			ViewPager2 viewPager = viewPagerRef.get();
			if (resizeAllowed && viewPager != null) {
				resizeViewPagerToWrapContent(viewPager, null);
			}
		});
	}

	@Override
	public void onPageSelected(int position) {

	}

	/**
	 * @param viewToWrap pass null if this param can be fetched only from RecyclerView
	 */
	public static void resizeViewPagerToWrapContent(@NonNull ViewPager2 viewPager, @Nullable View viewToWrap) {
		if (viewToWrap == null) {
			viewToWrap = getCurrentPageView(viewPager);
		}
		if (viewToWrap != null) {
			int unspecifiedSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			viewToWrap.measure(unspecifiedSpec, unspecifiedSpec);

			int width = viewPager.getWidth();
			int height = viewPager.getHeight();
			int measuredWidth = viewToWrap.getMeasuredWidth();
			int measuredHeight = viewToWrap.getMeasuredHeight();

			if (width != measuredWidth || height != measuredHeight) {
				LayoutParams pagerParams = viewPager.getLayoutParams();
				pagerParams.width = measuredWidth;
				pagerParams.height = measuredHeight;
				viewPager.setLayoutParams(pagerParams);
			}
		}
	}

	@Nullable
	public static View getCurrentPageView(@NonNull ViewPager2 viewPager) {
		RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
		ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(viewPager.getCurrentItem());
		return viewHolder != null ? viewHolder.itemView : null;
	}
}