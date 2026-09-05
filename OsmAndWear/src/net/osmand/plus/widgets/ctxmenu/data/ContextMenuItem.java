package net.osmand.plus.widgets.ctxmenu.data;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.ItemLongClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnIntegerValueChangedListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnItemDeleteAction;
import net.osmand.plus.widgets.ctxmenu.callback.OnRefreshCallback;
import net.osmand.plus.widgets.ctxmenu.callback.ProgressListener;

public class ContextMenuItem {

	public static final int INVALID_ID = -1;

	private final String id;
	private int tag;

	@LayoutRes
	private int layout = INVALID_ID;
	private boolean isCategory;
	private boolean hideCompoundButton;
	private boolean hideDivider;
	private boolean clickable = true;
	private int minHeight;

	@StringRes
	private int titleId;
	private String title;
	private String description;
	private String secondaryDescription;

	private boolean useNaturalIconColor;
	private boolean useNaturalSecondIconColor;
	@ColorInt
	private Integer color;
	@DrawableRes
	private int icon = INVALID_ID;
	@DrawableRes
	private int secondaryIcon = INVALID_ID;

	private Boolean selected;
	private int progress = INVALID_ID;
	private boolean loading;

	private boolean hidden;
	private int order;

	private OnRefreshCallback onRefreshCallback;
	private ItemClickListener itemClickListener;
	private ItemLongClickListener itemLongClickListener;
	private OnIntegerValueChangedListener integerListener;
	private ProgressListener progressListener;
	private OnItemDeleteAction itemDeleteAction;

	public ContextMenuItem(@Nullable String id) {
		this.id = id;
	}

	@NonNull
	@Override
	public String toString() {
		return title;
	}

	@Nullable
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

	public boolean isCategory() {
		return isCategory;
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

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public String getSecondaryDescription() {
		return secondaryDescription;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	public boolean useNaturalIconColor() {
		return useNaturalIconColor;
	}

	public boolean useNaturalSecondIconColor() {
		return useNaturalSecondIconColor;
	}

	@ColorInt
	@Nullable
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

	@Nullable
	public OnItemDeleteAction getItemDeleteAction() {
		return itemDeleteAction;
	}

	@NonNull
	public ContextMenuItem setTag(int tag) {
		this.tag = tag;
		return this;
	}

	@NonNull
	public ContextMenuItem setLayout(@LayoutRes int layout) {
		this.layout = layout;
		return this;
	}

	@NonNull
	public ContextMenuItem setCategory(boolean category) {
		isCategory = category;
		return this;
	}

	@NonNull
	public ContextMenuItem hideCompoundButton(boolean hideCompoundButton) {
		this.hideCompoundButton = hideCompoundButton;
		return this;
	}

	@NonNull
	public ContextMenuItem setHideDivider(boolean hideDivider) {
		this.hideDivider = hideDivider;
		return this;
	}

	@NonNull
	public ContextMenuItem setClickable(boolean clickable) {
		this.clickable = clickable;
		return this;
	}

	@NonNull
	public ContextMenuItem setMinHeight(int minHeight) {
		this.minHeight = minHeight;
		return this;
	}

	@NonNull
	public ContextMenuItem setTitleId(@StringRes int titleId,
	                                  @Nullable Context context) {
		this.titleId = titleId;
		if (context != null) {
			this.title = context.getString(titleId);
		}
		return this;
	}

	@NonNull
	public ContextMenuItem setDescription(@Nullable String description) {
		this.description = description;
		return this;
	}

	@NonNull
	public ContextMenuItem setSecondaryDescription(@Nullable String secondaryDescription) {
		this.secondaryDescription = secondaryDescription;
		return this;
	}

	@NonNull
	public ContextMenuItem setTitle(@Nullable String title) {
		this.title = title;
		this.titleId = title.hashCode();
		return this;
	}

	@NonNull
	public ContextMenuItem setUseNaturalIconColor(boolean useNaturalIconColor) {
		this.useNaturalIconColor = useNaturalIconColor;
		return this;
	}

	@NonNull
	public ContextMenuItem setUseNaturalSecondIconColor(boolean useNaturalSecondIconColor) {
		this.useNaturalSecondIconColor = useNaturalSecondIconColor;
		return this;
	}

	@NonNull
	public ContextMenuItem setColor(@ColorInt @Nullable Integer color) {
		this.color = color;
		return this;
	}

	@NonNull
	public ContextMenuItem setColor(@NonNull Context context, @ColorRes int colorRes) {
		this.color = colorRes != INVALID_ID ? ColorUtilities.getColor(context, colorRes) : null;
		return this;
	}

	@NonNull
	public ContextMenuItem setIcon(@DrawableRes int icon) {
		this.icon = icon;
		return this;
	}

	@NonNull
	public ContextMenuItem setSecondaryIcon(@DrawableRes int secondaryIcon) {
		this.secondaryIcon = secondaryIcon;
		return this;
	}

	@NonNull
	public ContextMenuItem setSelected(boolean selected) {
		this.selected = selected;
		return this;
	}

	@NonNull
	public ContextMenuItem setProgress(int progress) {
		this.progress = progress;
		return this;
	}

	@NonNull
	public ContextMenuItem setLoading(boolean loading) {
		this.loading = loading;
		return this;
	}

	@NonNull
	public ContextMenuItem setHidden(boolean hidden) {
		this.hidden = hidden;
		return this;
	}

	@NonNull
	public ContextMenuItem setOrder(int order) {
		this.order = order;
		return this;
	}

	@NonNull
	public ContextMenuItem setRefreshCallback(@Nullable OnRefreshCallback onRefreshCallback) {
		this.onRefreshCallback = onRefreshCallback;
		refreshWithActualData();
		return this;
	}

	@NonNull
	public ContextMenuItem setListener(@Nullable ItemClickListener itemClickListener) {
		this.itemClickListener = itemClickListener;
		return this;
	}

	@NonNull
	public ContextMenuItem setLongClickListener(@Nullable ItemLongClickListener longClickListener) {
		this.itemLongClickListener = longClickListener;
		return this;
	}

	@NonNull
	public ContextMenuItem setIntegerListener(@Nullable OnIntegerValueChangedListener integerListener) {
		this.integerListener = integerListener;
		return this;
	}

	@NonNull
	public ContextMenuItem setProgressListener(@Nullable ProgressListener progressListener) {
		this.progressListener = progressListener;
		return this;
	}

	@NonNull
	public ContextMenuItem setItemDeleteAction(@Nullable OsmandPreference<?>... prefs) {
		this.itemDeleteAction = OnItemDeleteAction.makeDeleteAction(prefs);
		return this;
	}

	@NonNull
	public ContextMenuItem refreshWithActualData() {
		if (onRefreshCallback != null) {
			onRefreshCallback.onRefreshMenuItem(this);
		}
		return this;
	}

}
