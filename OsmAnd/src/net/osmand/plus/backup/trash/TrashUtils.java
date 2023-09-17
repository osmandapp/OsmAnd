package net.osmand.plus.backup.trash;

import static java.util.Collections.sort;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.ChangesUtils;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.trash.data.TrashGroup;
import net.osmand.plus.backup.trash.data.TrashItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
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
		List<TrashItem> result = new ArrayList<>();
		BackupInfo info = app.getBackupHelper().getBackup().getBackupInfo();
		if (info != null) {
			for (RemoteFile remoteFile : info.filesInTrash) {
				TrashItem trashItem = createTrashItem(remoteFile);
				if (trashItem != null) {
					result.add(trashItem);
				}
			}
		}
		return !Algorithms.isEmpty(result) ? result : FakeTrashData.collectFakeTrashItems(app);
	}

	@Nullable
	private TrashItem createTrashItem(@NonNull RemoteFile remoteFile) {
		if (remoteFile.item == null) {
			return null;
		}
		long deleteTime = remoteFile.getUpdatetimems();
		SettingsItem settingsItem = remoteFile.item;
		TrashItem trashItem = new TrashItem()
				.setName(ChangesUtils.getName(app, settingsItem))
				.setIconId(ChangesUtils.getIconId(settingsItem))
				.setDeleteTime(deleteTime)
				.setDescription(ChangesUtils.generateDeletedTimeString(app, deleteTime))
				.setLocalItem(false);
		trashItem.remoteFile = remoteFile;
		return trashItem;
	}

	public void emptyTrash() {
		FakeTrashData.emptyTrash();
		app.showShortToastMessage(R.string.trash_is_empty);
		notifyTrashDataUpdated();
	}

	public void restoreFromTrash(@NonNull TrashItem trashItem) {
		FakeTrashData.restoreFromTrash(trashItem);
		notifyTrashDataUpdated();
	}

	public void deleteImmediately(@NonNull TrashItem trashItem) {
		FakeTrashData.deleteImmediately(trashItem);
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

	public interface TrashDataUpdatedListener {
		void onTrashDataUpdated();
	}
}
