package net.osmand.plus.utils;

import static net.osmand.plus.helpers.AndroidUiHelper.processSystemBarScrims;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.ISupportInsets;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.EnumSet;
import java.util.List;
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

	public static void applyPadding(@NonNull View view,
	                                @NonNull WindowInsetsCompat insets,
	                                @Nullable Set<InsetSide> sides) {
		if (sides == null || !isEdgeToEdgeSupported()) {
			return;
		}
		Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

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
		} else {
			view.setPadding(
					left ? initialLeft + sysBars.left : view.getPaddingLeft(),
					top ? initialTop + sysBars.top : view.getPaddingTop(),
					right ? initialRight + sysBars.right : view.getPaddingRight(),
					bottom ? initialBottom + sysBars.bottom : view.getPaddingBottom()
			);
		}
	}

	public static void processInsets(@NonNull ISupportInsets insetSupportedFragment, @NonNull View rootView, @Nullable View paddingsView) {
		Set<InsetSide> insetSides = insetSupportedFragment.getRootInsetSides();
		List<Integer> rootScrollableIds = insetSupportedFragment.getScrollableViewIds();
		List<Integer> bottomContainers = insetSupportedFragment.getBottomContainersIds();
		List<Integer> collapsingAppBarLayoutIds = insetSupportedFragment.getCollapsingAppBarLayoutId();
		List<Integer> fabs = insetSupportedFragment.getFabIds();

		InsetsUtils.setWindowInsetsListener(rootView, (v, insets) -> {
			View processedView = paddingsView != null ? paddingsView : v;
			processScrollInsets(insets, rootScrollableIds, processedView);
			processBottomContainerInsets(insets, bottomContainers, processedView);
			processCollapsingAppBarLayoutInsets(insets, collapsingAppBarLayoutIds, insetSides, processedView);
			processFabInsets(insets, fabs, processedView);
			processRootInsetSides(insetSupportedFragment, insets, insetSides, processedView);
			processSystemBarScrims(insets, v);

			insetSupportedFragment.setLastRootInsets(insets);
			insetSupportedFragment.onApplyInsets(insets);
		}, true);
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

	public static void processRootInsetSides(ISupportInsets insetSupportedFragment, @NonNull WindowInsetsCompat insets,
	                                          @Nullable Set<InsetSide> insetSides,
	                                          @NonNull View view){
		if (insetSupportedFragment instanceof BaseOsmAndDialogFragment dialogFragment) {
			if (dialogFragment.getDialog() != null && dialogFragment.getShowsDialog()) {
				InsetsUtils.applyPadding(view, insets, insetSides);
			}
		} else {
			InsetsUtils.applyPadding(view, insets, insetSides);
		}
	}

	public static void processFabInsets(@NonNull WindowInsetsCompat insets,
	                                     @Nullable List<Integer> fabs,
	                                     @NonNull View view){
		Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

		View fab = null;
		if (fabs != null) {
			for (int id : fabs) {
				fab = view.findViewById(id);
				if (fab != null) break;
			}
		}

		if (fab != null) {
			if (fab instanceof ViewGroup viewGroup) {
				viewGroup.setClipToPadding(false);
			}
			ViewGroup.LayoutParams params = fab.getLayoutParams();

			if (params instanceof RelativeLayout.LayoutParams p1) {
				int oldMargin = p1.bottomMargin;
				int marginBottom = (Integer) (view.getTag(R.id.initial_margin_bottom) != null
						? view.getTag(R.id.initial_margin_bottom)
						: oldMargin);

				if (view.getTag(R.id.initial_margin_bottom) == null) {
					view.setTag(R.id.initial_margin_bottom, oldMargin);
				}
				p1.bottomMargin = marginBottom + sysBars.bottom;
				fab.setLayoutParams(p1);
			} else if (params instanceof CoordinatorLayout.LayoutParams p1) {
				int oldMargin = p1.bottomMargin;
				int marginBottom = (Integer) (view.getTag(R.id.initial_margin_bottom) != null
						? view.getTag(R.id.initial_margin_bottom)
						: oldMargin);

				if (view.getTag(R.id.initial_margin_bottom) == null) {
					view.setTag(R.id.initial_margin_bottom, oldMargin);
				}
				p1.bottomMargin = marginBottom + sysBars.bottom;
				fab.setLayoutParams(p1);
			}

		}
	}

	public static void processCollapsingAppBarLayoutInsets(@NonNull WindowInsetsCompat insets,
	                                                       @Nullable List<Integer> bottomContainers,
	                                                       Set<InsetSide> insetSides, @NonNull View view) {
		View bottomContainer = null;
		if (bottomContainers != null) {
			for (int id : bottomContainers) {
				bottomContainer = view.findViewById(id);
				if (bottomContainer != null) break;
			}
		}

		if (bottomContainer != null) {
			InsetsUtils.applyPadding(bottomContainer, insets, EnumSet.of(InsetSide.TOP));
			if (insetSides != null) {
				insetSides.remove(InsetSide.TOP);
			}
		}
	}

	public static void processBottomContainerInsets(@NonNull WindowInsetsCompat insets,
	                                                 @Nullable List<Integer> bottomContainers,
	                                                 @NonNull View view){
		Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());

		View bottomContainer = null;
		if (bottomContainers != null) {
			for (int id : bottomContainers) {
				bottomContainer = view.findViewById(id);
				if (bottomContainer != null) break;
			}
		}

		if (bottomContainer != null) {
			if (bottomContainer instanceof ViewGroup viewGroup) {
				viewGroup.setClipToPadding(false);
			}
			InsetsUtils.applyPadding(bottomContainer, insets, EnumSet.of(InsetSide.BOTTOM));
			ViewGroup.LayoutParams layoutParams = bottomContainer.getLayoutParams();
			int oldHeight = layoutParams.height;
			if (oldHeight != ViewGroup.LayoutParams.MATCH_PARENT && oldHeight != ViewGroup.LayoutParams.WRAP_CONTENT) {
				int initialHeight = (Integer) (view.getTag(R.id.initial_height) != null
						? view.getTag(R.id.initial_height)
						: oldHeight);

				if (view.getTag(R.id.initial_height) == null) {
					view.setTag(R.id.initial_height, oldHeight);
				}
				layoutParams.height = initialHeight + sysBars.bottom;
				bottomContainer.setLayoutParams(layoutParams);
			}
		}
	}

	public static void processScrollInsets(@NonNull WindowInsetsCompat insets,
	                                        @Nullable List<Integer> rootScrollableIds,
	                                        @NonNull View view) {
		View listView = null;
		if (rootScrollableIds != null) {
			for (int id : rootScrollableIds) {
				listView = view.findViewById(id);
				if (listView != null) break;
			}
		}
		if (listView != null) {
			if (listView instanceof ViewGroup viewGroup) {
				viewGroup.setClipToPadding(false);
			}
			InsetsUtils.applyPadding(listView, insets, EnumSet.of(InsetSide.BOTTOM));
		}
	}
}
