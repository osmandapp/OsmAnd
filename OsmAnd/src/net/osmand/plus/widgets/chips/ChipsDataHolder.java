package net.osmand.plus.widgets.chips;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class ChipsDataHolder {

	private List<ChipItem> items = new ArrayList<>();
	private ChipItem selected = null;

	public void setItems(List<ChipItem> items) {
		this.items = items;
	}

	public List<ChipItem> getItems() {
		return items;
	}

	public void setSelected(ChipItem item) {
		if (selected != null) {
			selected.isSelected = false;
		}
		item.isSelected = true;
		selected = item;
	}

	public ChipItem getSelected() {
		return selected;
	}

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

}
