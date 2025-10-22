package net.osmand.plus.backup;

import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_AUTO_SYNC;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;

import org.apache.commons.logging.Log;

public class AutoBackupHelper implements OnPrepareBackupListener {

	private static final Log log = PlatformUtil.getLog(AutoBackupHelper.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final BackupHelper backupHelper;
	private final NetworkSettingsHelper settingsHelper;

	public AutoBackupHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.backupHelper = app.getBackupHelper();
		this.settingsHelper = app.getNetworkSettingsHelper();

		initListeners();
	}

	private void initListeners() {
		for (ExportType type : ExportType.availableValues()) {
			addListener(type);
		}
	}

	public void runAutoBackup() {
		if (InAppPurchaseUtils.isBackupAvailable(app) && isAutoBackupEnabled()
				&& app.getBackupHelper().isRegistered()
				&& !settingsHelper.isBackupSyncing() && !backupHelper.isBackupPreparing()) {
			backupHelper.prepareBackup(true, this);
		}
	}

	public boolean isAutoBackupEnabled() {
		for (ExportType type : ExportType.availableValues()) {
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
		if (result != null && result.isAutoSync() && !settingsHelper.isBackupSyncing()) {
			BackupInfo info = result.getBackupInfo();
			if (info != null && info.hasFilteredFiles()) {
				settingsHelper.syncSettingsItems(SYNC_ITEMS_KEY, SYNC_OPERATION_AUTO_SYNC);
			}
		}
	}

	private void addListener(@NonNull ExportType type) {
		switch (type) {
			case FAVORITES: {
				app.getFavoritesHelper().addListener(new FavoritesListener() {
					@Override
					public void onSavingFavoritesFinished() {
						runAutoBackup();
					}
				});
			}
		}
	}
}
