package net.osmand.plus.download.local;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.AndroidUtils;

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
	public CharSequence getName(@NonNull Context context, boolean includeParent) {
		return getName(context);
	}

	@NonNull
	public abstract String getDescription(@NonNull Context context);

	@NonNull
	public String getSizeDescription(@NonNull Context context) {
		return AndroidUtils.formatSize(context, getSize());
	}

	public abstract long getSize();

	public abstract long getLastModified();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BaseLocalItem that = (BaseLocalItem) o;
		return type == that.type;
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}
}
