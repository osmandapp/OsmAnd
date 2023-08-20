package net.osmand.plus.backup.trash;

import static net.osmand.plus.utils.OsmAndFormatter.formatChangesPassedTime;
import static java.util.Collections.sort;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.trash.data.TrashGroup;
import net.osmand.plus.backup.trash.data.TrashItem;
import net.osmand.util.Algorithms;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrashUtils {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("LLLL yyyy", Locale.getDefault());

	private final OsmandApplication app;
	private TrashDataUpdatedListener listener;

	// Fake data only for testing
	private static List<TrashItem> fakeTrashData = null;
	private static boolean isFakeDataInitialized = false;

	public TrashUtils(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void setListener(@Nullable TrashDataUpdatedListener listener) {
		this.listener = listener;
	}

	public List<TrashGroup> collectSortedTrashData() {
		List<TrashGroup> result = new ArrayList<>();
		Map<String, TrashGroup> quickCache = new HashMap<>();

		List<TrashItem> trashItems = collectTrashItems();
		// Sort trash items descending by deleting time
		sort(trashItems, (i1, i2) -> Long.compare(i2.getDeleteTime(), i1.getDeleteTime()));

		for (TrashItem trashItem : trashItems) {
			long deleteTime = trashItem.getDeleteTime();
			String formattedDate = Algorithms.capitalizeFirstLetter(formatDate(deleteTime));
			// Add new group if needed
			TrashGroup trashGroup = quickCache.get(formattedDate);
			if (trashGroup == null) {
				deleteTime = parseDateTime(formattedDate);
				trashGroup = new TrashGroup(formattedDate, deleteTime);
				quickCache.put(formattedDate, trashGroup);
				result.add(trashGroup);
			}
			trashGroup.addTrashItem(trashItem);
		}
		return result;
	}

	@NonNull
	public List<TrashItem> collectTrashItems() {
		if (!isFakeDataInitialized) {
			fakeTrashData = collectTestTrashItems(app);
			isFakeDataInitialized = true;
		}
		return fakeTrashData;
	}

	public void emptyTrash() {
		fakeTrashData.clear();
		app.showShortToastMessage(R.string.trash_is_empty);
		notifyTrashDataUpdated();
	}

	public void restoreFromTrash(@NonNull TrashItem trashItem) {
		fakeTrashData.remove(trashItem);
		notifyTrashDataUpdated();
	}

	public void deleteImmediately(@NonNull TrashItem trashItem) {
		fakeTrashData.remove(trashItem);
		notifyTrashDataUpdated();
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	private void notifyTrashDataUpdated() {
		if (listener != null) {
			listener.onTrashDataUpdated();
		}
	}

	private static String formatDate(long dateTimeMillis) {
		return DATE_FORMAT.format(dateTimeMillis);
	}

	private static long parseDateTime(String formattedDate) {
		try {
			Date date = DATE_FORMAT.parse(formattedDate);
			return date != null ? date.getTime() : 0;
		} catch (ParseException e) {
			return 0;
		}
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
			trashItem.setDescription(formatDeleteTimeDescription(app, trashItem.getDeleteTime()));
		}

		return trashItems;
	}

	public static String formatDeleteTimeDescription(@NonNull OsmandApplication app, long time) {
		String deleted = app.getString(R.string.shared_string_deleted);
		String formattedDate = formatChangesPassedTime(app, time, "", "MMM d, HH:mm", "HH:mm");
		return app.getString(R.string.ltr_or_rtl_combine_via_colon, deleted, formattedDate);
	}

	public interface TrashDataUpdatedListener {
		void onTrashDataUpdated();
	}
}
