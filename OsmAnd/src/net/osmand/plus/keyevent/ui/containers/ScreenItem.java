package net.osmand.plus.keyevent.ui.containers;

import androidx.annotation.Nullable;

public class ScreenItem {

	public final int type;
	public final Object value;

	public ScreenItem(int type, @Nullable Object value) {
		this.type = type;
		this.value = value;
	}

	public ScreenItem(int type) {
		this(type, null);
	}

	public int getId() {
		int id = type * 100_000;
		return value != null ? id + value.hashCode() : id;
	}
}
