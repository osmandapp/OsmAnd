package net.osmand.plus.download.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.TreeMap;

public class LocalItemsHolder {

	private final Map<CategoryType, LocalCategory> categories = new TreeMap<>();

	@NonNull
	public Map<CategoryType, LocalCategory> getCategories() {
		return categories;
	}

	@Nullable
	public LocalCategory getCategory(@NonNull CategoryType type) {
		return categories.get(type);
	}

	public void addLocalItem(@NonNull LocalItem item) {
		ItemType itemType = item.getType();
		CategoryType categoryType = itemType.getCategoryType();

		LocalCategory category = categories.get(categoryType);
		if (category == null) {
			category = new LocalCategory(categoryType);
			categories.put(categoryType, category);
		}
		category.addLocalItem(item);
	}
}
