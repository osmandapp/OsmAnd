package net.osmand.plus.base.bottomsheetmenu;

import android.support.annotation.LayoutRes;
import android.view.View;

public class BaseBottomSheetItem {

	public static final int INVALID_POSITION = -1;
	public static final int INVALID_ID = -1;

	private View customView;
	@LayoutRes
	private int layoutId;
	private boolean disabled;
	private View.OnClickListener onClickListener;
	private int position;

	public BaseBottomSheetItem(View customView,
							   @LayoutRes int layoutId,
							   boolean disabled,
							   View.OnClickListener onClickListener,
							   int position) {
		this.customView = customView;
		this.layoutId = layoutId;
		this.disabled = disabled;
		this.onClickListener = onClickListener;
		this.position = position;
	}

	public View getCustomView() {
		return customView;
	}

	@LayoutRes
	public int getLayoutId() {
		return layoutId;
	}

	public boolean isDisabled() {
		return disabled;
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
		protected int layoutId = INVALID_ID;
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
			return new BaseBottomSheetItem(customView, layoutId, disabled, onClickListener, position);
		}
	}
}
