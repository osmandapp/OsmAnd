package net.osmand.plus.backup.trash.data;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class TrashItem {

	private String name;
	private int iconId;
	private long deleteTime;
	private String description;
	private boolean isLocalItem = true;

	public String getName() {
		return name;
	}

	public TrashItem setName(String name) {
		this.name = name;
		return this;
	}

	@NonNull
	public String getDescription() {
		return description;
	}

	public TrashItem setDescription(@NonNull String description) {
		this.description = description;
		return this;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	public TrashItem setIconId(@DrawableRes int iconId) {
		this.iconId = iconId;
		return this;
	}

	public long getDeleteTime() {
		return deleteTime;
	}

	public TrashItem setDeleteTime(long deleteTime) {
		this.deleteTime = deleteTime;
		return this;
	}

	public boolean isLocalItem() {
		return isLocalItem;
	}

	public TrashItem setLocalItem(boolean localItem) {
		isLocalItem = localItem;
		return this;
	}
}
