package net.osmand.plus.download.local.dialogs;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class MemoryInfo {

	private final List<MemoryItem> items = new ArrayList<>();

	public MemoryInfo(@NonNull List<MemoryItem> items) {
		this.items.addAll(items);
	}

	@NonNull
	public List<MemoryItem> getItems() {
		return items;
	}

	public boolean hasData() {
		return !items.isEmpty();
	}

	public long getSize() {
		long size = 0;
		for (MemoryItem item : items) {
			size += item.value;
		}
		return size;
	}

	public static class MemoryItem {

		@NonNull
		private final String text;
		private final float value;
		@ColorInt
		private final int color;

		public MemoryItem(@NonNull String text, float value, @ColorInt int color) {
			this.text = text;
			this.value = value;
			this.color = color;
		}

		@NonNull
		public String getText() {
			return text;
		}

		public float getValue() {
			return value;
		}

		@ColorInt
		public int getColor() {
			return color;
		}
	}
}
