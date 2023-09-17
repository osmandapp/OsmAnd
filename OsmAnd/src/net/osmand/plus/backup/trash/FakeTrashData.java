package net.osmand.plus.backup.trash;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.ChangesUtils;
import net.osmand.plus.backup.trash.data.TrashItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;

import java.util.ArrayList;
import java.util.List;

public class FakeTrashData {

	// Fake data only for testing
	private static List<TrashItem> fakeTrashData = null;
	private static boolean isFakeDataInitialized = false;
	private static boolean useFakeData = false;

	@NonNull
	public static List<TrashItem> collectFakeTrashItems(@NonNull OsmandApplication app) {
		OsmandDevelopmentPlugin plugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
		if (plugin != null && plugin.isEnabled()) {
			useFakeData = true;
		}
		if (!isFakeDataInitialized) {
			fakeTrashData = collectTestTrashItems(app);
			isFakeDataInitialized = true;
		}
		return fakeTrashData;
	}

	@NonNull
	private static List<TrashItem> collectTestTrashItems(@NonNull OsmandApplication app) {
		// Time constants in milliseconds
		long ONE_HOUR = 3_600_000;
		long ONE_MINUTE = ONE_HOUR / 60;
		long ONE_DAY = ONE_HOUR * 24;
		long ONE_WEEK = ONE_DAY * 7;

		long now = System.currentTimeMillis();
		long deleteTime = now - ONE_HOUR;

		List<TrashItem> trashItems = new ArrayList<>();
		trashItems.add(new TrashItem()
				.setName("Driving")
				.setIconId(R.drawable.ic_action_car_dark)
				.setDeleteTime(deleteTime)
		);

		deleteTime -= ONE_DAY * 2;
		trashItems.add(new TrashItem()
				.setName("AmsterdamTrip")
				.setIconId(R.drawable.ic_action_polygom_dark)
				.setDeleteTime(deleteTime)
		);

		deleteTime -= ONE_WEEK - ONE_DAY * 4;
		trashItems.add(new TrashItem()
				.setName("2022-10-04_12-47_Tue")
				.setIconId(R.drawable.ic_action_polygom_dark)
				.setDeleteTime(deleteTime)
		);

		deleteTime -= 3 * ONE_WEEK + ONE_HOUR;
		trashItems.add(new TrashItem()
				.setName("Microsoft Earth")
				.setIconId(R.drawable.ic_layer_top)
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		deleteTime += ONE_HOUR * 7;
		trashItems.add(new TrashItem()
				.setName("Favorites")
				.setIconId(R.drawable.ic_action_folder_favorites)
				.setDeleteTime(deleteTime)
		);

		deleteTime -= ONE_WEEK * 12;
		trashItems.add(new TrashItem()
				.setName("Favorites Restaurant")
				.setIconId(R.drawable.ic_action_favorite)
				.setDeleteTime(deleteTime)
		);

		deleteTime -= ONE_HOUR * 1.5;
		trashItems.add(new TrashItem()
				.setName("Favorites SOTM")
				.setIconId(R.drawable.ic_action_favorite)
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		deleteTime -= ONE_MINUTE * 2;
		trashItems.add(new TrashItem()
				.setName("Favorites Flowers")
				.setIconId(R.drawable.ic_action_favorite)
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		deleteTime -= 500;
		trashItems.add(new TrashItem()
				.setName("Favorites TestOfflinePOI")
				.setIconId(R.drawable.ic_action_favorite)
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		deleteTime -= ONE_DAY * 2;
		trashItems.add(new TrashItem()
				.setName("Germany Baden-Wurttemberg Regierungsbezirk Stuttgart")
				.setIconId(R.drawable.ic_action_map_download)
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		// Setup description
		for (TrashItem trashItem : trashItems) {
			trashItem.setDescription(ChangesUtils.generateDeletedTimeString(app, trashItem.getDeleteTime()));
		}

		return trashItems;
	}

	public static void emptyTrash() {
		if (useFakeData) {
			fakeTrashData.clear();
		}
	}

	public static void restoreFromTrash(@NonNull TrashItem trashItem) {
		if (useFakeData) {
			fakeTrashData.remove(trashItem);
		}
	}

	public static void deleteImmediately(@NonNull TrashItem trashItem) {
		if (useFakeData) {
			fakeTrashData.remove(trashItem);
		}
	}

}
