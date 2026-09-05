package net.osmand.plus.base.containers;

import androidx.annotation.Nullable;

public class ScreenItem {

	private static final int TYPES_SHIFT = 10_000;

	public final int type;
	public final Object value;

	public ScreenItem(int type, @Nullable Object value) {
		this.type = type;
		this.value = value;
	}

	public ScreenItem(int type) {
		this(type, null);
	}

	public int getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	public int getId() {
		int id = type * TYPES_SHIFT;
		return value != null ? id + value.hashCode() : id;
	}
}
