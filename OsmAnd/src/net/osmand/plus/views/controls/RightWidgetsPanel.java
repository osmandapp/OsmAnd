package net.osmand.plus.views.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.ViewPager2;

public class RightWidgetsPanel extends FrameLayout {

	private static final int BORDER_WIDTH_DP = 2;
	private static final int BORDER_RADIUS_DP = 5;

	private final Paint borderPaint = new Paint();
	private final Path borderPath = new Path();

	private boolean nightMode;

	private ViewPager2 viewPager;
	private WidgetsPagerAdapter adapter;
	private LinearLayout dots;

	public RightWidgetsPanel(@NonNull Context context) {
		super(context);
		init();
	}

	public RightWidgetsPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public RightWidgetsPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public RightWidgetsPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	private void init() {
		nightMode = getMyApplication().getDaynightHelper().isNightMode();
		setWillNotDraw(false);
		setupPaddings();
		setupBorderPaint();
		inflate(UiUtilities.getThemedContext(getContext(), nightMode), R.layout.right_widgets_panel, this);
		setupChildren();
	}

	private void setupPaddings() {
		int padding = AndroidUtils.dpToPx(getContext(), BORDER_WIDTH_DP);
		setPaddingRelative(padding, padding, 0, padding);
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

		adapter = new WidgetsPagerAdapter(getMyApplication());
		adapter.setViewHolderBindListener((viewHolder, index) -> {
			if (index == viewPager.getCurrentItem()) {
				WrapContentViewPager2Callback.resizeViewPagerToWrapContent(viewPager, viewHolder.container);
			}
		});
		viewPager = findViewById(R.id.view_pager);
		viewPager.setAdapter(adapter);
		// Set transformer just to update pages without RecyclerView animation
		viewPager.setPageTransformer(new CompositePageTransformer());
		viewPager.registerOnPageChangeCallback(new WrapContentViewPager2Callback(viewPager) {
			@Override
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				updateDots();
			}
		});

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

		dots.removeAllViews();

		for (int i = 0; i < pagesCount; i++) {
			ImageView dot = new ImageView(getContext());
			int dp3 = AndroidUtils.dpToPx(getContext(), 3);
			MarginLayoutParams dotParams = new ViewGroup.MarginLayoutParams(dp3, dp3);
			AndroidUtils.setMargins(dotParams, dp3, 0, dp3, 0);
			dot.setLayoutParams(dotParams);

			boolean currentPage = i == viewPager.getCurrentItem();
			int dotColor = getDotColorId(currentPage);
			dot.setImageDrawable(getIconsCache().getIcon(R.drawable.ic_dot_position, dotColor));
			dots.addView(dot);
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

	public void update() {
		adapter.updateIfNeeded();
		AndroidUiHelper.updateVisibility(this, adapter.getItemCount() > 0);
		updateDots();
	}

	public void updateColors(@NonNull TextState textState) {
		this.nightMode = textState.night;
		borderPaint.setColor(ContextCompat.getColor(getContext(), textState.rightBorderColorId));
		updateDots();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WrapContentViewPager2Callback.resizeViewPagerToWrapContent(viewPager, null);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		drawBorder(canvas);
	}

	private void drawBorder(@NonNull Canvas canvas) {
		boolean rtl = AndroidUtils.isLayoutRtl(getContext());
		float inset = (float) Math.ceil(borderPaint.getStrokeWidth() / 2);
		float screenEdgeX = rtl ? 0 : getWidth();
		float roundedCornerX = rtl ? getWidth() - inset : inset;
		float bottomY = getHeight() - inset;

		borderPath.reset();
		borderPath.moveTo(screenEdgeX, inset);
		borderPath.lineTo(roundedCornerX, inset);
		borderPath.lineTo(roundedCornerX, bottomY);
		borderPath.lineTo(screenEdgeX, bottomY);

		canvas.drawPath(borderPath, borderPaint);
	}

	@NonNull
	private UiUtilities getIconsCache() {
		return getMyApplication().getUIUtilities();
	}

	@NonNull
	private OsmandApplication getMyApplication() {
		return ((OsmandApplication) getContext().getApplicationContext());
	}
}