package net.osmand.plus.mapcontextmenu.editors.data;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class IconsCategory {

	private final String key;
	private final String translatedName;
	private List<String> iconKeys = new ArrayList<>();

	public IconsCategory(@NonNull String key, @NonNull String translatedName) {
		this.key = key;
		this.translatedName = translatedName;
	}

	public IconsCategory(@NonNull String key,
	                     @NonNull String translatedName,
	                     @NonNull List<String> iconKeys) {
		this.key = key;
		this.translatedName = translatedName;
		this.iconKeys = iconKeys;
	}

	@NonNull
	public String getKey() {
		return key;
	}

	@NonNull
	public String getTranslatedName() {
		return translatedName;
	}

	public void addIcon(@NonNull String iconKey) {
		iconKeys.add(iconKey);
	}

	@NonNull
	public List<String> getIconKeys() {
		return iconKeys;
	}
}
