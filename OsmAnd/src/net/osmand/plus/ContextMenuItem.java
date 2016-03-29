package net.osmand.plus;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

public class ContextMenuItem {
	@StringRes
	private final int titleId;
	private String title;
	@DrawableRes
	private final int icon;
	@DrawableRes
	private final int lightIcon;
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

	private ContextMenuItem(int titleId, String title, int icon, int lightIcon,
							int secondaryIcon, Boolean selected, int progress,
							int layout, boolean loading, boolean category,
							int pos, String description,
							ContextMenuAdapter.ItemClickListener itemClickListener,
							ContextMenuAdapter.OnIntegerValueChangedListener integerListener) {
		this.titleId = titleId;
		this.title = title;
		this.icon = icon;
		this.lightIcon = lightIcon;
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

	public int getTitleId() {
		return titleId;
	}

	public String getTitle() {
		return title;
	}

	@Deprecated
	public int getIcon() {
		return icon;
	}

	public int getLightIcon() {
		return lightIcon;
	}

	public int getSecondaryIcon() {
		return secondaryIcon;
	}

	public Boolean getSelected() {
		return selected;
	}

	public int getProgress() {
		return progress;
	}

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
		private int mTitleId;
		private String mTitle;
		private int mIcon = -1;
		private int mLightIcon = -1;
		private int mSecondaryIcon = -1;
		private Boolean mSelected = null;
		private int mProgress = -1;
		private int mLayout = -1;
		private boolean mLoading = false;
		private boolean mCat = false;
		private int mPos = -1;
		private String mDescription = null;
		private ContextMenuAdapter.ItemClickListener mItemClickListener = null;
		private ContextMenuAdapter.OnIntegerValueChangedListener mIntegerListener = null;

		public ItemBuilder setTitleId(int titleId, @Nullable Context context) {
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

		@Deprecated
		public ItemBuilder setIcon(int icon) {
			mIcon = icon;
			return this;
		}

		public ItemBuilder setColorIcon(int lightIcon) {
			mLightIcon = lightIcon;
			return this;
		}

		public ItemBuilder setSecondaryIcon(int secondaryIcon) {
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

		public ItemBuilder setLayout(int layout) {
			mLayout = layout;
			return this;
		}

		public ItemBuilder setLoading(boolean loading) {
			mLoading = loading;
			return this;
		}

		public ItemBuilder setCategory(boolean cat) {
			mCat = cat;
			return this;
		}

		public ItemBuilder setPosition(int pos) {
			mPos = pos;
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
			return new ContextMenuItem(mTitleId, mTitle, mIcon, mLightIcon, mSecondaryIcon,
					mSelected, mProgress, mLayout, mLoading, mCat, mPos, mDescription,
					mItemClickListener, mIntegerListener);
		}
	}
}
