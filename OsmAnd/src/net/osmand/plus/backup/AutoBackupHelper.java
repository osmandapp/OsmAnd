package net.osmand.plus.backup;

import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_SYNC;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;

import org.apache.commons.logging.Log;

public class AutoBackupHelper implements OnPrepareBackupListener {

	private static final Log log = PlatformUtil.getLog(AutoBackupHelper.class);

	private static final String LAST_AUTO_BACKUP_ATTEMPT = "last_auto_backup_attempt";

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final BackupHelper backupHelper;
	private final NetworkSettingsHelper settingsHelper;

	public AutoBackupHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.backupHelper = app.getBackupHelper();
		this.settingsHelper = app.getNetworkSettingsHelper();
	}

	public void runAutoBackup() {
		if (isAutoBackupEnabled() && app.getBackupHelper().isRegistered()) {
			if (!backupHelper.isBackupPreparing()) {
				backupHelper.prepareBackup(true, this);
			} else if (!settingsHelper.isBackupSyncing() && backupHelper.isAutoSync()) {
				settingsHelper.syncSettingsItems(SYNC_ITEMS_KEY, SYNC_OPERATION_SYNC);
			}
		}
	}

	public boolean isAutoBackupEnabled() {
		for (ExportType type : ExportType.enabledValues()) {
			if (backupHelper.getBackupTypePref(type, true).get()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onBackupPreparing() {

	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult result) {
		if (!settingsHelper.isBackupSyncing() && result.isAutoSync()) {
			BackupInfo info = result.getBackupInfo();
			if (info != null && info.hasFilteredFiles()) {
				settingsHelper.syncSettingsItems(SYNC_ITEMS_KEY, SYNC_OPERATION_SYNC);
			}
		}
	}
}
