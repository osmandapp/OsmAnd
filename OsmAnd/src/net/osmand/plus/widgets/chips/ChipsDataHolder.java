package net.osmand.plus.widgets.chips;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ChipsDataHolder {

	private List<ChipItem> items = new ArrayList<>();
	private ChipItem selected;

	public void setItems(@NonNull List<ChipItem> items) {
		this.items = items;
	}

	@NonNull
	public List<ChipItem> getItems() {
		return items;
	}

	public void setSelected(@Nullable ChipItem item) {
		selected = item;
	}

	public boolean isSelected(@Nullable ChipItem item) {
		return Objects.equals(item, selected);
	}

	@Nullable
	public ChipItem getSelected() {
		return selected;
	}

	@NonNull
	public ChipItem getItem(int position) {
		return items.get(position);
	}

	public int indexOf(@NonNull String title) {
		ChipItem item = getItemById(title);
		if (item != null) {
			return indexOf(item);
		}
		return -1;
	}

	public int indexOf(@NonNull ChipItem item) {
		return items.indexOf(item);
	}

	@Nullable
	public ChipItem getItemById(@NonNull String id) {
		for (ChipItem item : items) {
			if (id.equals(item.id)) {
				return item;
			}
		}
		return null;
	}

	@Nullable
	public ChipItem getItemByTag(@NonNull Object tag) {
		for (ChipItem item : items) {
			if (Objects.equals(item.tag, tag)) {
				return item;
			}
		}
		return null;
	}

}
