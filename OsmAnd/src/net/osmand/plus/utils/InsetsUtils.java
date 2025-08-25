package net.osmand.plus.utils;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.EnumSet;

public class InsetsUtils {

	public enum InsetSide {
		LEFT, TOP, RIGHT, BOTTOM
	}


	public static class InitialPadding {
		public final int left, top, right, bottom;

		public InitialPadding(@NonNull View view) {
			this.left = view.getPaddingLeft();
			this.top = view.getPaddingTop();
			this.right = view.getPaddingRight();
			this.bottom = view.getPaddingBottom();
		}
	}

	public static void doOnApplyWindowInsets(@NonNull final View view,
	                                         @NonNull final OnInsetsApplied callback) {
		final InitialPadding initial = new InitialPadding(view);

		ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
			callback.onApply(v, insets, initial);
			return insets;
		});

		if (view.isAttachedToWindow()) {
			ViewCompat.requestApplyInsets(view);
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

	public static void doOnApplyWindowInsets(@NonNull final View view,
	                                         @NonNull final EnumSet<InsetSide> sides) {
		doOnApplyWindowInsets(view, (v, insets, padding) -> {
			applyPadding(v, insets, padding, sides);
		});
	}

	public interface OnInsetsApplied {
		void onApply(@NonNull View view, @NonNull WindowInsetsCompat insets, @NonNull InitialPadding padding);
	}

	public static void applyPadding(@NonNull View view,
	                                @NonNull WindowInsetsCompat insets,
	                                @NonNull InitialPadding padding,
	                                @NonNull EnumSet<InsetSide> sides) {
		Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

		int left = sides.contains(InsetSide.LEFT) ? sysBars.left : 0;
		int top = sides.contains(InsetSide.TOP) ? sysBars.top : 0;
		int right = sides.contains(InsetSide.RIGHT) ? sysBars.right : 0;
		int bottom = sides.contains(InsetSide.BOTTOM) ? sysBars.bottom : 0;

		view.setPadding(
				padding.left + left,
				padding.top + top,
				padding.right + right,
				padding.bottom + bottom
		);
	}
}
