package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_NONE;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_CONFLICTS;

import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.plus.backup.BackupInfo;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType;

import java.util.ArrayList;
import java.util.List;

public class ConflictsTabFragment extends ChangesTabFragment {

	@NonNull
	@Override
	public RecentChangesType getChangesTabType() {
		return RECENT_CHANGES_CONFLICTS;
	}

	@NonNull
	@Override
	public List<CloudChangeItem> generateData() {
		List<CloudChangeItem> changeItems = new ArrayList<>();

		PrepareBackupResult backup = backupHelper.getBackup();
		BackupInfo info = backup.getBackupInfo();
		if (info != null) {
			for (Pair<LocalFile, RemoteFile> pair : info.filteredFilesToMerge) {
				CloudChangeItem changeItem = createChangeItem(SYNC_OPERATION_NONE, pair.first, pair.second);
				if (changeItem != null) {
					// FIXME
					changeItems.add(changeItem);
				}
			}
		}
		return changeItems;
	}
}