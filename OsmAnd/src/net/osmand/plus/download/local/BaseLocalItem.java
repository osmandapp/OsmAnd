package net.osmand.plus.download.local;

import android.content.Context;

import androidx.annotation.NonNull;

public abstract class BaseLocalItem {

	protected final LocalItemType type;

	protected BaseLocalItem(LocalItemType type) {
		this.type = type;
	}

	@NonNull
	public LocalItemType getType() {
		return type;
	}

	public abstract LocalItemType getLocalItemType();

	public abstract long getLocalItemCreated();

	public abstract long getLocalItemSize();

	public abstract CharSequence getName(@NonNull Context context);

	public abstract String getDescription(Context context);
}
