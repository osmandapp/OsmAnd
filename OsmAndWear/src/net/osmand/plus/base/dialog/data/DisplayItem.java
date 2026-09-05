package net.osmand.plus.base.dialog.data;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;

public class DisplayItem {

	@LayoutRes
	private int layoutId;
	private String title;
	private String description;
	private Drawable normalIcon;
	private Drawable selectedIcon;
	@ColorInt
	private Integer controlsColor;
	@ColorInt
	private Integer backgroundColor;
	private boolean isClickable = true;
	private boolean showBottomDivider = false;
	private int dividerStartPadding = 0;
	private Object tag;

	public int getLayoutId() {
		return layoutId;
	}

	public DisplayItem setLayoutId(int layoutId) {
		this.layoutId = layoutId;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public DisplayItem setTitle(String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public DisplayItem setDescription(String description) {
		this.description = description;
		return this;
	}

	public DisplayItem setIcon(Drawable icon) {
		this.normalIcon = this.selectedIcon = icon;
		return this;
	}

	public Drawable getNormalIcon() {
		return normalIcon;
	}

	public DisplayItem setNormalIcon(Drawable normalIcon) {
		this.normalIcon = normalIcon;
		return this;
	}

	public Drawable getSelectedIcon() {
		return selectedIcon;
	}

	public DisplayItem setSelectedIcon(Drawable selectedIcon) {
		this.selectedIcon = selectedIcon;
		return this;
	}

	public boolean isClickable() {
		return isClickable;
	}

	public DisplayItem setClickable(boolean clickable) {
		isClickable = clickable;
		return this;
	}

	@ColorInt
	public Integer getControlsColor() {
		return controlsColor;
	}

	public DisplayItem setControlsColor(@ColorInt Integer customControlsColor) {
		this.controlsColor = customControlsColor;
		return this;
	}

	@ColorInt
	public Integer getBackgroundColor() {
		return backgroundColor;
	}

	public DisplayItem setBackgroundColor(@ColorInt Integer backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}

	public boolean shouldShowBottomDivider() {
		return showBottomDivider;
	}

	public DisplayItem hideBottomDivider() {
		return setShowBottomDivider(false);
	}

	public DisplayItem showBottomDivider() {
		return setShowBottomDivider(true);
	}

	public DisplayItem setShowBottomDivider(boolean showBottomDivider) {
		setShowBottomDivider(showBottomDivider, 0);
		return this;
	}

	public DisplayItem setShowBottomDivider(boolean showBottomDivider, int dividerStartPadding) {
		this.showBottomDivider = showBottomDivider;
		this.dividerStartPadding = dividerStartPadding;
		return this;
	}

	public int getDividerStartPadding() {
		return dividerStartPadding;
	}

	public Object getTag() {
		return tag;
	}

	public DisplayItem setTag(Object tag) {
		this.tag = tag;
		return this;
	}
}
