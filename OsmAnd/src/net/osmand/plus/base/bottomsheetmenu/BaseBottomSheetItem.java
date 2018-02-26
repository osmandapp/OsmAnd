package net.osmand.plus.base.bottomsheetmenu;

import android.support.annotation.LayoutRes;
import android.view.View;

public class BaseBottomSheetItem {

	private View customView;
	@LayoutRes
	private int layoutResId;
	private boolean clickable = true;
	private View.OnClickListener onClickListener;
	private int position = -1;

	public BaseBottomSheetItem(View customView,
							   @LayoutRes int layoutResId,
							   boolean clickable,
							   View.OnClickListener onClickListener,
							   int position) {
		this.customView = customView;
		this.layoutResId = layoutResId;
		this.clickable = clickable;
		this.onClickListener = onClickListener;
		this.position = position;
	}

	public View getCustomView() {
		return customView;
	}

	@LayoutRes
	public int getLayoutResId() {
		return layoutResId;
	}

	public boolean isClickable() {
		return clickable;
	}

	public View.OnClickListener getOnClickListener() {
		return onClickListener;
	}

	public int getPosition() {
		return position;
	}

	public static class Builder {

		protected View customView;
		@LayoutRes
		protected int layoutResId;
		protected boolean clickable;
		protected View.OnClickListener onClickListener;
		protected int position;

		public Builder setCustomView(View customView) {
			this.customView = customView;
			return this;
		}

		public Builder setLayoutResId(@LayoutRes int layoutResId) {
			this.layoutResId = layoutResId;
			return this;
		}

		public Builder setClickable(boolean clickable) {
			this.clickable = clickable;
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
			return new BaseBottomSheetItem(customView, layoutResId, clickable, onClickListener, position);
		}
	}
}
