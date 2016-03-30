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
	@StringRes
	private final int titleId;
	private String title;
	@DrawableRes
	private final int mIcon;
	@ColorRes
	private int colorRes;
	@DrawableRes
	private final int secondaryIcon;
	private Boolean selected;
	private int progress;
	@LayoutRes
	private final int layout;
	private boolean loading;
	private final boolean category;
	private final int pos;
	private String description;
	private final ContextMenuAdapter.ItemClickListener itemClickListener;
	private final ContextMenuAdapter.OnIntegerValueChangedListener integerListener;

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
							int pos,
							String description,
							ContextMenuAdapter.ItemClickListener itemClickListener,
							ContextMenuAdapter.OnIntegerValueChangedListener integerListener) {
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
		this.pos = pos;
		this.description = description;
		this.itemClickListener = itemClickListener;
		this.integerListener = integerListener;
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
		if (getColorRes() != -1) {
			return getColorRes();
		} else {
			return getDefaultColorRes(context);
		}
	}

	@ColorInt
	public int getThemedColor(Context context) {
		return ContextCompat.getColor(context, getThemedColorRes(context));
	}

	@ColorRes
	public static int getDefaultColorRes(Context context) {
		final OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		boolean light = app.getSettings().isLightContent();
		return light ? R.color.icon_color : 0;
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

	public void setTitle(String title) {
		this.title = title;
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

	public static ItemBuilder createBuilder(String title) {
		return new ItemBuilder().setTitle(title);
	}

	public static class ItemBuilder {
		@StringRes
		private int mTitleId;
		private String mTitle;
		@DrawableRes
		private int mIcon = -1;
		@ColorRes
		private int mColor = -1;
		@DrawableRes
		private int mSecondaryIcon = -1;
		private Boolean mSelected = null;
		private int mProgress = -1;
		@LayoutRes
		private int mLayout = -1;
		private boolean mLoading = false;
		private boolean mIsCategory = false;
		private int mPosition = -1;
		private String mDescription = null;
		private ContextMenuAdapter.ItemClickListener mItemClickListener = null;
		private ContextMenuAdapter.OnIntegerValueChangedListener mIntegerListener = null;

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

		public ItemBuilder setColor(@ColorRes int color) {
			mColor = color;
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

		public ContextMenuItem createItem() {
			return new ContextMenuItem(mTitleId, mTitle, mIcon, mColor, mSecondaryIcon,
					mSelected, mProgress, mLayout, mLoading, mIsCategory, mPosition, mDescription,
					mItemClickListener, mIntegerListener);
		}
	}
}
