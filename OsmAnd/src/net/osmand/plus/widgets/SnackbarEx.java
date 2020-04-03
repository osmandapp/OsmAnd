package net.osmand.plus.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.BaseTransientBottomBar;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import static android.view.View.VISIBLE;

public class SnackbarEx extends BaseTransientBottomBar<SnackbarEx> {

	private TextView messageView;
	private Button actionView;

	private SnackbarEx(
			@NonNull ViewGroup parent,
			@NonNull View content,
			@NonNull com.google.android.material.snackbar.ContentViewCallback contentViewCallback) {
		super(parent, content, contentViewCallback);
	}

	public static SnackbarEx make(@NonNull View v, @NonNull CharSequence text, @Duration int duration) {
		return make(v, text, duration, true);
	}

	public static SnackbarEx make(@NonNull View v, @NonNull CharSequence text, @Duration int duration, boolean inlineActionButton) {

		// inflate custom layout
		ViewGroup parent = findSuitableParent(v);
		if (parent == null) {
			throw new IllegalArgumentException(
					"No suitable parent found from the given view. Please provide a valid view.");
		}
		int layoutResId = inlineActionButton ? R.layout.snackbar_layout_inline_action_btn : R.layout.snackbar_layout;
		Context ctx = new ContextThemeWrapper(parent.getContext(), R.style.OsmandMaterialLightTheme);
//		ctx = parent.getContext();
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View view = inflater.inflate(layoutResId, parent, false);

		// create with custom view
		ContentViewCallback callback= new ContentViewCallback(view);
		int backgroundColor = false ? R.color.navigation_bar_bg_dark : R.color.navigation_bar_bg_light;
		Drawable drawable = ((OsmandApplication)ctx.getApplicationContext()).getUIUtilities().getIcon(R.drawable.rounded_background_3dp, backgroundColor);
		view.setBackgroundDrawable(drawable);
		SnackbarEx customSnackbar = new SnackbarEx(parent, view, callback);
		customSnackbar.setText(text);
		customSnackbar.setDuration(duration);
		customSnackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
		ViewCompat.setElevation(view, 6f);
//		addMargins(customSnackbar);

		return customSnackbar;
	}

	private static void addMargins(SnackbarEx snack) {
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snack.getView().getLayoutParams();
		params.setMargins(12, 12, 12, 12);
		snack.getView().setLayoutParams(params);
	}

	public void setText(@NonNull CharSequence message) {
		final TextView tv = (TextView) getMessageView();
		tv.setText(message);
	}

	public View getMessageView() {
		if (messageView == null) {
			messageView = getView().findViewById(R.id.snackbar_text);
		}
		return messageView;
	}

	public View getActionView() {
		if (actionView == null) {
			actionView = getView().findViewById(R.id.snackbar_btn);
		}
		return actionView;
	}

	public SnackbarEx setAction(@StringRes int resId, View.OnClickListener listener) {
		return setAction(getContext().getText(resId), listener);
	}

	public SnackbarEx setAction(@Nullable CharSequence text, @Nullable final View.OnClickListener listener) {
		final TextView tv = (TextView) getActionView();
		if (TextUtils.isEmpty(text) || listener == null) {
			tv.setVisibility(View.GONE);
			tv.setOnClickListener(null);
		} else {
			tv.setVisibility(View.VISIBLE);
			tv.setText(text);
			tv.setOnClickListener(
					new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							listener.onClick(view);
							dispatchDismiss(BaseCallback.DISMISS_EVENT_ACTION);
						}
					});
		}
		return this;
	}

	@Nullable
	private static ViewGroup findSuitableParent(View view) {
		ViewGroup fallback = null;
		do {
			if (view instanceof CoordinatorLayout) {
				// We've found a CoordinatorLayout, use it
				return (ViewGroup) view;
			} else if (view instanceof FrameLayout) {
				if (view.getId() == android.R.id.content) {
					// If we've hit the decor content view, then we didn't find a CoL in the
					// hierarchy, so use it.
					return (ViewGroup) view;
				} else {
					// It's not the content view but we'll use it as our fallback
					fallback = (ViewGroup) view;
				}
			}

			if (view != null) {
				// Else, we will loop and crawl up the view hierarchy and try to find a parent
				final ViewParent parent = view.getParent();
				view = parent instanceof View ? (View) parent : null;
			}
		} while (view != null);

		// If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
		return fallback;
	}

	private static class ContentViewCallback implements com.google.android.material.snackbar.ContentViewCallback {

		private TextView messageView;
		private Button actionView;

		// view inflated from custom layout
		private View view;

		public ContentViewCallback(View view) {
			this.view = view;
			messageView = view.findViewById(R.id.snackbar_text);
			actionView = view.findViewById(R.id.snackbar_btn);
		}

		@Override
		public void animateContentIn(int delay, int duration) {
			messageView.setAlpha(0f);
			messageView.animate().alpha(1f).setDuration(duration).setStartDelay(delay).start();

			if (actionView.getVisibility() == VISIBLE) {
				actionView.setAlpha(0f);
				actionView.animate().alpha(1f).setDuration(duration).setStartDelay(delay).start();
			}
		}

		@Override
		public void animateContentOut(int delay, int duration) {
			messageView.setAlpha(1f);
			messageView.animate().alpha(0f).setDuration(duration).setStartDelay(delay).start();

			if (actionView.getVisibility() == VISIBLE) {
				actionView.setAlpha(1f);
				actionView.animate().alpha(0f).setDuration(duration).setStartDelay(delay).start();
			}
		}
	}
}
