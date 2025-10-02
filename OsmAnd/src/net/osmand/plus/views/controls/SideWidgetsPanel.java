package net.osmand.plus.views.controls;

import static androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.WidgetsPagerAdapter.VisiblePages;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.widgets.FrameLayoutEx;
import net.osmand.util.Algorithms;

import java.util.List;

public class SideWidgetsPanel extends FrameLayoutEx implements WidgetsContainer {

	private static final int BORDER_WIDTH_DP = 2;
	private static final int BORDER_RADIUS_DP = 5;
	private static final float SIDE_PANEL_WEIGHT_RATIO = 0.45f;
	private final Paint borderPaint = new Paint();
	private final Path borderPath = new Path();

	protected boolean nightMode;
	protected boolean rightSide;
	protected boolean selfShowAllowed;
	protected boolean selfVisibilityChanging;

	protected ViewPager2 viewPager;
	protected WidgetsPagerAdapter adapter;
	protected LinearLayout dots;

	private int screenWidth = -1;

	public SideWidgetsPanel(@NonNull Context context) {
		this(context, null);
	}

	public SideWidgetsPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SideWidgetsPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public SideWidgetsPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		nightMode = getMyApplication().getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		context = UiUtilities.getThemedContext(getContext(), nightMode);

		definePanelSide(context, attrs);
		setWillNotDraw(false);
		setupPaddings();
		setupBorderPaint();
		inflate(context, R.layout.side_widgets_panel, this);
		setupChildren();
	}

	private void definePanelSide(@NonNull Context context, @Nullable AttributeSet attrs) {
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SideWidgetsPanel);
		rightSide = typedArray.getBoolean(R.styleable.SideWidgetsPanel_rightSide, true);
		typedArray.recycle();
	}

	private void setupPaddings() {
		int padding = AndroidUtils.dpToPx(getContext(), BORDER_WIDTH_DP);
		int startPadding = rightSide ? padding : 0;
		int endPadding = rightSide ? 0 : padding;
		setPaddingRelative(startPadding, padding, endPadding, padding);
	}

	private void setupBorderPaint() {
		borderPaint.setDither(true);
		borderPaint.setAntiAlias(true);
		borderPaint.setStrokeWidth(AndroidUtils.dpToPx(getContext(), BORDER_WIDTH_DP));
		borderPaint.setStyle(Style.STROKE);
		borderPaint.setStrokeCap(Cap.BUTT);
		borderPaint.setPathEffect(new CornerPathEffect(AndroidUtils.dpToPx(getContext(), BORDER_RADIUS_DP)));
	}

	private void setupChildren() {
		dots = findViewById(R.id.dots);

		adapter = createPagerAdapter();
		adapter.setViewHolderBindListener((viewHolder, index) -> {
			if (index == viewPager.getCurrentItem()) {
				wrapContentAroundPage(viewHolder.itemView);
			}
		});

		viewPager = findViewById(R.id.view_pager);
		viewPager.setAdapter(adapter);
		// Set transformer just to update pages without RecyclerView animation
		viewPager.setPageTransformer(new CompositePageTransformer());
		viewPager.registerOnPageChangeCallback(new OnPageChangeCallback() {
			@Override
			public void onPageScrollStateChanged(int state) {
				if (state == SCROLL_STATE_IDLE) { // when dragging is ended
					runInUIThread(() -> {
						wrapContentAroundPage(null);
					});
				}
			}

			@Override
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				updateDots();
			}
		});
		updateDots();
	}

	protected WidgetsPagerAdapter createPagerAdapter() {
		WidgetsPanel panel = rightSide ? WidgetsPanel.RIGHT : WidgetsPanel.LEFT;
		return new WidgetsPagerAdapter(getMyApplication(), panel);
	}

	public void update(@Nullable DrawSettings drawSettings) {
		adapter.updateIfNeeded();
		boolean show = hasVisibleWidgets() && selfShowAllowed;
		selfVisibilityChanging = true;
		if (AndroidUiHelper.updateVisibility(this, show) && !show) {
			selfShowAllowed = true;
		}
		wrapContentAroundPage(null);
		selfVisibilityChanging = false;
		updateDots();
	}

	private void updateDots() {
		int pagesCount = adapter.getItemCount();
		boolean needDots = pagesCount > 1;
		AndroidUiHelper.updateVisibility(dots, needDots);
		if (!needDots) {
			return;
		}

		int dotsBackgroundId = nightMode ? R.color.icon_color_secondary_dark : R.color.divider_color_light;
		dots.setBackgroundResource(dotsBackgroundId);

		if (dots.getChildCount() != pagesCount) {
			dots.removeAllViews();
			for (int i = 0; i < pagesCount; i++) {
				ImageView dot = new ImageView(getContext());
				int dp3 = AndroidUtils.dpToPx(getContext(), 3);
				MarginLayoutParams dotParams = new ViewGroup.MarginLayoutParams(dp3, dp3);
				AndroidUtils.setMargins(dotParams, dp3, 0, dp3, 0);
				dot.setLayoutParams(dotParams);
				int dotColor = getDotColorId(i == viewPager.getCurrentItem());
				dot.setImageDrawable(getIconsCache().getIcon(R.drawable.ic_dot_position, dotColor));
				dots.addView(dot);
			}
		} else {
			for (int i = 0; i < dots.getChildCount(); i++) {
				View childView = dots.getChildAt(i);
				if (childView instanceof ImageView) {
					ImageView dot = (ImageView) childView;
					int dotColor = getDotColorId(i == viewPager.getCurrentItem());
					dot.setImageDrawable(getIconsCache().getIcon(R.drawable.ic_dot_position, dotColor));
				}
			}
		}
	}

	@ColorRes
	private int getDotColorId(boolean selected) {
		if (nightMode) {
			return selected ? R.color.icon_color_primary_dark : R.color.icon_color_default_dark;
		} else {
			return selected ? R.color.icon_color_primary_light : R.color.icon_color_secondary_light;
		}
	}

	public void updateColors(@NonNull TextState textState) {
		this.nightMode = textState.night;
		borderPaint.setColor(ContextCompat.getColor(getContext(), textState.panelBorderColorId));
		updateDots();
		invalidate();
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		if (hasVisibleWidgets()) {
			drawBorder(canvas);
		}
	}

	private boolean hasVisibleWidgets() {
		if (adapter != null) {
			VisiblePages visiblePages = adapter.getVisiblePages();
			List<View> views = visiblePages.getWidgetsViews(viewPager.getCurrentItem());
			if (!Algorithms.isEmpty(views)) {
				for (View view : views) {
					View emptyBanner = view.findViewById(R.id.empty_banner);
					if (view.findViewById(R.id.container).getVisibility() == VISIBLE
							|| (emptyBanner != null &&  emptyBanner.getVisibility() == VISIBLE)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void drawBorder(@NonNull Canvas canvas) {
		boolean rtl = AndroidUtils.isLayoutRtl(getContext());
		boolean positionedOnLeft = rtl ^ !rightSide;
		float inset = (float) Math.ceil(borderPaint.getStrokeWidth() / 2);
		float screenEdgeX = positionedOnLeft ? 0 : getWidth();
		float roundedCornerX = positionedOnLeft ? getWidth() - inset : inset;
		float bottomY = getHeight() - inset;

		borderPath.reset();
		borderPath.moveTo(screenEdgeX, inset);
		borderPath.lineTo(roundedCornerX, inset);
		borderPath.lineTo(roundedCornerX, bottomY);
		borderPath.lineTo(screenEdgeX, bottomY);

		canvas.drawPath(borderPath, borderPaint);
	}

	@Override
	public void setVisibility(int visibility) {
		if (!selfVisibilityChanging) {
			selfShowAllowed = visibility == VISIBLE;
		}
		if (visibility == VISIBLE && !hasVisibleWidgets()) {
			return;
		}
		super.setVisibility(visibility);
	}

	/**
	 * @param viewToWrap pass null if this param can be fetched only from RecyclerView
	 */
	private void wrapContentAroundPage(@Nullable View viewToWrap) {
		if (viewToWrap == null) {
			viewToWrap = getCurrentPageView();
		}
		if (viewToWrap != null) {
			int unspecifiedSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
			viewToWrap.measure(unspecifiedSpec, unspecifiedSpec);

			int width = viewPager.getWidth();
			int height = viewPager.getHeight();
			int measuredWidth = viewToWrap.getMeasuredWidth();
			int measuredHeight = viewToWrap.getMeasuredHeight();

			if (screenWidth != -1) {
				int maxAllowedWidth = (int) (screenWidth * SIDE_PANEL_WEIGHT_RATIO);

				if (measuredWidth > maxAllowedWidth) {
					measuredWidth = maxAllowedWidth;
				}
			}

			if (width != measuredWidth || height != measuredHeight) {
				ViewGroup.LayoutParams pagerParams = viewPager.getLayoutParams();
				pagerParams.width = measuredWidth;
				pagerParams.height = measuredHeight;
				viewPager.setLayoutParams(pagerParams);
			}
		}
	}

	@Nullable
	private View getCurrentPageView() {
		RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
		ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(viewPager.getCurrentItem());
		return viewHolder != null ? viewHolder.itemView : null;
	}

	private void runInUIThread(@NonNull Runnable runnable) {
		getMyApplication().runInUIThread(runnable);
	}

	@NonNull
	protected UiUtilities getIconsCache() {
		return getMyApplication().getUIUtilities();
	}

	@NonNull
	protected OsmandApplication getMyApplication() {
		return ((OsmandApplication) getContext().getApplicationContext());
	}

	public void setScreenWidth(@NonNull Activity activity) {
		screenWidth = AndroidUtils.getScreenWidth(activity);
	}
}