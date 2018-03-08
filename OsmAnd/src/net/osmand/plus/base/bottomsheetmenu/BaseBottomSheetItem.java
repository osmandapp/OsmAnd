package net.osmand.plus.base.bottomsheetmenu;

import android.support.annotation.LayoutRes;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class BaseBottomSheetItem {

	public static final int INVALID_POSITION = -1;
	public static final int INVALID_ID = -1;

	protected View view;
	@LayoutRes
	protected int layoutId = INVALID_ID;
	private Object tag;
	private boolean disabled;
	private View.OnClickListener onClickListener;
	protected int position = INVALID_POSITION;

	public View getView() {
		return view;
	}

	public BaseBottomSheetItem(View view,
							   @LayoutRes int layoutId,
							   Object tag,
							   boolean disabled,
							   View.OnClickListener onClickListener,
							   int position) {
		this.view = view;
		this.layoutId = layoutId;
		this.tag = tag;
		this.disabled = disabled;
		this.onClickListener = onClickListener;
		this.position = position;
	}

	protected BaseBottomSheetItem() {

	}

	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		View view = getView(app, nightMode);
		if (tag != null) {
			view.setTag(tag);
		}
		if (disabled) {
			view.setEnabled(false);
			view.setAlpha(.5f);
		}
		view.setOnClickListener(onClickListener);
		view.setClickable(onClickListener != null);
		if (position != INVALID_POSITION) {
			container.addView(view, position);
		} else {
			container.addView(view);
		}
	}

	private View getView(OsmandApplication app, boolean nightMode) {
		if (view != null) {
			return view;
		}
		if (layoutId != INVALID_ID) {
			final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
			return view = View.inflate(new ContextThemeWrapper(app, themeRes), layoutId, null);
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

		public Builder setPosition(int position) {
			this.position = position;
			return this;
		}

		public BaseBottomSheetItem create() {
			return new BaseBottomSheetItem(customView, layoutId, tag, disabled, onClickListener, position);
		}
	}
}
