package net.osmand.plus.base;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;

public class BottomSheetDialog extends Dialog {

	private boolean cancelable = true;
	private boolean canceledOnTouchOutside = true;
	private boolean canceledOnTouchOutsideSet;

	public BottomSheetDialog(@NonNull Context context) {
		this(context, 0);
	}

	public BottomSheetDialog(@NonNull Context context, int themeResId) {
		super(context, themeResId);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	protected BottomSheetDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.cancelable = cancelable;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Window window = getWindow();
		if (window != null) {
			if (Build.VERSION.SDK_INT >= 21) {
				window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			}
			window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		}
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(wrapInContainer(layoutResID, null, null));
	}

	@Override
	public void setContentView(@NonNull View view) {
		super.setContentView(wrapInContainer(0, view, null));
	}

	@Override
	public void setContentView(@NonNull View view, @Nullable LayoutParams params) {
		super.setContentView(wrapInContainer(0, view, params));
	}

	@Override
	public void setCancelable(boolean flag) {
		super.setCancelable(flag);
		cancelable = flag;
	}

	@Override
	public void setCanceledOnTouchOutside(boolean cancel) {
		super.setCanceledOnTouchOutside(cancel);
		if (cancel && !cancelable) {
			cancelable = true;
		}
		canceledOnTouchOutside = cancel;
		canceledOnTouchOutsideSet = true;
	}

	@NonNull
	private View wrapInContainer(int layoutResId, View view, LayoutParams params) {
		View res = View.inflate(getContext(), R.layout.bottom_sheet_dialog, null);
		FrameLayout container = res.findViewById(R.id.content_container);

		if (layoutResId != 0 && view == null) {
			view = getLayoutInflater().inflate(layoutResId, container, false);
		}
		if (params == null) {
			container.addView(view);
		} else {
			container.addView(view, params);
		}

		res.findViewById(R.id.touch_outside).setOnClickListener(v -> {
			if (cancelable && isShowing() && shouldWindowCloseOnTouchOutside()) {
				cancel();
			}
		});

		container.setOnTouchListener((v, motionEvent) -> {
			// Consume the event and prevent it from falling through
			return true;
		});

		return res;
	}

	private boolean shouldWindowCloseOnTouchOutside() {
		if (!canceledOnTouchOutsideSet) {
			TypedArray a = getContext().obtainStyledAttributes(new int[]{android.R.attr.windowCloseOnTouchOutside});
			canceledOnTouchOutside = a.getBoolean(0, true);
			a.recycle();
			canceledOnTouchOutsideSet = true;
		}
		return canceledOnTouchOutside;
	}
}
