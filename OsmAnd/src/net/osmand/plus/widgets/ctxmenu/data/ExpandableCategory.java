package net.osmand.plus.widgets.ctxmenu.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;

public class ExpandableCategory extends ContextMenuItem {

	private final List<ContextMenuItem> items = new ArrayList<>();

	public ExpandableCategory(@NonNull String id) {
		super(id);
	}

	@Override
	public boolean isCategory() {
		return true;
	}

	@Override
	public int getLayout() {
		return R.layout.list_item_expandable_category;
	}

	@Override
	public String getDescription() {
		List<String> titles = new ArrayList<>();
		for (ContextMenuItem item : items) {
			titles.add(item.getTitle());
		}
		return TextUtils.join(", ", titles);
	}

	public void addItem(@NonNull ContextMenuItem item) {
		items.add(item);
	}

	public List<ContextMenuItem> getItems() {
		return items;
	}
}
