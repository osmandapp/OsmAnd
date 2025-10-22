package net.osmand.plus.backup;

import static net.osmand.plus.OsmAndConstants.AUTO_BACKUP;
import static net.osmand.plus.backup.NetworkSettingsHelper.SYNC_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_AUTO_SYNC;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;

import org.apache.commons.logging.Log;

public class AutoBackupHelper implements OnPrepareBackupListener {

	private static final Log log = PlatformUtil.getLog(AutoBackupHelper.class);

	private static final int MSG_RUN_AUTO_BACKUP = AUTO_BACKUP + 1;
	private static final int MSG_RUN_PERIODIC_BACKUP = AUTO_BACKUP + 2;
	private static final long MIN_AUTO_BACKUP_INTERVAL_MS = 60 * 1000; // 1 minute
	public static final long DEFAULT_AUTO_BACKUP_INTERVAL_MS = 60 * 60 * 1000; // 60 minutes

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final BackupHelper backupHelper;
	private final NetworkSettingsHelper settingsHelper;

	private final StateChangedListener<Long> intervalListener;

	public AutoBackupHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.backupHelper = app.getBackupHelper();
		this.settingsHelper = app.getNetworkSettingsHelper();

		initListeners();
		rescheduleAutoBackup();

		intervalListener = value -> rescheduleAutoBackup();
		settings.AUTO_BACKUP_INTERVAL_MS.addListener(intervalListener);
	}

	private void initListeners() {
		for (ExportType type : ExportType.availableValues()) {
			addListener(type);
		}
	}

	public void runAutoBackup() {
		long currentTime = System.currentTimeMillis();
		long lastAttemptTime = settings.LAST_AUTO_BACKUP_TIMESTAMP.get();
		long elapsedMillis = currentTime - lastAttemptTime;

		if (elapsedMillis >= MIN_AUTO_BACKUP_INTERVAL_MS) {
			attemptImmediateBackup(currentTime);
		} else {
			scheduleDelayedBackup(elapsedMillis);
		}
	}

	private void runPeriodicBackup() {
		runAutoBackup();
		rescheduleAutoBackup();
	}

	private void rescheduleAutoBackup() {
		app.removeMessagesInUiThread(MSG_RUN_PERIODIC_BACKUP);

		if (isAutoBackupConfigured()) {
			long interval = settings.AUTO_BACKUP_INTERVAL_MS.get();
			if (interval > 0) {
				app.runMessageInUiThread(MSG_RUN_PERIODIC_BACKUP, interval, this::runPeriodicBackup);
			}
		}
	}

	private void attemptImmediateBackup(long currentTime) {
		app.removeMessagesInUiThread(MSG_RUN_AUTO_BACKUP);

		if (canRunBackupNow()) {
			settings.LAST_AUTO_BACKUP_TIMESTAMP.set(currentTime);
			backupHelper.prepareBackup(true, this);
		}
	}

	private void scheduleDelayedBackup(long elapsedMillis) {
		if (isAutoBackupConfigured() && !app.hasMessagesInUiThread(MSG_RUN_AUTO_BACKUP)) {
			long delay = MIN_AUTO_BACKUP_INTERVAL_MS - elapsedMillis;
			app.runMessageInUiThread(MSG_RUN_AUTO_BACKUP, delay, this::runAutoBackup);
		}
	}

	private boolean canRunBackupNow() {
		return isAutoBackupConfigured() && !settingsHelper.isBackupSyncing() && !backupHelper.isBackupPreparing();
	}

	private boolean isAutoBackupConfigured() {
		return InAppPurchaseUtils.isBackupAvailable(app) && isAutoBackupEnabled() && backupHelper.isRegistered();
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
