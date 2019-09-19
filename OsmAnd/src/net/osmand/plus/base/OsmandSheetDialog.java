package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import net.osmand.plus.R;

import static net.osmand.plus.base.SheetDialogType.*;

public class OsmandSheetDialog extends Dialog {

	private boolean cancelable = true;
	private boolean canceledOnTouchOutside = true;
	private boolean interactWithOutside = false;
	private boolean canceledOnTouchOutsideSet;
	
	private SheetDialogType dialogType;

	public OsmandSheetDialog(@NonNull Context context) {
		this(context, 0, BOTTOM);
	}

	public OsmandSheetDialog(@NonNull Context context, int themeResId, SheetDialogType dialogType) {
		super(context, themeResId);
		this.dialogType = dialogType;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	protected OsmandSheetDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
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
	
	public void setInteractWithOutside(boolean interactWithOutside) {
		this.interactWithOutside = interactWithOutside;
	}

	@NonNull
	private View wrapInContainer(int layoutResId, View view, LayoutParams params) {
		final FrameLayout res = new FrameLayout(getContext());
		final View touchOutside = new View(getContext());
		final FrameLayout container = getContainer(dialogType);

		ViewGroup.LayoutParams fullScreenParams = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		res.setLayoutParams(fullScreenParams);
		touchOutside.setLayoutParams(fullScreenParams);
		
		res.addView(touchOutside);
		res.addView(container);

		if (layoutResId != 0 && view == null) {
			view = getLayoutInflater().inflate(layoutResId, container, false);
		}
		if (params == null) {
			container.addView(view);
		} else {
			container.addView(view, params);
		}

		touchOutside.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (cancelable && isShowing() && shouldWindowCloseOnTouchOutside()) {
					cancel();
				}
			}
		});
		
		touchOutside.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (interactWithOutside) {
					Activity ownerActivity = getOwnerActivity();
					if (ownerActivity != null) {
						ownerActivity.dispatchTouchEvent(event);
					}
				}
				return false;
			}
		});

		container.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				// Consume the event and prevent it from falling through
				return true;
			}
		});

		return res;
	}
	
	private FrameLayout getContainer(SheetDialogType dialogType) {
		if (dialogType == null) {
			return null;
		}
		int width = LayoutParams.MATCH_PARENT;
		int height = LayoutParams.WRAP_CONTENT;
		int gravity = 0;
		switch (dialogType) {
			case TOP:
				gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
				break;
			case BOTTOM:
				gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
				break;
		}
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
		params.gravity = gravity;
		FrameLayout container = new FrameLayout(getContext());
		container.setLayoutParams(params);
		return container;
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
