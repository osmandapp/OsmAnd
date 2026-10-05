package net.osmand.plus.widgets.popup;

import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PopUpMenu {

	private PopUpMenu() {
	}

	@Nullable
	public static PopupWindow showAndGet(@NonNull PopUpMenuDisplayData displayData) {
		return OsmAndDropdownMenuKt.showComposeDropdownMenu(displayData);
	}

	public static void show(@NonNull PopUpMenuDisplayData displayData) {
		OsmAndDropdownMenuKt.showComposeDropdownMenu(displayData);
	}
}
