package net.osmand.plus.mapcontextmenu.editors.data;

import androidx.annotation.NonNull;

public class IconSearchResult {

	private final IconData icon;
	private final String categoryName;

	public IconSearchResult(@NonNull IconData icon,
	                        @NonNull String categoryName) {
		this.icon = icon;
		this.categoryName = categoryName;
	}

	@NonNull
	public IconData getIcon() {
		return icon;
	}

	@NonNull
	public String getCategoryName() {
		return categoryName;
	}
}
