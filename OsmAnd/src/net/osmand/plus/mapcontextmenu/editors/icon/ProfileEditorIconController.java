package net.osmand.plus.mapcontextmenu.editors.icon;

import androidx.annotation.NonNull;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.profiles.ProfileIcons;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class ProfileEditorIconController extends EditorIconController {

	private static final int LAST_USED_ICONS_LIMIT = 12;

	public ProfileEditorIconController(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	protected void initIconCategories() {
		initProfileIconsCategory();
		initLastUsedCategory();
		initAssetsCategories();
		initPoiCategories();
		sortCategories();
	}

	private void initProfileIconsCategory() {
		List<String> iconKeys = ProfileIcons.getIconKeysForPicker(app);
		if (!Algorithms.isEmpty(iconKeys)) {
			categories.add(new IconsCategory(ACTIVITIES_KEY, app.getString(R.string.shared_string_activity), iconKeys, true));
		}
	}

	@Override
	protected void sortCategories() {
		Collator collator = OsmAndCollator.primaryCollator();
		categories.sort((c1, c2) -> {
			int order1 = getCategoryOrder(c1.getKey());
			int order2 = getCategoryOrder(c2.getKey());
			if (order1 != order2) {
				return Integer.compare(order1, order2);
			}
			return collator.compare(c1.getTranslation(), c2.getTranslation());
		});
	}

	private int getCategoryOrder(@NonNull String key) {
		return switch (key) {
			case ACTIVITIES_KEY -> 0;
			case LAST_USED_KEY -> 1;
			case SPECIAL_KEY -> 2;
			case SYMBOLS_KEY -> 3;
			case TRAVEL_KEY -> 4;
			default -> 5;
		};
	}

	@NonNull
	@Override
	protected List<String> readLastUsedIcons() {
		List<String> storedIcons = app.getSettings().LAST_USED_PROFILE_ICONS.getStringsList();
		if (storedIcons == null) {
			return new ArrayList<>();
		}
		List<String> result = new ArrayList<>();
		for (String iconKey : storedIcons) {
			int iconRes = ProfileIcons.getDrawableResByPickerIconKey(app, iconKey);
			if (iconRes != 0) {
				String normalizedKey = ProfileIcons.getPickerIconKey(app, iconRes);
				if (!Algorithms.isEmpty(normalizedKey) && !result.contains(normalizedKey)) {
					result.add(normalizedKey);
				}
			}
			if (result.size() >= LAST_USED_ICONS_LIMIT) {
				break;
			}
		}
		return result;
	}

	@Override
	public void addIconToLastUsed(@NonNull String iconKey) {
		int iconRes = ProfileIcons.getDrawableResByPickerIconKey(app, iconKey);
		if (iconRes == 0) {
			return;
		}
		String normalizedKey = ProfileIcons.getPickerIconKey(app, iconRes);
		if (Algorithms.isEmpty(normalizedKey)) {
			return;
		}
		if (lastUsedIcons == null) {
			lastUsedIcons = new ArrayList<>();
		}
		lastUsedIcons.remove(normalizedKey);
		lastUsedIcons.add(0, normalizedKey);
		if (lastUsedIcons.size() > LAST_USED_ICONS_LIMIT) {
			lastUsedIcons = new ArrayList<>(lastUsedIcons.subList(0, LAST_USED_ICONS_LIMIT));
		}
		app.getSettings().LAST_USED_PROFILE_ICONS.setStringsList(lastUsedIcons);
	}
}
