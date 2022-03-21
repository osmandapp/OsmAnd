package net.osmand.plus.widgets.ctxmenu;

import androidx.annotation.NonNull;

import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.ctxmenu.data.ExpandableCategory;

import java.util.ArrayList;
import java.util.List;

public class ContextMenuUtils {

	public static List<String> getNames(@NonNull List<ContextMenuItem> items) {
		List<String> itemNames = new ArrayList<>();
		for (ContextMenuItem item : items) {
			itemNames.add(item.getTitle());
		}
		return itemNames;
	}

	public static List<ContextMenuItem> getCategorizedItems(@NonNull List<ContextMenuItem> items) {
		ExpandableCategory c = null;
		List<ContextMenuItem> list = new ArrayList<>();
		for (ContextMenuItem item : items) {
			if (item.isCategory()) {
				if (c != null) {
					list.add(c);
				}
				c = (ExpandableCategory) item;
			} else if (c != null) {
				c.addItem(item);
			} else {
				list.add(item);
			}
		}
		return list;
	}

}
