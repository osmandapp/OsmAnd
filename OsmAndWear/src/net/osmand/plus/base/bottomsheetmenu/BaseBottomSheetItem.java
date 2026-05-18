package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;

public class BaseBottomSheetItem {

	public static final int INVALID_POSITION = -1;
	public static final int INVALID_ID = -1;
	public static final int INVALID_VALUE = -1;

	protected View view;
	@LayoutRes
	protected int layoutId = INVALID_ID;
	private Object tag;
	private boolean disabled;
	protected View.OnClickListener onClickListener;
	protected View.OnLongClickListener onLongClickListener;
	protected int position = INVALID_POSITION;

	public View getView() {
		return view;
	}

	public Object getTag() {
		return tag;
	}

	public void setTag(Object tag) {
		this.tag = tag;
	}

	public BaseBottomSheetItem(View view,
							   @LayoutRes int layoutId,
							   Object tag,
							   boolean disabled,
							   View.OnClickListener onClickListener,
							   int position) {
		this(view, layoutId, tag, disabled, onClickListener, null, position);
	}

	public BaseBottomSheetItem(@NonNull View view,
	                           @LayoutRes int layoutId,
	                           @Nullable Object tag,
	                           boolean disabled,
	                           @Nullable View.OnClickListener onClickListener,
	                           @Nullable View.OnLongClickListener onLongClickListener,
	                           int position) {
		this.view = view;
		this.layoutId = layoutId;
		this.tag = tag;
		this.disabled = disabled;
		this.onClickListener = onClickListener;
		this.onLongClickListener = onLongClickListener;
		this.position = position;
	}

	protected BaseBottomSheetItem() {

	}

	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		View view = getView(context, container, nightMode);
		if (tag != null) {
			view.setTag(tag);
		}
		if (disabled) {
			view.setEnabled(false);
			view.setAlpha(.5f);
		}
		if (onLongClickListener != null) {
			view.setOnLongClickListener(onLongClickListener);
		}
		view.setOnClickListener(onClickListener);
		view.setClickable(onClickListener != null || onLongClickListener != null);
		if (position != INVALID_POSITION) {
			container.addView(view, position);
		} else {
			container.addView(view);
		}
	}

	private View getView(Context context, ViewGroup parent, boolean nightMode) {
		if (view != null) {
			return view;
		}
		if (layoutId != INVALID_ID) {
			int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
			return view = LayoutInflater.from(new ContextThemeWrapper(context, themeRes))
					.inflate(layoutId, parent, false);
		}
		throw new RuntimeException("BottomSheetItem must have specified view or layoutId.");
	}

	public static class Builder {

		protected View customView;
		@LayoutRes
		protected int layoutId = INVALID_ID;
		protected Object tag;
		protected boolean disabled;
		protected View.OnClickListener onClickListener;
		protected View.OnLongClickListener onLongClickListener;
		protected int position = INVALID_POSITION;

		public Builder setCustomView(View customView) {
			this.customView = customView;
			return this;
		}

		public Builder setLayoutId(@LayoutRes int layoutId) {
			this.layoutId = layoutId;
			return this;
		}

		public Builder setTag(Object tag) {
			this.tag = tag;
			return this;
		}

		public Builder setDisabled(boolean disabled) {
			this.disabled = disabled;
			return this;
		}

		public Builder setOnClickListener(View.OnClickListener onClickListener) {
			this.onClickListener = onClickListener;
			return this;
		}

		public Builder setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
			this.onLongClickListener = onLongClickListener;
			return this;
		}

		public Builder setPosition(int position) {
			this.position = position;
			return this;
		}

		public BaseBottomSheetItem create() {
			return new BaseBottomSheetItem(customView, layoutId, tag, disabled, onClickListener, onLongClickListener, position);
		}
	}
}
