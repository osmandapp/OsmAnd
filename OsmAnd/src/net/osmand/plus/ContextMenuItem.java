package net.osmand.plus;

import android.content.Context;

import net.osmand.plus.ContextMenuAdapter.OnItemDeleteAction;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

public class ContextMenuItem {
	public static final int INVALID_ID = -1;

	@StringRes
	private final int titleId;
	private String title;
	@DrawableRes
	private int mIcon;
	@ColorInt
	private Integer color;
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
	private boolean hidden;
	private int order;
	private String description;
	private final OnUpdateCallback onUpdateCallback;
	private final ContextMenuAdapter.ItemClickListener itemClickListener;
	private final ContextMenuAdapter.ItemLongClickListener itemLongClickListener;
	private final ContextMenuAdapter.OnIntegerValueChangedListener integerListener;
	private final ContextMenuAdapter.ProgressListener progressListener;
	private final OnItemDeleteAction itemDeleteAction;
	private final boolean hideDivider;
	private final boolean hideCompoundButton;
	private final int minHeight;
	private final int tag;
	private final String id;

	private ContextMenuItem(@StringRes int titleId,
							String title,
							@DrawableRes int icon,
							@ColorInt Integer color,
							@DrawableRes int secondaryIcon,
							Boolean selected,
							int progress,
							@LayoutRes int layout,
							boolean loading,
							boolean category,
							boolean clickable,
							boolean skipPaintingWithoutColor,
							int order,
							String description,
							OnUpdateCallback onUpdateCallback,
							ContextMenuAdapter.ItemClickListener itemClickListener,
							ContextMenuAdapter.ItemLongClickListener itemLongClickListener,
							ContextMenuAdapter.OnIntegerValueChangedListener integerListener,
							ContextMenuAdapter.ProgressListener progressListener,
							OnItemDeleteAction itemDeleteAction,
							boolean hideDivider,
							boolean hideCompoundButton,
							int minHeight,
							int tag,
							String id) {
		this.titleId = titleId;
		this.title = title;
		this.mIcon = icon;
		this.color = color;
		this.secondaryIcon = secondaryIcon;
		this.selected = selected;
		this.progress = progress;
		this.layout = layout;
		this.loading = loading;
		this.category = category;
		this.clickable = clickable;
		this.skipPaintingWithoutColor = skipPaintingWithoutColor;
		this.order = order;
		this.description = description;
		this.onUpdateCallback = onUpdateCallback;
		this.itemClickListener = itemClickListener;
		this.itemLongClickListener = itemLongClickListener;
		this.integerListener = integerListener;
		this.progressListener = progressListener;
		this.hideDivider = hideDivider;
		this.itemDeleteAction = itemDeleteAction;
		this.hideCompoundButton = hideCompoundButton;
		this.minHeight = minHeight;
		this.tag = tag;
		this.id = id;
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

	@ColorInt
	public Integer getColor() {
		return color;
	}

	@ColorInt
	public int getThemedColor(Context context) {
		if (skipPaintingWithoutColor || color != null) {
			return color;
		}
		return ContextCompat.getColor(context, UiUtilities.getDefaultColorRes(context));
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

	public boolean isHidden() {
		return hidden;
	}

	public int getOrder() {
		return order;
	}

	public String getDescription() {
		return description;
	}

	public OnItemDeleteAction getItemDeleteAction() {
		return itemDeleteAction;
	}

	@Nullable
	public ContextMenuAdapter.ItemClickListener getItemClickListener() {
		return itemClickListener;
	}

	@Nullable
	public ContextMenuAdapter.ItemLongClickListener getItemLongClickListener() {
		return itemLongClickListener;
	}

	@Nullable
	public ContextMenuAdapter.OnIntegerValueChangedListener getIntegerListener() {
		return integerListener;
	}

	@Nullable
	public ContextMenuAdapter.ProgressListener getProgressListener() {
		return progressListener;
	}

	public boolean shouldSkipPainting() {
		return skipPaintingWithoutColor;
	}

	public boolean shouldHideDivider() {
		return hideDivider;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean shouldHideCompoundButton() {
		return hideCompoundButton;
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

	public void setColor(Context context, @ColorRes int colorRes) {
		color = colorRes != INVALID_ID ? ContextCompat.getColor(context, colorRes) : null;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setOrder(int order) {
		this.order = order;
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

	public String getId() {
		return id;
	}

	public void update() {
		if (onUpdateCallback != null) {
			onUpdateCallback.onUpdateMenuItem(this);
		}
	}

	public interface OnUpdateCallback {
		void onUpdateMenuItem(ContextMenuItem item);
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
		@ColorInt
		private Integer mColor = null;
		@DrawableRes
		private int mSecondaryIcon = INVALID_ID;
		private Boolean mSelected = null;
		private int mProgress = INVALID_ID;
		@LayoutRes
		private int mLayout = INVALID_ID;
		private boolean mLoading = false;
		private boolean mIsCategory = false;
		private boolean mIsClickable = true;
		private int mOrder = 0;
		private String mDescription = null;
		private OnUpdateCallback mOnUpdateCallback = null;
		private ContextMenuAdapter.ItemClickListener mItemClickListener = null;
		private ContextMenuAdapter.ItemLongClickListener mItemLongClickListener = null;
		private ContextMenuAdapter.OnIntegerValueChangedListener mIntegerListener = null;
		private ContextMenuAdapter.ProgressListener mProgressListener = null;
		private OnItemDeleteAction mItemDeleteAction = null;
		private boolean mSkipPaintingWithoutColor;
		private boolean mHideDivider;
		private boolean mHideCompoundButton;
		private int mMinHeight;
		private int mTag;
		private String mId;

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

		public ItemBuilder setColor(@ColorInt Integer color) {
			mColor = color;
			return this;
		}

		public ItemBuilder setColor(Context context, @ColorRes int colorRes) {
			if (colorRes != INVALID_ID) {
				mColor = ContextCompat.getColor(context, colorRes);
			}
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

		public ItemBuilder setOrder(int order) {
			mOrder = order;
			return this;
		}

		public ItemBuilder setDescription(String description) {
			mDescription = description;
			return this;
		}

		public ItemBuilder setOnUpdateCallback(OnUpdateCallback onUpdateCallback) {
			mOnUpdateCallback = onUpdateCallback;
			return this;
		}

		public ItemBuilder setListener(ContextMenuAdapter.ItemClickListener checkBoxListener) {
			mItemClickListener = checkBoxListener;
			return this;
		}

		public ItemBuilder setLongClickListener(ContextMenuAdapter.ItemLongClickListener longClickListener) {
			mItemLongClickListener = longClickListener;
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

		public ItemBuilder setItemDeleteAction(OnItemDeleteAction itemDeleteAction) {
			this.mItemDeleteAction = itemDeleteAction;
			return this;
		}

		public ItemBuilder hideDivider(boolean hideDivider) {
			mHideDivider = hideDivider;
			return this;
		}

		public ItemBuilder hideCompoundButton(boolean hideCompoundButton) {
			mHideCompoundButton = hideCompoundButton;
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

		public ItemBuilder setId(String id) {
			this.mId = id;
			return this;
		}

		public ContextMenuItem createItem() {
			ContextMenuItem item = new ContextMenuItem(mTitleId, mTitle, mIcon, mColor, mSecondaryIcon,
					mSelected, mProgress, mLayout, mLoading, mIsCategory, mIsClickable, mSkipPaintingWithoutColor,
					mOrder, mDescription, mOnUpdateCallback, mItemClickListener, mItemLongClickListener,
					mIntegerListener, mProgressListener, mItemDeleteAction, mHideDivider, mHideCompoundButton,
					mMinHeight, mTag, mId);
			item.update();
			return item;
		}
	}
}
