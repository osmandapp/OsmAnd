package net.osmand.plus.backup.trash;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class TrashUtils {

	private final OsmandApplication app;

	public TrashUtils(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	public List<TrashItem> collectTrashItems() {
		return collectTestTrashItems(app);
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
				.setIcon(getContentIcon(app, R.drawable.ic_action_car_dark))
				.setDeleteTime(deleteTime)
		);

		deleteTime -= ONE_DAY * 2;
		trashItems.add(new TrashItem()
				.setName("AmsterdamTrip")
				.setIcon(getContentIcon(app, R.drawable.ic_action_polygom_dark))
				.setDeleteTime(deleteTime)
		);

		deleteTime -= ONE_WEEK - ONE_DAY * 4;
		trashItems.add(new TrashItem()
				.setName("2022-10-04_12-47_Tue")
				.setIcon(getContentIcon(app, R.drawable.ic_action_polygom_dark))
				.setDeleteTime(deleteTime)
		);

		deleteTime -= ONE_HOUR;
		trashItems.add(new TrashItem()
				.setName("Microsoft Earth")
				.setIcon(getContentIcon(app, R.drawable.ic_layer_top))
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		deleteTime += ONE_HOUR * 7;
		trashItems.add(new TrashItem()
				.setName("Favorites")
				.setIcon(getContentIcon(app, R.drawable.ic_action_folder_favorites))
				.setDeleteTime(deleteTime)
		);

		deleteTime -= ONE_WEEK * 2;
		trashItems.add(new TrashItem()
				.setName("Favorites Restaurant")
				.setIcon(getContentIcon(app, R.drawable.ic_action_favorite))
				.setDeleteTime(deleteTime)
		);

		deleteTime -= ONE_HOUR * 1.5;
		trashItems.add(new TrashItem()
				.setName("Favorites SOTM")
				.setIcon(getContentIcon(app, R.drawable.ic_action_favorite))
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		deleteTime -= ONE_MINUTE * 2;
		trashItems.add(new TrashItem()
				.setName("Favorites Flowers")
				.setIcon(getContentIcon(app, R.drawable.ic_action_favorite))
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		deleteTime -= 500;
		trashItems.add(new TrashItem()
				.setName("Favorites TestOfflinePOI")
				.setIcon(getContentIcon(app, R.drawable.ic_action_favorite))
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		deleteTime -= ONE_DAY * 2;
		trashItems.add(new TrashItem()
				.setName("Germany Baden-Wurttemberg Regierungsbezirk Stuttgart")
				.setIcon(getContentIcon(app, R.drawable.ic_action_map_download))
				.setDeleteTime(deleteTime)
				.setLocalItem(false)
		);

		return trashItems;
	}

	@NonNull
	private static Drawable getContentIcon(@NonNull OsmandApplication app, int iconResId) {
		UiUtilities iconsCache = app.getUIUtilities();
		return iconsCache.getThemedIcon(iconResId);
	}

}
