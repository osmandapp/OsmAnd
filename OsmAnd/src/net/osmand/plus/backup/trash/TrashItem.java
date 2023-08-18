package net.osmand.plus.backup.trash;

import android.graphics.drawable.Drawable;

public class TrashItem {

	private String name;
	private Drawable icon;
	private long deleteTime;
	private boolean isLocalItem = true;

	public String getName() {
		return name;
	}

	public TrashItem setName(String name) {
		this.name = name;
		return this;
	}

	public Drawable getIcon() {
		return icon;
	}

	public TrashItem setIcon(Drawable icon) {
		this.icon = icon;
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
