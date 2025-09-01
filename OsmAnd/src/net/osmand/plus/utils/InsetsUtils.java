package net.osmand.plus.utils;

import android.content.Context;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.osmand.plus.R;

import java.util.EnumSet;

public class InsetsUtils {

	public static Insets getSysBars(@NonNull Context ctx, @Nullable WindowInsetsCompat insets) {
		if (!isEdgeToEdgeSupported()) {
			return Insets.of(0, AndroidUtils.getStatusBarHeight(ctx), 0, 0);
		} else {
			if (insets == null) {
				return null;
			} else {
				return insets.getInsets(WindowInsetsCompat.Type.systemBars());
			}
		}
	}

	public enum InsetSide {
		LEFT, TOP, RIGHT, BOTTOM
	}
	public static boolean isEdgeToEdgeSupported(){
		return Build.VERSION.SDK_INT > 29;
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

	public static void setWindowInsetsListener(@NonNull final View view,
	                                           @NonNull final EnumSet<InsetSide> sides) {
		setWindowInsetsListener(view, (v, insets) -> {
			applyPadding(v, insets, sides);
		}, false);
	}

	public static void applyPadding(@NonNull View view,
	                                @NonNull WindowInsetsCompat insets,
	                                @Nullable EnumSet<InsetSide> sides) {
		if (sides == null || !isEdgeToEdgeSupported()) {
			return;
		}
		Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

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

		view.setPadding(
				left ? initialLeft + sysBars.left : view.getPaddingLeft(),
				top ? initialTop + sysBars.top : view.getPaddingTop(),
				right ? initialRight + sysBars.right : view.getPaddingRight(),
				bottom ? initialBottom + sysBars.bottom : view.getPaddingBottom()
		);
	}

	public interface OnInsetsApplied {
		void onApply(@NonNull View view, @NonNull WindowInsetsCompat insets);
	}
}
