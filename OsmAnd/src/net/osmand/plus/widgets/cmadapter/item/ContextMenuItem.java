package net.osmand.plus.widgets.cmadapter.item;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.widgets.cmadapter.callback.OnRefreshCallback;
import net.osmand.plus.widgets.cmadapter.callback.ItemClickListener;
import net.osmand.plus.widgets.cmadapter.callback.ItemLongClickListener;
import net.osmand.plus.widgets.cmadapter.callback.OnIntegerValueChangedListener;
import net.osmand.plus.widgets.cmadapter.callback.OnItemDeleteAction;
import net.osmand.plus.widgets.cmadapter.callback.ProgressListener;

public class ContextMenuItem {

	public static final int INVALID_ID = -1;

	private final String id;
	private int tag;

	private @LayoutRes int layout = INVALID_ID;
	private boolean hideCompoundButton;
	private boolean hideDivider;
	private boolean clickable = true;
	private int minHeight;

	private @StringRes int titleId;
	private String description;
	private String title;

	private boolean useNaturalIconColor;
	private @ColorInt Integer color = null;
	private @DrawableRes int icon = INVALID_ID;
	private @DrawableRes int secondaryIcon = INVALID_ID;

	private Boolean selected = null;
	private int progress = INVALID_ID;
	private boolean loading;
	private boolean hidden;
	private int order = 0;

	private OnRefreshCallback onRefreshCallback;
	private ItemClickListener itemClickListener;
	private ItemLongClickListener itemLongClickListener;
	private OnIntegerValueChangedListener integerListener;
	private ProgressListener progressListener;
	private OnItemDeleteAction itemDeleteAction;

	public ContextMenuItem(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public int getTag() {
		return tag;
	}

	@LayoutRes
	public int getLayout() {
		return layout;
	}

	public boolean shouldHideCompoundButton() {
		return hideCompoundButton;
	}

	public boolean shouldHideDivider() {
		return hideDivider;
	}

	public boolean isClickable() {
		return clickable;
	}

	public int getMinHeight() {
		return minHeight;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	public String getDescription() {
		return description;
	}

	public String getTitle() {
		return title;
	}

	public boolean useNaturalIconColor() {
		return useNaturalIconColor;
	}

	@ColorInt
	public Integer getColor() {
		return color;
	}

	@DrawableRes
	public int getIcon() {
		return icon;
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

	public boolean isLoading() {
		return loading;
	}

	public boolean isHidden() {
		return hidden;
	}

	public int getOrder() {
		return order;
	}

	public boolean isCategory() {
		return false;
	}

	@Nullable
	public ItemClickListener getItemClickListener() {
		return itemClickListener;
	}

	@Nullable
	public ItemLongClickListener getItemLongClickListener() {
		return itemLongClickListener;
	}

	@Nullable
	public OnIntegerValueChangedListener getIntegerListener() {
		return integerListener;
	}

	@Nullable
	public ProgressListener getProgressListener() {
		return progressListener;
	}

	public OnItemDeleteAction getItemDeleteAction() {
		return itemDeleteAction;
	}

	public ContextMenuItem setTag(int tag) {
		this.tag = tag;
		return this;
	}

	public ContextMenuItem setLayout(@LayoutRes int layout) {
		this.layout = layout;
		return this;
	}

	public ContextMenuItem hideCompoundButton(boolean hideCompoundButton) {
		this.hideCompoundButton = hideCompoundButton;
		return this;
	}

	public ContextMenuItem hideDivider(boolean hideDivider) {
		this.hideDivider = hideDivider;
		return this;
	}

	public ContextMenuItem setClickable(boolean clickable) {
		this.clickable = clickable;
		return this;
	}

	public ContextMenuItem setMinHeight(int minHeight) {
		this.minHeight = minHeight;
		return this;
	}

	public ContextMenuItem setTitleId(@StringRes int titleId,
	                                  @Nullable Context context) {
		this.titleId = titleId;
		if (context != null) {
			this.title = context.getString(titleId);
		}
		return this;
	}

	public ContextMenuItem setDescription(String description) {
		this.description = description;
		return this;
	}

	public ContextMenuItem setTitle(String title) {
		this.title = title;
		this.titleId = title.hashCode();
		return this;
	}

	public ContextMenuItem setUseNaturalIconColor(boolean useNaturalIconColor) {
		this.useNaturalIconColor = useNaturalIconColor;
		return this;
	}

	public ContextMenuItem setColor(@ColorInt Integer color) {
		this.color = color;
		return this;
	}

	public ContextMenuItem setColor(Context context, @ColorRes int colorRes) {
		this.color = colorRes != INVALID_ID ? ContextCompat.getColor(context, colorRes) : null;
		return this;
	}

	public ContextMenuItem setIcon(@DrawableRes int icon) {
		this.icon = icon;
		return this;
	}

	public ContextMenuItem setSecondaryIcon(@DrawableRes int secondaryIcon) {
		this.secondaryIcon = secondaryIcon;
		return this;
	}

	public ContextMenuItem setSelected(boolean selected) {
		this.selected = selected;
		return this;
	}

	public ContextMenuItem setProgress(int progress) {
		this.progress = progress;
		return this;
	}

	public ContextMenuItem setLoading(boolean loading) {
		this.loading = loading;
		return this;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public ContextMenuItem setOrder(int order) {
		this.order = order;
		return this;
	}

	public ContextMenuItem setRefreshCallback(OnRefreshCallback onRefreshCallback) {
		this.onRefreshCallback = onRefreshCallback;
		refreshWithActualData();
		return this;
	}

	public ContextMenuItem setListener(ItemClickListener itemClickListener) {
		this.itemClickListener = itemClickListener;
		return this;
	}

	public ContextMenuItem setLongClickListener(ItemLongClickListener longClickListener) {
		this.itemLongClickListener = longClickListener;
		return this;
	}

	public ContextMenuItem setIntegerListener(OnIntegerValueChangedListener integerListener) {
		this.integerListener = integerListener;
		return this;
	}

	public ContextMenuItem setProgressListener(ProgressListener progressListener) {
		this.progressListener = progressListener;
		return this;
	}

	public ContextMenuItem setItemDeleteAction(OsmandPreference<?>... prefs) {
		this.itemDeleteAction = OnItemDeleteAction.makeDeleteAction(prefs);
		return this;
	}

	public ContextMenuItem refreshWithActualData() {
		if (onRefreshCallback != null) {
			onRefreshCallback.onRefreshMenuItem(this);
		}
		return this;
	}

}
