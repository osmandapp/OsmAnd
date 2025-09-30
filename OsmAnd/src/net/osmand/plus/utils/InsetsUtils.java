package net.osmand.plus.utils;

import static net.osmand.plus.helpers.AndroidUiHelper.processSystemBarScrims;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.osmand.plus.R;
import net.osmand.plus.base.ISupportInsets;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.InsetTarget.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class InsetsUtils {

	public static final int MINIMUM_EDGE_TO_EDGE_SUPPORTED_API = 30;

	public enum InsetSide {
		LEFT, TOP, RIGHT, BOTTOM, RESET
	}

	public interface OnInsetsApplied {
		void onApply(@NonNull View view, @NonNull WindowInsetsCompat insets);
	}

	public static boolean isEdgeToEdgeSupported(){
		return Build.VERSION.SDK_INT >= MINIMUM_EDGE_TO_EDGE_SUPPORTED_API;
	}

	@Nullable
	public static Insets getSysBars(@NonNull Context ctx, @Nullable WindowInsetsCompat insets) {
		if (!isEdgeToEdgeSupported()) {
			return Insets.of(0, AndroidUtils.getStatusBarHeight(ctx), 0, 0);
		}
		return insets != null ? insets.getInsets(WindowInsetsCompat.Type.systemBars()) : null;
	}

	public static void setWindowInsetsListener(@NonNull final View view,
	                                           @NonNull final OnInsetsApplied callback,
	                                           boolean consume) {
		if (!isEdgeToEdgeSupported()) {
			return;
		}

		ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
			callback.onApply(v, insets);
			return consume ? WindowInsetsCompat.CONSUMED : insets;
		});

		if (view.isAttachedToWindow()) {
			WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
			if (insets != null) {
				callback.onApply(view, insets);
			} else {
				ViewCompat.requestApplyInsets(view);
			}
		} else {
			view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
				@Override
				public void onViewAttachedToWindow(@NonNull View v) {
					v.removeOnAttachStateChangeListener(this);
					ViewCompat.requestApplyInsets(v);
				}

				@Override
				public void onViewDetachedFromWindow(@NonNull View v) {}
			});
		}
	}

	public static void setWindowInsetsListener(@NonNull View view, @Nullable Set<InsetSide> sides) {
		setWindowInsetsListener(view, (v, insets) -> applyPadding(v, insets, sides), false);
	}

	public static void applyPadding(@NonNull View rootView,
	                                @NonNull WindowInsetsCompat insets,
	                                @NonNull InsetTarget insetTarget) {
		EnumSet<InsetSide> sides = insetTarget.getSides(isLandscape(rootView.getContext()));
		for (View view : resolveViews(rootView, insetTarget)) {
			if (view != null) {
				applyPadding(view, insets, sides);
			}
		}
	}

	public static void applyPadding(@NonNull View view,
	                                @NonNull WindowInsetsCompat insets,
	                                @Nullable Set<InsetSide> sides) {
		if (sides == null || !isEdgeToEdgeSupported()) {
			return;
		}
		Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

		boolean resetToInitial = sides.contains(InsetSide.RESET);
		boolean left = sides.contains(InsetSide.LEFT);
		boolean top = sides.contains(InsetSide.TOP);
		boolean right = sides.contains(InsetSide.RIGHT);
		boolean bottom = sides.contains(InsetSide.BOTTOM);

		final int initialLeft = (Integer) (view.getTag(R.id.initial_padding_left) != null
				? view.getTag(R.id.initial_padding_left)
				: view.getPaddingLeft());
		final int initialTop = (Integer) (view.getTag(R.id.initial_padding_top) != null
				? view.getTag(R.id.initial_padding_top)
				: view.getPaddingTop());
		final int initialRight = (Integer) (view.getTag(R.id.initial_padding_right) != null
				? view.getTag(R.id.initial_padding_right)
				: view.getPaddingRight());
		final int initialBottom = (Integer) (view.getTag(R.id.initial_padding_bottom) != null
				? view.getTag(R.id.initial_padding_bottom)
				: view.getPaddingBottom());

		if (view.getTag(R.id.initial_padding_left) == null) {
			view.setTag(R.id.initial_padding_left, initialLeft);
			view.setTag(R.id.initial_padding_top, initialTop);
			view.setTag(R.id.initial_padding_right, initialRight);
			view.setTag(R.id.initial_padding_bottom, initialBottom);
		}

		if (resetToInitial) {
			view.setPadding(
					initialLeft,
					initialTop,
					initialRight,
					initialBottom
			);
		}
		view.setPadding(
				left ? initialLeft + sysBars.left : view.getPaddingLeft(),
				top ? initialTop + sysBars.top : view.getPaddingTop(),
				right ? initialRight + sysBars.right : view.getPaddingRight(),
				bottom ? initialBottom + sysBars.bottom : view.getPaddingBottom()
		);
	}

	public static void processInsets(@NonNull ISupportInsets insetSupportedFragment, @NonNull View rootView, @Nullable View paddingsView) {
		InsetTargetsCollection targetsCollection = insetSupportedFragment.getInsetTargets();

		InsetsUtils.setWindowInsetsListener(rootView, (v, insets) -> {
			View processedView = paddingsView != null ? paddingsView : v;
			processInsets(processedView, targetsCollection, insets);
			processSystemBarScrims(insets, v);

			insetSupportedFragment.setLastRootInsets(insets);
			insetSupportedFragment.onApplyInsets(insets);
		}, true);
	}

	public static void processInsets(View root, InsetTargetsCollection collection, @NonNull WindowInsetsCompat insets) {
		for (InsetTarget target : collection.getAll()) {
			if (Objects.requireNonNull(target.getType()) == Type.COLLAPSING_APPBAR) {
				applyAppBarWithCollapseInsets(root, target, insets, collection);
			} else {
				applyCustomInsets(root, target, insets);
			}
		}

		for (InsetTarget target : collection.getByType(Type.ROOT_INSET)) {
			applyRootInsetsPaddings(root, target, insets);
		}
	}

	private static void applyAppBarWithCollapseInsets(View root, InsetTarget target, WindowInsetsCompat insets, InsetTargetsCollection collection) {
		for (View view : resolveViews(root, target)) {
			if (view != null) {
				EnumSet<InsetSide> sides = target.getSides(isLandscape(view.getContext()));
				applyPadding(view, insets, sides);

				for(InsetTarget insetTargets : collection.getByType(Type.ROOT_INSET)){
					EnumSet<InsetsUtils.InsetSide> insetSides = insetTargets.getSides(isLandscape(view.getContext()));
					if (insetSides != null){
						insetSides.remove(InsetSide.TOP);
					}
				}
			}
		}
	}

	private static void applyRootInsetsPaddings(View root, InsetTarget target, WindowInsetsCompat insets) {
		if (root != null) {
			EnumSet<InsetSide> sides = target.getSides(isLandscape(root.getContext()));
			applyPadding(root, insets, sides);
		}
	}

	public static boolean isLandscape(Context context) {
		return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
	}

	private static void applyCustomInsets(View root, InsetTarget target, WindowInsetsCompat insets) {
		for (View view : resolveViews(root, target)) {
			if (view != null) {
				EnumSet<InsetSide> sides = target.getSides(isLandscape(view.getContext()));
				if (target.isClipToPadding()) {
					applyClipPadding(view);
				}
				if (target.isPreferMargin()) {
					applyMargin(view, target, sides, insets);
				}

				if (target.isApplyPadding()) {
					applyRootInsetsPaddings(view, target, insets);
				}

				if(target.isAdjustHeight()){
					applyHeightAdjust(view, target, sides, insets);
				}

				if(target.isAdjustWidth()){
					applyWidthAdjust(view, target, sides, insets);
				}
			}
		}
	}

	private static void applyWidthAdjust(View view, InsetTarget target, EnumSet<InsetSide> sides, @NonNull WindowInsetsCompat insets) {
		if (!isLandscape(view.getContext())) {
			return;
		}
		Insets sysBars = insets.getInsets(target.getTypeMask());
		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		int oldWidth = layoutParams.width;
		if (oldWidth != ViewGroup.LayoutParams.MATCH_PARENT && oldWidth != ViewGroup.LayoutParams.WRAP_CONTENT) {
			int initialWidth = (Integer) (view.getTag(R.id.initial_width) != null
					? view.getTag(R.id.initial_width)
					: oldWidth);

			if (view.getTag(R.id.initial_width) == null) {
				view.setTag(R.id.initial_width, oldWidth);
			}

			boolean leftSide = sides.contains(InsetSide.LEFT);
			layoutParams.width = initialWidth + (leftSide ? sysBars.left : sysBars.right);
			view.setLayoutParams(layoutParams);
		}
	}

	private static void applyHeightAdjust(View view, InsetTarget target, EnumSet<InsetSide> sides, @NonNull WindowInsetsCompat insets) {
		Insets sysBars = insets.getInsets(target.getTypeMask());
		ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
		int oldHeight = layoutParams.height;
		if (oldHeight != ViewGroup.LayoutParams.MATCH_PARENT && oldHeight != ViewGroup.LayoutParams.WRAP_CONTENT) {
			int initialHeight = (Integer) (view.getTag(R.id.initial_height) != null
					? view.getTag(R.id.initial_height)
					: oldHeight);

			if (view.getTag(R.id.initial_height) == null) {
				view.setTag(R.id.initial_height, oldHeight);
			}
			layoutParams.height = initialHeight + sysBars.bottom;
			view.setLayoutParams(layoutParams);
		}
	}

	private static void applyMargin(View view, InsetTarget target, EnumSet<InsetSide> sides, @NonNull WindowInsetsCompat insets) {
		Insets sysBars = insets.getInsets(target.getTypeMask());
		applyClipPadding(view);
		ViewGroup.LayoutParams params = view.getLayoutParams();

		int marginTop;
		int marginBottom;
		int marginRight;
		int marginLeft;

		boolean left = sides.contains(InsetSide.LEFT);
		boolean top = sides.contains(InsetSide.TOP);
		boolean right = sides.contains(InsetSide.RIGHT);
		boolean bottom = sides.contains(InsetSide.BOTTOM);

		if (params instanceof RelativeLayout.LayoutParams p1) {
			marginTop = p1.topMargin;
			marginBottom = p1.bottomMargin;
			marginRight = p1.rightMargin;
			marginLeft = p1.leftMargin;
			final int initialLeft = (Integer) (view.getTag(R.id.initial_margin_left) != null
					? view.getTag(R.id.initial_margin_left)
					: marginLeft);
			final int initialTop = (Integer) (view.getTag(R.id.initial_margin_top) != null
					? view.getTag(R.id.initial_margin_top)
					: marginTop);
			final int initialRight = (Integer) (view.getTag(R.id.initial_margin_right) != null
					? view.getTag(R.id.initial_margin_right)
					: marginRight);
			final int initialBottom = (Integer) (view.getTag(R.id.initial_margin_bottom) != null
					? view.getTag(R.id.initial_margin_bottom)
					: marginBottom);

			if (view.getTag(R.id.initial_margin_left) == null) {
				view.setTag(R.id.initial_margin_left, initialLeft);
				view.setTag(R.id.initial_margin_top, initialTop);
				view.setTag(R.id.initial_margin_right, initialRight);
				view.setTag(R.id.initial_margin_bottom, initialBottom);
			}
			p1.bottomMargin = bottom ? initialBottom + sysBars.bottom : initialBottom;
			p1.leftMargin = left ? initialLeft + sysBars.left : initialLeft;
			p1.rightMargin = right ? initialRight + sysBars.right : initialRight;
			p1.topMargin = top ? initialTop + sysBars.top : initialTop;
			view.setLayoutParams(p1);
		} else if (params instanceof CoordinatorLayout.LayoutParams p1) {
			marginTop = p1.topMargin;
			marginBottom = p1.bottomMargin;
			marginRight = p1.rightMargin;
			marginLeft = p1.leftMargin;

			final int initialLeft = (Integer) (view.getTag(R.id.initial_margin_left) != null
					? view.getTag(R.id.initial_margin_left)
					: marginLeft);
			final int initialTop = (Integer) (view.getTag(R.id.initial_margin_top) != null
					? view.getTag(R.id.initial_margin_top)
					: marginTop);
			final int initialRight = (Integer) (view.getTag(R.id.initial_margin_right) != null
					? view.getTag(R.id.initial_margin_right)
					: marginRight);
			final int initialBottom = (Integer) (view.getTag(R.id.initial_margin_bottom) != null
					? view.getTag(R.id.initial_margin_bottom)
					: marginBottom);

			if (view.getTag(R.id.initial_margin_left) == null) {
				view.setTag(R.id.initial_margin_left, initialLeft);
				view.setTag(R.id.initial_margin_top, initialTop);
				view.setTag(R.id.initial_margin_right, initialRight);
				view.setTag(R.id.initial_margin_bottom, initialBottom);
			}
			p1.bottomMargin = bottom ? initialBottom + sysBars.bottom : initialBottom;
			p1.leftMargin = left ? initialLeft + sysBars.left : initialLeft;
			p1.rightMargin = right ? initialRight + sysBars.right : initialRight;
			p1.topMargin = top ? initialTop + sysBars.top : initialTop;
			view.setLayoutParams(p1);
		} else if (params instanceof FrameLayout.LayoutParams p1) {
			marginTop = p1.topMargin;
			marginBottom = p1.bottomMargin;
			marginRight = p1.rightMargin;
			marginLeft = p1.leftMargin;
			final int initialLeft = (Integer) (view.getTag(R.id.initial_margin_left) != null
					? view.getTag(R.id.initial_margin_left)
					: marginLeft);
			final int initialTop = (Integer) (view.getTag(R.id.initial_margin_top) != null
					? view.getTag(R.id.initial_margin_top)
					: marginTop);
			final int initialRight = (Integer) (view.getTag(R.id.initial_margin_right) != null
					? view.getTag(R.id.initial_margin_right)
					: marginRight);
			final int initialBottom = (Integer) (view.getTag(R.id.initial_margin_bottom) != null
					? view.getTag(R.id.initial_margin_bottom)
					: marginBottom);

			if (view.getTag(R.id.initial_margin_left) == null) {
				view.setTag(R.id.initial_margin_left, initialLeft);
				view.setTag(R.id.initial_margin_top, initialTop);
				view.setTag(R.id.initial_margin_right, initialRight);
				view.setTag(R.id.initial_margin_bottom, initialBottom);
			}
			p1.bottomMargin = bottom ? initialBottom + sysBars.bottom : initialBottom;
			p1.leftMargin = left ? initialLeft + sysBars.left : initialLeft;
			p1.rightMargin = right ? initialRight + sysBars.right : initialRight;
			p1.topMargin = top ? initialTop + sysBars.top : initialTop;
			view.setLayoutParams(p1);
		}
	}

	private static void applyClipPadding(View view) {
		if (view instanceof ViewGroup viewGroup) {
			viewGroup.setClipToPadding(false);
		}
	}

	private static List<View> resolveViews(View root, InsetTarget target) {
		List<View> result = new ArrayList<>();
		if (target.getViews() != null) {
			result.addAll(Arrays.asList(target.getViews()));
		}
		if (target.getViewIds() != null) {
			for (int id : target.getViewIds()) {
				View v = root.findViewById(id);
				if (v != null) result.add(v);
			}
		}
		return result;
	}

	public static void processNavBarColor(@NonNull ISupportInsets insetSupportedFragment) {
		int colorId = insetSupportedFragment.getNavigationBarColorId();
		Activity activity = insetSupportedFragment.requireActivity();
		if (colorId != -1) {
			AndroidUiHelper.setNavigationBarColor(activity, activity.getColor(colorId));
		} else {
			AndroidUiHelper.setNavigationBarColor(activity, Color.TRANSPARENT);
		}
	}
}
