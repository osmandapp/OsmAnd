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
import androidx.core.graphics.Insets;
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

	private final OsmandApplication app;
	private final UiUtilities utilities;

	private final Path borderPath = new Path();
	private final Paint borderPaint = new Paint();

	private boolean nightMode;
	private boolean rightSide;
	private boolean selfShowAllowed;
	private boolean selfVisibilityChanging;
	private final boolean layoutRtl;

	private ViewPager2 viewPager;
	private WidgetsPagerAdapter adapter;
	private LinearLayout dots;

	private Insets insets;
	private int screenWidth = -1;
	private int screenHeight = -1;

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
		app = AndroidUtils.getApp(context);
		utilities = app.getUIUtilities();
		layoutRtl = AndroidUtils.isLayoutRtl(app);
		nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);

		definePanelSide(context, attrs);
		setWillNotDraw(false);
		setupPaddings();
		setupBorderPaint();
		inflate(context, R.layout.side_widgets_panel, this);
		setupChildren();
	}

	public boolean isRightSide() {
		return rightSide;
	}

	private void definePanelSide(@NonNull Context context, @Nullable AttributeSet attrs) {
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SideWidgetsPanel);
		rightSide = typedArray.getBoolean(R.styleable.SideWidgetsPanel_rightSide, true);
		typedArray.recycle();
	}

	private boolean isDetached() {
		boolean positionedOnLeft = layoutRtl ^ !rightSide;
		return insets != null && (positionedOnLeft ? insets.left : insets.right) > 0;
	}

	private void setupPaddings() {
		boolean detached = isDetached();
		int padding = AndroidUtils.dpToPx(getContext(), BORDER_WIDTH_DP);
		int startPadding = (rightSide || detached) ? padding : 0;
		int endPadding = (!rightSide || detached) ? padding : 0;
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
					app.runInUIThread(() -> wrapContentAroundPage(null));
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
		return new WidgetsPagerAdapter(getContext(), panel);
	}

	public void update(@Nullable DrawSettings drawSettings) {
		adapter.updateIfNeeded();
		boolean show = hasVisibleContent() && selfShowAllowed;
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

		Context context = getContext();
		int dotsBackgroundId = nightMode ? R.color.icon_color_secondary_dark : R.color.divider_color_light;
		dots.setBackgroundResource(dotsBackgroundId);

		if (dots.getChildCount() != pagesCount) {
			dots.removeAllViews();
			for (int i = 0; i < pagesCount; i++) {
				ImageView dot = new ImageView(context);
				int dp3 = AndroidUtils.dpToPx(context, 3);
				MarginLayoutParams dotParams = new ViewGroup.MarginLayoutParams(dp3, dp3);
				AndroidUtils.setMargins(dotParams, dp3, 0, dp3, 0);
				dot.setLayoutParams(dotParams);
				int dotColor = getDotColorId(i == viewPager.getCurrentItem());
				dot.setImageDrawable(utilities.getIcon(R.drawable.ic_dot_position, dotColor));
				dots.addView(dot);
			}
		} else {
			for (int i = 0; i < dots.getChildCount(); i++) {
				View childView = dots.getChildAt(i);
				if (childView instanceof ImageView dot) {
					int dotColor = getDotColorId(i == viewPager.getCurrentItem());
					dot.setImageDrawable(utilities.getIcon(R.drawable.ic_dot_position, dotColor));
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
	protected void dispatchDraw(@NonNull Canvas canvas) {
		super.dispatchDraw(canvas);

		if (hasVisibleContent()) {
			drawBorder(canvas);
		}
	}

	private boolean hasVisibleContent() {
		if (adapter != null) {
			VisiblePages visiblePages = adapter.getVisiblePages();
			List<View> views = visiblePages.getWidgetsViews(viewPager.getCurrentItem());
			if (!Algorithms.isEmpty(views)) {
				for (View view : views) {
					View emptyBanner = view.findViewById(R.id.empty_banner);
					if (view.findViewById(R.id.container).getVisibility() == VISIBLE
							|| (emptyBanner != null && emptyBanner.getVisibility() == VISIBLE)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void drawBorder(@NonNull Canvas canvas) {
		float halfStroke = borderPaint.getStrokeWidth() / 2f;
		float left = halfStroke;
		float top = halfStroke;
		float right = getWidth() - halfStroke;
		float bottom = getHeight() - halfStroke;

		boolean detached = isDetached();
		boolean positionedOnLeft = layoutRtl ^ !rightSide;

		borderPath.reset();
		if (detached) {
			borderPath.addRect(left, top, right, bottom, Path.Direction.CW);
		} else {
			float startX = positionedOnLeft ? 0 : getWidth();
			float outerX = positionedOnLeft ? right : left;

			borderPath.moveTo(startX, top);
			borderPath.lineTo(outerX, top);
			borderPath.lineTo(outerX, bottom);
			borderPath.lineTo(startX, bottom);
		}
		canvas.drawPath(borderPath, borderPaint);
	}

	@Override
	public void setVisibility(int visibility) {
		if (!selfVisibilityChanging) {
			selfShowAllowed = visibility == VISIBLE;
		}
		if (visibility == VISIBLE && !hasVisibleContent()) {
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
			View container = viewToWrap.findViewById(R.id.container);
			if (container != null) {
				viewToWrap = container;
			}

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

			if (screenHeight != -1) {
				int occupied = 0;
				if (insets != null) {
					occupied = insets.top + insets.bottom;
					occupied += getPaddingTop() + getPaddingBottom();
					if (getLayoutParams() instanceof MarginLayoutParams lp) {
						occupied += lp.topMargin + lp.bottomMargin;
					}
					int dotsHeight = getContext().getResources().getDimensionPixelSize(R.dimen.radius_large);
					occupied += dotsHeight;
				}
				int maxAllowedHeight = screenHeight - occupied;

				if (measuredHeight > maxAllowedHeight) {
					measuredHeight = maxAllowedHeight;
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

	public void setScreenSize(@NonNull Activity activity) {
		screenWidth = AndroidUtils.getScreenWidth(activity);
		screenHeight = AndroidUtils.getScreenHeight(activity);
	}

	public void setInsets(@NonNull Insets insets) {
		this.insets = insets;
		setupPaddings();
		wrapContentAroundPage(null);
	}
}