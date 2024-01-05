package net.osmand.plus.download.local;

import android.content.Context;

import androidx.annotation.NonNull;

public abstract class BaseLocalItem {

	protected final LocalItemType type;

	protected BaseLocalItem(@NonNull LocalItemType type) {
		this.type = type;
	}

	@NonNull
	public LocalItemType getType() {
		return type;
	}

	@NonNull
	public abstract CharSequence getName(@NonNull Context context);

	@NonNull
	public abstract String getDescription(@NonNull Context context);

	public abstract long getSize();

	public abstract long getLastModified();
}
