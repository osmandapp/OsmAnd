package net.osmand.plus;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

public class ContextMenuItem {
	public static final int INVALID_ID = -1;

	@StringRes
	private final int titleId;
	private String title;
	@DrawableRes
	private int mIcon;
	@ColorRes
	private int colorRes;
	@DrawableRes
	private int secondaryIcon;
	private Boolean selected;
	private int progress;
	@LayoutRes
	private final int layout;
	private boolean loading;
	private final boolean category;
	private final boolean clickable;
	private final boolean skipPaintingWithoutColor;
	private final int pos;
	private String description;
	private final ContextMenuAdapter.ItemClickListener itemClickListener;
	private final ContextMenuAdapter.OnIntegerValueChangedListener integerListener;
	private final ContextMenuAdapter.ProgressListener progressListener;
	private final boolean hideDivider;
	private final int minHeight;
	private final int tag;

	private ContextMenuItem(@StringRes int titleId,
							String title,
							@DrawableRes int icon,
							@ColorRes int colorRes,
							@DrawableRes int secondaryIcon,
							Boolean selected,
							int progress,
							@LayoutRes int layout,
							boolean loading,
							boolean category,
							boolean clickable,
							boolean skipPaintingWithoutColor, int pos,
							String description,
							ContextMenuAdapter.ItemClickListener itemClickListener,
							ContextMenuAdapter.OnIntegerValueChangedListener integerListener,
							ContextMenuAdapter.ProgressListener progressListener,
							boolean hideDivider,
							int minHeight,
							int tag) {
		this.titleId = titleId;
		this.title = title;
		this.mIcon = icon;
		this.colorRes = colorRes;
		this.secondaryIcon = secondaryIcon;
		this.selected = selected;
		this.progress = progress;
		this.layout = layout;
		this.loading = loading;
		this.category = category;
		this.clickable = clickable;
		this.skipPaintingWithoutColor = skipPaintingWithoutColor;
		this.pos = pos;
		this.description = description;
		this.itemClickListener = itemClickListener;
		this.integerListener = integerListener;
		this.progressListener = progressListener;
		this.hideDivider = hideDivider;
		this.minHeight = minHeight;
		this.tag = tag;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	public String getTitle() {
		return title;
	}

	@DrawableRes
	public int getIcon() {
		return mIcon;
	}

	@ColorRes
	public int getColorRes() {
		return colorRes;
	}

	@ColorRes
	public int getThemedColorRes(Context context) {
		if (skipPaintingWithoutColor || getColorRes() != INVALID_ID) {
			return getColorRes();
		} else {
			return IconsCache.getDefaultColorRes(context);
		}
	}

	@ColorInt
	public int getThemedColor(Context context) {
		return ContextCompat.getColor(context, getThemedColorRes(context));
	}

	@DrawableRes
	public int getSecondaryIcon() {
		return secondaryIcon;
	}

	public Boolean getSelected() {
		return selected;
	}

	public int getProgress() {
		return progress;
	}

	@LayoutRes
	public int getLayout() {
		return layout;
	}

	public boolean isLoading() {
		return loading;
	}

	public boolean isCategory() {
		return category;
	}

	public boolean isClickable() {
		return clickable;
	}

	public int getPos() {
		return pos;
	}

	public String getDescription() {
		return description;
	}

	public ContextMenuAdapter.ItemClickListener getItemClickListener() {
		return itemClickListener;
	}

	public ContextMenuAdapter.OnIntegerValueChangedListener getIntegerListener() {
		return integerListener;
	}

	public ContextMenuAdapter.ProgressListener getProgressListener() {
		return progressListener;
	}

	public boolean shouldSkipPainting() {
		return skipPaintingWithoutColor;
	}

	public boolean shouldHideDivider() {
		return hideDivider;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setIcon(int iconId) {
		this.mIcon = iconId;
	}

	public void setSecondaryIcon(int secondaryIcon) {
		this.secondaryIcon = secondaryIcon;
	}

	public void setColorRes(int colorRes) {
		this.colorRes = colorRes;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getMinHeight() {
		return minHeight;
	}

	public int getTag() {
		return tag;
	}

	public static ItemBuilder createBuilder(String title) {
		return new ItemBuilder().setTitle(title);
	}

	public static class ItemBuilder {
		@StringRes
		private int mTitleId;
		private String mTitle;
		@DrawableRes
		private int mIcon = INVALID_ID;
		@ColorRes
		private int mColorRes = INVALID_ID;
		@DrawableRes
		private int mSecondaryIcon = INVALID_ID;
		private Boolean mSelected = null;
		private int mProgress = INVALID_ID;
		@LayoutRes
		private int mLayout = INVALID_ID;
		private boolean mLoading = false;
		private boolean mIsCategory = false;
		private boolean mIsClickable = true;
		private int mPosition = -1;
		private String mDescription = null;
		private ContextMenuAdapter.ItemClickListener mItemClickListener = null;
		private ContextMenuAdapter.OnIntegerValueChangedListener mIntegerListener = null;
		private ContextMenuAdapter.ProgressListener mProgressListener = null;
		private boolean mSkipPaintingWithoutColor;
		private boolean mHideDivider;
		private int mMinHeight;
		private int mTag;

		public ItemBuilder setTitleId(@StringRes int titleId, @Nullable Context context) {
			this.mTitleId = titleId;
			if (context != null) {
				mTitle = context.getString(titleId);
			}
			return this;
		}

		public ItemBuilder setTitle(String title) {
			this.mTitle = title;
			this.mTitleId = title.hashCode();
			return this;
		}

		public ItemBuilder setColor(@ColorRes int colorRes) {
			mColorRes = colorRes;
			return this;
		}

		public ItemBuilder setIcon(@DrawableRes int icon) {
			mIcon = icon;
			return this;
		}

		public ItemBuilder setSecondaryIcon(@DrawableRes int secondaryIcon) {
			mSecondaryIcon = secondaryIcon;
			return this;
		}

		public ItemBuilder setSelected(boolean selected) {
			mSelected = selected;
			return this;
		}

		public ItemBuilder setProgress(int progress) {
			mProgress = progress;
			return this;
		}

		public ItemBuilder setLayout(@LayoutRes int layout) {
			mLayout = layout;
			return this;
		}

		public ItemBuilder setLoading(boolean loading) {
			mLoading = loading;
			return this;
		}

		public ItemBuilder setCategory(boolean category) {
			mIsCategory = category;
			return this;
		}

		public ItemBuilder setClickable(boolean clickable) {
			mIsClickable = clickable;
			return this;
		}

		public ItemBuilder setPosition(int position) {
			mPosition = position;
			return this;
		}

		public ItemBuilder setDescription(String description) {
			mDescription = description;
			return this;
		}

		public ItemBuilder setListener(ContextMenuAdapter.ItemClickListener checkBoxListener) {
			mItemClickListener = checkBoxListener;
			return this;
		}

		public ItemBuilder setIntegerListener(ContextMenuAdapter.OnIntegerValueChangedListener integerListener) {
			mIntegerListener = integerListener;
			return this;
		}

		public ItemBuilder setProgressListener(ContextMenuAdapter.ProgressListener progressListener) {
			mProgressListener = progressListener;
			return this;
		}

		public ItemBuilder setSkipPaintingWithoutColor(boolean skipPaintingWithoutColor) {
			mSkipPaintingWithoutColor = skipPaintingWithoutColor;
			return this;
		}

		public ItemBuilder hideDivider(boolean hideDivider) {
			mHideDivider = hideDivider;
			return this;
		}

		public ItemBuilder setMinHeight(int minHeight) {
			this.mMinHeight = minHeight;
			return this;
		}

		public int getTag() {
			return mTag;
		}

		public ItemBuilder setTag(int tag) {
			this.mTag = tag;
			return this;
		}

		public ContextMenuItem createItem() {
			return new ContextMenuItem(mTitleId, mTitle, mIcon, mColorRes, mSecondaryIcon,
					mSelected, mProgress, mLayout, mLoading, mIsCategory, mIsClickable, mSkipPaintingWithoutColor,
					mPosition, mDescription, mItemClickListener, mIntegerListener, mProgressListener,
					mHideDivider, mMinHeight, mTag);
		}
	}
}
