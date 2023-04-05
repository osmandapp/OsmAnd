package net.osmand.plus.base.dialog.uidata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class DialogDisplayData {

	private String title;
	private String description;
	private List<DialogDisplayItem> displayItems;
	private int selectedItemIndex;

	@NonNull
	public String getTitle() {
		return title;
	}

	public void setTitle(@NonNull String title) {
		this.title = title;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@NonNull
	public List<DialogDisplayItem> getDisplayItems() {
		return displayItems;
	}

	public void setDisplayItems(@NonNull List<DialogDisplayItem> displayItems) {
		this.displayItems = displayItems;
	}

	public void setSelectedItemIndex(int selectedItemIndex) {
		this.selectedItemIndex = selectedItemIndex;
	}

	public int getSelectedItemIndex() {
		return selectedItemIndex;
	}
}
