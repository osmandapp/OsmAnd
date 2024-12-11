package net.osmand.plus.mapcontextmenu.editors.icon.data;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

public class IconsCategory {

	private final String key;
	private final String translatedName;
	private final List<String> iconKeys;
	private final boolean isTopCategory;

	public IconsCategory(@NonNull String key,
	                     @NonNull String translatedName,
	                     @NonNull List<String> iconKeys) {
		this(key, translatedName, iconKeys, false);
	}

	public IconsCategory(@NonNull String key,
	                     @NonNull String translatedName,
	                     @NonNull List<String> iconKeys,
	                     boolean isTopCategory) {
		this.key = key;
		this.translatedName = translatedName;
		this.iconKeys = iconKeys;
		this.isTopCategory = isTopCategory;
	}

	@NonNull
	public String getKey() {
		return key;
	}

	public boolean isTopCategory() {
		return isTopCategory;
	}

	@NonNull
	public String getTranslation() {
		return translatedName;
	}

	@NonNull
	public List<String> getIconKeys() {
		return iconKeys;
	}

	public boolean containsIcon(@NonNull String iconKey) {
		for (String key : iconKeys) {
			if (Objects.equals(iconKey, key)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof IconsCategory category)) return false;

		return getKey().equals(category.getKey());
	}

	@Override
	public int hashCode() {
		return getKey().hashCode();
	}

	@NonNull
	@Override
	public String toString() {
		return key;
	}
}
